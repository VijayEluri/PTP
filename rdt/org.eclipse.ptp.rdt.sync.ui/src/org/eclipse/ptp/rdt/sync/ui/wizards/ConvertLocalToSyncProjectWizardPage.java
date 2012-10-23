/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - Initial API and implementation
 * John Eblen - Alter to support changes to synchronized projects
 *******************************************************************************/

package org.eclipse.ptp.rdt.sync.ui.wizards;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.internal.core.Configuration;
import org.eclipse.cdt.ui.newui.PageLayout;
import org.eclipse.cdt.ui.wizards.conversion.ConvertProjectWizardPage;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.ptp.rdt.core.resources.RemoteNature;
import org.eclipse.ptp.rdt.core.services.IRDTServiceConstants;
import org.eclipse.ptp.rdt.sync.core.BuildConfigurationManager;
import org.eclipse.ptp.rdt.sync.core.BuildScenario;
import org.eclipse.ptp.rdt.sync.core.SyncFileFilter;
import org.eclipse.ptp.rdt.sync.core.SyncFlag;
import org.eclipse.ptp.rdt.sync.core.SyncManager;
import org.eclipse.ptp.rdt.sync.core.resources.RemoteSyncNature;
import org.eclipse.ptp.rdt.sync.core.serviceproviders.ISyncServiceProvider;
import org.eclipse.ptp.rdt.sync.core.serviceproviders.SyncBuildServiceProvider;
import org.eclipse.ptp.rdt.sync.core.services.IRemoteSyncServiceConstants;
import org.eclipse.ptp.rdt.sync.ui.CommonSyncExceptionHandler;
import org.eclipse.ptp.rdt.sync.ui.ISynchronizeParticipant;
import org.eclipse.ptp.rdt.sync.ui.ISynchronizeParticipantDescriptor;
import org.eclipse.ptp.rdt.sync.ui.RDTSyncUIPlugin;
import org.eclipse.ptp.rdt.sync.ui.SyncFileFilterPage;
import org.eclipse.ptp.rdt.sync.ui.SynchronizeParticipantRegistry;
import org.eclipse.ptp.rdt.sync.ui.messages.Messages;
import org.eclipse.ptp.remote.core.IRemoteConnection;
import org.eclipse.ptp.services.core.IService;
import org.eclipse.ptp.services.core.IServiceConfiguration;
import org.eclipse.ptp.services.core.IServiceProviderDescriptor;
import org.eclipse.ptp.services.core.ServiceModelManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

/**
 * Converts existing CDT projects to sync projects.
 * @since 1.0
 */
public class ConvertLocalToSyncProjectWizardPage extends ConvertProjectWizardPage {
	private final boolean showProviderCombo = false; // Change this variable to support multiple sync providers
													 // Otherwise, the first provider is used automatically
	private Combo fProviderCombo;
	private Composite fProviderArea;
	private StackLayout fProviderStack;
	private Composite fConfigArea;
	private CheckboxTableViewer fConfigTable;
	private Button switchToRemoteConfigButton;
	private SyncFileFilter customFilter = null;
	private final List<Composite> fProviderControls = new ArrayList<Composite>();
	private ISynchronizeParticipantDescriptor fSelectedProvider;
	private final Map<Integer, ISynchronizeParticipantDescriptor> fComboIndexToDescriptorMap = new HashMap<Integer, ISynchronizeParticipantDescriptor>();

	protected Map<IProject, IServiceConfiguration> projectConfigs = new HashMap<IProject, IServiceConfiguration>();

	/**
	 * Constructor for ConvertToRemoteWizardPage.
	 * 
	 * @param pageName
	 */
	public ConvertLocalToSyncProjectWizardPage(String pageName) {
		super(pageName);
	}

	private void addProviderControl(ISynchronizeParticipantDescriptor desc) {
		Composite comp = null;
		ISynchronizeParticipant part = desc.getParticipant();
		if (part != null) {
			comp = new Composite(fProviderArea, SWT.NONE);
			comp.setLayout(new GridLayout(1, false));
			comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			part.createConfigurationArea(comp, getWizard().getContainer());
		}
		fProviderControls.add(comp);
	}

	@Override
	protected void addToMainPage(Composite container) {
		Composite comp = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		comp.setLayout(layout);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		comp.setLayoutData(gd);

		// Label for "Provider:"
		if (showProviderCombo) {
			Label providerLabel = new Label(comp, SWT.LEFT);
			providerLabel.setText(Messages.NewRemoteSyncProjectWizardPage_syncProvider);

			// combo for providers
			fProviderCombo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
			// set layout to grab horizontal space
			fProviderCombo.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
			gd = new GridData();
			gd.horizontalSpan = 2;
			fProviderCombo.setLayoutData(gd);
			fProviderCombo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					handleProviderSelected(fProviderCombo.getSelectionIndex());
				}
			});
		}

		fProviderArea = new Group(comp, SWT.SHADOW_ETCHED_IN);
		fProviderStack = new StackLayout();
		fProviderArea.setLayout(fProviderStack);
		GridData providerAreaData = new GridData(SWT.FILL, SWT.FILL, true, true);
		providerAreaData.horizontalSpan = 3;
		fProviderArea.setLayoutData(providerAreaData);

		// Get provider information
		ISynchronizeParticipantDescriptor[] providers = SynchronizeParticipantRegistry.getDescriptors();

		for (int k = 0; k < providers.length; k++) {
			if (showProviderCombo) {
				fProviderCombo.add(providers[k].getName(), k);
			}
			fComboIndexToDescriptorMap.put(k, providers[k]);
			addProviderControl(providers[k]);
		}

		// TODO: Consider the case where there are no providers
		if (showProviderCombo) {
			fProviderCombo.select(0);
		}
		handleProviderSelected(0);
		
		// Need to update whenever the project selection changes
		this.tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				update();
			}
		});

		// Label for configuration table
		Label configTableLabel = new Label(comp, SWT.LEFT);
		configTableLabel.setText(Messages.ConvertLocalToSyncProjectWizardPage_2);

		// Configuration table
		// Simple but requires lots of boilerplate code
		fConfigArea = new Composite(comp, SWT.NONE);
		fConfigArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		fConfigArea.setLayout(new PageLayout());
		fConfigTable = CheckboxTableViewer.newCheckList(fConfigArea, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		fConfigTable.setContentProvider(new IStructuredContentProvider() {
			@Override
			public void dispose() {
				// nothing to do
			}
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				// nothing to do
			}
			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement == null) {
					return new IConfiguration[0];
				}
				assert(inputElement instanceof IProject);
				if (getCheckedElements().length != 1) {
					return new IConfiguration[0];
				}

				IProject project = (IProject) inputElement;
				IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(project);
				if (buildInfo == null) {
					return new IConfiguration[0];
				}
				return buildInfo.getManagedProject().getConfigurations();
			}
		});
		fConfigTable.setLabelProvider(new ILabelProvider() {
			@Override
			public void addListener(ILabelProviderListener listener) {
				// not implemented
			}
			@Override
			public void dispose() {
				// nothing to do
			}
			@Override
			public boolean isLabelProperty(Object element, String property) {
				return true; // safe option
			}
			@Override
			public void removeListener(ILabelProviderListener listener) {
				// not implemented
			}
			@Override
			public Image getImage(Object element) {
				return null;
			}
			@Override
			public String getText(Object element) {
				assert(element instanceof IConfiguration);
				return ((IConfiguration) element).getName();
			}
		});
		
		// Button to switch to remote config after conversion
		switchToRemoteConfigButton = new Button(comp, SWT.CHECK);
		switchToRemoteConfigButton.setText(Messages.ConvertLocalToSyncProjectWizardPage_4);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		switchToRemoteConfigButton.setLayoutData(gd);
		switchToRemoteConfigButton.setSelection(true);

		// File filter button
		final Button filterButton = new Button(comp, SWT.PUSH);
		filterButton.setText(Messages.ConvertLocalToSyncProjectWizardPage_3);
		filterButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 3, 1));
		filterButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				SyncFileFilter tmpFilter;
				if (customFilter == null) {
					tmpFilter = SyncManager.getDefaultFileFilter();
				} else {
					tmpFilter = new SyncFileFilter(customFilter);
				}
				int filterReturnCode = SyncFileFilterPage.openBlocking(tmpFilter, filterButton.getShell());
				if (filterReturnCode == Window.OK) {
					customFilter = tmpFilter;
				}
			}
		});
		
		// These buttons are useless when only one project should be selected
		this.selectAllButton.setVisible(false);
		this.deselectAllButton.setVisible(false);
	}

	protected void convertProject(final IProject project, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(Messages.ConvertToSyncProjectWizardPage_convertingToSyncProject, 3);
		
		// Add project natures
		RemoteSyncNature.addNature(project, new NullProgressMonitor());
//		try {
//			RemoteMakeNature.updateProjectDescription(project, RemoteMakeBuilder.REMOTE_MAKE_BUILDER_ID, new NullProgressMonitor());
//		} catch (CoreException e) {
//			StatusManager.getManager().handle(e, RDTSyncUIPlugin.PLUGIN_ID);
//		}

		try {
			ISynchronizeParticipant participant = fSelectedProvider.getParticipant();

			// Build the service configuration
			ServiceModelManager smm = ServiceModelManager.getInstance();
			IServiceConfiguration serviceConfig = smm.newServiceConfiguration(getConfigName(project.getName()));
			IService syncService = smm.getService(IRemoteSyncServiceConstants.SERVICE_SYNC);
			serviceConfig.setServiceProvider(syncService, participant.getProvider(project));

			IService buildService = smm.getService(IRDTServiceConstants.SERVICE_BUILD);
			IServiceProviderDescriptor descriptor = buildService.getProviderDescriptor(SyncBuildServiceProvider.ID);
			SyncBuildServiceProvider rbsp = (SyncBuildServiceProvider) smm.getServiceProvider(descriptor);
			if (rbsp != null) {
				IRemoteConnection remoteConnection = participant.getProvider(project).getRemoteConnection();
				rbsp.setRemoteToolsConnection(remoteConnection);
				serviceConfig.setServiceProvider(buildService, rbsp);
			}

			smm.addConfiguration(project, serviceConfig);
			try {
				smm.saveModelConfiguration();
			} catch (IOException e) {
				RDTSyncUIPlugin.log(e.toString(), e);
			}

			BuildConfigurationManager bcm = BuildConfigurationManager.getInstance();

			// Initialize project with a local build scenario, which is applied to all configurations
			bcm.setBuildScenarioForAllBuildConfigurations(project, bcm.createLocalBuildScenario(project));

			// Create a remote build scenario
			ISyncServiceProvider provider = participant.getProvider(project);
			BuildScenario remoteBuildScenario = new BuildScenario(provider.getName(), provider.getToolLocation(),
					provider.getRemoteConnection(), provider.getLocation());

			Object[] selectedConfigs = fConfigTable.getCheckedElements();
			Set<Object> selectedConfigsSet = new HashSet<Object>(Arrays.asList(selectedConfigs));
			IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(project);
			if (buildInfo == null) {
				throw new RuntimeException("Build information for project not found. Project name: " + project.getName()); //$NON-NLS-1$
			}
			
			// Iterate through all configs
			boolean switchToRemoteConfig = switchToRemoteConfigButton.getSelection();
			boolean defaultConfigSet = false;
			IConfiguration[] allConfigs = buildInfo.getManagedProject().getConfigurations();
			for (IConfiguration config : allConfigs) {
				// For selected configs, create a new remote config
				if (selectedConfigsSet.contains(config)) {
					IConfiguration remoteConfig = bcm.createConfiguration(project, (Configuration) config, remoteBuildScenario,
							config.getName().replace(' ', '_'), null); // Bug 389899 - "remote toolchain name" contains spaces
					bcm.modifyConfigurationAsSyncRemote(remoteConfig);

					// The first remote found will be the initial default (active) configuration.
					if (switchToRemoteConfig && !defaultConfigSet) {
						ManagedBuildManager.setDefaultConfiguration(project, remoteConfig);
						defaultConfigSet = true;
					}
				}
				
				bcm.modifyConfigurationAsSyncLocal(config);
			}
			ManagedBuildManager.saveBuildInfo(project, true);
			
			if (customFilter != null) {
				SyncManager.saveFileFilter(project, customFilter);
			}
			
		    // Enable sync'ing and force an initial sync
		    SyncManager.setSyncMode(project, SyncManager.SYNC_MODE.ACTIVE);
		    SyncManager.sync(null, project, SyncFlag.FORCE, new CommonSyncExceptionHandler(false, true));
		} finally {
			monitor.done();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.cdt.ui.wizards.conversion.ConvertProjectWizardPage#convertProject
	 * (org.eclipse.core.resources.IProject,
	 * org.eclipse.core.runtime.IProgressMonitor, java.lang.String)
	 */
	@Override
	public void convertProject(IProject project, IProgressMonitor monitor, String projectID) throws CoreException {
		convertProject(project, monitor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.cdt.ui.wizards.conversion.ConvertProjectWizardPage#convertProject
	 * (org.eclipse.core.resources.IProject, java.lang.String,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void convertProject(IProject project, String bsId, IProgressMonitor monitor) throws CoreException {
		convertProject(project, monitor);
	}

	@Override
	public void doRun(IProgressMonitor monitor, String projectID, String bsId) throws CoreException {
		// Disable initial auto build but make sure to set it back to previous value before exiting.
		boolean autoBuildWasSet = setAutoBuild(false);
		try {
			monitor.beginTask(Messages.ConvertToRemoteWizardPage_0, 2);
			super.doRun(new SubProgressMonitor(monitor, 1), projectID, bsId);
			ServiceModelManager.getInstance().saveModelConfiguration();
		} catch (IOException e) {
			RDTSyncUIPlugin.log(e);
		} finally {
			setAutoBuild(autoBuildWasSet);
			monitor.done();
		}
	}
	
	// Helper function to disable/enable auto build during project conversion
	// Returns the value of auto build before function was called.
	private static boolean setAutoBuild(boolean shouldBeEnabled) {
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IWorkspaceDescription desc= workspace.getDescription();
		boolean isAutoBuilding= desc.isAutoBuilding();
		if (isAutoBuilding != shouldBeEnabled) {
			desc.setAutoBuilding(shouldBeEnabled);
			try {
				workspace.setDescription(desc);
			} catch (CoreException e) {
				RDTSyncUIPlugin.log(e);
			}
		}
		return isAutoBuilding;
	}

	protected IServiceConfiguration getConfig(IProject project) {
		IServiceConfiguration config = projectConfigs.get(project);
		if (config == null) {
			config = ServiceModelManager.getInstance().newServiceConfiguration(project.getName());
			projectConfigs.put(project, config);
		}
		return config;
	}

	/**
	 * Creates a name for the service configuration based on the remote
	 * connection name. If multiple names exist, appends a qualifier to the
	 * name.
	 * 
	 * @return new name guaranteed to be unique
	 */
	private String getConfigName(String candidateName) {
		Set<IServiceConfiguration> configs = ServiceModelManager.getInstance().getConfigurations();
		Set<String> existingNames = new HashSet<String>();
		for (IServiceConfiguration config : configs) {
			existingNames.add(config.getName());
		}

		int i = 2;
		String newConfigName = candidateName;
		while (existingNames.contains(newConfigName)) {
			newConfigName = candidateName + " (" + (i++) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		return newConfigName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#getErrorMessage()
	 */
	@Override
	public String getErrorMessage() {
		String errMsg = null;
		if (super.getErrorMessage() != null) {
			errMsg = super.getErrorMessage();
		} else if (fSelectedProvider == null) {
			errMsg = Messages.ConvertToSyncProjectWizardPage_0;
		} else if (this.getCheckedElements().length != 1) {
			errMsg = Messages.ConvertFromRemoteCToSyncProjectWizardPage_3;
		} else {
			errMsg = fSelectedProvider.getParticipant().getErrorMessage();
		}
		setPageComplete(super.validatePage() && errMsg == null);
		return errMsg;
	}

	/**
	 * Method getWzDescriptionResource returns the correct description Label for
	 * this class overriding the default in the superclass.
	 */
	@Override
	protected String getWzDescriptionResource() {
		return "Converts a managed or unmanaged project to a synchronized project by adding a sync nature"; //$NON-NLS-1$
	}

	/**
	 * Method getWzTitleResource returns the correct Title Label for this class
	 * overriding the default in the superclass.
	 */
	@Override
	protected String getWzTitleResource() {
		return "Convert to a synchronized project"; //$NON-NLS-1$
	}

	/**
	 * Handle synchronize provider selected.
	 */
	private void handleProviderSelected(int index) {
		fProviderStack.topControl = fProviderControls.get(index);
		fSelectedProvider = fComboIndexToDescriptorMap.get(index);
		fProviderArea.layout();
		update();
	}

	/**
	 * Return true for projects that are:
	 * 1) not hidden
	 * 2) have C or CC nature
	 * 3) are not already sync projects
	 * 4) do not have remote nature
	 */
	@Override
	public boolean isCandidate(IProject project) {
		boolean a = false;
		boolean b = false;
		boolean c = false;
		boolean d = false;
		a = !project.isHidden();
		try {
			b = project.hasNature(CProjectNature.C_NATURE_ID) || project.hasNature(CCProjectNature.CC_NATURE_ID);
			c = !project.hasNature(RemoteSyncNature.NATURE_ID);
			d = !project.hasNature(RemoteNature.REMOTE_NATURE_ID);
		} catch (CoreException e) {
			RDTSyncUIPlugin.log(e);
		}

		return a && b && c && d;
	}

	private void update() {
		getWizard().getContainer().updateMessage();
		if (this.getCheckedElements().length == 1 && fSelectedProvider != null) {
			IProject project = (IProject) this.getCheckedElements()[0];
			fSelectedProvider.getParticipant().setProjectName(project.getName());
			if (fConfigTable != null) {
				fConfigTable.setInput(project);
				fConfigTable.setAllChecked(true);
			}
		} else {
			if (fConfigTable != null) {
				fConfigTable.setInput(null);
			}
		}
	}
}
