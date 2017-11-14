/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.core.thing.internal;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.events.ItemEventFactory;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.events.ThingEventFactory;
import org.eclipse.smarthome.core.thing.internal.profiles.SystemProfileFactory;
import org.eclipse.smarthome.core.thing.link.ItemChannelLink;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.thing.profiles.ProfileAdvisor;
import org.eclipse.smarthome.core.thing.profiles.ProfileCallback;
import org.eclipse.smarthome.core.thing.profiles.ProfileFactory;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeUID;
import org.eclipse.smarthome.core.thing.profiles.StateProfile;
import org.eclipse.smarthome.core.thing.profiles.TriggerProfile;
import org.eclipse.smarthome.core.thing.type.ChannelKind;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
public class CommunicationManagerTest {

    private static final String EVENT = "event";
    private static final String ITEM_NAME_1 = "testItem1";
    private static final String ITEM_NAME_2 = "testItem2";
    private static final SwitchItem ITEM_1 = new SwitchItem(ITEM_NAME_1);
    private static final SwitchItem ITEM_2 = new SwitchItem(ITEM_NAME_2);
    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("test", "type");
    private static final ThingUID THING_UID = new ThingUID("test", "thing");
    private static final ChannelUID STATE_CHANNEL_UID_1 = new ChannelUID(THING_UID, "state-channel1");
    private static final ChannelUID STATE_CHANNEL_UID_2 = new ChannelUID(THING_UID, "state-channel2");
    private static final ChannelUID TRIGGER_CHANNEL_UID_1 = new ChannelUID(THING_UID, "trigger-channel1");
    private static final ChannelUID TRIGGER_CHANNEL_UID_2 = new ChannelUID(THING_UID, "trigger-channel2");
    private static final ItemChannelLink LINK_1_S1 = new ItemChannelLink(ITEM_NAME_1, STATE_CHANNEL_UID_1);
    private static final ItemChannelLink LINK_1_S2 = new ItemChannelLink(ITEM_NAME_1, STATE_CHANNEL_UID_2);
    private static final ItemChannelLink LINK_2_S2 = new ItemChannelLink(ITEM_NAME_2, STATE_CHANNEL_UID_2);
    private static final ItemChannelLink LINK_1_T1 = new ItemChannelLink(ITEM_NAME_1, TRIGGER_CHANNEL_UID_1);
    private static final ItemChannelLink LINK_1_T2 = new ItemChannelLink(ITEM_NAME_1, TRIGGER_CHANNEL_UID_2);
    private static final ItemChannelLink LINK_2_T2 = new ItemChannelLink(ITEM_NAME_2, TRIGGER_CHANNEL_UID_2);
    private static final Thing THING = ThingBuilder.create(THING_TYPE_UID, THING_UID)
            .withChannels(ChannelBuilder.create(STATE_CHANNEL_UID_1, "").withKind(ChannelKind.STATE).build(),
                    ChannelBuilder.create(STATE_CHANNEL_UID_2, "").withKind(ChannelKind.STATE).build(),
                    ChannelBuilder.create(TRIGGER_CHANNEL_UID_1, "").withKind(ChannelKind.TRIGGER).build(),
                    ChannelBuilder.create(TRIGGER_CHANNEL_UID_2, "").withKind(ChannelKind.TRIGGER).build())
            .build();

    private CommunicationManager manager;

    @Mock
    private ProfileFactory mockProfileFactory;

    @Mock
    private ProfileAdvisor mockProfileAdvisor;

    @Mock
    private StateProfile stateProfile;

    @Mock
    private TriggerProfile triggerProfile;

    @Mock
    private EventPublisher eventPublisher;

    @Before
    public void setup() {
        initMocks(this);

        manager = new CommunicationManager();
        manager.setEventPublisher(eventPublisher);
        manager.setDefaultProfileFactory(new SystemProfileFactory());

        doAnswer(invocation -> {
            switch (((Channel) invocation.getArguments()[0]).getKind()) {
                case STATE:
                    return new ProfileTypeUID("test:state");
                case TRIGGER:
                    return new ProfileTypeUID("test:trigger");
            }
            return null;
        }).when(mockProfileAdvisor).getSuggestedProfileTypeUID(isA(Channel.class), isA(String.class));
        doAnswer(invocation -> {
            switch (((ProfileTypeUID) invocation.getArguments()[0]).toString()) {
                case "test:state":
                    return stateProfile;
                case "test:trigger":
                    return triggerProfile;
            }
            return null;
        }).when(mockProfileFactory).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                isA(Configuration.class));

        when(mockProfileFactory.getSupportedProfileTypeUIDs()).thenReturn(Stream
                .of(new ProfileTypeUID("test:state"), new ProfileTypeUID("test:trigger")).collect(Collectors.toList()));

        manager.addProfileFactory(mockProfileFactory);
        manager.addProfileAdvisor(mockProfileAdvisor);

        ItemChannelLinkRegistry iclRegistry = new ItemChannelLinkRegistry() {
            @Override
            public Stream<ItemChannelLink> stream() {
                return Arrays.asList(LINK_1_S1, LINK_1_S2, LINK_2_S2, LINK_1_T1, LINK_1_T2, LINK_2_T2).stream();
            }
        };
        manager.setItemChannelLinkRegistry(iclRegistry);

        ItemRegistry itemRegistry = mock(ItemRegistry.class);
        when(itemRegistry.get(eq(ITEM_NAME_1))).thenReturn(ITEM_1);
        when(itemRegistry.get(eq(ITEM_NAME_2))).thenReturn(ITEM_2);
        manager.setItemRegistry(itemRegistry);

        ThingRegistry thingRegistry = mock(ThingRegistry.class);
        when(thingRegistry.get(eq(THING_UID))).thenReturn(THING);
        manager.setThingRegistry(thingRegistry);
    }

    @Test
    public void testStateUpdated_singleLink() {
        manager.stateUpdated(STATE_CHANNEL_UID_1, OnOffType.ON);
        verify(stateProfile).onStateUpdateFromHandler(eq(OnOffType.ON));
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testStateUpdated_multiLink() {
        manager.stateUpdated(STATE_CHANNEL_UID_2, OnOffType.ON);
        verify(stateProfile, times(2)).onStateUpdateFromHandler(eq(OnOffType.ON));
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testPostCommand_singleLink() {
        manager.postCommand(STATE_CHANNEL_UID_1, OnOffType.ON);
        verify(stateProfile).onCommandFromHandler(eq(OnOffType.ON));
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testPostCommand_multiLink() {
        manager.postCommand(STATE_CHANNEL_UID_2, OnOffType.ON);
        verify(stateProfile, times(2)).onCommandFromHandler(eq(OnOffType.ON));
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testItemCommandEvent_singleLink() {
        manager.receive(ItemEventFactory.createCommandEvent(ITEM_NAME_2, OnOffType.ON));
        verify(stateProfile).onCommandFromItem(eq(OnOffType.ON));
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testItemCommandEvent_multiLink() {
        manager.receive(ItemEventFactory.createCommandEvent(ITEM_NAME_1, OnOffType.ON));
        verify(stateProfile, times(2)).onCommandFromItem(eq(OnOffType.ON));
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testItemCommandEvent_notToSource() {
        manager.receive(
                ItemEventFactory.createCommandEvent(ITEM_NAME_1, OnOffType.ON, STATE_CHANNEL_UID_2.getAsString()));
        verify(stateProfile).onCommandFromItem(eq(OnOffType.ON));
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testItemStateEvent_singleLink() {
        manager.receive(ItemEventFactory.createStateEvent(ITEM_NAME_2, OnOffType.ON));
        verify(stateProfile).onStateUpdateFromItem(eq(OnOffType.ON));
        verify(triggerProfile).onStateUpdateFromItem(eq(OnOffType.ON));
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testItemStateEvent_multiLink() {
        manager.receive(ItemEventFactory.createStateEvent(ITEM_NAME_1, OnOffType.ON));
        verify(stateProfile, times(2)).onStateUpdateFromItem(eq(OnOffType.ON));
        verify(triggerProfile, times(2)).onStateUpdateFromItem(eq(OnOffType.ON));
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testItemStateEvent_notToSource() {
        manager.receive(
                ItemEventFactory.createStateEvent(ITEM_NAME_1, OnOffType.ON, STATE_CHANNEL_UID_2.getAsString()));
        verify(stateProfile).onStateUpdateFromItem(eq(OnOffType.ON));
        verify(triggerProfile, times(2)).onStateUpdateFromItem(eq(OnOffType.ON));
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testChannelTriggeredEvent_singleLink() {
        manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_1));
        verify(triggerProfile).onTriggerFromHandler(eq(EVENT));
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testChannelTriggeredEvent_multiLink() {
        manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        verify(triggerProfile, times(2)).onTriggerFromHandler(eq(EVENT));
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testProfileIsReused() {
        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }

        verify(mockProfileFactory, times(2)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                isA(Configuration.class));
        verify(mockProfileFactory, atLeast(0)).getSupportedProfileTypeUIDs();
        verify(mockProfileAdvisor, atLeast(0)).getSuggestedProfileTypeUID(any(), any());
        verifyNoMoreInteractions(mockProfileFactory);
        verifyNoMoreInteractions(mockProfileAdvisor);
    }

    @Test
    public void testProfileIsNotReusedOnFactoryChange() {
        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }
        verify(mockProfileFactory, times(2)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                isA(Configuration.class));

        manager.removeProfileFactory(mockProfileFactory);
        manager.addProfileFactory(mockProfileFactory);

        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }

        verify(mockProfileFactory, times(4)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                isA(Configuration.class));
        verify(mockProfileFactory, atLeast(0)).getSupportedProfileTypeUIDs();
        verify(mockProfileAdvisor, atLeast(0)).getSuggestedProfileTypeUID(any(), any());
        verifyNoMoreInteractions(mockProfileFactory);
        verifyNoMoreInteractions(mockProfileAdvisor);
    }

    @Test
    public void testProfileIsNotReusedOnLinkChange() {
        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }
        verify(mockProfileFactory, times(2)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                isA(Configuration.class));

        manager.removed(LINK_2_T2);
        manager.added(LINK_2_T2);

        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }

        verify(mockProfileFactory, times(3)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                isA(Configuration.class));
        verify(mockProfileFactory, atLeast(0)).getSupportedProfileTypeUIDs();
        verify(mockProfileAdvisor, atLeast(0)).getSuggestedProfileTypeUID(any(), any());
        verifyNoMoreInteractions(mockProfileFactory);
        verifyNoMoreInteractions(mockProfileAdvisor);
    }

    @Test
    public void testProfileIsReusedOnUnrelatedLinkChange() {
        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }

        manager.removed(LINK_1_S1);
        manager.added(LINK_1_S1);

        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }

        verify(mockProfileFactory, times(2)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                isA(Configuration.class));
        verify(mockProfileFactory, atLeast(0)).getSupportedProfileTypeUIDs();
        verify(mockProfileAdvisor, atLeast(0)).getSuggestedProfileTypeUID(any(), any());
        verifyNoMoreInteractions(mockProfileFactory);
        verifyNoMoreInteractions(mockProfileAdvisor);
    }

    @Test
    public void testProfileIsNotReusedOnLinkUpdate() {
        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }
        verify(mockProfileFactory, times(2)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                isA(Configuration.class));

        manager.updated(LINK_2_T2, LINK_2_T2);

        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }

        verify(mockProfileFactory, times(3)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                isA(Configuration.class));
        verify(mockProfileFactory, atLeast(0)).getSupportedProfileTypeUIDs();
        verify(mockProfileAdvisor, atLeast(0)).getSuggestedProfileTypeUID(any(), any());
        verifyNoMoreInteractions(mockProfileFactory);
        verifyNoMoreInteractions(mockProfileAdvisor);
    }

}
