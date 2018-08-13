/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas.webservice.utilities;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.atlas.webservice.Messages;
import com.ibm.atlas.webservice.resource.zos.entity.Sysplex;
import com.ibm.jzos.ZUtil;

public class JZOSUtilities {
	private static final Logger log = Logger.getLogger(JZOSUtilities.class.getName());

	private static final int ZOS_PSA_CVT_OFFSET = 16;
	private static final int ZOS_CVT_ECVT_OFFSET = 140;
	private static final int ZOS_ECVT_IPA_OFFSET = 392;
	private static final int ZOS_IPA_PLNUMX_OFFSET = 2134;
	private static final int ZOS_IPA_PLIB_OFFSET = 2136;
	private static final int ZOS_IPA_IPAPLI_SIZE = 64;
	
	/**
	 * Get PARMLIB concatenation
	 * @return The PARMLIB concatenation
	 */
	public static List<String> getParmlibDetails() {
		List<String> parmlibList = new LinkedList<>();

		long flccvt = ZOS_PSA_CVT_OFFSET;
		long cvt = ZUtil.peekOSMemory(flccvt, 4);
		long ecvt = ZUtil.peekOSMemory(cvt + ZOS_CVT_ECVT_OFFSET, 4);
		long ipa = ZUtil.peekOSMemory(ecvt + ZOS_ECVT_IPA_OFFSET, 4);

		byte[] ipaplnumx = new byte[2];
		ZUtil.peekOSMemory(ipa + ZOS_IPA_PLNUMX_OFFSET, ipaplnumx);
		int plnum = ((ipaplnumx[1] & 0xFF) | ((ipaplnumx[0] & 0xFF) << 8));

		long ipapli = (ipa + ZOS_IPA_PLIB_OFFSET);
		long ipaplib = ZUtil.peekOSMemory(ipapli, 4);
		
		for (int i = 0; i < plnum; i++) {
			byte[] ipaplidsn = new byte[44];
			ZUtil.peekOSMemory(ipaplib, ipaplidsn);
			String plidsn = null;
			try {
				plidsn = new String(ipaplidsn, "cp1047").trim(); //$NON-NLS-1$
				parmlibList.add(plidsn);
			} catch (UnsupportedEncodingException e) {
				String error = String.format(Messages.getString("JZOSUtilities.CodePageConversionException1"), e.getMessage()); //$NON-NLS-1$
				log.log(Level.SEVERE, error);
			}
			ipaplib += ZOS_IPA_IPAPLI_SIZE;
		}

		return parmlibList;
	}

	/**
	 * Get sysplex and system names
	 * @return sysplex and system names
	 */
	public static Sysplex getSysplexDetails() {
		long flccvt = 0x10;
		long cvt = ZUtil.peekOSMemory(flccvt, 4);
		long ecvt = ZUtil.peekOSMemory(cvt + 140, 4);
		long ipa = ZUtil.peekOSMemory(ecvt + 392, 4);

		byte[] cvtsname = new byte[8];
		ZUtil.peekOSMemory(cvt + 340, cvtsname);
		String sysname = null;

		byte[] ipasxnam = new byte[8];
		ZUtil.peekOSMemory(ipa + 352, ipasxnam);
		String sysplex = null;

		try {
			sysname = new String(cvtsname, "cp1047").trim(); //$NON-NLS-1$
			sysplex = new String(ipasxnam, "cp1047").trim(); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			String error = String.format(Messages.getString("JZOSUtilities.CodePageConversionException2"), e.getMessage()); //$NON-NLS-1$
			log.log(Level.SEVERE, error);
		}

		return new Sysplex(sysplex, sysname);
	}
}