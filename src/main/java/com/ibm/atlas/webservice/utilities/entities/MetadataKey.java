/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas.webservice.utilities.entities;

import java.io.Serializable;

/**
 * MetadataKey is required to provide composite key to the Metadata persistent
 * object class
 *
 */
public class MetadataKey implements Serializable {

	private String owner;
	private String resource;
	private String attribute;

	public MetadataKey(String owner, String resource, String attribute) {
		super();
		this.owner = owner;
		this.resource = resource;
		this.attribute = attribute;
	}

	public MetadataKey() {
		super();
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof MetadataKey) {
			MetadataKey key = (MetadataKey) object;
			if (!key.getOwner().equals(owner)) {
				return false;
			}

			if (!key.getResource().equals(resource)) {
				return false;
			}

			if (!key.getAttribute().equals(attribute)) {
				return false;
			}

			return true;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return owner.hashCode() + resource.hashCode() + attribute.hashCode();
	}

	public String getOwner() {
		return owner;
	}

	public String getResource() {
		return resource;
	}

	public String getAttribute() {
		return attribute;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}
}