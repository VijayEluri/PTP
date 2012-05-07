/*******************************************************************************
 * Copyright (c) 2011 University of Illinois All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html 
 * 	
 * Contributors: 
 * 	Albert L. Rossi - design and implementation
 ******************************************************************************/

package org.eclipse.ptp.rm.jaxb.tests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.ptp.core.IPTPLaunchConfigurationConstants;
import org.eclipse.ptp.core.ModelManager;
import org.eclipse.ptp.core.jobs.IJobStatus;
import org.eclipse.ptp.remote.core.IRemoteConnection;
import org.eclipse.ptp.remote.core.IRemoteConnectionManager;
import org.eclipse.ptp.remote.core.IRemoteServices;
import org.eclipse.ptp.remote.core.PTPRemoteCorePlugin;
import org.eclipse.ptp.rm.jaxb.control.JAXBControlConstants;
import org.eclipse.ptp.rm.jaxb.control.JAXBResourceManagerConfiguration;
import org.eclipse.ptp.rm.jaxb.control.JAXBResourceManagerControl;
import org.eclipse.ptp.rm.jaxb.core.IVariableMap;
import org.eclipse.ptp.rm.jaxb.core.JAXBCoreConstants;
import org.eclipse.ptp.rm.jaxb.core.data.PropertyType;
import org.eclipse.ptp.rm.lml.monitor.core.LMLResourceManagerMonitor;
import org.eclipse.ptp.rm.lml_jaxb.core.LMLJAXBResourceManager;
import org.eclipse.ptp.rmsystem.AbstractResourceManagerConfiguration;
import org.eclipse.ptp.rmsystem.ResourceManagerServiceProvider;

public class RMLaunchTest extends TestCase {

	private class TestLaunchConfiguration implements ILaunchConfiguration {

		private final Map<String, Object> store = new TreeMap<String, Object>();

		public boolean contentsEqual(ILaunchConfiguration configuration) {
			return false;
		}

		public ILaunchConfigurationWorkingCopy copy(String name) throws CoreException {
			return null;
		}

		public void delete() throws CoreException {
		}

		public boolean exists() {
			return false;
		}

		@SuppressWarnings("rawtypes")
		public Object getAdapter(Class adapter) {
			return null;
		}

		public boolean getAttribute(String attributeName, boolean defaultValue) throws CoreException {
			Boolean value = (Boolean) store.get(attributeName);
			if (value == null) {
				value = defaultValue;
			}
			return value;
		}

		public int getAttribute(String attributeName, int defaultValue) throws CoreException {
			Integer value = (Integer) store.get(attributeName);
			if (value == null) {
				value = defaultValue;
			}
			return value;
		}

		@SuppressWarnings("rawtypes")
		public List getAttribute(String attributeName, List defaultValue) throws CoreException {
			List value = (List) store.get(attributeName);
			if (value == null) {
				value = defaultValue;
			}
			return value;
		}

		@SuppressWarnings("rawtypes")
		public Map getAttribute(String attributeName, Map defaultValue) throws CoreException {
			Map value = (Map) store.get(attributeName);
			if (value == null) {
				value = defaultValue;
			}
			return value;
		}

		@SuppressWarnings("rawtypes")
		public Set getAttribute(String attributeName, Set defaultValue) throws CoreException {
			Set value = (Set) store.get(attributeName);
			if (value == null) {
				value = defaultValue;
			}
			return value;
		}

		public String getAttribute(String attributeName, String defaultValue) throws CoreException {
			String value = (String) store.get(attributeName);
			if (value == null) {
				value = defaultValue;
			}
			return value;
		}

		@SuppressWarnings("rawtypes")
		public Map getAttributes() throws CoreException {
			return store;
		}

		public String getCategory() throws CoreException {
			return null;
		}

		public IFile getFile() {
			return null;
		}

		public IPath getLocation() {
			return null;
		}

		public IResource[] getMappedResources() throws CoreException {
			return null;
		}

		public String getMemento() throws CoreException {
			return null;
		}

		@SuppressWarnings("rawtypes")
		public Set getModes() throws CoreException {
			return null;
		}

		public String getName() {
			return null;
		}

		@SuppressWarnings("rawtypes")
		public ILaunchDelegate getPreferredDelegate(Set modes) throws CoreException {
			return null;
		}

		public ILaunchConfigurationType getType() throws CoreException {
			return null;
		}

		public ILaunchConfigurationWorkingCopy getWorkingCopy() throws CoreException {
			return null;
		}

		public boolean hasAttribute(String attributeName) throws CoreException {
			return false;
		}

		public boolean isLocal() {
			return false;
		}

		public boolean isMigrationCandidate() throws CoreException {
			return false;
		}

		public boolean isReadOnly() {
			return false;
		}

		public boolean isWorkingCopy() {
			return false;
		}

		public ILaunch launch(String mode, IProgressMonitor monitor) throws CoreException {
			return null;
		}

		public ILaunch launch(String mode, IProgressMonitor monitor, boolean build) throws CoreException {
			return null;
		}

		public ILaunch launch(String mode, IProgressMonitor monitor, boolean build, boolean register) throws CoreException {
			return null;
		}

		public void migrate() throws CoreException {

		}

		public boolean supportsMode(String mode) throws CoreException {
			return false;
		}
	}

	private static final String xml = JAXBCoreConstants.DATA + "tabbed-example.xml"; //$NON-NLS-1$
	private JAXBResourceManagerConfiguration rmConfig;
	private LMLJAXBResourceManager rm;
	private ILaunchConfiguration launchConfig;

	@Override
	public void setUp() {
		/*
		 * You will need to copy all the executables in the org.eclipse.ptp.rm.jaxb.tests/data directory to you home; it seems the
		 * JUnit plugin runner does not actually execute in the directory indicated by the Run Configuration.
		 */
	}

	@Override
	public void tearDown() {
	}

	public void testResourceManager() {
		Job j = new Job("testResourceManager") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					emulateConfigureWizard();
					rm = new LMLJAXBResourceManager(rmConfig, new JAXBResourceManagerControl(rmConfig),
							new LMLResourceManagerMonitor(rmConfig));
					ModelManager.getInstance().addResourceManager(rm);
					rm.start(monitor);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException ignored) {
					}
					emulateLaunchTab();
					String jobId = rm.submitJob(launchConfig, ILaunchManager.RUN_MODE, monitor);
					System.out.println("SUBMITTED: " + jobId); //$NON-NLS-1$
					IJobStatus status = rm.getJobStatus(jobId, null);
					System.out.println("STATUS: " + status.getState()); //$NON-NLS-1$
					if (status != null) {
						status.getStreamsProxy().getOutputStreamMonitor().addListener(new IStreamListener() {
							public void streamAppended(String text, IStreamMonitor monitor) {
								System.out.println(text);
							}
						});
					}
					rm.stop();
				} catch (Throwable t) {
					t.printStackTrace();
					assertNotNull(t);
				}
				return Status.OK_STATUS;
			}
		};
		j.schedule();
		try {
			j.join();
		} catch (InterruptedException ignored) {
		}
	}

	/*
	 * We do here what is done through the wizard.
	 */
	private void emulateConfigureWizard() throws Throwable {
		rmConfig = new JAXBResourceManagerConfiguration(AbstractResourceManagerConfiguration.BASE,
				new ResourceManagerServiceProvider());
		// JAXBRMConfigurationSelectionWizardPage
		rmConfig.setRMConfigurationURL(JAXBTestsPlugin.getURL(xml));
		// JAXBRMControlConfigurationWizardPage
		rmConfig.getResourceManagerData();
		// use remote = local
		IRemoteServices localServices = PTPRemoteCorePlugin.getDefault().getDefaultServices();
		assert (localServices != null);
		IRemoteConnectionManager localConnectionManager = localServices.getConnectionManager();
		assert (localConnectionManager != null);
		IRemoteConnection localConnection = localConnectionManager.getConnection(IRemoteConnectionManager.DEFAULT_CONNECTION_NAME);
		assert (localConnection != null);
		rmConfig.setRemoteServicesId(localServices.getId());
		rmConfig.setConnectionName(localConnection.getName());
	}

	@SuppressWarnings("unchecked")
	private void emulateLaunchTab() throws Throwable {
		launchConfig = new TestLaunchConfiguration();
		Map<Object, Object> env = launchConfig.getAttributes();
		Map<String, String> live = new HashMap<String, String>();
		live.put("FOO_VAR_1", "FOO_VALUE_1"); //$NON-NLS-1$ //$NON-NLS-2$
		live.put("FOO_VAR_2", "FOO_VALUE_2"); //$NON-NLS-1$ //$NON-NLS-2$
		live.put("FOO_VAR_3", "FOO_VALUE_3"); //$NON-NLS-1$ //$NON-NLS-2$
		env.put(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, live);
		env.put(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true);
		env.put(IPTPLaunchConfigurationConstants.ATTR_WORKING_DIR, "/u/ncsa/arossi/test"); //$NON-NLS-1$
		env.put(IPTPLaunchConfigurationConstants.ATTR_EXECUTABLE_PATH, "/u/ncsa/arossi/test/foo"); //$NON-NLS-1$
		env.put(IPTPLaunchConfigurationConstants.ATTR_ARGUMENTS, "-v -f /u/ncsa/arossi/test/data"); //$NON-NLS-1$
		env.put("Job_Name", "TestRMLaunch"); //$NON-NLS-1$ //$NON-NLS-2$
		env.put("Resource_List.nodes", "1:ppn=8"); //$NON-NLS-1$ //$NON-NLS-2$
		env.put("Resource_List.walltime", "00:10:00"); //$NON-NLS-1$ //$NON-NLS-2$
		env.put("export_all", true); //$NON-NLS-1$
		env.put(JAXBControlConstants.MPI_CMD, "mpiexec"); //$NON-NLS-1$ 
		env.put(JAXBControlConstants.MPI_ARGS, "-machinefile $PBS_NODEFILE -np 8"); //$NON-NLS-1$ 
		IVariableMap rmVarMap = rm.getJAXBConfiguration().getRMVariableMap();
		PropertyType queues = (PropertyType) rmVarMap.getVariables().get("available_queues"); //$NON-NLS-1$ 
		if (queues != null) {
			List<String> q = (List<String>) queues.getValue();
			env.put("destination", q.get(0)); //$NON-NLS-1$
		}
		env.put("directory", "/Users/arossi"); //$NON-NLS-1$//$NON-NLS-2$
	}
}
