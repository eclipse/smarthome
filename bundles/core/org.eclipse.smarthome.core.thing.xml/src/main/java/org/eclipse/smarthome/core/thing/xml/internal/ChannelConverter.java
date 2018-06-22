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
package org.eclipse.smarthome.core.thing.xml.internal;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.smarthome.config.xml.util.ConverterAttributeMapValidator;
import org.eclipse.smarthome.config.xml.util.GenericUnmarshaller;
import org.eclipse.smarthome.config.xml.util.NodeIterator;
import org.eclipse.smarthome.config.xml.util.NodeValue;
import org.eclipse.smarthome.core.thing.type.AutoUpdatePolicy;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link ChannelConverter} is a concrete implementation of the {@code XStream} {@link Converter} interface used
 * to convert channel information within an XML document
 * into a {@link ChannelXmlResult} object.
 * <p>
 * This converter converts {@code channel} XML tags.
 *
 * @author Chris Jackson - Initial Contribution
 * @author Simon Kaufmann - Fixing wrong inheritance
 * @author Chris Jackson - Added label and description
 */
public class ChannelConverter extends GenericUnmarshaller<ChannelXmlResult> {

    private final ConverterAttributeMapValidator attributeMapValidator;

    public ChannelConverter() {
        super(ChannelXmlResult.class);

        attributeMapValidator = new ConverterAttributeMapValidator(
                new String[][] { { "id", "true" }, { "typeId", "false" } });
    }

    @SuppressWarnings("unchecked")
    protected List<NodeValue> getProperties(NodeIterator nodeIterator) {
        return (List<NodeValue>) nodeIterator.nextList("properties", false);
    }

    protected ChannelXmlResult unmarshalType(HierarchicalStreamReader reader, UnmarshallingContext context,
            Map<String, String> attributes, NodeIterator nodeIterator) throws ConversionException {
        String id = attributes.get("id");
        String typeId = attributes.get("typeId");
        String label = (String) nodeIterator.nextValue("label", false);
        String description = (String) nodeIterator.nextValue("description", false);
        AutoUpdatePolicy autoUpdatePolicy = readAutoUpdatePlicy(nodeIterator);
        List<NodeValue> properties = getProperties(nodeIterator);

        ChannelXmlResult channelXmlResult = new ChannelXmlResult(id, typeId, label, description, properties,
                autoUpdatePolicy);

        return channelXmlResult;
    }

    private AutoUpdatePolicy readAutoUpdatePlicy(NodeIterator nodeIterator) {
        String string = (String) nodeIterator.nextValue("autoUpdatePolicy", false);
        if (string != null) {
            return AutoUpdatePolicy.valueOf(string.toUpperCase(Locale.ENGLISH));
        }
        return null;
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        // read attributes
        Map<String, String> attributes = this.attributeMapValidator.readValidatedAttributes(reader);

        // read values
        List<?> nodes = (List<?>) context.convertAnother(context, List.class);
        NodeIterator nodeIterator = new NodeIterator(nodes);

        // create object
        Object object = unmarshalType(reader, context, attributes, nodeIterator);

        nodeIterator.assertEndOfType();

        return object;
    }
}
