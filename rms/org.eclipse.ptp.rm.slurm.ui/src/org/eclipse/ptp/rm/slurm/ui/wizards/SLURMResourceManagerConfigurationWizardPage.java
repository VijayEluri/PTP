/*******************************************************************************
 * Copyright (c) 2008,2009 School of Computer Science,  
 * National University of Defense Technology, P.R.China
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Peichang Shi <pcmutates@163.com>/<pcshi@nudt.edu.cn>
 *******************************************************************************/
package org.eclipse.ptp.rm.slurm.ui.wizards;

import org.eclipse.ptp.rm.slurm.ui.messages.Messages;
import org.eclipse.ptp.rm.ui.wizards.AbstractRemoteProxyResourceManagerConfigurationWizardPage;
import org.eclipse.ptp.ui.wizards.IRMConfigurationWizard;

public final class SLURMResourceManagerConfigurationWizardPage extends
	AbstractRemoteProxyResourceManagerConfigurationWizardPage {
	
	public SLURMResourceManagerConfigurationWizardPage(IRMConfigurationWizard wizard) {
		super(wizard, Messages.SLURMResourceManagerConfigurationWizardPage_name);
		setTitle(Messages.SLURMResourceManagerConfigurationWizardPage_title);
		setDescription(Messages.SLURMResourceManagerConfigurationWizardPage_description);
	}
}
