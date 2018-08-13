/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas.webservice.resource.datasets.entity;

import javax.xml.bind.annotation.XmlRootElement;

import com.ibm.json.java.JSONObject;

@XmlRootElement
public class DatasetAttributes {
	
	public final static String BLOCKS = "BLOCKS"; //$NON-NLS-1$
	public final static String TRACKS = "TRACKS"; //$NON-NLS-1$
	public final static String CYLINDERS = "CYLINDERS"; //$NON-NLS-1$	
	public final static String ALLOCATE_BLOCKS = "BLK"; //$NON-NLS-1$
	public final static String ALLOCATE_TRACKS = "TRK"; //$NON-NLS-1$
	public final static String ALLOCATE_CYLINDERS = "CYL"; //$NON-NLS-1$	

	private String name;	// Dataset name
	private String blksize;	// Blocksize
	private String lrecl;	// Logical record length
	private String recfm;	// Record format
	private String dsorg;	// Dataset organisation
	private String catnm;	// Catalog name
	private String cdate;	// Creation date
	private String dev;		// device e.g. 3390
	private String edate;	// expiration date
	private String extx;				
	private String migr;
	private String ovf;
	private String rdate;	
	private String sizex;	// allocate size in tracks
	private String spacu;	// current allocated space units
	private String alcunit;	// allocation units in use	
	private String primary;	// primary space allocation	
	private String secondary;	// secondary space allocation		
	private String used;	// percentage of allocation used
	private String vol;		// volume
	
	public DatasetAttributes() {}

	public DatasetAttributes(JSONObject details) {
		setName((String) details.get("dsname")); //$NON-NLS-1$
		setBlksize((String) details.get("blksz")); //$NON-NLS-1$
		setLrecl((String) details.get("lrecl")); //$NON-NLS-1$
		setRecfm((String) details.get("recfm")); //$NON-NLS-1$
		setDsorg((String) details.get("dsorg")); //$NON-NLS-1$
		setCatnm((String) details.get("catnm")); //$NON-NLS-1$
		setCdate((String) details.get("cdate")); //$NON-NLS-1$
		setDev((String) details.get("dev")); //$NON-NLS-1$				
		setEdate((String) details.get("edate")); //$NON-NLS-1$
		setExtx((String) details.get("extx")); //$NON-NLS-1$				
		setMigr((String) details.get("migr")); //$NON-NLS-1$
		setOvf((String) details.get("ovf")); //$NON-NLS-1$
		setRdate((String) details.get("rdate")); //$NON-NLS-1$
		setSizex((String) details.get("sizex")); //$NON-NLS-1$
		setSpacu((String) details.get("spacu")); //$NON-NLS-1$	
		setUsed((String) details.get("used")); //$NON-NLS-1$
		setVol((String) details.get("vol")); //$NON-NLS-1$	
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBlksize() {
		return blksize;
	}

	public void setBlksize(String blksize) {
		this.blksize = blksize;
	}

	public String getLrecl() {
		return lrecl;
	}

	public void setLrecl(String lrecl) {
		this.lrecl = lrecl;
	}

	public String getRecfm() {
		return recfm;
	}

	public void setRecfm(String recfm) {
		this.recfm = recfm;
	}

	public String getDsorg() {
		return dsorg;
	}

	public void setDsorg(String dsorg) {
		this.dsorg = dsorg;
	}

	public void setCatnm(String catnm) {
		this.catnm = catnm;		
	}

	public String getCdate() {
		return cdate;
	}

	public void setCdate(String cdate) {
		this.cdate = cdate;
	}

	public String getDev() {
		return dev;
	}

	public void setDev(String dev) {
		this.dev = dev;
	}

	public String getEdate() {
		return edate;
	}

	public void setEdate(String edate) {
		this.edate = edate;
	}

	public String getExtx() {
		return extx;
	}

	public void setExtx(String extx) {
		this.extx = extx;
	}

	public String getMigr() {
		return migr;
	}

	public void setMigr(String migr) {
		this.migr = migr;
	}

	public String getOvf() {
		return ovf;
	}

	public void setOvf(String ovf) {
		this.ovf = ovf;
	}

	public String getRdate() {
		return rdate;
	}

	public void setRdate(String rdate) {
		this.rdate = rdate;
	}

	public String getSizex() {
		return sizex;
	}

	public void setSizex(String sizex) {
		this.sizex = sizex;
	}

	public String getSpacu() {
		return spacu;
	}

	public void setSpacu(String spacu) {
		this.spacu = spacu;
	}

	public String getUsed() {
		return used;
	}

	public void setUsed(String used) {
		this.used = used;
	}

	public String getVol() {
		return vol;
	}

	public void setVol(String vol) {
		this.vol = vol;
	}

	public String getCatnm() {
		return catnm;
	}

	public String getAlcunit() {
		return alcunit;
	}

	public void setAlcunit(String alcunit) {
		this.alcunit = alcunit;
	}

	public String getPrimary() {
		return primary;
	}

	public void setPrimary(String primary) {
		this.primary = primary;
	}

	public String getSecondary() {
		return secondary;
	}

	public void setSecondary(String secondary) {
		this.secondary = secondary;
	}

	public String createCopyForAllocate() {
		DatasetAttributes attribute = new DatasetAttributes();
		attribute.setDsorg(getDsorg());
		attribute.setRecfm(getRecfm());		
		attribute.setBlksize(getBlksize());
		if (getSpacu().equals(BLOCKS)) {
			attribute.setAlcunit(ALLOCATE_BLOCKS);
			int size = new Integer(getSizex()).intValue()*2;
			attribute.setPrimary(Integer.toString(size));
		} else if (getSpacu().equals(CYLINDERS)) {
			attribute.setAlcunit(ALLOCATE_CYLINDERS);
			int size = (new Integer(getSizex()).intValue()+8)/15;
			attribute.setPrimary(Integer.toString(size));
		} else {
			attribute.setAlcunit(ALLOCATE_TRACKS);
			attribute.setPrimary(getSizex());
		}
		attribute.setSecondary(attribute.getPrimary());		
		attribute.setLrecl(getLrecl());	
		StringBuffer buffy = new StringBuffer('{');
		if (this.dsorg!=null) {
			buffy.append(" \"dsorg\": "+dsorg);
			buffy.append(',');
		}
		if (this.recfm!=null) {
			buffy.append(" \"recfm\": "+recfm);
			buffy.append(',');
		}
		if (this.lrecl!=null) {
			buffy.append(" \"lrecl\": "+lrecl);
			buffy.append(',');
		}	
		if (this.blksize!=null) {
			buffy.append(" \"blksize\": "+blksize);
			buffy.append(',');
		}	
		if (this.alcunit!=null) {
			buffy.append(" \"alcunit\": "+alcunit);
			buffy.append(',');
		}	
		if (this.primary!=null) {
			buffy.append(" \"primary\": "+primary);
			buffy.append(',');
		}
		if (this.secondary!=null) {
			buffy.append(" \"secondary\": "+secondary);
			buffy.append(',');
		}			
		buffy.append('}');
		return buffy.toString();
		
	}

}
