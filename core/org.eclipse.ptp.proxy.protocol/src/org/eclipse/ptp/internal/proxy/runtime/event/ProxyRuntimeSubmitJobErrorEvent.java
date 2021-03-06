/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - Initial API and implementation
 *******************************************************************************/

package org.eclipse.ptp.internal.proxy.runtime.event;

import org.eclipse.ptp.proxy.runtime.event.AbstractProxyRuntimeEvent;
import org.eclipse.ptp.proxy.runtime.event.IProxyRuntimeSubmitJobErrorEvent;

public class ProxyRuntimeSubmitJobErrorEvent
		extends AbstractProxyRuntimeEvent
		implements IProxyRuntimeSubmitJobErrorEvent {

	public ProxyRuntimeSubmitJobErrorEvent(int transID, String[] args) {
		super(SUBMITJOB_ERROR, transID, args);
	}
}
