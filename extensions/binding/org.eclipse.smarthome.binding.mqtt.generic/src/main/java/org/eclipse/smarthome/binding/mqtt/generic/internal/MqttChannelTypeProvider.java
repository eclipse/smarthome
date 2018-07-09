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
package org.eclipse.smarthome.binding.mqtt.generic.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;

/**
 * If the user configures a channel and defines for example minimum/maximum values, we need a specific
 *
 * @author david
 *
 */
@NonNullByDefault
public class MqttChannelTypeProvider implements ChannelTypeProvider {
    private final Map<ChannelTypeUID, ChannelType> types = new HashMap<>();
    private final Map<ChannelGroupTypeUID, ChannelGroupType> groups = new HashMap<>();

    @Override
    public @Nullable Collection<@NonNull ChannelType> getChannelTypes(@Nullable Locale locale) {
        return types.values();
    }

    @Override
    public @Nullable ChannelType getChannelType(@NonNull ChannelTypeUID channelTypeUID, @Nullable Locale locale) {
        return types.get(channelTypeUID);
    }

    @Override
    public @Nullable ChannelGroupType getChannelGroupType(@NonNull ChannelGroupTypeUID channelGroupTypeUID,
            @Nullable Locale locale) {
        return groups.get(channelGroupTypeUID);
    }

    @Override
    public @Nullable Collection<@NonNull ChannelGroupType> getChannelGroupTypes(@Nullable Locale locale) {
        return groups.values();
    }

    public void removeChannelType(ChannelTypeUID uid) {
        types.remove(uid);
    }

    public void removeChannelGroupType(ChannelGroupTypeUID uid) {
        groups.remove(uid);
    }

    public void addChannelGroupType(ChannelGroupTypeUID uid, ChannelGroupType type) {
        groups.put(uid, type);
    }

    public void addChannelGroupType(ChannelGroupTypeUID uid, String label) {
        String description = "";
        boolean advanced = false;
        List<ChannelDefinition> channelDefinitions = Collections.emptyList();
        ChannelGroupType type = new ChannelGroupType(uid, advanced, label, description, null, channelDefinitions);
        groups.put(uid, type);
    }

    public void addChannelType(ChannelTypeUID uid, ChannelType type) {
        types.put(uid, type);
    }
}
