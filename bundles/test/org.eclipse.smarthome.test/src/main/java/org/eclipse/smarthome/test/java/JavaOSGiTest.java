/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.test.java;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.smarthome.core.autoupdate.AutoUpdateBindingConfigProvider;
import org.eclipse.smarthome.test.OSGiTest;
import org.eclipse.smarthome.test.storage.VolatileStorageService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * {@link JavaOSGiTest} is an abstract base class for OSGi based tests. It provides convenience methods to register and
 * unregister mocks as OSGi services. All services, which are registered through the {@link OSGiTest#registerService}
 * methods, are unregistered automatically in the tear down of the test.
 *
 * @author Markus Rathgeb - Create a pure Java implementation based on the Groovy {@link OSGiTest} class
 */
public class JavaOSGiTest {

    protected final int DFL_TIMEOUT = 10000;
    protected final int DFL_SLEEP_TIME = 50;

    private final Map<String, ServiceRegistration<?>> registeredServices = new HashMap<>();
    BundleContext bundleContext;

    @Before
    public void bindBundleContext() {
        bundleContext = getBundleContext();
        assertThat(bundleContext, is(notNullValue()));
    }

    /**
     * Get the {@link BundleContext}, which is used for registration and unregistration of OSGi services.
     *
     * <p>
     * By default it uses the bundle context of the test class itself. This method can be overridden by concrete
     * implementations to provide another bundle context.
     *
     * @return bundle context
     */
    private BundleContext getBundleContext() {
        final Bundle bundle = FrameworkUtil.getBundle(this.getClass());
        if (bundle != null) {
            return bundle.getBundleContext();
        } else {
            return null;
        }
    }

    private <T> T unrefService(final ServiceReference<T> serviceReference) {
        if (serviceReference == null) {
            return null;
        } else {
            return bundleContext.getService(serviceReference);
        }
    }

    /**
     * Get an OSGi service for the given class.
     *
     * @param clazz class under which the OSGi service is registered
     * @return OSGi service or null if no service can be found for the given class
     */
    protected <T> T getService(Class<T> clazz) {
        @SuppressWarnings("unchecked")
        final ServiceReference<T> serviceReference = (ServiceReference<T>) bundleContext
                .getServiceReference(clazz.getName());

        return unrefService(serviceReference);
    }

    /**
     * Get an OSGi service for the given class and the given filter.
     *
     * @param clazz class under which the OSGi service is registered
     * @param filter
     * @return OSGi service or null if no service can be found for the given class
     */
    protected <T> T getService(Class<T> clazz, Predicate<ServiceReference<T>> filter) {
        final ServiceReference<T> serviceReferences[] = getServices(clazz);

        if (serviceReferences == null) {
            return null;
        }
        final List<T> filteredServiceReferences = new ArrayList<>(serviceReferences.length);
        for (final ServiceReference<T> serviceReference : serviceReferences) {
            if (filter.test(serviceReference)) {
                filteredServiceReferences.add(unrefService(serviceReference));
            }
        }

        if (filteredServiceReferences.size() > 1) {
            Assert.fail("More than 1 service matching the filter is registered.");
        }
        if (filteredServiceReferences.isEmpty()) {
            return null;
        } else {
            return filteredServiceReferences.get(0);
        }
    }

    private <T> ServiceReference<T>[] getServices(final Class<T> clazz) {
        try {
            @SuppressWarnings("unchecked")
            ServiceReference<T> serviceReferences[] = (ServiceReference<T>[]) bundleContext
                    .getServiceReferences(clazz.getName(), null);
            return serviceReferences;
        } catch (InvalidSyntaxException e) {
            throw new Error("Invalid exception for a null filter");
        }
    }

    /**
     * Get an OSGi service for the given class and the given filter.
     *
     * @param clazz class under which the OSGi service is registered
     * @param filter
     * @return OSGi service or null if no service can be found for the given class
     */
    protected <T, I extends T> I getService(Class<T> clazz, Class<I> implementationClass) {
        @SuppressWarnings("unchecked")
        final I service = (I) getService(clazz, srvRef -> implementationClass.isInstance(unrefService(srvRef)));
        return service;
    }

    /**
     * Register the given object as OSGi service.
     *
     * <p>
     * The first interface is used as OSGi service interface name.
     *
     * @param service service to be registered
     * @return service registration object
     */
    protected ServiceRegistration<?> registerService(final Object service) {
        return registerService(service, getInterfaceName(service), null);
    }

    /**
     * Register the given object as OSGi service. The first interface is used as OSGi service interface name.
     *
     * @param service service to be registered
     * @param properties OSGi service properties
     * @return service registration object
     */
    protected ServiceRegistration<?> registerService(final Object service, final Dictionary<String, ?> properties) {
        return registerService(service, getInterfaceName(service), properties);
    }

    /**
     * Register the given object as OSGi service.
     *
     * <p>
     * The given interface name is used as OSGi service interface name.
     *
     * @param service service to be registered
     * @param interfaceName interface name of the OSGi service
     * @return service registration object
     */
    protected ServiceRegistration<?> registerService(final Object service, final String interfaceName) {
        return registerService(service, interfaceName, null);
    }

    /**
     * Register the given object as OSGi service.
     *
     * <p>
     * The given interface name is used as OSGi service interface name.
     *
     * @param service service to be registered
     * @param interfaceName interface name of the OSGi service
     * @param properties OSGi service properties
     * @return service registration object
     */
    protected ServiceRegistration<?> registerService(final Object service, final String interfaceName,
            final Dictionary<String, ?> properties) {
        assertThat(interfaceName, is(notNullValue()));
        final ServiceRegistration<?> srvReg = bundleContext.registerService(interfaceName, service, properties);
        registeredServices.put(interfaceName, srvReg);
        return srvReg;
    }

    /**
     * Register the given object as OSGi service.
     *
     * <p>
     * The given interface names are used as OSGi service interface name.
     *
     * @param service service to be registered
     * @param interfaceName interface name of the OSGi service
     * @param properties OSGi service properties
     * @return service registration object
     */
    protected ServiceRegistration<?> registerService(final Object service, final String[] interfaceNames,
            final Dictionary<String, ?> properties) {
        assertThat(interfaceNames, is(notNullValue()));

        final ServiceRegistration<?> srvReg = bundleContext.registerService(interfaceNames, service, properties);

        for (final String interfaceName : interfaceNames) {
            registeredServices.put(interfaceName, srvReg);
        }

        return srvReg;
    }

    /**
     * Unregister an OSGi service by the given object, that was registered before.
     *
     * <p>
     * The interface name is taken from the first interface of the service object.
     *
     * @param service the service
     * @return the service registration that was unregistered or null if no service could be found
     */
    protected ServiceRegistration<?> unregisterService(final Object service) {
        return unregisterService(getInterfaceName(service));
    }

    /**
     * Unregister an OSGi service by the given object, that was registered before.
     *
     * @param interfaceName the interface name of the service
     * @return the service registration that was unregistered or null if no service could be found
     */
    protected ServiceRegistration<?> unregisterService(final String interfaceName) {
        final ServiceRegistration<?> reg = registeredServices.remove(interfaceName);
        if (reg != null) {
            reg.unregister();
            Iterator<ServiceRegistration<?>> regs = registeredServices.values().iterator();
            while (regs.hasNext()) {
                final ServiceRegistration<?> otherReg = regs.next();
                if (otherReg == reg) {
                    regs.remove();
                }
            }
        }
        return reg;
    }

    /**
     * Wait until the condition is fulfilled or the timeout is reached.
     *
     * <p>
     * This method uses the default timing parameters.
     *
     * @param condition the condition to check
     * @return true on success, false on timeout
     */
    protected boolean waitFor(BooleanSupplier condition) {
        return waitFor(condition, DFL_TIMEOUT, DFL_SLEEP_TIME);
    }

    /**
     * Wait until the condition is fulfilled or the timeout is reached.
     *
     * @param condition the condition to check
     * @param timeout timeout
     * @param sleepTime interval for checking the condition
     * @return true on success, false on timeout
     */
    protected boolean waitFor(BooleanSupplier condition, int timeout, int sleepTime) {
        int waitingTime = 0;
        boolean rv;
        while (!(rv = condition.getAsBoolean()) && waitingTime < timeout) {
            waitingTime += sleepTime;
            internalSleep(sleepTime);
        }
        return rv;
    }

    /**
     * Wait until the assertion is fulfilled or the timeout is reached.
     *
     * <p>
     * This method uses the default timing parameters.
     *
     * @param assertion closure that must not have an argument
     */
    protected void waitForAssert(Runnable assertion) {
        waitForAssert(assertion, null, DFL_TIMEOUT, DFL_SLEEP_TIME);
    }

    /**
     * Wait until the assertion is fulfilled or the timeout is reached.
     *
     * @param assertion the logic to execute
     * @param timeout timeout
     * @param sleepTime interval for checking the condition
     */
    protected void waitForAssert(Runnable assertion, int timeout, int sleepTime) {
        waitForAssert(assertion, null, timeout, sleepTime);
    }

    /**
     * Wait until the assertion is fulfilled or the timeout is reached.
     *
     * <p>
     * This method uses the default timing parameters.
     *
     * @param assertion the logic to execute
     * @return the return value of the supplied assertion object's function on success
     */
    protected <T> T waitForAssert(Supplier<T> assertion) {
        return waitForAssert(assertion, null, DFL_TIMEOUT, DFL_SLEEP_TIME);
    }

    /**
     * Wait until the assertion is fulfilled or the timeout is reached.
     *
     * @param assertion the logic to execute
     * @param timeout timeout
     * @param sleepTime interval for checking the condition
     * @return the return value of the supplied assertion object's function on success
     */
    protected <T> T waitForAssert(Supplier<T> assertion, int timeout, int sleepTime) {
        return waitForAssert(assertion, null, timeout, sleepTime);
    }

    /**
     * Wait until the assertion is fulfilled or the timeout is reached.
     *
     * @param assertion the logic to execute
     * @param beforeLastCall logic to execute in front of the last call to ${code assertion}
     * @param sleepTime interval for checking the condition
     */
    protected void waitForAssert(Runnable assertion, Runnable beforeLastCall, int timeout, int sleepTime) {
        int waitingTime = 0;
        while (waitingTime < timeout) {
            try {
                assertion.run();
                return;
            } catch (final Error | NullPointerException error) {
                waitingTime += sleepTime;
                internalSleep(sleepTime);
            }
        }
        if (beforeLastCall != null) {
            beforeLastCall.run();
        }
        assertion.run();
    }

    /**
     * Wait until the assertion is fulfilled or the timeout is reached.
     *
     * @param assertion the logic to execute
     * @param beforeLastCall logic to execute in front of the last call to ${code assertion}
     * @param sleepTime interval for checking the condition
     * @return the return value of the supplied assertion object's function on success
     */
    protected <T> T waitForAssert(Supplier<T> assertion, Runnable beforeLastCall, long timeout, int sleepTime) {
        final long timeoutNs = TimeUnit.MILLISECONDS.toNanos(timeout);
        final long startingTime = System.nanoTime();
        while (System.nanoTime() - startingTime < timeoutNs) {
            try {
                return assertion.get();
            } catch (final Error | NullPointerException error) {
                internalSleep(sleepTime);
            }
        }
        if (beforeLastCall != null) {
            beforeLastCall.run();
        }
        return assertion.get();
    }

    /**
     * Returns the interface name for a given service object by choosing the first interface.
     *
     * @param service service object
     * @return name of the first interface or null if the object has no interfaces
     */
    protected String getInterfaceName(final Object service) {
        Class<?>[] classes = service.getClass().getInterfaces();
        if (classes.length >= 1) {
            return classes[0].getName();
        } else {
            return null;
        }
    }

    /**
     * Registers a volatile storage service.
     */
    protected void registerVolatileStorageService() {
        registerService(new VolatileStorageService());
    }

    @After
    public void unregisterMocks() {
        registeredServices.forEach((interfaceName, service) -> service.unregister());
        registeredServices.clear();
    }

    /**
     * Inject a service to disable the auto-update feature.
     */
    protected void disableItemAutoUpdate() {
        registerService(new AutoUpdateBindingConfigProvider() {

            @Override
            public Boolean autoUpdate(String itemName) {
                return false;
            }
        });
    }

    /**
     * Inject a service to enable the auto-update feature.
     */
    protected void enableItemAutoUpdate() {
        registerService(new AutoUpdateBindingConfigProvider() {

            @Override
            public Boolean autoUpdate(String itemName) {
                return true;
            }
        });
    }

    private void internalSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new Error("We shouldn't be interrupted while testing");
        }
    }

}
