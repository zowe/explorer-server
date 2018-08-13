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

import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.SecurityContext;

import com.ibm.atlas.webservice.resource.jobs.entity.DD;
import com.ibm.atlas.webservice.resource.jobs.entity.Step;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class JobUtilities {
	private static final Logger log = Logger.getLogger(JobUtilities.class.getName());

	private static final String JES_JCL_DSN_PATTERN = "^.*(\\/\\/|XX)([^*].+?) .+?DSN=([^, \n]+)"; //$NON-NLS-1$
	private static final String JES_JCL_DSNAME_PATTERN = "^.*(\\/\\/|XX)([^*].+?) .+?DSNAME=([^, \n]+)"; //$NON-NLS-1$
	private static final String JES_JCL_DD_PATTERN = "^.*(\\/\\/|XX)([^*].+?) DD (.+)"; //$NON-NLS-1$
	private static final String JES_JCL_STEP_PATTERN = "^.*(\\/\\/|XX)([^*\\s][^\\s]{0,7}) .+?PGM=([^\\s,]{1,8})"; //$NON-NLS-1$

	/**
	 * Find the ID of a particular file in a list of job output
	 * 
	 * @param jobFiles
	 *            The list of job files from z/OSMF
	 * @param fieldToMatch
	 *            The field name to search based on
	 * @param valueToMatch
	 *            The field value to search for
	 * @return The ID for the desired job file
	 */
	public static String findJobFileID(JSONArray jobFiles, String fieldToMatch, String valueToMatch) {
		String id = null;

		if (jobFiles != null && !jobFiles.isEmpty()) {
			for (Object fileObject : jobFiles) {
				JSONObject file = (JSONObject) fileObject;

				String ddname = (String) file.get(fieldToMatch);
				if (ddname != null && !ddname.isEmpty() && ddname.equals(valueToMatch)) {
					id = String.valueOf(file.get("id")); //$NON-NLS-1$
					break;
				}
			}
		}

		if (id == null) {
			return null;
		}

		return id;
	}

	public static List<Step> findJobSteps(String JCL) {
		List<Step> steps = new LinkedList<>();

		Pattern pattern = Pattern.compile(JES_JCL_STEP_PATTERN);
		Scanner scanner = new Scanner(JCL);
		int stepCount = 1;
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			Matcher matcher = pattern.matcher(line);
			if (matcher.find() && matcher.groupCount() == 3) {
				Step step = new Step(matcher.group(2), matcher.group(3), stepCount++);
				steps.add(step);
			}
		}
		scanner.close();

		return steps;
	}

	public static List<DD> findJobDDs(String JCL, int stepNo) {
		List<DD> dds = new LinkedList<>();
		
		Pattern stepPattern = Pattern.compile(JES_JCL_STEP_PATTERN);
		Pattern ddPattern = Pattern.compile(JES_JCL_DD_PATTERN);
		Pattern dsnPattern = Pattern.compile(JES_JCL_DSN_PATTERN);
		Pattern dsnamePattern = Pattern.compile(JES_JCL_DSNAME_PATTERN);

		int stepCount = 0;
		boolean foundStep = false;
		Scanner scanner = new Scanner(JCL);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			Matcher stepMatcher = stepPattern.matcher(line);
			if (stepMatcher.find() && stepMatcher.groupCount() == 3) { // Found step
				stepCount++;
				if (stepCount == stepNo) { // Is it the step we want?
					foundStep = true;
					boolean pendingDD = false;
					String name = null;
					int ddNumber = 0;
					String ddContent = null;
					List<String> dsns = new LinkedList<>();

					while (scanner.hasNextLine()) { // Process the step
						line = scanner.nextLine();
						stepMatcher = stepPattern.matcher(line);
						if (stepMatcher.find() && stepMatcher.groupCount() == 3) { // End of step?
							break;
						}

						Matcher ddMatcher = ddPattern.matcher(line);
						if (ddMatcher.find() && ddMatcher.groupCount() == 3 && !ddMatcher.group(2).trim().isEmpty()) {
														
							if (pendingDD) {
								DD dd = new DD();
								dd.setName(name);
								dd.setDd(ddNumber);
								dd.setContent(ddContent);
								dd.setDatasets(dsns);
								dds.add(dd);
								ddContent = ""; //$NON-NLS-1$
								dsns = new LinkedList<>();
							}

							ddNumber++;
							pendingDD = true;
							String ddCard = ddMatcher.group(3).trim(); 
							int idx = ddCard.indexOf(' ');
							if ( idx > 0 ) {
								ddContent = ddCard.substring(0, idx);
							} else {
								ddContent = ddCard;
							}
							name = ddMatcher.group(2).trim();

							Matcher dsnMatcher = dsnPattern.matcher(line);
							if (dsnMatcher.find() && dsnMatcher.groupCount() == 3) {
								dsns.add(dsnMatcher.group(3));
							} else {
								Matcher dsnameMatcher = dsnamePattern.matcher(line);
								if (dsnameMatcher.find() && dsnameMatcher.groupCount() == 3) {
									dsns.add(dsnameMatcher.group(3));
								}
							}
						} else {
							int idx = line.indexOf("//"); //$NON-NLS-1$
							if (idx == -1)
								idx = line.indexOf("XX"); //$NON-NLS-1$
							if (line.charAt(idx + 2) != '*') { // Ignore comment
																// lines in JCL
								if ( line.length() > 72 ) {
									line = line.substring(0, 72);
								}
								ddContent = ddContent + '\n' + line.substring(idx + 2).trim();
								Matcher dsnMatcher = dsnPattern.matcher(line);
								if (dsnMatcher.find() && dsnMatcher.groupCount() == 3) {
									dsns.add(dsnMatcher.group(3));
								} else {
									Matcher dsnameMatcher = dsnamePattern.matcher(line);
									if (dsnameMatcher.find() && dsnameMatcher.groupCount() == 3) {
										dsns.add(dsnameMatcher.group(3));
									}
								}
							}
						}
					}
					if (pendingDD) {
						DD dd = new DD();
						dd.setName(name);
						dd.setDd(ddNumber);
						dd.setContent(ddContent);
						dd.setDatasets(dsns);
						dds.add(dd);
					}
					break;
				}
			}
		}
		scanner.close();
		
		if (dds.size() == 0 && !foundStep) {
			return null;
		}

		return dds;
	}

	public static String findJobSubsystem(String jobId, String JCL) {
		String subsys = null;

		if (jobId.startsWith("TSU")) { //$NON-NLS-1$
			subsys = "TSO"; //$NON-NLS-1$
		} else {
			Pattern pattern = Pattern.compile(JES_JCL_STEP_PATTERN);
			Scanner scanner = new Scanner(JCL);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				Matcher matcher = pattern.matcher(line);
				if (matcher.find() && matcher.groupCount() == 3) {
					if (matcher.group(3).equals("DFHSIP")) { //$NON-NLS-1$
						subsys = "CICS"; //$NON-NLS-1$
						break;
					}
					if (matcher.group(3).equals("DSNYASCP")) { //$NON-NLS-1$
						subsys = "DB2"; //$NON-NLS-1$
						break;
					}
					if (matcher.group(3).equals("DFSMVRC0")) { //$NON-NLS-1$
						subsys = "IMS"; //$NON-NLS-1$
						break;
					}
					if (matcher.group(3).equals("CSQYASCP")) { //$NON-NLS-1$
						subsys = "MQ"; //$NON-NLS-1$
						break;
					}
				}
			}
			scanner.close();
		}

		return subsys;
	}
	
	private static final String MATCH_ALL = "*"; //$NON-NLS-1$
	
	public static String getOwnerFilterValue(SecurityContext securityContext, String owner) {
		String ownerFilter = owner;
		if (ownerFilter == null) {
			String username = ZosUtilities.getUsername(securityContext);
			ownerFilter = (username != null) ? username : MATCH_ALL;
		}
		return ownerFilter;
	}
	
}
