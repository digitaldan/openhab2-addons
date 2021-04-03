/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.grandstreamgds.internal;

import static org.openhab.binding.grandstreamgds.internal.GDSConfigParameter.*;
import static org.openhab.binding.grandstreamgds.internal.GrandstreamGDSBindingConstants.CHANNEL_DOOR_OPEN;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.openhab.binding.grandstreamgds.internal.dto.GDSEventDTO;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.util.HexUtils;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@link GrandstreamGDSHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Dan Cunningham - Initial contribution
 */
@NonNullByDefault
public class GrandstreamGDSHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(GrandstreamGDSHandler.class);
    private static final String API_CMD_LOGIN_URL = "%s/goform/apicmd?cmd=0&user=%s";
    private static final String CONFIG_LOGIN_CHALLENGE_URL = "%s/goform/login?cmd=login&user=%s&type=0";
    private static final String CONFIG_LOGIN_RESPONSE_URL = "%s/goform/login?cmd=login&user=%s&authcode=%s&type=0";
    private static final String CONFIG_SET_URL = "%s/goform/config";
    private static final String CONFIG_EXPORT_URL = "%s/goform/config?cmd=export&type=0&data_type=1";
    private static final String CONFIG_EVENT_STREAM_URL = "%s:%s/%s/%s";
    private static final String CONFIG_EVENT_PAYLOAD_FORMAT = "{\"mac\":\"${MAC}\",\"type\":\"${TYPE}\",\"content\":\"${WARNING_MSG}\",\"sipnum\":\"${SIPNUM}\",\"username\":\"${USERNAME}\",\"date\":\"${DATE}\",\"cardid\":\"${CARDID}\"}";
    private static final String CONFIG_CHALLENGE_CODE = "Configuration/ChallengeCode";
    private final Gson gson = new GsonBuilder().create();

    private String gdsBaseURL = "";
    private String userName = "";
    private String password = "";

    private HttpClient httpClient;
    private NetworkAddressService networkAddressService;
    private int httpLisenerPort;
    private final HttpService httpService;
    private final String servletPath;
    private @Nullable ScheduledFuture<?> pollFuture;

    public GrandstreamGDSHandler(Thing thing, HttpService httpService, NetworkAddressService networkAddressService,
            int httpLisenerPort) {
        super(thing);
        this.networkAddressService = networkAddressService;
        this.httpService = httpService;
        httpClient = new HttpClient(new SslContextFactory.Client(true));
        servletPath = GrandstreamGDSBindingConstants.BASE_SERVLET_PATH + "/" + getThing().getUID().getId();
        try {
            httpService.registerServlet(servletPath, new GDSEventServlet(), null,
                    httpService.createDefaultHttpContext());
            logger.debug("GrandstreamGDSEventServlet started at '{}'", servletPath);
        } catch (NamespaceException | ServletException | IllegalArgumentException e) {
            logger.warn("Could not start GrandstreamGDSEventServlet", e);
        }
    }

    @Override
    public void initialize() {
        if (!httpClient.isStarted()) {
            try {
                httpClient.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        GrandstreamGDSConfiguration config = getConfigAs(GrandstreamGDSConfiguration.class);
        gdsBaseURL = config.url;
        userName = config.username;
        password = config.password;
        updateStatus(ThingStatus.UNKNOWN);
        if (config.modifyGdsConfig) {
            scheduler.execute(() -> {
                try {
                    updateEventStreamConfig();
                    updateStatus(ThingStatus.ONLINE);
                } catch (InterruptedException | ExecutionException | TimeoutException | GDSResponseException e) {
                    logger.debug("Could not update GDS", e);
                }
            });
        }
        initPolling(0);
    }

    @Override
    public void dispose() {
        clearPolling();

        try {
            httpService.unregister(servletPath);
        } catch (IllegalArgumentException e) {
            logger.debug("Error unregestering servlet", e);
        }

        if (httpClient.isStarted()) {
            try {
                httpClient.stop();
            } catch (Exception e) {
                logger.debug("Error stopping httpClient", e);
            }
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (CHANNEL_DOOR_OPEN.equals(channelUID.getId())) {
            if (command instanceof RefreshType) {
                initPolling(0);
            }

            if (command instanceof OnOffType) {
                if (command == OnOffType.ON) {
                    String remoteCode;
                    try {
                        remoteCode = getRemoteCode();
                        if (remoteCode != null) {
                            openDoor(remoteCode);
                        }
                    } catch (InterruptedException | ExecutionException | TimeoutException | GDSResponseException e) {
                        logger.debug("Could not open gate", e);
                    }

                    updateState(channelUID.getId(), OnOffType.OFF);
                }
            }
        }
    }

    private void handleGDSEvent(String data) {
        GDSEventDTO event = gson.fromJson(data, GDSEventDTO.class);
        if (event != null) {
            logger.debug("Event type {} : {} {}", event.type, event.date, event.content);

            GDSEventType type = GDSEventType.fromType(event.type);
            if (type != null) {
                triggerChannel("event_all", data);
                triggerChannel("event_" + type.name().toLowerCase());
            }
        }
    }

    private synchronized void initPolling(int initalDelay) {
        clearPolling();
        pollFuture = scheduler.scheduleWithFixedDelay(this::poll, initalDelay, 30, TimeUnit.SECONDS);
    }

    /**
     * Stops/clears this thing's polling future
     */
    private void clearPolling() {
        ScheduledFuture<?> pollFutureLocal = pollFuture;
        if (pollFutureLocal != null && !pollFutureLocal.isCancelled()) {
            logger.trace("Canceling future");
            pollFutureLocal.cancel(false);
        }
    }

    private void poll() {
        try {
            updateDigitalInputState();
            updateStatus(ThingStatus.ONLINE);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.debug("Could not update DI states", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } catch (GDSResponseException e) {
            String message = e.responseCode.getDescription();
            switch (e.responseCode) {
                case AUTHENTICATION_FAILED:
                case RETRIEVE_PASSWORD_NO_ACCOUNT:
                case USER_DOES_NOT_EXIST:
                case PASSWORD_ERROR:
                    clearPolling();
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, message);
                    break;
                default:
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, message);
            }
        }
    }

    private void updateEventStreamConfig()
            throws InterruptedException, ExecutionException, TimeoutException, GDSResponseException {
        String ip = networkAddressService.getPrimaryIpv4HostAddress();
        if (ip == null) {
            logger.debug("Could not determine IP address, not setting event stream config");
            return;
        }

        if (httpLisenerPort <= 0) {
            logger.warn("Http listening port not found, aborting GDS update");
            return;
        }

        String cookie = configLogin();
        if (cookie == null) {
            logger.warn("Unexpected response, aborting GDS update");
            return;
        }
        String eventUrl = String.format(CONFIG_EVENT_STREAM_URL, ip, httpLisenerPort,
                GrandstreamGDSBindingConstants.BINDING_ID, this.getThing().getUID().getId());
        Fields fields = new Fields();
        fields.put("cmd", "set");
        // set HTTP (1) or HTTPS (2)
        fields.put(EVENT_NOTIFICATION_EVENT_UPGRADE_TYPE.getId(), "1");
        // set enable events
        fields.put(EVENT_NOTIFICATION_ENABLE_EVENT_NOTIFICATION.getId(), "1");
        // set the callback url (host/path)
        fields.put(EVENT_NOTIFICATION_HTTP_SERVER_URL.getId(), eventUrl);
        // set the payload template
        fields.put(EVENT_NOTIFICATION_URL_TEMPLATE.getId(), CONFIG_EVENT_PAYLOAD_FORMAT);
        ContentResponse response = doPost(String.format(CONFIG_SET_URL, gdsBaseURL), cookie, fields);
        logger.debug("Config event url set response {}", response.getContentAsString());
    }

    private @Nullable String getRemoteCode()
            throws InterruptedException, ExecutionException, TimeoutException, GDSResponseException {
        String config = getGDSConfig();
        if (config != null) {
            // XML returned is sometimes not valid enough for XPATH and throws a parse exception, so do this the ugly
            // way.
            // Pattern pattern = Pattern.compile(".*<P10457>(.*?)<\\/P10457>.*");
            // Matcher match = pattern.matcher(config);
            // if (match.find()) {
            // logger.debug("Open Code {}", match.group(1));
            // return match.group(1);
            // }
            return getXMLValue(config, DOOR_SYSTEM_REMOTE_PIN_TO_OPEN_THE_DOOR.getXapth());
        }
        return null;
    }

    private void updateDigitalInputState()
            throws InterruptedException, ExecutionException, TimeoutException, GDSResponseException {
        String cookie = configLogin();
        logger.debug("Cookie {}", cookie);
        if (cookie != null) {
            Fields fields = new Fields();
            fields.put("cmd", "get");
            fields.put("type", "event");
            fields.put("t", String.valueOf(System.currentTimeMillis()));
            ContentResponse response = doPost(String.format(CONFIG_SET_URL, gdsBaseURL), cookie, fields);
            String content = response.getContentAsString();
            String digital1 = getXMLValue(content, EVENT_DIGIT_INPUT_1_STATUS.getXapth());
            String digital2 = getXMLValue(content, EVENT_DIGIT_INPUT_2_STATUS.getXapth());
            logger.debug("DI {} : {}", digital1, digital2);
        }
    }

    private @Nullable String getGDSConfig()
            throws InterruptedException, ExecutionException, TimeoutException, GDSResponseException {
        String cookie = configLogin();
        logger.debug("Cookie {}", cookie);
        if (cookie != null) {
            String doorConfigUrl = String.format(CONFIG_EXPORT_URL, gdsBaseURL);
            ContentResponse response = doGet(doorConfigUrl, cookie);
            return response.getContentAsString();
        }
        return null;
    }

    private @Nullable String configLogin()
            throws InterruptedException, ExecutionException, TimeoutException, GDSResponseException {
        ContentResponse response = doGet(String.format(CONFIG_LOGIN_CHALLENGE_URL, gdsBaseURL, userName));
        String content = response.getContentAsString();
        String challengeCode = getXMLValue(content, CONFIG_CHALLENGE_CODE);
        String secret = String.format("%s:%s:%s", challengeCode, "GDS3710lZpRsFzCbM", password);
        logger.debug("Secret {}", secret);
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Coult not load md5 instance", e);
        }

        md.update(secret.getBytes());
        String hash = HexUtils.bytesToHex(md.digest()).toLowerCase();
        String loginUrl = String.format(CONFIG_LOGIN_RESPONSE_URL, gdsBaseURL, userName, hash);
        logger.debug("loginUrl {}", loginUrl);
        response = httpClient.GET(loginUrl);
        return response.getHeaders().get("Set-Cookie");
    }

    private void openDoor(String remoteCode)
            throws InterruptedException, ExecutionException, TimeoutException, GDSResponseException {
        ContentResponse response = doGet(String.format(API_CMD_LOGIN_URL, gdsBaseURL, userName));
        String content = response.getContentAsString();

        String challengeCode = getXMLValue(content, CONFIG_CHALLENGE_CODE);
        logger.debug("Challenge Code {}", challengeCode);

        String idCode = getXMLValue(content, "Configuration/IDCode");
        String secret = String.format("%s:%s:%s", challengeCode, remoteCode, password);

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(secret.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Coult not load md5 instance", e);
        }

        String hash = HexUtils.bytesToHex(md.digest()).toLowerCase();
        String doorOpenUrl = String.format("%s/goform/apicmd?cmd=1&user=%s&authcode=%s&idcode=%s&type=1", gdsBaseURL,
                userName, hash, idCode);
        doGet(doorOpenUrl);
    }

    private ContentResponse doGet(String url)
            throws InterruptedException, ExecutionException, TimeoutException, GDSResponseException {
        return doGet(url, null);
    }

    private ContentResponse doGet(String url, @Nullable String cookie)
            throws InterruptedException, ExecutionException, TimeoutException, GDSResponseException {
        logger.trace("doGet Request {}", url);
        Request request = httpClient.newRequest(url);
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        ContentResponse response = request.send();
        String content = response.getContentAsString();
        logger.trace("doGet Response {}", content);
        checkGDSResponse(content);
        return response;
    }

    private ContentResponse doPost(String url, String cookie, Fields formData)
            throws InterruptedException, ExecutionException, TimeoutException, GDSResponseException {
        logger.trace("doPost Request {}", url);
        ContentResponse response = httpClient.newRequest(String.format(CONFIG_SET_URL, gdsBaseURL)).method("POST")
                .header("Cookie", cookie).content(new FormContentProvider(formData)).send();
        String content = response.getContentAsString();
        logger.trace("doPost Response {}", content);
        checkGDSResponse(content);
        return response;
    }

    private @Nullable String getXMLValue(String doc, String path) {
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        InputSource is = new InputSource(new StringReader(doc));
        try {
            return xpath.evaluate(path, is);
        } catch (XPathExpressionException e) {
            return null;
        }
    }

    private void checkGDSResponse(String data) throws GDSResponseException {
        String value = getXMLValue(data, "Configuration/ResCode");
        GDSResponseCode responseCode = GDSResponseCode.fromCode(value);
        if (responseCode == GDSResponseCode.SUCCESS) {
            throw new GDSResponseException(responseCode);
        }
    }

    private class GDSEventServlet extends HttpServlet {
        private static final long serialVersionUID = 196528887872920765L;

        @Override
        protected void service(@Nullable HttpServletRequest request, @Nullable HttpServletResponse resp)
                throws ServletException, IOException, IllegalArgumentException {

            if (request == null || resp == null) {
                logger.debug("bad request");
                return;
            }

            String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            logger.debug("GDS Event {}", body);
            handleGDSEvent(body);
            resp.setCharacterEncoding(StandardCharsets.UTF_8.toString());
            resp.getWriter().write("ok");

        }
    }

    private class GDSResponseException extends Exception {
        private GDSResponseCode responseCode;

        public GDSResponseException(GDSResponseCode responseCode) {
            super();
            this.responseCode = responseCode;
        }

        public GDSResponseCode getGSDResponseCode() {
            return responseCode;
        }

        @Override
        public String toString() {
            return responseCode.getDescription();
        }
    }
}
