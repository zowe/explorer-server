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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

import com.ibm.atlas.webservice.resource.jobs.entity.DD;
import com.ibm.atlas.webservice.resource.jobs.entity.Step;
import com.ibm.atlas.webservice.utilities.JobUtilities;
import com.ibm.json.java.JSON;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONArtifact;
import com.ibm.json.java.JSONObject;

public class JobUtilitiesTest {

	@Test
	public void testFindJobFileID() {	
		try (FileInputStream fis = new FileInputStream(new File("src/test/resources/com/ibm/atlas/utilities/jobFiles.json"))){
			JSONArtifact jobFiles = JSON.parse(fis);
			String id = JobUtilities.findJobFileID((JSONArray) jobFiles, "ddname", "JESYSMSG");
			assertEquals("4", id);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testFindJobFileIDInvalidSearchField() {				
		try (FileInputStream fis = new FileInputStream(new File("src/test/resources/com/ibm/atlas/utilities/jobFiles.json"))){
			JSONArtifact jobFiles = JSON.parse(fis);
			String id = JobUtilities.findJobFileID((JSONArray) jobFiles, "fakefile", "JESYSMSG");
			assertNull(id);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testFindJobFileIDInvalidSearchValue() {				
		try (FileInputStream fis = new FileInputStream(new File("src/test/resources/com/ibm/atlas/utilities/jobFiles.json"))){
			JSONArtifact jobFiles = JSON.parse(fis);
			String id = JobUtilities.findJobFileID((JSONArray) jobFiles, "ddname", "fakevalue");
			assertNull(id);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testFindJobFileIDNoJobs() {				
		try {
			JSONArtifact jobFiles = JSON.parse("[]");
			String id = JobUtilities.findJobFileID((JSONArray) jobFiles, "ddname", "fakevalue");
			assertNull(id);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testFindJobFileIDInvalidJSON() {				
		try {
			String id = JobUtilities.findJobFileID(null, "ddname", "fakevalue");
			assertNull(id);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testFindJobStepsSingleStep() {				
		try {
			String JCL = readFile("src/test/resources/com/ibm/atlas/utilities/JCLrecordsSingleStep.txt", Charset.forName("UTF8"));
			List<Step> steps = JobUtilities.findJobSteps(JCL);
			assertEquals(1, steps.size());
			assertEquals("UNIT", steps.get(0).getName());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testFindJobStepsMultipleSteps() {
		try {
			String JCL = readFile("src/test/resources/com/ibm/atlas/utilities/JCLrecordsMultipleSteps.txt", Charset.forName("UTF8"));
			List<Step> steps = JobUtilities.findJobSteps(JCL);
			assertEquals(5, steps.size());
			assertEquals("TSTP0005", steps.get(4).getProgram());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testFindJobStepsNoSteps() {
		try {
			String JCL = readFile("src/test/resources/com/ibm/atlas/utilities/JCLrecordsNoSteps.txt", Charset.forName("UTF8"));
			List<Step> steps = JobUtilities.findJobSteps(JCL);
			assertEquals(0, steps.size());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testFindJobDDsSingleDDs() {
		try (FileInputStream fis = new FileInputStream(new File("src/test/resources/com/ibm/atlas/utilities/singleDDs.json"))){
			JSONArtifact DDsJSON = JSON.parse(fis);
			JSONObject DDsExpected = new JSONObject();
			DDsExpected.put("dds", DDsJSON);
			
			String JCL = readFile("src/test/resources/com/ibm/atlas/utilities/JCLrecordsSingleDDs.txt", Charset.forName("UTF8"));
			List<DD> DDsActual = JobUtilities.findJobDDs(JCL, 1);
			
			assertEquals(1, DDsActual.size());
			DD outputDD = DDsActual.get(0);
			assertEquals("STEPLIB", outputDD.getName());
			assertEquals("DSN=ATLAS.TEST.LOAD,DISP=SHR", outputDD.getContent());
			assertEquals(1, outputDD.getDd());
			List<String> outputDatasets = outputDD.getDatasets();
			assertEquals(1, outputDatasets.size());
			assertEquals("ATLAS.TEST.LOAD", outputDatasets.get(0));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testFindJobDDsMultipleDDs() {
		try (FileInputStream fis = new FileInputStream(new File("src/test/resources/com/ibm/atlas/utilities/multipleDDs.json"))){
			JSONArtifact DDsJSON = JSON.parse(fis);
			JSONObject DDsExpected = new JSONObject();
			DDsExpected.put("dds", DDsJSON);
			
			String JCL = readFile("src/test/resources/com/ibm/atlas/utilities/JCLrecordsSingleStep.txt", Charset.forName("UTF8"));
			List<DD> DDsActual = JobUtilities.findJobDDs(JCL, 1);
			
			assertEquals(4, DDsActual.size());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void tesFindJobDDsNoDDs(){
		try (FileInputStream fis = new FileInputStream(new File("src/test/resources/com/ibm/atlas/utilities/noDDs.json"))){
			JSONArray DDsExpected = new JSONArray();
			
			String JCL = readFile("src/test/resources/com/ibm/atlas/utilities/JCLrecordsNoDDs.txt", Charset.forName("UTF8"));
			List<DD> DDsActual = JobUtilities.findJobDDs(JCL, 1);
			
			assertEquals(DDsExpected.toString(),DDsActual.toString());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testFindJobSubsystemCICS() {
		try {
			String JCL = readFile("src/test/resources/com/ibm/atlas/utilities/JCLrecordsCICSSubsystem.txt", Charset.forName("UTF8"));
			String subsystem = JobUtilities.findJobSubsystem("JOB14215", JCL);
			assertEquals(subsystem, "CICS");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testFindJobSubsystemDB2() {
		try {
			String JCL = readFile("src/test/resources/com/ibm/atlas/utilities/JCLrecordsDB2Subsystem.txt", Charset.forName("UTF8"));
			String subsystem = JobUtilities.findJobSubsystem("JOB14215", JCL);
			assertEquals(subsystem, "DB2");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testFindJobSubsystemIMS() {
		try {
			String JCL = readFile("src/test/resources/com/ibm/atlas/utilities/JCLrecordsIMSSubsystem.txt", Charset.forName("UTF8"));
			String subsystem = JobUtilities.findJobSubsystem("JOB14215", JCL);
			assertEquals(subsystem, "IMS");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testFindJobSubsystemMQ() {
		try {
			String JCL = readFile("src/test/resources/com/ibm/atlas/utilities/JCLrecordsMQSubsystem.txt", Charset.forName("UTF8"));
			String subsystem = JobUtilities.findJobSubsystem("JOB14215", JCL);
			assertEquals(subsystem, "MQ");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testFindJobSubsystemTSO() {
		try {
			String JCL = readFile("src/test/resources/com/ibm/atlas/utilities/JCLrecordsMQSubsystem.txt", Charset.forName("UTF8"));
			String subsystem = JobUtilities.findJobSubsystem("TSUJOB14215", JCL);
			assertEquals(subsystem, "TSO");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testFindJobSubsystemUnknown() {
		try {
			String JCL = readFile("src/test/resources/com/ibm/atlas/utilities/JCLrecordsSingleStep.txt", Charset.forName("UTF8"));
			String subsystem = JobUtilities.findJobSubsystem("JOB14215", JCL);
			assertNull(subsystem);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	static String readFile(String path, Charset encoding) throws IOException {
	  byte[] encoded = Files.readAllBytes(Paths.get(path));
	  return new String(encoded, encoding);
	}
}
