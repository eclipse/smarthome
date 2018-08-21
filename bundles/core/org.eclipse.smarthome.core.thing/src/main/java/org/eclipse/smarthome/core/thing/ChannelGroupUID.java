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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * {@link ChannelGroupUID} represents a unique identifier for channel groups.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class ChannelGroupUID extends UID {

    private static final String CHANNEL_GROUP_SEGMENT_PATTERN = "[\\w-]*#";
    private static final String CHANNEL_GROUP_SEPERATOR = "#";

    /**
     * Default constructor in package scope only. Will allow to instantiate this
     * class by reflection. Not intended to be used for normal instantiation.
     */
    ChannelGroupUID() {
        super();
    }

    public ChannelGroupUID(String channelGroupUid) {
        super(channelGroupUid);
    }

    /**
     * @param thingUID the unique identifier of the thing the channel belongs to
     * @param id the channel group's id
     */
    public ChannelGroupUID(ThingUID thingUID, String id) {
        super(toSegments(thingUID, id));
    }

    private static List<String> toSegments(ThingUID thingUID, String id) {
        List<String> ret = new ArrayList<>(thingUID.getAllSegments());
        ret.add(id + CHANNEL_GROUP_SEPERATOR);
        return ret;
    }

    /**
     * Returns the id.
     *
     * @return id
     */
    public String getId() {
        List<String> segments = getAllSegments();
        return segments.get(segments.size() - 1).replaceAll(CHANNEL_GROUP_SEPERATOR, "");
    }

    @Override
    protected int getMinimalNumberOfSegments() {
        return 4;
    }

    @Override
    protected void validateSegment(String segment, int index, int length) {
        if (index < length - 1) {
            super.validateSegment(segment, index, length);
        } else {
            if (!segment.matches(CHANNEL_GROUP_SEGMENT_PATTERN)) {
                throw new IllegalArgumentException(String.format(
                        "UID segment '%s' contains invalid characters. The last segment of the channel UID must match the pattern '%s'.",
                        segment, CHANNEL_GROUP_SEGMENT_PATTERN));
            }
        }
    }

    /**
     * Returns the thing UID
     *
     * @return the thing UID
     */
    public ThingUID getThingUID() {
        List<String> allSegments = getAllSegments();
        return new ThingUID(allSegments.subList(0, allSegments.size() - 1).toArray(new String[allSegments.size() - 1]));
    }
}
