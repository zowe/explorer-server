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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import com.ibm.atlas.model.jobs.JobStatus;
import com.ibm.atlas.webservice.Messages;
import com.ibm.atlas.webservice.services.ZosmfJobsService;

@ServerEndpoint(value = "/api/sockets/jobs/{jobname}/ids/{jobid}/files/{fileid}", configurator = JobOutputConfigurator.class)
public class JobOutput {

	@Inject
	private Logger log;

	private static final int UPDATE_INTERVAL = 5;
	private static final int INITIAL_UPDATE_DELAY = UPDATE_INTERVAL;

	private Session session;
	private String jobname;
	private String jobid;
	private String fileid;
	private int lastRecordNumber;
	private ScheduledExecutorService service;
	
	@Inject 
	private ZosmfJobsService zosmfService;

	@OnOpen
	public void onOpen(Session session, EndpointConfig config, @PathParam("jobname") String jobname,
			@PathParam("jobid") String jobid, @PathParam("fileid") String fileid) {
		initializeSession(session, config, jobname, jobid, fileid);
	}

	@OnClose
	public void onClose(Session session) {
		terminateSession(session);
	}

	@OnError
	public void onError(Session session, Throwable t) {
		log.throwing(JobOutput.class.getName(), "onError", t); //$NON-NLS-1$
		sendMessage(session, Messages.getString("JobOutput.ErrorReading") + t.getMessage()); //$NON-NLS-1$
	}

	@OnMessage
	public void onMessage(Session session, String message) {
		try {
			CloseReason reason = new CloseReason(CloseCodes.UNEXPECTED_CONDITION, Messages.getString("JobOutput.ReceiptUnexpected")); //$NON-NLS-1$
			session.close(reason);
		} catch (IOException e) {
			log.severe(String.format(Messages.getString("JobOutput.WebsocketCloseError"), session.getId(), e.getMessage())); //$NON-NLS-1$
		}
	}

	private void sendMessage(Session session, String message) {
		try {
			session.getBasicRemote().sendText(message);
		} catch (IOException e) {
			log.severe(String.format(Messages.getString("JobOutput.UnableToSend"), session.getId(), e.getMessage())); //$NON-NLS-1$
		}
	}

	private void initializeSession(Session session, EndpointConfig config, String jobname, String jobid,
			String fileid) {
		int records;
		int startRecord;

		this.session = session;
		this.jobname = jobname;
		this.jobid = jobid;
		this.fileid = fileid;
		lastRecordNumber = 0;

		String cookie = ""; //$NON-NLS-1$
		List<String>list =  (List<String>) config.getUserProperties().get("cookie"); //$NON-NLS-1$
		for(String item : list){
			cookie = cookie.concat(item);
		}
		 
		session.getUserProperties().put("cookie", cookie); //$NON-NLS-1$

		try {
			records = Integer.parseInt((String) config.getUserProperties().get("records")); //$NON-NLS-1$
		} catch (NumberFormatException e) {
			records = 0;
		}

		String output = zosmfService.getJobFileRecordsByRange(jobname, jobid, fileid, "0", "0"); //$NON-NLS-1$ //$NON-NLS-2$
		if ( output == null ) {
			output = ""; //$NON-NLS-1$
		}
		String[] lines = output.split(Messages.getString("Files.Newline")); //$NON-NLS-1$

		startRecord = lines.length - records;
		if (startRecord < 0 || records == 0) {
			startRecord = 0;
		}

		StringBuilder sb = new StringBuilder();
		for (int i = startRecord; i < lines.length; i++) {
			sb.append(lines[i]);
			sb.append(Messages.getString("Files.Newline")); //$NON-NLS-1$
		}
		if (sb.length() > 0) sendMessage(session, sb.toString());

		lastRecordNumber = lines.length;

		JobStatus status = zosmfService.getJobStatus(jobname, jobid);
		if (JobStatus.ACTIVE.equals(status)) {
			service = scheduleJobOutputUpdates();
		} else {
			try {
				CloseReason reason = new CloseReason(CloseCodes.NORMAL_CLOSURE, Messages.getString("JobOutput.NoLongerActive")); //$NON-NLS-1$
				session.close(reason);
			} catch (IOException e) {
				log.severe(String.format(Messages.getString("JobOutput.SessionCloseError"), session.getId(), e.getMessage())); //$NON-NLS-1$
			}
		}

		return;
	}

	private void terminateSession(Session session) {
		if ( service != null ) {
			service.shutdown();
		}
		return;
	}

	private ScheduledExecutorService scheduleJobOutputUpdates() {
		ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
		service.scheduleAtFixedRate(getRunnableUpdate(), INITIAL_UPDATE_DELAY, UPDATE_INTERVAL, TimeUnit.SECONDS);
		return service;
	}

	private Runnable getRunnableUpdate() {
		return new Runnable() {
			@Override
			public void run() {
				processJobOutputRequests();
			}
		};
	}

	void processJobOutputRequests() {
		String output = zosmfService.getJobFileRecordsByRange(jobname, jobid, fileid,
				String.format("%d", lastRecordNumber), "0"); //$NON-NLS-1$ //$NON-NLS-2$
		if ( output == null ) {
			output = ""; //$NON-NLS-1$
		}
		String[] lines = output.split("\n"); //$NON-NLS-1$
		if (lines.length > 0 && !lines[0].isEmpty()) {
			sendMessage(session, output);
			lastRecordNumber += lines.length;
		}

		JobStatus status = zosmfService.getJobStatus(jobname, jobid);
		if (JobStatus.ACTIVE.equals(status)) {
			try {
				CloseReason reason = new CloseReason(CloseCodes.NORMAL_CLOSURE, Messages.getString("JobOutput.NoLongerActive")); //$NON-NLS-1$
				session.close(reason);
			} catch (IOException e) {
				log.severe(String.format(Messages.getString("JobOutput.SessionCloseError"), session.getId(), e.getMessage())); //$NON-NLS-1$
			}
		}
	}

}
