package org.openhab.binding.hydrawise.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.auth.client.oauth2.AccessTokenRefreshListener;
import org.eclipse.smarthome.core.auth.client.oauth2.AccessTokenResponse;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthClientService;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthException;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthFactory;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthResponseException;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.hydrawise.internal.api.HydrawiseAuthenticationException;
import org.openhab.binding.hydrawise.internal.api.HydrawiseConnectionException;
import org.openhab.binding.hydrawise.internal.api.graphql.HydrawiseGraphQLClient;
import org.openhab.binding.hydrawise.internal.api.graphql.schema.Customer;
import org.openhab.binding.hydrawise.internal.api.graphql.schema.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class HydrawiseAccountHandler extends BaseBridgeHandler implements AccessTokenRefreshListener {
    private final Logger logger = LoggerFactory.getLogger(HydrawiseAccountHandler.class);
    /**
     * Minimum amount of time we can poll for updates
     */
    private static final int MIN_REFRESH_SECONDS = 30;
    private static final int DEFAULT_REFRESH_SECONDS = 60;
    private static final String BASE_URL = "https://app.hydrawise.com/api/v2/";
    private static final String AUTH_URL = BASE_URL + "oauth/access-token";
    private static final String CLIENT_SECRET = "zn3CrjglwNV1";
    private static final String CLIENT_ID = "hydrawise_app";
    private static final String SCOPE = "all";
    private final List<HydrawiseControllerListener> controllerListeners = new ArrayList<HydrawiseControllerListener>();
    private final HydrawiseGraphQLClient apiClient;
    private final OAuthClientService oAuthService;
    private @Nullable ScheduledFuture<?> pollFuture;
    private @Nullable Customer lastData;
    private int refresh;

    public HydrawiseAccountHandler(final Bridge bridge, final HttpClient httpClient, final OAuthFactory oAuthFactory) {
        super(bridge);
        this.oAuthService = oAuthFactory.createOAuthClientService(getThing().toString(), AUTH_URL, AUTH_URL, CLIENT_ID,
                CLIENT_SECRET, SCOPE, false);
        oAuthService.addAccessTokenRefreshListener(this);
        this.apiClient = new HydrawiseGraphQLClient(httpClient, oAuthService);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

    }

    @Override
    public void initialize() {
        logger.debug("Handler initialized.");
        scheduler.schedule(this::configure, 0, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        logger.debug("Handler disposed.");
        clearPolling();
    }

    @Override
    public void onAccessTokenResponse(AccessTokenResponse tokenResponse) {
        logger.debug("Auth Token Refreshed, expires in {}", tokenResponse.getExpiresIn());
    }

    public void addControllerListeners(HydrawiseControllerListener listener) {
        this.controllerListeners.add(listener);
        Customer data = lastData;
        if (data != null) {
            listener.onData(data.controllers);
        }
    }

    public void removeControllerListeners(HydrawiseControllerListener listener) {
        this.controllerListeners.remove(listener);
    }

    public @Nullable HydrawiseGraphQLClient graphQLClient() {
        return apiClient;
    }

    public @Nullable Customer lastData() {
        return lastData;
    }

    public void refreshData(int delaySeconds) {
        initPolling(delaySeconds, this.refresh);
    }

    private void configure() {
        HydrawiseAccountConfiguration config = getConfig().as(HydrawiseAccountConfiguration.class);
        try {
            // TODO switch to Java 11 String.isBlank
            if (StringUtils.isNotBlank(config.userName) && StringUtils.isNotBlank(config.password)) {
                if (!config.savePassword) {
                    Configuration editedConfig = editConfiguration();
                    editedConfig.remove("password");
                    updateConfiguration(editedConfig);
                }
                oAuthService.getAccessTokenByResourceOwnerPasswordCredentials(config.userName, config.password, SCOPE);
            } else if (oAuthService.getAccessTokenResponse() == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Login credentials required.");
                return;
            }

            this.refresh = Math.max(config.refreshInterval != null ? config.refreshInterval : DEFAULT_REFRESH_SECONDS,
                    MIN_REFRESH_SECONDS);
            initPolling(0, refresh);
        } catch (OAuthException | IOException e) {
            logger.debug("Could not log in", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } catch (OAuthResponseException e) {
            logger.debug("Could not log in", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Login credentials required.");
        }
    }

    /**
     * Starts/Restarts polling with an initial delay. This allows changes in the poll cycle for when commands are sent
     * and we need to poll sooner then the next refresh cycle.
     */
    private synchronized void initPolling(int initalDelay, int refresh) {
        clearPolling();
        pollFuture = scheduler.scheduleWithFixedDelay(this::poll, initalDelay, refresh, TimeUnit.SECONDS);
    }

    /**
     * Stops/clears this thing's polling future
     */
    private void clearPolling() {
        ScheduledFuture<?> localFuture = pollFuture;
        if (isFutureValid(localFuture)) {
            if (localFuture != null) {
                localFuture.cancel(false);
            }
        }
    }

    private boolean isFutureValid(@Nullable ScheduledFuture<?> future) {
        return future != null && !future.isCancelled();
    }

    private void poll() {
        poll(true);
    }

    private void poll(boolean retry) {
        try {
            QueryResponse response = apiClient.queryControllers();
            if (getThing().getStatus() != ThingStatus.ONLINE) {
                updateStatus(ThingStatus.ONLINE);
            }
            lastData = response.data.me;
            controllerListeners.forEach(listener -> {
                listener.onData(response.data.me.controllers);
            });
        } catch (HydrawiseConnectionException e) {
            if (retry) {
                logger.debug("Retrying failed poll", e);
                poll(false);
            } else {
                logger.debug("Will try again during next poll period", e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            }
        } catch (HydrawiseAuthenticationException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
            clearPolling();
        }
    }
}
