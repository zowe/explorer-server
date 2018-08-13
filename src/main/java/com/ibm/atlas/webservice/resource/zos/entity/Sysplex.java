/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas.webservice.resource.zos.entity;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Sysplex {

	private String sysplex;
	private String system;
	
	public Sysplex(String sysplex, String system) {
		this.sysplex = sysplex;
		this.system = system;
	}

	public String getSysplex() {
		return sysplex;
	}

	public void setSysplex(String sysplex) {
		this.sysplex = sysplex;
	}

	public String getSystem() {
		return system;
	}

	public void setSystem(String system) {
		this.system = system;
	}	
}
