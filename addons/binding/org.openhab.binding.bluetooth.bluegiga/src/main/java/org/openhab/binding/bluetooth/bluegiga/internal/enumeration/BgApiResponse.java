/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.bluetooth.bluegiga.internal.enumeration;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to implement the BlueGiga Enumeration <b>BgApiResponse</b>.
 * <p>
 * Response codes
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
public enum BgApiResponse {
    /**
     * Default unknown value
     */
    UNKNOWN(-1),

    /**
     * [0] Completed successfully.
     */
    SUCCESS(0x0000),

    /**
     * [257] Invalid GATT connection handle.
     */
    INVALID_CONN_HANDLE(0x0101),

    /**
     * [258] Waiting response from GATT server to previous procedure.
     */
    WAITING_RESPONSE(0x0102),

    /**
     * [384] Command contained invalid parameter
     */
    INVALID_PARAM(0x0180),

    /**
     * [385] Device is in wrong state to receive command
     */
    WRONG_STATE(0x0181),

    /**
     * [386] Device has run out of memory
     */
    OUT_OF_MEMORY(0x0182),

    /**
     * [387] Feature is not implemented
     */
    NOT_IMPLEMENTED(0x0183),

    /**
     * [388] Command was not recognized
     */
    INVALID_COMMAND(0x0184),

    /**
     * [389] Command or Procedure failed due to timeout
     */
    TIMEOUT(0x0185),

    /**
     * [390] Connection handle passed is to command is not a valid handle
     */
    NOT_CONNECTED(0x0186),

    /**
     * [391] Command would cause either underflow or overflow error
     */
    FLOW(0x0187),

    /**
     * [392] User attribute was accessed through API which is not supported
     */
    USER_ATTRIBUTE(0x0188),

    /**
     * [393] No valid license key found
     */
    INVALID_LICENSE_KEY(0x0189),

    /**
     * [394] Command maximum length exceeded
     */
    COMMAND_TOO_LONG(0x018A),

    /**
     * [395] Bonding procedure can't be started because device has no space left for bond.
     */
    OUT_OF_BONDS(0x018B),

    /**
     * [396] Unspecified error
     */
    UNSPECIFIED(0x018C),

    /**
     * [397] Hardware failure
     */
    HARDWARE(0x018D),

    /**
     * [398] Command not accepted, because internal buffers are full
     */
    BUFFERS_FULL(0x018E),

    /**
     * [399] Command or Procedure failed due to disconnection
     */
    DISCONNECTED(0x018F),

    /**
     * [400] Too many Simultaneous Requests
     */
    TOO_MANY_REQUESTS(0x0190),

    /**
     * [401] Feature is not supported in this firmware build
     */
    NOT_SUPPORTED(0x0191),

    /**
     * [402] The bonding does not exist.
     */
    NO_BONDING(0x0192),

    /**
     * [403] Error using crypto functions
     */
    CRYPTO(0x0193),

    /**
     * [514] A command was sent from the Host that should identify a connection, but that connection
     * does not exist.
     */
    UNKNOWN_CONNECTION_IDENTIFIER(0x0202),

    /**
     * [520] Link supervision timeout has expired.
     */
    CONNECTION_TIMEOUT(0x0208),

    /**
     * [521] Controller is at limit of connections it can support.
     */
    CONNECTION_LIMIT_EXCEEDED(0x0209),

    /**
     * [522]
     */
    SYNCHRONOUS_CONNECTIONTION_LIMIT_EXCEEDED(0x020A),

    /**
     * [523] The ACL Connection Already Exists error code indicates that an attempt to create a new
     * ACL Connection to a device when there is already a connection to this device.
     */
    ACL_CONNECTION_ALREADY_EXISTS(0x020B),

    /**
     * [524] Command requested cannot be executed because the Controller is in a state where it
     * cannot process this command at this time.
     */
    COMMAND_DISALLOWED(0x020C),

    /**
     * [525] The Connection Rejected Due To Limited Resources error code indicates that an
     * incoming connection was rejected due to limited resources.
     */
    CONNECTION_REJECTED_DUE_TO_LIMITED_RESOURCES(0x020D),

    /**
     * [526] The Connection Rejected Due To Security Reasons error code indicates that a
     * connection was rejected due to security requirements not being fulfilled, like
     * authentication or pairing.
     */
    CONNECTION_REJECTED_DUE_TO_SECURITY_REASONS(0x020E),

    /**
     * [527] The Connection was rejected because this device does not accept the BD_ADDR. This may
     * be because the device will only accept connections from specific BD_ADDRs.
     */
    CONNECTION_REJECTED_DUE_TO_UNACCEPTABLE_BD_ADDR(0x020F),

    /**
     * [528] The Connection Accept Timeout has been exceeded for this connection attempt.
     */
    CONNECTION_ACCEPT_TIMEOUT_EXCEEDED(0x0210),

    /**
     * [529] A feature or parameter value in the HCI command is not supported.
     */
    UNSUPPORTED_FEATURE_OR_PARAMETER_VALUE(0x0211),

    /**
     * [530] Command contained invalid parameters.
     */
    INVALID_COMMAND_PARAMETERS(0x0212),

    /**
     * [531] User on the remote device terminated the connection.
     */
    REMOTE_USER_TERMINATED(0x0213),

    /**
     * [532] The remote device terminated the connection because of low resources
     */
    REMOTE_DEVICE_TERMINATED_CONNECTION_LOW_RESOURCES(0x0214),

    /**
     * [533] Remote Device Terminated Connection due to Power Off
     */
    REMOTE_POWERING_OFF(0x0215),

    /**
     * [534] Local device terminated the connection.
     */
    CONNECTION_TERMINATED_BY_LOCAL_HOST(0x0216),

    /**
     * [535] The Controller is disallowing an authentication or pairing procedure because too
     * little time has elapsed since the last authentication or pairing attempt failed.
     */
    REPEATED_ATTEMPTS(0x0217),

    /**
     * [536] The device does not allow pairing. This can be for example, when a device only allows
     * pairing during a certain time window after some user input allows pairing
     */
    PAIRING_NOT_ALLOWED(0x0218),

    /**
     * [537] The Controller has received an unknown LMP OpCode.
     */
    UNKNOWN_LMP_PDU(0x0219),

    /**
     * [538] The remote device does not support the feature associated with the issued command or
     * LMP PDU.
     */
    UNSUPPORTED_REMOTE_FEATURE(0x021A),

    /**
     * [560] A parameter value requested is outside the mandatory range of parameters for the given
     * HCI command or LMP PDU.
     */
    PARAMETER_OUT_OF_MANDATORY_RANGE(0x0230),

    /**
     * [569] The Controller could not calculate an appropriate value for the Channel selection
     * operation.
     */
    CONNECTION_REJECTED_NO_SUITABLE_CHANNEL(0x0239),

    /**
     * [570] Operation was rejected because the controller is busy and unable to process the
     * request.
     */
    CONTROLLER_BUSY(0x023A),

    /**
     * [571] Remote device terminated the connection because of an unacceptable connection
     * interval.
     */
    UNACCEPTABLE_CONNECTION_INTERVAL(0x023B),

    /**
     * [573] Connection was terminated because the Message Integrity Check (MIC) failed on a
     * received packet.
     */
    CONNECTION_TERMINATED_DUE_TO_MIC_FAILURE(0x023D),

    /**
     * [574] LL initiated a connection but the connection has failed to be established. Controller
     * did not receive any packets from remote end.
     */
    CONNECTION_FAILED_TO_BE_ESTABLISHED(0x023E),

    /**
     * [575] The MAC of the 802.11 AMP was requested to connect to a peer, but the connection failed.
     */
    MAC_CONNECTION_FAILED(0x023F),

    /**
     * [576] The master, at this time, is unable to make a coarse adjustment to the piconet clock,
     * using the supplied parameters. Instead the master will attempt to move the clock using clock
     * dragging.
     */
    COARSE_CLOCK_ADJUSTMENT_REJECTED(0x0240),

    /**
     * [769] The user input of passkey failed, for example, the user cancelled the operation
     */
    PASSKEY_ENTRY_FAILED(0x0301),

    /**
     * [1025] The attribute handle given was not valid on this server
     */
    INVALID_HANDLE(0x0401),

    /**
     * [1026] The attribute cannot be read
     */
    READ_NOT_PERMITTED(0x0402),

    /**
     * [1027] The attribute cannot be written
     */
    WRITE_NOT_PERMITTED(0x0403),

    /**
     * [1028] The attribute PDU was invalid
     */
    INVALID_PDU(0x0404),

    /**
     * [1029] The attribute requires authentication before it can be read or written.
     */
    INSUFFICIENT_AUTHENTICATION(0x0405),

    /**
     * [1030] Attribute Server does not support the request received from the client.
     */
    REQUEST_NOT_SUPPORTED(0x0406),

    /**
     * [1031] Offset specified was past the end of the attribute
     */
    INVALID_OFFSET(0x0407),

    /**
     * [1032] The attribute requires authorization before it can be read or written.
     */
    INSUFFICIENT_AUTHORIZATION(0x0408),

    /**
     * [1033] Too many prepare writes have been queueud
     */
    PREPARE_QUEUE_FULL(0x0409),

    /**
     * [1034] No attribute found within the given attribute handle range.
     */
    ATT_NOT_FOUND(0x040A),

    /**
     * [1035] The attribute cannot be read or written using the Read Blob Request
     */
    ATT_NOT_LONG(0x040B),

    /**
     * [1037] The attribute value length is invalid for the operation
     */
    INVALID_ATT_LENGTH(0x040D),

    /**
     * [1040] The attribute type is not a supported grouping attribute as defined by a higher layer
     * specification.
     */
    UNSUPPORTED_GROUP_TYPE(0x0410),

    /**
     * [1041] Insufficient Resources to complete the request
     */
    INSUFFICIENT_RESOURCES(0x0411),

    /**
     * [1152] Application error code defined by a higher layer specification.
     */
    APPLICATION(0x0480),

    /**
     * [1537] Service Record not found
     */
    RECORD_NOT_FOUND(0x0601),

    /**
     * [1538] Service Record with this handle already exist
     */
    RECORD_ALREADY_EXIST(0x0602),

    /**
     * [2305] File not found.
     */
    FILE_NOT_FOUND(0x0901),

    /**
     * [2561] File open failed.
     */
    FILE_OPEN_FAILED(0x0A01),

    /**
     * [2562] XML parsing failed.
     */
    XML_PARSE_FAILED(0x0A02),

    /**
     * [2563] Device connection failed.
     */
    DEVICE_CONNECTION_FAILED(0x0A03),

    /**
     * [2817] Device firmware signature verification failed.
     */
    IMAGE_SIGNATURE_VERIFICATION_FAILED(0x0B01),

    /**
     * [2818] File signature verification failed.
     */
    FILE_SIGNATURE_VERIFICATION_FAILED(0x0B02),

    /**
     * [2819] Device firmware checksum is not valid.
     */
    IMAGE_CHECKSUM_ERROR(0x0B03);

    /**
     * A mapping between the integer code and its corresponding type to
     * facilitate lookup by code.
     */
    private static Map<Integer, BgApiResponse> codeMapping;

    private int key;

    private BgApiResponse(int key) {
        this.key = key;
    }

    private static void initMapping() {
        codeMapping = new HashMap<Integer, BgApiResponse>();
        for (BgApiResponse s : values()) {
            codeMapping.put(s.key, s);
        }
    }

    /**
     * Lookup function based on the type code. Returns null if the code does not exist.
     *
     * @param bgApiResponse
     *            the code to lookup
     * @return enumeration value.
     */
    public static BgApiResponse getBgApiResponse(int bgApiResponse) {
        if (codeMapping == null) {
            initMapping();
        }

        if (codeMapping.get(bgApiResponse) == null) {
            return UNKNOWN;
        }

        return codeMapping.get(bgApiResponse);
    }

    /**
     * Returns the BlueGiga protocol defined value for this enum
     *
     * @return the BGAPI enumeration key
     */
    public int getKey() {
        return key;
    }
}
