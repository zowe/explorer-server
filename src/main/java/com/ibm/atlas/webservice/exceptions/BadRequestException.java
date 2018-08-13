/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas.webservice.exceptions;

import javax.ws.rs.core.Response.Status;

public class BadRequestException extends AtlasException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3836615959803649121L;

	public BadRequestException(String message) {
		super(Status.BAD_REQUEST, message);
	}
}
