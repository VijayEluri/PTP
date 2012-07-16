/*******************************************************************************
 * Copyright (c) 2012 University of Illinois and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jeff Overbey (Illinois) - Initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.rdt.sync.ui.properties;

import java.net.URI;
import java.util.Set;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.managedbuilder.ui.properties.AbstractSingleBuildPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.ems.core.EnvManagerProjectProperties;
import org.eclipse.ptp.ems.ui.EnvManagerConfigWidget;
import org.eclipse.ptp.ems.ui.IErrorListener;
import org.eclipse.ptp.rdt.sync.core.BuildConfigurationManager;
import org.eclipse.ptp.rdt.sync.core.BuildScenario;
import org.eclipse.ptp.rdt.sync.core.MissingConnectionException;
import org.eclipse.ptp.rdt.sync.core.RDTSyncCorePlugin;
import org.eclipse.ptp.rdt.sync.ui.RDTSyncUIPlugin;
import org.eclipse.ptp.remote.core.IRemoteConnection;
import org.eclipse.swt.widgets.Composite;

/**
 * The Environment Management property page, which is available under the C/C++ Build category for synchronized remote projects.
 * 
 * @author Jeff Overbey
 */
public final class EnvManagerPropertiesPage extends AbstractSingleBuildPage {

	private EnvManagerConfigWidget ui = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.ui.newui.AbstractSinglePage#createWidgets(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createWidgets(Composite parent) {
		final IRemoteConnection remoteConnection = getConnection();

		this.ui = new EnvManagerConfigWidget(parent, remoteConnection);
		this.ui.setErrorListener(new IErrorListener() {
			public void errorRaised(String message) {
				setErrorMessage(message);
			}
			public void errorCleared() {
				setErrorMessage(null);
			}
		});

		this.ui.setUseEMSCheckbox(isEnvConfigSupportEnabled());
		this.ui.setManualConfigCheckbox(isManualConfigEnabled());
		this.ui.setManualConfigText(getManualConfigText());
		this.ui.configurationChanged(getSyncURI(), remoteConnection, computeSelectedItems());
	}

	private IRemoteConnection getConnection() {
		if (getControl().isDisposed()) {
			return null;
		}

		final BuildConfigurationManager bcm = BuildConfigurationManager.getInstance();
		final BuildScenario bs = bcm.getBuildScenarioForBuildConfiguration(getCfg());
		if (bs == null) {
			return null;
		}
		
		try {
			return bs.getRemoteConnection();
		} catch (MissingConnectionException e) {
			return null;
		}
	}

	private boolean isEnvConfigSupportEnabled() {
		try {
			return getProjectProperties().isEnvMgmtEnabled();
		} catch (final Error e) {
			return false;
		}
	}

	private EnvManagerProjectProperties getProjectProperties() {
		try {
			return new EnvManagerProjectProperties(getProject());
		} catch (final Error e) {
			setErrorMessage(e.getClass().getSimpleName() + ": " + e.getLocalizedMessage()); //$NON-NLS-1$
			RDTSyncUIPlugin.log(e);
			throw e;
		}
	}

	private boolean isManualConfigEnabled() {
		try {
			return getProjectProperties().isManualConfigEnabled();
		} catch (final Error e) {
			return false;
		}
	}

	private String getManualConfigText() {
		try {
			return getProjectProperties().getManualConfigText();
		} catch (final Error e) {
			return ""; //$NON-NLS-1$
		}
	}

	private URI getSyncURI() {
		final BuildConfigurationManager bcm = BuildConfigurationManager.getInstance();
		try {
			return bcm.getSyncLocationURI(getCfg(), getCfg().getOwner().getProject());
		} catch (final CoreException e) {
			setErrorMessage(e.getClass().getSimpleName() + ": " + e.getLocalizedMessage()); //$NON-NLS-1$
			RDTSyncUIPlugin.log(e);
			return null;
		}
	}

	private Set<String> computeSelectedItems() {
		try {
			final EnvManagerProjectProperties projectProperties = new EnvManagerProjectProperties(getProject());
			if (projectProperties.getConnectionName().equals(ui.getConnectionName())) {
				return projectProperties.getConfigElements();
			} else {
				// If the stored connection name is different,
				// then the stored list of modules is probably for a different machine,
				// so don't try to select those modules, since they're probably incomplete or invalid for this connection
				return null; // Revert to default selection
			}
		} catch (final Error e) {
			return null; // Revert to default selection
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.ui.newui.AbstractSinglePage#cfgChanged(org.eclipse.cdt.core.settings.model.ICConfigurationDescription)
	 */
	@Override
	protected void cfgChanged(ICConfigurationDescription cfgd) {
		super.cfgChanged(cfgd);
		if (ui != null) {
			IRemoteConnection connection = getConnection();
			ui.configurationChanged(getSyncURI(), connection, computeSelectedItems());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.ui.newui.AbstractSinglePage#performDefaults()
	 */
	@Override
	public void performDefaults() {
		if (ui != null) {
			ui.setUseEMSCheckbox(false);
		}
		super.performDefaults();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.ui.newui.AbstractSinglePage#performApply(org.eclipse.cdt.core.settings.model.ICResourceDescription,
	 * org.eclipse.cdt.core.settings.model.ICResourceDescription)
	 */
	@Override
	protected void performApply(ICResourceDescription src, ICResourceDescription dst) {
		storeProjectProperties();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.ui.newui.AbstractSinglePage#performOk()
	 */
	@Override
	public boolean performOk() {
		storeProjectProperties();
		return super.performOk();
	}

	private void storeProjectProperties() {
		try {
			final EnvManagerProjectProperties projectProperties = new EnvManagerProjectProperties(getProject());
			ui.saveConfiguration(projectProperties);
		} catch (final Error e) {
			RDTSyncCorePlugin.log(e);
			setErrorMessage(e.getClass().getSimpleName() + ": " + e.getLocalizedMessage()); //$NON-NLS-1$
		}
	}
}
