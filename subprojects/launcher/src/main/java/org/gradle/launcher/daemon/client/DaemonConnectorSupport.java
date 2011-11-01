/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.launcher.daemon.client;

import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;
import org.gradle.launcher.daemon.registry.DaemonInfo;
import org.gradle.launcher.daemon.registry.DaemonRegistry;
import org.gradle.launcher.daemon.context.DaemonContext;
import org.gradle.messaging.remote.internal.ConnectException;
import org.gradle.messaging.remote.internal.Connection;
import org.gradle.messaging.remote.internal.OutgoingConnector;
import org.gradle.util.UncheckedException;

import java.util.Date;
import java.util.List;

/**
 * Provides the general connection mechanics of connecting to a daemon, leaving implementations
 * to define how new daemons should be created if needed.
 * 
 * Subclassing instead of delegation with regard to creating new daemons seems more appropriate
 * as the way that new daemons are launched is likely to be coupled to the DaemonRegistry implementation.
 */
abstract public class DaemonConnectorSupport<T extends DaemonRegistry> implements DaemonConnector {

    private static final Logger LOGGER = Logging.getLogger(DaemonConnectorSupport.class);

    private final T daemonRegistry;
    private final long connectTimout;
    private final Spec<DaemonContext> contextCompatibilitySpec;

    public final static int DEFAULT_CONNECT_TIMEOUT = 30000;

    protected DaemonConnectorSupport(T daemonRegistry) {
        this(daemonRegistry, DEFAULT_CONNECT_TIMEOUT);
    }

    protected DaemonConnectorSupport(T daemonRegistry, int connectTimout) {
        this.daemonRegistry = daemonRegistry;
        this.connectTimout = connectTimout;
        this.contextCompatibilitySpec = getContextCompatibilitySpec();
    }

    public Connection<Object> maybeConnect() {
        return findConnection(daemonRegistry.getAll());
    }

    private Connection<Object> findConnection(List<DaemonInfo> daemonInfos) {
        for (DaemonInfo daemonInfo : daemonInfos) {
            if (!contextCompatibilitySpec.isSatisfiedBy(daemonInfo.getContext())) {
                continue;
            }

            try {
                return getConnector().connect(daemonInfo.getAddress());
            } catch (ConnectException e) {
                //this means the daemon died without removing its address from the registry
                //we can safely remove this address now
                LOGGER.debug("We cannot connect to the daemon at " + daemonInfo.getAddress() + " due to " + e + ". "
                        + "We will not remove this daemon from the registry because the connection issue may have been temporary.");
                //TODO it might be good to store in the registry the number of failed attempts to connect to the deamon
                //if the number is high we may decide to remove the daemon from the registry
                //daemonRegistry.remove(address);
            }
        }
        return null;
    }

    public Connection<Object> connect() {
        Connection<Object> connection = findConnection(daemonRegistry.getIdle());
        if (connection != null) {
            return connection;
        }

        LOGGER.info("Starting Gradle daemon");
        startDaemon();
        Date expiry = new Date(System.currentTimeMillis() + connectTimout);
        do {
            connection = findConnection(daemonRegistry.getIdle());
            if (connection != null) {
                return connection;
            }
            try {
                Thread.sleep(200L);
            } catch (InterruptedException e) {
                throw UncheckedException.asUncheckedException(e);
            }
        } while (System.currentTimeMillis() < expiry.getTime());

        throw new GradleException("Timeout waiting to connect to Gradle daemon.");
    }

    public T getDaemonRegistry() {
        return daemonRegistry;
    }

    protected Spec<DaemonContext> getContextCompatibilitySpec() {
        return Specs.<DaemonContext>satisfyAll();
    }

    abstract protected OutgoingConnector<Object> getConnector();

    abstract protected void startDaemon();
}
