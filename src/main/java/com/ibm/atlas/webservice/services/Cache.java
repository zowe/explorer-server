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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ibm.atlas.model.jobs.Job;

@Singleton
@ApplicationScoped
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class Cache {

	@Inject
	Logger log;
	
	private Map<String, Job> cachedJobInstances;
	public Cache() {
		cachedJobInstances = new ConcurrentHashMap<>();
	}
	public Job get(String instanceId) {
		return cachedJobInstances.get(instanceId);
	}
	public void put(String instanceId, Job cachedInstance) {
		cachedJobInstances.put(instanceId, cachedInstance);		
	}
	protected static String getInstanceKey(String jobName, String jobId) {
		return String.format("%s : %s", jobName, jobId); //$NON-NLS-1$
	}
	protected void tidyCache(List<Job> allJobs) {
		HashSet<String> allJobIds = new HashSet<>();
		for (Job instance : allJobs) {
			allJobIds.add(getInstanceKey(instance.getJobName(), instance.getJobId()));
		}
		log.info("Cache.. latest instances "+allJobIds.size()+ " cache size "+cachedJobInstances.size()); //$NON-NLS-1$ //$NON-NLS-2$
		for (String key : cachedJobInstances.keySet()) {
			if (!allJobIds.contains(key)) {
				log.info("Cache.. removing "+key); //$NON-NLS-1$
				cachedJobInstances.remove(key);
			}
		}
	}

}
