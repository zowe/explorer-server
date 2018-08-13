/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas.webservice.utilities.response;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ibm.json.java.JSONObject;

/**
 * Standard Response format utility, based on the JSend specification.
 * 
 * https://labs.omniti.com/labs/jsend
 */
public class ResponseUtils {

	/**
	 * All went well, and return some data
	 * 
	 * @param data Data to send to the client, or null
	 * @return A success response to send to the client
	 */
	public static Response success(JSONObject data) {
		JSONObject obj = new JSONObject();
		
		obj.put("status", "success"); //$NON-NLS-1$ //$NON-NLS-2$
		obj.put("data", data); //$NON-NLS-1$
		
		return Response.ok().entity(obj).type(MediaType.APPLICATION_JSON).build();
	}
	
	/**
	 * All went well
	 * 
	 * @return A success response to send to the client
	 */
	public static Response success() {
		return success(null);
	}
	
	/**
	 * There was a problem with the data submitted, or some pre-condition of the API call wasn't satisfied
	 * 
	 * @param status The HTTP Status for the response
	 * @param data A meaningful, end-user-readable (or at the least log-worthy) message, explaining what went wrong.
	 * @return A fail Response with data about what went wrong
	 */
	public static Response fail(Status status, String error) {
		JSONObject obj = new JSONObject();
		
		obj.put("status", "fail"); //$NON-NLS-1$ //$NON-NLS-2$
		obj.put("message", error); //$NON-NLS-1$
		
		return Response.status(status).entity(obj).type(MediaType.APPLICATION_JSON).build();
	}
	
	/**
	 * An error occurred in processing the request, i.e. an exception was thrown
	 * 
	 * @param errorMessage A meaningful, end-user-readable (or at the least log-worthy) message, explaining what went wrong.
	 * @return An error Response explaining the problem
	 */
	public static Response error(String error) {
		JSONObject obj = new JSONObject();
		
		obj.put("status", "error"); //$NON-NLS-1$ //$NON-NLS-2$
		obj.put("message", error); //$NON-NLS-1$
		
		return Response.serverError().entity(obj).type(MediaType.APPLICATION_JSON).build();
	}
}
