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

import java.util.regex.Pattern;

import com.ibm.atlas.webservice.Messages;
import com.ibm.atlas.webservice.exceptions.BadRequestException;

public class FilePermissionsHelper {

	private static Pattern validNumeric = Pattern.compile("[0-7]{3}");
	private static Pattern validSymbolic = Pattern.compile("[wrx-]{9}");
	
	private static final int READ_BIT_MASK = 4;
	private static final int WRITE_BIT_MASK = 2;
	private static final int EXECUTE_BIT_MASK = 1;
	
	public static String convertPermissionsNumericToSymbolicForm(String input) throws BadRequestException {
		
		if (validNumeric.matcher(input).matches()) {
			StringBuffer symbolic = new StringBuffer();
			input.toCharArray();
			for (char digit : input.toCharArray()) {
				symbolic.append(getGroupPermissions(digit));
			}
			
			return symbolic.toString();
		}
		throw new BadRequestException(Messages.getString("Files.InvalidNumericPermissions"));
	}
	
	private static String getGroupPermissions(char value) {
		int number = Character.getNumericValue(value);
		StringBuffer group = new StringBuffer();
		group.append(getPermisionString(number, READ_BIT_MASK, "r"));
		group.append(getPermisionString(number, WRITE_BIT_MASK, "w"));
		group.append(getPermisionString(number, EXECUTE_BIT_MASK, "x"));
		return group.toString();
	}

	private static Object getPermisionString(int number, int bitMask, String string) {
		return (number & bitMask) == bitMask ? string : "-";
	}
	
	public static String convertPermissionsSymbolicToNumericForm(String input) throws BadRequestException {
		if (validSymbolic.matcher(input).matches()) {
			StringBuffer numeric = new StringBuffer();
			for(int i=0; i < 9; i=i+3) {
				numeric.append(getNumericGroupPermissions(input.substring(i,i+3)));
			}
			return numeric.toString();
		}
		throw new BadRequestException(Messages.getString("Files.InvalidSymbolicPermissions"));
	}
	
	private static String getNumericGroupPermissions(String groupString) {
		Integer number = 0;
		number += updateNumericPermission(READ_BIT_MASK, groupString.charAt(0), 'r');
		number += updateNumericPermission(WRITE_BIT_MASK, groupString.charAt(1), 'w');
		number += updateNumericPermission(EXECUTE_BIT_MASK, groupString.charAt(2), 'x');
		return number.toString();
	}
	
	private static int updateNumericPermission(int bitMask, char string, char searchString) {
		return string == searchString ? bitMask : 0;
	}
}
