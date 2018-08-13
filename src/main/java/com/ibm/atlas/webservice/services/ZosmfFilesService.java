/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas.webservice.services;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ibm.atlas.model.files.ChtagResponse;
import com.ibm.atlas.model.files.CreateFileRequest;
import com.ibm.atlas.model.files.GetFileAttributesResponse;
import com.ibm.atlas.model.files.GetFileAttributesResponse.GetFileAttributesResponseBuilder;
import com.ibm.atlas.model.files.GetFileChildAttributesResponse;
import com.ibm.atlas.model.files.GetFileChildAttributesResponse.GetFileChildAttributesResponseBuilder;
import com.ibm.atlas.model.files.GetFileContentResponse;
import com.ibm.atlas.model.files.UpdateFileContentsRequest;
import com.ibm.atlas.model.files.UssFileType;
import com.ibm.atlas.model.files.ZosmfPostFileRequest;
import com.ibm.atlas.model.files.ZosmfPostFileRequest.ZosmfPostFileRequestBuilder;
import com.ibm.atlas.webservice.Messages;
import com.ibm.atlas.webservice.exceptions.AtlasException;
import com.ibm.atlas.webservice.exceptions.BadRequestException;
import com.ibm.atlas.webservice.exceptions.ForbiddenException;
import com.ibm.atlas.webservice.exceptions.InternalServerErrorException;
import com.ibm.atlas.webservice.exceptions.NotFoundException;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class ZosmfFilesService extends ZosmfService {

	@Inject
	Logger log;
	
	@Context
	UriInfo uriInfo;
	
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	public String createFileOrDirectory(CreateFileRequest request) throws AtlasException {

		String symbolicPermissions = null;
		if (request.getPermissions() != null) {
			symbolicPermissions = FilePermissionsHelper.convertPermissionsNumericToSymbolicForm(request.getPermissions());
		}

		createNewFileOrDirectoryRequest(request.getType(), request.getPath(), symbolicPermissions);
		return request.getPath();
	}

	private void createNewFileOrDirectoryRequest(UssFileType type, String path, String symbolicPermissions) throws AtlasException {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		Builder request = createRequest("restfiles/fs" + path); //$NON-NLS-1$
		ZosmfPostFileRequestBuilder bodyBuilder = ZosmfPostFileRequest.builder().type(type.toString());

		if (symbolicPermissions != null) {
			bodyBuilder.mode(symbolicPermissions);
		}
		ZosmfPostFileRequest body = bodyBuilder.build();

		Response response;
		try {
			response = client.postRequestWithContent(request, body, MediaType.APPLICATION_JSON_TYPE);
		} catch (JsonProcessingException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new InternalServerErrorException(Messages.getString("ZosUtilities.ObjectCannotConvertToJson"));
		}

		if (response.getStatus() != Status.CREATED.getStatusCode()) {
			String error = response.readEntity(String.class);
			log.log(Level.SEVERE, error);
			if (error.contains("The specified file already exists")) {
				throw new BadRequestException(String.format(Messages.getString("ZosmfFilesService.FileAlreadyExists"), path));
			} else if (error.contains("The specified directory already exists")) {
				throw new BadRequestException(String.format(Messages.getString("ZosmfFilesService.DirectoryAlreadyExists"), path));
			} else if (error.contains("category\":8,\"rc\":-1")) {
				throw new ForbiddenException(String.format(Messages.getString("ZosmfFilesService.AccessDenied"), path));
			}

			Response errorResponse = Response.status(response.getStatus()).entity(error).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(errorResponse);
		}

	}

	//TODO - refactor with purge job?
	public void delete(String path) throws AtlasException {
		String requestURL = String.format("restfiles/fs/%s", path); //$NON-NLS-1$
		Builder request = createRequest(requestURL);
		request = request.header("X-IBM-Option", "recursive"); //$NON-NLS-1$ //$NON-NLS-2$

		Response response = client.sendRequest(request, HttpMethod.DELETE);

		if (response.getStatus() != Status.NO_CONTENT.getStatusCode()) {
			String error = response.readEntity(String.class);
			checkForFileNotFoundAndAccessDenied(path, error);
			Response errorResponse = Response.status(response.getStatus()).entity(error).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(errorResponse);
		}
	}

	//TODO - refactor with dataset put?
	public void updateFileContent(String path, UpdateFileContentsRequest fileContentsRequest) throws AtlasException {
		String requestURL = String.format("restfiles/fs/%s", path); //$NON-NLS-1$
		ChtagResponse tag = new ChtagResponse("");
		try {
			tag = getTag(path);
		} catch (WebApplicationException wae) {
			// An internal server error can happen if chtag is performed on a directory. 
			// The desired effect is to treat as binary and allow the getContent method 
			// to throw it's error if applicable
			if (wae.getResponse().getStatus() != Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
				throw wae;
			}
		}
		String checksum = fileContentsRequest.getChecksum();
		Builder request = createRequest(requestURL);
		if (checksum != null) {
			request = request.header("If-Match", checksum); //$NON-NLS-1$
		}
		if (!tag.convertRecommended()) {
			// default behaviour is to treat as text
			request = request.header("X-IBM-Data-Type", "binary"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		request = request.header("Content-Type", MediaType.TEXT_PLAIN); //$NON-NLS-1$ //$NON-NLS-2$
		Response response = client.putRequestWithContent(request, fileContentsRequest.getContent(), MediaType.TEXT_PLAIN_TYPE);
		
		if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
			throw new BadRequestException(String.format(Messages.getString("ZosmfFilesService.ChecksumInvalid"), path));
		} else if (response.getStatus() != Status.NO_CONTENT.getStatusCode()) {	
			String error = response.readEntity(String.class);
			checkForFileNotFoundAndAccessDenied(path, error);
			Response errorResponse = Response.status(response.getStatus()).entity(error).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(errorResponse);
		}		
	}

	public GetFileAttributesResponse getAttributes(String path) throws AtlasException {
		if (!path.equals("/")) {
			path = path.replaceFirst("/$","");
		}
		String requestURL = String.format("restfiles/fs"); //$NON-NLS-1$
		Builder request = createRequest(requestURL, "path", path);
		
		Response response = client.sendRequest(request, HttpMethod.GET);
		if (response.getStatus() != Status.OK.getStatusCode()) {	
			String error = response.readEntity(String.class);
			checkForFileNotFoundAndAccessDenied(path, error);
			Response errorResponse = Response.status(response.getStatus()).entity(error).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(errorResponse);
		}
		
		try {
			JSONObject responseJSON = JSONObject.parse(response.readEntity(String.class));
			JSONArray items = (JSONArray) responseJSON.get("items");
			if (items.size() == 1) {
				return getFileAttributes((JSONObject) items.get(0));
			}
			GetFileAttributesResponseBuilder builder = GetFileAttributesResponse.builder();
			List<GetFileChildAttributesResponse> children = new ArrayList<>();
			for (Object item : items) {
				if (item instanceof JSONObject) {
					JSONObject itemJson = (JSONObject) item;
					String name = (String) itemJson.get("name");
					if (name.equals("..")) {
						//ignore parent
					} else if (name.equals(".")) {
						addFileAttributes(itemJson, builder);
					} else {
						URI childUri = new URI(uriInfo.getAbsolutePath().toString() + URLEncoder.encode("/" + name, "UTF-8"));
						GetFileChildAttributesResponseBuilder childBuilder = GetFileChildAttributesResponse.builder()
							.name(name)
							.link(childUri);
						String mode = (String) itemJson.get("mode");
						if (mode.startsWith("d")) {
							childBuilder.type(UssFileType.directory);
						} else {
							childBuilder.type(UssFileType.file);
						}
						children.add(childBuilder.build());
					}
				}
			}
			builder.children(children);
			return builder.build();
		} catch (IOException | ClassCastException | ParseException | URISyntaxException e) {
			Response errorResponse = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(errorResponse);
		}
	}
	
	private GetFileAttributesResponse getFileAttributes(JSONObject itemJson) throws BadRequestException, ParseException {
		GetFileAttributesResponseBuilder builder = GetFileAttributesResponse.builder();
		addFileAttributes(itemJson, builder);
		builder.content(uriInfo.getAbsolutePathBuilder().path("content").build());
		return builder.build();
	}

	private void addFileAttributes(JSONObject itemJson, GetFileAttributesResponseBuilder builder) throws BadRequestException, ParseException {
		String mode = (String) itemJson.get("mode");
		if (mode.startsWith("d")) {
			builder.type(UssFileType.directory);
		} else {
			builder.type(UssFileType.file);
		}
		String permissions = mode.substring(1);
		builder.permissionsSymbolic(permissions);
		builder.permissionsNumeric(FilePermissionsHelper.convertPermissionsSymbolicToNumericForm(permissions));
		builder.size((Long) itemJson.get("size"));
		builder.fileOwner((String) itemJson.get("user"));
		builder.group((String) itemJson.get("group"));
		builder.lastModifiedDate(dateFormat.parse((String) itemJson.get("mtime")));
	}

	public GetFileContentResponse getContent(String path) throws AtlasException {
		String requestURL = String.format("restfiles/fs/%s", path.replaceFirst("^/","")); //$NON-NLS-1$
		ChtagResponse tag = new ChtagResponse("");
		try {
			tag = getTag(path);
		} catch (WebApplicationException wae) {
			// An internal server error can happen if chtag is performed on a directory. 
			// The desired effect is to treat as binary and allow the getContent method 
			// to throw it's error if applicable
			if (wae.getResponse().getStatus() != Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
				throw wae;
			}
		}
		Builder request = createRequest(requestURL);
		if (!tag.convertRecommended()) {
			// default behaviour is to treat as text
			request = request.header("X-IBM-Data-Type", "binary"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		Response response = client.sendRequest(request, HttpMethod.GET);

		if (response.getStatus() != Status.OK.getStatusCode()) {
			String error = response.readEntity(String.class);
			if (error.contains("EDC5121I")) {
				throw new BadRequestException(String.format(Messages.getString("ZosmfFilesService.GetContentOnDirectory"), path));
			}
			checkForFileNotFoundAndAccessDenied(path, error);
			Response errorResponse = Response.status(response.getStatus()).entity(error).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(errorResponse);
		}
		String entity = response.readEntity(String.class);
		String checksum = response.getHeaderString("ETag");
		return GetFileContentResponse.builder().content(entity).checksum(checksum).build();
	}
	
	private void checkForFileNotFoundAndAccessDenied(String path, String error) throws NotFoundException, ForbiddenException {
		try {
			JSONObject responseJSON = JSONObject.parse(error);
			if (responseJSON.containsKey("details")) {
				if (responseJSON.get("details").toString().contains("EDC5129I")){
					throw new NotFoundException(String.format(Messages.getString("ZosmfFilesService.FileNotFound"), path));
				} else if (responseJSON.get("details").toString().contains("EDC5111I")){
					throw new ForbiddenException(String.format(Messages.getString("ZosmfFilesService.AccessDenied"), path));
				}
			}
		} catch (IOException e) {
			//Error message not json, so just throw without processing
		}
	}
	public ChtagResponse getTag(String path) throws AtlasException {
		try {
			JSONObject inputJSON = JSONObject.parse("{\"request\":\"chtag\",\"action\":\"list\"}"); //$NON-NLS-1$
			String requestURL = String.format("restfiles/fs/%s", path.replaceFirst("^/","")); //$NON-NLS-1$
			Builder request = createRequest(requestURL);

			Response response = client.putRequestWithContent(request, inputJSON, MediaType.APPLICATION_JSON_TYPE);
			if (response.getStatus() != Status.OK.getStatusCode()) {
				String error = response.readEntity(String.class);
				checkForFileNotFoundAndAccessDenied(path, error);
				Response errorResponse = Response.status(response.getStatus()).entity(error).type(MediaType.APPLICATION_JSON_TYPE).build();
				throw new WebApplicationException(errorResponse);
			}
			JSONObject responseJSON = JSONObject.parse(response.readEntity(String.class));
			JSONArray responseArray = (JSONArray) responseJSON.get("stdout");
			return new ChtagResponse((String) responseArray.get(0));
		} catch (IOException | ClassCastException e) {
			log.severe(e.getMessage());
			Response errorResponse = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(errorResponse);
		}
	}
}
