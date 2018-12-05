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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ibm.atlas.utilities.client.HTTPClient;
import com.ibm.atlas.webservice.Messages;

public abstract class ZosmfService {

	static final String CONTEXT_ROOT = "zosmf"; //$NON-NLS-1$
	static final int DEFAULT_HTTPS_PORT = 443;
	private static final String RESPONSE_PARSE_ERROR = Messages.getString("ZOSMFService.ResponseParseError");
	protected final int httpsPort = getZOSMFHttpsPort();

	@Inject
	HTTPClient client;

	@Inject
	Logger log;

	/**
	 * Read the z/OSMF HTTPS Port number from the z/OSMF configuration.
	 * 
	 * @return z/OSMF HTTPS port number
	 */
	public int getZOSMFHttpsPort() {
		int port = DEFAULT_HTTPS_PORT;

		try {
			port = ((Integer) new InitialContext().lookup("zOSMFHttpsPort")).intValue(); //$NON-NLS-1$
		} catch (NamingException e) {
			if (log != null) {
				log.severe(Messages.getString("ZOSMFService.PropertyErrorzosmfHTTPPort") + port);//$NON-NLS-1$
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		return port;
	}
	
	protected Builder createRequest(String relativeUri, String... queryParamPairs) {
		WebTarget webTarget = client.createTarget(httpsPort, CONTEXT_ROOT).path(relativeUri);
		for (int i = 0; i < queryParamPairs.length; i += 2) {
			webTarget = webTarget.queryParam(queryParamPairs[i], queryParamPairs[i+1]);
		}
		return webTarget
			.request(MediaType.APPLICATION_JSON)
			.header("X-CSRF-ZOSMF-HEADER", "")
			.header("X-IBM-Response-Timeout", "600");
	}
	
	protected WebApplicationException createJSONParseException(IOException exception) {
		String error = String.format(RESPONSE_PARSE_ERROR, exception.getMessage()); //$NON-NLS-1$
		log.log(Level.SEVERE, exception.getMessage(), exception);
		Response errorResponse = Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).type(MediaType.TEXT_PLAIN).build();
		return new WebApplicationException(errorResponse);
	}
	
	protected WebApplicationException createAuthorizationFailureException(String errorText) {
		String error = String.format(Messages.getString("ZOSMFService.UnauthroizedDataset"), errorText);
		log.log(Level.SEVERE, error);
		Response errorResponse = Response.status(Status.FORBIDDEN).entity(error).type(MediaType.TEXT_PLAIN).build();
		return new WebApplicationException(errorResponse);
	}
}
