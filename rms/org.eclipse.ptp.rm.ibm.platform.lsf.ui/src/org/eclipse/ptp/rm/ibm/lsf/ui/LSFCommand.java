// Copyright (c) 2013 IBM Corporation and others. All rights reserved. 
// This program and the accompanying materials are made available under the 
// terms of the Eclipse Public License v1.0s which accompanies this distribution, 
// and is available at http://www.eclipse.org/legal/epl-v10.html

package org.eclipse.ptp.rm.ibm.lsf.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ptp.rm.ibm.lsf.ui.widgets.Messages;
import org.eclipse.remote.core.IRemoteConnection;
import org.eclipse.remote.core.IRemoteProcess;
import org.eclipse.remote.core.IRemoteProcessBuilder;

/**
 * This class implements invocation of LSF commands as a job running on a non-UI thread with the
 * ability for the user to cancel the job.
 */
public class LSFCommand implements IRunnableWithProgress {

	private final String command[];
	private final String commandDescription;
	private final IRemoteConnection remoteConnection;
	private final Vector<String[]> commandResponse;
	private String columnLabels[];
	public static final int OK = 0;
	public static final int COMMAND_ERROR = 3;
	public static final int CANCELED = 2;
	public static final int NO_DATA = 3;
	private IStatus runStatus;

	/**
	 * Constructor for creating a job to run a LSF command
	 * 
	 * @param name
	 *            - Name of the job
	 * @param connection
	 *            The remote connection where the job will be run
	 * @param cmd
	 *            Array containing command name and command parameters
	 */
	public LSFCommand(String name, IRemoteConnection connection, String cmd[]) {
		commandDescription = name;
		remoteConnection = connection;
		command = cmd;
		commandResponse = new Vector<String[]>();
	}

	/**
	 * Run the LSF command specified when this object was created.
	 * 
	 * @param monitor
	 *            - Progress monitor used to monitor and control job execution
	 */
	@Override
	public void run(IProgressMonitor monitor) {
		IRemoteProcessBuilder processBuilder = remoteConnection.getProcessBuilder(command);
		IRemoteProcess process = null;
		try {
			BufferedReader reader;
			String data;
			boolean headerLine;

			monitor.beginTask(commandDescription, IProgressMonitor.UNKNOWN);
			process = processBuilder.start();
			try {
				for (;;) {
					if (process.isCompleted()) {
						break;
					}
					if (monitor.isCanceled()) {
						process.destroy();
						monitor.done();
						runStatus = new Status(IStatus.CANCEL, Activator.PLUGIN_ID, CANCELED, Messages.CommandCancelMessage, null);
						return;
					}
					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {
				// Do nothing
			}
			if (process.exitValue() == 0) {
				String columnData[];

				/*
				 * Read stderr and check for "No application profiles found."
				 * as the first line of output. Subsequent lines are ignored.
				 */
				reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				data = reader.readLine();
				headerLine = true;
				while (data != null) {
					if (headerLine) {
						if (data.equals("No application profiles found.")) { //$NON-NLS-1$
							reader.close();
							monitor.done();
							runStatus = new Status(IStatus.INFO, Activator.PLUGIN_ID, NO_DATA, Messages.NoProfileMessage, null);
							return;
						}
						headerLine = false;
					}
					data = reader.readLine();
				}
				reader.close();
				/*
				 * Read stdout and tokenize each line of data into an array of
				 * blank-delimited strings. The first line of output is the
				 * column headings. Subsequent lines are reservation data.
				 */
				reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				data = reader.readLine();
				headerLine = true;
				commandResponse.clear();
				while (data != null) {
					if (headerLine) {
						if (data.equals("No reservation found")) { //$NON-NLS-1$
							reader.close();
							monitor.done();
							runStatus = new Status(IStatus.INFO, Activator.PLUGIN_ID, NO_DATA, Messages.NoReservationMessage, null);
							return;
						} else {
							columnLabels = data.split(" +"); //$NON-NLS-1$
							headerLine = false;
						}
					} else {
						data = data.replaceAll(" +/ +", "/"); //$NON-NLS-1$ //$NON-NLS-2$
						columnData = data.split(" +"); //$NON-NLS-1$
						commandResponse.add(columnData);
					}
					data = reader.readLine();
				}
				reader.close();
			} else {
				monitor.done();
				runStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, COMMAND_ERROR, Messages.LSFCommandFailed, null);
				return;
			}
		} catch (IOException e) {
			monitor.done();
			runStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, COMMAND_ERROR, Messages.LSFCommandFailed, e);
			return;
		}
		monitor.done();
		runStatus = new Status(IStatus.OK, Activator.PLUGIN_ID, OK, Messages.OkMessage, null);
		return;
	}

	/**
	 * Get the command response
	 * 
	 * @return Command response
	 */
	public Vector<String[]> getCommandResponse() {
		return commandResponse;
	}

	/**
	 * Get the column headings. Column headings are the first line of the command response data
	 * where each column has a blank-delimited column heading
	 * 
	 * @return Column heading data
	 */
	public String[] getColumnLabels() {
		return columnLabels;
	}

	public IStatus getRunStatus() {
		return runStatus;
	}
}