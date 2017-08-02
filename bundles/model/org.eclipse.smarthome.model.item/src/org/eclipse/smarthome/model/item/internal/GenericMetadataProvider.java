/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.model.item.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.codegen.ecore.templates.edit.ItemProvider;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.common.registry.AbstractProvider;
import org.eclipse.smarthome.core.items.Metadata;
import org.eclipse.smarthome.core.items.MetadataKey;
import org.eclipse.smarthome.core.items.MetadataProvider;
import org.osgi.service.component.annotations.Component;

/**
 * This class serves as a provider for all metadata that is found within item files.
 * It is filled with content by the {@link GenericItemProvider}, which cannot itself implement the
 * {@link MetadataProvider} interface as it already implements {@link ItemProvider}, which would lead to duplicate
 * methods.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
@NonNullByDefault
@Component(immediate = true, service = { MetadataProvider.class, GenericMetadataProvider.class })
public class GenericMetadataProvider extends AbstractProvider<Metadata> implements MetadataProvider {

    Set<Metadata> metadata = new HashSet<>();

    /**
     * Adds metadata to this provider
     *
     * @param bindingType
     * @param itemName
     * @param configuration
     */
    public void addMetadata(String bindingType, String itemName, String value, Map<String, Object> configuration) {
        MetadataKey key = new MetadataKey(bindingType, itemName);
        Metadata md = new Metadata(key, value, configuration);
        metadata.add(md);
        notifyListenersAboutAddedElement(md);
    }

    /**
     * Removes all meta-data for a given item name
     *
     * @param itemName
     */
    public void removeMetadata(String itemName) {
        metadata = metadata.stream().filter(md -> md.getUID().getItemName().equals(itemName))
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<Metadata> getAll() {
        return Collections.unmodifiableSet(metadata);
    }

}
