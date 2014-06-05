package org.eclipse.smarthome.core.thing;

import java.util.List;


/**
 * The {@link BridgeType} describes a concrete type of a {@link Bridge}. A
 * {@link BridgeType} inherits a
 * {@link org.eclipse.smarthome.core.thing.ThingType} and signals a
 * parent-child relation.
 * <p>
 * This description is used as template definition for the creation of the
 * according concrete {@link Bridge} object.
 * <p>
 * <b>Hint:</b> This class is immutable.
 * 
 * @author Michael Grammling - Initial Contribution
 */
public class BridgeType extends org.eclipse.smarthome.core.thing.ThingType {
    
    /**
     * @see BridgeType#BridgeType(String, List, DescriptionTypeMetaInfo, String,
     *      List, String)
     */
    public BridgeType(String bindingId, String thingTypeId, DescriptionTypeMetaInfo metaInfo,
            String manufacturer) throws IllegalArgumentException {
        this(new ThingTypeUID(bindingId, thingTypeId), null, metaInfo, manufacturer, null, null);
    }

    /**
     * Creates a new instance of this class with the specified parameters.
     * 
     * @param uid
     *            the unique identifier which identifies this Bridge type within
     *            the overall system (must neither be null, nor empty)
     * 
     * @param supportedBridgeTypeUIDs
     *            the unique identifiers to the bridges this Bridge type
     *            supports (could be null or empty)
     * 
     * @param metaInfo
     *            the meta information containing human readable text of this
     *            Bridge type (must not be null)
     * 
     * @param manufacturer
     *            the human readable name of the manufacturer of this Bridge
     *            type (could be null or empty)
     * 
     * @param channelDefinitions
     *            the channels this Bridge type provides (could be null or
     *            empty)
     * 
     * @param configDescriptionURI
     *            the link to the concrete ConfigDescription (could be null)
     * 
     * @throws IllegalArgumentException
     *             if the UID is null or empty, or the the meta information is
     *             null
     */
    public BridgeType(ThingTypeUID uid, List<String> supportedBridgeTypeUIDs,
            DescriptionTypeMetaInfo metaInfo, String manufacturer,
            List<ChannelDefinition> channelDefinitions, String configDescriptionURI)
            throws IllegalArgumentException {

        super(uid, supportedBridgeTypeUIDs, metaInfo, manufacturer,
                channelDefinitions, configDescriptionURI);
    }

}
