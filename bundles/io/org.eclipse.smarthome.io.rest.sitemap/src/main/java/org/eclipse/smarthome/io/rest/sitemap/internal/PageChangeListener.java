/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.io.rest.sitemap.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.StateChangeListener;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.io.rest.core.item.EnrichedItemDTOMapper;
import org.eclipse.smarthome.io.rest.sitemap.SitemapSubscriptionService.SitemapSubscriptionCallback;
import org.eclipse.smarthome.model.sitemap.Frame;
import org.eclipse.smarthome.model.sitemap.Widget;
import org.eclipse.smarthome.ui.items.ItemUIRegistry;

/**
 * This is a class that listens on item state change events and creates sitemap events for a dedicated sitemap page.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
public class PageChangeListener implements StateChangeListener {

    private final String sitemapName;
    private final String pageId;
    private final ItemUIRegistry itemUIRegistry;
    private final EList<Widget> widgets;
    private final Set<GenericItem> items;
    private final SitemapSubscriptionCallback callback;

    /**
     * Creates a new instance.
     *
     * @param sitemapName the sitemap name of the page
     * @param pageId the id of the page for which events are created
     * @param itemUIRegistry the ItemUIRegistry which is needed for the functionality
     * @param widgets the list of widgets that are part of the page.
     * @param callback the instance that should receive the created sitemap events
     */
    public PageChangeListener(String sitemapName, String pageId, ItemUIRegistry itemUIRegistry, EList<Widget> widgets,
            SitemapSubscriptionCallback callback) {
        this.sitemapName = sitemapName;
        this.pageId = pageId;
        this.itemUIRegistry = itemUIRegistry;
        this.widgets = widgets;
        this.callback = callback;
        items = getAllItems(widgets);
        for (GenericItem item : items) {
            item.addStateChangeListener(this);
        }
    }

    /**
     * Disposes this instance and releases all resources.
     */
    public void dispose() {
        for (GenericItem item : items) {
            item.removeStateChangeListener(this);
        }
    }

    /**
     * Collects all items that are represented by a given list of widgets
     *
     * @param widgets
     *            the widget list to get the items for added to all bundles containing REST resources
     * @return all items that are represented by the list of widgets
     */
    private Set<GenericItem> getAllItems(EList<Widget> widgets) {
        Set<GenericItem> items = new HashSet<GenericItem>();
        if (itemUIRegistry != null) {
            for (Widget widget : widgets) {
                String itemName = widget.getItem();
                if (itemName != null) {
                    try {
                        Item item = itemUIRegistry.getItem(itemName);
                        if (item instanceof GenericItem) {
                            final GenericItem gItem = (GenericItem) item;
                            items.add(gItem);
                        }
                    } catch (ItemNotFoundException e) {
                        // ignore
                    }
                } else {
                    if (widget instanceof Frame) {
                        items.addAll(getAllItems(((Frame) widget).getChildren()));
                    }
                }
            }
        }
        return items;
    }

    @Override
    public void stateChanged(Item item, State oldState, State newState) {
        Set<SitemapEvent> events = constructSitemapEvents(item, oldState, newState, widgets);
        for (SitemapEvent event : events) {
            callback.onEvent(event);
        }
    }

    @Override
    public void stateUpdated(Item item, State state) {
    }

    private Set<SitemapEvent> constructSitemapEvents(Item item, State oldState, State newState, List<Widget> widgets) {
        Set<SitemapEvent> events = new HashSet<>();
        for (Widget w : widgets) {
            if (w instanceof Frame) {
                events.addAll(constructSitemapEvents(item, oldState, newState, ((Frame) w).getChildren()));
            } else {
                if (w.getItem() != null && w.getItem().equals(item.getName())) {
                    SitemapWidgetEvent event = new SitemapWidgetEvent();
                    event.sitemapName = sitemapName;
                    event.pageId = pageId;
                    event.label = itemUIRegistry.getLabel(w);
                    event.labelcolor = itemUIRegistry.getLabelColor(w);
                    event.valuecolor = itemUIRegistry.getValueColor(w);
                    event.widgetId = itemUIRegistry.getWidgetId(w);
                    event.visibility = itemUIRegistry.getVisiblity(w);
                    event.item = EnrichedItemDTOMapper.map(item, false, null, null);
                    events.add(event);
                }
            }
        }
        return events;
    }

}
