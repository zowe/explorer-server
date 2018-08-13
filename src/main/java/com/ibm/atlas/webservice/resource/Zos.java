/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas.webservice.resource;

import java.security.Principal;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import com.ibm.atlas.webservice.Messages;
import com.ibm.atlas.webservice.resource.zos.entity.Sysplex;
import com.ibm.atlas.webservice.resource.zos.entity.Username;
import com.ibm.atlas.webservice.utilities.JZOSUtilities;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path(value = "/zos")

@Api(value = "Atlas : z/OS System APIs")
public class Zos {
	
	@Context
	private UriInfo uriInfo;
	
	@Inject
	Logger log;
	
	/**
	 * Get the current user's TSO username
	 * 
	 * @return User's TSO userid
	 */
	@GET
	@Path(value = "username")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get current userid", 
    notes = "This API returns the caller's current TSO userid.")
	@ApiResponses({@ApiResponse(code = 200, message = "Ok", response = Username.class)})
	public Username getCurrentUserName(@Context SecurityContext securityContext) {

		String username = null;

		Principal userPrincipal = securityContext.getUserPrincipal();
		if (userPrincipal != null) {
			username = userPrincipal.getName();
		}

		if (username == null || username.isEmpty()) {
			String error = Messages.getString("Zos.InvalidUsername"); //$NON-NLS-1$
			Response errorResponse = Response.status(Status.NOT_FOUND).entity(error).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(errorResponse);
		}
		
		return new Username(username.toUpperCase());
	}

	/**
	 * Get the PARMLIB concatenation
	 * @return The PARMLIB dataset concatenation
	 */
	@GET
	@Path(value = "parmlib")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get system PARMLIB information", 
    notes = "This API returns the target system PARMLIB concatenation, equivalent to the /D PARMLIB command.")
	@ApiResponses({@ApiResponse(code = 200, message = "Ok", response = String.class, responseContainer = "List")})
	public List<String> getParmlibInfo() {
		return JZOSUtilities.getParmlibDetails();
	}

	/**
	 * Get sysplex and system names
	 * @return Current sysplex and system name
	 */
	@GET
	@Path(value = "sysplex")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get target system sysplex and system name", 
    notes = "This API returns the target system sysplex and system names.")
	@ApiResponses({@ApiResponse(code = 200, message = "Ok", response = Sysplex.class)})
	public Sysplex getSysplexInfo() {
		return JZOSUtilities.getSysplexDetails();
	}
}