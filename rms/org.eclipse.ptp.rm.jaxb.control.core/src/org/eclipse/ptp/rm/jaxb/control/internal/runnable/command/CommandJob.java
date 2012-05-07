/*******************************************************************************
 * Copyright (c) 2011 University of Illinois All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html 
 * 	
 * Contributors: 
 * 	Albert L. Rossi - design and implementation
 ******************************************************************************/
package org.eclipse.ptp.rm.jaxb.control.internal.runnable.command;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.ptp.core.ModelManager;
import org.eclipse.ptp.core.attributes.AttributeManager;
import org.eclipse.ptp.core.elements.IPJob;
import org.eclipse.ptp.core.elements.IPResourceManager;
import org.eclipse.ptp.core.elements.attributes.JobAttributes;
import org.eclipse.ptp.core.elements.attributes.ProcessAttributes;
import org.eclipse.ptp.core.jobs.IJobStatus;
import org.eclipse.ptp.core.util.CoreExceptionUtils;
import org.eclipse.ptp.remote.core.IRemoteConnection;
import org.eclipse.ptp.remote.core.IRemoteProcess;
import org.eclipse.ptp.remote.core.IRemoteProcessBuilder;
import org.eclipse.ptp.remote.core.RemoteServicesDelegate;
import org.eclipse.ptp.remote.core.exception.RemoteConnectionException;
import org.eclipse.ptp.rm.jaxb.control.IJobController;
import org.eclipse.ptp.rm.jaxb.control.JAXBControlConstants;
import org.eclipse.ptp.rm.jaxb.control.JAXBControlCorePlugin;
import org.eclipse.ptp.rm.jaxb.control.JAXBResourceManagerControl;
import org.eclipse.ptp.rm.jaxb.control.JAXBUtils;
import org.eclipse.ptp.rm.jaxb.control.data.ArgImpl;
import org.eclipse.ptp.rm.jaxb.control.internal.ICommandJob;
import org.eclipse.ptp.rm.jaxb.control.internal.ICommandJobStatus;
import org.eclipse.ptp.rm.jaxb.control.internal.ICommandJobStreamsProxy;
import org.eclipse.ptp.rm.jaxb.control.internal.IStreamParserTokenizer;
import org.eclipse.ptp.rm.jaxb.control.internal.messages.Messages;
import org.eclipse.ptp.rm.jaxb.control.internal.utils.DebuggingLogger;
import org.eclipse.ptp.rm.jaxb.control.internal.utils.EnvironmentVariableUtils;
import org.eclipse.ptp.rm.jaxb.core.IVariableMap;
import org.eclipse.ptp.rm.jaxb.core.JAXBCoreConstants;
import org.eclipse.ptp.rm.jaxb.core.data.ArgType;
import org.eclipse.ptp.rm.jaxb.core.data.AttributeType;
import org.eclipse.ptp.rm.jaxb.core.data.CommandType;
import org.eclipse.ptp.rm.jaxb.core.data.NameValuePairType;
import org.eclipse.ptp.rm.jaxb.core.data.PropertyType;
import org.eclipse.ptp.rm.jaxb.core.data.SimpleCommandType;
import org.eclipse.ptp.rm.jaxb.core.data.TokenizerType;
import org.eclipse.ptp.utils.core.ArgumentParser;
import org.eclipse.ui.progress.IProgressConstants;

/**
 * Implementation of runnable Job for executing external processes. Uses the IRemoteProcessBuilder with the IRemoteConnection for
 * the resource manager's target.
 * 
 * @author arossi
 * 
 */
public class CommandJob extends Job implements ICommandJob {

	public enum JobMode {
		BATCH,
		STATUS,
		INTERACTIVE
	}

	/**
	 * Internal class used for multiplexing output streams between two different endpoints, usually a tokenizer on the one hand and
	 * the stream proxy passed back to the caller on the other.
	 * 
	 * @author arossi
	 */
	private class StreamSplitter extends Thread {
		private final InputStream in;
		private final PipedOutputStream[] pout;
		private final List<BufferedOutputStream> boutList;

		/**
		 * @param in
		 *            the stream to be multiplexed
		 * @param pipe1
		 *            sink's stream from which it will read
		 * @param pipe2
		 *            sink's stream from which it will read
		 * @throws IOException
		 */
		private StreamSplitter(InputStream in, PipedInputStream pipe1, PipedInputStream pipe2) throws IOException {
			this.in = in;
			assert (pipe1 != null && pipe2 != null);
			pout = new PipedOutputStream[] { new PipedOutputStream(pipe1), new PipedOutputStream(pipe2) };
			boutList = new ArrayList<BufferedOutputStream>();
			boutList.add(new BufferedOutputStream(pout[0], JAXBControlConstants.STREAM_BUFFER_SIZE));
			boutList.add(new BufferedOutputStream(pout[1], JAXBControlConstants.STREAM_BUFFER_SIZE));
		}

		/**
		 * Reads from input and writes to the piped streams.
		 */
		@Override
		public void run() {
			BufferedInputStream bin = new BufferedInputStream(in);
			while (true) {
				try {
					int i = bin.read();
					if (i == -1) {
						break;
					}
					BufferedOutputStream stream = null;
					for (Iterator<BufferedOutputStream> b = boutList.iterator(); b.hasNext();) {
						try {
							stream = b.next();
							stream.write(i);
							stream.flush();
						} catch (IOException dead) {
							/*
							 * we need to check for this here because the tokenizer can be set to exit early
							 */
							if (dead.getMessage().indexOf(JAXBControlConstants.DEAD) >= 0) {
								b.remove();
								try {
									stream.close();
								} catch (IOException t) {
								}
							} else {
								throw dead;
							}
						}
					}
				} catch (EOFException eof) {
					break;
				} catch (IOException t) {
					JAXBControlCorePlugin.log(t);
					break;
				}
			}
			for (BufferedOutputStream b : boutList) {
				try {
					b.close();
				} catch (IOException t) {
					JAXBControlCorePlugin.log(t);
				}
			}
			// input stream closed elsewhere
		}
	}

	/**
	 * Extension-based instantiation for custom tokenizer.
	 * 
	 * @param type
	 *            extension name
	 * @return the tokenizer instance
	 * @throws CoreException
	 */
	public static IStreamParserTokenizer getTokenizer(String type) throws CoreException {
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(JAXBControlCorePlugin.PLUGIN_ID,
				JAXBControlConstants.TOKENIZER_EXT_PT);
		IConfigurationElement[] elements = extensionPoint.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			try {
				if (element.getAttribute(JAXBControlConstants.ID).equals(type)) {
					return (IStreamParserTokenizer) element.createExecutableExtension(JAXBControlConstants.CLASS);
				}
			} catch (CoreException ce) {
				throw ce;
			} catch (Throwable t) {
				throw CoreExceptionUtils.newException(Messages.StreamTokenizerInstantiationError + type, t);
			}
		}
		return null;
	}

	private final String uuid;
	private final CommandType command;
	private final IJobController control;
	private final ICommandJobStreamsProxy proxy;
	private final IVariableMap rmVarMap;
	private final int flags;
	private final boolean waitForId;
	private final JobMode jobMode;
	private final boolean keepOpen;
	private final ILaunchConfiguration launchConfiguration;
	private final String launchMode;
	private final List<Job> cmdJobs = new ArrayList<Job>();

	private final StringBuffer error;
	private Thread jobThread;
	private IRemoteProcess process;
	private IStreamParserTokenizer stdoutTokenizer;
	private IStreamParserTokenizer stderrTokenizer;
	private Thread stdoutT;
	private Thread stderrT;
	private InputStream tokenizerOut;
	private InputStream tokenizerErr;
	private StreamSplitter outSplitter;
	private StreamSplitter errSplitter;
	private IStreamMonitor[] batchMonitors;
	private ICommandJobStatus jobStatus;
	private IStatus status;
	private boolean active;

	/**
	 * @param jobUUID
	 *            either internal or resource specific identifier
	 * @param command
	 *            JAXB data element
	 * @param mode
	 *            whether submission is batch, interactive or status
	 * @param rm
	 *            the calling resource manager
	 */
	public CommandJob(String jobUUID, CommandType command, JobMode jobMode, IJobController control,
			ILaunchConfiguration configuration, String launchMode) {
		super(command.getName() + JAXBControlConstants.CO + JAXBControlConstants.SP
				+ (jobUUID == null ? control.getConnectionName() : jobUUID));
		this.command = command;
		this.jobMode = jobMode;
		this.launchConfiguration = configuration;
		this.launchMode = launchMode;
		this.control = control;
		this.rmVarMap = control.getEnvironment();
		this.uuid = jobUUID;
		this.proxy = new CommandJobStreamsProxy();
		this.waitForId = command.isWaitForId();
		this.error = new StringBuffer();
		this.keepOpen = command.isKeepOpen();
		String flags = command.getFlags();
		this.flags = getFlags(flags);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.rm.jaxb.core.ICommandJob#getExecuteStatus()
	 */
	public ICommandJobStatus getJobStatus() {
		return jobStatus;
	}

	/**
	 * @return the process wrapper
	 */
	public IRemoteProcess getProcess() {
		return process;
	}

	/**
	 * @return object wrapping stream monitors.
	 */
	public ICommandJobStreamsProxy getProxy() {
		return proxy;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.rm.jaxb.core.ICommandJob#getRunStatus()
	 */
	public IStatus getRunStatus() {
		return status;
	}

	/**
	 * @return if job is active
	 */
	public boolean isActive() {
		boolean b = false;
		synchronized (this) {
			b = active;
		}
		return b;
	}

	/**
	 * @return if job is batch
	 */
	public boolean isBatch() {
		return jobMode == JobMode.BATCH;
	}

	/**
	 * Wait for any special stream consumer threads to exit. We ignore the stream monitors here.
	 * 
	 * @return CoreException
	 */
	public CoreException joinConsumers() {
		if (!isActive()) {
			return null;
		}

		Throwable t = null;

		if (outSplitter != null) {
			try {
				outSplitter.join();
			} catch (InterruptedException ignored) {
			}
		}

		if (errSplitter != null) {
			try {
				errSplitter.join();
			} catch (InterruptedException ignored) {
			}
		}

		if (stdoutT != null) {
			try {
				stdoutT.join();
			} catch (InterruptedException ignored) {
			}
			t = stdoutTokenizer.getInternalError();
		}

		if (stderrT != null) {
			try {
				stderrT.join();
			} catch (InterruptedException ignored) {
			}
			t = stderrTokenizer.getInternalError();
		}

		if (t != null) {
			return CoreExceptionUtils.newException(t.getMessage(), t);
		}

		return null;
	}

	/*
	 * First unblock any wait; this will allow the run method to return. Destroy the process and close streams, interrupt the thread
	 * and cancel with manager. (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.rm.jaxb.core.ICommandJob#terminate()
	 */
	public synchronized void terminate() {
		if (active) {
			active = false;
			if (jobStatus != null) {
				jobStatus.cancelWait();
			}
			if (process != null && !process.isCompleted()) {
				process.destroy();
				if (proxy != null) {
					proxy.close();
				}
				CoreException e = joinConsumers();
				if (e != null) {
					JAXBControlCorePlugin.log(e);
				}
			}
			if (jobThread != null && jobThread != Thread.currentThread()) {
				if (jobThread.isAlive()) {
					jobThread.interrupt();
				}
			}
			for (Job job : cmdJobs) {
				if (job.getState() == Job.RUNNING) {
					job.cancel();
				}
			}
			cmdJobs.clear();
			cancel();
		}
	}

	/**
	 * The resource manager should wait for the job id on the stream (parsed by an apposite tokenizer) before returning the status
	 * object to the caller.
	 * 
	 * @return whether to wait
	 */
	public boolean waitForId() {
		return waitForId;
	}

	/**
	 * Create a temporary debug model so that the debug view displays correctly
	 * 
	 * @param jobId
	 *            job ID for this job
	 * @param vars
	 *            variable map
	 */
	private IStatus createDebugModel(String jobId, IVariableMap vars) {
		IPResourceManager rm = ModelManager.getInstance().getUniverse().getResourceManager(control.getControlId());
		if (rm != null) {
			/*
			 * Remove any old jobs with the same job ID
			 */
			IPJob job = rm.getJobById(jobId);
			if (job != null) {
				rm.removeJobs(Arrays.asList(job));
			}

			AttributeManager attrMgr = new AttributeManager();
			attrMgr.addAttribute(JobAttributes.getJobIdAttributeDefinition().create(jobId));
			attrMgr.addAttribute(JobAttributes.getStateAttributeDefinition().create(JobAttributes.State.RUNNING));
			attrMgr.addAttribute(JobAttributes.getDebugFlagAttributeDefinition().create(true));

			job = rm.newJob(jobId, attrMgr);

			attrMgr = new AttributeManager();
			attrMgr.addAttribute(ProcessAttributes.getStateAttributeDefinition().create(ProcessAttributes.State.RUNNING));

			Object attr = getAttributeValue(vars, JAXBControlConstants.MPI_PROCESSES);
			if (attr != null) {
				String numProcsStr = String.valueOf(attr);
				int numProcs;
				try {
					numProcs = Integer.parseInt(numProcsStr);
				} catch (NumberFormatException e) {
					return new Status(IStatus.ERROR, JAXBControlCorePlugin.getUniqueIdentifier(),
							Messages.CommandJob_UnableToDetermineTasksError);
				}
				BitSet procRanks = new BitSet(numProcs);
				procRanks.set(0, numProcs, true);
				job.addProcessesByJobRanks(procRanks, attrMgr);
				rm.addJobs(null, Arrays.asList(job));
				return Status.OK_STATUS;
			}
		}

		return new Status(IStatus.ERROR, JAXBControlCorePlugin.getUniqueIdentifier(),
				Messages.CommandJob_UnableToDetermineTasksError);
	}

	/**
	 * Uses the IRemoteProcessBuilder to set up the command and environment. After start, the tokenizers (if any) are handled, and
	 * stream redirection managed. Returns immediately if <code>keepOpen</code> is true; else waits for the process, then joins on
	 * the consumers.x
	 */
	private IStatus execute(IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, 100);
		try {
			synchronized (this) {
				status = null;
				active = false;
			}
			IRemoteProcessBuilder builder = prepareCommand(progress.newChild(10));
			if (progress.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			prepareEnv(builder);
			progress.worked(10);

			process = null;
			try {
				process = builder.start(flags);
			} catch (IOException t) {
				throw CoreExceptionUtils.newException(Messages.CouldNotLaunch + builder.command(), t);
			}
			progress.worked(30);
			maybeInitializeTokenizers(builder, progress.newChild(10));
			setOutStreamRedirection(process);
			setErrStreamRedirection(process);
			startConsumers(process);

			synchronized (this) {
				active = true;
			}
			progress.worked(20);

			int exit = 0;
			if (uuid != null) {
				if (!waitForId) {
					try {
						exit = process.exitValue();
					} catch (Throwable t) {
					}
					if ((exit != 0 || error.length() > 0)) {
						processError(builder.command().get(0), exit, null);
					}
					return Status.OK_STATUS;
				}

				if (keepOpen) {
					control.setInteractiveJob(this);
					return Status.OK_STATUS;
				}
			}

			try {
				exit = process.waitFor();
			} catch (InterruptedException ignored) {
			}

			CoreException e = joinConsumers();

			if (exit != 0) {
				processError(builder.command().get(0), exit, e);
			}
		} catch (CoreException ce) {
			return ce.getStatus();
		} catch (Throwable t) {
			return CoreExceptionUtils.getErrorStatus(Messages.ProcessRunError, t);
		}

		synchronized (this) {
			active = false;
		}
		return Status.OK_STATUS;
	}

	/**
	 * Look up the value of an attribute in the map. Checks if the attribute is linked and if so, returns the linked value instead.
	 * 
	 * @param vars
	 *            map containing attributes
	 * @param name
	 *            name of attribute to look up
	 * @return value of the attribute, or null if not found
	 */
	private Object getAttributeValue(IVariableMap vars, String name) {
		AttributeType attr = (AttributeType) vars.get(name);
		if (attr != null) {
			String link = attr.getLinkValueTo();
			if (link != null) {
				Object linkVal = getAttributeValue(vars, link);
				if (linkVal != null) {
					return linkVal;
				}
			}
			return attr.getValue();
		}
		return null;
	}

	/**
	 * Converts or'd string into bit-wise or of available flags for remote process builder.
	 * 
	 * @param flags
	 * @return bit-wise or
	 */
	private int getFlags(String flags) {
		if (flags == null) {
			return IRemoteProcessBuilder.NONE;
		}

		String[] split = flags.split(JAXBControlConstants.REGPIP);
		int f = IRemoteProcessBuilder.NONE;
		for (String s : split) {
			s = s.trim();
			if (JAXBControlConstants.TAG_ALLOCATE_PTY.equals(s)) {
				f |= IRemoteProcessBuilder.ALLOCATE_PTY;
			} else if (JAXBControlConstants.TAG_FORWARD_X11.equals(s)) {
				f |= IRemoteProcessBuilder.FORWARD_X11;
			}
		}
		return f;
	}

	/**
	 * Checks to see what tokenizers are configured for this resource manager. If the two streams have been joined, it will prefer
	 * the redirect parser if it exists; otherwise the joined streams will be parsed by the stdout parser.<br>
	 * <br>
	 * If there is a custom extension tokenizer, it will be instantiated here.
	 * 
	 * @param builder
	 * @param monitor
	 * @throws CoreException
	 */
	private void maybeInitializeTokenizers(IRemoteProcessBuilder builder, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 100);
		try {
			TokenizerType t = null;

			if (builder.redirectErrorStream()) {
				t = command.getRedirectParser();
			}

			if (t == null) {
				t = command.getStdoutParser();
			}

			if (t != null) {
				try {
					String type = t.getType();
					if (type != null) {
						stdoutTokenizer = getTokenizer(type);
					} else {
						stdoutTokenizer = new ConfigurableRegexTokenizer(t);
					}
					stdoutTokenizer.initialize(uuid, rmVarMap, progress.newChild(50));
					if (progress.isCanceled()) {
						return;
					}
				} catch (Throwable e) {
					throw CoreExceptionUtils.newException(Messages.StdoutParserError, e);
				}
			}

			t = command.getStderrParser();
			if (t != null) {
				try {
					String type = t.getType();
					if (type != null) {
						stderrTokenizer = getTokenizer(type);
					} else {
						stderrTokenizer = new ConfigurableRegexTokenizer(t);
					}
					stderrTokenizer.initialize(uuid, rmVarMap, progress.newChild(50));
					if (progress.isCanceled()) {
						return;
					}
				} catch (Throwable e) {
					throw CoreExceptionUtils.newException(Messages.StdoutParserError, e);
				}
			}
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}

	/**
	 * Resolves the command arguments against the current environment, then gets the process builder from the remote connection.
	 * Also sets the directory if it is defined (otherwise it defaults to the connection dir).
	 * 
	 * @param monitor
	 * @return the process builder
	 * @throws CoreException
	 */
	private IRemoteProcessBuilder prepareCommand(IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 10);
		try {
			List<ArgType> args = command.getArg();
			if (args == null) {
				throw CoreExceptionUtils.newException(Messages.MissingArglistFromCommandError, null);
			}
			ArgumentParser cmdArgs = new ArgumentParser(ArgImpl.getArgs(uuid, args, rmVarMap));
			RemoteServicesDelegate delegate = JAXBUtils.getRemoteServicesDelegate(control.getRemoteServicesId(),
					control.getConnectionName(), progress.newChild(5));
			if (progress.isCanceled()) {
				return null;
			}
			if (delegate.getRemoteConnection() == null) {
				throw CoreExceptionUtils.newException(Messages.MissingArglistFromCommandError, new Throwable(
						Messages.UninitializedRemoteServices));
			}
			IRemoteConnection conn = delegate.getRemoteConnection();
			try {
				JAXBResourceManagerControl.checkConnection(conn, progress.newChild(5));
			} catch (RemoteConnectionException rce) {
				throw CoreExceptionUtils.newException(rce.getLocalizedMessage(), rce);
			}
			if (progress.isCanceled()) {
				return null;
			}
			if (DebuggingLogger.getLogger().getCommand()) {
				System.out.println(getName() + ": " + cmdArgs.getCommandLine(false)); //$NON-NLS-1$
			}
			IRemoteProcessBuilder builder = delegate.getRemoteServices().getProcessBuilder(conn, cmdArgs.getTokenList());
			String directory = command.getDirectory();
			if (directory != null && !JAXBControlConstants.ZEROSTR.equals(directory)) {
				directory = rmVarMap.getString(uuid, directory);
				IFileStore dir = delegate.getRemoteFileManager().getResource(directory);
				builder.directory(dir);
			}
			return builder;
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}

	/**
	 * Either appends to or replaces the process builder's environment with the Launch Configuration environment variables. Also
	 * sets redirectErrorStream on the builder.
	 * 
	 * @param builder
	 * @throws CoreException
	 */
	private void prepareEnv(IRemoteProcessBuilder builder) throws CoreException {
		if (!control.getAppendEnv()) {
			builder.environment().clear();
			Map<String, String> live = control.getLaunchEnv();
			for (String var : live.keySet()) {
				builder.environment().put(var, live.get(var));
			}
		} else {
			if (command.isReplaceEnvironment()) {
				builder.environment().clear();
			}
			/*
			 * first static env, then dynamic
			 */
			List<NameValuePairType> vars = command.getEnvironment();
			for (NameValuePairType var : vars) {
				EnvironmentVariableUtils.addVariable(uuid, var, builder.environment(), rmVarMap);
			}

			Map<String, String> live = control.getLaunchEnv();
			for (String var : live.keySet()) {
				builder.environment().put(var, live.get(var));
			}
		}

		builder.redirectErrorStream(command.isRedirectStderr());
	}

	/**
	 * Resolves the command input arguments against the current environment.
	 * 
	 * @return the arguments as a single string
	 * @throws CoreException
	 */
	private String prepareInput() throws CoreException {
		List<ArgType> args = command.getInput();
		return ArgImpl.toString(uuid, args, control.getEnvironment());
	}

	/**
	 * Auxiliary for processing error based on exit value.
	 * 
	 * @param arg
	 *            first arg of command
	 * @param exit
	 *            of process
	 * @param e
	 *            additional exception info
	 * @throws CoreException
	 */
	private void processError(String arg, int exit, CoreException e) throws CoreException {
		if (e != null) {
			error.append(e.getMessage()).append(JAXBControlConstants.LINE_SEP);
		}
		String message = error.toString();
		error.setLength(0);
		throw CoreExceptionUtils.newException(arg + JAXBControlConstants.SP + Messages.ProcessExitValueError
				+ (JAXBControlConstants.ZEROSTR + exit) + JAXBControlConstants.LINE_SEP + message, null);
	}

	/**
	 * Configures handling of the error stream. If there is a tokenizer, the stream is split between it and the proxy monitor.
	 * Otherwise, the proxy just gets the stream.
	 * 
	 * @param process
	 * @throws IOException
	 */
	private void setErrStreamRedirection(IRemoteProcess process) throws IOException {
		if (stderrTokenizer == null) {
			proxy.setErrMonitor(new CommandJobStreamMonitor(process.getErrorStream()));
		} else {
			PipedInputStream tokenizerErr = new PipedInputStream();
			this.tokenizerErr = tokenizerErr;
			PipedInputStream monitorErr = new PipedInputStream();
			errSplitter = new StreamSplitter(process.getErrorStream(), tokenizerErr, monitorErr);
			proxy.setErrMonitor(new CommandJobStreamMonitor(monitorErr));
		}
		proxy.getErrorStreamMonitor().addListener(new IStreamListener() {
			public void streamAppended(String text, IStreamMonitor monitor) {
				error.append(text);
			}
		});
	}

	/**
	 * Configures handling of the stdout stream. If there is a tokenizer, the stream is split between it and the proxy monitor.
	 * Otherwise, the proxy just gets the stream.
	 * 
	 * @param process
	 * @throws IOException
	 */
	private void setOutStreamRedirection(IRemoteProcess process) throws IOException {
		if (stdoutTokenizer == null) {
			proxy.setOutMonitor(new CommandJobStreamMonitor(process.getInputStream()));
		} else {
			PipedInputStream tokenizerOut = new PipedInputStream();
			this.tokenizerOut = tokenizerOut;
			PipedInputStream monitorOut = new PipedInputStream();
			outSplitter = new StreamSplitter(process.getInputStream(), tokenizerOut, monitorOut);
			proxy.setOutMonitor(new CommandJobStreamMonitor(monitorOut));
		}
	}

	/**
	 * Initiates stream reading on all consumers.
	 * 
	 * @param process
	 * @throws CoreException
	 */
	private void startConsumers(IRemoteProcess process) throws CoreException {
		if (outSplitter != null) {
			outSplitter.start();
		}

		if (errSplitter != null) {
			errSplitter.start();
		}

		proxy.startMonitors();

		if (isBatch()) {
			/*
			 * hang on to the streams for the brief life of the job itself, only for error tracking; do not send to console
			 */
			batchMonitors = new IStreamMonitor[2];
			batchMonitors[0] = proxy.getOutputStreamMonitor();
			batchMonitors[1] = proxy.getErrorStreamMonitor();
			proxy.setOutMonitor(null);
			proxy.setErrMonitor(null);
		}

		if (stdoutTokenizer != null) {
			try {
				stdoutTokenizer.setInputStream(tokenizerOut);
				stdoutT = new Thread(stdoutTokenizer);
				stdoutT.start();
			} catch (Throwable e) {
				throw CoreExceptionUtils.newException(Messages.StdoutParserError, e);
			}
		}

		if (stderrTokenizer != null) {
			try {
				stderrTokenizer.setInputStream(tokenizerErr);
				stderrT = new Thread(stderrTokenizer);
				stderrT.start();
			} catch (Throwable e) {
				throw CoreExceptionUtils.newException(Messages.StderrParserError, e);
			}
		}
	}

	/**
	 * Calls {@link #prepareInput()}.
	 * 
	 * @param process
	 */
	private IStatus writeInputToProcess(IRemoteProcess process) {
		OutputStream stream = process.getOutputStream();
		try {
			stream.write(prepareInput().getBytes());
			stream.write(JAXBControlConstants.LINE_SEP.getBytes());
			stream.flush();
		} catch (Throwable t) {
			return CoreExceptionUtils.getErrorStatus(Messages.ProcessRunError, t);
		}
		return Status.OK_STATUS;
	}

	/**
	 * If this process has no input, execute it normally. Otherwise, if the process is to be kept open, check for the pseudoTerminal
	 * job; if it is there and still alive, send the input to it; if not, start the process, and then send the input.
	 */
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, 100);
		try {
			jobThread = Thread.currentThread();
			boolean input = !command.getInput().isEmpty();
			if (input) {
				ICommandJob job = control.getInteractiveJob();
				if (job != null && job.isActive()) {
					IRemoteProcess process = job.getProcess();
					/*
					 * Do not allow relaunching debugger. This can't be easily supported currently since the debugger assumes each
					 * launch will have a unique job ID.
					 */
					if (process != null && !process.isCompleted() && !launchMode.equals(ILaunchManager.DEBUG_MODE)) {
						jobStatus = job.getJobStatus();
						return writeInputToProcess(process);
					} else {
						job.terminate();
						/*
						 * since the process is dead, termination is just clean-up, no need to force external termination
						 */
						control.setInteractiveJob(null);
					}
				}
			}
			progress.worked(25);

			for (SimpleCommandType cmd : command.getPreLaunchCmd()) {
				Job job = new SimpleCommandJob(null, cmd, command.getDirectory(), control);
				job.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
				job.schedule();
				if (cmd.isWait()) {
					try {
						job.join();
						if (!cmd.isIgnoreExitStatus()) {
							if (!job.getResult().isOK()) {
								return job.getResult();
							}
						}
					} catch (InterruptedException ignored) {
					}
				} else {
					cmdJobs.add(job);
				}
			}

			status = execute(progress.newChild(50));

			if (progress.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			if (uuid == null) {
				/*
				 * these jobs will have waited for the exit of the process.
				 */
				return status;
			}

			/*
			 * When there is a UUID defined for this command, set the status for it. If the submit job lacks a jobId on the standard
			 * streams, then we assign it the UUID (it is most probably interactive); else we wait for the id to be set by the
			 * tokenizer. NOTE that the caller should now join on all commands with this property (05.01.2011). Open connection jobs
			 * should have their jobId tokenizers set a RUNNING state.
			 */
			jobStatus = null;
			String waitUntil = keepOpen ? IJobStatus.RUNNING : IJobStatus.SUBMITTED;
			ICommandJob parent = keepOpen ? this : null;

			if (waitForId) {
				jobStatus = new CommandJobStatus(parent, control);
				jobStatus.setOwner(rmVarMap.getString(JAXBControlConstants.CONTROL_USER_NAME));
				jobStatus.setQueueName(rmVarMap.getString(JAXBControlConstants.CONTROL_QUEUE_NAME));
				if (!isBatch()) {
					jobStatus.setProcess(process);
				}
				jobStatus.setProxy(getProxy());
				jobStatus.setLaunchConfig(launchConfiguration);
				try {
					jobStatus.waitForJobId(uuid, waitUntil, control.getStatusMap(), progress.newChild(20));
				} catch (CoreException failed) {
					error.append(jobStatus.getStreamsProxy().getOutputStreamMonitor().getContents()).append(
							JAXBCoreConstants.LINE_SEP);

					status = CoreExceptionUtils.getErrorStatus(failed.getMessage() + JAXBCoreConstants.LINE_SEP + error.toString(),
							null);
					error.setLength(0);
					return status;
				}
			} else {
				if (!keepOpen) {
					CoreException e = joinConsumers();
					if (e != null) {
						return CoreExceptionUtils.getErrorStatus(e.getMessage(), e);
					}
				}
				PropertyType p = (PropertyType) rmVarMap.get(uuid);
				String state = (String) p.getValue();
				if (state == null) {
					state = isActive() ? IJobStatus.RUNNING : IJobStatus.FAILED;
					p.setValue(state);
				}
				p.setName(uuid);
				jobStatus = new CommandJobStatus(uuid, state, parent, control);
				jobStatus.setOwner(rmVarMap.getString(JAXBControlConstants.CONTROL_USER_NAME));
				jobStatus.setQueueName(rmVarMap.getString(JAXBControlConstants.CONTROL_QUEUE_NAME));
				if (!isBatch()) {
					jobStatus.setProcess(process);
				}
				jobStatus.setProxy(getProxy());
				jobStatus.setLaunchConfig(launchConfiguration);
			}

			if (progress.isCanceled()) {
				return status;
			}

			if (!jobStatus.getState().equals(IJobStatus.COMPLETED)) {
				if (input) {
					if (process != null && !process.isCompleted()) {
						status = writeInputToProcess(process);
					}
				}

				/*
				 * Create the debug model if necessary
				 */
				if (status.isOK() && launchMode.equals(ILaunchManager.DEBUG_MODE)) {
					status = createDebugModel(jobStatus.getJobId(), rmVarMap);
				}

				if (status.isOK()) {
					/*
					 * Once job has started running, execute any post launch commands
					 */
					for (SimpleCommandType cmd : command.getPostLaunchCmd()) {
						Job job = new SimpleCommandJob(uuid, cmd, command.getDirectory(), control);
						job.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
						job.schedule();
						if (cmd.isWait()) {
							try {
								job.join();
								if (!cmd.isIgnoreExitStatus()) {
									if (!job.getResult().isOK()) {
										terminate();
										status = job.getResult();
									}
								}
							} catch (InterruptedException ignored) {
							}
						} else {
							cmdJobs.add(job);
						}
					}
				} else {
					terminate();
				}
			} else if (keepOpen && IJobStatus.CANCELED.equals(jobStatus.getStateDetail())) {
				terminate();
			}

		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
		return status;
	}
}
