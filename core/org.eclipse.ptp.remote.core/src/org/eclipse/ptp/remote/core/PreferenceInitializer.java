/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM - Initial API and implementation
 *******************************************************************************/

package org.eclipse.ptp.remote.core;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

/**
 * Class used to initialize default preference values.
 * 
 * @since 6.0
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		Preferences.setDefaultString(PTPRemoteCorePlugin.getUniqueIdentifier(), IRemotePreferenceConstants.PREF_REMOTE_SERVICES_ID,
				IRemotePreferenceConstants.REMOTE_TOOLS_REMOTE_SERVICES_ID);
	}
}
