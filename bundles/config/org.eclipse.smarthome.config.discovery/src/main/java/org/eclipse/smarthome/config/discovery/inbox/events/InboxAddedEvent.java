/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
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
package org.eclipse.smarthome.config.discovery.inbox.events;

import org.eclipse.smarthome.config.discovery.dto.DiscoveryResultDTO;

/**
 * An {@link InboxAddedEvent} notifies subscribers that a discovery result has been added to the inbox.
 * Inbox added events must be created with the {@link InboxEventFactory}.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
public class InboxAddedEvent extends AbstractInboxEvent {

    /**
     * The inbox added event type.
     */
    public static final String TYPE = InboxAddedEvent.class.getSimpleName();

    /**
     * Constructs a new inbox added event object.
     *
     * @param topic the topic
     * @param payload the payload
     * @param discoveryResult the discovery result data transfer object
     */
    InboxAddedEvent(String topic, String payload, DiscoveryResultDTO discoveryResult) {
        super(topic, payload, discoveryResult);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "Discovery Result with UID '" + getDiscoveryResult().thingUID + "' has been added.";
    }
}
