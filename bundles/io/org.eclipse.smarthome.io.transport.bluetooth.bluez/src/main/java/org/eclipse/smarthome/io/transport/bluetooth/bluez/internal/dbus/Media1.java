/**
 * Copyright (c) 1997, 2015 by Huawei Technologies Co., Ltd. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.io.transport.bluetooth.bluez.internal.dbus;

import java.util.Map;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusInterfaceName;
import org.freedesktop.dbus.Variant;

@DBusInterfaceName("org.bluez.Media1")
public interface Media1 extends DBusInterface {

    public void RegisterEndpoint(DBusInterface endpoint, Map<String, Variant> properties);

    public void UnregisterEndpoint(DBusInterface endpoint);

    public void RegisterPlayer(DBusInterface player, Map<String, Variant> properties);

    public void UnregisterPlayer(DBusInterface player);

}
