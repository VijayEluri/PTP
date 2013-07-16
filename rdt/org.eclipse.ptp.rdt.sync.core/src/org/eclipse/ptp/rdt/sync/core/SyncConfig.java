/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    John Eblen - initial implementation
 *******************************************************************************/
package org.eclipse.ptp.rdt.sync.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.ptp.rdt.sync.core.exceptions.MissingConnectionException;
import org.eclipse.ptp.rdt.sync.core.handlers.IMissingConnectionHandler;
import org.eclipse.ptp.remote.core.IRemoteConnection;
import org.eclipse.ptp.remote.core.IRemoteServices;
import org.eclipse.ptp.remote.core.RemoteServices;

/**
 * Class to encapsulate information about syncing a project
 * 
 * @since 3.0
 */
public class SyncConfig implements Comparable<SyncConfig> {
	/**
	 * Utility function to resolve a string based on path variables for a certain project. Unless string is in the form:
	 * ${path_variable:/remainder}, where "path_variable" is a path variable defined for the project, the original string
	 * is returned unchanged.
	 * 
	 * The Eclipse platform should provide a standard mechanism for doing this, but various combinations of URIUtil and
	 * PathVariableManager methods failed.
	 * 
	 * @param project
	 * @param path
	 * @return resolved string
	 */
	public static String resolveString(IProject project, String path) {
		// Check basic syntax
		if (!path.startsWith("${") || !path.endsWith("}")) { //$NON-NLS-1$ //$NON-NLS-2$
			return path;
		}

		String newPath = path.substring(2, path.length() - 1);

		// Extract variable's value
		String variable = newPath.split(":")[0]; //$NON-NLS-1$
		IPathVariableManager pvm = project.getPathVariableManager();
		String value = pvm.getURIValue(variable.toUpperCase()).toString();
		if (value == null) {
			return path;
		}

		// Build and return new path
		value = value.replaceFirst("file:", ""); //$NON-NLS-1$ //$NON-NLS-2$
		if (value.endsWith("/") || value.endsWith("\\")) { //$NON-NLS-1$ //$NON-NLS-2$
			value = value.substring(0, path.length() - 1);
		}
		return newPath.replaceFirst(variable + ":*", value); //$NON-NLS-1$
	}

	private String fName;
	private String fSyncProviderId;
	private String fConnectionName;
	private String fRemoteServicesId;
	private String fLocation;
	private IProject fProject;
	private boolean fSyncOnPreBuild = true;
	private boolean fSyncOnPostBuild = true;
	private boolean fSyncOnSave = true;
	private final Map<String, String> fProperties = new HashMap<String, String>();

	private IRemoteServices fRemoteServices;
	private IRemoteConnection fRemoteConnection;

	/**
	 * Create a new sync configuration. Should not be called by clients directly. Use
	 * {@link SyncConfigManager#newConfig(String, String, IRemoteConnection, String)} instead.
	 * 
	 * @param name
	 *            Name of this configuration. Must be unique per project.
	 */
	public SyncConfig(String name) {
		fName = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(SyncConfig config) {
		return getName().compareTo(config.getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SyncConfig other = (SyncConfig) obj;
		if (!fName.equals(other.fName)) {
			return false;
		}
		return true;
	}

	/**
	 * @return remote services ID
	 */
	public String getConnectionName() {
		return fConnectionName;
	}

	/**
	 * Get the keys for all properties set for this configuration.
	 * 
	 * @return configuration properties keys
	 */
	public String[] getKeys() {
		return fProperties.keySet().toArray(new String[0]);
	}

	/**
	 * Get the remote fLocation
	 * 
	 * @return fLocation
	 */
	public String getLocation() {
		return fLocation;
	}

	/**
	 * Get fLocation (directory), resolved in terms of the passed project
	 * 
	 * @param project
	 * @return fLocation
	 */
	public String getLocation(IProject project) {
		return resolveString(project, fLocation);
	}

	/**
	 * Get the configuration name
	 * 
	 * @return config name
	 */
	public String getName() {
		return fName;
	}

	/**
	 * Get the synchronized project
	 * 
	 * @return
	 */
	public IProject getProject() {
		return fProject;
	}

	/**
	 * Get an arbitrary property for the configuration
	 * 
	 * @param key
	 * @return value
	 */
	public String getProperty(String key) {
		return fProperties.get(key);
	}

	/**
	 * Get remote connection. If connection is missing, this function calls the missing-connection handler. Thus, after catching
	 * the exception, callers can assume user has already been notified and given an opportunity to define the connection. So
	 * callers only need to worry about recovering gracefully.
	 * 
	 * @return remote connection - never null
	 * 
	 * @throws MissingConnectionException
	 *             if no connection with the stored name exist. This can happen for various reasons:
	 *             1) The connection was renamed
	 *             2) The connection was deleted
	 *             3) The connection never existed, such as when a project is imported to a different workspace
	 */
	public IRemoteConnection getRemoteConnection() throws MissingConnectionException {
		if (fRemoteServices == null) {
			fRemoteServices = RemoteServices.getRemoteServices(fRemoteServicesId);
			fRemoteConnection = null;
		}

		if (fRemoteConnection == null) {
			fRemoteConnection = fRemoteServices.getConnectionManager().getConnection(fConnectionName);
			if (fRemoteConnection == null) {
				IMissingConnectionHandler mcHandler = SyncManager.getDefaultMissingConnectionHandler();
				if (mcHandler != null) {
					mcHandler.handle(fRemoteServices, fConnectionName);
					fRemoteConnection = fRemoteServices.getConnectionManager().getConnection(fConnectionName);
				}
			}
		}

		if (fRemoteConnection == null) {
			throw new MissingConnectionException(fConnectionName);
		}

		return fRemoteConnection;
	}

	/**
	 * Get the remote services ID
	 * 
	 * @return remote services ID
	 */
	public String getRemoteServicesId() {
		return fRemoteServicesId;
	}

	/**
	 * Get sync provider ID
	 * 
	 * @return sync provider ID
	 */
	public String getSyncProviderId() {
		return fSyncProviderId;
	}

	@Override
	public int hashCode() {
		return fName.hashCode();
	}

	/**
	 * Check if syncs should occur on post-build
	 * 
	 * @return
	 */
	public boolean isSyncOnPostBuild() {
		return fSyncOnPostBuild;
	}

	/**
	 * Check if syncs should occur on pre-build
	 * 
	 * @return
	 */
	public boolean isSyncOnPreBuild() {
		return fSyncOnPreBuild;
	}

	/**
	 * Check if syncs should occur on saves
	 * 
	 * @return
	 */
	public boolean isSyncOnSave() {
		return fSyncOnSave;
	}

	/**
	 * Set the configuration name
	 * 
	 * @param configName
	 */
	public void setConfigName(String configName) {
		fName = configName;
	}

	/**
	 * Set the remote connection
	 * 
	 * @param connection
	 */
	public void setConnection(IRemoteConnection connection) {
		fRemoteServices = connection.getRemoteServices();
		fRemoteServicesId = connection.getRemoteServices().getId();
		fConnectionName = connection.getName();
		fRemoteConnection = connection;
	}

	/**
	 * Set the connection name
	 * 
	 * @param connectionName
	 */
	public void setConnectionName(String connectionName) {
		fConnectionName = connectionName;
		fRemoteConnection = null;
	}

	/**
	 * Set the sync location
	 * 
	 * @param location
	 */
	public void setLocation(String location) {
		fLocation = location;
	}

	/**
	 * Set the synchronized project
	 * 
	 * @param project
	 */
	public void setProject(IProject project) {
		fProject = project;
	}

	/**
	 * Set an arbitrary property for the configuration
	 * 
	 * @param key
	 * @param value
	 */
	public void setProperty(String key, String value) {
		fProperties.put(key, value);
	}

	/**
	 * Set the remote services ID
	 * 
	 * @param remoteServicesId
	 */
	public void setRemoteServicesId(String remoteServicesId) {
		fRemoteServicesId = remoteServicesId;
		fRemoteServices = null;
	}

	/**
	 * Set the sync on post-build flag
	 * 
	 * @param syncOnPostBuild
	 */
	public void setSyncOnPostBuild(boolean syncOnPostBuild) {
		fSyncOnPostBuild = syncOnPostBuild;
	}

	/**
	 * Set the sync on pre-build flag
	 * 
	 * @param syncOnPreBuild
	 */
	public void setSyncOnPreBuild(boolean syncOnPreBuild) {
		fSyncOnPreBuild = syncOnPreBuild;
	}

	/**
	 * Set the sync on save flag
	 * 
	 * @param syncOnSave
	 */
	public void setSyncOnSave(boolean syncOnSave) {
		fSyncOnSave = syncOnSave;
	}

	/**
	 * Set the sync provider ID
	 * 
	 * @param syncProvider
	 */
	public void setSyncProviderId(String syncProviderId) {
		fSyncProviderId = syncProviderId;
	}
}