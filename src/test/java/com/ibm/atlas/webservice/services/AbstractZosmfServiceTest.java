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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.mockito.Matchers;
import org.mockito.Mockito;

import com.ibm.atlas.AtlasTest;
import com.ibm.atlas.utilities.client.HTTPClient;

public class AbstractZosmfServiceTest <T extends ZosmfService> extends AtlasTest {
	
	private T service;

	public void init(T aService) {
		this.service = aService;
		aService.client = Mockito.mock(HTTPClient.class, Mockito.RETURNS_DEEP_STUBS);
		aService.log = Mockito.mock(Logger.class);
	}
	
	public String loadTestFile(String relativePath) {
		String output = null;
		try {
			byte[] encoded = Files.readAllBytes(Paths.get("src/test/resources/com/ibm/atlas/webservice/services/ZOSMFService/" + relativePath));
			output = new String(encoded, Charset.forName("UTF8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return output;
	}

	void mockHTTPClientSendRequest() {
		Response r = Response.status(Status.OK).entity(loadTestFile("zosmfService_getJobFiles.txt")).type(MediaType.APPLICATION_JSON).build();
		Mockito.when(service.client.sendRequest((Builder)Matchers.any(), Matchers.anyString())).thenReturn(r);
	}
	protected void mockGetRequestResponse(Builder builder, Response response) throws Exception {
		when(service.client.sendRequest(builder, HttpMethod.GET)).thenReturn(response);
	}
	protected void mockPutRequestResponse(Builder builder, Object body, Response response) throws Exception {
		when(service.client.putRequestWithContent(builder, body, MediaType.APPLICATION_JSON_TYPE)).thenReturn(response);
	}
	protected void mockRequestResponse(String uri, String method, Response response, String... queryParamPairs) throws Exception {
		Builder builder = mockClient(uri, queryParamPairs);
		when(service.client.sendRequest(builder, method)).thenReturn(response);
	}
	
	protected void mockPostRequestResponse(String uri, Object body, Response response) throws Exception {
		Builder builder = mockClient(uri);
		when(service.client.postRequestWithContent(builder, body, MediaType.APPLICATION_JSON_TYPE)).thenReturn(response);
	}

	protected Builder mockClient(String uri, String... queryParamPairs) throws Exception {
		WebTarget webTarget = mock(WebTarget.class);
		Builder builder = mock(Builder.class);
		when(service.client.createTarget(ZosmfService.DEFAULT_HTTPS_PORT, ZosmfService.CONTEXT_ROOT)).thenReturn(webTarget);
		when(webTarget.path(uri)).thenReturn(webTarget);
		for (int i = 0; i < queryParamPairs.length; i += 2) {
			when(webTarget.queryParam(queryParamPairs[i],queryParamPairs[i+1])).thenReturn(webTarget);
		}
		when(webTarget.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
		when(builder.header("X-CSRF-ZOSMF-HEADER","")).thenReturn(builder);
		return builder;
	}
}
