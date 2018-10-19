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

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.ibm.atlas.model.jobs.Job;
import com.ibm.atlas.model.jobs.JobStatus;
import com.ibm.atlas.webservice.Messages;
import com.ibm.atlas.webservice.exceptions.JobNotFoundException;
import com.ibm.atlas.webservice.resource.jobs.ZosmfJobsWithCacheService;
import com.ibm.atlas.webservice.resource.jobs.entity.DD;
import com.ibm.atlas.webservice.resource.jobs.entity.JobFile;
import com.ibm.atlas.webservice.resource.jobs.entity.JobNameList;
import com.ibm.atlas.webservice.resource.jobs.entity.OutputFile;
import com.ibm.atlas.webservice.resource.jobs.entity.Step;
import com.ibm.atlas.webservice.resource.jobs.entity.Subsystem;
import com.ibm.atlas.webservice.services.ZosmfJobsService;
import com.ibm.atlas.webservice.utilities.JobUtilities;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path(value = "/jobs")
@Api(value = "Zowe : JES Jobs APIs")
public class Jobs {

	private static final String JOB_NOT_FOUND = Messages.getString("Jobs.JESNameNotFound"); //$NON-NLS-1$

	@Context
	UriInfo uriInfo;

	@Inject
	ZosmfJobsService zosmfService;

	@Inject
	ZosmfJobsWithCacheService zosmfWithCacheService;

	@Inject
	Logger log;
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get a list of jobs", 
                  notes = "This API returns the a list of jobs for a given prefix and owner.")
	@ApiResponses({@ApiResponse(code = 200, message = "Ok", response = JobNameList.class, responseContainer = "List")})
	public List<JobNameList> getJobs(@Context SecurityContext securityContext,
			@ApiParam(value = "Job name prefix. If omitted, defaults to '*'.", required = false) @DefaultValue("*") @QueryParam("prefix") String prefix,
			@ApiParam(value = "Job owner. Defaults to requester's userid.", required = false) @QueryParam("owner") String owner,
			@ApiParam(value = "Job status to filter on, defaults to ALL.", allowableValues = "ACTIVE, OUTPUT, INPUT, ALL", required = false) @QueryParam("status") JobStatus status) {
		String ownerFilter = JobUtilities.getOwnerFilterValue(securityContext, owner);
		if (status == null) {
			status = JobStatus.ALL;
		}
		return zosmfService.getJobs(prefix, ownerFilter, status);
	}

	//TODO LATER - remove this as equivalent to getJobs?
	@GET
	@Path(value = "{jobName}/ids")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get a list of job identifiers for a given job name", 
	              notes = "This API returns a list of job identifiers (if any) for a given job name.")
	@ApiResponses({@ApiResponse(code = 200, message = "Ok", response = JobNameList.class, responseContainer = "List")})
	public List<JobNameList> getJobIdsByName(@Context SecurityContext securityContext,
			@ApiParam(value = "Job name.", required = true) @PathParam("jobName") String jobName,
			@ApiParam(value = "Job owner. Defaults to requester's userid.", required = false) @QueryParam("owner") String owner,
			@ApiParam(value = "Job status to filter on, defaults to ALL.", allowableValues = "ACTIVE, OUTPUT, INPUT, ALL", required = false) @QueryParam("status") JobStatus status) {
		String ownerFilter = JobUtilities.getOwnerFilterValue(securityContext, owner);
		if (status == null) {
			status = JobStatus.ALL;
		}
		return zosmfService.getJobs(jobName, ownerFilter, status);
	}

	@GET
	@Path(value = "{jobName}/{jobId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get the details of a job for a given job name and identifier", 
	              notes = "This API returns the details of a job for a given job name and identifier.")
	@ApiResponses({@ApiResponse(code = 200, message = "Ok", response = Job.class)})
	public Job getJobByNameAndId(@ApiParam(value = "Job name.", required = true) @PathParam("jobName") String jobName,
			@ApiParam(value = "Job identifier.", required = true) @PathParam("jobId") String jobId) {
		try {
			return zosmfWithCacheService.getJob(jobName, jobId, true);
		} catch (JobNotFoundException e) {
			throw e.createWebApplicationException();
		} 
	}



	@GET
	@Path(value = "{jobName}/ids/{jobId}/steps")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get job steps for a given job", 
	              notes = "This API returns the step name and executed program for each job step for a given job name and identifier.")
	@ApiResponses({@ApiResponse(code = 200, message = "Ok", response = Step.class, responseContainer = "List")})
	public List<Step> getJobSteps(@ApiParam(value = "Job name.", required = true) @PathParam("jobName") String jobName,
			@ApiParam(value = "Job identifier.", required = true) @PathParam("jobId") String jobId) {

		String records = zosmfService.getJobJCLRecords(jobName, jobId);
		
		if (records == null) {
			String error = String.format(Messages.getString("Jobs.NoSteps"), jobName, jobId); //$NON-NLS-1$
			throw createNotFoundException(error);
		}

		return JobUtilities.findJobSteps(records);
	}

	@GET
	@Path(value = "{jobName}/ids/{jobId}/steps/{stepNumber}/dds")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get dataset definitions (DDs) for a given job step", 
	              notes = "This API returns the JCL dataset definitions (DDs) for a job step.")
	@ApiResponses({@ApiResponse(code = 200, message = "Ok", response = DD.class, responseContainer = "List")})
	public List<DD> getJobStepDDs(@ApiParam(value = "Job name.", required = true) @PathParam("jobName") String jobName,
			@ApiParam(value = "Job identifier.", required = true) @PathParam("jobId") String jobId,
			@ApiParam(value = "Job step number.", required = true) @PathParam("stepNumber") int stepNumber) {

		String records = zosmfService.getJobJCLRecords(jobName, jobId);

		if (records == null) {
			String error = String.format(Messages.getString("Jobs.NoDD"), stepNumber, jobName, jobId); //$NON-NLS-1$
			throw createNotFoundException(error);
		}
		
		List<DD> dds = JobUtilities.findJobDDs(records, stepNumber);
		
		if (dds == null) {
			String error = String.format(Messages.getString("Jobs.NoStep"), stepNumber, jobName, jobId); //$NON-NLS-1$
			throw createNotFoundException(error);
		}
		
		return dds;
	}

	@GET
	@Path(value = "{jobName}/ids/{jobId}/files")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get a list of output file names for a job", 
	              notes = "This API returns the output file names for a given job.")
	@ApiResponses({@ApiResponse(code = 200, message = "Ok", response = JobFile.class, responseContainer = "List")})
	public List<JobFile> getJobOutputFiles(
			@ApiParam(value = "Job name.", required = true) @PathParam("jobName") String jobName,
			@ApiParam(value = "Job identifier.", required = true) @PathParam("jobId") String jobId) {

		List<JobFile> files = new LinkedList<>();
		JSONArray fileList = zosmfService.getJobFiles(jobName, jobId);

		if (fileList == null) {
			String error = String.format(Messages.getString("Jobs.NoJob"), jobName, jobId); //$NON-NLS-1$
			throw createNotFoundException(error);
		}

		for (int i = 0; i < fileList.size(); i++) {
			JSONObject file = (JSONObject) fileList.get(i);
			JobFile jobFile = new JobFile();
			jobFile.setDdname((String) file.get("ddname")); //$NON-NLS-1$
			jobFile.setId((long) file.get("id")); //$NON-NLS-1$
			jobFile.setLrecl((long) file.get("lrecl")); //$NON-NLS-1$
			jobFile.setByteCount((long) file.get("byte-count")); //$NON-NLS-1$
			jobFile.setRecfm((String) file.get("recfm")); //$NON-NLS-1$
			jobFile.setRecordCount((long) file.get("record-count")); //$NON-NLS-1$
			files.add(jobFile);
		}

		return files;
	}

	@GET
	@Path(value = "{jobName}/ids/{jobId}/files/{fileId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Read content from a specific job output file", 
	              notes = "This API reads content from a specific job output file. The API can read all output, or a relative record range.")
	@ApiResponses({@ApiResponse(code = 200, message = "Ok", response = OutputFile.class)})
	public OutputFile getJobOutputFile(@ApiParam(value = "Job name.", required = true) @PathParam("jobName") String jobName,
			@ApiParam(value = "Job identifier.", required = true) @PathParam("jobId") String jobId,
			@ApiParam(value = "Job file id number.", required = true) @PathParam("fileId") String fileId,
			@ApiParam(value = "Optional starting relative record number to read.", required = false) @QueryParam("start") String start,
			@ApiParam(value = "Optional ending relative record number to read. If omitted, all records are returned.", required = false) @QueryParam("end") String end) {

		String output = zosmfService.getJobFileRecordsByRange(jobName, jobId, fileId, start, end);

		if (output == null) {
			String error = String.format(Messages.getString("Jobs.NoFile"), jobName, jobId, fileId); //$NON-NLS-1$
			throw createNotFoundException(error);
		}

		return new OutputFile(output);
	}

	@GET
	@Path(value = "{jobName}/ids/{jobId}/files/{fileId}/tail")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Read the tail of a job's output file", 
	              notes = "This API returns the tail end of a job's output file. The number of records to tail can be specified.")
	@ApiResponses({@ApiResponse(code = 200, message = "Ok", response = OutputFile.class)})
	public OutputFile getJobOutputTail(@ApiParam(value = "Job name.", required = true) @PathParam("jobName") String jobName,
			@ApiParam(value = "Job identifier.", required = true) @PathParam("jobId") String jobId,
			@ApiParam(value = "Job file id number.", required = true) @PathParam("fileId") String fileId,
			@ApiParam(value = "Number of records to tail. Default is 24.", required = false) @DefaultValue("24") @QueryParam("records") String records) {

		JSONArray fileList = zosmfService.getJobFiles(jobName, jobId);

		if (fileList == null) {
			String error = String.format(Messages.getString("Jobs.NoFile"), jobName, jobId, fileId); //$NON-NLS-1$
			throw createNotFoundException(error);
		}

		String output = ""; //$NON-NLS-1$
		for (Object fileObject : fileList) {
			JSONObject file = (JSONObject) fileObject;
			long id = (long) file.get("id"); //$NON-NLS-1$
			if (fileId.equals(Long.toString(id))) {
				long recNumber = Integer.parseInt(records);
				long recordCount = (long) file.get("record-count"); //$NON-NLS-1$
				if (recNumber > recordCount) {
					recNumber = recordCount;
				}
				String start = Long.toString(recordCount - recNumber);
				String end = Long.toString(recordCount - 1);

				output = zosmfService.getJobFileRecordsByRange(jobName, jobId, fileId, start, end);

				if (output != null) {
					break;
				}
			}
		}
		
		return new OutputFile(output);
	}

	@GET
	@Path(value = "{jobName}/ids/{jobId}/subsystem")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get the subsystem type for a job", 
	              notes = "This API returns the related subsystem type (if any) for a given job. "
	                    + "Recognized subsystems include: CICS, DB2, IMS, MQ and TSO.")
	@ApiResponses({@ApiResponse(code = 200, message = "Ok", response = Subsystem.class)})
	public Subsystem getSubsysFromJobOutput(
			@ApiParam(value = "Job name.", required = true) @PathParam("jobName") String jobName,
			@ApiParam(value = "Job identifier.", required = true) @PathParam("jobId") String jobId) {

		return new Subsystem(zosmfService.getJobSubsystem(jobName, jobId));
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Submit a job and get the job id back", 
	  notes = "This API submits a partitioned data set member or Unix file. "
			+"For fully qualified dataset members use 'MYJOBS.TEST.CNTL(TESTJOBX)' "
			  +"For non fully qualified use TEST.CNTL(TESTJOBX) "
				+ "For Unix files use /u/myjobs/job1" )
	@ApiResponses({@ApiResponse(code = 201, response = Job.class, message = "Job successfully created")})
	public Response submitJob(@ApiParam(value = "USS file path or Data set name in the form: {\"file\":\"'ATLAS.TEST.JCL(TSTJ0001)'\"}, {\"file\":\"TEST.JCL(TSTJ0001)\"}, {\"file\":\"/u/myjobs/job1\"}", required = true) String dsn) {

		JSONObject data = null;
		try {
			data = JSONObject.parse(dsn);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (data != null && data.get("file") != null) {
			Job job = zosmfService.submitJob((String) data.get("file"));
			UriBuilder builder = uriInfo.getAbsolutePathBuilder();
	        builder.path(job.getJobName());
	        builder.path(job.getJobId());
			return Response.created(builder.build()).entity(job).build();
		}
		String error = Messages.getString("Jobs.InvalidSubmitData");
		throw createNotFoundException(error);
	}
	
	@DELETE
	@Path(value = "{jobName}/{jobId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Cancel a Job and Purge it's associated files", 
	  notes = "This api purges a Job")
	@ApiResponses({@ApiResponse(code = 204, message = "Job purge succesfully requested")})
	public Response purgeJob(
			@ApiParam(value = "Job name", required = true) @PathParam("jobName") String jobName,
			@ApiParam(value = "Job identifier", required =true) @PathParam("jobId")String jobId) {
		
		zosmfService.purgeJob(jobName, jobId);
		
		return Response.status(Status.NO_CONTENT).build();
	}
	
	private static WebApplicationException createNotFoundException(String errorMessage) {
		Response errorResponse = Response.status(Status.NOT_FOUND).entity(errorMessage).type(MediaType.TEXT_PLAIN).build();
		return new WebApplicationException(errorResponse);
	}
}