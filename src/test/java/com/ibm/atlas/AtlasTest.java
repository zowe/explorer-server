/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas;

import static org.junit.Assert.*;

public class AtlasTest {

	protected void shouldThrow(Throwable expectedException, RunnableWithException toThrow) throws Exception {
		try {
			toThrow.run();
			fail(String.format("Expected exception %s, but it wasn't throw", toThrow.getClass()));
		} catch (Throwable e) {
			shouldHaveThrown(expectedException, e);
		}
	}

	public static void shouldHaveThrown(Throwable expected, Throwable actual) {
		if (expected.getClass() != actual.getClass()) {
			throw new Error(actual);
		}
		assertEquals(expected.getMessage(), actual.getMessage());
	}
}
