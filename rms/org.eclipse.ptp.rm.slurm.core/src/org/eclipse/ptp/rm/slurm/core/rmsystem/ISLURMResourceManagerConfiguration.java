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
 * 	Peichang Shi, National University of Defense Technology
 *  	Jie Jiang, National University of Defense Technology
 *******************************************************************************/
package org.eclipse.ptp.rm.slurm.core.rmsystem;

import org.eclipse.ptp.rm.core.rmsystem.IRemoteResourceManagerConfiguration;

public interface ISLURMResourceManagerConfiguration extends IRemoteResourceManagerConfiguration {

	/**
	 * @return the slurmdArgs
	 */
	public String getSlurmdArgs();

	/**
	 * @return the slurmdPath
	 */
	public String getSlurmdPath();
	
	/**
	 * @return the useDefaults
	 */
	public boolean getUseDefaults();

	/**
	 * @param slurmdArguments
	 *            the slurmdArgs to set
	 */
	public void setSlurmdArgs(String slurmdArgs);
	
	/**
	 * @param slurmdPath
	 *            the slurmdPath to set
	 */
	public void setSlurmdPath(String slurmdPath);

	/**
	 * @param useDefaults
	 *            the useDefaults to set
	 */
	public void setUseDefaults(boolean useDefaults);
}