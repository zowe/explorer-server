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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ibm.atlas.model.jobs.Job;
import com.ibm.atlas.model.jobs.JobStatus;
import com.ibm.atlas.webservice.Messages;
import com.ibm.atlas.webservice.exceptions.JobNotFoundException;
import com.ibm.atlas.webservice.resource.jobs.entity.JobNameList;
import com.ibm.atlas.webservice.utilities.JobUtilities;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class ZosmfJobsService extends ZosmfService {

	private static final String JCL_STRUCTURE_PDS = "\"//%s\""; //$NON-NLS-1$
	private static final String JCL_STRUCTURE_UNIX = "\"%s\""; //$NON-NLS-1$
	
	public JSONArray getJobFiles(String jobName, String jobId) {
		String requestURL = String.format("restjobs/jobs/%s/%s/files", jobName, jobId); //$NON-NLS-1$
		Builder request = createRequest(requestURL);
		Response response = client.sendRequest(request, HttpMethod.GET);

		if (response.getStatus() == Status.OK.getStatusCode()) {
			String entity = response.readEntity(String.class);
			try {
				return JSONArray.parse(entity);
			} catch (IOException e) {
				throw createJSONParseException(e);
			}
		}

		return null;
	}

	public String getJobFileRecords(String jobName, String jobId, String fileId) {
		String requestURL = String.format("restjobs/jobs/%s/%s/files/%s/records", jobName, jobId, fileId); //$NON-NLS-1$
		Builder request = createRequest(requestURL);
		Response response = client.sendRequest(request, HttpMethod.GET);

		String records = null;
		if (response.getStatus() == Status.OK.getStatusCode()) {
			records = response.readEntity(String.class);
		} else {
			String error = String.format(Messages.getString("ZOSMFService.AttemptFail"), jobName, jobId, fileId); //$NON-NLS-1$
			Response errorResponse = Response.status(response.getStatus()).entity(error).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(errorResponse);
		}

		return records;
	}

	public String getJobJCLRecords(String jobName, String jobId) {
		Response response = getJobJCLRecordsResponse(jobName, jobId);

		if (response.getStatus() == Status.OK.getStatusCode()) {
			return response.readEntity(String.class);
		}

		return null;
	}

	private Response getJobJCLRecordsResponse(String jobName, String jobId) {
		String requestURL = String.format("restjobs/jobs/%s/%s/files/3/records", jobName, jobId); //$NON-NLS-1$
		Builder request = createRequest(requestURL);
		return client.sendRequest(request, HttpMethod.GET);
	}

	public List<JobNameList> getJobs(String prefix, String owner, JobStatus jobStatus) {
		String requestURL = "restjobs/jobs"; //$NON-NLS-1$
		String queryPrefix = "*"; //$NON-NLS-1$
		String queryOwner = "*"; //$NON-NLS-1$

		if (prefix != null) {
			queryPrefix = prefix;
		}
		if (owner != null) {
			queryOwner = owner;
		}

		Builder request = createRequest(requestURL, "prefix", queryPrefix, "owner", queryOwner); //$NON-NLS-1$ //$NON-NLS-2$

		Response response = client.sendRequest(request, HttpMethod.GET);

		if (response.getStatus() != Status.OK.getStatusCode()) {
			String error = String.format(Messages.getString("ZOSMFService.RequestFailed"), queryPrefix, queryOwner); //$NON-NLS-1$
			Response errorResponse = Response.status(response.getStatus()).entity(error).build();
			throw new WebApplicationException(errorResponse);
		}

		String result = response.readEntity(String.class);
		JSONArray jsonArray = null;
		try {
			jsonArray = JSONArray.parse(result);
		} catch (IOException e) {
			String error = String.format(Messages.getString("ZOSMFService.BodyFailed"), e.getMessage()); //$NON-NLS-1$
			log.log(Level.SEVERE, error, e);
			Response errorResponse = Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(errorResponse);
		}
		
		Map<String, JobNameList> results = new HashMap<>();
		for (int i = 0; i < jsonArray.size(); i++) {
			JSONObject json = (JSONObject) jsonArray.get(i);
			Job job = getJobFromJSONObject(json);
			if (jobStatus.matches(job.getStatus())) {
				String jobName = job.getJobName();
				JobNameList jobNameList = results.get(jobName);
				if (jobNameList == null) {
					jobNameList = new JobNameList(jobName);
					results.put(jobName, jobNameList);
				}
				jobNameList.addJobInstance(job);
			}
		}
		
		List<JobNameList> resultList = new ArrayList<>();
		resultList.addAll(results.values());
		return resultList;
	}

	public JSONArray getJobIds(String jobName, String owner) {
		JSONArray jobs = null;
		String requestURL = "restjobs/jobs"; //$NON-NLS-1$
		String queryOwner = "*"; //$NON-NLS-1$

		if (owner != null) {
			queryOwner = owner;
		}

		if (jobName != null) {
			Builder request = createRequest(requestURL, "prefix", jobName, "owner", queryOwner); //$NON-NLS-1$ //$NON-NLS-2$

			Response response = client.sendRequest(request, HttpMethod.GET);

			if (response.getStatus() != Status.OK.getStatusCode()) {
				String error = String.format(Messages.getString("ZOSMFService.JobIdsFailed"), jobName, queryOwner); //$NON-NLS-1$
				Response errorResponse = Response.status(response.getStatus()).entity(error).build();
				throw new WebApplicationException(errorResponse);
			}

			String result = response.readEntity(String.class);
			try {
				jobs = JSONArray.parse(result);
			} catch (IOException e) {
				String error = String.format(Messages.getString("ZOSMFService.BodyFailed"), e.getMessage()); //$NON-NLS-1$
				log.log(Level.SEVERE, error, e);
				Response errorResponse = Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).type(MediaType.TEXT_PLAIN).build();
				throw new WebApplicationException(errorResponse);
			}

		} else {
			String error = Messages.getString("ZOSMFService.JobIdsFailedNoName"); //$NON-NLS-1$
			Response errorResponse = Response.status(Status.BAD_REQUEST).entity(error).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(errorResponse);
		}

		return jobs;
	}

	public String getJobFileRecordsByRange(String jobName, String jobId, String fileId, String start, String end) {
		String requestURL = String.format("restjobs/jobs/%s/%s/files/%s/records", jobName, jobId, fileId); //$NON-NLS-1$

		Builder request = createRequest(requestURL);
		// By default, request the whole file
		if (start != null && end != null) {
			request = request.header("X-IBM-Record-Range", start + "-" + end); //$NON-NLS-1$ //$NON-NLS-2$
		}
		Response response = client.sendRequest(request, HttpMethod.GET);

		String records = null;
		if (response.getStatus() == Status.OK.getStatusCode()) {
			records = response.readEntity(String.class);
		} else {
			String error = String.format(Messages.getString("ZOSMFService.JobContetntRequestFailed"), jobName, jobId, fileId); //$NON-NLS-1$
			Response errorResponse = Response.status(response.getStatus()).entity(error).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(errorResponse);
		}

		return records;
	}

	public JobStatus getJobStatus(String jobName, String jobId) {
		Job job = null;
		try {
			job = getJob(jobName, jobId);
		} catch (JobNotFoundException e) {
			//TODO LATER - previous code wants null rather than exception - improve this
		}
		if (job != null) {
			return job.getStatus();
		}
		return null;
	}

	public Job submitJob(String dsn) {
		Builder request = createRequest("restjobs/jobs"); //$NON-NLS-1$
		String jsonContent = dsn.startsWith("/") ? //$NON-NLS-1$
				"{ \"file\" : " + String.format(JCL_STRUCTURE_UNIX, dsn) + " }" //$NON-NLS-1$ //$NON-NLS-2$
				:"{ \"file\" : " + String.format(JCL_STRUCTURE_PDS, dsn) + " }"; //$NON-NLS-1$ //$NON-NLS-2$

		request = request.header("X-IBM-Data-Type", "text"); //$NON-NLS-1$ //$NON-NLS-2$
		request = request.header("Content-Type", "text/plain"); //$NON-NLS-1$ //$NON-NLS-2$
		Response response = client.putRequestWithContent(request, jsonContent, MediaType.APPLICATION_JSON_TYPE);

		if (response.getStatus() != Status.CREATED.getStatusCode()) {
			String error = String.format(Messages.getString("ZOSMFService.SubmitFailed"), dsn); //$NON-NLS-1$
			log.log(Level.SEVERE, error);
			Response errorResponse = Response.status(response.getStatus()).entity(error).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(errorResponse);
		}

		String entity = response.readEntity(String.class);
		try {
			JSONObject returned = JSONObject.parse(entity);
			return getJobFromJSONObject(returned);
		} catch (IOException e) {
			throw createJSONParseException(e);
		}
	}

	public void purgeJob(String jobName, String jobId) {
		String requestURL = String.format("restjobs/jobs/%s/%s", jobName, jobId); //$NON-NLS-1$
		Builder request = createRequest(requestURL);

		Response response = client.sendRequest(request, HttpMethod.DELETE);

		if (response.getStatus() != Status.ACCEPTED.getStatusCode()) {
			String error = String.format(Messages.getString("ZOSMFService.PurgeFailed"), jobName, jobId); //$NON-NLS-1$
			Response errorResponse = Response.status(response.getStatus()).entity(error).type(MediaType.TEXT_PLAIN).build();
			throw new WebApplicationException(errorResponse);
		}

		return;
	}

	public String getJobSubsystem(String jobName, String jobId) {
		String subsys = null;
		if (jobId.startsWith("TSU")) { //$NON-NLS-1$
			subsys = "TSO"; //$NON-NLS-1$
		} else {
			Response response = getJobJCLRecordsResponse(jobName, jobId);
			if (response.getStatus() == Status.OK.getStatusCode()) {
				String records = response.readEntity(String.class);
				if (records != null) {
					subsys = JobUtilities.findJobSubsystem(jobId, records);
				}
			} else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
				log.info("Internal server error: Not Authorised response getJobJCLRecords " + response.getStatus()); //$NON-NLS-1$
				subsys = ""; //$NON-NLS-1$
			}
		}

		return subsys;
	}

	public Job getJob(String jobName, String jobId) throws JobNotFoundException {
		String requestURL = String.format("restjobs/jobs/%s/%s", jobName, jobId); //$NON-NLS-1$
		Builder request = createRequest(requestURL);
		Response response = client.sendRequest(request, HttpMethod.GET);

		if (response.getStatus() != Status.OK.getStatusCode()) {
			JobNotFoundException exception = new JobNotFoundException(jobName, jobId, response.getStatus());
			log.log(Level.SEVERE, exception.getMessage());
			throw exception;
		}
		
		String entity = response.readEntity(String.class);
		try {
			JSONObject returned = JSONObject.parse(entity);
			return getJobFromJSONObject(returned);
		} catch (IOException e) {
			throw createJSONParseException(e);
		}
	}

	private static Job getJobFromJSONObject(JSONObject returned) {
		return Job.builder().jobId((String) returned.get("jobid")) //$NON-NLS-1$
				.jobName((String) returned.get("jobname")) //$NON-NLS-1$
				.owner((String) returned.get("owner")) //$NON-NLS-1$
				.type((String) returned.get("type")) //$NON-NLS-1$
				.status(JobStatus.valueOf((String) returned.get("status"))) //$NON-NLS-1$
				.returnCode((String) returned.get("retcode")) //$NON-NLS-1$
				.subsystem((String) returned.get("subsystem")) //$NON-NLS-1$
				.executionClass((String) returned.get("class")) //$NON-NLS-1$
				.phaseName((String) returned.get("phase-name")) //$NON-NLS-1$
				.build();
	}

	
}
