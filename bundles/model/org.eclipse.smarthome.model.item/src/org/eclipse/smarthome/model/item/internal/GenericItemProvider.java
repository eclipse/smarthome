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
package org.eclipse.smarthome.model.item.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.common.registry.AbstractProvider;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.GroupFunction;
import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemFactory;
import org.eclipse.smarthome.core.items.ItemProvider;
import org.eclipse.smarthome.core.items.dto.GroupFunctionDTO;
import org.eclipse.smarthome.core.items.dto.ItemDTOMapper;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateDescriptionProvider;
import org.eclipse.smarthome.model.core.EventType;
import org.eclipse.smarthome.model.core.ModelRepository;
import org.eclipse.smarthome.model.core.ModelRepositoryChangeListener;
import org.eclipse.smarthome.model.item.BindingConfigParseException;
import org.eclipse.smarthome.model.item.BindingConfigReader;
import org.eclipse.smarthome.model.items.ItemModel;
import org.eclipse.smarthome.model.items.ModelBinding;
import org.eclipse.smarthome.model.items.ModelGroupFunction;
import org.eclipse.smarthome.model.items.ModelGroupItem;
import org.eclipse.smarthome.model.items.ModelItem;
import org.eclipse.smarthome.model.items.ModelNormalItem;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ItemProvider implementation which computes *.items file based item configurations.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Thomas.Eichstaedt-Engelen
 */
@Component(service = { ItemProvider.class, StateDescriptionProvider.class }, immediate = true)
public class GenericItemProvider extends AbstractProvider<Item>
        implements ModelRepositoryChangeListener, ItemProvider, StateDescriptionProvider {

    private final Logger logger = LoggerFactory.getLogger(GenericItemProvider.class);

    /** to keep track of all binding config readers */
    private final Map<String, BindingConfigReader> bindingConfigReaders = new HashMap<String, BindingConfigReader>();

    private ModelRepository modelRepository = null;

    private GenericMetadataProvider genericMetaDataProvider = null;

    private final Map<String, Collection<Item>> itemsMap = new ConcurrentHashMap<>();

    private final Collection<ItemFactory> itemFactorys = new ArrayList<ItemFactory>();

    private final Map<String, StateDescription> stateDescriptions = new ConcurrentHashMap<>();

    private Integer rank;
    private boolean active = false;

    protected void activate(Map<String, Object> properties) {
        Object serviceRanking = properties.get(Constants.SERVICE_RANKING);
        if (serviceRanking instanceof Integer) {
            rank = (Integer) serviceRanking;
        } else {
            rank = 0;
        }

        itemFactorys.forEach(itemFactory -> dispatchBindingsPerItemType(null, itemFactory.getSupportedItemTypes()));

        // process models which are already parsed by modelRepository:
        for (String modelName : modelRepository.getAllModelNamesOfType("items")) {
            modelChanged(modelName, EventType.ADDED);
        }
        modelRepository.addModelRepositoryChangeListener(this);

        active = true;
    }

    protected void deactivate() {
        active = false;
        modelRepository.removeModelRepositoryChangeListener(this);
    }

    @Override
    public Integer getRank() {
        return rank;
    }

    @Reference()
    public void setModelRepository(ModelRepository modelRepository) {
        this.modelRepository = modelRepository;
    }

    public void unsetModelRepository(ModelRepository modelRepository) {
        this.modelRepository = null;
    }

    protected void setGenericMetaDataProvider(GenericMetadataProvider genericMetaDataProvider) {
        this.genericMetaDataProvider = genericMetaDataProvider;
    }

    protected void unsetGenericMetaDataProvider(GenericMetadataProvider genericMetaDataProvider) {
        this.genericMetaDataProvider = null;
    }

    /**
     * Add another instance of an {@link ItemFactory}. Used by Declarative Services.
     *
     * @param factory The {@link ItemFactory} to add.
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addItemFactory(ItemFactory factory) {
        itemFactorys.add(factory);
        if (active) {
            dispatchBindingsPerItemType(null, factory.getSupportedItemTypes());
        }
    }

    /**
     * Removes the given {@link ItemFactory}. Used by Declarative Services.
     *
     * @param factory The {@link ItemFactory} to remove.
     */
    public void removeItemFactory(ItemFactory factory) {
        itemFactorys.remove(factory);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addBindingConfigReader(BindingConfigReader reader) {
        if (!bindingConfigReaders.containsKey(reader.getBindingType())) {
            bindingConfigReaders.put(reader.getBindingType(), reader);
            dispatchBindingsPerType(reader, new String[] { reader.getBindingType() });
        } else {
            logger.warn("Attempted to register a second BindingConfigReader of type '{}'."
                    + " The primaraly reader will remain active!", reader.getBindingType());
        }
    }

    public void removeBindingConfigReader(BindingConfigReader reader) {
        if (bindingConfigReaders.get(reader.getBindingType()).equals(reader)) {
            bindingConfigReaders.remove(reader.getBindingType());
        }
    }

    @Override
    public Collection<Item> getAll() {
        List<Item> items = new ArrayList<Item>();
        stateDescriptions.clear();
        for (String name : modelRepository.getAllModelNamesOfType("items")) {
            items.addAll(getItemsFromModel(name));
        }
        return items;
    }

    private Collection<Item> getItemsFromModel(String modelName) {
        logger.debug("Read items from model '{}'", modelName);

        List<Item> items = new ArrayList<Item>();
        if (modelRepository != null) {
            ItemModel model = (ItemModel) modelRepository.getModel(modelName);
            if (model != null) {
                for (ModelItem modelItem : model.getItems()) {
                    Item item = createItemFromModelItem(modelItem);
                    if (item != null) {
                        for (String groupName : modelItem.getGroups()) {
                            ((GenericItem) item).addGroupName(groupName);
                        }
                        items.add(item);
                    }
                }
            }
        }

        return items;
    }

    private void processBindingConfigsFromModel(String modelName, EventType type) {
        logger.debug("Processing binding configs for items from model '{}'", modelName);

        if (modelRepository != null) {
            ItemModel model = (ItemModel) modelRepository.getModel(modelName);
            if (model == null) {
                return;
            }

            // start binding configuration processing
            for (BindingConfigReader reader : bindingConfigReaders.values()) {
                reader.startConfigurationUpdate(modelName);
            }

            // create items and read new binding configuration
            if (!EventType.REMOVED.equals(type)) {
                for (ModelItem modelItem : model.getItems()) {
                    Item item = createItemFromModelItem(modelItem);
                    if (item != null) {
                        internalDispatchBindings(modelName, item, modelItem.getBindings());
                    }
                }
            }

            // end binding configuration processing
            for (BindingConfigReader reader : bindingConfigReaders.values()) {
                reader.stopConfigurationUpdate(modelName);
            }
        }
    }

    private Item createItemFromModelItem(ModelItem modelItem) {
        GenericItem item = null;
        if (modelItem instanceof ModelGroupItem) {
            ModelGroupItem modelGroupItem = (ModelGroupItem) modelItem;
            GenericItem baseItem;
            try {
                baseItem = createItemOfType(modelGroupItem.getType(), modelGroupItem.getName());
            } catch (IllegalArgumentException e) {
                logger.debug("Error creating base item for group item '{}', item will be ignored: {}",
                        modelGroupItem.getName(), e.getMessage());
                return null;
            }
            if (baseItem != null) {
                // if the user did not specify a function the first value of the enum in xtext (EQUAL) will be used
                ModelGroupFunction function = modelGroupItem.getFunction();
                item = applyGroupFunction(baseItem, modelGroupItem, function);
            } else {
                item = new GroupItem(modelGroupItem.getName());
            }
        } else {
            ModelNormalItem normalItem = (ModelNormalItem) modelItem;
            try {
                item = createItemOfType(normalItem.getType(), normalItem.getName());
            } catch (IllegalArgumentException e) {
                logger.debug("Error creating item '{}', item will be ignored: {}", normalItem.getName(),
                        e.getMessage());
                return null;
            }
        }
        if (item != null) {
            String label = modelItem.getLabel();
            String format = StringUtils.substringBetween(label, "[", "]");
            if (format != null) {
                label = StringUtils.substringBefore(label, "[").trim();
                stateDescriptions.put(modelItem.getName(), new StateDescription(null, null, null, format, false, null));
            }
            item.setLabel(label);
            item.setCategory(modelItem.getIcon());
            assignTags(modelItem, item);
            return item;
        } else {
            return null;
        }
    }

    private void assignTags(ModelItem modelItem, GenericItem item) {
        List<String> tags = modelItem.getTags();
        for (String tag : tags) {
            item.addTag(tag);
        }
    }

    private GroupItem applyGroupFunction(GenericItem baseItem, ModelGroupItem modelGroupItem,
            ModelGroupFunction function) {
        GroupFunctionDTO dto = new GroupFunctionDTO();
        dto.name = function.getName();
        dto.params = modelGroupItem.getArgs().toArray(new String[modelGroupItem.getArgs().size()]);

        GroupFunction groupFunction = ItemDTOMapper.mapFunction(baseItem, dto);

        return new GroupItem(modelGroupItem.getName(), baseItem, groupFunction);
    }

    private void dispatchBindingsPerItemType(BindingConfigReader reader, String[] itemTypes) {
        if (modelRepository != null) {
            for (String modelName : modelRepository.getAllModelNamesOfType("items")) {
                ItemModel model = (ItemModel) modelRepository.getModel(modelName);
                if (model != null) {
                    for (ModelItem modelItem : model.getItems()) {
                        for (String itemType : itemTypes) {
                            if (itemType.equals(modelItem.getType())) {
                                Item item = createItemFromModelItem(modelItem);
                                if (item != null) {
                                    internalDispatchBindings(reader, modelName, item, modelItem.getBindings());
                                }
                            }
                        }
                    }
                } else {
                    logger.debug("Model repository returned NULL for model named '{}'", modelName);
                }
            }
        } else {
            logger.warn("ModelRepository is NULL > dispatch bindings aborted!");
        }
    }

    private void dispatchBindingsPerType(BindingConfigReader reader, String[] bindingTypes) {
        if (modelRepository != null) {
            for (String modelName : modelRepository.getAllModelNamesOfType("items")) {
                ItemModel model = (ItemModel) modelRepository.getModel(modelName);
                if (model != null) {
                    for (ModelItem modelItem : model.getItems()) {
                        for (ModelBinding modelBinding : modelItem.getBindings()) {
                            for (String bindingType : bindingTypes) {
                                if (bindingType.equals(modelBinding.getType())) {
                                    Item item = createItemFromModelItem(modelItem);
                                    if (item != null) {
                                        internalDispatchBindings(reader, modelName, item, modelItem.getBindings());
                                    }
                                }
                            }
                        }
                    }
                } else {
                    logger.debug("Model repository returned NULL for model named '{}'", modelName);
                }
            }
        } else {
            logger.warn("ModelRepository is NULL > dispatch bindings aborted!");
        }
    }

    private void internalDispatchBindings(String modelName, Item item, EList<ModelBinding> bindings) {
        internalDispatchBindings(null, modelName, item, bindings);
    }

    private void internalDispatchBindings(BindingConfigReader reader, String modelName, Item item,
            EList<ModelBinding> bindings) {
        for (ModelBinding binding : bindings) {
            String bindingType = binding.getType();
            String config = binding.getConfiguration();

            Configuration configuration = new Configuration();
            binding.getProperties().forEach(p -> configuration.put(p.getKey(), p.getValue()));

            BindingConfigReader localReader = reader;
            if (reader == null) {
                logger.trace("Given binding config reader is null > query cache to find appropriate reader!");
                localReader = bindingConfigReaders.get(bindingType);
            } else {
                if (!localReader.getBindingType().equals(binding.getType())) {
                    logger.trace(
                            "The Readers' binding type '{}' and the Bindings' type '{}' doesn't match > continue processing next binding.",
                            localReader.getBindingType(), binding.getType());
                    continue;
                } else {
                    logger.debug("Start processing binding configuration of Item '{}' with '{}' reader.", item,
                            localReader.getClass().getSimpleName());
                }
            }

            if (localReader != null) {
                try {
                    localReader.validateItemType(item.getType(), config);
                    localReader.processBindingConfiguration(modelName, item.getType(), item.getName(), config,
                            configuration);
                } catch (BindingConfigParseException e) {
                    logger.error("Binding configuration of type '{}' of item '{}' could not be parsed correctly.",
                            bindingType, item.getName(), e);
                } catch (Exception e) {
                    // Catch badly behaving binding exceptions and continue processing
                    logger.error("Binding configuration of type '{}' of item '{}' could not be parsed correctly.",
                            bindingType, item.getName(), e);
                }
            } else {
                genericMetaDataProvider.addMetadata(bindingType, item.getName(), config, configuration.getProperties());
            }
        }
    }

    @Override
    public void modelChanged(String modelName, EventType type) {
        if (modelName.endsWith("items")) {
            switch (type) {
                case ADDED:
                case MODIFIED:
                    Map<String, Item> oldItems = toItemMap(itemsMap.get(modelName));
                    Map<String, Item> newItems = toItemMap(getItemsFromModel(modelName));
                    itemsMap.put(modelName, newItems.values());
                    for (Item newItem : newItems.values()) {
                        if (oldItems.containsKey(newItem.getName())) {
                            Item oldItem = oldItems.get(newItem.getName());
                            if (hasItemChanged(oldItem, newItem)) {
                                notifyListenersAboutUpdatedElement(oldItem, newItem);
                            }
                        } else {
                            notifyListenersAboutAddedElement(newItem);
                        }
                    }
                    processBindingConfigsFromModel(modelName, type);
                    for (Item oldItem : oldItems.values()) {
                        if (!newItems.containsKey(oldItem.getName())) {
                            notifyListenersAboutRemovedElement(oldItem);
                            this.stateDescriptions.remove(oldItem.getName());
                            genericMetaDataProvider.removeMetadata(oldItem.getName());
                        }
                    }
                    break;
                case REMOVED:
                    processBindingConfigsFromModel(modelName, type);
                    Collection<Item> itemsFromModel = getItemsFromModel(modelName);
                    itemsMap.remove(modelName);
                    for (Item item : itemsFromModel) {
                        notifyListenersAboutRemovedElement(item);
                    }
                    break;
            }
        }
    }

    protected boolean hasItemChanged(Item item1, Item item2) {
        return !Objects.equals(item1.getClass(), item2.getClass()) || //
                !Objects.equals(item1.getName(), item2.getName()) || //
                !Objects.equals(item1.getCategory(), item2.getCategory()) || //
                !Objects.equals(item1.getGroupNames(), item2.getGroupNames()) || //
                !Objects.equals(item1.getLabel(), item2.getLabel()) || //
                !Objects.equals(item1.getTags(), item2.getTags()) || //
                !Objects.equals(item1.getType(), item2.getType()) || //
                hasGroupItemChanged(item1, item2);
    }

    private boolean hasGroupItemChanged(Item item1, Item item2) {
        GroupItem gItem1 = null;
        GroupItem gItem2 = null;

        if (item1 instanceof GroupItem) {
            gItem1 = (GroupItem) item1;
        }
        if (item2 instanceof GroupItem) {
            gItem2 = (GroupItem) item2;
        }

        if (gItem1 == null && gItem2 == null) {
            return false;
        }

        if ((gItem1 != null && gItem2 == null) || (gItem1 == null && gItem2 != null)) {
            return true;
        }

        boolean sameBaseItemClass = Objects.equals(gItem1.getBaseItem(), gItem2.getBaseItem());

        boolean sameFunction = false;
        GroupFunction gf1 = gItem1.getFunction();
        GroupFunction gf2 = gItem2.getFunction();
        if (gf1 != null && gf2 != null) {
            if (Objects.equals(gf1.getClass(), gf2.getClass())) {
                sameFunction = Arrays.equals(gf1.getParameters(), gf2.getParameters());
            }
        } else if (gf1 == null && gf2 == null) {
            sameFunction = true;
        }

        return !(sameBaseItemClass && sameFunction);
    }

    private Map<String, Item> toItemMap(Collection<Item> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Item> ret = new LinkedHashMap<>();
        for (Item item : items) {
            ret.put(item.getName(), item);
        }
        return ret;
    }

    /**
     * Creates a new item of type {@code itemType} by utilizing an appropriate {@link ItemFactory}.
     *
     * @param itemType The type to find the appropriate {@link ItemFactory} for.
     * @param itemName The name of the {@link Item} to create.
     *
     * @return An Item instance of type {@code itemType} or null if no item factory for it was found.
     */
    private GenericItem createItemOfType(String itemType, String itemName) {
        if (itemType == null) {
            return null;
        }

        for (ItemFactory factory : itemFactorys) {
            GenericItem item = factory.createItem(itemType, itemName);
            if (item != null) {
                logger.trace("Created item '{}' of type '{}'", itemName, itemType);
                return item;
            }
        }

        logger.debug("Couldn't find ItemFactory for item '{}' of type '{}'", itemName, itemType);
        return null;
    }

    @Override
    public StateDescription getStateDescription(String itemName, Locale locale) {
        return stateDescriptions.get(itemName);
    }

}
