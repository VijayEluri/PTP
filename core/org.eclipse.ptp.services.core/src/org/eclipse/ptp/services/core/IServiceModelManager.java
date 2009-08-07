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
package org.eclipse.ptp.services.core;

import java.util.Set;

import org.eclipse.core.resources.IProject;

public interface IServiceModelManager {
	/**
	 * Get the configuration that is currently active for the project. Each project has
	 * exactly one active configuration, which describes the mapping from services to
	 * service providers. By default, the first configuration created for a project will 
	 * be the active configuration for that project.
	 * 
	 * @param project project for which the configuration will be obtained
	 * @return the service configuration for this project
	 * 
	 * @throws NullPointerException if project is null
	 * @throws ProjectNotConfiguredException if the project has not been configured
	 */
	public IServiceConfiguration getActiveConfiguration(IProject project);
	
	/**
	 * Get the named configuration for this project.
	 * 
	 * @param project project for which the configuration will be obtained
	 * @param name name of the configuration
	 * @return the service configuration or null if no configurations with the supplied name exist
	 * 
	 * @throws NullPointerException if project is null
	 * @throws ProjectNotConfiguredException if the project has not been configured
	 */
	public IServiceConfiguration getConfiguration(IProject project, String name);
	
	/**
	 * Get the configuration with the specified ID.
	 * 
	 * @param id ID of the configuration
	 * @return the service configuration or null if no configurations with the supplied ID exist
	 */
	public IServiceConfiguration getConfiguration(String id);
	
	/**
	 * Get all configurations available in the workspace.
	 * 
	 * @return all configurations that could be found, or an empty set
	 */
	public Set<IServiceConfiguration> getConfigurations();
	
	/**
	 * Get all the configurations that are known by the project
	 * 
	 * @param project project containing the configurations
	 * @return set of configurations known by the project
	 * 
	 * @throws NullPointerException if project is null
	 * @throws ProjectNotConfiguredException if the project has not been configured
	 */
	public Set<IServiceConfiguration> getConfigurations(IProject project);
	
	/**
	 * Retrieves the service corresponding to a given id.
	 * 
	 * @param id The unique id of the service to retrieve.
	 * @return IService or null
	 */
	public IService getService(String id);
	
	/**
	 * Return a new service provider instance.
	 * 
	 * @param desc extension description
	 * @return service provider
	 */
	public IServiceProvider getServiceProvider(IServiceProviderDescriptor desc);
	
	/**
	 * Get all the services that have been registered with the system.
	 * 
	 * @return a set of all the services
	 */
	public Set<IService> getServices();
	
	/**
	 * Get all the services that are used by a particular project.
	 * 
	 * @param project project using the services
	 * @return set of services
	 * 
	 * @throws NullPointerException if project is null
	 * @throws ProjectNotConfiguredException if the project has not been configured
	 */
	public Set<IService> getServices(IProject project);
	
	/**
	 * Get all the services that are associated with a project nature.
	 * 
	 * @param nature project nature
	 * @return set of services or null
	 */
	public Set<IService> getServices(String natureID);
	
	/**
	 * Returns true if the given project has a configuration.
	 * 
	 */
	public boolean isConfigured(IProject project);
	
	/**
	 * Obtain a new service configuration with name 'name'. The name
	 * does not need to be unique.
	 * 
	 * @param name name of service configuration
	 * @return new service configuration
	 */
	public IServiceConfiguration newServiceConfiguration(String name);
	
	/**
	 * Removes all the configurations and services associated to the given project.
	 * If the project has not been configured then this method does nothing.
	 * 
	 * @throws NullPointerException if project is null
	 */
	public void remove(IProject project);
	
	
	/**
	 * Removes the service configuration.
	 * 
	 * @param conf the configuration
	 */
	public void remove(IServiceConfiguration conf);
	
	/**
	 * TODO What happens if you try to remove the active configuration?
	 * TODO What happens if there are no configurations left after removing the given configuration?
	 * 
	 * Remove the service configuration from a project.
	 * If the configuration was not set up on the project then this method
	 * does nothing.
	 * 
	 * @param project the project
	 * @param conf the configuration
	 * 
	 * @throws NullPointerException if project or conf is null
	 * @throws ProjectNotConfiguredException if the project has not been configured
	 */
	public void removeConfiguration(IProject project, IServiceConfiguration conf);

	/**
	 * Set the active configuration for a project. By default, the first configuration created
	 * for a project will be the active configuration for that project.
	 * 
	 * @param project project for which the configuration will be obtained
	 * @param configuration configuration to set as active for this project
	 * 
	 * @throws NullPointerException if project or configuration is null
	 * @throws ProjectNotConfiguredException if the project has not been configured yet
	 * @throws IllegalArgumentException if the configuration was not part of the project
	 */
	public void setActiveConfiguration(IProject project, IServiceConfiguration configuration);

	/**
	 * Associate the service configuration with a project. A project can have multiple
	 * service configurations. The service configuration will become the active
	 * configuration for the project.
	 * 
	 * @param project the project
	 * @param conf the configuration
	 * 
	 * @throws NullPointerException if project or conf is null
	 */
	public void setConfiguration(IProject project, IServiceConfiguration conf);

}
