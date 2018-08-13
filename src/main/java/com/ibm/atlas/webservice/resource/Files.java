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
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.ibm.atlas.model.files.CreateFileRequest;
import com.ibm.atlas.model.files.GetFileAttributesResponse;
import com.ibm.atlas.model.files.GetFileContentResponse;
import com.ibm.atlas.model.files.UpdateFileContentsRequest;
import com.ibm.atlas.webservice.exceptions.AtlasException;
import com.ibm.atlas.webservice.services.ZosmfFilesService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path(value = "/uss/files")

@Api(value = "Atlas : USS Files APIs")
public class Files {

	@Inject
	private Logger log;
		
	@Inject
	ZosmfFilesService zosmfFilesService;
	
	@Context
	UriInfo uriInfo;
	
	@GET
	@Path(value = "{path}/content")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get USS file content", notes = "This API returns USS file content")
	@ApiResponses({@ApiResponse(code = 200, message = "Ok", response = GetFileContentResponse.class)})
	public Response getContent(@ApiParam(value = "Full path of file to get content for", required = true) @PathParam("path") String path) {
		try {
			GetFileContentResponse response = zosmfFilesService.getContent(path);
			return Response.status(Status.OK).entity(response).build();
		} catch (AtlasException e) {
			throw e.createWebApplicationException();
		}
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Create USS directory or file", notes = "This API creates a new USS directory or file")
	@ApiResponses({ @ApiResponse(code = 201, message = "Directory/File successfully created") })
	public Response create(
		@ApiParam(value = "JSON format input body") CreateFileRequest request) {
		try {
			String newFilePath = zosmfFilesService.createFileOrDirectory(request);
			UriBuilder builder = uriInfo.getAbsolutePathBuilder();
			builder.path(newFilePath);
			return Response.created(builder.build()).build();
		} catch (AtlasException e) {
			throw e.createWebApplicationException();
		}
	}
	
	@DELETE
	@Path(value = "{path}")
	@ApiOperation(value = "Delete a USS directory or file", notes = "This API delete an existing USS directory or file")
	@ApiResponses({ @ApiResponse(code = 204, message = "Directory/file successfully deleted") })
	public Response delete(
		@ApiParam(value = "Full path of resource to be deleted", required = true) @PathParam("path") String path) {
		try {
			zosmfFilesService.delete(path);
			return Response.status(Status.NO_CONTENT).build();
		} catch (AtlasException e) {
			throw e.createWebApplicationException();
		}
	}
	
	@PUT
	@Path(value = "{path}/content")
	@Consumes(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Update the contents of a USS file", notes = "This API updates the contents of a USS file")
	@ApiResponses({ @ApiResponse(code = 204, message = "Directory/file successfully updated") })
	public Response updateFileContent(@ApiParam(value = "Full path of resource to be updated", required = true) @PathParam("path") String path,
		@ApiParam(value = "JSON format input body") UpdateFileContentsRequest request) {
		try {
			zosmfFilesService.updateFileContent(path, request);
			return Response.status(Status.NO_CONTENT).build();
		} catch (AtlasException e) {
			throw e.createWebApplicationException();
		}
	}
	
	@GET
	@Path(value = "{path}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "List the attributes of a USS file or directory",   notes = "This API returns the attributes of a given USS file or directory.")
	@ApiResponses({@ApiResponse(code = 200, message = "Ok", response = GetFileAttributesResponse.class)})
	public Response getAttributes(@ApiParam(value = "Full path of resource to get the attributes, without a trailing \"/\"", required = true) @PathParam("path") String path) {
		try {
			GetFileAttributesResponse response = zosmfFilesService.getAttributes(path);
			return Response.status(Status.OK).entity(response).build();
		} catch (AtlasException e) {
			throw e.createWebApplicationException();
		}
	}
}