/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas.webservice.resource.socket;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import com.ibm.atlas.webservice.Messages;

public class JobOutputConfigurator extends ServerEndpointConfig.Configurator {
	
	@Inject
	private Logger log;

	@Override
	public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
		String records = "0"; //$NON-NLS-1$
		
		if (request != null) {
			// Extract cookie containing SSO token
			Map<String, List<String>> headers = request.getHeaders();
			if (headers != null) {
				config.getUserProperties().put("cookie", headers.get("cookie")); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
			// Extract query parameter
			Map<String, List<String>> params = request.getParameterMap();
			if (params != null) {
				List<String> paramValues = params.get("records"); //$NON-NLS-1$
				if (paramValues != null && !paramValues.isEmpty()) {
					records = paramValues.get(0);
				}
			}
		} else {
			log.warning(Messages.getString("JobOutputConfigurator.NullRequest")); //$NON-NLS-1$
		}

		config.getUserProperties().put("records", records); //$NON-NLS-1$
	}
}