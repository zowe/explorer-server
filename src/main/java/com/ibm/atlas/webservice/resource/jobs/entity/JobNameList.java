/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas.webservice.resource.jobs.entity;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.ibm.atlas.model.jobs.Job;

@XmlRootElement
public class JobNameList {

	private String name;
	private List<Job> jobInstances;
	
	public JobNameList(String name) {
		this.setName(name);
		this.setJobInstances(new LinkedList<Job>());
	}
	
	public JobNameList(String name, List<Job> jobInstances) {
		this.setName(name);
		this.setJobInstances(jobInstances);
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public List<Job> getJobInstances() {
		return jobInstances;
	}

	public void setJobInstances(List<Job> jobInstances) {
		this.jobInstances = jobInstances;
	}

	public void addJobInstance(Job jobInstance) {
		if (getJobInstances() == null) {
			setJobInstances(new LinkedList<Job>());
		}
		
		this.jobInstances.add(jobInstance);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jobInstances == null) ? 0 : jobInstances.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JobNameList other = (JobNameList) obj;
		if (jobInstances == null) {
			if (other.jobInstances != null)
				return false;
		} else if (!jobInstances.equals(other.jobInstances))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	

}
