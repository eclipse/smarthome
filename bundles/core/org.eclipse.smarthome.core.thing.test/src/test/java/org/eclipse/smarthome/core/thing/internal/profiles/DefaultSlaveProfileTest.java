package org.eclipse.smarthome.core.thing.internal.profiles;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.items.events.ItemCommandEvent;
import org.eclipse.smarthome.core.items.events.ItemStateEvent;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.link.ItemChannelLink;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class DefaultSlaveProfileTest {

    private static final String TEST_ITEM = "testItem";
    private static final ThingUID THING_UID = new ThingUID("test", "thing");
    private static final ChannelUID CHANNEL_UID = new ChannelUID(THING_UID, "channel");
    private static final ItemChannelLink LINK = new ItemChannelLink(TEST_ITEM, CHANNEL_UID);

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private Thing thing;

    @Mock
    private ThingHandler handler;

    @Before
    public void setup() {
        initMocks(this);
        when(thing.getHandler()).thenReturn(handler);
    }

    @Test
    public void testOnUpdate() {
        DefaultSlaveProfile profile = new DefaultSlaveProfile();

        when(thing.getStatus()).thenReturn(ThingStatus.ONLINE);

        profile.onUpdate(LINK, thing, OnOffType.ON);

        verify(handler).handleCommand(same(CHANNEL_UID), eq(OnOffType.ON));
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void testOnUpdate_notInitialized() {
        DefaultSlaveProfile profile = new DefaultSlaveProfile();

        when(thing.getStatus()).thenReturn(ThingStatus.UNINITIALIZED);
        profile.onUpdate(LINK, thing, OnOffType.ON);
        when(thing.getStatus()).thenReturn(ThingStatus.INITIALIZING);
        profile.onUpdate(LINK, thing, OnOffType.ON);
        when(thing.getStatus()).thenReturn(ThingStatus.REMOVING);
        profile.onUpdate(LINK, thing, OnOffType.ON);
        when(thing.getStatus()).thenReturn(ThingStatus.REMOVED);
        profile.onUpdate(LINK, thing, OnOffType.ON);

        verifyNoMoreInteractions(handler);
    }

    @Test
    public void testOnUpdate_noHandler() {
        DefaultSlaveProfile profile = new DefaultSlaveProfile();
        when(thing.getHandler()).thenReturn(null);

        profile.onUpdate(LINK, thing, OnOffType.ON);

        verifyNoMoreInteractions(handler);
    }

    @Test
    public void testOnCommand() {
        DefaultSlaveProfile profile = new DefaultSlaveProfile();

        when(thing.getStatus()).then(invocation -> ThingStatus.ONLINE);

        profile.onCommand(LINK, thing, OnOffType.ON);

        verifyNoMoreInteractions(handler);
    }

    @Test
    public void testStateUpdated() {
        DefaultSlaveProfile profile = new DefaultSlaveProfile();
        ItemChannelLink link = new ItemChannelLink(TEST_ITEM, CHANNEL_UID);
        SwitchItem item = new SwitchItem(TEST_ITEM);
        ArgumentCaptor<ItemStateEvent> eventCaptor = ArgumentCaptor.forClass(ItemStateEvent.class);

        profile.stateUpdated(eventPublisher, link, OnOffType.ON, item);

        verifyNoMoreInteractions(eventPublisher);
        verifyNoMoreInteractions(thing);
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void testPostCommand() {
        DefaultSlaveProfile profile = new DefaultSlaveProfile();
        ItemChannelLink link = new ItemChannelLink(TEST_ITEM, CHANNEL_UID);
        SwitchItem item = new SwitchItem(TEST_ITEM);
        ArgumentCaptor<ItemCommandEvent> eventCaptor = ArgumentCaptor.forClass(ItemCommandEvent.class);

        profile.postCommand(eventPublisher, link, OnOffType.ON, item);

        verifyNoMoreInteractions(eventPublisher);
        verifyNoMoreInteractions(thing);
        verifyNoMoreInteractions(handler);
    }

}
