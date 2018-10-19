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

import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import com.ibm.atlas.webservice.resource.system.entity.Version;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path(value = "/system")

@Api(value = "Zowe : System APIs")
public class System {
	
	@Context
	private UriInfo uriInfo;
	
	@Inject
	Logger log;
	
	/**
	 * Get the current Atlas version
	 * 
	 * @return Atlas version
	 */
	@GET
	@Path(value = "version")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get current Atlas version", 
    notes = "This API returns the current version of the Atlas micro-service.")
	@ApiResponses({@ApiResponse(code = 200, message = "Ok", response = Version.class)})
	public Version getVersion(@Context SecurityContext securityContext) {
		
		return new Version();

	}
	
}