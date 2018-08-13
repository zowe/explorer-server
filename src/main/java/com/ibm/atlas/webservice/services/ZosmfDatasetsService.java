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
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ibm.atlas.model.datasets.DataSetContentResponse;
import com.ibm.atlas.webservice.Messages;
import com.ibm.atlas.webservice.resource.datasets.entity.DatasetAttributes;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class ZosmfDatasetsService extends ZosmfService {

	private static final String PHYSICAL_SEQUENTIAL_DATA_SET_ORG = "PS"; //$NON-NLS-1$
	private static final String PATRITIONED_DATA_SET_ORG = "PO"; //$NON-NLS-1$
	private static final String AUTHORIZATION_FAILURE = "ISRZ002 Authorization failed";
	private static final Collection<String> CREATE_PARMS = Arrays.asList("unit", "dsorg", "alcunit", "primary", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"secondary", "dirblk", "avgblk", "recfm", "blksize", "lrecl", "storeclass", "mgntclass", "dataclass", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
			"spacu", "sizex", "blksz"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$	

	public JSONObject listDatasets(String filter) {
		String requestURL = String.format("restfiles/ds"); //$NON-NLS-1$
		Builder request = createRequest(requestURL, "dslevel", filter);
		request = request.header("X-IBM-Max-Items", "0"); //$NON-NLS-1$ //$NON-NLS-2$

		Response response = client.sendRequest(request, HttpMethod.GET);

		if (response.getStatus() != Status.OK.getStatusCode()) {
			String error = String.format(Messages.getString("ZOSMFService.ListFailedForFilter"), filter); //$NON-NLS-1$
			Response errorResponse = Response.status(response.getStatus()).entity(error).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(errorResponse);
		}
		try {
			return JSONObject.parse(response.readEntity(String.class));
		} catch (IOException e) {
			throw createJSONParseException(e);
		}
	}

	public JSONObject listDatasetMembers(String dsn) {
		String requestURL = String.format("restfiles/ds/%s/member", dsn); //$NON-NLS-1$
		Builder request = createRequest(requestURL);
		request = request.header("X-IBM-Max-Items", "0"); //$NON-NLS-1$ //$NON-NLS-2$

		Response response = client.sendRequest(request, HttpMethod.GET);
		try {
			JSONObject responseJSON = JSONObject.parse(response.readEntity(String.class));
			if (response.getStatus() != Status.OK.getStatusCode()) {
				if (responseJSON.get("details").toString().contains(AUTHORIZATION_FAILURE)){
					throw createAuthorizationFailureException(responseJSON.get("details").toString());
				}
				String error = String.format(Messages.getString("ZOSMFService.ListFailedForDataset"), dsn); //$NON-NLS-1$
				Response errorResponse = Response.status(response.getStatus()).entity(error).type(MediaType.TEXT_PLAIN).build();
				throw new WebApplicationException(errorResponse);
			}
			return responseJSON;
		} catch (IOException e) {
			throw createJSONParseException(e);
		}
		
	}

	public DataSetContentResponse getConvertedContentWithEtag(String dsn) {
		return getContent(dsn, true, true, null, null);
	}

	public DataSetContentResponse getConvertedContentByRange(String dsn, String start, String end) {
		return getContent(dsn, true, false, start, end);
	}

	public DataSetContentResponse getContent(String dsn, boolean convert, boolean etag, String start, String end) {
		int recordCount = 0;
		int startRecord = 0;
		int endRecord = -1;

		if (start != null) {
			try {
				startRecord = Integer.parseInt(start);
			} catch (@SuppressWarnings("unused") NumberFormatException e) {
				startRecord = 0;
			}
		}

		if (end != null) {
			try {
				endRecord = Integer.parseInt(end);
			} catch (@SuppressWarnings("unused") NumberFormatException e) {
				endRecord = -1;
			}
		}

		String requestURL = String.format("restfiles/ds/%s", dsn); //$NON-NLS-1$
		Builder request = createRequest(requestURL);
		if (convert) {
			request = request.header("X-IBM-Data-Type", "text"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			request = request.header("X-IBM-Data-Type", "binary"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (etag) {
			request = request.header("X-IBM-Return-Etag", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		Response response = client.sendRequest(request, HttpMethod.GET);

		if (response.getStatus() != Status.OK.getStatusCode()) {
			try{
				JSONObject responseJSON = JSONObject.parse(response.readEntity(String.class));
				if (responseJSON.get("details").toString().contains(AUTHORIZATION_FAILURE)){
					throw createAuthorizationFailureException(responseJSON.get("details").toString());
				}
			} catch(IOException e){
				throw createJSONParseException(e);
			}
			String error = String.format(Messages.getString("ZOSMFService.ContentFailedForDataset"), dsn); //$NON-NLS-1$
			Response errorResponse = Response.status(response.getStatus()).entity(error).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(errorResponse);
		}

		DataSetContentResponse result = new DataSetContentResponse();
		if (etag) {
			result.setChecksum(response.getHeaderString("Etag")); //$NON-NLS-1$
		}

		String output = response.readEntity(String.class);

		if (endRecord == -1) {
			result.setRecords(output);
		} else {
			String[] lines = output.toString().split("\n"); //$NON-NLS-1$
			StringBuilder sb = new StringBuilder();

			for (String line : lines) {
				if (recordCount >= startRecord) {
					if (recordCount <= endRecord || endRecord == -1) {
						// if (recordCount != startRecord) {
						// sb.append("\n");
						// }
						sb.append(line);
						sb.append("\n"); //$NON-NLS-1$
					} else {
						break;
					}
				}
				recordCount++;
			}
			result.setRecords(sb.toString());
		}

		return result;
	}

	/**
	 * Get recfm, lrecl and blksize of named dsn(member)
	 * 
	 * @param dsn
	 *            The PDS dataset name
	 * @param member
	 *            The target member name
	 * @return Attributes of named dsn(member)
	 */
	public JSONObject getDatasetAttributes(String dsn) {
		String requestURL = String.format("restfiles/ds"); //$NON-NLS-1$
		Builder request = createRequest(requestURL, "dslevel", dsn); //$NON-NLS-1$
		request = request.header("X-IBM-Attributes", "base"); //$NON-NLS-1$ //$NON-NLS-2$

		Response response = client.sendRequest(request, HttpMethod.GET);

		if (response.getStatus() != Status.OK.getStatusCode()) {
			String error = String.format(Messages.getString("ZOSMFService.AttributesFailedForDataset"), dsn); //$NON-NLS-1$
			Response errorResponse = Response.status(response.getStatus()).entity(error).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(errorResponse);
		}

		String result = response.readEntity(String.class);
		try {
			return JSONObject.parse(result);
		} catch (IOException e) {
			throw createJSONParseException(e);
		}
	}

	public boolean putConvertedContent(String dsn, String records) {
		return putContent(dsn, records, null);
	}

	public boolean putConvertedContentWithChecksum(String dsn, String records, String checksum) {
		return putContent(dsn, records, checksum);
	}

	public boolean putContent(String dsn, String records, String checksum) {
		String requestURL = String.format("restfiles/ds/%s", dsn); //$NON-NLS-1$

		Builder request = createRequest(requestURL);
		if (checksum != null) {
			request = request.header("If-Match", checksum); //$NON-NLS-1$
		}
		request = request.header("X-IBM-Data-Type", "text"); //$NON-NLS-1$ //$NON-NLS-2$
		request = request.header("Content-Type", "text/plain"); //$NON-NLS-1$ //$NON-NLS-2$
		Response response = client.putRequestWithContent(request, records, MediaType.TEXT_PLAIN_TYPE);

		if (response.getStatus() == Status.NO_CONTENT.getStatusCode()
				|| response.getStatus() == Status.CREATED.getStatusCode()) {
			return true;
		}

		String error = String.format(Messages.getString("ZOSMFService.PutFailedDataset"), dsn); //$NON-NLS-1$
		Response errorResponse = Response.status(response.getStatus()).entity(error).type(MediaType.TEXT_PLAIN).build();
		throw new WebApplicationException(errorResponse);
	}

	public boolean createDatasetExtended(String dsn, String attributes) {
		JSONObject dsnAttributes = null;
		try {
			dsnAttributes = JSONObject.parse(attributes);
		} catch (IOException e) {
			throw createJSONParseException(e);
		}
		if (dsnAttributes!=null) {
			if (dsnAttributes.containsKey("basedsn")) { //$NON-NLS-1$ 
				createDatasetMergeAttributes(dsnAttributes, getDatasetAttributes((String) dsnAttributes.get("basedsn"))); //$NON-NLS-1$
			} 
			createDatasetPreChecks(dsn, dsnAttributes);
			createDataset(dsn, createDatasetReformat(dsnAttributes));
			if (dsnAttributes.containsKey("records")) { //$NON-NLS-1$ //$NON-NLS-2$ 
				String records =  (String) dsnAttributes.get("records"); //$NON-NLS-1$ 
				putConvertedContent(dsn, records);
			}
		}
		return true;
	}
	protected void createDatasetPreChecks(String dsn, JSONObject dsnAttributes) {
		int memberCheck = dsn.indexOf('(');
		// Test for invalid dataset organization with directory blocks specified
		if (dsnAttributes.containsKey("dsorg")) {
			String dsorg = (String) dsnAttributes.get("dsorg"); //$NON-NLS-1$
			if (dsorg.equals(PHYSICAL_SEQUENTIAL_DATA_SET_ORG)&&dsnAttributes.containsKey("dirblk"))	 { 
				String error = String.format(Messages.getString("Datasets.ParameterError"), "dsorg=PS, dirblk="); //$NON-NLS-1$ //$NON-NLS-2$
				Response errorResponse = Response.status(Status.BAD_REQUEST).entity(error).type(MediaType.TEXT_PLAIN).build();
				throw new WebApplicationException(errorResponse);
				
			} else if (dsorg.equals(PATRITIONED_DATA_SET_ORG)&&memberCheck==-1&&dsnAttributes.containsKey("records"))	 { 
				String error = Messages.getString("Datasets.ContentInvalid"); //$NON-NLS-1$ 
				Response errorResponse = Response.status(Status.BAD_REQUEST).entity(error).type(MediaType.TEXT_PLAIN).build();
				throw new WebApplicationException(errorResponse);
			}
		}
		// Test for invalid record length and block size. This is a z/os mf bug really
		if (dsnAttributes.containsKey("lrecl") && dsnAttributes.containsKey("blksize")) { //$NON-NLS-1$
			Object temp = dsnAttributes.get("lrecl");
			long lrecl = (temp instanceof String) ? Long.parseLong(((String)temp)) : ((Long)temp).longValue();
			temp = dsnAttributes.get("blksize");
			long blksize = (temp instanceof String) ? Long.parseLong(((String)temp)) : ((Long)temp).longValue();
			if (blksize%lrecl>0) {
				String error = String.format(Messages.getString("ZOSMFService.PossibleBlksizeIssue"), new Long(lrecl), new Long(blksize)); //$NON-NLS-1$
				Response errorResponse = Response.status(Status.BAD_REQUEST).entity(error).type(MediaType.TEXT_PLAIN).build();
				throw new WebApplicationException(errorResponse);
			}
		}
	}
	protected void createDatasetMergeAttributes(JSONObject dsnAttributes, JSONObject details) {
		if (details != null && !details.isEmpty() && ((Long)details.get("returnedRows")).longValue()>0l) {
			JSONArray items = (JSONArray) details.get("items");
			JSONObject item = (JSONObject)items.get(0);
			for (Object key : item.keySet()) {
				if (CREATE_PARMS.contains(key) &&!dsnAttributes.containsKey(key)) {
					if (key.equals("spacu") && !dsnAttributes.containsKey("alcunit")) { //$NON-NLS-1$
						dsnAttributes.put("alcunit", item.get(key).equals(DatasetAttributes.TRACKS) ? DatasetAttributes.ALLOCATE_TRACKS : 
							item.get(key).equals(DatasetAttributes.CYLINDERS) ? DatasetAttributes.ALLOCATE_CYLINDERS : DatasetAttributes.ALLOCATE_BLOCKS);
					} else if (key.equals("blksz") && !dsnAttributes.containsKey("blksize")) {  //$NON-NLS-1$  //$NON-NLS-2$
						dsnAttributes.put("blksize", item.get(key)); //$NON-NLS-1$
					} else if (item.containsKey("spacu") && key.equals("sizex") && !dsnAttributes.containsKey("primary")) {	
						switch ((String) item.get("spacu")) {
							case DatasetAttributes.TRACKS:
								dsnAttributes.put("primary", item.get("sizex")); //$NON-NLS-1$
								break;
							case DatasetAttributes.CYLINDERS:
								int size = (new Integer((String) item.get("sizex")).intValue() + 8) / 15; //$NON-NLS-1$
								dsnAttributes.put("primary", Integer.toString(size));
								break;
							default: // Blocks does not work (in zosmf) so default to tracks
								float fsize = ((new Float(((String) item.get("sizex"))).floatValue()) * 2); //$NON-NLS-1$
								dsnAttributes.put("primary", Integer.toString(Math.round(fsize)));
						}
					} else {
						dsnAttributes.put(key, item.get(key));
					}
				}
			}
		} else {
			String error = String.format(Messages.getString("ZOSMFService.DSNInvalid")); //$NON-NLS-1$
			Response errorResponse = Response.status(Status.NOT_ACCEPTABLE).entity(error).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(errorResponse);
		}
		dsnAttributes.remove("basedsn");
	}

	private void createDataset(String dsn, String attributes) {
		log.log(Level.INFO, "Creation attributes "+attributes); //$NON-NLS-1$
		int memberCheck = dsn.indexOf('(');
		String revisedDSN = memberCheck>-1 ? dsn.substring(0, memberCheck) : dsn;
		log.log(Level.INFO, "Creation dsn "+revisedDSN); //$NON-NLS-1$
		String requestURL = String.format("restfiles/ds/%s", revisedDSN); //$NON-NLS-1$
		Builder request = createRequest(requestURL);
		Response response = client.postRequestWithContent(request, attributes, MediaType.APPLICATION_JSON_TYPE);
		if (response.getStatus() != Status.CREATED.getStatusCode()) {
			String error = String.format(Messages.getString("ZOSMFService.CreateDatasetFailed"), dsn); //$NON-NLS-1$ 
			Response errorResponse = Response.status(response.getStatus()).entity(error).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(errorResponse);
		}
	}

	protected String createDatasetReformat(JSONObject dsnAttributes) { // Blocks does not work (in zosmf) so default to tracks
		String alcunit = DatasetAttributes.ALLOCATE_TRACKS; 
		Object primary = null; //$NON-NLS-1$		
		String dsorg = (String) dsnAttributes.get("dsorg"); //$NON-NLS-1$
		if (dsorg==null||(!dsorg.equals(PATRITIONED_DATA_SET_ORG)&&!dsorg.equals(PHYSICAL_SEQUENTIAL_DATA_SET_ORG)))	 { 
			String error = String.format(Messages.getString("ZOSMFService.DsorgNotSupported")); //$NON-NLS-1$
			Response errorResponse = Response.status(Status.NOT_ACCEPTABLE).entity(error).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(errorResponse);
		}
		if (dsnAttributes.containsKey("primary")) { //User specified
			primary = dsnAttributes.get("primary");
		}
		if (dsnAttributes.containsKey("alcunit")) { //User specified
			alcunit = (String) dsnAttributes.get("alcunit");
		}

		StringBuffer buffy = new StringBuffer();
		buffy.append('{');
		if (dsnAttributes.get("dsorg") != null) { //$NON-NLS-1$
			buffy.append("\"dsorg\":\"" + dsorg+ "\","); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (!dsorg.equals(PHYSICAL_SEQUENTIAL_DATA_SET_ORG)) {
			Object dirblk = dsnAttributes.get("dirblk");
			if (dirblk!=null) {
				buffy.append("\"dirblk\":" + dirblk +  ","); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				buffy.append("\"dirblk\":" + "40,");  //$NON-NLS-1$ //$NON-NLS-2$
			}		
		}
		if (dsnAttributes.get("recfm") != null) { //$NON-NLS-1$
			buffy.append("\"recfm\":\"" + dsnAttributes.get("recfm")+ "\","); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (dsnAttributes.get("lrecl") != null) { //$NON-NLS-1$
			buffy.append("\"lrecl\":" + dsnAttributes.get("lrecl")+ ","); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (dsnAttributes.get("blksize") != null) { //$NON-NLS-1$
			buffy.append("\"blksize\":" + dsnAttributes.get("blksize")+ ","); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} else if (dsnAttributes.get("blksz") != null) { //$NON-NLS-1$
			buffy.append("\"blksize\":" + dsnAttributes.get("blksz")+ ","); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (alcunit != null && !alcunit.equals(DatasetAttributes.ALLOCATE_BLOCKS)) {
			buffy.append("\"alcunit\":\"" + alcunit+ "\","); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (primary != null) {
			buffy.append("\"primary\":" + primary+ ","); //$NON-NLS-1$ //$NON-NLS-2$
			Object secondary = dsnAttributes.get("secondary");
			if (secondary!=null) {
				buffy.append("\"secondary\":" + secondary); //$NON-NLS-1$
			} else {
				buffy.append("\"secondary\":" + primary); //$NON-NLS-1$
			}
		}
		buffy.append('}');
		return buffy.toString();
	}

	public void deleteDataset(String dsn) {
		String requestURL = String.format("restfiles/ds/%s", dsn); //$NON-NLS-1$
		Builder request = createRequest(requestURL);
		Response response = client.sendRequest(request, HttpMethod.DELETE);

		if (response.getStatus() != Status.NO_CONTENT.getStatusCode()) {
			String error = String.format(Messages.getString("ZOSMFService.DeleteFailed"), dsn); //$NON-NLS-1$
			Response errorResponse = Response.status(response.getStatus()).entity(error).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(errorResponse);
		}

		return;
	}
	
}
