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
package org.eclipse.smarthome.automation.parser.gson.internal;

import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.smarthome.automation.core.dto.RuleTemplateDTOMapper;
import org.eclipse.smarthome.automation.dto.RuleTemplateDTO;
import org.eclipse.smarthome.automation.parser.ParsingException;
import org.eclipse.smarthome.automation.parser.ParsingNestedException;
import org.eclipse.smarthome.automation.template.RuleTemplate;
import org.eclipse.smarthome.automation.template.Template;

import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/**
 * This class can parse and serialize sets of {@link Template}s.
 *
 * @author Kai Kreuzer - Initial Contribution
 *
 */
public class TemplateGSONParser extends AbstractGSONParser<RuleTemplate> {

    @Override
    public Set<RuleTemplate> parse(InputStreamReader reader) throws ParsingException {
        Set<RuleTemplate> templates = new HashSet<>();
        try (JsonReader jr = new JsonReader(reader)) {
            if (jr.hasNext()) {
                JsonToken token = jr.peek();
                if (JsonToken.BEGIN_ARRAY.equals(token)) {
                    List<RuleTemplateDTO> templateDtos = gson.fromJson(jr, new TypeToken<List<RuleTemplateDTO>>() {
                    }.getType());
                    for (RuleTemplateDTO templateDto : templateDtos) {
                        templates.add(RuleTemplateDTOMapper.map(templateDto));
                    }
                } else {
                    RuleTemplateDTO template = gson.fromJson(jr, RuleTemplateDTO.class);
                    templates.add(RuleTemplateDTOMapper.map(template));
                }
            }
        } catch (Exception e) {
            throw new ParsingException(new ParsingNestedException(ParsingNestedException.TEMPLATE, null, e));
        }
        return templates;
    }
}
