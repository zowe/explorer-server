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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.URI;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.ibm.atlas.AtlasTest;
import com.ibm.atlas.model.files.CreateFileRequest;
import com.ibm.atlas.model.files.GetFileAttributesResponse;
import com.ibm.atlas.model.files.GetFileContentResponse;
import com.ibm.atlas.model.files.UpdateFileContentsRequest;
import com.ibm.atlas.model.files.UssFileType;
import com.ibm.atlas.webservice.exceptions.AtlasException;
import com.ibm.atlas.webservice.services.ZosmfFilesService;

public class FilesTest extends AtlasTest {

	private static final String PATH = "/u/stevenh/file";
	
	//TODO - once we have junit5 move these into nested sections
	CreateFileRequest createRequest = CreateFileRequest.builder()
		.type(UssFileType.file)
		.path(PATH)
		.build();
	
	UpdateFileContentsRequest updateContentRequest = UpdateFileContentsRequest.builder()
		.content("dummyContent")
		.build();
		
	private Files filesResource;
	
	@Before
	public void init() {
		filesResource = new Files();
		filesResource.zosmfFilesService = Mockito.mock(ZosmfFilesService.class);
		filesResource.uriInfo = Mockito.mock(UriInfo.class);
	}
	
	@Test
	public void testValidCreateWorks() throws Exception {
		when(filesResource.zosmfFilesService.createFileOrDirectory(createRequest)).thenReturn(PATH);

		URI dummyLocation = new URI("http://dummy.com/junk");
		UriBuilder uriBuilder = mock(UriBuilder.class);
		when(filesResource.uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
		when(uriBuilder.path(PATH)).thenReturn(uriBuilder);
		when(uriBuilder.build()).thenReturn(dummyLocation);

		Response response = filesResource.create(createRequest);
		assertEquals(false, response.hasEntity());
		assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
		assertEquals(dummyLocation, response.getLocation());
	}
	
	@Test
	public void testCreateWithAtlasExceptionConvertedIntoWebAppException() throws Exception {
		WebApplicationException expected = new WebApplicationException(Status.CONFLICT);
		AtlasException atlasException = mock(AtlasException.class);
		when(atlasException.createWebApplicationException()).thenReturn(expected);
		when(filesResource.zosmfFilesService.createFileOrDirectory(createRequest)).thenThrow(atlasException);
		
		shouldThrow(expected, () -> filesResource.create(createRequest));
	}
	
	@Test
	public void testValidDeleteWorks() throws Exception {
		Response response = filesResource.delete(PATH);
		assertEquals(false, response.hasEntity());
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
	}
	
	@Test
	public void testDeleteWithAtlasExceptionConvertedIntoWebAppException() throws Exception {
		WebApplicationException expected = new WebApplicationException(Status.CONFLICT);
		AtlasException atlasException = mock(AtlasException.class);
		when(atlasException.createWebApplicationException()).thenReturn(expected);
		doThrow(atlasException).when(filesResource.zosmfFilesService).delete(PATH);
		
		shouldThrow(expected, () -> filesResource.delete(PATH));
	}
	
	@Test
	public void testValidUpdateWorks() throws Exception {
		Response response = filesResource.updateFileContent(PATH, updateContentRequest);
		assertEquals(false, response.hasEntity());
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
	}
	
	@Test
	public void testUpdateWithAtlasExceptionConvertedIntoWebAppException() throws Exception {
		WebApplicationException expected = new WebApplicationException(Status.CONFLICT);
		AtlasException atlasException = mock(AtlasException.class);
		when(atlasException.createWebApplicationException()).thenReturn(expected);
		doThrow(atlasException).when(filesResource.zosmfFilesService).updateFileContent(PATH, updateContentRequest);
		
		shouldThrow(expected, () -> filesResource.updateFileContent(PATH, updateContentRequest));
	}
	
	@Test
	public void testValidGetAttributesWorks() throws Exception {
		GetFileAttributesResponse mockResponse = mock(GetFileAttributesResponse.class);
		when(filesResource.zosmfFilesService.getAttributes(PATH)).thenReturn(mockResponse);
		
		Response response = filesResource.getAttributes(PATH);
		assertEquals(mockResponse, response.getEntity());
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
	}
	
	@Test
	public void testGetAttributesWithAtlasExceptionConvertedIntoWebAppException() throws Exception {
		WebApplicationException expected = new WebApplicationException(Status.CONFLICT);
		AtlasException atlasException = mock(AtlasException.class);
		when(atlasException.createWebApplicationException()).thenReturn(expected);
		doThrow(atlasException).when(filesResource.zosmfFilesService).getAttributes(PATH);
		
		shouldThrow(expected, () -> filesResource.getAttributes(PATH));
	}
	
	@Test
	public void testValidGetContentWorks() throws Exception {
		GetFileContentResponse mockResponse = mock(GetFileContentResponse.class);
		when(filesResource.zosmfFilesService.getContent(PATH)).thenReturn(mockResponse);
		
		Response response = filesResource.getContent(PATH);
		assertEquals(mockResponse, response.getEntity());
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
	}
	
	@Test
	public void testGetContentWithAtlasExceptionConvertedIntoWebAppException() throws Exception {
		WebApplicationException expected = new WebApplicationException(Status.CONFLICT);
		AtlasException atlasException = mock(AtlasException.class);
		when(atlasException.createWebApplicationException()).thenReturn(expected);
		doThrow(atlasException).when(filesResource.zosmfFilesService).getContent(PATH);
		
		shouldThrow(expected, () -> filesResource.getContent(PATH));
	}
}
