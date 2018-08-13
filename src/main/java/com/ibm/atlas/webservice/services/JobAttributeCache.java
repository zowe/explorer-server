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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import com.ibm.atlas.model.jobs.Job;

@Singleton
@RequestScoped
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class JobAttributeCache  {
	
	@Inject
	private ZosmfJobsService zosmfService;
	
	@Inject
	Logger log;
		
	@Inject
	Cache cachedJobInstances;
	
	public Job getAdditionalAttributes(Job jobInstance) {
		synchronized (jobInstance) {
			String jobName = jobInstance.getJobName();
			String jobId = jobInstance.getJobId();
			String instanceId = Cache.getInstanceKey(jobName, jobId);
			Job cachedInstance = cachedJobInstances.get(instanceId);	
			if (cachedInstance == null) {
				cachedInstance = jobInstance;
				cachedInstance.setSubsystem(getJobSubsystem(jobName, jobId));
				cachedJobInstances.put(instanceId, cachedInstance);
			} else if (cachedInstance.getSubsystem()!=null&&cachedInstance.getSubsystem().equals("")) { //$NON-NLS-1$
				log.info("empty string identified"); //$NON-NLS-1$
				// see if the previous get was a permission failure and if so retry. 
				// Permission failure is represented by a non null zero length string
				cachedInstance.setSubsystem(getJobSubsystem(jobName, jobId));
			}		
			jobInstance.setSubsystem(cachedInstance.getSubsystem());
		}
		return jobInstance;
	}
	
	public Job getSubsystemForFilter(Job jobInstance) {
		synchronized (jobInstance) {
			String jobName = jobInstance.getJobName();
			String jobId = jobInstance.getJobId();
			String instanceId = Cache.getInstanceKey(jobName, jobId);
			Job cachedInstance = cachedJobInstances.get(instanceId);	
			jobInstance.setSubsystem(cachedInstance != null?cachedInstance.getSubsystem():getJobSubsystem(jobName, jobId));
		}
		return jobInstance;
	}

	private String getJobSubsystem(String jobName, String jobId) {
		try {
			return zosmfService.getJobSubsystem(jobName, jobId);
		} catch (Exception e) {
			// CCS Catch the exception for now. SSL chaining causes this
			log.warning("Jobs "+e.getMessage()); //$NON-NLS-1$
		}
		return null;
	}
	public void backgroundUpdate(final List<Job> jobInstances) {
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		scheduler.schedule(new Runnable() {
			@Override
			public void run() {
				log.severe("cache cleanup starting at " + dateFormat.format(new Date())); //$NON-NLS-1$
				long tStart = System.currentTimeMillis();
				cachedJobInstances.tidyCache(jobInstances);
				long tDelta = System.currentTimeMillis() - tStart;
				log.severe("cache trim took " + tDelta + "milliseconds"); //$NON-NLS-1$
			}
		}, 5, TimeUnit.SECONDS);
	}
}
