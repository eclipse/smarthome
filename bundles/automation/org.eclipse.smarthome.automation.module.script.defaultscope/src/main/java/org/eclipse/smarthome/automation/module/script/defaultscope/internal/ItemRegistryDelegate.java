/**
 * Copyright (c) 1997, 2015 by ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.automation.module.script.defaultscope.internal;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.types.State;

/**
 * This is a helper class that can be added to script scopes. It provides easy access to the current item states.
 *
 * @author Kai Kreuzer - Initial contribution
 *
 */
@NonNullByDefault
public class ItemRegistryDelegate implements Map<String, State> {

    private final ItemRegistry itemRegistry;

    public ItemRegistryDelegate(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    @Override
    public int size() {
        return itemRegistry.getAll().size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
        if (key instanceof String) {
            try {
                return itemRegistry.getItem((String) key) != null;
            } catch (ItemNotFoundException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
        return false;
    }

    @Override
    public @Nullable State get(@Nullable Object key) {
        final Item item = itemRegistry.get((String) key);
        if (item == null) {
            return null;
        }
        return item.getState();
    }

    @Override
    public @Nullable State put(String key, State value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable State remove(@Nullable Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends State> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> keySet() {
        Set<String> keys = new HashSet<>();
        for (Item item : itemRegistry.getAll()) {
            keys.add(item.getName());
        }
        return keys;
    }

    @Override
    public Collection<State> values() {
        Set<State> values = new HashSet<>();
        for (Item item : itemRegistry.getAll()) {
            values.add(item.getState());
        }
        return values;
    }

    @Override
    public Set<java.util.Map.Entry<String, State>> entrySet() {
        Set<Map.Entry<String, State>> entries = new HashSet<Map.Entry<String, State>>();
        for (Item item : itemRegistry.getAll()) {
            entries.add(new AbstractMap.SimpleEntry<>(item.getName(), item.getState()));
        }
        return entries;
    }

}
