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
package org.eclipse.ptp.rdt.sync.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.ptp.rdt.sync.core.BinaryResourceMatcher;
import org.eclipse.ptp.rdt.sync.core.BuildConfigurationManager;
import org.eclipse.ptp.rdt.sync.core.BuildScenario;
import org.eclipse.ptp.rdt.sync.core.PathResourceMatcher;
import org.eclipse.ptp.rdt.sync.core.RemoteContentProvider;
import org.eclipse.ptp.rdt.sync.core.ResourceMatcher;
import org.eclipse.ptp.rdt.sync.core.RegexResourceMatcher;
import org.eclipse.ptp.rdt.sync.core.SyncFileFilter;
import org.eclipse.ptp.rdt.sync.core.SyncFileFilter.PatternType;
import org.eclipse.ptp.rdt.sync.core.SyncManager;
import org.eclipse.ptp.rdt.sync.ui.messages.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * File tree where users can select the files to be sync'ed
 */
public class SyncFileTree extends ApplicationWindow {
	private static final int ERROR_DISPLAY_SECONDS = 3;
	private static final Display display = Display.getCurrent();

	private final int windowWidth;
	private final int viewHeight; // Used for file tree and pattern table

	private final IProject project;
	private final SyncFileFilter filter;
	private CheckboxTreeViewer treeViewer;
	private Table patternTable;
	private Button showRemoteButton;
	private Label remoteErrorLabel;
	private Button upButton;
	private Button downButton;
	private Button removeButton;
	private Text newPath;
	private Button excludeButtonForPath;
	private Button includeButtonForPath;
	private Text newRegex;
	private Button excludeButtonForRegex;
	private Button includeButtonForRegex;
	private Combo specialFiltersCombo;
	private Button excludeButtonForSpecial;
	private Button includeButtonForSpecial;
	private Label patternErrorLabel;
	private Button cancelButton;
	private Button okButton;
	private final Map<String, ResourceMatcher> specialFilterNameToPatternMap = new HashMap<String, ResourceMatcher>();

	public SyncFileTree() {
		this(null);
	}

	/**
	 * General constructor for a new tree. If project is null, the default filter will be used. Otherwise, the passed project's
	 * filter used. Most likely, in the former case the page has been invoked from the preference menu, and in the latter case
	 * from the sync context menu.
	 *
	 * @param p project
	 */
	public SyncFileTree(IProject p) {
		super(null);
		project = p;
		if (project == null) {
			filter = SyncManager.getDefaultFileFilter();
		} else {
			filter = SyncManager.getFileFilter(project);
		}

		// Only one special (not path or regex) filter at the moment. If more are added later, we need a more sophisticated
		// method for handling these special filters.
		BinaryResourceMatcher bpm = new BinaryResourceMatcher();
		specialFilterNameToPatternMap.put(bpm.toString(), bpm);
		
		windowWidth = display.getBounds().width / 3;
		viewHeight = display.getBounds().height / 6;
	}

	/**
	 * Configures the shell
	 *
	 * @param shell
	 */
	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		if (project == null) {
			shell.setText(Messages.SyncFileTree_20);
		} else {
			shell.setText(Messages.SyncFileTree_0);
		}
	}

	/**
	 * Creates the main window's contents
	 * 
	 * @param parent
	 *            the main window
	 * @return Control
	 */
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		gl.verticalSpacing = 20;
		composite.setLayout(gl);

		// Composite for tree viewer
		Composite treeViewerComposite = new Composite(composite, SWT.BORDER);
		treeViewerComposite.setLayout(new GridLayout(2, false));
		treeViewerComposite.setLayoutData(new GridData(windowWidth, viewHeight));

		// Label for tree viewer
		Label treeViewerLabel = new Label(treeViewerComposite, SWT.NONE);
		treeViewerLabel.setText(Messages.SyncFileTree_1);
		treeViewerLabel.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, false, false, 2, 1));
		this.formatAsHeader(treeViewerLabel);

		// File tree viewer
		treeViewer = new CheckboxTreeViewer(treeViewerComposite);
		treeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		treeViewer.setContentProvider(new SFTTreeContentProvider());
		treeViewer.setLabelProvider(new SFTTreeLabelProvider());
		treeViewer.setCheckStateProvider(new ICheckStateProvider() {
			public boolean isChecked(Object element) {
				if (filter.shouldIgnore((IResource) element)) {
					return false;
				} else {
					return true;
				}
			}

			public boolean isGrayed(Object element) {
				return false;
			}
		});
		treeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				IPath path = ((IResource) (event.getElement())).getProjectRelativePath();
				if (event.getChecked()) {
					filter.addPattern(new PathResourceMatcher(path), PatternType.INCLUDE);
				} else {
					filter.addPattern(new PathResourceMatcher(path), PatternType.EXCLUDE);
				}

				update();
			}
		});
		treeViewer.setInput(project);
		
		showRemoteButton = new Button(treeViewerComposite, SWT.CHECK);
		showRemoteButton.setText(Messages.SyncFileTree_2);
		showRemoteButton.setSelection(((SFTTreeContentProvider) treeViewer.getContentProvider()).getShowRemoteFiles());
        showRemoteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	update();
            }
        });
        
        remoteErrorLabel = new Label(treeViewerComposite, SWT.CENTER);
        remoteErrorLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// Composite for pattern table and buttons
		Composite patternTableComposite = new Composite(composite, SWT.BORDER);
		patternTableComposite.setLayout(new GridLayout(2, false));
		patternTableComposite.setLayoutData(new GridData(windowWidth, viewHeight));
		
		// Label for pattern table
		Label patternTableLabel = new Label(patternTableComposite, SWT.NONE);
		patternTableLabel.setText(Messages.SyncFileTree_3);
		this.formatAsHeader(patternTableLabel);
		patternTableLabel.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, false, false, 4, 1));
		
		// Pattern table
		patternTable = new Table(patternTableComposite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		patternTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3));
		new TableColumn(patternTable, SWT.LEAD, 0);
		new TableColumn(patternTable, SWT.LEAD, 1); // Separate column for pattern for alignment and font change.
		
		// Pattern table buttons (up, down, and delete)
		upButton = new Button(patternTableComposite, SWT.PUSH);
	    upButton.setText(Messages.SyncFileTree_6);
	    upButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
	    upButton.addSelectionListener(new SelectionAdapter() {
	    	public void widgetSelected(SelectionEvent event) {
	    		TableItem[] selectedPatternItems = patternTable.getSelection();
	    		if (selectedPatternItems.length != 1) {
	    			return;
	    		}
	    		int patternIndex = patternTable.getSelectionIndex();
	    		if (filter.promote((ResourceMatcher) selectedPatternItems[0].getData())) {
	    			patternIndex--;
	    		}
	    		update();
	    		patternTable.select(patternIndex);
	    	}
	    });

	    downButton = new Button(patternTableComposite, SWT.PUSH);
	    downButton.setText(Messages.SyncFileTree_7);
	    downButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
	    downButton.addSelectionListener(new SelectionAdapter() {
	    	public void widgetSelected(SelectionEvent event) {
	    		TableItem[] selectedPatternItems = patternTable.getSelection();
	    		if (selectedPatternItems.length != 1) {
	    			return;
	    		}
	    		int patternIndex = patternTable.getSelectionIndex();
	    		if (filter.demote((ResourceMatcher) selectedPatternItems[0].getData())) {
	    			patternIndex++;
	    		}
	    		update();
	    		patternTable.select(patternIndex);
	    	}
	    });

	    removeButton = new Button(patternTableComposite, SWT.PUSH);
	    removeButton.setText(Messages.SyncFileTree_8);
	    removeButton.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
	    removeButton.addSelectionListener(new SelectionAdapter() {
	    	public void widgetSelected(SelectionEvent event) {
	    		TableItem[] selectedPatternItems = patternTable.getSelection();
	    		for (TableItem selectedPatternItem : selectedPatternItems) {
	    			ResourceMatcher selectedPattern = (ResourceMatcher) selectedPatternItem.getData();
	    			filter.removePattern(selectedPattern);
	    		}
	    		update();
	    	}
	    });
	    
	    //Composite for text box, combo, and buttons to enter a new pattern
	    Composite patternEnterComposite = new Composite(composite, SWT.FILL);
	    patternEnterComposite.setLayout(new GridLayout(4, false));
		patternEnterComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

		// Label for entering new path
		new Label(patternEnterComposite, SWT.NONE).setText(Messages.SyncFileTree_5);

	    // Text box to enter new path
	    newPath = new Text(patternEnterComposite, SWT.NONE);
	    newPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

	    // Submit buttons (exclude and include)
	    excludeButtonForPath = new Button(patternEnterComposite, SWT.PUSH);
	    excludeButtonForPath.setText(Messages.SyncFileTree_10);
	    excludeButtonForPath.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
	    excludeButtonForPath.addSelectionListener(new SelectionAdapter() {
	    	public void widgetSelected(SelectionEvent event) {
	    		enterNewPathPattern(PatternType.EXCLUDE);
	    	}
	    });

	    includeButtonForPath = new Button(patternEnterComposite, SWT.PUSH);
	    includeButtonForPath.setText(Messages.SyncFileTree_11);
	    includeButtonForPath.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
	    includeButtonForPath.addSelectionListener(new SelectionAdapter() {
	    	public void widgetSelected(SelectionEvent event) {
	    		enterNewPathPattern(PatternType.INCLUDE);
	    	}
	    });

		// Label for entering new regex
		new Label(patternEnterComposite, SWT.NONE).setText(Messages.SyncFileTree_9);

	    // Text box to enter new regex
	    newRegex = new Text(patternEnterComposite, SWT.NONE);
	    newRegex.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

	    // Submit buttons (exclude and include)
	    excludeButtonForRegex = new Button(patternEnterComposite, SWT.PUSH);
	    excludeButtonForRegex.setText(Messages.SyncFileTree_10);
	    excludeButtonForRegex.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
	    excludeButtonForRegex.addSelectionListener(new SelectionAdapter() {
	    	public void widgetSelected(SelectionEvent event) {
	    		enterNewRegexPattern(PatternType.EXCLUDE);
	    	}
	    });

	    includeButtonForRegex = new Button(patternEnterComposite, SWT.PUSH);
	    includeButtonForRegex.setText(Messages.SyncFileTree_11);
	    includeButtonForRegex.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
	    includeButtonForRegex.addSelectionListener(new SelectionAdapter() {
	    	public void widgetSelected(SelectionEvent event) {
	    		enterNewRegexPattern(PatternType.INCLUDE);
	    	}
	    });
	    
	    // Label for special filters combo
	    new Label(patternEnterComposite, SWT.NONE).setText(Messages.SyncFileTree_12);
	    // Combo for special filters
	    specialFiltersCombo = new Combo(patternEnterComposite, SWT.READ_ONLY);
		specialFiltersCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	    for (String filterName : specialFilterNameToPatternMap.keySet()) {
	    	specialFiltersCombo.add(filterName);
	    }
	    
	    // Submit buttons (exclude and include)
	    excludeButtonForSpecial = new Button(patternEnterComposite, SWT.PUSH);
	    excludeButtonForSpecial.setText(Messages.SyncFileTree_10);
	    excludeButtonForSpecial.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
	    excludeButtonForSpecial.addSelectionListener(new SelectionAdapter() {
	    	public void widgetSelected(SelectionEvent event) {
	    		enterNewSpecialPattern(PatternType.EXCLUDE);
	    	}
	    });

	    includeButtonForSpecial = new Button(patternEnterComposite, SWT.PUSH);
	    includeButtonForSpecial.setText(Messages.SyncFileTree_11);
	    includeButtonForSpecial.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
	    includeButtonForSpecial.addSelectionListener(new SelectionAdapter() {
	    	public void widgetSelected(SelectionEvent event) {
	    		enterNewSpecialPattern(PatternType.INCLUDE);
	    	}
	    });
	    
	    // Blank labels to occupy two spaces
	    new Label(patternEnterComposite, SWT.NONE).setVisible(false);
	    new Label(patternEnterComposite, SWT.NONE).setVisible(false);
	    
	    // Place for displaying error message if pattern is illegal
	    patternErrorLabel = new Label(patternEnterComposite, SWT.NONE);
	    patternErrorLabel.setForeground(display.getSystemColor(SWT.COLOR_DARK_RED));
		patternErrorLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

	    // Composite for cancel and OK buttons
	    Composite buttonComposite = new Composite(composite, SWT.FILL);
	    buttonComposite.setLayout(new GridLayout(2, false));
		buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

	    // Cancel button
	    cancelButton = new Button(buttonComposite, SWT.PUSH);
	    cancelButton.setText(Messages.SyncFileTree_13);
	    cancelButton.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, true, false));
	    cancelButton.addSelectionListener(new SelectionAdapter() {
	      public void widgetSelected(SelectionEvent event) {
	    	  getShell().close();
	      }
	    });
	    
	    // OK button
	    okButton = new Button(buttonComposite, SWT.PUSH);
	    okButton.setText(Messages.SyncFileTree_14);
	    okButton.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, false, false));
	    okButton.addSelectionListener(new SelectionAdapter() {
	      public void widgetSelected(SelectionEvent event) {
	    	  if (project == null) {
	    		  SyncManager.saveDefaultFileFilter(filter);
	    	  } else {
	    		  SyncManager.saveFileFilter(project, filter);
	    	  }
	    	  getShell().close();
	      }
	    });

	    update();
		return composite;
	}
	
	// Utility function to bold and resize labels to be headers
	private void formatAsHeader(Label widget) {
		FontData[] fontData = widget.getFont().getFontData();
		for (FontData data : fontData) {
			data.setStyle(SWT.BOLD);
		}

		final Font newFont = new Font(display, fontData);
		widget.setFont(newFont);
		widget.addDisposeListener(new DisposeListener() {
		    public void widgetDisposed(DisposeEvent e) {
		        newFont.dispose();
		    }
		});
	}
	
	private void enterNewPathPattern(PatternType type) {
		String pattern = newPath.getText();
		if (pattern.isEmpty()) {
			return;
		}

		PathResourceMatcher matcher = null;
		matcher = new PathResourceMatcher(new Path(pattern));
		filter.addPattern(matcher, type);

		newPath.setText(""); //$NON-NLS-1$
		update();
	}

	private void enterNewRegexPattern(PatternType type) {
		String pattern = newRegex.getText();
		if (pattern.isEmpty()) {
			return;
		}

		RegexResourceMatcher matcher = null;
		try {
			matcher = new RegexResourceMatcher(pattern);
		} catch (PatternSyntaxException e) {
			// Do nothing but display an error message for a few seconds
			patternErrorLabel.setText(Messages.SyncFileTree_15);
			display.timerExec(ERROR_DISPLAY_SECONDS*1000, new Runnable() {
				public void run() {
					if (patternErrorLabel.isDisposed ()) {
						return;
					}
					patternErrorLabel.setText(""); //$NON-NLS-1$
				}
			});
			return;
		}

		filter.addPattern(matcher, type);

		newRegex.setText(""); //$NON-NLS-1$
		update();
	}
	
	private void enterNewSpecialPattern(PatternType type) {
		int selectionIndex = specialFiltersCombo.getSelectionIndex();
		if (selectionIndex < 0) {
			return;
		}
		String filterName = specialFiltersCombo.getItem(selectionIndex);
		ResourceMatcher rm = specialFilterNameToPatternMap.get(filterName);
		if (rm != null) {
			filter.addPattern(rm, type);
		}
		
		specialFiltersCombo.deselectAll();
		update();
	}

	private void update() {
		boolean showRemote = showRemoteButton.getSelection();
		if (showRemote) {
			if (!((SFTTreeContentProvider) treeViewer.getContentProvider()).isConnected()) {
				showRemote = false;
				remoteErrorLabel.setText(Messages.SyncFileTree_19);
			} else {
				remoteErrorLabel.setText(""); //$NON-NLS-1$
			}
		}
		showRemoteButton.setSelection(showRemote);
		((SFTTreeContentProvider) treeViewer.getContentProvider()).setShowRemoteFiles(showRemote);

		patternTable.removeAll();
		for (ResourceMatcher pattern : filter.getPatterns()) {
			TableItem ti = new TableItem(patternTable, SWT.LEAD);
			ti.setData(pattern);

			String[] tableValues = new String[2];
			String patternType;
			if (filter.getPatternType(pattern) == PatternType.EXCLUDE) {
				patternType = Messages.SyncFileTree_21;
			} else {
				patternType = Messages.SyncFileTree_22;
			}
			if (pattern instanceof PathResourceMatcher) {
				patternType = patternType + " " + Messages.SyncFileTree_16; //$NON-NLS-1$
			} else if (pattern instanceof RegexResourceMatcher) {
				patternType = patternType + " " + Messages.SyncFileTree_17; //$NON-NLS-1$
			}
			patternType += ":  "; //$NON-NLS-1$

			tableValues[0] = patternType;
			tableValues[1] = pattern.toString();
			ti.setText(tableValues);
			
			// Italicize pattern
			FontData currentFontData = ti.getFont().getFontData()[0];
			Font italicizedFont = new Font(display, new FontData(currentFontData.getName(), currentFontData.getHeight(), SWT.ITALIC));
			ti.setFont(1, italicizedFont);

		}
		
		patternTable.getColumn(0).pack();
		patternTable.getColumn(1).pack();
		
		treeViewer.refresh();
	}

	private class SFTTreeContentProvider implements ITreeContentProvider {
		private final RemoteContentProvider remoteFiles;
		private boolean showRemoteFiles = false;

		public SFTTreeContentProvider() {
			IConfiguration bconf = ManagedBuildManager.getBuildInfo(project).getDefaultConfiguration();
			BuildScenario bs = BuildConfigurationManager.getInstance().getBuildScenarioForBuildConfiguration(bconf);
			if (bs == null) {
				// System error handled by BuildConfigurationManager
				remoteFiles = null;
			} else {
				remoteFiles = new RemoteContentProvider(bs.getRemoteConnection(), new Path(bs.getLocation()), project);
			}
		}

		/**
		 * Get whether remote files are displayed
		 */
		public boolean getShowRemoteFiles() {
			return showRemoteFiles;
		}

		/**
		 * Set displaying of remote files
		 * @param b
		 */
		public void setShowRemoteFiles(boolean b) {
			showRemoteFiles = b;
		}

		/**
		 * Gets the children of the specified object
		 * 
		 * @param element
		 *            the parent object
		 * @return Object[]
		 */
		public Object[] getChildren(Object element) {
			ArrayList<IResource> children = new ArrayList<IResource>();

			if (element instanceof IFolder) {
				if (((IFolder) element).isAccessible()) {
					try {
						for (IResource localChild : ((IFolder) element).members()) {
							children.add(localChild);
						}
					} catch (CoreException e) {
						assert(false); // This should never happen, since we check for existence before accessing the folder.
					}
				}
				
				if (showRemoteFiles && remoteFiles != null) {
					for (Object remoteChild : remoteFiles.getChildren(element)) {
						this.addUniqueResource(children, (IResource) remoteChild);
					}
				}
			}

			return children.toArray();
		}

		/**
		 * Gets the parent of the specified object
		 * 
		 * @param element
		 *            the object
		 * @return Object
		 */
		public Object getParent(Object element) {
			return ((IResource) element).getParent();
		}

		/**
		 * Returns whether the passed object has children
		 * 
		 * @param element
		 *            the parent object	private class SFTStyledCellLabelProvider extends 
		 * @return boolean
		 */
		public boolean hasChildren(Object element) {
			// Get the children
			Object[] obj = getChildren(element);

			// Return whether the parent has children
			return obj == null ? false : obj.length > 0;
		}

		/**
		 * Gets the root element(s) of the tree
		 * This code is very similar to "getChildren" but the root of the project tree requires special handling (no IFolder
		 * for the root).
		 * 
		 * @param element
		 *            the input data
		 * @return Object[]
		 */
		public Object[] getElements(Object element) {
			ArrayList<IResource> children = new ArrayList<IResource>();

			if (element instanceof IProject && ((IProject) element).isAccessible()) {
				try {
					for (IResource localChild : ((IProject) element).members()) {
						children.add(localChild);
					}
				} catch (CoreException e) {
					assert(false); // This should never happen, since we check for existence before accessing the project.
				}

				if (showRemoteFiles && remoteFiles != null) {
					for (Object remoteChild : remoteFiles.getElements(element)) {
						this.addUniqueResource(children, (IResource) remoteChild);
					}
				}
			}

			return children.toArray();
		}

		/**
		 * Disposes any created resources
		 */
		public void dispose() {
			// Nothing to dispose
		}

		/**
		 * Called when the input changes
		 * 
		 * @param element
		 *            the viewer
		 * @param arg1
		 *            the old input
		 * @param arg2
		 *            the new input
		 */
		public void inputChanged(Viewer element, Object arg1, Object arg2) {
			// Nothing to do
		}
		
		// Utility function to add resources to a list only if it is unique.
		// Returns whether the resource was added.
		private boolean addUniqueResource(Collection<IResource> resList, IResource newRes) {
			for (IResource res : resList) {
				if (res.getProjectRelativePath().equals(newRes.getProjectRelativePath())) {
					return false;
				}
			}
			
			resList.add(newRes);
			return true;
		}

		/**
		 * Check that connection is still open - useful for client to inform user when the connection goes down.
		 * @return whether connection is still open
		 */
		public boolean isConnected() {
			if (remoteFiles == null) {
				return false;
			} else {
				return remoteFiles.isOpen();
			}
		}
	}

	private class SFTTreeLabelProvider implements ILabelProvider {
		// Images for tree nodes
		private final Image folderImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
		private final Image fileImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);

		/**
		 * Gets the image to display for a node in the tree
		 * 
		 * @param element
		 *            the node
		 * @return Image
		 */
		public Image getImage(Object element) {
			if (element instanceof IFolder) {
				return folderImage;
			} else {
				return fileImage;
			}
		}

		/**
		 * Gets the text to display for a node in the tree
		 * 
		 * @param element
		 *            the node
		 * @return String
		 */
		public String getText(Object element) {
			return ((IResource) element).getName();
		}

		/**
		 * Called when this LabelProvider is being disposed
		 */
		public void dispose() {
			// Nothing to dispose
		}

		/**
		 * Returns whether changes to the specified property on the specified
		 * element would affect the label for the element
		 * 
		 * @param element
		 *            the element
		 * @param arg1
		 *            the property
		 * @return boolean
		 */
		public boolean isLabelProperty(Object element, String arg1) {
			return false;
		}

		public void addListener(ILabelProviderListener listener) {
			// Listeners not supported
			
		}

		public void removeListener(ILabelProviderListener listener) {
			// Listeners not supported
		}
	}
}