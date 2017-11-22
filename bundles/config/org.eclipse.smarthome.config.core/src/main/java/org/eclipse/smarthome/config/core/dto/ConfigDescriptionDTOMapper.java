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
package org.eclipse.smarthome.config.core.dto;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterGroup;
import org.eclipse.smarthome.config.core.FilterCriteria;
import org.eclipse.smarthome.config.core.ParameterOption;

/**
 * {@link ConfigDescriptionDTOMapper} maps {@link ConfigDescription}s to the data transfer object
 * {@link ConfigDescriptionDTO}.
 *
 * @author Dennis Nobel - Initial contribution
 *
 */
public class ConfigDescriptionDTOMapper {

    /**
     * Maps config description into config description DTO object.
     *
     * @param configDescription the config description (not null)
     * @return the config description DTO object
     */
    public static ConfigDescriptionDTO map(ConfigDescription configDescription) {
        List<ConfigDescriptionParameterGroupDTO> parameterGroups = mapParameterGroups(
                configDescription.getParameterGroups());
        List<ConfigDescriptionParameterDTO> parameters = mapParameters(configDescription.getParameters());
        return new ConfigDescriptionDTO(toDecodedString(configDescription.getUID()), parameters, parameterGroups);
    }

    private static String toDecodedString(URI uri) {
        // combine these partials because URI.toString() does not decode
        return uri.getScheme() + ":" + uri.getSchemeSpecificPart();
    }

    /**
     * Maps config description parameters into DTO objects.
     *
     * @param parameters the config description parameters (not null)
     *
     * @return the parameter DTO objects (not null)
     */
    public static List<ConfigDescriptionParameterDTO> mapParameters(List<ConfigDescriptionParameter> parameters) {

        List<ConfigDescriptionParameterDTO> configDescriptionParameterBeans = new ArrayList<>(parameters.size());
        for (ConfigDescriptionParameter configDescriptionParameter : parameters) {
            ConfigDescriptionParameterDTO configDescriptionParameterBean = new ConfigDescriptionParameterDTO(
                    configDescriptionParameter.getName(), configDescriptionParameter.getType(),
                    configDescriptionParameter.getMinimum(), configDescriptionParameter.getMaximum(),
                    configDescriptionParameter.getStepSize(), configDescriptionParameter.getPattern(),
                    configDescriptionParameter.isRequired(), configDescriptionParameter.isReadOnly(),
                    configDescriptionParameter.isMultiple(), configDescriptionParameter.getContext(),
                    configDescriptionParameter.getDefault(), configDescriptionParameter.getLabel(),
                    configDescriptionParameter.getDescription(), mapOptions(configDescriptionParameter.getOptions()),
                    mapFilterCriteria(configDescriptionParameter.getFilterCriteria()),
                    configDescriptionParameter.getGroupName(), configDescriptionParameter.isAdvanced(),
                    configDescriptionParameter.getLimitToOptions(), configDescriptionParameter.getMultipleLimit(),
                    configDescriptionParameter.getUnit(), configDescriptionParameter.getUnitLabel(),
                    configDescriptionParameter.isVerifyable());
            configDescriptionParameterBeans.add(configDescriptionParameterBean);
        }
        return configDescriptionParameterBeans;

    }

    /**
     * Maps config description parameter groups into DTO objects.
     *
     * @param parameterGroups the config description parameter groups (not null)
     *
     * @return the parameter group DTO objects (not null)
     */
    public static List<ConfigDescriptionParameterGroupDTO> mapParameterGroups(
            List<ConfigDescriptionParameterGroup> parameterGroups) {

        List<ConfigDescriptionParameterGroupDTO> parameterGroupBeans = new ArrayList<>(parameterGroups.size());

        for (ConfigDescriptionParameterGroup parameterGroup : parameterGroups) {
            parameterGroupBeans
                    .add(new ConfigDescriptionParameterGroupDTO(parameterGroup.getName(), parameterGroup.getContext(),
                            parameterGroup.isAdvanced(), parameterGroup.getLabel(), parameterGroup.getDescription()));
        }

        return parameterGroupBeans;
    }

    private static List<FilterCriteriaDTO> mapFilterCriteria(List<FilterCriteria> filterCriteria) {
        if (filterCriteria == null) {
            return null;
        }
        List<FilterCriteriaDTO> result = new LinkedList<FilterCriteriaDTO>();
        for (FilterCriteria criteria : filterCriteria) {
            result.add(new FilterCriteriaDTO(criteria.getName(), criteria.getValue()));
        }
        return result;
    }

    private static List<ParameterOptionDTO> mapOptions(List<ParameterOption> options) {
        if (options == null) {
            return null;
        }
        List<ParameterOptionDTO> result = new LinkedList<ParameterOptionDTO>();
        for (ParameterOption option : options) {
            result.add(new ParameterOptionDTO(option.getValue(), option.getLabel()));
        }
        return result;
    }

}
