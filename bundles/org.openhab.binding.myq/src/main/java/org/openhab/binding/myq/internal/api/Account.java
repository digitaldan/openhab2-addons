/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.myq.internal.api;

/**
 * The {@link Account} entity from the MyQ API
 *
 * @author Dan Cunningham - Initial contribution
 */
public class Account {

    public Users users;
    public Boolean admin;
    public AccountInfo account;
    public String analyticsId;
    public String userId;
    public String userName;
    public String email;
    public String firstName;
    public String lastName;
    public String cultureCode;
    public Address address;
    public TimeZone timeZone;
    public Boolean mailingListOptIn;
    public Boolean requestAccountLinkInfo;
    public String phone;
    public Boolean diagnosticDataOptIn;
}
