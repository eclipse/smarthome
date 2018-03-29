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
package org.eclipse.smarthome.core.internal.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.service.StateDescriptionService;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateDescriptionFragment;
import org.eclipse.smarthome.core.types.StateDescriptionFragmentBuilder;
import org.eclipse.smarthome.core.types.StateDescriptionFragmentProvider;
import org.eclipse.smarthome.core.types.StateDescriptionProvider;
import org.eclipse.smarthome.core.types.StateOption;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This service contains different StateDescriptionProviders and provides a
 * getStateDescription method that returns a single StateDescription using all
 * of the providers.
 *
 * @author Lyubomir Papazov - Initial contribution
 *
 */
@NonNullByDefault
@Component
public class StateDescriptionServiceImpl implements StateDescriptionService {

    @Deprecated
    private final Set<StateDescriptionProvider> stateDescriptionProviders = Collections
            .synchronizedSet(new TreeSet<StateDescriptionProvider>(new Comparator<StateDescriptionProvider>() {
                @Override
                public int compare(StateDescriptionProvider provider1, StateDescriptionProvider provider2) {
                    return provider2.getRank().compareTo(provider1.getRank());
                }
            }));

    private final Set<StateDescriptionFragmentProvider> stateDescriptionFragmentProviders = Collections.synchronizedSet(
            new TreeSet<StateDescriptionFragmentProvider>(new Comparator<StateDescriptionFragmentProvider>() {
                @Override
                public int compare(StateDescriptionFragmentProvider provider1,
                        StateDescriptionFragmentProvider provider2) {
                    return provider2.getRank().compareTo(provider1.getRank());
                }
            }));

    @Deprecated
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addStateDescriptionProvider(StateDescriptionProvider provider) {
        stateDescriptionProviders.add(provider);
    }

    @Deprecated
    public void removeStateDescriptionProvider(StateDescriptionProvider provider) {
        stateDescriptionProviders.remove(provider);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addStateDescriptionFragmentProvider(StateDescriptionFragmentProvider provider) {
        stateDescriptionFragmentProviders.add(provider);
    }

    public void removeStateDescriptionFragmentProvider(StateDescriptionFragmentProvider provider) {
        stateDescriptionFragmentProviders.remove(provider);
    }

    @Override
    public @Nullable StateDescription getStateDescription(String itemName, @Nullable Locale locale) {
        StateDescription legacy = getLegacyStateDescription(itemName, locale);
        StateDescriptionFragment stateDescriptionFragment = mergeStateDescriptionFragments(itemName, locale);

        if (legacy != null) {
            StateDescriptionFragmentBuilder builder = StateDescriptionFragmentBuilder.instance();
            builder.mergeStateDescription(legacy) //
                    .mergeStateDescriptionFragment(stateDescriptionFragment);

            stateDescriptionFragment = builder.build();
        }

        return stateDescriptionFragment.toStateDescription();
    }

    private StateDescriptionFragment mergeStateDescriptionFragments(String itemName, @Nullable Locale locale) {
        StateDescriptionFragmentBuilder builder = StateDescriptionFragmentBuilder.instance();
        for (StateDescriptionFragmentProvider provider : stateDescriptionFragmentProviders) {
            StateDescriptionFragment fragment = provider.getStateDescriptionFragment(itemName, locale);
            if (fragment == null) {
                continue;
            }
            builder.mergeStateDescriptionFragment(fragment);
        }

        return builder.build();
    }

    @Deprecated
    private @Nullable StateDescription getLegacyStateDescription(String itemName, @Nullable Locale locale) {
        StateDescription result = null;
        List<StateOption> stateOptions = Collections.emptyList();
        for (StateDescriptionProvider stateDescriptionProvider : stateDescriptionProviders) {
            StateDescription stateDescription = stateDescriptionProvider.getStateDescription(itemName, locale);
            if (stateDescription == null) {
                continue;
            }

            // we pick up the first valid StateDescription here:
            if (result == null) {
                result = stateDescription;
            }

            // if the current StateDescription does provide options
            // and we don't already have some, we pick them up here:
            if (!stateDescription.getOptions().isEmpty() && stateOptions.isEmpty()) {
                stateOptions = stateDescription.getOptions();
            }
        }

        // we recreate the StateDescription in case we found a valid one and state options are given,
        // or readOnly is set:
        if (result != null && !stateOptions.isEmpty()) {
            result = new StateDescription(result.getMinimum(), result.getMaximum(), result.getStep(),
                    result.getPattern(), result.isReadOnly(), stateOptions);
        }

        return result;
    }
}
