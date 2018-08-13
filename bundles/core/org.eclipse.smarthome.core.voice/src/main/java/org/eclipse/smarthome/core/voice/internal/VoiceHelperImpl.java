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
package org.eclipse.smarthome.core.voice.internal;

import static java.util.Comparator.comparing;

import java.util.Comparator;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.voice.TTSService;
import org.eclipse.smarthome.core.voice.Voice;
import org.osgi.service.component.annotations.Component;

/**
 * {@link VoiceHelperImpl} implements a utility method to consume sorted voices.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
@Component
public class VoiceHelperImpl implements VoiceHelper {

    @Override
    public void withSortedVoices(Stream<TTSService> ttsServices, Locale locale,
            BiConsumer<TTSService, Voice> consumer) {
        Stream<TTSService> sortedTTSs = ttsServices.sorted(comparing(s -> s.getLabel(locale)));

        Comparator<Voice> voiceLocaleComparator = (Voice v1, Voice v2) -> {
            return v1.getLocale().getDisplayName(locale).compareTo(v2.getLocale().getDisplayName(locale));
        };

        Function<TTSService, Stream<Voice>> getSortedVoices = (TTSService s) -> {
            return s.getAvailableVoices().stream().sorted(voiceLocaleComparator.thenComparing(Voice::getLabel));
        };

        sortedTTSs.forEach(s -> getSortedVoices.apply(s).forEach(v -> consumer.accept(s, v)));
    }

}
