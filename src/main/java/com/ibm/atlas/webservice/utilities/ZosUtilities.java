/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas.webservice.utilities;

import java.security.Principal;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ibm.atlas.webservice.Messages;

import javax.ws.rs.core.SecurityContext;

public class ZosUtilities {
	
	public static String getUsername(SecurityContext securityContext) {
		Principal userPrincipal = securityContext.getUserPrincipal();
		if (userPrincipal != null) {
			String username = userPrincipal.getName();
			if ( username != null ) {
				return username.toUpperCase();
			}
		}

		String error = String.format(Messages.getString("ZosUtilities.NoUsername")); //$NON-NLS-1$
		Response errorResponse = Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).type(MediaType.TEXT_PLAIN).build();
		throw new WebApplicationException(errorResponse);
	}

}
