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
package org.eclipse.ptp.rdt.sync.ui;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ptp.rdt.sync.core.SyncFlag;
import org.eclipse.ptp.rdt.sync.core.SyncManager;
import org.eclipse.ptp.rdt.sync.core.SyncManager.SYNC_MODE;
import org.eclipse.ptp.rdt.sync.ui.messages.Messages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

public class SyncMenuOperation extends AbstractHandler implements IElementUpdater {
	private static final String SYNC_COMMAND_PARAMETER_ID = "org.eclipse.ptp.rdt.sync.ui.syncCommand.syncModeParameter"; //$NON-NLS-1$
	private static final String syncActiveCommand = "sync_active"; //$NON-NLS-1$
	private static final String syncAllCommand = "sync_all"; //$NON-NLS-1$
	private static final String setNoneCommand = "set_none"; //$NON-NLS-1$
	private static final String setActiveCommand = "set_active"; //$NON-NLS-1$
	private static final String setAllCommand = "set_all"; //$NON-NLS-1$
	private static final String syncAutoCommand = "sync_auto"; //$NON-NLS-1$
	private static final String syncMergeCommand = "sync_merge"; //$NON-NLS-1$

	public Object execute(ExecutionEvent event) throws ExecutionException {
		String command = event.getParameter(SYNC_COMMAND_PARAMETER_ID);
		IProject project = getProject();
		if (project == null) {
			RDTSyncUIPlugin.getDefault().logErrorMessage(Messages.SyncMenuOperation_0);
			return null;
		}

		// On sync request, sync regardless of the flags
		try {
			if (command.equals(syncActiveCommand)) {
				SyncManager.sync(null, project, SyncFlag.FORCE, null);
			} else if (command.equals(syncAllCommand)) {
				SyncManager.syncAll(null, project, SyncFlag.FORCE, null);
				// If user switches to active or all, assume the user wants to sync right away
			} else if (command.equals(setActiveCommand)) {
				SyncManager.setSyncMode(project, SYNC_MODE.ACTIVE);
				SyncManager.sync(null, project, SyncFlag.FORCE, null);
			} else if (command.equals(setAllCommand)) {
				SyncManager.setSyncMode(project, SYNC_MODE.ALL);
				SyncManager.syncAll(null, project, SyncFlag.FORCE, null);
			} else if (command.equals(setNoneCommand)) {
				SyncManager.setSyncMode(project, SYNC_MODE.NONE);
			} else if (command.equals(syncAutoCommand)) {
				SyncManager.setSyncAuto(!(SyncManager.getSyncAuto()));
				// If user switches to automatic sync'ing, go ahead and sync based on current setting for project
				if (SyncManager.getSyncAuto()) {
					SYNC_MODE syncMode = SyncManager.getSyncMode(project);
					if (syncMode == SYNC_MODE.ACTIVE) {
						SyncManager.sync(null, project, SyncFlag.FORCE, null);
					} else if (syncMode == SYNC_MODE.ALL) {
						SyncManager.syncAll(null, project, SyncFlag.FORCE, null);
					}
				}
			} else if (command.equals(syncMergeCommand)) {
				new SyncMergeEditor().open();
			}
		} catch (CoreException e) {
			// This should never happen because only a blocking sync can throw a core exception, and all syncs here are non-blocking.
			RDTSyncUIPlugin.getDefault().logErrorMessage(Messages.SyncMenuOperation_1);
		}

		ICommandService service = (ICommandService) HandlerUtil.getActiveWorkbenchWindowChecked(event).getService(
				ICommandService.class);
		service.refreshElements(event.getCommand().getId(), null);

		return null;
	}

	public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {
		String command = (String) parameters.get(SYNC_COMMAND_PARAMETER_ID);
		if (command == null) {
			RDTSyncUIPlugin.getDefault().logErrorMessage(Messages.SyncMenuOperation_2);
			return;
		}

		IProject project = this.getProject();
		if (project == null) {
			RDTSyncUIPlugin.getDefault().logErrorMessage(Messages.SyncMenuOperation_0);
			return;
		}

		SYNC_MODE syncMode = SyncManager.getSyncMode(project);
		if ((command.equals(setActiveCommand) && syncMode == SYNC_MODE.ACTIVE) ||
				(command.equals(setAllCommand) && syncMode == SYNC_MODE.ALL) ||
				(command.equals(setNoneCommand) && syncMode == SYNC_MODE.NONE) ||
				(command.equals(syncAutoCommand) && SyncManager.getSyncAuto())) {
			element.setChecked(true);
		} else {
			element.setChecked(false);
		}
	}
	
	/*
	 * Portions copied from org.eclipse.ptp.services.ui.wizards.setDefaultFromSelection
	 */
	private IProject getProject() {
		IWorkbenchWindow wnd = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage pg = wnd.getActivePage();
		ISelection sel = pg.getSelection();

		if (!(sel instanceof IStructuredSelection)) {
			return null;
		}
		IStructuredSelection selection = (IStructuredSelection) sel;

		Object firstElement = selection.getFirstElement();
		if (!(firstElement instanceof IAdaptable)) {
			return null;
		}
		Object o = ((IAdaptable) firstElement).getAdapter(IResource.class);
		if (o == null) {
			return null;
		}
		IResource resource = (IResource) o;

		return resource.getProject();
	}
}
