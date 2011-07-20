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
package org.eclipse.ptp.rdt.sync.ui.wizards;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.internal.core.envvar.EnvironmentVariableManager;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.ui.wizards.conversion.ConvertProjectWizardPage;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ptp.rdt.core.resources.RemoteNature;
import org.eclipse.ptp.rdt.core.services.IRDTServiceConstants;
import org.eclipse.ptp.rdt.sync.core.BuildConfigurationManager;
import org.eclipse.ptp.rdt.sync.core.resources.RemoteSyncNature;
import org.eclipse.ptp.rdt.sync.core.serviceproviders.SyncBuildServiceProvider;
import org.eclipse.ptp.rdt.sync.core.services.IRemoteSyncServiceConstants;
import org.eclipse.ptp.rdt.sync.ui.ISynchronizeParticipant;
import org.eclipse.ptp.rdt.sync.ui.ISynchronizeParticipantDescriptor;
import org.eclipse.ptp.rdt.sync.ui.RDTSyncUIPlugin;
import org.eclipse.ptp.rdt.sync.ui.SynchronizeParticipantRegistry;
import org.eclipse.ptp.rdt.sync.ui.messages.Messages;
import org.eclipse.ptp.remote.core.IRemoteConnection;
import org.eclipse.ptp.services.core.IService;
import org.eclipse.ptp.services.core.IServiceConfiguration;
import org.eclipse.ptp.services.core.IServiceProviderDescriptor;
import org.eclipse.ptp.services.core.ServiceModelManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Converts existing CDT projects to sync projects.
 * @since 1.0
 */
public class ConvertFromRemoteCToSyncProjectWizardPage extends ConvertProjectWizardPage {

	private Combo fProviderCombo;
	private Text fLocationText;
	private ISynchronizeParticipantDescriptor fSelectedProvider;
	private final Map<Integer, ISynchronizeParticipantDescriptor> fComboIndexToDescriptorMap = new HashMap<Integer, ISynchronizeParticipantDescriptor>();

	/**
	 * @since 2.0
	 */
	protected Map<IProject, IServiceConfiguration> projectConfigs = new HashMap<IProject, IServiceConfiguration>();

	/**
	 * Constructor for ConvertToRemoteWizardPage.
	 * 
	 * @param pageName
	 */
	public ConvertFromRemoteCToSyncProjectWizardPage(String pageName) {
		super(pageName);
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
				handleProviderSelected();
			}
		});

		// populate the combo with a list of providers
		ISynchronizeParticipantDescriptor[] providers = SynchronizeParticipantRegistry.getDescriptors();

		for (int k = 0; k < providers.length; k++) {
			fProviderCombo.add(providers[k].getName(), k);
			fComboIndexToDescriptorMap.put(k, providers[k]);
		}

		// Label for location
		Label locationLabel = new Label(comp, SWT.LEFT);
		locationLabel.setText(Messages.ConvertFromRemoteCToSyncProjectWizardPage_0);

		// Location text box
		fLocationText = new Text(comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		gd.grabExcessHorizontalSpace = true;
		gd.widthHint = 250;
		fLocationText.setLayoutData(gd);
		fLocationText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				update();
			}
		});

		fProviderCombo.select(0);
		handleProviderSelected();
	}

	/**
	 * @since 2.0
	 */
	protected void convertProject(IProject project, IProgressMonitor monitor) throws CoreException {
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
				SyncBuildServiceProvider currentBuildProvider = (SyncBuildServiceProvider) smm.getActiveConfiguration(project).
																								getServiceProvider(buildService);
				IRemoteConnection remoteConnection = null;
				if (currentBuildProvider != null) {
					remoteConnection = currentBuildProvider.getConnection();
				}
				rbsp.setRemoteToolsConnection(remoteConnection);
				serviceConfig.setServiceProvider(buildService, rbsp);
			}

			// For each build configuration, set builder to be the sync builder
			IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(project);
			if (buildInfo == null) {
				throw new RuntimeException("Build information for project not found. Project name: " + project.getName()); //$NON-NLS-1$
			}
			IConfiguration[] allConfigs = buildInfo.getManagedProject().getConfigurations();
			for (IConfiguration config : allConfigs) {
				IBuilder syncBuilder = ManagedBuildManager.getExtensionBuilder("org.eclipse.ptp.rdt.sync.core.SyncBuilder"); //$NON-NLS-1$
				config.changeBuilder(syncBuilder, "org.eclipse.ptp.rdt.sync.core.SyncBuilder", "Sync Builder"); //$NON-NLS-1$ //$NON-NLS-2$
				//turn off append contributed(local) environment variables for the build configuration of the remote project
				ICConfigurationDescription c_mb_confgDes = ManagedBuildManager.getDescriptionForConfiguration(config);
				if(c_mb_confgDes!=null){
					EnvironmentVariableManager.fUserSupplier.setAppendContributedEnvironment(false, c_mb_confgDes);
					//EnvironmentVariableManager.fUserSupplier.setAppendEnvironment(false, c_mb_confgDes);
				}
			}
			ManagedBuildManager.saveBuildInfo(project, true);

			// Initialize project for the BuildConfigurationManager
			// Do this last (except for adding local configuration) so that project is not flagged as initialized prematurely.
			BuildConfigurationManager.getInstance().initProject(project, serviceConfig, null);
			try {
				BuildConfigurationManager.getInstance().saveConfigurationData();
			} catch (IOException e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, RDTSyncUIPlugin.PLUGIN_ID, e.getMessage(), e),
						StatusManager.SHOW);
			}
			monitor.done();
		} finally {
			monitor.done();
		}

		BuildConfigurationManager.getInstance().createLocalConfiguration(project);
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
		monitor.beginTask(Messages.ConvertToRemoteWizardPage_0, 2);
		super.doRun(new SubProgressMonitor(monitor, 1), projectID, bsId);
		try {
			ServiceModelManager.getInstance().saveModelConfiguration();
		} catch (IOException e) {
			RDTSyncUIPlugin.log(e);
		} finally {
			monitor.done();
		}
	}

	/**
	 * @since 2.0
	 */
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
		} else if (fLocationText.getText().length() == 0 ) {
			errMsg = Messages.ConvertFromRemoteCToSyncProjectWizardPage_1;
		} else if (URIUtil.toURI(fLocationText.getText()) == null) {
			errMsg = Messages.ConvertFromRemoteCToSyncProjectWizardPage_2;
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
	private void handleProviderSelected() {
		int index = fProviderCombo.getSelectionIndex();
		fSelectedProvider = fComboIndexToDescriptorMap.get(index);
		update();
	}


	/**
	 * Return true for projects that are:
	 * 1) not hidden
	 * 2) have C or CC nature
	 * 3) are not already sync projects
	 * 4) have remote nature
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
			d = project.hasNature(RemoteNature.REMOTE_NATURE_ID);
		} catch (CoreException e) {
			RDTSyncUIPlugin.log(e);
		}

		return a && b && c && d;
	}

	/*
	 * @Override public boolean validatePage() { return super.validatePage();//
	 * && getErrorMessage()==null; }
	 */

	private void update() {
		getWizard().getContainer().updateMessage();
	}
}