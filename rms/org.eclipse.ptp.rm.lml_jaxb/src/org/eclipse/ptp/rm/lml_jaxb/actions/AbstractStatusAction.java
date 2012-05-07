/*******************************************************************************
 * Copyright (c) 2011 University of Illinois All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html 
 * 	
 * Contributors: 
 * 	Albert L. Rossi - design and implementation
 ******************************************************************************/
package org.eclipse.ptp.rm.lml_jaxb.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ptp.core.ModelManager;
import org.eclipse.ptp.core.PTPCorePlugin;
import org.eclipse.ptp.rm.jaxb.core.IJAXBResourceManager;
import org.eclipse.ptp.rm.jaxb.core.IJAXBResourceManagerConfiguration;
import org.eclipse.ptp.rm.jaxb.core.data.ControlType;
import org.eclipse.ptp.rm.jaxb.core.data.ResourceManagerData;
import org.eclipse.ptp.rm.lml.core.JobStatusData;
import org.eclipse.ptp.rm.lml.internal.core.model.Row;
import org.eclipse.ptp.rm.lml.ui.views.TableView;
import org.eclipse.ptp.rmsystem.IResourceManager;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Base class for actions on the job status object.
 * 
 * @author arossi
 * 
 */
public abstract class AbstractStatusAction implements IObjectActionDelegate {
	protected static final String JOB_STATUS = "get-job-status";//$NON-NLS-1$
	protected static final String COSP = ": ";//$NON-NLS-1$

	protected List<Row> selected;
	protected TableView view;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action .IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection.isEmpty()) {
			action.setEnabled(false);
			return;
		}
		List<?> list = ((IStructuredSelection) selection).toList();
		selected = new ArrayList<Row>();
		for (Object o : list) {
			selected.add((Row) o);
		}
		validate(action);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface. action.IAction, org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		view = (TableView) targetPart;
	}

	/**
	 * Enables the action.
	 * 
	 * @param action
	 */
	protected abstract void validate(IAction action);

	/**
	 * Checks the JAXB data tree to see if the operation is implemented.
	 * 
	 * @param status
	 * @param operation
	 * @return
	 */
	protected static boolean operationSupported(JobStatusData status, String operation, IViewPart targetPart) {
		IJAXBResourceManager rm = (IJAXBResourceManager) ModelManager.getInstance().getResourceManagerFromUniqueName(
				status.getControlId());
		if (rm == null) {
			return false;
		}
		if (!IResourceManager.STARTED_STATE.equals(rm.getState())) {
			return false;
		}
		IJAXBResourceManagerConfiguration config = (IJAXBResourceManagerConfiguration) rm.getConfiguration();
		if (config == null) {
			return false;
		}

		try {
			ResourceManagerData data = config.getResourceManagerData();
			if (data != null) {
				ControlType control = data.getControlData();
				if (operation.equals(JOB_STATUS)) {
					return control.getGetJobStatus() != null;
				}
				if (operation.equals(IResourceManager.HOLD_OPERATION)) {
					return control.getHoldJob() != null;
				}
				if (operation.equals(IResourceManager.RELEASE_OPERATION)) {
					return control.getReleaseJob() != null;
				}
				if (operation.equals(IResourceManager.RESUME_OPERATION)) {
					return control.getResumeJob() != null;
				}
				if (operation.equals(IResourceManager.SUSPEND_OPERATION)) {
					return control.getSuspendJob() != null;
				}
				if (operation.equals(IResourceManager.TERMINATE_OPERATION)) {
					return control.getTerminateJob() != null;
				}
			}
		} catch (Throwable t) {
			PTPCorePlugin.log(t);
		}
		return false;
	}
}
