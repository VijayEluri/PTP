/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     John Eblen - Oak Ridge National Laboratory - change to use sync manager
 *                  and move to ui package
 *******************************************************************************/
package org.eclipse.ptp.rdt.sync.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.rdt.sync.core.SyncFlag;
import org.eclipse.ptp.rdt.sync.core.SyncManager;
import org.eclipse.ptp.rdt.sync.ui.messages.Messages;
import org.eclipse.ptp.rdt.sync.core.SyncManager.SYNC_MODE;
import org.eclipse.ptp.rdt.sync.core.resources.RemoteSyncNature;

public class ResourceChangeListener {
	private ResourceChangeListener() {
	}

	public static void startListening() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener, IResourceChangeEvent.POST_CHANGE |
				IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.POST_BUILD);
	}

	public static void stopListening() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
	}

	private static IResourceChangeListener resourceListener = new IResourceChangeListener() {
		public void resourceChanged(IResourceChangeEvent event) {
			// RDTSyncCorePlugin.log("Event type of " + event.getType()); //$NON-NLS-1$
			// Turn off sync'ing for a project before deleting it and close repository - see bug 360170
			// Note that event.getDelta() returns null, so this event cannot be handled inside the loop below.
			if (event.getType() == IResourceChangeEvent.PRE_DELETE) {
				IProject project = (IProject) event.getResource();
				if (!RemoteSyncNature.hasNature(project)) {
					return;
				}
				SyncManager.setSyncMode(project, SYNC_MODE.UNAVAILABLE);
				return;
			}
			for (IResourceDelta delta : event.getDelta().getAffectedChildren()) {
				IProject project = delta.getResource().getProject();
				if (project == null) {
					return;
				}
				if (RemoteSyncNature.hasNature(project)) {
					SYNC_MODE syncMode = SyncManager.getSyncMode(project);
					boolean syncOn = true;
					if (!(SyncManager.getSyncAuto()) || syncMode == SYNC_MODE.NONE) {
						syncOn = false;
					}
					try {
						// Post-build event
						// Force a sync in order to download any new remote files but no need to sync if sync'ing is disabled.
						// Ignore auto builds, which are triggered for every resource change.
						if (event.getType() == IResourceChangeEvent.POST_BUILD &&
								event.getBuildKind() != IncrementalProjectBuilder.AUTO_BUILD) {
							if (!syncOn || syncMode == SYNC_MODE.UNAVAILABLE) {
								continue;
							} else if (syncMode == SYNC_MODE.ALL) {
								SyncManager.syncAll(null, project, SyncFlag.FORCE, new CommonSyncExceptionHandler(true, true));
							} else if (syncMode == SYNC_MODE.ACTIVE) {
								SyncManager.sync(null, project, SyncFlag.FORCE, new CommonSyncExceptionHandler(true, true));
							}
						}
						// Post-change event
						// Sync on all CHANGED events
						else if (delta.getKind() == IResourceDelta.CHANGED) {
							// Do a non-forced sync to update any changes reported in delta. Sync'ing is necessary even if user has
							// turned it off. This allows for some bookkeeping but no files are transferred.
							if (syncMode == SYNC_MODE.UNAVAILABLE) {
								continue;
							} else if (!syncOn) {
								SyncManager.sync(delta, project, SyncFlag.NO_SYNC, null);
							} else if (syncMode == SYNC_MODE.ALL) {
								SyncManager.syncAll(delta, project, SyncFlag.NO_FORCE, new CommonSyncExceptionHandler(true, false));
							} else if (syncMode == SYNC_MODE.ACTIVE) {
								SyncManager.sync(delta, project, SyncFlag.NO_FORCE, new CommonSyncExceptionHandler(true, false));
							}
						}
					} catch (CoreException e){
						// This should never happen because only a blocking sync can throw a core exception, and all syncs here are non-blocking.
						RDTSyncUIPlugin.log(Messages.ResourceChangeListener_0, e);
					}
				}
			}
		}
	};
}