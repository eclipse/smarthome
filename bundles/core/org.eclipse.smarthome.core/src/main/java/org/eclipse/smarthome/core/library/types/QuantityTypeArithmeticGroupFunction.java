/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.core.library.types;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

import javax.measure.Quantity;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.items.GroupFunction;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;

/**
 *
 * @author Henning Treu - initial contribution
 *
 */
public interface QuantityTypeArithmeticGroupFunction extends GroupFunction {

    abstract class DimensionalGroupFunction implements GroupFunction {

        protected final Class<? extends Quantity<?>> dimension;

        public DimensionalGroupFunction(Class<? extends Quantity<?>> dimension) {
            this.dimension = dimension;
        }

        @Override
        public State getStateAs(Set<Item> items, Class<? extends State> stateClass) {
            State state = calculate(items);
            if (stateClass.isInstance(state)) {
                return state;
            } else {
                return null;
            }
        }

        @Override
        public State[] getParameters() {
            return new State[0];
        }

    }

    /**
     * This calculates the numeric average over all item states of decimal type.
     */
    static class Avg extends DimensionalGroupFunction {

        public Avg(@NonNull Class<? extends Quantity<?>> dimension) {
            super(dimension);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public State calculate(Set<Item> items) {
            if (items == null || items.size() <= 0) {
                return UnDefType.UNDEF;
            }

            QuantityType<?> sum = null;
            int count = 0;
            for (Item item : items) {
                if (item instanceof NumberItem && dimension.equals(((NumberItem) item).getDimension())) {
                    QuantityType itemState = (QuantityType) item.getStateAs(QuantityType.class);
                    if (itemState != null) {
                        if (sum == null) {
                            sum = itemState; // initialise the sum from the first item
                            count++;
                        } else {
                            sum = sum.add(itemState);
                            count++;
                        }
                    }
                }
            }

            if (sum != null && count > 0) {
                BigDecimal result = sum.toBigDecimal().divide(BigDecimal.valueOf(count), RoundingMode.HALF_UP);
                return new QuantityType<>(result.doubleValue(), sum.getUnit());
            }

            return UnDefType.UNDEF;
        }

    }

    /**
     * This calculates the numeric sum over all item states of decimal type.
     */
    static class Sum extends DimensionalGroupFunction {

        public Sum(@NonNull Class<? extends @NonNull Quantity<?>> dimension) {
            super(dimension);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public State calculate(Set<Item> items) {
            if (items == null || items.size() <= 0) {
                return UnDefType.UNDEF;
            }

            QuantityType<?> sum = null;
            for (Item item : items) {
                if (item instanceof NumberItem && dimension.equals(((NumberItem) item).getDimension())) {
                    QuantityType itemState = (QuantityType) item.getStateAs(QuantityType.class);
                    if (itemState != null) {
                        if (sum == null) {
                            sum = itemState; // initialise the sum from the first item
                        } else if (sum.getUnit().isCompatible(itemState.getUnit())) {
                            sum = sum.add(itemState);
                        }
                    }
                }
            }

            return sum != null ? sum : UnDefType.UNDEF;
        }

    }

    /**
     * This calculates the minimum value of all item states of decimal type.
     */
    static class Min extends DimensionalGroupFunction {

        public Min(@NonNull Class<? extends @NonNull Quantity<?>> dimension) {
            super(dimension);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public State calculate(Set<Item> items) {
            if (items == null || items.size() <= 0) {
                return UnDefType.UNDEF;
            }

            QuantityType<?> min = null;
            for (Item item : items) {
                if (item instanceof NumberItem && dimension.equals(((NumberItem) item).getDimension())) {
                    QuantityType itemState = (QuantityType) item.getStateAs(QuantityType.class);
                    if (itemState != null) {
                        if (min == null
                                || (min.getUnit().isCompatible(itemState.getUnit()) && min.compareTo(itemState) > 0)) {
                            min = itemState;
                        }
                    }
                }
            }
            return min != null ? min : UnDefType.UNDEF;
        }

    }

    /**
     * This calculates the maximum value of all item states of decimal type.
     */
    static class Max extends DimensionalGroupFunction {

        public Max(@NonNull Class<? extends @NonNull Quantity<?>> dimension) {
            super(dimension);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public State calculate(Set<Item> items) {
            if (items == null || items.size() <= 0) {
                return UnDefType.UNDEF;
            }

            QuantityType<?> max = null;
            for (Item item : items) {
                if (item instanceof NumberItem && dimension.equals(((NumberItem) item).getDimension())) {
                    QuantityType itemState = (QuantityType) item.getStateAs(QuantityType.class);
                    if (itemState != null) {
                        if (max == null
                                || (max.getUnit().isCompatible(itemState.getUnit()) && max.compareTo(itemState) < 0)) {
                            max = itemState;
                        }
                    }
                }
            }

            return max != null ? max : UnDefType.UNDEF;
        }

    }
}
