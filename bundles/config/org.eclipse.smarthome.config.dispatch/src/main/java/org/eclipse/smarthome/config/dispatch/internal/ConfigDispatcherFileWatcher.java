/**
 * Copyright (c) 2014,2017 Contributors to the Eclipse Foundation
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
package org.eclipse.smarthome.config.dispatch.internal;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;

import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.core.service.AbstractWatchService;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Watches file-system events and passes them to our {@link ConfigDispatcher}
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Stefan Triller - factored out this code from {@link ConfigDispatcher}
 *
 */
@Component(immediate = true)
public class ConfigDispatcherFileWatcher extends AbstractWatchService {

    /** The program argument name for setting the service config directory path */
    final static public String SERVICEDIR_PROG_ARGUMENT = "smarthome.servicedir";

    /** The default folder name of the configuration folder of services */
    final static public String SERVICES_FOLDER = "services";

    private ConfigDispatcher configDispatcher;

    private ConfigurationAdmin configAdmin;

    public ConfigDispatcherFileWatcher() {
        super(getPathToWatch());
    }

    private static String getPathToWatch() {
        String progArg = System.getProperty(SERVICEDIR_PROG_ARGUMENT);
        if (progArg != null) {
            return ConfigConstants.getConfigFolder() + File.separator + progArg;
        } else {
            return ConfigConstants.getConfigFolder() + File.separator + SERVICES_FOLDER;
        }
    }

    @Activate
    public void activate(BundleContext bundleContext) {
        super.activate();
        configDispatcher = new ConfigDispatcher(bundleContext, configAdmin);
        configDispatcher.processConfigFile(getSourcePath().toFile());
    }

    @Deactivate
    @Override
    public void deactivate() {
        super.deactivate();

    }

    @Override
    protected boolean watchSubDirectories() {
        return false;
    }

    @Override
    protected Kind<?>[] getWatchEventKinds(Path subDir) {
        return new Kind<?>[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY };
    }

    @Override
    protected void processWatchEvent(WatchEvent<?> event, Kind<?> kind, Path path) {
        if (kind == ENTRY_CREATE || kind == ENTRY_MODIFY) {
            File f = path.toFile();
            if (!f.isHidden()) {
                configDispatcher.processConfigFile(f);
            }
        } else if (kind == ENTRY_DELETE) {
            // Detect if a service specific configuration file was removed. We want to
            // notify the service in this case with an updated empty configuration.
            File configFile = path.toFile();
            if (configFile.isHidden() || configFile.isDirectory() || !configFile.getName().endsWith(".cfg")) {
                return;
            }
            configDispatcher.fileRemoved(configFile.getAbsolutePath());
        }
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    protected void setConfigurationAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    protected void unsetConfigurationAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = null;
    }

}
