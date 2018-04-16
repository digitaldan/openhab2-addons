/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.alexa.internal;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFilter;
import org.eclipse.smarthome.core.events.EventSubscriber;
import org.eclipse.smarthome.core.items.events.ItemStateEvent;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlexaService implements EventSubscriber {

    private Logger logger = LoggerFactory.getLogger(AlexaService.class);

    public AlexaService() {
    }

    protected void activate(BundleContext context, Map<String, ?> config) {
        logger.debug("openHAB Alexa connector activated");
    }

    protected void deactivate() {
        logger.debug("openHAB Alexa connector deactivated");
    }

    protected void modified(Map<String, ?> config) {
        logger.debug("openHAB Alexa connector modified");
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return Collections.singleton(ItemStateEvent.TYPE);
    }

    @Override
    public EventFilter getEventFilter() {
        return null;
    }

    @Override
    public void receive(Event event) {
    }

}
