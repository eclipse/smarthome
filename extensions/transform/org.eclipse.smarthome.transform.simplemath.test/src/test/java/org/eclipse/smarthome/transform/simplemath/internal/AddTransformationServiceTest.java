/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.transform.simplemath.internal;

import org.eclipse.smarthome.core.transform.TransformationException;
import org.eclipse.smarthome.core.transform.TransformationService;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Martin van Wingerden - Initial contribution
 */
public class AddTransformationServiceTest {
    private TransformationService subject = new AddTransformationService();

    @Test
    public void testTransform() throws TransformationException {
        String result = subject.transform("20", "100");

        Assert.assertEquals("120", result);
    }

    @Test(expected = TransformationException.class)
    public void testTransformInvalidSource() throws TransformationException {
        subject.transform("20", "*");
    }

    @Test(expected = TransformationException.class)
    public void testTransformInvalidFunction() throws TransformationException {
        subject.transform("*", "90");
    }
}
