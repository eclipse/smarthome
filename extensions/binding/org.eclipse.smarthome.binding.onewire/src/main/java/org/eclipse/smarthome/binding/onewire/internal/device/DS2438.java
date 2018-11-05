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
package org.eclipse.smarthome.binding.onewire.internal.device;

import static org.eclipse.smarthome.binding.onewire.internal.OwBindingConstants.*;
import static org.eclipse.smarthome.core.library.unit.MetricPrefix.MILLI;

import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.ElectricCurrent;
import javax.measure.quantity.ElectricPotential;
import javax.measure.quantity.Illuminance;
import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.binding.onewire.internal.OwException;
import org.eclipse.smarthome.binding.onewire.internal.Util;
import org.eclipse.smarthome.binding.onewire.internal.handler.OwBaseBridgeHandler;
import org.eclipse.smarthome.binding.onewire.internal.handler.OwBaseThingHandler;
import org.eclipse.smarthome.binding.onewire.internal.owserver.OwserverDeviceParameter;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link DS2438} class defines an DS2438 device
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class DS2438 extends AbstractOwDevice {
    private final Logger logger = LoggerFactory.getLogger(DS2438.class);

    public enum LightSensorType {
        ELABNET_V1,
        ELABNET_V2,
        IBUTTONLINK
    }

    public enum CurrentSensorType {
        INTERNAL,
        IBUTTONLINK
    }

    private LightSensorType lightSensorType = LightSensorType.ELABNET_V1;
    private CurrentSensorType currentSensorType = CurrentSensorType.INTERNAL;

    private static final OwDeviceParameterMap TEMPERATURE_PARAMETER = new OwDeviceParameterMap() {
        {
            set(THING_TYPE_OWSERVER, new OwserverDeviceParameter("/temperature"));
        }
    };

    private static final OwDeviceParameterMap HUMIDITY_PARAMETER = new OwDeviceParameterMap() {
        {
            set(THING_TYPE_OWSERVER, new OwserverDeviceParameter("/humidity"));
        }
    };

    private static final OwDeviceParameterMap VOLTAGE_PARAMETER = new OwDeviceParameterMap() {
        {
            set(THING_TYPE_OWSERVER, new OwserverDeviceParameter("/VAD"));
        }
    };

    private static final OwDeviceParameterMap CURRENT_PARAMETER = new OwDeviceParameterMap() {
        {
            set(THING_TYPE_OWSERVER, new OwserverDeviceParameter("/vis"));
        }
    };

    private static final OwDeviceParameterMap SUPPLY_VOLTAGE_PARAMETER = new OwDeviceParameterMap() {
        {
            set(THING_TYPE_OWSERVER, new OwserverDeviceParameter("/VDD"));
        }
    };

    public DS2438(String sensorId, OwBaseThingHandler callback) {
        super(sensorId, callback);
    }

    @Override
    public void configureChannels() {
        Thing thing = callback.getThing();

        Channel humidityChannel = thing.getChannel(CHANNEL_HUMIDITY);
        if (humidityChannel != null) {
            Configuration channelConfiguration = humidityChannel.getConfiguration();
            if (channelConfiguration.get(CONFIG_HUMIDITY) != null) {
                HUMIDITY_PARAMETER.set(THING_TYPE_OWSERVER,
                        new OwserverDeviceParameter((String) channelConfiguration.get(CONFIG_HUMIDITY)));
            } else {
                HUMIDITY_PARAMETER.set(THING_TYPE_OWSERVER, new OwserverDeviceParameter("/humidity"));
            }
        }

        isConfigured = true;
    }

    @Override
    public void refresh(OwBaseBridgeHandler bridgeHandler, Boolean forcedRefresh) throws OwException {
        if (isConfigured) {
            double Vcc = 5.0;

            if (enabledChannels.contains(CHANNEL_TEMPERATURE) || enabledChannels.contains(CHANNEL_HUMIDITY)
                    || enabledChannels.contains(CHANNEL_ABSOLUTE_HUMIDITY)
                    || enabledChannels.contains(CHANNEL_DEWPOINT)) {
                QuantityType<Temperature> temperature = new QuantityType<Temperature>(
                        (DecimalType) bridgeHandler.readDecimalType(sensorId, TEMPERATURE_PARAMETER), SIUnits.CELSIUS);
                logger.trace("read temperature {} from {}", temperature, sensorId);

                if (enabledChannels.contains(CHANNEL_TEMPERATURE)) {
                    callback.postUpdate(CHANNEL_TEMPERATURE, temperature);
                }

                if (enabledChannels.contains(CHANNEL_HUMIDITY) || enabledChannels.contains(CHANNEL_ABSOLUTE_HUMIDITY)
                        || enabledChannels.contains(CHANNEL_DEWPOINT)) {
                    QuantityType<Dimensionless> humidity = new QuantityType<Dimensionless>(
                            (DecimalType) bridgeHandler.readDecimalType(sensorId, HUMIDITY_PARAMETER),
                            SmartHomeUnits.PERCENT);
                    logger.trace("read humidity {} from {}", humidity, sensorId);

                    if (enabledChannels.contains(CHANNEL_HUMIDITY)) {
                        callback.postUpdate(CHANNEL_HUMIDITY, humidity);
                    }

                    if (enabledChannels.contains(CHANNEL_ABSOLUTE_HUMIDITY)) {
                        callback.postUpdate(CHANNEL_ABSOLUTE_HUMIDITY,
                                Util.calculateAbsoluteHumidity(temperature, humidity));
                    }

                    if (enabledChannels.contains(CHANNEL_DEWPOINT)) {
                        callback.postUpdate(CHANNEL_DEWPOINT, Util.calculateDewpoint(temperature, humidity));
                    }
                }
            }

            if (enabledChannels.contains(CHANNEL_VOLTAGE)) {
                State voltage = new QuantityType<ElectricPotential>(
                        (DecimalType) bridgeHandler.readDecimalType(sensorId, VOLTAGE_PARAMETER), SmartHomeUnits.VOLT);
                logger.trace("read voltage {} from {}", voltage, sensorId);
                callback.postUpdate(CHANNEL_VOLTAGE, voltage);
            }

            if (enabledChannels.contains(CHANNEL_CURRENT)) {
                if (currentSensorType == CurrentSensorType.IBUTTONLINK) {
                    State current = bridgeHandler.readDecimalType(sensorId, VOLTAGE_PARAMETER);
                    if (current instanceof DecimalType) {
                        double currentDouble = ((DecimalType) current).doubleValue();
                        if (currentDouble >= 0.1 || currentDouble <= 3.78) {
                            current = new QuantityType<ElectricCurrent>(currentDouble * 5.163 + 0.483,
                                    SmartHomeUnits.AMPERE);
                        }
                        callback.postUpdate(CHANNEL_CURRENT, current);
                    } else {
                        callback.postUpdate(CHANNEL_CURRENT, UnDefType.UNDEF);
                    }
                } else {
                    State current = new QuantityType<ElectricCurrent>(
                            (DecimalType) bridgeHandler.readDecimalType(sensorId, CURRENT_PARAMETER),
                            MILLI(SmartHomeUnits.AMPERE));
                    callback.postUpdate(CHANNEL_CURRENT, current);
                }
            }

            if (enabledChannels.contains(CHANNEL_SUPPLYVOLTAGE)) {
                Vcc = ((DecimalType) bridgeHandler.readDecimalType(sensorId, SUPPLY_VOLTAGE_PARAMETER)).doubleValue();
                State supplyVoltage = new QuantityType<ElectricPotential>(Vcc, SmartHomeUnits.VOLT);
                callback.postUpdate(CHANNEL_SUPPLYVOLTAGE, supplyVoltage);
            }

            if (enabledChannels.contains(CHANNEL_LIGHT)) {
                switch (lightSensorType) {
                    case ELABNET_V2:
                        State light = bridgeHandler.readDecimalType(sensorId, CURRENT_PARAMETER);
                        if (light instanceof DecimalType) {
                            light = new QuantityType<Illuminance>(
                                    Math.round(Math.pow(10, ((DecimalType) light).doubleValue() / 47 * 1000)),
                                    SmartHomeUnits.LUX);
                            callback.postUpdate(CHANNEL_LIGHT, light);
                        }
                        break;
                    case ELABNET_V1:
                        light = bridgeHandler.readDecimalType(sensorId, CURRENT_PARAMETER);
                        if (light instanceof DecimalType) {
                            light = new QuantityType<Illuminance>(Math.round(Math
                                    .exp(1.059 * Math.log(1000000 * ((DecimalType) light).doubleValue() / (4096 * 390))
                                            + 4.518)
                                    * 20000), SmartHomeUnits.LUX);
                            callback.postUpdate(CHANNEL_LIGHT, light);
                        }
                        break;
                    case IBUTTONLINK:
                        light = bridgeHandler.readDecimalType(sensorId, VOLTAGE_PARAMETER);
                        if (light instanceof DecimalType) {
                            light = new QuantityType<Illuminance>(
                                    Math.pow(10, (65 / 7.5) - (47 / 7.5) * (Vcc / ((DecimalType) light).doubleValue())),
                                    SmartHomeUnits.LUX);
                            callback.postUpdate(CHANNEL_LIGHT, light);
                        }
                }
            }
        }
    }

    /**
     * set the type of the attached light sensor
     *
     * @param lightSensorType
     */
    public void setLightSensorType(LightSensorType lightSensorType) {
        this.lightSensorType = lightSensorType;
    }

    /**
     * set the type of the attached current sensor
     *
     * @param currentSensorType
     */
    public void setCurrentSensorType(CurrentSensorType currentSensorType) {
        this.currentSensorType = currentSensorType;
    }

}
