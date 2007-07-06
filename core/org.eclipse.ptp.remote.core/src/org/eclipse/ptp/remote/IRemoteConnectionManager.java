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
package org.eclipse.ptp.remote;


public interface IRemoteConnectionManager {
	/**
	 * Get the current remote connection
	 * 
	 * @return remote connection
	 */
	public IRemoteConnection getConnection();
	
	/**
	 * Find a remote connection given its name
	 * 
	 * @param name
	 * @return remote connection
	 */
	public IRemoteConnection getConnection(String name);
	
	/**
	 * Get known connections
	 * 
	 * @return connections that we know about
	 */
	public IRemoteConnection[] getConnections();
	
	/**
	 * Create a new connection. The implementation can chose to do this in any way,
	 * but typically will use a dialog or wizard.
	 * 
	 * @return the newly created connection, or null if creation was cancelled
	 */
	public IRemoteConnection getNewConnection();
	
	/**
	 * Set the remote connection
	 * 
	 * @param conn
	 */
	public void setConnection(IRemoteConnection conn);
}
