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
package org.eclipse.ptp.services.internal.core;

import org.eclipse.ptp.services.core.IServiceProviderDescriptor;

/**
 * @author greg
 *
 */
public class ServiceProviderDescriptor implements IServiceProviderDescriptor {
	private String id;
	private String name;
	private String serviceId;
	
	public ServiceProviderDescriptor(String id, String name, String serviceId) {
		this.id = id;
		this.name = name;
		this.serviceId = serviceId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ptp.services.core.IServiceProviderDescriptor#getId()
	 */
	public String getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ptp.services.core.IServiceProviderDescriptor#getName()
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ptp.services.core.IServiceProviderDescriptor#getServiceId()
	 */
	public String getServiceId() {
		return serviceId;
	}
}
