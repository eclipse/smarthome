/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.core.auth;

/**
 * Definition of authentication given to username after verification of credentials by authentication provider.
 *
 * Each authentication must at least point to some identity (username) and roles.
 */
public interface Authentication {

    String getUsername();

    String[] getRoles();

}
