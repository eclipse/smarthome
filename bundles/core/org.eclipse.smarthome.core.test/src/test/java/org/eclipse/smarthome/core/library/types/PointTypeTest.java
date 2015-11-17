/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.core.library.types;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Gaël L'hopital
 */
public class PointTypeTest {

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorEmpty() {
        @SuppressWarnings("unused")
        PointType errorGenerator = new PointType("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorBadlyFormated() {
        @SuppressWarnings("unused")
        PointType errorGenerator = new PointType("2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorBadlyFormated2() {
        @SuppressWarnings("unused")
        PointType errorGenerator = new PointType("2,");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorBadlyFormated3() {
        @SuppressWarnings("unused")
        PointType errorGenerator = new PointType("2,3,4,5");
    }

    @Test
    public void testDistance() {
        // Ensure presence of default constructor
        PointType midleOfTheOcean = new PointType();
        assertEquals(0, midleOfTheOcean.getLongitude().doubleValue(), 0.0000001);
        assertEquals(0, midleOfTheOcean.getLatitude().doubleValue(), 0.0000001);
        assertEquals(0, midleOfTheOcean.getAltitude().doubleValue(), 0.0000001);

        PointType pointParis = new PointType("48.8566140,2.3522219");

        assertEquals(2.3522219, pointParis.getLongitude().doubleValue(), 0.0000001);
        assertEquals(48.856614, pointParis.getLatitude().doubleValue(), 0.0000001);

        double gravParis = pointParis.getGravity().doubleValue();
        assertEquals(gravParis, 9.809, 0.001);

        // Check canonization of position
        PointType point3 = new PointType("-100,200");
        double lat3 = point3.getLatitude().doubleValue();
        double lon3 = point3.getLongitude().doubleValue();
        assertTrue(lat3 > -90);
        assertTrue(lat3 < 90);
        assertTrue(lon3 < 180);
        assertTrue(lon3 > -180);

        PointType pointTest1 = new PointType("48.8566140,2.3522219,118");
        PointType pointTest2 = new PointType(pointTest1.toString());
        assertEquals(pointTest1.getAltitude().longValue(), pointTest2.getAltitude().longValue());
        assertEquals(pointTest1.getLatitude().longValue(), pointTest2.getLatitude().longValue());
        assertEquals(pointTest1.getLongitude().longValue(), pointTest2.getLongitude().longValue());
    }

}
