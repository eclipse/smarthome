/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
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
package org.eclipse.smarthome.binding.hue.internal;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.reflect.TypeToken;

/**
 * Basic light information.
 *
 * @author Q42, standalone Jue library (https://github.com/Q42/Jue)
 * @author Denis Dudnik - moved Jue library source code inside the smarthome Hue binding
 * @author Samuel Leisering - introduced Sensor support, renamed supertype to HueObject
 */
public class HueObject {
    public static final Type GSON_TYPE = new TypeToken<Map<String, HueObject>>() {
    }.getType();

    private String id;
    private String name;

    HueObject() {
    }

    void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the id of the light.
     *
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the name of the light.
     *
     * @return name
     */
    public String getName() {
        return name;
    }
}
