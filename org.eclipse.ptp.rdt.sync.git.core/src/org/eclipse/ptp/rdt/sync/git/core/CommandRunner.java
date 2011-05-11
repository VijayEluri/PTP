/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    John Eblen - initial implementation
 *******************************************************************************/
package org.eclipse.ptp.rdt.sync.git.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ptp.rdt.sync.git.core.messages.Messages;
import org.eclipse.ptp.remote.core.IRemoteConnection;
import org.eclipse.ptp.remote.core.IRemoteFileManager;
import org.eclipse.ptp.remote.core.IRemoteProcess;
import org.eclipse.ptp.remote.core.IRemoteProcessBuilder;
import org.eclipse.ptp.remote.core.exception.RemoteConnectionException;

// Static class for running system-level commands. This includes local and remote directory operations and also running arbitrary
// commands on remote machines.
public class CommandRunner {
	// Nested convenience class for storing the results of a command.
	public static class CommandResults {
		private String stdout;
		private String stderr;
		private int exitCode;

		/**
		 * @return the exitCode
		 */
		public int getExitCode() {
			return exitCode;
		}

		/**
		 * @return the stderr
		 */
		public String getStderr() {
			return stderr;
		}

		/**
		 * @return the stdout
		 */
		public String getStdout() {
			return stdout;
		}

		/**
		 * @param exitCode
		 */
		public void setExitCode(int exitCode) {
			this.exitCode = exitCode;
		}

		/**
		 * @param stderr
		 */
		public void setStderr(String stderr) {
			this.stderr = stderr;
		}

		/**
		 * @param stdout
		 */
		public void setStdout(String stdout) {
			this.stdout = stdout;
		}
	};

	enum DirectoryStatus {
		NOT_A_DIRECTORY, NOT_PRESENT, PRESENT
	}

	/**
	 * Simply check if a local directory is present but do not create
	 * 
	 * @return the directory status
	 */
	public static DirectoryStatus checkLocalDirectory(String localDirectory) {
		final File localDir = new File(localDirectory);
		if (localDir.exists() == false) {
			return DirectoryStatus.NOT_PRESENT;
		}

		else if (localDir.isDirectory()) {
			return DirectoryStatus.PRESENT;
		}

		return DirectoryStatus.NOT_A_DIRECTORY;
	}

	/**
	 * Simply check if a remote directory is present but do not create
	 * 
	 * @param conn
	 * @param remoteDir
	 * @return the directory status
	 */
	public static DirectoryStatus checkRemoteDirectory(IRemoteConnection conn, String remoteDir) {
		final IRemoteFileManager fileManager = conn.getRemoteServices().getFileManager(conn);
		final IFileStore fileStore = fileManager.getResource(remoteDir);
		final IFileInfo fileInfo = fileStore.fetchInfo();

		if (fileInfo.exists() == false) {
			return DirectoryStatus.NOT_PRESENT;
		} else if (fileInfo.isDirectory() == false) {
			return DirectoryStatus.NOT_A_DIRECTORY;
		} else {
			return DirectoryStatus.PRESENT;
		}
	}

	/**
	 * This function creates the local directory if it does not exist.
	 * 
	 * @return whether the directory was already PRESENT
	 * TODO: Handle false return from mkdir
	 */
	public static DirectoryStatus createLocalDirectory(String localDirectory) {
		final DirectoryStatus directoryStatus = checkLocalDirectory(localDirectory);
		if (directoryStatus == DirectoryStatus.NOT_PRESENT) {
			final File localDir = new File(localDirectory);
			localDir.mkdir();
		}

		return directoryStatus;
	}

	/**
	 * This function creates the remote directory if it does not exist. Parent directories are also created if necessary. Note that
	 * this command does not overwrite if the requested remote directory exists but is not a directory.
	 * 
	 * @param conn
	 * @param remoteDir
	 * @param monitor 
	 * @throws CoreException
	 *             on problem creating the remote directory.
	 * @return whether the directory was already PRESENT
	 */
	public static DirectoryStatus createRemoteDirectory(IRemoteConnection conn, String remoteDir, IProgressMonitor monitor) throws CoreException {
		final IRemoteFileManager fileManager = conn.getRemoteServices().getFileManager(conn);
		final IFileStore fileStore = fileManager.getResource(remoteDir);
		final IFileInfo fileInfo = fileStore.fetchInfo();

		if (fileInfo.exists() == true) {
			if (fileInfo.isDirectory() == true) {
				return DirectoryStatus.PRESENT;
			} else {
				return DirectoryStatus.NOT_A_DIRECTORY;
			}
		}

		fileStore.mkdir(EFS.NONE, monitor);

		return DirectoryStatus.NOT_PRESENT;
	}

	/**
	 * Execute command on a remote host and wait for the command to complete.
	 * 
	 * @param conn
	 * @param command
	 * @return CommandResults (contains stdout, stderr, and exit code)
	 * @throws IOException
	 *             in several cases if there is a problem communicating with the remote host.
	 * @throws InterruptedException
	 *             if execution of remote command is interrupted.
	 * @throws RemoteConnectionException
	 * 			   if connection closed and cannot be opened. 
	 * TODO: Expand to work with multiple platforms (assumes UNIX \n line endings.)
	 * TODO: Make robust against buffer overflows - use threads.
	 */
	public static CommandResults executeRemoteCommand(IRemoteConnection conn, String command, String remoteDirectory,
														IProgressMonitor monitor) throws 
																IOException, InterruptedException, RemoteConnectionException {
		if (!conn.isOpen()) {
			try {
				conn.open(monitor);
			} catch (RemoteConnectionException e) {
				throw e;
			}
		}

		// Setup a new process
		final List<String> commandList = new LinkedList<String>();
		commandList.add("sh"); //$NON-NLS-1$
		commandList.add("-c"); //$NON-NLS-1$
		commandList.add(command);

		final IRemoteProcessBuilder rpb = conn.getRemoteServices().getProcessBuilder(conn, commandList);
		final IRemoteFileManager rfm = conn.getRemoteServices().getFileManager(conn);
		if (rfm != null) {
			rpb.directory(rfm.getResource(remoteDirectory));
		}

		// Run process and stream readers
		IRemoteProcess rp = null;
		StreamReader outputStreamReader = null;
		StreamReader errorStreamReader = null;
		int exitCode;
		try {
			rp = rpb.start();
			outputStreamReader = new StreamReader(rp.getInputStream());
			errorStreamReader = new StreamReader(rp.getErrorStream());
			outputStreamReader.start();
			errorStreamReader.start();
			exitCode = rp.waitFor();
			
			// Notify callers of stream IO errors
			IOException outputStreamError = outputStreamReader.getIOException();
			IOException errorStreamError = errorStreamReader.getIOException();
			if (outputStreamError != null) {
				throw outputStreamError;
			} else if (errorStreamError != null) {
				throw errorStreamError;
			}
		} catch (final IOException e) {
			throw e;
		} catch (final InterruptedException e) {
			throw e;
		}
		
		// Successful execution - compile and return results
		final CommandResults commandResults = new CommandResults();
		commandResults.setExitCode(exitCode);
		commandResults.setStdout(outputStreamReader.getResult());
		commandResults.setStderr(errorStreamReader.getResult());
		return commandResults;
	}

	// Enforce as static
	private CommandRunner() {
		throw new AssertionError(Messages.CR_CreateInstanceError);
	}
	
	private static class StreamReader extends Thread {
		private String result = ""; //$NON-NLS-1$
		private final InputStream stream;
		private IOException ioException = null;

		public StreamReader(InputStream s) {
			stream = s;
		}

		public void run() {
			BufferedReader streamReader = new BufferedReader(new InputStreamReader(stream));
			try {
				String line;
				while ((line = streamReader.readLine()) != null) {
					result += line;
					result += "\n"; //$NON-NLS-1$
				}
			} catch (final IOException e) {
				ioException = e;
			} finally {
				try {
					streamReader.close();
				} catch (IOException e) {
					// Ignore
				}
			}
		}

		public IOException getIOException() {
			return ioException;
		}

		public String getResult() {
			return result;
		}
	}
}
