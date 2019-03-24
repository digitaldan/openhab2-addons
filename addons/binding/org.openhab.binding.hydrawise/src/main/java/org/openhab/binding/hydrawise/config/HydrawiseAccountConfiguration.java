/**
 * Copyright (c) 2014,2018 by the respective copyright holders.
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
package org.openhab.binding.hydrawise.config;

/**
 * The {@link HydrawiseAccountConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Dan Cunningham - Initial contribution
 */
public class HydrawiseAccountConfiguration {

    /**
     * Customer API key {@link https://app.hydrawise.com/config/account}
     */
    public String apiKey;

    /**
     * refresh interval in seconds.
     */
    public int refresh;
}
