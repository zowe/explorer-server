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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Test;

import com.ibm.atlas.model.jobs.Job;
import com.ibm.atlas.model.jobs.JobStatus;
import com.ibm.atlas.webservice.resource.jobs.entity.JobNameList;

public class ZosmfJobsServiceTest extends AbstractZosmfServiceTest<ZosmfService> {

	ZosmfJobsService zosmfService;

	@Before
	public void setUp() {
		this.zosmfService = new ZosmfJobsService();
		super.init(zosmfService);
	}

	@Test
	public void getJobByIdShouldReturnJobCorrectly() throws Exception {
		String jobId = "TSU06342";
		String jobName = "STEVENH";

		Job expected = createJob("TSU06342", JobStatus.OUTPUT, "Job is on the hard copy queue", "ABEND S222");
		
		Response response = mock(Response.class);
		when(response.getStatus()).thenReturn(Status.OK.getStatusCode());
		when(response.readEntity(String.class)).thenReturn(loadTestFile("zosmfJobResponse.json"));
		String getJobRelativeUri = "restjobs/jobs/" + jobName + "/" + jobId;
		mockRequestResponse(getJobRelativeUri, HttpMethod.GET, response);

		assertEquals(expected, zosmfService.getJob(jobName, jobId));
	}

	@Test
	public void getAllStatusJobsReturnCorrectly() throws Exception {
		Job tsu06342 = createJob("TSU06342", JobStatus.OUTPUT, "Job is on the hard copy queue", "ABEND S222");
		Job tsu06806 = createJob("TSU06806", JobStatus.INPUT, "Job is queued for execution", null);
		Job tsu07248 = createJob("TSU07248", JobStatus.OUTPUT, "Job is on the hard copy queue", "ABEND S622");
		Job tsu07316 = createJob("TSU07316", JobStatus.ACTIVE, "Job is actively executing", null);
		
		test_getJobs(JobStatus.ALL, Arrays.asList(tsu06342, tsu06806, tsu07248, tsu07316));
	}

	@Test
	public void getActiveJobsReturnCorrectly() throws Exception {
		Job tsu06342 = createJob("TSU06342", JobStatus.OUTPUT, "Job is on the hard copy queue", "ABEND S222");
		Job tsu07248 = createJob("TSU07248", JobStatus.OUTPUT, "Job is on the hard copy queue", "ABEND S622");

		test_getJobs(JobStatus.OUTPUT, Arrays.asList(tsu06342, tsu07248));
	}
	
	private void test_getJobs(JobStatus status, List<Job> jobList) throws Exception {
		String owner = "STEVENH";
		String prefix = "*";

		JobNameList stevenh = new JobNameList("STEVENH", jobList);
		List<JobNameList> expected = Arrays.asList(stevenh);

		Response response = mock(Response.class);
		when(response.getStatus()).thenReturn(Status.OK.getStatusCode());
		when(response.readEntity(String.class)).thenReturn(loadTestFile("zosmfJobsResponse.json"));
		
		String getJobRelativeUri = "restjobs/jobs";
		mockRequestResponse(getJobRelativeUri, HttpMethod.GET, response, "prefix", prefix, "owner", owner);

		assertEquals(expected, zosmfService.getJobs(prefix, owner, status));
	}
	
	private static Job createJob(String id, JobStatus status, String phase, String returnCode) {
		return Job.builder().jobId(id) // $NON-NLS-1$
			.jobName("STEVENH") // $NON-NLS-1$
			.owner("STEVENH") //$NON-NLS-1$
			.type("TSU") //$NON-NLS-1$
			.status(status) //$NON-NLS-1$
			.subsystem("JES2") //$NON-NLS-1$
			.executionClass("TSU") //$NON-NLS-1$
			.phaseName(phase) //$NON-NLS-1$
			.returnCode(returnCode) //$NON-NLS-1$
			.build();
	}
}
