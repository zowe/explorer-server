/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas.webservice.exceptions;

import javax.ws.rs.core.Response.Status;

import com.ibm.atlas.webservice.Messages;

public class JobNotFoundException extends AtlasException {

	private static final long serialVersionUID = 8607029992311773207L;
	
	private String jobName;
	private String jobId;
	private int status;

	public JobNotFoundException(String jobName, String jobId, int status) {
		super(Status.NOT_FOUND);
		this.jobName = jobName;
		this.jobId = jobId;
		this.status = status;
	}
	
	@Override
	public String getMessage() {
		return String.format(Messages.getString("ZOSMFService.GetJobByNameAndIdFailed"), jobName, jobId, status); //$NON-NLS-1$
	}
	
}
