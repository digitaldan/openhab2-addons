/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.grandstreamgds.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link GrandstreamGDSEventServlet} is responsible for
 *
 * @author Dan Cunningham - Initial contribution
 */
@NonNullByDefault
@Component(service = HttpServlet.class, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class GrandstreamGDSEventServlet extends HttpServlet {
    private static final long serialVersionUID = -5477564753975001988L;

    private final Logger logger = LoggerFactory.getLogger(GrandstreamGDSEventServlet.class);
    private static final String BASE_SERVLET_PATH = "/grandstreamds";
    private final HttpService httpService;
    private final GrandstreamGDSHandlerFactory handlerFactory;

    @Activate
    public GrandstreamGDSEventServlet(@Reference HttpService httpService,
            @Reference GrandstreamGDSHandlerFactory handlerFactory, Map<String, Object> config) {
        this.httpService = httpService;
        this.handlerFactory = handlerFactory;
        try {
            httpService.registerServlet(BASE_SERVLET_PATH, this, null, httpService.createDefaultHttpContext());
            logger.debug("GrandstreamGDSEventServlet started at '{}'", BASE_SERVLET_PATH);
        } catch (NamespaceException | ServletException | IllegalArgumentException e) {
            logger.warn("Could not start GrandstreamGDSEventServlet", e);
        }
    }

    @Deactivate
    protected void deactivate() {
        httpService.unregister(BASE_SERVLET_PATH);
        logger.debug("GrandstreamGDSEventServlet stopped");
    }

    @Override
    protected void service(@Nullable HttpServletRequest request, @Nullable HttpServletResponse resp)
            throws ServletException, IOException, IllegalArgumentException {

        if ((request == null) || (resp == null)) {
            logger.debug("request or resp must not be null!");
            return;
        }
        try {
            String[] segments = request.getRequestURI().toLowerCase().split("\\/");
            for (String segment : segments) {
                logger.debug("Segment {}", segment);
            }
        } finally {
            resp.setCharacterEncoding(StandardCharsets.UTF_8.toString());
            resp.getWriter().write("ok");
        }
    }
}