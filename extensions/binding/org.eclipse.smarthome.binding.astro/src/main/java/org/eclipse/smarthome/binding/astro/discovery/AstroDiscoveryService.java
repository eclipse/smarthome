/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.astro.discovery;

import static org.eclipse.smarthome.binding.astro.AstroBindingConstants.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.i18n.LocationProvider;
import org.eclipse.smarthome.core.library.types.PointType;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AstroDiscoveryService} creates things based on the configured location.
 *
 * @author Gerhard Riegler - Initial Contribution
 * @author Stefan Triller - Use configured location
 */
public class AstroDiscoveryService extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(AstroDiscoveryService.class);
    private static final int DISCOVER_TIMEOUT_SECONDS = 30;
    private static final int LOCATION_CHANGED_CHECK_INTERVAL = 60;
    private LocationProvider locationProvider;
    private ScheduledFuture<?> astroDiscoveryJob;
    private PointType previousLocation;

    private static ThingUID SUN_THING = new ThingUID(THING_TYPE_SUN, LOCAL);
    private static ThingUID MOON_THING = new ThingUID(THING_TYPE_MOON, LOCAL);

    /**
     * Creates a AstroDiscoveryService with enabled autostart.
     */
    public AstroDiscoveryService() {
        super(new HashSet<>(Arrays.asList(new ThingTypeUID(BINDING_ID, "-"))), DISCOVER_TIMEOUT_SECONDS, true);
    }

    @Override
    protected void startScan() {
        logger.debug("Starting Astro discovery scan");
        PointType location = locationProvider.getLocation();
        if (location == null) {
            logger.debug("LocationProvider.getLocation() is not set -> Will not provide any discovery results");
            return;
        }
        createResults(location);
    }

    @Override
    protected void startBackgroundDiscovery() {
        if (astroDiscoveryJob == null) {
            astroDiscoveryJob = scheduler.scheduleAtFixedRate(() -> {
                PointType currentLocation = locationProvider.getLocation();
                if ((currentLocation != null) && !currentLocation.equals(previousLocation)) {
                    logger.info("Location has been changed from {} to {}: Creating new Discovery Results",
                            previousLocation, currentLocation);
                    createResults(currentLocation);
                    previousLocation = currentLocation;
                }
            }, 0, LOCATION_CHANGED_CHECK_INTERVAL, TimeUnit.SECONDS);
            logger.debug("Scheduled astro location-changed job every {} seconds", LOCATION_CHANGED_CHECK_INTERVAL);
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("Stop Astro device background discovery");
        if (astroDiscoveryJob != null && !astroDiscoveryJob.isCancelled()) {
            astroDiscoveryJob.cancel(true);
            astroDiscoveryJob = null;
        }
    }

    public void createResults(PointType location) {
        String propGeolocation;
        if (location.getAltitude() != null) {
            propGeolocation = String.format("%s,%s,%s", location.getLatitude(), location.getLongitude(),
                    location.getAltitude());
        } else {
            propGeolocation = String.format("%s,%s", location.getLatitude(), location.getLongitude());
        }
        thingDiscovered(DiscoveryResultBuilder.create(SUN_THING).withLabel("Local Sun")
                .withProperty("geolocation", propGeolocation).build());
        thingDiscovered(DiscoveryResultBuilder.create(MOON_THING).withLabel("Local Moon")
                .withProperty("geolocation", propGeolocation).build());
    }

    protected void setLocationProvider(LocationProvider locationProvider) {
        this.locationProvider = locationProvider;
    }

    protected void unsetLocationProvider(LocationProvider locationProvider) {
        this.locationProvider = null;
    }

}
