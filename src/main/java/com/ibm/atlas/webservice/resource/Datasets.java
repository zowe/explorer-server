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

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.ibm.atlas.model.datasets.DataSetContentResponse;
import com.ibm.atlas.webservice.Messages;
import com.ibm.atlas.webservice.resource.datasets.entity.DatasetAttributes;
import com.ibm.atlas.webservice.services.ZosmfDatasetsService;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path(value = "/datasets")

@Api(value = "Atlas : Dataset APIs")
public class Datasets {

	@Context
	private UriInfo uriInfo;

	@Inject
	private Logger log;

	@Inject
	private ZosmfDatasetsService zosmfService;

	/**
	 * Gets a list of dataset names by filter
	 * 
	 * @param filter
	 *            Dataset name filter, e.g. HLQ.**, **.SUFFIX, etc
	 * @return List of dataset names that match the given filter.
	 */
	@GET
	@Path(value = "{filter}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get a list of data sets by filter", 
	  notes = "This API returns a list of data sets according to a given filter.")
	@ApiResponses({@ApiResponse(code = 200, message = "Ok", response = String.class, responseContainer = "List")})
	public List<String> getDSNs(
			@ApiParam(value = "Dataset filter string, e.g. HLQ.\\*\\*, \\*\\*.SUF, etc.", required = true) @PathParam("filter") String filter) {

		JSONObject datasets = zosmfService.listDatasets(filter);

		List<String> datasetNames = new LinkedList<>();
		JSONArray dsnList = (JSONArray) datasets.get("items"); //$NON-NLS-1$
		for (int i = 0; i < dsnList.size(); i++) {
			JSONObject entry = (JSONObject) dsnList.get(i);
			datasetNames.add((String) entry.get("dsname")); //$NON-NLS-1$
		}

		return datasetNames;
	}

	/**
	 * Get a list of members for a given PDS(E)
	 * 
	 * @param dsn
	 *            The PDS(E) for which the member list is required
	 * @return List of members
	 */
	@GET
	@Path(value = "{dsn}/members")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get a list of members for a partitioned data set", 
	  notes = "This API returns a list of members for a given partitioned data set.")
	@ApiResponses({@ApiResponse(code = 200, message = "Ok", response = String.class, responseContainer = "List")})
	public List<String> getDSNMembers(
			@ApiParam(value = "Partitioned data set name", required = true) @PathParam("dsn") String dsn) {

		JSONObject members = zosmfService.listDatasetMembers(dsn);

		List<String> memberNames = new LinkedList<>();
		JSONArray memberList = (JSONArray) members.get("items"); //$NON-NLS-1$
		for (int i = 0; i < memberList.size(); i++) {
			JSONObject entry = (JSONObject) memberList.get(i);
			memberNames.add((String) entry.get("member")); //$NON-NLS-1$
		}

		return memberNames;
	}

	/**
	 * Get the content of a sequential dataset.
	 * 
	 * @param dsn
	 *            The dataset name to read
	 * @param start
	 *            Optional query paramater indicating starting relative record
	 *            number to read
	 * @param end
	 *            Optional query paramater indicating ending relative record
	 *            number to read
	 * @return The requested content from the named dataset
	 */
	@GET
	@Path(value = "{dsn}/content")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Read content from a data set or member", 
	  notes = "This API reads content from a sequential data set or member of a partitioned data set.")
	@ApiResponses({@ApiResponse(code = 200, message = "Ok", response = DataSetContentResponse.class)})
	public DataSetContentResponse getDSNContent(@ApiParam(value = "Data set name, e.g. HLQ.PS or HLQ.PO(MEMBER)", required = true) @PathParam("dsn") String dsn,
			@ApiParam(value = "Indicator to codepage convert content", required = false) @QueryParam("convert") @DefaultValue("true") boolean convert,
			@ApiParam(value = "Indicator to return a checksum (if planning subsequent write)", required = false) @QueryParam("checksum") boolean checksum,
			@ApiParam(value = "Starting relative record number to read. Defaults to record 0.", required = false) @QueryParam("start") String start,
			@ApiParam(value = "Ending relative record number to read. If not specified, all records are read.", required = false) @QueryParam("end") String end) {

		return zosmfService.getContent(dsn, convert, checksum, start, end);
	}

	@PUT
	@Path(value = "{dsn}/content")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Write content to a data set or member", 
	  notes = "This API writes content to a sequential data set or partitioned data set member.")
	@ApiResponses({@ApiResponse(code = 200, message = "Ok")})
	public Response putDSNContent(@ApiParam(value = "Dataset name", required = true) @PathParam("dsn") String dsn,
			@ApiParam(value = "Request content (content-type:application/json) in the form: {\"records\":\"data Content\",\"checksum\":\"checksum_value\"} "
					+ "If checksum is passed and it does not match the checksum returned by a previous read, "
					+ "it is deemed a concurrent update has occured, and the write fails.", required = true) String content) {

		JSONObject dsnContent = null;

		try {
			dsnContent = JSONObject.parse(content);
		} catch (Exception e) {
			String error = String.format(Messages.getString("Datasets.RequestError"), e.getMessage()); //$NON-NLS-1$
			return Response.status(Status.BAD_REQUEST).entity(error).type(MediaType.TEXT_PLAIN).build();
		}

		String records = (String) dsnContent.get("records"); //$NON-NLS-1$
		String checksum = (String) dsnContent.get("checksum"); //$NON-NLS-1$

		if (checksum != null) {
			zosmfService.putConvertedContentWithChecksum(dsn, records, checksum);
		} else {
			zosmfService.putConvertedContent(dsn, records);
		}

		return Response.status(Status.OK).build();
	}

	/*
	@POST
	@Path(value = "{dsn}/content")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Create a dataset/member with content", notes = "This API creates a dataset/member with the provided content.", response = Response.class)
	public Response createDSNContent(@ApiParam(value = "Dataset name", required = true) @PathParam("dsn") String dsn,
			@ApiParam(value = "Request content (content-type:application/json) in the form: {\"records\":\"data Content\"}", required = true) String content) {

		boolean result = false;
		JSONObject dsnContent = null;

		try {
			dsnContent = JSONObject.parse(content);
		} catch (Exception e) {
			String error = "Exception parsing request content";
			log.log(Level.SEVERE, error, e);
			return ResponseUtils.fail(Status.BAD_REQUEST, error);
		}

		try {
			Content target = zosmfService.getContent(dsn, false, false, "0", "0");
		} catch ( )

		if (target != null) {
			String error = String.format("Dataset '%s' already exists", dsn);
			log.warning(error);
			return ResponseUtils.fail(Status.CONFLICT, error);
		}

		String records = (String) dsnContent.get("records");

		result = zosmfService.putConvertedContent(dsn, records);

		if (!result) {
			String error = String.format("Could not write content using z/OSMF for dataset '%s'", dsn);
			log.warning(error);
			return ResponseUtils.fail(Status.CONFLICT, error);
		}

		return ResponseUtils.success();
	}
	*/

	@POST
	@Path(value = "{dsn}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Create (and populate) a data set", 
	  notes = "This API uses z/OSMF to create the data set and uses the syntax and rules described in the z/OSMF Programming Guide. " + 
			  "A dataset can be created based directly upon another dataset's attributes by using the \"basedsn\" reference in the json body. " +
			  "The attributes for the dataset are derived from the \"basedsn\" file but can be overwritten with explicit definitions in the json body. " +
			  "Content can be written to a newly created Physical Sequential (PS) file using the \"records\" reference " )
	@ApiResponses({@ApiResponse(code = 201, message = "Data set successfully created")})
	public Response createDataset(@ApiParam(value = "Data set name", required = true) @PathParam("dsn") String dsn,
			@ApiParam(value = "Request content (content-type:application/json) in the form, "
					+ "{\"dsorg\":\"PO\",\"alcunit\":\"TRK\",\"primary\":10,\"secondary\":5,\"dirblk\":10,\"recfm\":\"FB\",\"blksize\":400,\"lrecl\":80}   "
					+ "{\"basedsn\":\"AUSER.TEST.JCL\",\"secondary\":5,\"dirblk\":10}                                  "
					+ "{\"basedsn\":\"AUSER.TEST.REPORT\",\"records\":\"This is my test report\"}                      "
					+ "", required = true) String attributes) {

		int memberCheck = dsn.indexOf('(');
		String revisedDSN = memberCheck>-1 ? dsn.substring(0, memberCheck-1) : dsn;
		JSONObject details = zosmfService.getDatasetAttributes(revisedDSN);
		JSONArray items = (JSONArray) details.get("items"); //$NON-NLS-1$
		if (memberCheck==-1 && items.size() != 0 ) {
			String error = String.format(Messages.getString("Datasets.DatasetExists"), revisedDSN); //$NON-NLS-1$
			return Response.status(Status.CONFLICT).entity(error).type(MediaType.TEXT_PLAIN).build();
		}
		
		zosmfService.createDatasetExtended(dsn, attributes);		
		UriBuilder builder = uriInfo.getAbsolutePathBuilder();
		return Response.created(builder.build()).build();
	}
	
	@DELETE
	@Path(value = "{dsn}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Delete a data set or member", 
	  notes = "This API deletes a sequential data set or partitioned data set member.")
	@ApiResponses({@ApiResponse(code = 204, message = "Data set or member successfully deleted")})
	public Response deleteDatasetMember(@ApiParam(value = "Data set name", required = true) @PathParam("dsn") String dsn) {

		zosmfService.deleteDataset(dsn);		
	
		return Response.status(Status.NO_CONTENT).build();
	}

	/**
	 * Get dataset attributes (name, recfm, lrecl, blksize, dsorg) for a named
	 * dataset(s)
	 * 
	 * @param dsn
	 *            The dataset name. Can be fully or partially qualified.
	 * @return Attributes of named dataset(s).
	 */
	@GET
	@Path(value = "{dsn}/attributes")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Retrieve attributes of a data set(s)", 
	  notes = "This API returns the attributes of a data set (or data sets) including the RECFM, BLKSIZE, and LRECL.")
	@ApiResponses({@ApiResponse(code = 200, message = "Ok", response = DatasetAttributes.class, responseContainer = "List")})
	public List<DatasetAttributes> getDSNMemberDetails(
			@ApiParam(value = "Data set name, e.g. HLQ.SEQ, HLQ.\\*\\*, \\*\\*.SUF, etc.", required = true) @PathParam("dsn") String dsn) {

		JSONObject output = zosmfService.getDatasetAttributes(dsn);

		List<DatasetAttributes> list = new LinkedList<>();
		JSONArray dataset = (JSONArray) output.get("items"); //$NON-NLS-1$
		for (int i = 0; i < dataset.size(); i++) {
			JSONObject details = (JSONObject) dataset.get(i);
			if (details != null) {
				DatasetAttributes attributes = new DatasetAttributes();
				attributes.setName((String) details.get("dsname")); //$NON-NLS-1$
				attributes.setBlksize((String) details.get("blksz")); //$NON-NLS-1$
				attributes.setLrecl((String) details.get("lrecl")); //$NON-NLS-1$
				attributes.setRecfm((String) details.get("recfm")); //$NON-NLS-1$
				attributes.setDsorg((String) details.get("dsorg")); //$NON-NLS-1$
//				attributes.setCatnm((String) details.get("catnm")); //$NON-NLS-1$
//				attributes.setCdate((String) details.get("cdate")); //$NON-NLS-1$
//				attributes.setDev((String) details.get("dev")); //$NON-NLS-1$				
//				attributes.setEdate((String) details.get("edate")); //$NON-NLS-1$
//				attributes.setExtx((String) details.get("extx")); //$NON-NLS-1$				
//				attributes.setMigr((String) details.get("migr")); //$NON-NLS-1$
//				attributes.setOvf((String) details.get("ovf")); //$NON-NLS-1$
//				attributes.setRdate((String) details.get("rdate")); //$NON-NLS-1$
//				attributes.setSizex((String) details.get("sizex")); //$NON-NLS-1$
//				attributes.setSpacu((String) details.get("spacu")); //$NON-NLS-1$	
//				attributes.setUsed((String) details.get("used")); //$NON-NLS-1$
//				attributes.setVol((String) details.get("vol")); //$NON-NLS-1$				
				list.add(attributes);
			}
		}
		return list;
	}

}