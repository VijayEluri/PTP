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
package org.eclipse.ptp.ems.internal.ui;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ptp.ems.core.EnvManagerRegistry;
import org.eclipse.ptp.ems.core.IEnvManager;
import org.eclipse.ptp.ems.ui.EnvManagerConfigWidget;
import org.eclipse.ptp.ems.ui.IErrorListener;
import org.eclipse.ptp.remote.core.IRemoteConnection;
import org.eclipse.ptp.remote.core.IRemoteServices;
import org.eclipse.ptp.remote.ui.IRemoteUIConnectionManager;
import org.eclipse.ptp.remote.ui.PTPRemoteUIPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Instances of this class represent a user interface element which is used to set an environment configuration using a checklist;
 * this is intended to be used solely as part of a {@link EnvManagerConfigWidget}.
 * <p>
 * This element appears as one of the following.
 * <ul>
 * <li>If a connection to the remote machine is available but not yet opened, an informational message is displayed with a button
 * allowing the user to establish the connection.
 * <li>If a connection to the remote machine is open, a {@link SearchableChecklist} is displayed with a list of available
 * environment modules/macros.
 * <li>If there is no remote connection configured, or if the remote system does not have a supported environment configuration
 * system installed, then an informational message is displayed to the user.
 * </ul>
 * 
 * @author Jeff Overbey
 * 
 * @see Composite
 */
public final class EnvManagerChecklist extends Composite {
	private IRemoteServices remoteServices;
	private IRemoteConnection remoteConnection;
	private IEnvManager envManager;
	private IErrorListener errorListener;

	private URI lastSyncURI;
	private Set<String> lastSelectedItems;

	private Composite stack;
	private StackLayout stackLayout;

	private Composite messageComposite;
	private Label messageLabel;
	private Button connectButton;

	private SearchableChecklist checklist;
	@SuppressWarnings("unused") private Button loadDefaultsButton;

	/**
	 * Constructor.
	 * 
	 * @param parent
	 */
	public EnvManagerChecklist(Composite parent, IRemoteServices remoteServices, IRemoteConnection remoteConnection) {
		super(parent, SWT.NONE);

		this.remoteServices = remoteServices;
		this.remoteConnection = remoteConnection;
		this.envManager = EnvManagerRegistry.getNullEnvManager();
		this.lastSyncURI = null;
		this.lastSelectedItems = Collections.<String> emptySet();

		this.errorListener = new NullErrorListener();

		setLayoutData(new GridData(GridData.FILL_BOTH));
		setLayout(new GridLayout(1, false));

		createStack(this);
		createMsgComposite();
		createChecklist();

		checklist.setEnabledAndVisible(this.isEnabled());

		stackLayout.topControl = messageComposite;
		stack.layout(true, true);
	}

	private void createStack(Composite composite) {
		stack = new Composite(composite, SWT.NONE);
		stack.setLayoutData(new GridData(GridData.FILL_BOTH));
		stackLayout = new StackLayout();
		stack.setLayout(stackLayout);
	}

	private void createMsgComposite() {
		messageComposite = new Composite(stack, SWT.NONE);
		final GridLayout layout = new GridLayout(2, false);
		layout.horizontalSpacing = 10;
		layout.verticalSpacing = 10;
		messageComposite.setLayout(layout);

		messageLabel = new Label(messageComposite, SWT.WRAP);
		final GridData gridData = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gridData.widthHint = 450;
		messageLabel.setLayoutData(gridData);

		connectButton = new Button(messageComposite, SWT.PUSH);
		connectButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		connectButton.setText(Messages.EnvManagerChecklist_ConnectButtonLabel);
		connectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (connect()) {
					reset(null, remoteServices, remoteConnection, lastSelectedItems); // force reset() to think that the sync URI has changed
				}
			}
		});

		setNotConnectedMessage();
	}

	private void setNotConnectedMessage() {
		final String connectionName = getConnectionName();
		if (connectionName != null) {
			messageLabel.setText(NLS.bind(Messages.EnvManagerChecklist_RemoteEnvironmentIsNotConnected, connectionName));
		} else {
			messageLabel.setText(Messages.EnvManagerChecklist_NotRemoteSync);
		}
		connectButton.setEnabled(connectionName != null);
		connectButton.setVisible(connectionName != null);
		messageComposite.pack();

		stackLayout.topControl = messageComposite;
		stack.layout(true, true);
	}

	/** See {@link EnvManagerConfigWidget#setErrorListener(IErrorListener)} */
	public void setErrorListener(IErrorListener listener) {
		if (listener == null) {
			this.errorListener = new NullErrorListener();
		} else {
			this.errorListener = listener;
		}
	}

	/** See {@link EnvManagerConfigWidget#getConnectionName()} */
	public String getConnectionName() {
		return remoteConnection == null ? null : remoteConnection.getName();
	}

	private boolean connect() {
		if (remoteServices == null || remoteConnection == null) {
			return false;
		}

		if (!remoteServices.isInitialized()) {
			remoteServices.initialize();
		}

		if (remoteConnection.isOpen()) {
			return true;
		} else {
			final IRemoteUIConnectionManager connectionManager =
					PTPRemoteUIPlugin.getDefault().getRemoteUIServices(remoteServices).getUIConnectionManager();
			if (connectionManager == null) {
				return false;
			}

			connectionManager.openConnectionWithProgress(getShell(), null, remoteConnection);
			return true;
		}
	}

	private void createChecklist() {
		checklist = new SearchableChecklist(stack);
		checklist.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		checklist.setEnabledAndVisible(false);
		checklist.setColumnHeaders(Messages.EnvManagerChecklist_EnableColumnText,
		                           Messages.EnvManagerChecklist_ModuleNameColumnText);
		checklist.addDefaultButtonSelectonListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// No need to recompute the list of available modules; store the existing list and use it
				final Set<String> allModules = new HashSet<String>(checklist.getAllItems());
				checklist.asyncRepopulate(new AsyncRepopulationStrategy() {
					@Override
					public String getMessage() {
						return Messages.EnvManagerChecklist_PleaseWaitRetrievingModuleList;
					}

					@Override
					public Set<String> computeItems() {
						return allModules;
					}

					@Override
					public Set<String> computeSelectedItems() throws Exception {
						// Recompute the list of modules loaded by default
						return envManager.determineDefaultElements();
					}

					@Override
					public void afterRepopulation() {
						checklist.setEnabledAndVisible(EnvManagerChecklist.this.isEnabled());
					}
				});
			}
		});

	}

	/** See {@link EnvManagerConfigWidget#configurationChanged(URI, IRemoteServices, IRemoteConnection, Set)} */
	public void reset(URI newURI, final IRemoteServices remoteServices, final IRemoteConnection remoteConnection, final Set<String> selectedItems) {
		this.remoteServices = remoteServices;
		this.remoteConnection = remoteConnection;

		final boolean syncURIHasChanged = newURI == null || !newURI.equals(lastSyncURI);
		final boolean selectedItemsHaveChanged = selectedItems == null || !selectedItems.equals(lastSelectedItems);
		if (newURI != null) {
			lastSyncURI = newURI;
		}
		if (selectedItems != null) {
			lastSelectedItems = selectedItems;
		}
		if (messageComposite != null && checklist != null) {
			if (syncURIHasChanged || selectedItemsHaveChanged) {
				if (connectionIsOpen()) {
					inBackgroundThreadDetectEnvManager(remoteServices, remoteConnection, selectedItems);
				} else {
					setNotConnectedMessage();
				}
			}
		}
	}

	private void inBackgroundThreadDetectEnvManager(final IRemoteServices remoteServices, final IRemoteConnection remoteConnection, final Set<String> selectedItems) {
		setDetectingMessage();
		final Job job = new Job(Messages.EnvManagerChecklist_DetectingRemoteEMS) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					envManager = EnvManagerRegistry.getEnvManager(remoteServices, remoteConnection);
					final String description = envManager.getDescription();
					inUIThreadDisplayChecklist(selectedItems, description);
					return Status.OK_STATUS;
				} catch (final Exception e) {
					EMSUIPlugin.log(e);
					return new Status(IStatus.ERROR, EMSUIPlugin.PLUGIN_ID, e.getLocalizedMessage(), e);
				}
			}
		};
		job.setPriority(Job.INTERACTIVE);
		job.schedule();
	}

	private void setDetectingMessage() {
		messageLabel.setText(NLS.bind(Messages.EnvManagerChecklist_DetectingEMSPleaseWait, getConnectionName()));
		connectButton.setEnabled(false);
		connectButton.setVisible(false);
		messageComposite.pack();

		stackLayout.topControl = messageComposite;
		stack.layout(true, true);
	}

	private void inUIThreadDisplayChecklist(final Set<String> selectedItems, final String description) {
		final Job job = new Job(Messages.EnvManagerChecklist_UpdatingChecklist) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (!isDisposed()) {
					getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							String instructions = envManager.getInstructions();
							if (instructions.length() > 0) {
								instructions += Messages.EnvManagerChecklist_SettingsOnEnvironmentsPageAreAppliedBeforehand;
							}
							checklist.setInstructions(instructions, 250);
							checklist.setComparator(envManager.getComparator());
	
							if (envManager.equals(EnvManagerRegistry.getNullEnvManager())) {
								setIncompatibleInstallationMessage();
							} else {
								checklist.setTitle(
										NLS.bind(
												Messages.EnvManagerChecklist_EnvManagerInfo,
												description,
												getConnectionName()));
	
								checklist.setEnabledAndVisible(false);
								stackLayout.topControl = checklist;
								stack.layout(true, true);
								errorListener.errorCleared();
	
								populateModuleList(selectedItems);
							}
						}
					});
				}
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.INTERACTIVE);
		job.schedule();
	}

	private boolean connectionIsOpen() {
		if (remoteServices == null || remoteConnection == null) {
			return false;
		} else {
			return remoteConnection.isOpen();
		}
	}

	private void setIncompatibleInstallationMessage() {
		messageLabel.setText(NLS.bind(Messages.EnvManagerChecklist_NoSupportedEMSInstalled, getConnectionName()));
		connectButton.setEnabled(false);
		connectButton.setVisible(false);
		messageComposite.pack();

		stackLayout.topControl = messageComposite;
		stack.layout(true, true);
	}

	private void populateModuleList(final Set<String> selectedItems) {
		checklist.asyncRepopulate(new AsyncRepopulationStrategy() {
			@Override
			public String getMessage() {
				return Messages.EnvManagerChecklist_PleaseWaitRetrievingModuleList;
			}

			@Override
			public Set<String> computeItems() throws Exception {
				return envManager.determineAvailableElements();
			}

			@Override
			public Set<String> computeSelectedItems() throws Exception {
				if (selectedItems != null) {
					return selectedItems;
				} else {
					return envManager.determineDefaultElements();
				}
			}

			@Override
			public void afterRepopulation() {
				checklist.setEnabledAndVisible(EnvManagerChecklist.this.isEnabled());
			}
		});
	}

	// public boolean isModulesSupportEnabled() {
	// return this.isEnabled();
	// }

	/** See {@link EnvManagerConfigWidget#getSelectedElements()} */
	public Set<String> getSelectedElements() {
		return checklist.getSelectedItems();
	}

	/** See {@link EnvManagerConfigWidget#isChecklistEnabled()} */
	public boolean isChecklistEnabled() {
		return checklist.isEnabled();
	}

	/** @return the {@link IEnvManager} being used to populate this checklist */
	public IEnvManager getEnvManager() {
		return envManager;
	}

	// public Set<String> determineDefaultModules() throws RemoteConnectionException, IOException {
	// return modulesSupport.determineDefaultModules();
	// }
}
