/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas.webservice.resource.system.entity;

/**
 * NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE! 
 * 
 * Any public release of Atlas *must* include and increment of the CURRENT_ATLAS_VERSION 
 * 
 * NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE!NOTE! 
 *
 */
public class Version {
	
	private static final String CURRENT_ATLAS_VERSION = "V 0.0.3";

	private String version;

	public Version() {
		version = CURRENT_ATLAS_VERSION;
	}

	public String getVersion() {
		return version;
	}
	
}
