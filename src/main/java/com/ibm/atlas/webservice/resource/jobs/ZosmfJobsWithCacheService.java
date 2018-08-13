/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas.webservice.resource.jobs;

import javax.inject.Inject;

import com.ibm.atlas.model.jobs.Job;
import com.ibm.atlas.webservice.exceptions.JobNotFoundException;
import com.ibm.atlas.webservice.services.JobAttributeCache;
import com.ibm.atlas.webservice.services.ZosmfJobsService;

public class ZosmfJobsWithCacheService  {
	
	@Inject
	private JobAttributeCache cache;

	@Inject
	ZosmfJobsService zosmfService;
	
	public Job getJob(String jobName, String jobId, boolean concise) throws JobNotFoundException {
		Job job = zosmfService.getJob(jobName, jobId);
		if (concise) {
			return job;
		}
		return cache.getAdditionalAttributes(job);
	}
}
