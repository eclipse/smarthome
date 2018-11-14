/**
 * Copyright (c) 2014,2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.thing;

/**
 * {@link ThingStatusDetail} defines possible status details of a {@link ThingStatusInfo}.
 *
 * @author Stefan Bußweiler - Initial contribution, added new status details
 * @author Chris Jackson - Added GONE status
 */
public enum ThingStatusDetail {
    NONE,
    HANDLER_MISSING_ERROR,
    HANDLER_REGISTERING_ERROR,
    HANDLER_INITIALIZING_ERROR,
    HANDLER_CONFIGURATION_PENDING,
    CONFIGURATION_PENDING,
    COMMUNICATION_ERROR,
    CONFIGURATION_ERROR,
    BRIDGE_OFFLINE,
    FIRMWARE_UPDATING,
    DUTY_CYCLE,
    BRIDGE_UNINITIALIZED,
    /**
     * Device has been removed. Used for example when the device has been removed from its bridge and
     * the thing handler should be removed.
     */
    GONE,
    DISABLED,

    /**
     * A device which can be reached by the handler and is ONLINE while it can not receive or respond to command sent to
     * it. A vendor application might set/unset this status.
     * E.g. a speaker which is paired as a slave in a stereo pair and will not react to commands. But it will send
     * updates about its pairing status or even updates on channel states. Only the vendor application can unpair the
     * speaker to make it ONLINE/NONE again.
     *
     */
    DEACTIVATED;

    public static final UninitializedStatus UNINITIALIZED = new UninitializedStatus();
    public static final NoneOnlyStatus INITIALIZING = new NoneOnlyStatus();
    public static final NoneOnlyStatus UNKNOWN = new NoneOnlyStatus();
    public static final OnlineStatus ONLINE = new OnlineStatus();
    public static final OfflineStatus OFFLINE = new OfflineStatus();
    public static final NoneOnlyStatus REMOVING = new NoneOnlyStatus();
    public static final NoneOnlyStatus REMOVED = new NoneOnlyStatus();

    public static final class NoneOnlyStatus {
        private NoneOnlyStatus() {
        }

        public ThingStatusDetail NONE = ThingStatusDetail.NONE;
    }

    public static final class UninitializedStatus {
        private UninitializedStatus() {
        }

        public ThingStatusDetail NONE = ThingStatusDetail.NONE;
        public ThingStatusDetail HANDLER_MISSING_ERROR = ThingStatusDetail.HANDLER_MISSING_ERROR;
        public ThingStatusDetail HANDLER_REGISTERING_ERROR = ThingStatusDetail.HANDLER_REGISTERING_ERROR;
        public ThingStatusDetail HANDLER_CONFIGURATION_PENDING = ThingStatusDetail.HANDLER_CONFIGURATION_PENDING;
        public ThingStatusDetail HANDLER_INITIALIZING_ERROR = ThingStatusDetail.HANDLER_INITIALIZING_ERROR;
        public ThingStatusDetail BRIDGE_UNINITIALIZED = ThingStatusDetail.BRIDGE_UNINITIALIZED;
    };

    public static final class OnlineStatus {
        private OnlineStatus() {
        }

        public ThingStatusDetail NONE = ThingStatusDetail.NONE;
        public ThingStatusDetail CONFIGURATION_PENDING = ThingStatusDetail.CONFIGURATION_PENDING;
        public ThingStatusDetail DEACTIVATED = ThingStatusDetail.DEACTIVATED;
    };

    public static final class OfflineStatus {
        private OfflineStatus() {
        }

        public ThingStatusDetail NONE = ThingStatusDetail.NONE;
        public ThingStatusDetail COMMUNICATION_ERROR = ThingStatusDetail.COMMUNICATION_ERROR;
        public ThingStatusDetail CONFIGURATION_ERROR = ThingStatusDetail.CONFIGURATION_ERROR;
        public ThingStatusDetail BRIDGE_OFFLINE = ThingStatusDetail.BRIDGE_OFFLINE;
        public ThingStatusDetail FIRMWARE_UPDATING = ThingStatusDetail.FIRMWARE_UPDATING;
        public ThingStatusDetail DUTY_CYCLE = ThingStatusDetail.DUTY_CYCLE;
        public ThingStatusDetail GONE = ThingStatusDetail.GONE;
    };

}
