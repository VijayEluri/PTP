/*******************************************************************************
 * Copyright (c) 2009 School of Computer Science,
 * National University of Defense Technology, P.R.China.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Jie Jiang, National University of Defense Technology
 *******************************************************************************/
package org.eclipse.ptp.rm.slurm.core.rmsystem;

import org.eclipse.ptp.rm.core.rmsystem.AbstractRemoteResourceManagerConfiguration;
import org.eclipse.ptp.services.core.IServiceProvider;

/**
 * Service provider for IBM Parallel Environment
 */
public class SLURMResourceManagerConfiguration extends AbstractRemoteResourceManagerConfiguration implements
		ISLURMResourceManagerConfiguration {
	private static final String TAG_SLURMD_PATH = "slurmdPath"; //$NON-NLS-1$
	private static final String TAG_SLURMD_ARGS = "slurmdArgs"; //$NON-NLS-1$
	private static final String TAG_SLURMD_DEFAULTS = "slurmdDefaults"; //$NON-NLS-1$
	public static final String EMPTY_STRING = ""; //$NON-NLS-1$

	public SLURMResourceManagerConfiguration(String namespace, IServiceProvider provider) {
		super(namespace, provider);
		setDescription("SLURM Resource Manager"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ptp.rm.slurm.core.rmsystem.ISLURMResourceManagerConfiguration
	 * #getSlurmdArgs()
	 */
	public String getSlurmdArgs() {
		return getString(TAG_SLURMD_ARGS, ""); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ptp.rm.slurm.core.rmsystem.ISLURMResourceManagerConfiguration
	 * #getSlurmdPath()
	 */
	public String getSlurmdPath() {
		return "";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ptp.rm.slurm.core.rmsystem.ISLURMResourceManagerConfiguration
	 * #getUseDefaults()
	 */
	public boolean getUseDefaults() {
		return getBoolean(TAG_SLURMD_DEFAULTS, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.services.core.IServiceProvider#isConfigured()
	 */
	@Override
	public boolean isConfigured() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.rmsystem.IResourceManagerConfiguration#
	 * needsDebuggerLaunchHelp()
	 */
	@Override
	public boolean needsDebuggerLaunchHelp() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ptp.rmsystem.IResourceManagerConfiguration#setDefaultNameAndDesc
	 * ()
	 */
	public void setDefaultNameAndDesc() {
		String name = "SLURM"; //$NON-NLS-1$
		String conn = getConnectionName();
		if (conn != null && !conn.equals("")) { //$NON-NLS-1$
			name += "@" + conn; //$NON-NLS-1$
		}
		setName(name);
		setDescription("SLURM Resource Manager"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ptp.rm.slurm.core.rmsystem.ISLURMResourceManagerConfiguration
	 * #setSlurmdArgs(java.lang.String)
	 */
	public void setSlurmdArgs(String slurmdArgs) {
		putString(TAG_SLURMD_ARGS, slurmdArgs);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ptp.rm.slurm.core.rmsystem.ISLURMResourceManagerConfiguration
	 * #setSlurmdPath(java.lang.String)
	 */
	public void setSlurmdPath(String slurmdPath) {
		putString(TAG_SLURMD_PATH, slurmdPath);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ptp.rm.slurm.core.rmsystem.ISLURMResourceManagerConfiguration
	 * #setUseDefaults(boolean)
	 */
	public void setUseDefaults(boolean useDefaults) {
		putBoolean(TAG_SLURMD_DEFAULTS, useDefaults);
	}
}
