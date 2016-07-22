/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.core.persistence;

/**
 * This class provides information about an item that is stored in a persistence service.
 * It is used to return information about the item to the system
 *
 * @author Chris Jackson - Initial contribution
 *
 */
public interface PersistenceItemInfo {
    /**
     * Returns the item name.
     * It should be noted that the item name is as stored in the persistence service and as such may not be linked to an
     * item. This may be the case if the item was removed from the system, but data still exists in the persistence
     * service.
     *
     * @return Item name
     */
    String getName();

    /**
     * Returns the number of rows of data associated with the item
     * Note that this should be used as a guide to the amount of data and may note be 100% accurate. If accurate
     * information is required, the {@link QueryablePersistenceService#query} method should be used.
     *
     * @return count of the number of rows of data.
     */
    Integer getRows();
}
