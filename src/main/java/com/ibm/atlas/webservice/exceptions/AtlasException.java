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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public abstract class AtlasException extends Exception {

	private Status status;

	AtlasException(Status status) {
		this.status = status;
	}
	
	AtlasException(Status status, String message) {
		super(message);
		this.status = status;
	}
	
	public WebApplicationException createWebApplicationException() {
		Response errorResponse = Response.status(status).entity(getMessage()).type(MediaType.TEXT_PLAIN).build();
		return new WebApplicationException(errorResponse);
	}
}
