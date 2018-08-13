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

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class JobFile {

	private String ddname;
	private String recfm;
	private long lrecl;
	private long byteCount;
	private long recordCount;
	private long id;
	
	public JobFile() {}

	public String getDdname() {
		return ddname;
	}

	public void setDdname(String ddname) {
		this.ddname = ddname;
	}

	public String getRecfm() {
		return recfm;
	}

	public void setRecfm(String recfm) {
		this.recfm = recfm;
	}

	public long getLrecl() {
		return lrecl;
	}

	public void setLrecl(long lrecl) {
		this.lrecl = lrecl;
	}

	public long getByteCount() {
		return byteCount;
	}

	public void setByteCount(long byteCount) {
		this.byteCount = byteCount;
	}

	public long getRecordCount() {
		return recordCount;
	}

	public void setRecordCount(long recordCount) {
		this.recordCount = recordCount;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}
