/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas.webservice.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;

import org.junit.Before;
import org.junit.Test;

import com.ibm.json.java.JSONObject;

public class ZosmfDatasetsServiceTest extends AbstractZosmfServiceTest<ZosmfService> {
	
	ZosmfDatasetsService zosmfService;

	@Before
	public void setUp() {
		this.zosmfService = new ZosmfDatasetsService();
		super.init(zosmfService);
	}
	
	@Test
	public void testcreateCopyForAllocateBasic() {
		try {
			JSONObject[] tests = new JSONObject[] {
				JSONObject.parse(
					"{\"dsname\":\"ATLAS.TEST.FAOPTS\",\"blksz\":\"27920\",\"catnm\":\"ICFCAT.SYSPLEX4.CATALOG3\",\"cdate\":\"2016/10/13\",\"dev\":\"3390\",\"dsntp\":\"PDS\",\"dsorg\":\"PO\",\"edate\":\"***None***\",\"extx\":\"1\"," +
					"\"lrecl\":\"80\",\"migr\":\"NO\",\"mvol\":\"N\",\"ovf\":\"NO\",\"rdate\":\"2017/07/26\",\"recfm\":\"FB\",\"primary\":\"151\",\"used\":\"1\",\"vol\":\"P4P001\"}"),
		
				JSONObject.parse(
					"{\"dsname\":\"PP.CAND.DKLSSHLF\",\"blksz\":\"8900\",\"catnm\":\"ICFCAT.SYSPLEX4.CATALOG3\",\"cdate\":\"2004/08/04\",\"dev\":\"3390\",\"dsntp\":\"PDS\",\"dsorg\":\"PO\",\"edate\":\"***None***\",\"extx\":\"1\","+
					"\"lrecl\":\"259\",\"migr\":\"NO\",\"mvol\":\"N\",\"ovf\":\"NO\",\"rdate\":\"2008/02/08\",\"recfm\":\"VB\",\"primary\":\"2\",\"spacu\":\"BLOCKS\",\"used\":\"100\",\"vol\":\"P4PP01\"}"),
				
				JSONObject.parse(
					"{\"dsname\":\"ATLASID.SPF.ISPPROF\",\"blksz\":\"3120\",\"catnm\":\"ICFCAT.SYSPLEX4.CATALOGA\",\"cdate\":\"2017/04/25\",\"dev\":\"3390\",\"dsntp\":\"PDS\",\"dsorg\":\"PO\",\"edate\":\"***None***\",\"extx\":\"1\"," +
					"\"lrecl\":\"80\",\"migr\":\"NO\",\"mvol\":\"N\",\"ovf\":\"NO\",\"rdate\":\"2017/07/14\",\"recfm\":\"FB\",\"primary\":\"2\",\"spacu\":\"TRACKS\",\"used\":\"50\",\"vol\":\"P4P036\"},"),

				JSONObject.parse(
					"{\"dsname\":\"PP.CAND.DLSCMDS\",\"blksz\":\"8880\",\"catnm\":\"ICFCAT.SYSPLEX4.CATALOG3\",\"cdate\":\"2004/08/04\",\"dev\":\"3390\",\"dsntp\":\"PDS\",\"dsorg\":\"PO\",\"edate\":\"***None***\",\"extx\":\"1\"," +
					 "\"lrecl\":\"80\",\"migr\":\"NO\",\"mvol\":\"N\",\"ovf\":\"NO\",\"rdate\":\"2008/02/08\",\"recfm\":\"FB\",\"primary\":\"4\",\"spacu\":\"BLOCKS\",\"used\":\"75\",\"vol\":\"P4PP01\"},"),	
				
				JSONObject.parse(
				  "{\"dsname\":\"SYS1.VTOCIX.P4SY01\",\"blksz\":\"2048\",\"cdate\":\"2017/06/27\",\"dev\":\"3390\",\"dsorg\":\"PS\",\"edate\":\"***None***\",\"extx\":\"1\"," +
				  "\"lrecl\":\"2048\",\"migr\":\"NO\",\"ovf\":\"NO\",\"rdate\":\"***None***\",\"recfm\":\"F\",\"primary\":\"179\",\"spacu\":\"TRACKS\",\"used\":\"100\",\"vol\":\"P4SY01\"}"),

				JSONObject.parse(
					"{\"dsname\":\"SYS1.IBM.PARMLIB\",\"blksz\":\"27920\",\"cdate\":\"2013/07/03\",\"dev\":\"3390\",\"dsntp\":\"PDS\",\"dsorg\":\"PO\",\"edate\":\"***None***\",\"extx\":\"1\"," + 
					"\"lrecl\":\"80\",\"migr\":\"NO\",\"ovf\":\"NO\",\"rdate\":\"2017/07/27\",\"recfm\":\"FB\",\"primary\":\"5\",\"alcunit\":\"CYL\",\"used\":\"30\",\"vol\":\"P4SY01\"},")
			};
			String[] results = new String[] {
				"{\"dsorg\":\"PO\",\"dirblk\":40,\"recfm\":\"FB\",\"lrecl\":80,\"blksize\":27920,\"alcunit\":\"TRK\",\"primary\":151,\"secondary\":151}",
				"{\"dsorg\":\"PO\",\"dirblk\":40,\"recfm\":\"VB\",\"lrecl\":259,\"blksize\":8900,\"alcunit\":\"TRK\",\"primary\":2,\"secondary\":2}",
				"{\"dsorg\":\"PO\",\"dirblk\":40,\"recfm\":\"FB\",\"lrecl\":80,\"blksize\":3120,\"alcunit\":\"TRK\",\"primary\":2,\"secondary\":2}",
				"{\"dsorg\":\"PO\",\"dirblk\":40,\"recfm\":\"FB\",\"lrecl\":80,\"blksize\":8880,\"alcunit\":\"TRK\",\"primary\":4,\"secondary\":4}",
				"{\"dsorg\":\"PS\",\"recfm\":\"F\",\"lrecl\":2048,\"blksize\":2048,\"alcunit\":\"TRK\",\"primary\":179,\"secondary\":179}",
				"{\"dsorg\":\"PO\",\"dirblk\":40,\"recfm\":\"FB\",\"lrecl\":80,\"blksize\":27920,\"alcunit\":\"CYL\",\"primary\":5,\"secondary\":5}",				
			};		
	
			for (int i=0;i<tests.length; i++) {
				assertEquals("testcreateCopyForAllocateBasic: Failure in test "+i,  results[i], zosmfService.createDatasetReformat(tests[i]));
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	@Test
	public void testcreateCopyForAllocateReject() {
		try {
			JSONObject[] tests = new JSONObject[] {
				JSONObject.parse(
					"{\"dsname\":\"PP.ATAM.SATHLOAD\",\"blksz\":\"32760\",\"catnm\":\"ICFCAT.SYSPLEX4.CATALOG3\",\"cdate\":\"2013/07/02\",\"dev\":\"3390\",\"dsntp\":\"LIBRARY\",\"dsorg\":\"PO-E\",\"edate\":\"***None***\",\"extx\":\"1\"," + //$NON-NLS-1$ 
						"\"lrecl\":\"0\",\"migr\":\"NO\",\"mvol\":\"N\",\"ovf\":\"NO\",\"rdate\":\"2017/07/12\",\"recfm\":\"U\",\"sizex\":\"41\",\"spacu\":\"TRACKS\",\"used\":\"81\",\"vol\":\"P4SY01\"},"), //$NON-NLS-1$
			};
			// It's a PO-E
			for (int i=0;i<tests.length; i++) {
				boolean passed = false;
				try {
					zosmfService.createDatasetReformat(tests[i]);
				} catch (WebApplicationException e) {
					passed = e.getResponse().getEntity().toString().contains("datasets with organization"); //$NON-NLS-1$
				}
				if (!passed) {
					fail("testcreateCopyForAllocateReject: Correct exception not thrown"); //$NON-NLS-1$
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	@Test
	// Uses createCopyForAllocate to perform test PS with a dirblk
	public void testCreateDatasetIncompatibleParameters() {
		try {
			String[] tests = new String[] {
			
						"{\"dsname\":\"ABODY.IBM.PARMLIB\",\"blksz\":\"27920\",\"cdate\":\"2013/07/03\",\"dev\":\"3390\",\"dsntp\":\"PDS\",\"dsorg\":\"PS\",\"dirblk\":40,\"edate\":\"***None***\",\"extx\":\"1\"," + 
								"\"lrecl\":\"80\",\"migr\":\"NO\",\"ovf\":\"NO\",\"rdate\":\"2017/07/27\",\"recfm\":\"FB\",\"sizex\":\"75\",\"spacu\":\"CYLINDERS\",\"used\":\"30\",\"vol\":\"P4SY01\"}"
			};
			for (int i=0;i<tests.length; i++) {
				boolean passed = false;
				try {
					zosmfService.createDatasetExtended("TEST.TEST", tests[i]); 
				} catch (WebApplicationException e) {
					System.out.println("testCreateDatasetIncompatibleParameters exception "+e.getMessage());
					passed = e.getResponse().getEntity().toString().contains("Incompatible data set attributes:"); //$NON-NLS-1$
				}
				if (!passed) {
					fail("testCreateDatasetIncompatibleParameters: Correct exception not thrown"); //$NON-NLS-1$
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	@Test
	// Uses createCopyForAllocate to perform test
	public void testCreateDatasetLreclBlksize() {
		try {
			String[] tests = new String[] {
						"{\"dsname\":\"ABODY.IBM.PARMLIB\",\"blksize\":27900,\"cdate\":\"2013/07/03\",\"dev\":\"3390\",\"dsntp\":\"PDS\",\"dsorg\":\"PO\",\"edate\":\"***None***\",\"extx\":\"1\"," + 
								"\"lrecl\":80,\"migr\":\"NO\",\"ovf\":\"NO\",\"rdate\":\"2017/07/27\",\"recfm\":\"FB\",\"sizex\":75,\"spacu\":\"CYLINDERS\",\"used\":\"30\",\"vol\":\"P4SY01\"}"
			};
			for (int i=0;i<tests.length; i++) {
				boolean passed = false;
				try {
					zosmfService.createDatasetExtended("TEST.TEST", tests[i]);
				} catch (WebApplicationException e) {
					System.out.println("testCreateDatasetLreclBlksize "+e.getResponse().getEntity().toString());
					passed = e.getResponse().getEntity().toString().contains("Limitation:"); //$NON-NLS-1$
				}
				
				if (!passed) {
					fail("testCreateDatasetLreclBlksize: Correct exception not thrown"); //$NON-NLS-1$
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	@Test
	public void testCreateCopyForAllocateOverwrite() {
		try {
			JSONObject[] tests = new JSONObject[] {
				JSONObject.parse(
					"{\"dsname\":\"ATLAS.TEST.FAOPTS\",\"blksz\":27920,\"catnm\":\"ICFCAT.SYSPLEX4.CATALOG3\",\"cdate\":\"2016/10/13\",\"dev\":\"3390\",\"dsntp\":\"PDS\",\"dsorg\":\"PO\",\"dirblk\":17,\"edate\":\"***None***\",\"extx\":\"1\"," +
					"\"lrecl\":80,\"secondary\":20,\"migr\":\"NO\",\"mvol\":\"N\",\"ovf\":\"NO\",\"rdate\":\"2017/07/26\",\"recfm\":\"FB\",\"primary\":\"151\",\"used\":\"1\",\"vol\":\"P4P001\"}"),
		
				JSONObject.parse(
					"{\"dsname\":\"PP.CAND.DKLSSHLF\",\"blksz\":8900,\"catnm\":\"ICFCAT.SYSPLEX4.CATALOG3\",\"cdate\":\"2004/08/04\",\"dev\":\"3390\",\"dsntp\":\"PDS\",\"dsorg\":\"PO\",\"dirblk\":17,\"edate\":\"***None***\",\"extx\":\"1\","+
					"\"lrecl\":259,\"secondary\":24,\"migr\":\"NO\",\"mvol\":\"N\",\"ovf\":\"NO\",\"rdate\":\"2008/02/08\",\"recfm\":\"VB\",\"primary\":\"2\",\"used\":\"100\",\"vol\":\"P4PP01\"}"),
				
				JSONObject.parse(
				  "{\"dsname\":\"SYS1.VTOCIX.P4SY01\",\"blksz\":2048,\"cdate\":\"2017/06/27\",\"dev\":3390,\"dsorg\":\"PS\",\"edate\":\"***None***\",\"extx\":\"1\"," +
				  "\"lrecl\":2048,\"migr\":\"NO\",\"ovf\":\"NO\",\"rdate\":\"***None***\",\"recfm\":\"F\",\"primary\":\"179\",\"spacu\":\"TRACKS\",\"used\":\"100\",\"vol\":\"P4SY01\"}"),
				
				JSONObject.parse(
						  "{\"dsname\":\"SYS1.VTOCIX.P4SY01\",\"blksz\":2048,\"cdate\":\"2017/06/27\",\"dev\":3390,\"dsorg\":\"PS\",\"edate\":\"***None***\",\"extx\":\"1\"," +
						  "\"lrecl\":2048,\"migr\":\"NO\",\"ovf\":\"NO\",\"rdate\":\"***None***\",\"recfm\":\"F\",\"primary\":\"179\",\"alcunit\":\"TRK\",\"used\":\"100\",\"vol\":\"P4SY01\"}"),		
			};
			String[] results = new String[] {
				"{\"dsorg\":\"PO\",\"dirblk\":17,\"recfm\":\"FB\",\"lrecl\":80,\"blksize\":27920,\"alcunit\":\"TRK\",\"primary\":151,\"secondary\":20}",
				"{\"dsorg\":\"PO\",\"dirblk\":17,\"recfm\":\"VB\",\"lrecl\":259,\"blksize\":8900,\"alcunit\":\"TRK\",\"primary\":2,\"secondary\":24}",
				"{\"dsorg\":\"PS\",\"recfm\":\"F\",\"lrecl\":2048,\"blksize\":2048,\"alcunit\":\"TRK\",\"primary\":179,\"secondary\":179}",
				"{\"dsorg\":\"PS\",\"recfm\":\"F\",\"lrecl\":2048,\"blksize\":2048,\"alcunit\":\"TRK\",\"primary\":179,\"secondary\":179}",				
			};		
	
			for (int i=0;i<tests.length; i++) {
				assertEquals("testCreateCopyForAllocateOverwrite: Failure in test "+i,  results[i], zosmfService.createDatasetReformat(tests[i]));
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	@Test
	public void testCreateCopyForMergeDuffReference() {
		boolean passed = false;
		JSONObject dsnAttributes = null;
				try {
			dsnAttributes = JSONObject.parse("{\"basedsn\":\"ATOOL.TEST.REPORT\",\"records\":\"This is my  report\"}");
			zosmfService.createDatasetMergeAttributes(dsnAttributes, new JSONObject());
		} catch (IOException e1) {
			fail(e1.getMessage());
		} catch (WebApplicationException wea) {
			passed = wea.getResponse().getEntity().toString().contains("Referenced dataset"); //$NON-NLS-1$
		}
		assertTrue("testCreateCopyForMergeDuffReference. Incorrect or no exception thrown 1", passed);
		passed = false;
		try {
			dsnAttributes = JSONObject.parse("{\"basedsn\":\"ATOOL.TEST.REPORT\",\"records\":\"This is my  report\"}");
			zosmfService.createDatasetMergeAttributes(dsnAttributes, null);
		} catch (IOException e1) {
			fail(e1.getMessage());
		} catch (WebApplicationException wea) {
			passed = wea.getResponse().getEntity().toString().contains("Referenced dataset"); //$NON-NLS-1$
		}
		assertTrue("testCreateCopyForMergeDuffReference. Incorrect or no exception thrown 2", passed);
		passed = false;
		try {
			dsnAttributes = JSONObject.parse("{\"basedsn\":\"ATOOL.TEST.REPORT\",\"records\":\"This is my  report\"}");
			zosmfService.createDatasetMergeAttributes(dsnAttributes, JSONObject.parse("{\"returnedRows\":1,\"JSONversion\":1,\"items\":[{\"dsorg\":\"PO\",\"dirblk\":17,\"recfm\":\"FB\",\"lrecl\":\"80\",\"blksize\":\"2000\",\"primary\":\"76\",\"secondary\":\"20\"}]}"));
			zosmfService.createDatasetPreChecks("ATOOL.NEW.JCL", dsnAttributes);
		} catch (IOException e1) {
			fail(e1.getMessage());
		} catch (WebApplicationException wea) {
			passed = wea.getResponse().getEntity().toString().contains("Specifying content"); //$NON-NLS-1$
		}	
		assertTrue("testCreateCopyForMergeDuffReference. Incorrect or no exception thrown 3", passed);

		try {
			dsnAttributes = JSONObject.parse("{\"basedsn\":\"ATOOL.TEST.REPORT\",\"records\":\"This is my  report\"}");
			zosmfService.createDatasetMergeAttributes(dsnAttributes, JSONObject.parse("{\"returnedRows\":1,\"JSONversion\":1,\"items\":[{\"dsorg\":\"PO\",\"dirblk\":17,\"recfm\":\"FB\",\"lrecl\":\"80\",\"blksize\":\"2000\",\"primary\":\"76\",\"secondary\":\"20\"}]}"));
			zosmfService.createDatasetPreChecks("ATOOL.NEW.JCL(LOBBER)", dsnAttributes);
		} catch (IOException e1) {
			fail(e1.getMessage());
		} catch (WebApplicationException wea) {
			fail(wea.getMessage());
		}		

		try { // this one works
			dsnAttributes = JSONObject.parse("{\"basedsn\":\"ATOOL.TEST.REPORT\",\"records\":\"This is my  report\"}");
			zosmfService.createDatasetMergeAttributes(dsnAttributes, JSONObject.parse("{\"returnedRows\":1,\"JSONversion\":1,\"items\":[{\"dsorg\":\"PS\",\"recfm\":\"FB\",\"lrecl\":\"80\",\"blksize\":\"2000\",\"primary\":\"76\",\"secondary\":\"20\"}]}"));
			zosmfService.createDatasetPreChecks("", dsnAttributes);
			assertFalse("testCreateCopyForAllocateDuffReference. basedsn still present", dsnAttributes.containsKey("basedsn"));
		} catch (IOException e1) {
			fail(e1.getMessage());
		} catch (WebApplicationException wea) {
			fail(wea.getMessage());
		}	
	}
	@Test
	public void testCreateCopyForMerge() {
		JSONObject dsnAttributes = null;
		try { // this one works
			dsnAttributes = JSONObject.parse("{\"basedsn\":\"ATOOL.TEST.REPORT\",\"records\":\"This is my  report\"}");
			zosmfService.createDatasetMergeAttributes(dsnAttributes, JSONObject.parse("{\"returnedRows\":1,\"JSONversion\":1,\"items\":[{\"dsorg\":\"PS\",\"recfm\":\"FB\",\"lrecl\":\"80\",\"blksize\":\"2000\",\"sizex\":\"75\",\"spacu\":\"CYLINDERS\",}]}"));
			assertTrue("testCreateCopyForMergeDuffReference. Units not copied across", dsnAttributes.containsKey("alcunit")&&dsnAttributes.get("alcunit").equals("CYL"));
			assertTrue("testCreateCopyForMergeDuffReference. Size not copied across", dsnAttributes.containsKey("primary")&&dsnAttributes.get("primary").equals("5"));
			zosmfService.createDatasetPreChecks("", dsnAttributes);
			assertFalse("testCreateCopyForMergeDuffReference. Basedsn reference still present", dsnAttributes.containsKey("basedsn"));
		} catch (IOException e1) {
			fail(e1.getMessage());
		} catch (WebApplicationException wea) {
			fail(wea.getMessage());
		}
	}	

}
