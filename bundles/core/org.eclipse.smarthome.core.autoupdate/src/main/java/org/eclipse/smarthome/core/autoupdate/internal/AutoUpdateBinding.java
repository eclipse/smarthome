/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.core.autoupdate.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.smarthome.core.autoupdate.AutoUpdateBindingConfigProvider;
import org.eclipse.smarthome.core.autoupdate.AutoUpdateConfigurator;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.events.AbstractItemEventSubscriber;
import org.eclipse.smarthome.core.items.events.ItemCommandEvent;
import org.eclipse.smarthome.core.items.events.ItemEventFactory;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * The AutoUpdate-Binding is no 'normal' binding as it doesn't connect any hardware to the Eclipse SmartHome system. In
 * fact it takes care of updating the State of an item with respect to the received command automatically or not. By
 * default the State is getting updated automatically which is desired behavior in most of the cases. However it could
 * be useful to disable this default behavior.
 *
 * <p>
 * For example when implementing validation steps before changing a State one needs to control the State update oneself.
 *
 * @author Thomas.Eichstaedt-Engelen - Initial contribution
 * @author Kai Kreuzer - added sending real events
 * @author Stefan Bußweiler - Migration to new ESH event concept
 * @author Markus Rathgeb - Add support for auto-update configurator interface
 */
public class AutoUpdateBinding extends AbstractItemEventSubscriber implements AutoUpdateConfigurator {

    private final Map<ChannelUID, Boolean> BINDING_GLOBAL_TRUE = new HashMap<>();
    private final Map<ChannelUID, Boolean> BINDING_GLOBAL_FALSE = new HashMap<>();

    private final Logger logger = LoggerFactory.getLogger(AutoUpdateBinding.class);

    protected volatile ItemRegistry itemRegistry;
    protected volatile ItemChannelLinkRegistry itemChannelLinkRegistry;

    /** to keep track of all binding config providers */
    protected Collection<AutoUpdateBindingConfigProvider> providers = new CopyOnWriteArraySet<>();

    // Binding specific settings
    protected Map<String, Map<ChannelUID, Boolean>> bindingSpecificSettings = new ConcurrentHashMap<>();

    protected EventPublisher eventPublisher = null;

    public void addBindingConfigProvider(AutoUpdateBindingConfigProvider provider) {
        providers.add(provider);
    }

    public void removeBindingConfigProvider(AutoUpdateBindingConfigProvider provider) {
        providers.remove(provider);
    }

    public void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    public void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    public void setItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
    }

    public void unsetItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = null;
    }

    @Override
    public void addAutoUpdateByBindingIdConfig(String bindingId, boolean autoUpdate)
            throws IllegalArgumentException, IllegalStateException {
        final Map<ChannelUID, Boolean> settings = bindingSpecificSettings.get(bindingId);
        if (settings == null) {
            bindingSpecificSettings.put(bindingId, autoUpdate ? BINDING_GLOBAL_TRUE : BINDING_GLOBAL_FALSE);
        } else {
            if (settings == BINDING_GLOBAL_TRUE || settings == BINDING_GLOBAL_FALSE) {
                throw new IllegalArgumentException(
                        String.format("There is already an configuration for given binding ID '%s'.", bindingId));
            } else {
                throw new IllegalStateException(String.format(
                        "There is already a channel specific configuration for the given binding ID '%s'.", bindingId));
            }
        }
    }

    @Override
    public void removeAutoUpdateByBindingIdConfig(String bindingId) throws IllegalArgumentException {
        final Map<ChannelUID, Boolean> settings = bindingSpecificSettings.get(bindingId);
        if (settings == null || (settings != BINDING_GLOBAL_TRUE && settings != BINDING_GLOBAL_FALSE)) {
            throw new IllegalArgumentException(
                    String.format("There is no configuration for given binding ID '%s'.", bindingId));
        } else {
            settings.remove(bindingId);
        }
    }

    @Override
    public void addAutoUpdateByChannelUidConfig(ChannelUID channelUid, boolean autoUpdate)
            throws IllegalArgumentException, IllegalStateException {
        final String bindingId = channelUid.getBindingId();
        final Map<ChannelUID, Boolean> settings = bindingSpecificSettings.get(bindingId);
        if (settings == null) {
            final Map<ChannelUID, Boolean> newSettings = new ConcurrentHashMap<>();
            newSettings.put(channelUid, autoUpdate);
            bindingSpecificSettings.put(bindingId, newSettings);
        } else {
            if (settings == BINDING_GLOBAL_TRUE || settings == BINDING_GLOBAL_FALSE) {
                throw new IllegalArgumentException(
                        String.format("There is already an configuration for given binding ID '%s'.", bindingId));
            } else {
                if (settings.containsKey(channelUid)) {
                    throw new IllegalStateException(String.format(
                            "There is already a channel specific configuration for the given binding ID '%s'.",
                            bindingId));
                } else {
                    settings.put(channelUid, autoUpdate);
                }
            }
        }
    }

    @Override
    public void removeAutoUpdateByChannelUidConfig(ChannelUID channelUid) throws IllegalArgumentException {
        final Map<ChannelUID, Boolean> settings = bindingSpecificSettings.get(channelUid.getBindingId());
        if (settings == null || !settings.containsKey(channelUid)) {
            throw new IllegalArgumentException(
                    String.format("There is no configuration for given channel UID '%s'.", channelUid));
        } else {
            settings.remove(channelUid);
        }
    }

    /**
     * Handle the received command event.
     *
     * <p>
     * If the command could be converted to a {@link State} the auto-update configurations are inspected.
     * If there is at least one configuration that enable the auto-update, auto-update is applied.
     * If there is no configuration provided at all the autoupdate defaults to {@code true} and an update is posted for
     * the corresponding {@link State}.
     *
     * @param commandEvent the command event
     */
    @Override
    protected void receiveCommand(ItemCommandEvent commandEvent) {
        final Command command = commandEvent.getItemCommand();
        if (command instanceof State) {
            final State state = (State) command;

            final String itemName = commandEvent.getItemName();

            Boolean autoUpdate = autoUpdate(itemName);

            // we didn't find any autoupdate configuration, so apply the default now
            if (autoUpdate == null) {
                autoUpdate = Boolean.TRUE;
            }

            if (autoUpdate) {
                postUpdate(itemName, state);
            } else {
                logger.trace("Won't update item '{}' as it is not configured to update its state automatically.",
                        itemName);
            }
        }
    }

    /**
     * Check auto update configuration(s) for given item name.
     *
     * @param itemName the name of the item
     * @return true if auto-update is enabled, false if auto-update is disabled, null if there is no explicit
     *         configuration
     */
    private Boolean autoUpdate(final String itemName) {
        Boolean autoUpdate = null;

        // Check auto update by configuration for binding ID
        final ItemChannelLinkRegistry itemChannelLinkRegistry = this.itemChannelLinkRegistry;
        if (itemChannelLinkRegistry != null) {
            final Set<ChannelUID> channelUIDs = itemChannelLinkRegistry.getBoundChannels(itemName);

            for (final ChannelUID channelUID : channelUIDs) {
                final Boolean au;

                final Map<ChannelUID, Boolean> settings = bindingSpecificSettings.get(channelUID.getBindingId());
                if (settings == null) {
                    au = null;
                } else {
                    if (settings == BINDING_GLOBAL_TRUE) {
                        au = true;
                    } else if (settings == BINDING_GLOBAL_FALSE) {
                        au = false;
                    } else {
                        final Boolean channelSettings = settings.get(channelUID);
                        if (channelSettings == null) {
                            au = null;
                        } else if (channelSettings) {
                            au = true;
                        } else {
                            au = false;
                        }
                    }
                }

                // Now 'au' is set and we need to handle the different options: unset, set to true, set to false
                if (au == null) {
                    // There is no setting for the binding ID, keep value unchanged.
                    continue;
                } else {
                    // There is an entry for the binding ID.
                    if (au) {
                        // setting is true, we could stop.
                        return true;
                    } else {
                        // setting is false, set it if autoupdate is not set
                        if (autoUpdate == null) {
                            autoUpdate = false;
                        }
                    }
                }
            }
        }

        // Check auto update by configuration for item name
        for (AutoUpdateBindingConfigProvider provider : providers) {
            Boolean au = provider.autoUpdate(itemName);
            if (au == null) {
                // There is no setting for the item, keep value unchanged.
                continue;
            } else {
                // There is an entry for the item.
                if (au) {
                    // setting is true, we could stop.
                    return true;
                } else {
                    // setting is false, set it if autoupdate is not set
                    if (autoUpdate == null) {
                        autoUpdate = false;
                    }
                }
            }
        }

        return autoUpdate;
    }

    private void postUpdate(String itemName, State newState) {
        if (itemRegistry != null) {
            try {
                GenericItem item = (GenericItem) itemRegistry.getItem(itemName);
                boolean isAccepted = false;
                if (item.getAcceptedDataTypes().contains(newState.getClass())) {
                    isAccepted = true;
                } else {
                    // Look for class hierarchy
                    for (Class<? extends State> state : item.getAcceptedDataTypes()) {
                        try {
                            if (!state.isEnum()
                                    && state.newInstance().getClass().isAssignableFrom(newState.getClass())) {
                                isAccepted = true;
                                break;
                            }
                        } catch (InstantiationException e) {
                            logger.warn("InstantiationException on {}", e.getMessage(), e); // Should never happen
                        } catch (IllegalAccessException e) {
                            logger.warn("IllegalAccessException on {}", e.getMessage(), e); // Should never happen
                        }
                    }
                }
                if (isAccepted) {
                    eventPublisher.post(ItemEventFactory.createStateEvent(itemName, newState,
                            "org.eclipse.smarthome.core.autoupdate"));
                } else {
                    logger.debug("Received update of a not accepted type ({}) for item {}",
                            newState.getClass().getSimpleName(), itemName);
                }
            } catch (ItemNotFoundException e) {
                logger.debug("Received update for non-existing item: {}", e.getMessage());
            }
        }
    }

}
