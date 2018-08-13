/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas.webservice.resource.data.entity;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class NameValuePair {

	private String attribute;
	private String value;
	
	public NameValuePair() {}

	public NameValuePair(String attr, String val) {
		attribute = attr;
		value = val;
	}

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
}
