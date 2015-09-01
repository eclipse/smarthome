/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.core.library.types;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Kai Kreuzer - Initial contribution and API
 */
public class PercentTypeTest {

    @Test(expected = IllegalArgumentException.class)
    public void negativeNumber() {
        new PercentType(-3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void MoreThan100() {
        new PercentType("100.2");
    }

    @Test
    public void DoubleValue() {
        PercentType pt = new PercentType("0.0001");
        Assert.assertEquals("0.0001", pt.toString());
    }

    @Test
    public void IntValue() {
        PercentType pt = new PercentType(100);
        Assert.assertEquals("100", pt.toString());
    }

    @Test
    public void testEquals() {
        PercentType pt1 = new PercentType(new Integer(100));
        PercentType pt2 = new PercentType("100.0");
        PercentType pt3 = new PercentType(0);
        PercentType pt4 = new PercentType(0);

        Assert.assertEquals(true, pt1.equals(pt2));
        Assert.assertEquals(true, pt3.equals(pt4));
        Assert.assertEquals(false, pt3.equals(pt1));
    }
}
