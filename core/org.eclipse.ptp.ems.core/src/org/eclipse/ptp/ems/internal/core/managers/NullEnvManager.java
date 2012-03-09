/*******************************************************************************
 * Copyright (c) 2012 University of Illinois and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jeff Overbey (Illinois) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.ems.internal.core.managers;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.ptp.ems.core.IEnvManager;
import org.eclipse.ptp.ems.core.IEnvManagerConfig;
import org.eclipse.ptp.ems.internal.core.Messages;

/**
 * An {@link IEnvManager} which represents the absence of a supported environment management system (a Null Object).
 * 
 * @author Jeff Overbey
 */
public final class NullEnvManager extends AbstractEnvManager {

	@Override
	public String getName() {
		return Messages.NullEnvManager_NoSupportedEMSFound;
	}

	@Override
	public Set<String> determineAvailableElements() {
		return Collections.<String> emptySet();
	}

	@Override
	public Set<String> determineDefaultElements() {
		return Collections.<String> emptySet();
	}

	@Override
	public boolean checkForCompatibleInstallation() {
		return true;
	}

	@Override
	public String getDescription() {
		return Messages.NullEnvManager_NoSupportedEMSFound;
	}

	@Override
	public String getInstructions() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public String getBashConcatenation(String separator, boolean echo, IEnvManagerConfig config, String commandToExecuteAfterward) {
		return commandToExecuteAfterward == null ? "" : commandToExecuteAfterward; //$NON-NLS-1$
	}

	@Override
	protected List<String> getInitialBashCommands(boolean echo) {
		return Collections.emptyList();
	}

	@Override
	protected List<String> getBashCommand(boolean echo, String item) {
		return Collections.emptyList();
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		} else {
			return o instanceof NullEnvManager;
		}
	}
}
