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

import java.io.ByteArrayInputStream;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ptp.rdt.sync.core.SyncFlag;
import org.eclipse.ptp.rdt.sync.core.SyncManager;
import org.eclipse.ptp.rdt.sync.core.serviceproviders.ISyncServiceProvider;
import org.eclipse.ptp.rdt.sync.git.core.messages.Messages;
import org.eclipse.ptp.remote.core.IRemoteConnection;
import org.eclipse.ptp.remote.core.IRemoteServices;
import org.eclipse.ptp.remote.core.PTPRemoteCorePlugin;
import org.eclipse.ptp.remote.core.exception.RemoteConnectionException;
import org.eclipse.ptp.services.core.ServiceProvider;

public class GitServiceProvider extends ServiceProvider implements ISyncServiceProvider {
	public static final String ID = "org.eclipse.ptp.rdt.sync.git.core.GitServiceProvider"; //$NON-NLS-1$

	private static final String GIT_LOCATION = "location"; //$NON-NLS-1$

	private static final String GIT_CONNECTION_NAME = "connectionName"; //$NON-NLS-1$
	private static final String GIT_SERVICES_ID = "servicesId"; //$NON-NLS-1$
	private static final String GIT_PROJECT_NAME = "projectName"; //$NON-NLS-1$
	private IProject fProject = null;
	private String fLocation = null;
	private IRemoteConnection fConnection = null;
	private GitRemoteSyncConnection fSyncConnection = null;
	private boolean hasBeenSynced = false;

	private static final ReentrantLock syncLock = new ReentrantLock();
	private Integer fWaitingThreadsCount = 0;
	private Integer syncTaskId = -1; // ID for most recent synchronization task, functions as a time-stamp
	private int finishedSyncTaskId = -1; // all synchronizations up to this ID (including it) have finished

	/**
	 * Get the remote directory that will be used for synchronization
	 * 
	 * @return path
	 */
	public String getLocation() {
		if (fLocation == null) {
			fLocation = getString(GIT_LOCATION, null);
		}
		return fLocation;
	}

	/**
	 * Get the project to be synchronized
	 * 
	 * @return project
	 */
	public IProject getProject() {
		if (fProject == null) {
			final String name = getString(GIT_PROJECT_NAME, null);
			if (name != null) {
				fProject = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
			}
		}
		return fProject;
	}

	/**
	 * Get the remote connection used for synchronization
	 * 
	 * @return remote connection
	 */
	public IRemoteConnection getRemoteConnection() {
		if (fConnection == null) {
			final String name = getString(GIT_CONNECTION_NAME, null);
			if (name != null) {
				final IRemoteServices services = getRemoteServices();
				if (services != null) {
					fConnection = services.getConnectionManager().getConnection(name);
				}
			}
		}
		return fConnection;
	}

	/**
	 * Get the remote services used for the connection
	 * 
	 * @return remote services
	 */
	public IRemoteServices getRemoteServices() {
		final String id = getString(GIT_SERVICES_ID, null);
		if (id != null) {
			return PTPRemoteCorePlugin.getDefault().getRemoteServices(id);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.services.core.IServiceProvider#isConfigured()
	 */
	public boolean isConfigured() {
		return getLocation() != null && getRemoteConnection() != null && getProject() != null;
	}

	/**
	 * Set the remote directory that will be used for synchronization
	 * 
	 * @param location
	 *            directory path
	 * @throws RuntimeException
	 *             if already set. Changing these local parameters is not currently supported but should be possible.
	 */
	public void setLocation(String location) {
		if (fLocation != null) {
			throw new RuntimeException(Messages.GSP_ChangeLocationError);
		}
		fLocation = location;
		putString(GIT_LOCATION, location);
	}

	/**
	 * Set the project that will be synchronized
	 * 
	 * @param project
	 *            project to synchronize
	 */
	public void setProject(IProject project) {
		if (fProject != null) {
			throw new RuntimeException(Messages.GSP_ChangeProjectError);
		}
		fProject = project;
		putString(GIT_PROJECT_NAME, project.getName());
	}

	/**
	 * set the remote connection used for synchronization
	 * 
	 * @param conn
	 *            remote connection
	 * @throws RuntimeException
	 *             if already set. Changing these local parameters is not currently supported but should be possible.
	 */
	public void setRemoteConnection(IRemoteConnection conn) {
		if (fConnection != null) {
			throw new RuntimeException(Messages.GSP_ChangeConnectionError);
		}
		fConnection = conn;
		putString(GIT_CONNECTION_NAME, conn.getName());
	}

	/**
	 * Set the remote services used for the connection
	 * 
	 * @param services
	 *            remote services
	 * @throws RuntimeException
	 *             if already set. Changing these local parameters is not currently supported but should be possible.
	 */
	public void setRemoteServices(IRemoteServices services) {
		putString(GIT_SERVICES_ID, services.getId());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.rdt.sync.core.serviceproviders.ISyncServiceProvider#
	 * synchronize(org.eclipse.core.resources.IResourceDelta, org.eclipse.core.runtime.IProgressMonitor, boolean)
	 */
	public void synchronize(IResourceDelta delta, IProgressMonitor monitor, EnumSet<SyncFlag> syncFlags) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.GSP_SyncTaskName, 130);
		// On first sync, place .gitignore in directories. This is useful for folders that are already present and thus are never
		// captured by a resource add or change event. (This can happen for projects converted to sync projects.)
		if (!hasBeenSynced) {
			final IProject project = getProject();
			project.accept(new IResourceVisitor() {
				public boolean visit(IResource resource) throws CoreException {
					if (irrelevantPath(resource.getFullPath().toString())) {
						return false;
					}
					if (resource.getType() == IResource.FOLDER) {
						IFile emptyFile = project.getFile(resource.getProjectRelativePath().addTrailingSeparator() + ".gitignore"); //$NON-NLS-1$
						try {
							if (!(emptyFile.exists())) {
								emptyFile.create(new ByteArrayInputStream("".getBytes()), false, null); //$NON-NLS-1$
							}
						} catch (CoreException e) {
							// Nothing to do. Can happen if another thread creates the file between the check and creation.
						}
					}
					return true;
				}
			});
		}
		hasBeenSynced = true;

		// Make a visitor that explores the delta. At the moment, this visitor is responsible for two tasks (the list may grow in
		// the future):
		// 1) Find out if there are any "relevant" resource changes (changes that need to be mirrored remotely)
		// 2) Add an empty ".gitignore" file to new directories so that Git will sync them
		class SyncResourceDeltaVisitor implements IResourceDeltaVisitor {
			private boolean relevantChangeFound = false;

			public boolean visit(IResourceDelta delta) throws CoreException {
				if (irrelevantPath(delta.getFullPath().toString())) {
					return false;
				} else {
					if ((delta.getAffectedChildren().length == 0) && (delta.getFlags() != IResourceDelta.MARKERS)) {
						relevantChangeFound = true;
					}
				}

				// Add .gitignore to empty directories
				if (delta.getResource().getType() == IResource.FOLDER
						&& (delta.getKind() == IResourceDelta.ADDED || delta.getKind() == IResourceDelta.CHANGED)) {
					IFile emptyFile = getProject().getFile(
							delta.getResource().getProjectRelativePath().addTrailingSeparator() + ".gitignore"); //$NON-NLS-1$
					try {
						if (!(emptyFile.exists())) {
							emptyFile.create(new ByteArrayInputStream("".getBytes()), false, null); //$NON-NLS-1$
						}
					} catch (CoreException e) {
						// Nothing to do. Can happen if another thread creates the file between the check and creation.
					}
				}

				return true;
			}

			public boolean isRelevant() {
				return relevantChangeFound;
			}
		}

		// Explore delta only if it is not null
		boolean hasRelevantChangedResources = false;
		if (delta != null) {
			SyncResourceDeltaVisitor visitor = new SyncResourceDeltaVisitor();
			delta.accept(visitor);
			hasRelevantChangedResources = visitor.isRelevant();
		}

		try {
			/*
			 * A synchronize with SyncFlag.FORCE guarantees that both directories are in sync.
			 * 
			 * More precise: it guarantees that all changes written to disk at the moment of the call are guaranteed to be
			 * synchronized between both directories. No guarantees are given for changes occurring during the synchronize call.
			 * 
			 * To satisfy this guarantee, this call needs to make sure that both the current delta and all outstanding sync requests
			 * finish before this call returns.
			 * 
			 * Example: Why sync if current delta is empty? The RemoteMakeBuilder forces a sync before and after building. In some
			 * cases, we want to ensure repos are synchronized regardless of the passed delta, which can be set to null.
			 */
			// TODO: We are not using the individual "sync to local" and "sync to remote" flags yet.
			if (syncFlags.contains(SyncFlag.DISABLE_SYNC)) {
				return;
			}
			if ((syncFlags == SyncFlag.NO_FORCE) && (!(hasRelevantChangedResources))) {
				return;
			}

			int mySyncTaskId;
			synchronized (syncTaskId) {
				syncTaskId++;
				mySyncTaskId = syncTaskId;
				// suggestion for Deltas: add delta to list of deltas
			}

			synchronized (fWaitingThreadsCount) {
				if (fWaitingThreadsCount > 0 && syncFlags == SyncFlag.NO_FORCE) {
					return; // the queued thread will do the work for us. And we don't have to wait because of NO_FORCE
				} else {
					fWaitingThreadsCount++;
				}
			}

			// lock syncLock. interruptible by progress monitor
			try {
				while (!syncLock.tryLock(50, TimeUnit.MILLISECONDS)) {
					if (progress.isCanceled()) {
						throw new CoreException(new Status(IStatus.CANCEL, Activator.PLUGIN_ID, Messages.GitServiceProvider_1));
					}
				}
			} catch (InterruptedException e1) {
				throw new CoreException(new Status(IStatus.CANCEL, Activator.PLUGIN_ID, Messages.GitServiceProvider_2));
			} finally {
				synchronized (fWaitingThreadsCount) {
					fWaitingThreadsCount--;
				}
			}

			try {
				if (mySyncTaskId <= finishedSyncTaskId) { // some other thread has already done the work for us
					return;
				}

				// TODO: Review exception handling
				if (fSyncConnection == null) {
					// Open a remote sync connection
					fSyncConnection = new GitRemoteSyncConnection(this.getRemoteConnection(), this.getProject().getLocation()
							.toString(), this.getLocation(), new FileFilter(), progress);
				}

				// Open remote connection if necessary
				if (this.getRemoteConnection().isOpen() == false) {
					this.getRemoteConnection().open(progress.newChild(10));
				}

				// This synchronization operation will include all tasks up to current syncTaskId
				// syncTaskId can be larger than mySyncTaskId (than we do also the work for other threads)
				// we might synchronize even more than that if a file is already saved but syncTaskId wasn't increased yet
				// thus we cannot guarantee a maximum but we can guarantee syncTaskId as a minimum
				// suggestion for Deltas: make local copy of list of deltas, remove list of deltas
				int willFinishTaskId;
				synchronized (syncTaskId) {
					willFinishTaskId = syncTaskId;
				}

				// Sync local and remote. For now, do both ways each time.
				// TODO: Sync more efficiently and appropriately to the situation.
				fSyncConnection.syncLocalToRemote(progress.newChild(40));
				fSyncConnection.syncRemoteToLocal(progress.newChild(40), true); // Temporarily enable syncing new remote files

				finishedSyncTaskId = willFinishTaskId;
			} catch (final RemoteSyncException e) {
				throw e;
			} catch (RemoteConnectionException e) {
				throw new RemoteSyncException(e);
			} finally {
				syncLock.unlock();
			}

			// Sync successful - re-enable error messages. This is really UI code, but there is no way at the moment to notify UI
			// of a successful sync.
			SyncManager.setShowErrors(getProject(), true);

			IProject project = this.getProject();
			if (project != null) {
				project.refreshLocal(IResource.DEPTH_INFINITE, progress.newChild(20));
			}
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}

	// Paths that the Git sync provider can ignore.
	private boolean irrelevantPath(String path) {
		if (path.endsWith("/" + GitRemoteSyncConnection.gitDir)) { //$NON-NLS-1$
			return true;
		} else if (path.endsWith("/.git")) { //$NON-NLS-1$
			return true;
		} else if (path.endsWith("/.settings")) { //$NON-NLS-1$
			return true;
		} else {
			return false;
		}
	}

	private class FileFilter implements SyncFileFilter {
		public boolean shouldIgnore(String fileName) {
			if (fileName.equals(".cproject") || fileName.equals(".project")) { //$NON-NLS-1$ //$NON-NLS-2$
				return true;
			}

			if (fileName.startsWith(".settings")) { //$NON-NLS-1$
				return true;
			}

			if (fileName.startsWith(GitRemoteSyncConnection.gitDir)) {
				return true;
			}

			if (this.isBinaryFile(fileName)) {
				return true;
			}

			return false;
		}

		private boolean isBinaryFile(String fileName) {
			try {
				ICElement fileElement = CoreModel.getDefault().create(getProject().getFile(fileName));
				if (fileElement == null) {
					return false;
				}
				int resType = fileElement.getElementType();
				if (resType == ICElement.C_BINARY) {
					return true;
				} else {
					return false;
				}
			} catch (NullPointerException e) {
				// CDT throws this exception for files not recognized. For now, be conservative and allow these files.
				return false;
			}
		}
	}
	
	public IFile[] getMergeConflictedFiles() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.rdt.core.serviceproviders.IRemoteExecutionServiceProvider#getConnection()
	 */
	public IRemoteConnection getConnection() {
		return fConnection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.rdt.core.serviceproviders.IRemoteExecutionServiceProvider#getConfigLocation()
	 */
	public String getConfigLocation() {
		return fLocation;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.rdt.core.serviceproviders.IRemoteExecutionServiceProvider#setRemoteToolsConnection()
	 */
	public void setRemoteToolsConnection(IRemoteConnection connection) {
		syncLock.lock();
		try {
			fConnection = connection;
			putString(GIT_CONNECTION_NAME, connection.getName());
			fSyncConnection = null; // get reinitialized by next synchronize call
		} finally {
			syncLock.unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.rdt.core.serviceproviders.IRemoteExecutionServiceProvider#setConfigLocation()
	 */
	public void setConfigLocation(String configLocation) {
		syncLock.lock();
		try {
			fLocation = configLocation;
			putString(GIT_LOCATION, configLocation);
			fSyncConnection = null; // get reinitialized by next synchronize call
		} finally {
			syncLock.unlock();
		}
	}
}
