/**
 * Copyright (c) 2014,2017 Contributors to the Eclipse Foundation
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
package org.eclipse.smarthome.model.thing.internal

import java.math.BigDecimal
import java.util.ArrayList
import java.util.Collection
import java.util.HashSet
import java.util.List
import java.util.Map
import java.util.Set
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type
import org.eclipse.smarthome.config.core.ConfigDescriptionRegistry
import org.eclipse.smarthome.config.core.Configuration
import org.eclipse.smarthome.core.common.registry.AbstractProvider
import org.eclipse.smarthome.core.i18n.LocaleProvider
import org.eclipse.smarthome.core.service.ReadyMarker
import org.eclipse.smarthome.core.service.ReadyMarkerFilter
import org.eclipse.smarthome.core.service.ReadyService
import org.eclipse.smarthome.core.thing.Bridge
import org.eclipse.smarthome.core.thing.Channel
import org.eclipse.smarthome.core.thing.ChannelUID
import org.eclipse.smarthome.core.thing.Thing
import org.eclipse.smarthome.core.thing.ThingProvider
import org.eclipse.smarthome.core.thing.ThingTypeUID
import org.eclipse.smarthome.core.thing.ThingUID
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory
import org.eclipse.smarthome.core.thing.binding.builder.BridgeBuilder
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder
import org.eclipse.smarthome.core.thing.type.ChannelDefinition
import org.eclipse.smarthome.core.thing.type.ChannelKind
import org.eclipse.smarthome.core.thing.type.ChannelType
import org.eclipse.smarthome.core.thing.type.ChannelTypeRegistry
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID
import org.eclipse.smarthome.core.thing.type.ThingTypeRegistry
import org.eclipse.smarthome.core.thing.util.ThingHelper
import org.eclipse.smarthome.model.core.ModelRepository
import org.eclipse.smarthome.model.core.ModelRepositoryChangeListener
import org.eclipse.smarthome.model.thing.thing.ModelBridge
import org.eclipse.smarthome.model.thing.thing.ModelChannel
import org.eclipse.smarthome.model.thing.thing.ModelPropertyContainer
import org.eclipse.smarthome.model.thing.thing.ModelThing
import org.eclipse.smarthome.model.thing.thing.ThingModel
import org.eclipse.xtend.lib.annotations.Data
import org.osgi.framework.FrameworkUtil
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * {@link ThingProvider} implementation which computes *.things files.
 * 
 * @author Oliver Libutzki - Initial contribution
 * @author niehues - Fix ESH Bug 450236
 *         https://bugs.eclipse.org/bugs/show_bug.cgi?id=450236 - Considering
 *         ThingType Description
 * @author Simon Kaufmann - Added asynchronous retry in case the handler 
 *         factory cannot load a thing yet (bug 470368), 
 *         added delay until ThingTypes are fully loaded
 * @author Markus Rathgeb - Add locale provider support
 * 
 */
 @Component(immediate=true, service=ThingProvider)
class GenericThingProvider extends AbstractProvider<Thing> implements ThingProvider, ModelRepositoryChangeListener, ReadyService.ReadyTracker {

    private static final String XML_THING_TYPE = "esh.xmlThingTypes";

    private LocaleProvider localeProvider

    private ModelRepository modelRepository

    private ThingTypeRegistry thingTypeRegistry
    private ChannelTypeRegistry channelTypeRegistry

    private Map<String, Collection<Thing>> thingsMap = new ConcurrentHashMap

    private List<ThingHandlerFactory> thingHandlerFactories = new CopyOnWriteArrayList<ThingHandlerFactory>()
    
    private ConfigDescriptionRegistry configDescriptionRegistry

    private val List<QueueContent> queue = new CopyOnWriteArrayList
    private var Thread lazyRetryThread = null
    
    private static final Logger logger = LoggerFactory.getLogger(GenericThingProvider)

    private val Set<String> loadedXmlThingTypes = new CopyOnWriteArraySet

    def void activate() {
        modelRepository.getAllModelNamesOfType("things").forEach [
            createThingsFromModel
        ]
    }

    override Collection<Thing> getAll() {
        thingsMap.values.flatten.toList
    }

    def private void createThingsFromModel(String modelName) {
        logger.debug("Read things from model '{}'", modelName);
        if (thingsMap.get(modelName) == null) {
            thingsMap.put(modelName, newArrayList)
        }
        if (modelRepository != null) {
            val model = modelRepository.getModel(modelName) as ThingModel
            model?.things?.map[
                // Get the ThingHandlerFactories
                val ThingUID thingUID = constructThingUID
                if (thingUID != null) {
                    val thingTypeUID = constructThingTypeUID(thingUID)
                    return thingHandlerFactories.findFirst[
                        supportsThingType(thingTypeUID)
                    ]
                } else {
                    // ignore the Thing because its definition is broken
                    return null
                }
            ]?.filter[
                // Drop it if there is no ThingHandlerFactory yet which can handle it 
                it != null
            ]?.toSet?.forEach[
                // Execute for each unique ThingHandlerFactory
                createThingsFromModelForThingHandlerFactory(modelName, it)
            ]
        }
    }
    
    def private ThingUID constructThingUID(ModelThing modelThing) {
        if (modelThing.id != null) {
            return new ThingUID(modelThing.id)
        } else {
            if (modelThing.bridgeUID != null) {
                val bindingId = new ThingUID(modelThing.bridgeUID).bindingId
                return new ThingUID(bindingId, modelThing.thingTypeId, modelThing.thingId)
            } else {
                logger.warn("Thing {} does not have a bridge so it needs to be defined in full notation like <bindingId>:{}:{}", modelThing.thingTypeId, modelThing.thingTypeId, modelThing.thingId)
                return null
            }
        }
    }
    
    def private ThingTypeUID constructThingTypeUID(ModelThing modelThing, ThingUID thingUID) {
        if (modelThing.thingTypeId != null) {
            return new ThingTypeUID(thingUID.bindingId, modelThing.thingTypeId)
        } else {
            return new ThingTypeUID(thingUID.bindingId, thingUID.thingTypeId)
        }
    }

    def private void createThing(ModelThing modelThing, Bridge parentBridge, Collection<Thing> thingList,
        ThingHandlerFactory thingHandlerFactory) {
        val ThingUID thingUID = getThingUID(modelThing, parentBridge?.UID)
        if (thingUID == null) {
            // ignore the Thing because its definition is broken
            return
        }
        val thingTypeUID = modelThing.constructThingTypeUID(thingUID)
        var ThingUID bridgeUID = null
        if (parentBridge != null) {
            bridgeUID = parentBridge.UID
        } else {
            if (modelThing.bridgeUID != null && !modelThing.bridgeUID.empty) {
                bridgeUID = new ThingUID(modelThing.bridgeUID)
            }
        }

        if (!isSupportedByThingHandlerFactory(thingTypeUID, thingHandlerFactory)) {
            // return silently, we were not asked to do anything
            return
        }

        logger.trace("Creating thing for type '{}' with UID '{}.", thingTypeUID, thingUID);
        val configuration = modelThing.createConfiguration
        val uid = thingUID
        if (thingList.exists[UID.equals(uid)]) {
            // the thing is already in the list and nothing will be done!
            logger.debug("Thing already exists {}", uid.toString)
            return
        }

        val thingType = thingTypeUID.thingType

        val label = if (modelThing.label != null) modelThing.label else thingType?.label
        
        val location = modelThing.location

        val thingFromHandler = getThingFromThingHandlerFactories(thingTypeUID, label, configuration, thingUID,
            bridgeUID, thingHandlerFactory)

        val thingBuilder = if (modelThing instanceof ModelBridge) {
                BridgeBuilder.create(thingTypeUID, thingUID)
            } else {
                ThingBuilder.create(thingTypeUID, thingUID)
            }

        thingBuilder.withConfiguration(configuration)
        thingBuilder.withBridge(bridgeUID)
        if(label !== null) {
            thingBuilder.withLabel(label)
            }
        thingBuilder.withLocation(location)

        val channels = createChannels(thingTypeUID, thingUID, modelThing.channels,
            thingType?.channelDefinitions ?: newArrayList)
        thingBuilder.withChannels(channels)

        var thing = thingBuilder.build

        // ask the ThingHandlerFactories for a thing
        if (thingFromHandler != null) {

            // If a thingHandlerFactory could create a thing, merge the content of the modelThing to it
            thingFromHandler.merge(thing)
        }

        thingList += thingFromHandler ?: thing

        if (modelThing instanceof ModelBridge) {
            val bridge = (thingFromHandler ?: thing) as Bridge
            modelThing.things.forEach [
                createThing(bridge as Bridge, thingList, thingHandlerFactory)
            ]
        }
    }

    def private boolean isSupportedByThingHandlerFactory(ThingTypeUID thingTypeUID, ThingHandlerFactory specific) {
        if (specific !== null) {
            return specific.supportsThingType(thingTypeUID)
        }
        for (ThingHandlerFactory thingHandlerFactory : thingHandlerFactories) {
            if (thingHandlerFactory.supportsThingType(thingTypeUID)) {
                return true;
            }
        }
        return false;
    }

    def private Thing getThingFromThingHandlerFactories(ThingTypeUID thingTypeUID, String label,
        Configuration configuration, ThingUID thingUID, ThingUID bridgeUID, ThingHandlerFactory specific) {
        if (specific != null && specific.supportsThingType(thingTypeUID)) {
            logger.trace("Creating thing from specific ThingHandlerFactory {} for thingType {}", specific, thingTypeUID)
            return getThingFromThingHandlerFactory(thingTypeUID, label, configuration, thingUID, bridgeUID, specific)
        }
        for (ThingHandlerFactory thingHandlerFactory : thingHandlerFactories) {
            logger.trace("Searching thingHandlerFactory for thingType: {}", thingTypeUID)
            if (thingHandlerFactory.supportsThingType(thingTypeUID)) {
                return getThingFromThingHandlerFactory(thingTypeUID, label, configuration, thingUID, bridgeUID,
                    thingHandlerFactory)
            }
        }
        null
    }

    def private getThingFromThingHandlerFactory(ThingTypeUID thingTypeUID, String label, Configuration configuration,
        ThingUID thingUID, ThingUID bridgeUID, ThingHandlerFactory thingHandlerFactory) {
        val thing = thingHandlerFactory.createThing(thingTypeUID, configuration, thingUID, bridgeUID)
        if (thing == null) {
            // Apparently the HandlerFactory's eyes were bigger than its stomach...
            // Possible cause: Asynchronous loading of the XML files
            // Add the data to the queue in order to retry it later
            logger.debug(
                "ThingHandlerFactory '{}' claimed it can handle '{}' type but actually did not. Queued for later refresh.",
                thingHandlerFactory.class.simpleName, thingTypeUID.asString)
            queue.add(new QueueContent(thingTypeUID, label, configuration, thingUID, bridgeUID, thingHandlerFactory))
            if (lazyRetryThread == null || !lazyRetryThread.alive) {
                lazyRetryThread = new Thread(lazyRetryRunnable)
                lazyRetryThread.start
            }
        } else {
            thing.label = label
        }
        return thing
    }

    def dispatch void merge(Thing targetThing, Thing sourceThing) {
        targetThing.bridgeUID = sourceThing.bridgeUID
        targetThing.configuration.merge(sourceThing.configuration)
        targetThing.merge(sourceThing.channels)
        targetThing.location = sourceThing.location
        targetThing.label = sourceThing.label
    }

    def dispatch void merge(Configuration target, Configuration source) {
        source.keySet.forEach [
            target.put(it, source.get(it))
        ]
    }

    def dispatch void merge(Thing targetThing, List<Channel> source) {
        val List<Channel> channelsToAdd = newArrayList()
        source.forEach [ sourceChannel |
            val targetChannels = targetThing.channels.filter[it.UID.equals(sourceChannel.UID)]
            targetChannels.forEach [
                merge(sourceChannel)
            ]
            if (targetChannels.empty) {
                channelsToAdd.add(sourceChannel)
            }
        ]

        // add the channels only defined in source list to the target list
        ThingHelper.addChannelsToThing(targetThing, channelsToAdd)
    }

    def dispatch void merge(Channel target, Channel source) {
        target.configuration.merge(source.configuration)
    }

    def private getParentPath(ThingUID bridgeUID) {
        var bridgeIds = bridgeUID.bridgeIds
        bridgeIds.add(bridgeUID.id)
        return bridgeIds
    }

    def private List<Channel> createChannels(ThingTypeUID thingTypeUID, ThingUID thingUID,
        List<ModelChannel> modelChannels, List<ChannelDefinition> channelDefinitions) {
        val Set<String> addedChannelIds = newHashSet
        val List<Channel> channels = newArrayList
        modelChannels.forEach [
            if (addedChannelIds.add(id)) {
                var ChannelKind parsedKind = ChannelKind.STATE
                
                var ChannelTypeUID channelTypeUID
                var String itemType
                var label = it.label
                val configuration = createConfiguration
                if (it.channelType != null) {
                    channelTypeUID = new ChannelTypeUID(thingUID.bindingId, it.channelType)
                    val resolvedChannelType = channelTypeUID.channelType
                    if (resolvedChannelType != null) {
                        itemType = resolvedChannelType.itemType
                        parsedKind = resolvedChannelType.kind
                        if (label == null) {
                            label = resolvedChannelType.label
                        }
                        applyDefaultConfiguration(configuration, resolvedChannelType)
                    } else {
                        logger.error("Channel type {} could not be resolved.",  channelTypeUID.asString)
                    }
                } else {
                    itemType = it.type
                    
                    val kind = if (it.channelKind == null) "State" else it.channelKind                 
                    parsedKind = ChannelKind.parse(kind)
                }
                
                var channel = ChannelBuilder.create(new ChannelUID(thingUID, id), itemType)
                    .withKind(parsedKind)
                    .withConfiguration(configuration)
                    .withType(channelTypeUID)
                    .withLabel(label)
                channels += channel.build()
            }
        ]
        channelDefinitions.forEach [
            if (addedChannelIds.add(id)) {
                val channelType = it.channelTypeUID.channelType
                if (channelType != null) {
                    channels +=
                        ChannelBuilder.create(new ChannelUID(thingTypeUID, thingUID, id), channelType.itemType).
                            withType(it.channelTypeUID).build
                } else {
                    logger.warn(
                        "Could not create channel '{}' for thing '{}', because channel type '{}' could not be found.",
                        it.getId(), thingUID, it.getChannelTypeUID());
                }
            }
        ]
        channels
    }
    
    def private applyDefaultConfiguration(Configuration configuration, ChannelType channelType) {
        if (configDescriptionRegistry != null && configuration != null) {
            if (channelType.configDescriptionURI != null) {
                val configDescription = configDescriptionRegistry.getConfigDescription(channelType.configDescriptionURI)
                if (configDescription != null) {
                    configDescription.parameters.filter[
                        ^default != null && configuration.get(name) == null
                    ].forEach[
                        val value = getDefaultValueAsCorrectType(type, ^default)
                        if (value != null) {
                            configuration.put(name, value);
                        }
                    ]
                }
            }
        }
    }
    
    def private Object getDefaultValueAsCorrectType(Type parameterType, String defaultValue) {
        try {
            switch (parameterType) {
                case TEXT:
                    return defaultValue
                case BOOLEAN:
                    return Boolean.parseBoolean(defaultValue)
                case INTEGER:
                    return new BigDecimal(defaultValue)
                case DECIMAL:
                    return new BigDecimal(defaultValue)
                default:
                    return null
            }
        } catch (NumberFormatException ex) {
            logger.warn("Could not parse default value '{}' as type '{}': {}", defaultValue, parameterType, ex.getMessage(), ex);
            return null;
        }
    }
    

    def private createConfiguration(ModelPropertyContainer propertyContainer) {
        val configuration = new Configuration
        propertyContainer.properties.forEach [
            configuration.put(key, value)
        ]
        configuration
    }

    def private getThingType(ThingTypeUID thingTypeUID) {
        thingTypeRegistry?.getThingType(thingTypeUID, localeProvider.getLocale())
    }

    def private getChannelType(ChannelTypeUID channelTypeUID) {
        channelTypeRegistry?.getChannelType(channelTypeUID, localeProvider.getLocale())
    }

    @Reference
    def protected void setModelRepository(ModelRepository modelRepository) {
        this.modelRepository = modelRepository
        modelRepository.addModelRepositoryChangeListener(this)
    }

    def protected void unsetModelRepository(ModelRepository modelRepository) {
        modelRepository.removeModelRepositoryChangeListener(this)
        this.modelRepository = null
    }

    override void modelChanged(String modelName, org.eclipse.smarthome.model.core.EventType type) {
        if (modelName.endsWith("things")) {
            switch type {
                case org.eclipse.smarthome.model.core.EventType.ADDED: {
                    createThingsFromModel(modelName)
                }
                case org.eclipse.smarthome.model.core.EventType.MODIFIED: {
                    val oldThings = thingsMap.get(modelName) ?: newArrayList
                    val model = modelRepository.getModel(modelName) as ThingModel
                    if (model != null) {
                        val newThingUIDs = model.allThingUIDs
                        val removedThings = oldThings.filter[!newThingUIDs.contains(it.UID)]
                        removedThings.forEach [
                            notifyListenersAboutRemovedElement
                        ]
                        createThingsFromModel(modelName)
                    }
                }
                case org.eclipse.smarthome.model.core.EventType.REMOVED: {
                    val things = thingsMap.remove(modelName) ?: newArrayList
                    things.forEach [
                        notifyListenersAboutRemovedElement
                    ]
                }
            }
        }
    }

    def private Set<ThingUID> getAllThingUIDs(ThingModel model) {
        return getAllThingUIDs(model.things, null)
    }    

    def private Set<ThingUID> getAllThingUIDs(List<ModelThing> thingList, ThingUID parentUID) {
        val ret = new HashSet<ThingUID>()
        thingList.forEach[
            val thingUID = getThingUID(it, parentUID)
            ret.add(thingUID)
            if (it instanceof ModelBridge) {
                ret.addAll(getAllThingUIDs(things, thingUID))
            } 
        ]
        return ret
    }    
    
    def private ThingUID getThingUID(ModelThing modelThing, ThingUID parentUID) {
        if (parentUID != null && modelThing.id == null) {
            val thingTypeUID = new ThingTypeUID(parentUID.bindingId, modelThing.thingTypeId)
            return new ThingUID(thingTypeUID, modelThing.thingId, parentUID.parentPath)
        } else {
            return modelThing.constructThingUID
        }
    }

    @Reference
    def protected void setLocaleProvider(LocaleProvider localeProvider) {
        this.localeProvider = localeProvider
    }

    def protected void unsetLocaleProvider(LocaleProvider localeProvider) {
        this.localeProvider = null
    }

    @Reference(cardinality=OPTIONAL, policy=DYNAMIC)
    def protected void setThingTypeRegistry(ThingTypeRegistry thingTypeRegistry) {
        this.thingTypeRegistry = thingTypeRegistry
    }

    def protected void unsetThingTypeRegistry(ThingTypeRegistry thingTypeRegistry) {
        this.thingTypeRegistry = null
    }

    @Reference
    def protected void setChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = channelTypeRegistry
    }

    def protected void unsetChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = null
    }

    @Reference(cardinality=MULTIPLE, policy=DYNAMIC)
    def protected void addThingHandlerFactory(ThingHandlerFactory thingHandlerFactory) {
        logger.debug("ThingHandlerFactory added {}", thingHandlerFactory)
        thingHandlerFactories.add(thingHandlerFactory);
        thingHandlerFactoryAdded(thingHandlerFactory)
    }

    def protected void removeThingHandlerFactory(ThingHandlerFactory thingHandlerFactory) {
        thingHandlerFactories.remove(thingHandlerFactory);
        thingHandlerFactoryRemoved()
    }

    def private thingHandlerFactoryRemoved() {
        // Don't do anything, Things should not be deleted
    }

    def thingHandlerFactoryAdded(ThingHandlerFactory thingHandlerFactory) {
        thingsMap.keySet.forEach [
            //create things for this specific thingHandlerFactory from the model.
            createThingsFromModelForThingHandlerFactory(it, thingHandlerFactory)
        ]
    }
    
    @Reference
    def void setReadyService(ReadyService readyService) {
        readyService.registerTracker(this, new ReadyMarkerFilter().withType(XML_THING_TYPE));
    }

    def void unsetReadyService(ReadyService readyService) {
        readyService.unregisterTracker(this);
    }

    override onReadyMarkerAdded(ReadyMarker readyMarker) {
        val bsn = readyMarker.identifier
        loadedXmlThingTypes.add(bsn)
        bsn.handleXmlThingTypesLoaded
    }
    
    def private getBundleName(ThingHandlerFactory thingHandlerFactory) {
        FrameworkUtil.getBundle(thingHandlerFactory.class).symbolicName
    }
    
    def private handleXmlThingTypesLoaded(String bsn) {
        thingHandlerFactories.filter[
            getBundleName.equals(bsn)
        ].forEach[ thingHandlerFactory |
            thingHandlerFactory.thingHandlerFactoryAdded
        ]
    }

    override onReadyMarkerRemoved(ReadyMarker readyMarker) {
        val bsn = readyMarker.identifier
        loadedXmlThingTypes.remove(bsn);
    }

    def private createThingsFromModelForThingHandlerFactory(String modelName, ThingHandlerFactory factory) {
        if (!loadedXmlThingTypes.contains(factory.bundleName)) {
            return
        }
        val oldThings = thingsMap.get(modelName).clone
        val newThings = newArrayList()
        if (modelRepository != null) {
            val model = modelRepository.getModel(modelName) as ThingModel
            if (model != null) {
                model.things.forEach [
                    createThing(null, newThings, factory)
                ]
            }
        }
        newThings.forEach [ newThing |
            val oldThing = oldThings.findFirst[it.UID == newThing.UID]
            if (oldThing != null) {
                logger.debug("Updating thing '{}' from model '{}'.", newThing.UID, modelName);
                notifyListenersAboutUpdatedElement(oldThing, newThing)
            } else {
                logger.debug("Adding thing '{}' from model '{}'.", newThing.UID, modelName);
                thingsMap.get(modelName).add(newThing)
                newThing.notifyListenersAboutAddedElement
            }
        ]
    }

    private val lazyRetryRunnable = new Runnable() {
        override run() {
            logger.debug("Starting lazy retry thread")
            while (!queue.empty) {
                if (!queue.empty) {
                    val newThings = new ArrayList
                    queue.forEach [ qc |
                        logger.trace("Searching thingHandlerFactory for thingType: {}", qc.thingTypeUID)
                        val thing = qc.thingHandlerFactory.createThing(qc.thingTypeUID, qc.configuration, qc.thingUID,
                            qc.bridgeUID)
                        if (thing != null) {
                            queue.remove(qc)
                            logger.debug("Successfully loaded '{}' during retry", qc.thingUID)
                            newThings.add(thing)
                        }
                    ]
                    if (!newThings.empty) {
                        newThings.forEach [ newThing |
                            val modelName = thingsMap.keySet.findFirst [ mName |
                                !thingsMap.get(mName).filter[it.UID == newThing.UID].empty
                            ]
                            val oldThing = thingsMap.get(modelName).findFirst[it.UID == newThing.UID]
                            if (oldThing != null) {
                                newThing.merge(oldThing)
                                thingsMap.get(modelName).remove(oldThing)
                                thingsMap.get(modelName).add(newThing)
                                logger.debug("Refreshing thing '{}' after successful retry", newThing.UID)
                                if (!ThingHelper.equals(oldThing, newThing)) {
                                    notifyListenersAboutUpdatedElement(oldThing, newThing)
                                }
                            } else {
                                throw new IllegalStateException(String.format("Item %s not yet known", newThing.UID))
                            }
                        ]
                    }
                }
                if (!queue.empty) {
                    Thread.sleep(1000)
                }
            }
            logger.debug("Lazy retry thread ran out of work. Good bye.")
        }
    }

    @Data
    private static final class QueueContent {
        ThingTypeUID thingTypeUID
        String label
        Configuration configuration
        ThingUID thingUID
        ThingUID bridgeUID
        ThingHandlerFactory thingHandlerFactory
    }

    @Reference
    def protected void setConfigDescriptionRegistry(ConfigDescriptionRegistry configDescriptionRegistry) {
        this.configDescriptionRegistry = configDescriptionRegistry
    }

    def protected void unsetConfigDescriptionRegistry(ConfigDescriptionRegistry configDescriptionRegistry) {
        this.configDescriptionRegistry = null
    }

}
