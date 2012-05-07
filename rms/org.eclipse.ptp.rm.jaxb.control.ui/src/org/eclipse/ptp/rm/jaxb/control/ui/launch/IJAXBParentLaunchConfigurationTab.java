/*******************************************************************************
 * Copyright (c) 2011 University of Illinois All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html 
 * 	
 * Contributors: 
 * 	Albert L. Rossi - design and implementation
 ******************************************************************************/
package org.eclipse.ptp.rm.jaxb.control.ui.launch;

import org.eclipse.ptp.launch.ui.extensions.IRMLaunchConfigurationDynamicTab;
import org.eclipse.ptp.remote.core.RemoteServicesDelegate;
import org.eclipse.ptp.rm.jaxb.control.IJobController;
import org.eclipse.ptp.rm.jaxb.control.ui.handlers.ValueUpdateHandler;
import org.eclipse.ptp.rm.jaxb.control.ui.variables.LCVariableMap;
import org.eclipse.ptp.rm.jaxb.core.data.ScriptType;

/**
 * JAXB Launch configuration tab
 * 
 */
public interface IJAXBParentLaunchConfigurationTab extends IRMLaunchConfigurationDynamicTab {
	public ValueUpdateHandler getUpdateHandler();

	public IJobController getJobControl();

	public RemoteServicesDelegate getRemoteServicesDelegate();

	public boolean isInitialized();

	public LCVariableMap getVariableMap();

	public boolean hasScript();

	public ScriptType getScript();
}
