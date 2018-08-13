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

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ibm.atlas.model.jobs.Job;
import com.ibm.atlas.model.jobs.JobStatus;
import com.ibm.atlas.webservice.exceptions.JobNotFoundException;
import com.ibm.atlas.webservice.resource.jobs.ZosmfJobsWithCacheService;
import com.ibm.atlas.webservice.resource.jobs.entity.JobNameList;
import com.ibm.atlas.webservice.services.ZosmfJobsService;

@RunWith(PowerMockRunner.class)
public class JobsTest {

	private static final String JOB_NAME = "TESTJOB";
	private static final String JOB_ID_A = "A0001";
	
	Job JOB_A = Job.builder()
			.jobId(JOB_ID_A)
			.jobName(JOB_NAME)
			.owner("TESTUSER")
			.type("TYPE")
			.status(JobStatus.ALL)
			.returnCode("RETCODE")
			.subsystem("SUBSYSTEM")
			.executionClass("CLASS")
			.phaseName("PHASENAME")
			.build();

	Job JOB_B = Job.builder()
			.jobId("B0001")
			.jobName(JOB_NAME)
			.owner("TESTUSER")
			.type("TYPE")
			.status(JobStatus.ALL)
			.returnCode("RETCODE")
			.subsystem("SUBSYSTEM")
			.executionClass("CLASS")
			.phaseName("PHASENAME")
			.build();

	Jobs jobs;
	SecurityContext securityContext;

	@Before
	public void init() {
		this.jobs = new Jobs();
		this.jobs.zosmfService = Mockito.mock(ZosmfJobsService.class);
		this.jobs.zosmfWithCacheService = Mockito.mock(ZosmfJobsWithCacheService.class);
		this.jobs.log = Mockito.mock(Logger.class);
		this.securityContext = Mockito.mock(SecurityContext.class);
	}

	private void mockZosUtilitiesGetOwnerFilterValue() {
		Principal principal = Mockito.mock(Principal.class);
		Mockito.when(securityContext.getUserPrincipal()).thenReturn(principal);
		Mockito.when(principal.getName()).thenReturn("TESTUSER");
	}

	private void mockJobSearchFindJobs() {
		List<JobNameList> mockList = new LinkedList<>();
		List<Job> jobInstances = Arrays.asList(JOB_A, JOB_B);
		mockList.add(new JobNameList(JOB_NAME, jobInstances));
		Mockito.when(jobs.zosmfService.getJobs(JOB_NAME, "TESTUSER", JobStatus.ALL)).thenReturn(mockList);
	}

	@Test
	public void testGetJobs() {
		mockZosUtilitiesGetOwnerFilterValue();
		mockJobSearchFindJobs();
		List<JobNameList> jobList = jobs.getJobs(securityContext, JOB_NAME, null, null);
		assertEquals(1, jobList.size());
	}
	
	@Test
	public void testGetJobsWildcardPrefix() {
		mockZosUtilitiesGetOwnerFilterValue();
		mockJobSearchFindJobs();
		assertEquals(Collections.emptyList(), jobs.getJobs(securityContext, "*", null, null));
	}

	@Test
	public void testGetJobsError() {
		mockZosUtilitiesGetOwnerFilterValue();
		mockJobSearchFindJobs();
		List<JobNameList> jobList = jobs.getJobs(securityContext, "BAD_JOB", null, null);
		assertEquals(0, jobList.size());
	}

	@Test
	public void testGetJobIdsByName() {
		mockZosUtilitiesGetOwnerFilterValue();
		mockJobSearchFindJobs();
		List<JobNameList> jobList = jobs.getJobIdsByName(securityContext, JOB_NAME, null, null);
		assertEquals(1, jobList.size());
	}

	@Test
	public void testGetJobIdsByNameError() {
		mockZosUtilitiesGetOwnerFilterValue();
		mockJobSearchFindJobs();
		assertEquals(Collections.emptyList(), jobs.getJobIdsByName(securityContext, "BAD_JOB", null, null));
	}

	@Test
	public void testGetJobByIdAndName() throws Exception {
		Job jobWith = mock(Job.class);
		Mockito.when(jobs.zosmfWithCacheService.getJob(JOB_NAME, JOB_ID_A, true)).thenReturn(jobWith);
		assertEquals(jobWith, jobs.getJobByNameAndId(JOB_NAME, JOB_ID_A));
	}

	@Test(expected=WebApplicationException.class)
	public void testGetJobByIdAndNameForInvalidId() throws Exception {
		Mockito.when(jobs.zosmfWithCacheService.getJob(JOB_NAME, "BADID", true)).thenThrow(new JobNotFoundException(JOB_NAME, JOB_ID_A, Status.FORBIDDEN.getStatusCode()));
		jobs.getJobByNameAndId(JOB_NAME, "BADID");
	}
}
