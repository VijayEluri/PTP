/*******************************************************************************
 * Copyright (c) 2012 University of Illinois and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 *     Jeff Overbey (Illinois/NCSA) - Design and implementation
 ******************************************************************************/
package org.eclipse.ptp.ems.internal.ui;

import org.eclipse.osgi.util.NLS;

@SuppressWarnings("javadoc")
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ptp.ems.internal.ui.messages"; //$NON-NLS-1$

	public static String EnvConfigurationControl_ManualOverride;
	public static String EnvConfigurationControl_UseEMS;

	public static String EnvManagerChecklist_ConnectButtonLabel;
	public static String EnvManagerChecklist_DetectingEMSPleaseWait;
	public static String EnvManagerChecklist_DetectingRemoteEMS;
	public static String EnvManagerChecklist_EnableColumnText;
	public static String EnvManagerChecklist_EnvManagerInfo;
	public static String EnvManagerChecklist_ModuleNameColumnText;
	public static String EnvManagerChecklist_RemoteEnvironmentIsNotConnected;
	public static String EnvManagerChecklist_NoSupportedEMSInstalled;
	public static String EnvManagerChecklist_NotRemoteSync;
	public static String EnvManagerChecklist_PleaseWaitRetrievingModuleList;
	public static String EnvManagerChecklist_SettingsOnEnvironmentsPageAreAppliedBeforehand;
	public static String EnvManagerChecklist_UpdatingChecklist;

	public static String EnvManagerConfigButton_ConfigureButtonText;
	public static String EnvManagerConfigButton_EnvConfigurationDialogTitle;

	public static String SearchableChecklist_ClearSelection;

	public static String SearchableChecklist_ReloadList;
	public static String SearchableChecklist_SearchBoxLabel;
	public static String SearchableChecklist_SelectDefaults;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
