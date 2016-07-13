/*
 * Copyright (c) Alexander Kammerer 2015.
 *
 * All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.smarthome.documentation.data;

import org.eclipse.smarthome.documentation.models.Parameter;

/**
 * @author Alexander Kammerer <alexander.kammerer@online.de>
 */
public class ParameterList extends ModelList {
    /**
     * @return Returns a new {@link Parameter} object.
     */
    public Parameter getNewModel() {
        return new Parameter();
    }
}
