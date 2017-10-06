/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.core.thing.type;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.types.StateDescription;

/**
 * The {@link DynamicStateDescriptionProvider} is responsible for providing {@link StateDescription} for a channel
 * dynamically in the runtime.
 *
 * @author Pawel Pieczul - initial contribution
 *
 */
@NonNullByDefault
public interface DynamicStateDescriptionProvider {
    /**
     * For a given channel UID, return a {@link StateDescription} that should be used for the channel, instead of the
     * one defined statically in the {@link ChannelType}.
     *
     * For a particular channel, there should be only one provider of the dynamic state description. When more than one
     * description is provided for the same channel (by different providers), only one will be used, from the provider
     * that registered first.
     *
     * @param channelUID channel UID
     * @param locale locale (can be null)
     * @return state description or null if none provided
     */
    @Nullable
    StateDescription getStateDescription(ChannelUID channelUID, @Nullable Locale locale);
}
