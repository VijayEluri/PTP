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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.ptp.rdt.sync.ui.ISynchronizeParticipant;
import org.eclipse.ptp.rdt.sync.ui.ISynchronizeParticipantDescriptor;
import org.eclipse.ptp.rdt.sync.ui.SynchronizeParticipantRegistry;
import org.eclipse.ptp.rdt.sync.ui.messages.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.IIDEHelpContextIds;
import org.eclipse.cdt.ui.CDTSharedImages;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.cdt.ui.newui.PageLayout;
import org.eclipse.cdt.ui.wizards.CDTMainWizardPage;
import org.eclipse.cdt.ui.wizards.CNewWizard;
import org.eclipse.cdt.ui.wizards.CWizardHandler;
import org.eclipse.cdt.ui.wizards.EntryDescriptor;
import org.eclipse.cdt.ui.wizards.IWizardItemsListListener;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.ui.wizards.MBSWizardHandler;

/**
 * Main wizard page for creating new synchronized projects. All elements needed for a synchronized project are configured here.
 * This includes:
 * 1) Project name and workspace location
 * 2) Remote connection and directory
 * 3) Project type
 * 4) Local and remote toolchains
 *
 * Since this wizard page's operation differs greatly from a normal CDT wizard page, this class simply reimplements (overrides) all
 * functionality in the two immediate superclasses (CDTMainWizardPage and WizardNewProjectCreationPage) but borrows much of the code
 * from those two classes. Thus, except for very basic functionality, such as jface methods, this class is self-contained.
 */
public class SyncMainWizardPage extends CDTMainWizardPage implements IWizardItemsListListener {
	public static final String REMOTE_SYNC_WIZARD_PAGE_ID = "org.eclipse.ptp.rdt.sync.ui.remoteSyncWizardPage"; //$NON-NLS-1$
	public static final String SERVICE_PROVIDER_PROPERTY = "org.eclipse.ptp.rdt.sync.ui.remoteSyncWizardPage.serviceProvider"; //$NON-NLS-1$
	private static final String RDT_PROJECT_TYPE = "org.eclipse.ptp.rdt"; //$NON-NLS-1$
	private static final Image IMG_CATEGORY = CDTSharedImages.getImage(CDTSharedImages.IMG_OBJS_SEARCHFOLDER);
	private static final Image IMG_ITEM = CDTSharedImages.getImage(CDTSharedImages.IMG_OBJS_VARIABLE);
	private static final String EXTENSION_POINT_ID = "org.eclipse.cdt.ui.CDTWizard"; //$NON-NLS-1$
	private static final String ELEMENT_NAME = "wizard"; //$NON-NLS-1$
	private static final String CLASS_NAME = "class"; //$NON-NLS-1$
	public static final String DESC = "EntryDescriptor"; //$NON-NLS-1$
	private static final int SIZING_TEXT_FIELD_WIDTH = 250;

	// widgets
	private Text projectNameField;
	private Button defaultLocationButton;
	private Text projectLocationField;
	private Button browseButton;
	private Tree projectTypeTree;
	private Composite remoteToolChain;
	private Composite localToolChain;
	private Table localToolChainTable;
	private Button showSupportedOnlyButton;
	private Label projectRemoteOptionsLabel;
	private Label projectLocalOptionsLabel;
	private Label categorySelectedForRemoteLabel;
	private Label categorySelectedForLocalLabel;

	private SortedMap<String, IToolChain> toolChainMap;
	private ISynchronizeParticipant fSelectedParticipant = null;
	private String message = null;
	private int messageType = IMessageProvider.NONE;
	private String errorMessage = null;
	public enum ConfigType {
		LOCAL, REMOTE, BOTH
	}
	private Map<String, ConfigType> toolChainToConfigTypeMap = new HashMap<String, ConfigType>();

	/**
	 * Creates a new project creation wizard page.
	 *
	 * @param pageName the name of this page
	 */
	public SyncMainWizardPage(String pageName) {
		super(pageName);
		setPageComplete(false);
	}

	/** (non-Javadoc)
	 * Method declared on IDialogPage.
	 */
	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);

		initializeDialogUnits(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IIDEHelpContextIds.NEW_PROJECT_WIZARD_PAGE);

		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		setControl(composite);

		createProjectBasicInfoGroup(composite);
		createProjectRemoteInfoGroup(composite);
		createProjectDetailedInfoGroup(composite); 
		this.switchTo(this.updateData(projectTypeTree, remoteToolChain, false, SyncMainWizardPage.this, getWizard()), getDescriptor(projectTypeTree));

		setPageComplete(false);
		errorMessage = null;
		message = null;
		messageType = IMessageProvider.NONE;
		Dialog.applyDialogFont(composite);
	}

	private void createProjectDetailedInfoGroup(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		c.setLayout(new GridLayout(2, true));

		Label left_label = new Label(c, SWT.NONE);
		left_label.setText(Messages.SyncMainWizardPage_0);
		left_label.setFont(parent.getFont());
		left_label.setLayoutData(new GridData(GridData.BEGINNING));

		projectRemoteOptionsLabel = new Label(c, SWT.NONE);
		projectRemoteOptionsLabel.setFont(parent.getFont());
		projectRemoteOptionsLabel.setLayoutData(new GridData(GridData.BEGINNING));
		projectRemoteOptionsLabel.setText(Messages.SyncMainWizardPage_1);

		projectTypeTree = new Tree(c, SWT.SINGLE | SWT.BORDER);
		projectTypeTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3));
		projectTypeTree.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TreeItem[] tis = projectTypeTree.getSelection();
				if (tis == null || tis.length == 0) return;
				switchTo((CWizardHandler)tis[0].getData(), (EntryDescriptor)tis[0].getData(DESC));
				setPageComplete(validatePage());
				getWizard().getContainer().updateMessage();
			}});
		projectTypeTree.getAccessible().addAccessibleListener(
				new AccessibleAdapter() {                       
					@Override
					public void getName(AccessibleEvent e) {
						for (int i = 0; i < projectTypeTree.getItemCount(); i++) {
							if (projectTypeTree.getItem(i).getText().equals(e.result))
								return;
						}
						e.result = Messages.SyncMainWizardPage_2;
					}
				}
				);

		remoteToolChain = new Composite(c, SWT.NONE);
		remoteToolChain.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		remoteToolChain.setLayout(new PageLayout());

		projectLocalOptionsLabel = new Label(c, SWT.NONE);
		projectLocalOptionsLabel.setFont(parent.getFont());
		projectLocalOptionsLabel.setLayoutData(new GridData(GridData.BEGINNING));
		projectLocalOptionsLabel.setText(Messages.SyncMainWizardPage_3);

		localToolChain = new Composite(c, SWT.NONE);
		localToolChain.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		localToolChain.setLayout(new PageLayout());
		localToolChainTable = new Table(localToolChain, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		localToolChainTable.setVisible(true);


		showSupportedOnlyButton = new Button(c, SWT.CHECK);
		showSupportedOnlyButton.setText(Messages.SyncMainWizardPage_4);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		showSupportedOnlyButton.setLayoutData(gd);
		showSupportedOnlyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				switchTo(updateData(projectTypeTree, remoteToolChain, false, SyncMainWizardPage.this, getWizard()),
						getDescriptor(projectTypeTree));
			}} );
		showSupportedOnlyButton.setSelection(false);
	}

	@Override
	public IWizardPage getNextPage() {
		return (h_selected == null) ? null : h_selected.getSpecificPage();
	}		

	/**
	 * Returns the current project location path as entered by the user
	 *
	 * @return the project location path or its anticipated initial value.
	 */
	@Override
	public IPath getLocationPath() {
		return new Path(projectLocationField.getText());
	}
	

	/**
	 * Get workspace URI
	 * 
	 * @return URI or null if location path is not a valid URI
	 */
    @Override
    public URI getLocationURI() {
    	try {
			return new URI(getLocationPath().toString());
		} catch (URISyntaxException e) {
			return null;
		}
    }

    /**
     * Get project location URI
     * @return URI (may be null if location path is not a valid URI)
     */
    @Override
    public URI getProjectLocation() {
    	return useDefaults() ? null : getLocationURI();
    }


	@Override
	protected boolean validatePage() {
		message = null;
		messageType = IMessageProvider.NONE;
		errorMessage = null;
		return (validateProjectNameAndLocation() && validateProjectTypeSelection() && validateRemoteLocation());
	}

	protected boolean validateProjectNameAndLocation() {
		// Check if name is empty
		String projectFieldContents = getProjectNameFieldValue();
		if (projectFieldContents.equals("")) { //$NON-NLS-1$
			message = Messages.SyncMainWizardPage_5;
			messageType = IMessageProvider.NONE;
			return false;
		}

		// General name check
		IWorkspace workspace = IDEWorkbenchPlugin.getPluginWorkspace();
		IStatus nameStatus = workspace.validateName(projectFieldContents, IResource.PROJECT);
		if (!nameStatus.isOK()) {
			errorMessage = nameStatus.getMessage();
			return false;
		}

		// Do not allow # in the name
		if (getProjectName().indexOf('#') >= 0) {
			errorMessage = Messages.SyncMainWizardPage_6;
			return false;
		}

		IProject handle = ResourcesPlugin.getWorkspace().getRoot().getProject(projectNameField.getText());
		URI location = URIUtil.toURI(projectLocationField.getText());

		// Check if project exists
		if (handle.exists()) {
			errorMessage = Messages.SyncMainWizardPage_7;
			return false;
		}
		
		// Validate location according to built-in rules
		if (!defaultLocationButton.getSelection())
		{
			IStatus locationStatus = ResourcesPlugin.getWorkspace().validateProjectLocationURI(handle, location);
			if (!locationStatus.isOK()) {
				errorMessage = locationStatus.getMessage();
				return false;
			}
		}

		// Check if location is an existing file or directory
		try {
			IFileStore fs = EFS.getStore(location);
			IFileInfo f = fs.fetchInfo();
			if (f.exists()) {
				if (f.isDirectory()) {
					message = Messages.SyncMainWizardPage_8;
					messageType = IMessageProvider.WARNING;
				} else {
					errorMessage = Messages.SyncMainWizardPage_9;
					return false;
				}
			}
		} catch (CoreException e) {
			CUIPlugin.log(e.getStatus());
		}

		return true;
	}

	protected boolean validateProjectTypeSelection() {
		if (projectTypeTree.getItemCount() == 0) {
			errorMessage = Messages.SyncMainWizardPage_10;
			return false;
		}

		if (h_selected == null) {
			message = Messages.SyncMainWizardPage_11;
			messageType = IMessageProvider.NONE;
			return false;	        	
		}

		String s = h_selected.getErrorMessage(); 
		if (s != null) {
			errorMessage = s;
			return false;
		}

		return true;
	}

	protected boolean validateRemoteLocation() {
		errorMessage = fSelectedParticipant.getErrorMessage();
		return fSelectedParticipant.getErrorMessage()==null && fSelectedParticipant.isConfigComplete();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#getMessage()
	 */
	@Override
	public String getMessage() {
		setPageComplete(validatePage()); // Necessary to update message when participant changes
		return message;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#getMessageType()
	 */
	@Override
	public int getMessageType() {
		setPageComplete(validatePage()); // Necessary to update message when participant changes
		return messageType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#getErrorMessage()
	 */
	@Override
	public String getErrorMessage() {
		setPageComplete(validatePage()); // Necessary to update message when participant changes
		return errorMessage;
	}

	/**
	 * 
	 * @param tree
	 * @param right
	 * @param show_sup
	 * @param ls
	 * @param wizard
	 * @return : selected Wizard Handler.
	 */
	public CWizardHandler updateData(Tree tree, Composite right, boolean show_sup, IWizardItemsListListener ls, IWizard wizard) {
		// remember selected item
		TreeItem[] selection = tree.getSelection();
		TreeItem selectedItem = selection.length>0 ? selection[0] : null; 
		String savedLabel = selectedItem!=null ? selectedItem.getText() : null;
		String savedParentLabel = getParentText(selectedItem);

		tree.removeAll();
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(EXTENSION_POINT_ID);
		if (extensionPoint == null) return null;
		IExtension[] extensions = extensionPoint.getExtensions();
		if (extensions == null) return null;

		List<EntryDescriptor> items = new ArrayList<EntryDescriptor>();
		for (int i = 0; i < extensions.length; ++i)	{
			IConfigurationElement[] elements = extensions[i].getConfigurationElements();
			for (IConfigurationElement element : elements) {
				if (element.getName().equals(ELEMENT_NAME)) {
					CNewWizard w = null;
					try {
						w = (CNewWizard) element.createExecutableExtension(CLASS_NAME);
					} catch (CoreException e) {
						System.out.println(Messages.SyncMainWizardPage_12 + e.getLocalizedMessage()); 
						return null; 
					}
					if (w == null) return null;
					w.setDependentControl(right, ls);
					for (EntryDescriptor ed : w.createItems(show_sup, wizard)) {
						items.add(ed);
					}
				}
			}
		}
		// If there is a EntryDescriptor which is default for category, make sure it 
		// is in the front of the list.
		for (int i = 0; i < items.size(); ++i)
		{
			EntryDescriptor ed = items.get(i);
			if (ed.isDefaultForCategory())
			{
				items.remove(i);
				items.add(0, ed);
				break;
			}				
		}

		// bug # 211935 : allow items filtering.
		if (ls != null) // NULL means call from prefs
			items = ls.filterItems(items);
		addItemsToTree(tree, items);

		if (tree.getItemCount() > 0) {
			TreeItem target = null;
			// try to search item which was selected before
			if (savedLabel!=null) {
				target = findItem(tree, savedLabel, savedParentLabel);
			}
			if (target == null) {
				target = tree.getItem(0);
				if (target.getItemCount() != 0)
					target = target.getItem(0);
			}
			tree.setSelection(target);
			return (CWizardHandler)target.getData();
		}
		return null;
	}

	private static String getParentText(TreeItem item) {
		if (item==null || item.getParentItem()==null)
			return ""; //$NON-NLS-1$
		return item.getParentItem().getText();
	}

	private static TreeItem findItem(Tree tree, String label, String parentLabel) {
		for (TreeItem item : tree.getItems()) {
			TreeItem foundItem = findTreeItem(item, label, parentLabel);
			if (foundItem!=null)
				return foundItem;
		}
		return null;
	}

	private static TreeItem findTreeItem(TreeItem item, String label, String parentLabel) {
		if (item.getText().equals(label) && getParentText(item).equals(parentLabel))
			return item;

		for (TreeItem child : item.getItems()) {
			TreeItem foundItem = findTreeItem(child, label, parentLabel);
			if (foundItem!=null)
				return foundItem;
		}
		return null;
	}

	private static void addItemsToTree(Tree tree, List<EntryDescriptor> items) {
		//  Sorting is disabled because of users requests	
		//	Collections.sort(items, CDTListComparator.getInstance());

		ArrayList<TreeItem> placedTreeItemsList = new ArrayList<TreeItem>(items.size());
		ArrayList<EntryDescriptor> placedEntryDescriptorsList = new ArrayList<EntryDescriptor>(items.size());
		for (EntryDescriptor wd : items) {
			if (wd.getParentId() == null) {
				wd.setPath(wd.getId());
				TreeItem ti = new TreeItem(tree, SWT.NONE);
				ti.setText(TextProcessor.process(wd.getName()));
				ti.setData(wd.getHandler());
				ti.setData(DESC, wd);
				ti.setImage(calcImage(wd));
				placedTreeItemsList.add(ti);
				placedEntryDescriptorsList.add(wd);
			}
		}
		while(true) {
			boolean found = false;
			Iterator<EntryDescriptor> it2 = items.iterator();
			while (it2.hasNext()) {
				EntryDescriptor wd1 = it2.next();
				if (wd1.getParentId() == null) continue;
				for (int i = 0; i< placedEntryDescriptorsList.size(); i++) {
					EntryDescriptor wd2 = placedEntryDescriptorsList.get(i);
					if (wd2.getId().equals(wd1.getParentId())) {
						found = true;
						wd1.setParentId(null);
						CWizardHandler h = wd2.getHandler();
						/* If neither wd1 itself, nor its parent (wd2) have a handler
						 * associated with them, and the item is not a category,
						 * then skip it. If it's category, then it's possible that
						 * children will have a handler associated with them.
						 */
						if (h == null && wd1.getHandler() == null && !wd1.isCategory())
							break;

						wd1.setPath(wd2.getPath() + "/" + wd1.getId()); //$NON-NLS-1$
						wd1.setParent(wd2);
						if (h != null) {
							if (wd1.getHandler() == null && !wd1.isCategory())
								wd1.setHandler((CWizardHandler)h.clone());
							if (!h.isApplicable(wd1))
								break;
						}

						TreeItem p = placedTreeItemsList.get(i);
						TreeItem ti = new TreeItem(p, SWT.NONE);
						ti.setText(wd1.getName());
						ti.setData(wd1.getHandler());
						ti.setData(DESC, wd1);
						ti.setImage(calcImage(wd1));
						placedTreeItemsList.add(ti);
						placedEntryDescriptorsList.add(wd1);
						break;
					}
				}
			}
			// repeat iterations until all items are placed.
			if (!found) break;
		}
		// orphan elements (with not-existing parentId) are ignored
	}

	private static Image calcImage(EntryDescriptor ed) {
		if (ed.getImage() != null) return ed.getImage();
		if (ed.isCategory()) return IMG_CATEGORY;
		return IMG_ITEM;
	}

	private void switchTo(CWizardHandler h, EntryDescriptor ed) {
		if (h == null) 
			h = ed.getHandler();
		if (ed.isCategory())
			h = null;
		try {
			if (h != null) 
				h.initialize(ed);
		} catch (CoreException e) { 
			h = null;
		}
		if (h_selected != null) 
			h_selected.handleUnSelection();
		h_selected = h;
		if (h == null) {
			if (ed.isCategory()) {
				if (categorySelectedForRemoteLabel == null) {
					categorySelectedForRemoteLabel = new Label(remoteToolChain, SWT.WRAP);
					categorySelectedForRemoteLabel.setText(Messages.SyncMainWizardPage_13);
					remoteToolChain.layout();
				}

				if (categorySelectedForLocalLabel == null) {
					categorySelectedForLocalLabel = new Label(localToolChain, SWT.WRAP);
					categorySelectedForLocalLabel.setText(Messages.SyncMainWizardPage_13);
					localToolChain.layout();
				}
			}
			return;
		}
		if (categorySelectedForRemoteLabel != null)
			categorySelectedForRemoteLabel.setVisible(false);
		if (categorySelectedForLocalLabel != null)
			categorySelectedForLocalLabel.setVisible(false);
		h_selected.handleSelection();
		h_selected.setSupportedOnly(false);

		// Create local view
		localToolChainTable.removeAll();
		toolChainMap = ((MBSWizardHandler) h_selected).getToolChains();
		boolean filterToolChains = showSupportedOnlyButton.getSelection();
		for (Map.Entry<String, IToolChain> entry : toolChainMap.entrySet()) {
			String name = entry.getKey();
			IToolChain tc = entry.getValue();
			
			if (filterToolChains) {
				if (tc != null && (!tc.isSupported() || !ManagedBuildManager.isPlatformOk(tc))) {
					continue;
				}
			}
			TableItem ti = new TableItem(localToolChainTable, SWT.NONE);
			ti.setText(name);
		}
		if (toolChainMap.keySet().size() > 0) {
			localToolChainTable.select(0);
		}
	}

	public static EntryDescriptor getDescriptor(Tree _tree) {
		TreeItem[] sel = _tree.getSelection();
		if (sel == null || sel.length == 0) 
			return null;
		return (EntryDescriptor)sel[0].getData(DESC);
	}

	@Override
	public void toolChainListChanged(int count) {
		setPageComplete(validatePage());
		getWizard().getContainer().updateButtons();
	}

	@Override
	public boolean isCurrent() { return isCurrentPage(); }

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List filterItems(List items) {
		/*
		 * Remove RDT project types as these will not work with synchronized
		 * projects
		 */
		Iterator iterator = items.iterator();

		List<EntryDescriptor> filteredList = new LinkedList<EntryDescriptor>();

		while (iterator.hasNext()) {
			EntryDescriptor ed = (EntryDescriptor) iterator.next();
			if (!ed.getId().startsWith(RDT_PROJECT_TYPE)) {
				filteredList.add(ed);
			}
		}

		return filteredList;
	}

	/**
	 * Creates the project name specification controls.
	 *
	 * @param parent the parent composite
	 */
	private final void createProjectBasicInfoGroup(Composite parent) {
		Composite projectGroup = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		projectGroup.setLayout(layout);
		projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// new project name label
		Label projectNameLabel = new Label(projectGroup, SWT.NONE);
		projectNameLabel.setText(Messages.SyncMainWizardPage_14);
		projectNameLabel.setFont(parent.getFont());

		// new project name entry field
		projectNameField = new Text(projectGroup, SWT.BORDER);
		GridData nameData = new GridData(GridData.FILL_HORIZONTAL);
		nameData.horizontalSpan = 2;
		nameData.widthHint = SIZING_TEXT_FIELD_WIDTH;
		projectNameField.setLayoutData(nameData);
		projectNameField.setFont(parent.getFont());

		projectNameField.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event e) {
				setProjectLocation();
				setPageComplete(validatePage());
				getWizard().getContainer().updateMessage();
			}
		});

		// Use default location button
		defaultLocationButton = new Button(projectGroup, SWT.CHECK);
		defaultLocationButton.setText(Messages.SyncMainWizardPage_15);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		defaultLocationButton.setLayoutData(gd);
		defaultLocationButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setProjectLocation();
			}});
		defaultLocationButton.setSelection(true);

		// new project location label
		Label projectLocationLabel = new Label(projectGroup, SWT.NONE);
		projectLocationLabel.setText(Messages.SyncMainWizardPage_16);
		projectLocationLabel.setFont(parent.getFont());

		// new project location entry field
		projectLocationField = new Text(projectGroup, SWT.BORDER);
		GridData locationData = new GridData(GridData.FILL_HORIZONTAL);
		locationData.widthHint = SIZING_TEXT_FIELD_WIDTH;
		projectLocationField.setLayoutData(locationData);
		projectLocationField.setFont(parent.getFont());
		projectLocationField.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event e) {
				setPageComplete(validatePage());
				getWizard().getContainer().updateMessage();
			}
		});
		projectLocationField.setEnabled(false);
		this.setProjectLocation();

		// Browse button
		browseButton = new Button(projectGroup, SWT.PUSH);
		browseButton.setText(Messages.SyncMainWizardPage_17);
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dirDialog = new DirectoryDialog(projectLocationField.getShell());
				dirDialog.setText(Messages.SyncMainWizardPage_18);
				String selectedDir = dirDialog.open();
				projectLocationField.setText(selectedDir);
			}
		});
	}

	private final void createProjectRemoteInfoGroup(Composite parent) {
		// For now, assume only one provider, to reduce the number of GUI elements.
		// TODO: Add error handling if there are no providers
		ISynchronizeParticipantDescriptor[] providers = SynchronizeParticipantRegistry.getDescriptors();
		fSelectedParticipant = providers[0].getParticipant();
		fSelectedParticipant.createConfigurationArea(parent, getWizard().getContainer());
	}

	// Decides what should appear in project location field and whether or not it should be enabled
	private void setProjectLocation() {
		// Build string if default location is indicated.
		if (defaultLocationButton.getSelection()) {
			projectLocationField.setText(Platform.getLocation().toOSString() + File.separator + projectNameField.getText());
			// If user just unchecked default location, erase field contents.
		} else if (!projectLocationField.isEnabled()) {
			projectLocationField.setText(""); //$NON-NLS-1$
		}

		// These two values should never match.
		projectLocationField.setEnabled(!defaultLocationButton.getSelection());
	}

	/**
	 * Creates a project resource handle for the current project name field
	 * value. The project handle is created relative to the workspace root.
	 * <p>
	 * This method does not create the project resource; this is the
	 * responsibility of <code>IProject::create</code> invoked by the new
	 * project resource wizard.
	 * </p>
	 * 
	 * @return the new project resource handle
	 */
	@Override
	public IProject getProjectHandle() {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(getProjectName());
	}

	/**
	 * Returns the current project name as entered by the user, or its anticipated
	 * initial value.
	 *
	 * @return the project name, its anticipated initial value, or <code>null</code>
	 *   if no project name is known
	 */
	@Override
	public String getProjectName() {
		return getProjectNameFieldValue();
	}

	/**
	 * Returns the value of the project name field
	 * with leading and trailing spaces removed.
	 * 
	 * @return the project name in the field
	 */
	private String getProjectNameFieldValue() {
		if (projectNameField == null) {
			return ""; //$NON-NLS-1$
		}

		return projectNameField.getText().trim();
	}

	/**
	 * Sets the initial project name that this page will use when
	 * created. The name is ignored if the createControl(Composite)
	 * method has already been called. Leading and trailing spaces
	 * in the name are ignored.
	 * Providing the name of an existing project will not necessarily 
	 * cause the wizard to warn the user.  Callers of this method 
	 * should first check if the project name passed already exists 
	 * in the workspace.
	 * 
	 * @param name initial project name for this page
	 * 
	 * @see IWorkspace#validateName(String, int)
	 * 
	 * TODO: Calls to this function can be ignored probably, but I'm not certain.
	 */
	@Override
	public void setInitialProjectName(String name) {
		// Ignore
	}

	/*
	 * see @DialogPage.setVisible(boolean)
	 */
	@Override
	public void setVisible(boolean visible) {
		this.getControl().setVisible(visible);
		if (visible) {
			projectNameField.setFocus();
		}
	}
	
	@Override
	public boolean useDefaults() {
		return defaultLocationButton.getSelection();
	}

	/**
	 * Get the synchronize participant, which contains remote information
	 * @return participant
	 */
	public ISynchronizeParticipant getSynchronizeParticipant() {
		return fSelectedParticipant;
	}

	/**
	 * Get the selected local tool chain
	 * @return tool chain or null if either none selected or name does not map to a value (such as "Other Toolchain")
	 */
	public IToolChain getLocalToolChain() {
		TableItem[] selectedToolChains  = localToolChainTable.getSelection();
		if (selectedToolChains.length < 1) {
			return null;
		} else {
			String toolChainName = selectedToolChains[0].getText();
			return toolChainMap.get(toolChainName);
		}
	}
	
    /**
	 * No working sets for this wizard
	 * @return null
	 */
	@Override
	public IWorkingSet[] getSelectedWorkingSets() {
		return null;
	}
	
	/**
	 * Prepare page for reading by CDT. Specifically, select the local toolchains in the remote toolchains window, so CDT will
	 * create configurations for them. Also, record data to distinguish local and remote toolchains.
	 */
	public void prepareForSubmission() {
		// Record all local toolchain names
		Set<String> localToolNames = new HashSet<String>();
		for (TableItem ti : localToolChainTable.getSelection()) {
			localToolNames.add(ti.getText());
		}

		// Compare remotes and locals
		Table remoteTable = ((MBSWizardHandler) h_selected).getToolChainsTable();
		for (int i = 0; i < remoteTable.getItemCount(); i++) {
			ConfigType configType = null;
			String toolChainName = remoteTable.getItem(i).getText();
			if (remoteTable.isSelected(i)) {
				configType = ConfigType.REMOTE;
			}

			if (localToolNames.contains(toolChainName)) {
				if (configType == null) {
					configType = ConfigType.LOCAL;
				} else {
					configType = ConfigType.BOTH;
				}
				remoteTable.select(i);
			}
			
			if (configType != null) {
				toolChainToConfigTypeMap.put(toolChainName, configType);
			}
		}
	}
	
	public ConfigType getConfigType(IConfiguration config) {
		String toolChainName = config.getToolChain().getSuperClass().getName();
		return toolChainToConfigTypeMap.get(toolChainName);
	}
}