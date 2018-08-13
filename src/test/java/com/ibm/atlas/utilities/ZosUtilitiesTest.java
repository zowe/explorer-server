/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas.utilities;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.ibm.atlas.webservice.resource.zos.entity.Sysplex;
import com.ibm.atlas.webservice.utilities.JZOSUtilities;

public class ZosUtilitiesTest {
	
	@Before
	public void setup(){
		//Skip the tests if we aren't running on zos
		Assume.assumeTrue(System.getProperty("os.name").toLowerCase().contains("z/os")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testGetParmlibDetails() {
		List<String> actualParmlibDetails = JZOSUtilities.getParmlibDetails();
		assertFalse(actualParmlibDetails.isEmpty());
	}
	
	@Test
	public void testGetSysplexDetails() throws NullPointerException{		
		Sysplex actualSysplexDetails = JZOSUtilities.getSysplexDetails();
		
		assertTrue(actualSysplexDetails.getSysplex() != null);
		assertTrue(actualSysplexDetails.getSystem() != null);
	}
}