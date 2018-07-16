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
package org.eclipse.smarthome.automation.core.util;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.automation.Trigger;

/**
 * This class allows the easy construction of a {@link Trigger} instance using the builder pattern.
 *
 * @author Markus Rathgeb - Initial contribution and API
 */
@NonNullByDefault
public class TriggerBuilder extends ModuleBuilder<TriggerBuilder, Trigger> {

    protected TriggerBuilder() {
        super();
    }

    protected TriggerBuilder(final Trigger condition) {
        super(condition);
    }

    public static TriggerBuilder create() {
        return new TriggerBuilder();
    }

    public static TriggerBuilder create(final Trigger trigger) {
        return new TriggerBuilder(trigger);
    }

    @Override
    public Trigger build() {
        return new Trigger(getId(), getTypeUID(), configuration, label, description);
    }

}
