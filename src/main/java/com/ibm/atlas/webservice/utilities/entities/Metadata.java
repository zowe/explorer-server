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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name="METADATA")
@IdClass(value=MetadataKey.class)

@NamedQueries({
	@NamedQuery(name="getMetadataByOwnerAndResource", 
			    query="Select m from Metadata m where (m.owner=:own and m.resource=:res)"),
	@NamedQuery(name="getMetadataByOwnerResourceAndAttribute", 
	            query="Select m from Metadata m where (m.owner=:own and m.resource=:res and m.attribute=:atr)"),
})

/**
 * Metadata is a persistent object class used to store name/value pairs for named resources.
 */
public class Metadata implements Serializable {

	@Id
    private String owner;

	@Id
    private String resource;

	@Id
    private String attribute;
    
	@Column
    private String value;

    public Metadata(String owner, String resource, String attribute, String value) {
    	super();
    	this.owner = owner;
    	this.resource = resource;
    	this.attribute = attribute;
    	this.value = value;
    }

    public Metadata() {
    	super();
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
    
    public String getValue() {
        return value;
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
    
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Metadata[" + owner + "," + resource + "," + attribute + "," + value + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    }
}