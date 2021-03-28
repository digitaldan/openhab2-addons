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

import static org.openhab.binding.grandstreamgds.internal.GrandstreamGDSBindingConstants.CHANNEL_DOOR_OPEN;

import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

/**
 * The {@link GrandstreamGDSHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Dan Cunningham - Initial contribution
 */
@NonNullByDefault
public class GrandstreamGDSHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(GrandstreamGDSHandler.class);

    private @Nullable GrandstreamGDSConfiguration config;
    private String gdsBaseURL = "http://192.168.91.139";
    private String userName = "admin";
    private String password = "ch1potl3";

    private HttpClient httpClient;

    public GrandstreamGDSHandler(Thing thing, HttpClient httpClient) {
        super(thing);
        this.httpClient = httpClient;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (CHANNEL_DOOR_OPEN.equals(channelUID.getId())) {
            if (command instanceof RefreshType) {
                // TODO: handle data refresh
            }

            if (command instanceof OnOffType) {
                if (command == OnOffType.ON) {
                    String remoteCode;
                    try {
                        remoteCode = getRemoteCode();
                        if (remoteCode != null) {
                            openGate(remoteCode);
                        }
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        logger.debug("Could not open gate", e);
                    }

                    updateState(channelUID.getId(), OnOffType.OFF);
                }
            }
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(GrandstreamGDSConfiguration.class);

        updateStatus(ThingStatus.UNKNOWN);
        scheduler.execute(() -> {
            boolean thingReachable = true; // <background task with long running initialization here>
            // when done do:
            if (thingReachable) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        });
    }

    private @Nullable String getRemoteCode() throws InterruptedException, ExecutionException, TimeoutException {
        String config = getGDSConfig();
        if (config != null) {
            // XML returned is not valid enough for XPATH and throws a parse exception, so do this the ugly way.
            Pattern pattern = Pattern.compile(".*<P10457>(.*?)<\\/P10457>.*");
            Matcher match = pattern.matcher(config);
            if (match.find()) {
                logger.debug("Open Code {}", match.group(1));
                return match.group(1);
            }
            /**
             * For setting http://<servername>/goform/config?cmd=set&<parameter>=<value>.
             * <!-- Event Notification -->
             * <Module_Event_Notification>
             * <!-- Enable Event Notification (0:Disable 1:Enable) -->
             * <P15410>1</P15410>
             * <!-- Upgrade Type (1: HTTP 2: HTTPS) -->
             * <P15417>2</P15417>
             * <!-- HTTP Server URL (Type:string. Example: http://192.168.1.2/ Max.length=256) -->
             * <P15413>oh.digitaldan.com</P15413>
             * <!-- HTTP Server Username (Type:string. Max.length=128) -->
             * <P15414></P15414>
             * <!-- HTTP Server Password (Type:string. Max.length=128) -->
             * <P15415></P15415>
             * <!-- URL Template (Type: String Maxlen=1024) -->
             * <P15416>mac=${MAC}&type=${TYPE}&content=${WARNING_MSG}&sipnum=${SIPNUM}&username=${USERNAME}&date=${DATE}</P15416>
             * </Module_Event_Notification>
             */
        }
        return null;
    }

    private @Nullable String getGDSConfig() throws InterruptedException, ExecutionException, TimeoutException {
        String cookie = configLogin();
        logger.debug("Cookie {}", cookie);
        if (cookie != null) {
            String doorConfigUrl = String.format("%s/goform/config?cmd=export&type=0&data_type=1", gdsBaseURL);

            try {
                ContentResponse response = httpClient.newRequest(doorConfigUrl).header("Cookie", cookie).send();
                logger.debug("Response {}", response.getContentAsString());
                return response.getContentAsString();
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                logger.debug("Error requesting data", e);
            }
        }
        return null;
    }

    private @Nullable String configLogin() throws InterruptedException, ExecutionException, TimeoutException {

        ContentResponse response = httpClient
                .GET(String.format("%s/goform/login?cmd=login&user=%s&type=0", gdsBaseURL, userName));
        String content = response.getContentAsString();
        logger.debug("Token Response {}", content);
        String challengeCode = getXMLValue(content, "Configuration/ChallengeCode");
        String secret = String.format("%s:%s:%s", challengeCode, "GDS3710lZpRsFzCbM", password);
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Coult not load md5 instance", e);
        }

        md.update(secret.getBytes());
        String hash = HexUtils.bytesToHex(md.digest()).toLowerCase();
        String loginUrl = String.format("%s/goform/login?cmd=login&user=%s&authcode=%s&type=0", gdsBaseURL, userName,
                hash);
        logger.debug("loginUrl {}", loginUrl);
        response = httpClient.GET(loginUrl);

        logger.debug("Response {}", response.getContentAsString());
        /**
         * Success
         * <?xml version="1.0"encoding="UTF-8" ?> <Configuration> <ResCode>0</ResCode> <LoginType>0</LoginType>
         * <RetMsg>OK</RetMsg>
         * </Configuration>
         */
        response.getHeaders().stream().forEach(e -> {
            logger.debug("Header {} {} ", e.getName(), e.getValue());
        });
        return response.getHeaders().get("Set-Cookie");

    }

    private void openGate(String remoteCode) throws InterruptedException, ExecutionException, TimeoutException {
        ContentResponse response = httpClient
                .GET(String.format("%s/goform/apicmd?cmd=0&user=%s", gdsBaseURL, userName));

        if (response.getStatus() != 200) {
            // do what?
        }
        String content = response.getContentAsString();
        logger.debug("Token Response {}", content);

        String challengeCode = getXMLValue(content, "Configuration/ChallengeCode");
        logger.debug("Challenge Code {}", challengeCode);
        String idCode = getXMLValue(content, "Configuration/IDCode");
        String secret = String.format("%s:%s:%s", challengeCode, remoteCode, password);
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Coult not load md5 instance", e);
        }
        md.update(secret.getBytes());
        String hash = HexUtils.bytesToHex(md.digest()).toLowerCase();
        String doorOpenUrl = String.format("%s/goform/apicmd?cmd=1&user=%s&authcode=%s&idcode=%s&type=1", gdsBaseURL,
                userName, hash, idCode);
        logger.debug("doorURL {}", doorOpenUrl);
        response = httpClient.GET(doorOpenUrl);
        logger.debug("Response {}", response.getContentAsString());
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
}
