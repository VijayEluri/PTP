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
package org.eclipse.ptp.rdt.sync.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ptp.rdt.sync.core.messages.Messages;
import org.eclipse.ptp.rdt.sync.core.serviceproviders.ISyncServiceProvider;
import org.eclipse.ptp.rdt.sync.core.services.IRemoteSyncServiceConstants;
import org.eclipse.ptp.services.core.IService;
import org.eclipse.ptp.services.core.IServiceConfiguration;
import org.eclipse.ptp.services.core.IServiceModelManager;
import org.eclipse.ptp.services.core.ServiceModelManager;
import org.osgi.service.prefs.Preferences;

public class SyncManager  {
	// Static class - do not allow creating of instances
	private SyncManager() {
	}

	// ACTIVE: Sync with current active configuration
	// ALL: Sync with all configurations
	// NONE: Do not transfer files but still call sync and do bookkeeping
	// UNAVAILABLE: Do not call sync. (Used internally during project creation and deletion.)
	public static enum SYNC_MODE {
		ACTIVE, ALL, NONE, UNAVAILABLE
	};

	private static final String projectScopeSyncNode = "org.eclipse.ptp.rdt.sync.core"; //$NON-NLS-1$
	private static final String instanceScopeSyncNode = "org.eclipse.ptp.rdt.sync.core"; //$NON-NLS-1$
	private static final String SYNC_MODE_KEY = "sync-mode"; //$NON-NLS-1$
	private static final String SYNC_AUTO_KEY = "sync-auto"; //$NON-NLS-1$
	private static final String SHOW_ERROR_KEY = "show-error"; //$NON-NLS-1$
	private static final Map<IProject, Set<IPath>> fProjectToResolvedFilesMap = Collections
			.synchronizedMap(new HashMap<IProject, Set<IPath>>());
	
	// Sync unavailable by default. Wizards should explicitly set the sync mode once the project is ready.
	private static final SYNC_MODE DEFAULT_SYNC_MODE = SYNC_MODE.UNAVAILABLE;
	private static final boolean DEFAULT_SYNC_AUTO_SETTING = true;
	private static final boolean DEFAULT_SHOW_ERROR_SETTING = true;

	private static class SynchronizeJob extends Job {
		private final IProject fProject;
		private final BuildScenario fBuildScenario;
		private final IResourceDelta fDelta;
		private final ISyncServiceProvider fSyncProvider;
		private final EnumSet<SyncFlag> fSyncFlags;
		private final boolean fResolveAsLocal;
		private final SyncExceptionHandler fSyncExceptionHandler;

		public SynchronizeJob(IProject project, BuildScenario buildScenario, IResourceDelta delta, ISyncServiceProvider provider,
				boolean resolveAsLocal, SyncExceptionHandler seHandler) {
			super(Messages.SyncManager_4);
			fProject = project;
			fBuildScenario = buildScenario;
			fDelta = delta;
			fSyncProvider = provider;
			fSyncFlags = syncFlags;
			fResolveAsLocal = resolveAsLocal;
			fSyncExceptionHandler = seHandler;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			SubMonitor progress = SubMonitor.convert(monitor, 100);
			try {
				if (!fResolveAsLocal) {
					fSyncProvider.synchronize(fDelta, getFileFilter(fProject), progress.newChild(100), fSyncFlags);
				} else {
					fSyncProvider.synchronizeResolveAsLocal(fDelta, getFileFilter(fProject), progress.newChild(100), fSyncFlags);
				}
			} catch (CoreException e) {
				if (fSyncExceptionHandler == null) {
					System.out.println(Messages.SyncManager_8 + e.getLocalizedMessage());
				} else {
					fSyncExceptionHandler.handle(e);
				}
			} finally {
				monitor.done();
			}
			return Status.OK_STATUS;
		}
	};
	
	/**
	 * Return a copy of the project's file filter.
	 * If there are any problems retrieving the filter, the workspace default filter is returned.
	 * Since only a copy is returned, users must execute "saveFileFilter(IProject, SyncFileFilter)" after making changes to have
	 * those changes actually applied.
	 *
	 * @param project cannot be null
	 * @return the file filter. This is never null.
	 */
	public static SyncFileFilter getFileFilter(IProject project) {
		if (project == null) {
			throw new NullPointerException();
		}
		IScopeContext context = new ProjectScope(project);
		Preferences node = context.getNode(projectScopeSyncNode);
		if (node == null) {
			RDTSyncCorePlugin.log(Messages.SyncManager_3);
			return SyncManager.getDefaultFileFilter();
		}

		SyncFileFilter filter = SyncFileFilter.loadFilter(node);
		if (filter == null) {
			return SyncManager.getDefaultFileFilter();
		} else {
			return filter;
		}
	}
	
	/**
	 * Return a copy of the default file filter
	 * If there are any problems retrieving the filter, the built-in default filter is returned.
	 * @return the file filter. This is never null.
	 */
	public static SyncFileFilter getDefaultFileFilter() {
		IScopeContext context = InstanceScope.INSTANCE;
		Preferences node = context.getNode(instanceScopeSyncNode);
		if (node == null) {
			RDTSyncCorePlugin.log(Messages.SyncManager_6);
			return SyncFileFilter.createBuiltInDefaultFilter();
		}
		
		SyncFileFilter filter = SyncFileFilter.loadFilter(node);
		if (filter == null) {
			return SyncFileFilter.createBuiltInDefaultFilter();
		} else {
			return filter;
		}
	}

	/**
	 * Get sync mode for a project
	 * 
	 * @param project cannot be null
	 * @return sync mode. This is never null.
	 */
	public static SYNC_MODE getSyncMode(IProject project) {
		if (project == null) {
			throw new NullPointerException();
		}
		IScopeContext context = new ProjectScope(project);
		Preferences node = context.getNode(projectScopeSyncNode);
		if (node == null) {
			RDTSyncCorePlugin.log(Messages.SyncManager_3);
			return DEFAULT_SYNC_MODE;
		}
		
		return SYNC_MODE.valueOf(node.get(SYNC_MODE_KEY, DEFAULT_SYNC_MODE.name()));
	}

	/**
	 * Should sync'ing be done automatically?
	 * 
	 * @return if sync'ing should be done automatically
	 */
	public static boolean getSyncAuto() {
		IScopeContext context = InstanceScope.INSTANCE;
		Preferences node = context.getNode(instanceScopeSyncNode);
		if (node == null) {
			RDTSyncCorePlugin.log(Messages.SyncManager_6);
			return DEFAULT_SYNC_AUTO_SETTING;
		}
		return node.getBoolean(SYNC_AUTO_KEY, DEFAULT_SYNC_AUTO_SETTING);
	}
	
	/**
	 * Should error messages be displayed for the given project?
	 * 
	 * @param project
	 * @return whether error messages should be displayed.
	 */
	public static boolean getShowErrors(IProject project) {
		if (project == null) {
			throw new NullPointerException();
		}
		IScopeContext context = new ProjectScope(project);
		Preferences node = context.getNode(projectScopeSyncNode);
		if (node == null) {
			RDTSyncCorePlugin.log(Messages.SyncManager_3);
			return DEFAULT_SHOW_ERROR_SETTING;
		}
		return node.getBoolean(SHOW_ERROR_KEY, DEFAULT_SHOW_ERROR_SETTING);
	}
	
	/**
	 * Is the given path for the given project resolved?
	 *
	 * @param project
	 * @param path
	 * @return whether path is resolved
	 */
	public static boolean getResolved(IProject project, IPath path) {
		if (project == null || path == null) {
			throw new NullPointerException();
		}
		if (!(fProjectToResolvedFilesMap.containsKey(project))) {
			fProjectToResolvedFilesMap.put(project, new HashSet<IPath>());
			try {
				saveConfigurationData();
			} catch (IOException e) {
				RDTSyncCorePlugin.log(Messages.SyncManager_2, e);
			}
		}
		return fProjectToResolvedFilesMap.get(project).contains(path);
	}

	/**
	 * Set sync mode for a project
	 *
	 * @param project
	 * @param mode
	 */
	public static void setSyncMode(IProject project, SYNC_MODE mode) {
		if (project == null || mode == null) {
			throw new NullPointerException();
		}

		IScopeContext context = new ProjectScope(project);
		Preferences node = context.getNode(projectScopeSyncNode);
		if (node == null) {
			RDTSyncCorePlugin.log(Messages.SyncManager_3);
			return;
		}

		if (mode == DEFAULT_SYNC_MODE) {
			node.remove(SYNC_MODE_KEY);
		} else {
			node.put(SYNC_MODE_KEY, mode.name());
		}

		BuildConfigurationManager.flushNode(node);
	}

	/**
	 * Turn automatic sync'ing on or off
	 * 
	 * @param isSyncAutomatic
	 */
	public static void setSyncAuto(boolean isSyncAutomatic) {
		IScopeContext context = InstanceScope.INSTANCE;
		Preferences node = context.getNode(instanceScopeSyncNode);
		if (node == null) {
			RDTSyncCorePlugin.log(Messages.SyncManager_6);
			return;
		}

		if (isSyncAutomatic == DEFAULT_SYNC_AUTO_SETTING) {
			node.remove(SYNC_AUTO_KEY);
		} else {
			node.putBoolean(SYNC_AUTO_KEY, isSyncAutomatic);
		}

		BuildConfigurationManager.flushNode(node);
	}
	
	/**
	 * Set whether error messages should be displayed
	 *
	 * @param project
	 * @param shouldBeDisplayed
	 */
	public static void setShowErrors(IProject project, boolean shouldBeDisplayed) {
		if (project == null) {
			throw new NullPointerException();
		}

		IScopeContext context = new ProjectScope(project);
		Preferences node = context.getNode(projectScopeSyncNode);
		if (node == null) {
			RDTSyncCorePlugin.log(Messages.SyncManager_3);
			return;
		}

		if (shouldBeDisplayed == DEFAULT_SHOW_ERROR_SETTING) {
			node.remove(SHOW_ERROR_KEY);
		} else {
			node.putBoolean(SHOW_ERROR_KEY, shouldBeDisplayed);
		}

		BuildConfigurationManager.flushNode(node);
	}

	/**
	 * Save a new file filter for a project.
	 * Use this in conjunction with "getFileFilter(IProject)" to modify the current file filtering for a project.
	 *
	 * @param project cannot be null
	 * @param filter cannot be null
	 */
	public static void saveFileFilter(IProject project, SyncFileFilter filter) {
		if (project == null || filter == null) {
			throw new NullPointerException();
		}

		IScopeContext context = new ProjectScope(project);
		Preferences node = context.getNode(projectScopeSyncNode);
		if (node == null) {
			RDTSyncCorePlugin.log(Messages.SyncManager_3);
			return;
		}

		filter.saveFilter(node);

		BuildConfigurationManager.flushNode(node);
	}

	/**
	 * Set the given path as resolved or unresolved for the given project
	 *
	 * @param project
	 * @param path
	 */
	public static void setResolved(IProject project, IPath path, boolean isResolved) {
		if (project == null || path == null) {
			throw new NullPointerException();
		}
		if (!(fProjectToResolvedFilesMap.containsKey(project))) {
			fProjectToResolvedFilesMap.put(project, new HashSet<IPath>());
		}
		if (isResolved) {
			fProjectToResolvedFilesMap.get(project).add(path);
		} else {
			fProjectToResolvedFilesMap.get(project).remove(path);
		}
	}

	/**
	 * Save a new default file filter.
	 * Use this in conjunction with "getDefaultFileFilter()" to modify the default filter.
	 * @param filter cannot be null
	 */
	public static void saveDefaultFileFilter(SyncFileFilter filter) {
		if (filter == null) {
			throw new NullPointerException();
		}
		
		IScopeContext context = InstanceScope.INSTANCE;
		Preferences node = context.getNode(instanceScopeSyncNode);
		if (node == null) {
			RDTSyncCorePlugin.log(Messages.SyncManager_6);
			return;
		}

		filter.saveFilter(node);

		BuildConfigurationManager.flushNode(node);
	}

	/**
	 * Invoke sync for active (default) configuration on a project
	 * 
	 * @param delta
	 *            project delta
	 * @param project
	 *            project to sync
	 * @param syncFlags
	 *            sync flags
	 * @param seHandler
	 * 			  logic to handle exceptions
	 * @return the scheduled sync job
	 * @throws CoreException 
	 */
	public static Job sync(IResourceDelta delta, IProject project, EnumSet<SyncFlag> syncFlags, SyncExceptionHandler seHandler)
			throws CoreException {
		return sync(delta, project, syncFlags, false, false, seHandler, null);
	}
	
	/**
	 * Invoke a "resolve as local" sync for active (default) configuration on a project
	 * Warning: This is a dangerous operation as it will destroy all remote differences. The UI should provide precautions
	 * to prevent users from invoking this type of sync without first understanding the implications.
	 *
	 * @param delta
	 *            project delta
	 * @param project
	 *            project to sync
	 * @param syncFlags
	 *            sync flags
	 * @param seHandler
	 * 			  logic to handle exceptions
	 * @return the scheduled sync job
	 * @throws CoreException 
	 */
	public static Job syncResolveAsLocal(IResourceDelta delta, IProject project, EnumSet<SyncFlag> syncFlags,
			SyncExceptionHandler seHandler) throws CoreException {
		return sync(delta, project, syncFlags, false, true, seHandler, null);
	}
	
	/**
	 * Invoke sync and block until sync finishes. This does not spawn another thread and no locking of resources is done.
	 * 
	 * @param delta
	 *            project delta
	 * @param project
	 *            project to sync
	 * @param syncFlags
	 *            sync flags
	 * @return the scheduled sync job
	 * @throws CoreException
	 * 			  on problems sync'ing
	 */
	public static Job syncBlocking(IResourceDelta delta, IProject project, EnumSet<SyncFlag> syncFlags, IProgressMonitor monitor)
			throws CoreException {
		return sync(delta, project, syncFlags, true, false, null, monitor);
	}
	
	private static Job sync(IResourceDelta delta, IProject project, EnumSet<SyncFlag> syncFlags, boolean isBlocking,
			boolean resolveAsLocal, SyncExceptionHandler seHandler, IProgressMonitor monitor) throws CoreException {
		BuildConfigurationManager bcm = BuildConfigurationManager.getInstance();
		if (!(bcm.isInitialized(project))) {
			return null;
		}

		IConfiguration[] buildConfigurations = new IConfiguration[1];
		buildConfigurations[0] = ManagedBuildManager.getBuildInfo(project).getDefaultConfiguration();
		Job[] syncJobs = scheduleSyncJobs(delta, project, syncFlags, buildConfigurations, isBlocking, resolveAsLocal, seHandler,
				monitor);
		return syncJobs[0];
	}

	/**
	 * Invoke sync for all configurations on a project.
	 * Note that there is no syncAllBlocking, because it was not needed but would be easy to add.
	 * 
	 * @param delta
	 *            project delta
	 * @param project
	 *            project to sync
	 * @param syncFlags
	 *            sync flags
	 * @param seHandler
	 *			  logic to handle exceptions
	 * @return array of sync jobs scheduled
	 * @throws CoreException
	 * 			  on problems sync'ing
	 */
	public static Job[] syncAll(IResourceDelta delta, IProject project, EnumSet<SyncFlag> syncFlags, SyncExceptionHandler seHandler)
			throws CoreException {
		BuildConfigurationManager bcm = BuildConfigurationManager.getInstance();
		if (!(bcm.isInitialized(project))) {
			return new Job[0];
		}

		return scheduleSyncJobs(delta, project, syncFlags, ManagedBuildManager.getBuildInfo(project).getManagedProject()
				.getConfigurations(), false, false, seHandler, null);
	}

	// Note that the monitor is ignored for non-blocking jobs since SynchronizeJob creates its own monitor
	private static Job[] scheduleSyncJobs(IResourceDelta delta, IProject project, EnumSet<SyncFlag> syncFlags,
			IConfiguration[] buildConfigurations, boolean isBlocking, boolean resolveAsLocal, SyncExceptionHandler seHandler,
			IProgressMonitor monitor)
			throws CoreException {
		int jobNum = 0;
		Job[] syncJobs = new Job[buildConfigurations.length];
		for (IConfiguration buildConfig : buildConfigurations) {
			SynchronizeJob job = null;
			BuildScenario buildScenario = BuildConfigurationManager.getInstance().getBuildScenarioForBuildConfiguration(buildConfig);
			ISyncServiceProvider provider = (ISyncServiceProvider) SyncManager.getSyncProvider(buildConfig);
			if (provider != null) {
				if (isBlocking) {
					if (!resolveAsLocal) {
					} else {
						provider.synchronizeResolveAsLocal(delta, getFileFilter(project), monitor, syncFlags);
					}
				} else {
						job = new SynchronizeJob(delta, project, provider, syncFlags, resolveAsLocal, seHandler);
					job.schedule();
				}
			}

			// Each build configuration is matched with a job, which may be null if a job could not be created.
			syncJobs[jobNum] = job;
			jobNum++;
		}

		return syncJobs;
	}
	
	/**
	 * Get the sync service provider for the given project's current active configuration. 
	 *
	 * @param project
	 * @return sync service provider or null if provider cannot be found. (Logs error message in that case.)
	 */
	public static ISyncServiceProvider getSyncProvider(IProject project) {
		IConfiguration config = ManagedBuildManager.getBuildInfo(project).getDefaultConfiguration();
		return SyncManager.getSyncProvider(config);
	}
	
	/**
	 * Get the sync service provider for the given build configuration.
	 *
	 * @param config
	 * @return sync service provider or null if provider cannot be found. (Logs error message in that case.)
	 */
	public static ISyncServiceProvider getSyncProvider(IConfiguration config) {
		ISyncServiceProvider provider = null;
		BuildConfigurationManager bcm = BuildConfigurationManager.getInstance();
		IServiceConfiguration serviceConfig = bcm.getConfigurationForBuildConfiguration(config);
		if (serviceConfig != null) {
			IServiceModelManager serviceModel = ServiceModelManager.getInstance();
			IService syncService = serviceModel.getService(IRemoteSyncServiceConstants.SERVICE_SYNC);
			provider = (ISyncServiceProvider) serviceConfig.getServiceProvider(syncService);
		}
		
		return provider;
	}
}
