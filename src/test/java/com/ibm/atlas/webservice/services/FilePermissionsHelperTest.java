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

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.atlas.AtlasTest;
import com.ibm.atlas.webservice.Messages;
import com.ibm.atlas.webservice.exceptions.BadRequestException;

public class FilePermissionsHelperTest extends AtlasTest {

	static Map<String, String> cases;
	
	@BeforeClass
	public static void setUpCases() {
		cases = new HashMap<>();
		cases.put("700", "rwx------");
		cases.put("770", "rwxrwx---");
		cases.put("777", "rwxrwxrwx");
		cases.put("111", "--x--x--x");
		cases.put("222", "-w--w--w-");
		cases.put("333", "-wx-wx-wx");
		cases.put("444", "r--r--r--");
		cases.put("555", "r-xr-xr-x");
		cases.put("666", "rw-rw-rw-");
		cases.put("740", "rwxr-----");
	}
	
	@Test
	public void callTestNumericToSymbolicPermissionsConversion() throws Exception {
		for (Entry<String, String> entry : cases.entrySet()) {
			String expected = entry.getValue();
			String input = entry.getKey();
			assertEquals(String.format("input={%s}, expected={%s}", input, expected),
				expected, FilePermissionsHelper.convertPermissionsNumericToSymbolicForm(input));
		}

	}

	@Test
	public void testInvalidNumericFormsException() throws Exception {
		List<String> invalidInputs = Arrays.asList("77", "7777", "00", "7-7", "888", "aaa");
		for (String invalid : invalidInputs) {
			final BadRequestException expectedException = new BadRequestException(Messages.getString("Files.InvalidNumericPermissions"));
			shouldThrow(expectedException, () -> FilePermissionsHelper.convertPermissionsNumericToSymbolicForm(invalid));
		}

	}
	
	@Test
	public void callTestSymbolicToNumericPermissionsConversion() throws Exception {
		for (Entry<String, String> entry : cases.entrySet()) {
			String expected = entry.getKey();
			String input = entry.getValue();
			assertEquals(String.format("input={%s}, expected={%s}", input, expected),
				expected, FilePermissionsHelper.convertPermissionsSymbolicToNumericForm(input));
		}

	}
	
	@Test
	public void testInvalidSymbolicFormsException() throws Exception {
		List<String> invalidInputs = Arrays.asList("drwx------", "rwx-------", "rsx------", "rwx-----");
		for (String invalid : invalidInputs) {
			final BadRequestException expectedException = new BadRequestException(Messages.getString("Files.InvalidSymbolicPermissions"));
			shouldThrow(expectedException, () -> FilePermissionsHelper.convertPermissionsSymbolicToNumericForm(invalid));
		}

	}
}