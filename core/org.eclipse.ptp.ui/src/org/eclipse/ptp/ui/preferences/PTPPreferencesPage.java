/*******************************************************************************
 * Copyright (c) 2005 The Regents of the University of California. 
 * This material was produced under U.S. Government contract W-7405-ENG-36 
 * for Los Alamos National Laboratory, which is operated by the University 
 * of California for the U.S. Department of Energy. The U.S. Government has 
 * rights to use, reproduce, and distribute this software. NEITHER THE 
 * GOVERNMENT NOR THE UNIVERSITY MAKES ANY WARRANTY, EXPRESS OR IMPLIED, OR 
 * ASSUMES ANY LIABILITY FOR THE USE OF THIS SOFTWARE. If software is modified 
 * to produce derivative works, such modified software should be clearly marked, 
 * so as not to confuse it with the version available from LANL.
 * 
 * Additionally, this program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * LA-CC 04-115
 *******************************************************************************/
package org.eclipse.ptp.ui.preferences;

import java.io.File;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ptp.core.PTPCorePlugin;
import org.eclipse.ptp.core.PreferenceConstants;
import org.eclipse.ptp.core.Preferences;
import org.eclipse.ptp.ui.messages.Messages;
import org.eclipse.ptp.utils.ui.swt.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PTPPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {
	public static final String EMPTY_STRING = ""; //$NON-NLS-1$
	protected Text outputDirText = null;
	protected Button browseButton = null;
	protected IntegerFieldEditor storeLineField = null;
	private String outputDIR = EMPTY_STRING;
	private final String defaultOutputDIR = "/tmp"; //$NON-NLS-1$
	private int storeLine = PreferenceConstants.DEFAULT_STORE_LINES;

	protected class WidgetListener extends SelectionAdapter implements ModifyListener, IPropertyChangeListener {
		@Override
		public void widgetSelected(SelectionEvent e) {
			Object source = e.getSource();
			if (source == browseButton)
				handleOutputDirectoryBrowseButtonSelected();
			else
				updatePreferencePage();
		}

		public void modifyText(ModifyEvent evt) {
			updatePreferencePage();
		}

		public void propertyChange(PropertyChangeEvent event) {
			if (event.getProperty().equals(FieldEditor.IS_VALID))
				updatePreferencePage();
		}
	}

	protected WidgetListener listener = new WidgetListener();

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(1, true, 0, 0));
		composite.setLayoutData(spanGridData(GridData.FILL_HORIZONTAL, 2));
		createOutputContents(composite);
		loadSaved();
		defaultSetting();
		return composite;
	}

	protected Button createCheckButton(Composite parent, String label) {
		return createButton(parent, label, SWT.CHECK | SWT.LEFT);
	}

	protected Button createButton(Composite parent, String label, int type) {
		Button button = new Button(parent, type);
		button.setText(label);
		GridData data = new GridData();
		button.setLayoutData(data);
		return button;
	}

	private void createOutputContents(Composite parent) {
		Group aGroup = new Group(parent, SWT.SHADOW_ETCHED_IN);
		aGroup.setLayout(createGridLayout(1, true, 10, 10));
		aGroup.setLayoutData(spanGridData(GridData.FILL_HORIZONTAL, 2));
		aGroup.setText(Messages.PTPPreferencesPage_0);
		Composite outputComposite = new Composite(aGroup, SWT.NONE);
		outputComposite.setLayout(createGridLayout(3, false, 0, 0));
		outputComposite.setLayoutData(spanGridData(GridData.FILL_HORIZONTAL, 5));
		new Label(outputComposite, SWT.NONE).setText(Messages.PTPPreferencesPage_1);
		outputDirText = new Text(outputComposite, SWT.SINGLE | SWT.BORDER);
		outputDirText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		outputDirText.addModifyListener(listener);
		browseButton = SWTUtil.createPushButton(outputComposite, Messages.PTPPreferencesPage_2, null);
		browseButton.addSelectionListener(listener);
		Composite lineComposite = new Composite(aGroup, SWT.NONE);
		lineComposite.setLayout(new FillLayout());
		lineComposite.setLayoutData(spanGridData(GridData.FILL_HORIZONTAL, 5));
		storeLineField = new IntegerFieldEditor(PreferenceConstants.PREFS_STORE_LINES, Messages.PTPPreferencesPage_3, lineComposite);
		storeLineField.setPropertyChangeListener(listener);
		storeLineField.setEmptyStringAllowed(false);
	}

	protected void defaultSetting() {
		outputDirText.setText(outputDIR);
		storeLineField.setStringValue(String.valueOf(storeLine));
	}

	private void loadSaved() {
		outputDIR = Preferences.getString(PTPCorePlugin.getUniqueIdentifier(), PreferenceConstants.PREFS_OUTPUT_DIR);
		if (outputDIR.equals("")) //$NON-NLS-1$
			outputDIR = defaultOutputDIR;
		if (outputDIR != null)
			outputDirText.setText(outputDIR);
		storeLine = Preferences.getInt(PTPCorePlugin.getUniqueIdentifier(), PreferenceConstants.PREFS_STORE_LINES);
		storeLineField.setStringValue(String.valueOf(storeLine));
	}

	/* do stuff on init() of preferences, if anything */
	public void init(IWorkbench workbench) {
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	public void performDefaults() {
		defaultSetting();
		updateApplyButton();
	}

	private void store() {
		outputDIR = outputDirText.getText();
		storeLine = storeLineField.getIntValue();
	}

	@Override
	public boolean performOk() {
		store();
		Preferences.setString(PTPCorePlugin.getUniqueIdentifier(), PreferenceConstants.PREFS_OUTPUT_DIR, outputDIR);
		Preferences.setInt(PTPCorePlugin.getUniqueIdentifier(), PreferenceConstants.PREFS_STORE_LINES, storeLine);
		Preferences.savePreferences(PTPCorePlugin.getUniqueIdentifier());
		/*
		 * IModelManager manager = PTPCorePlugin.getDefault().getModelManager();
		 * if (manager.isParallelPerspectiveOpen() && (lastMSChoiceID !=
		 * MSChoiceID || lastCSChoiceID != CSChoiceID)) {
		 * manager.refreshRuntimeSystems(CSChoiceID, MSChoiceID); }
		 */
		File outputDirPath = new File(outputDIR);
		if (!outputDirPath.exists())
			outputDirPath.mkdir();
		return true;
	}

	protected void handleOutputDirectoryBrowseButtonSelected() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setText(Messages.PTPPreferencesPage_4);
		String currectDirPath = getFieldContent(outputDirText.getText());
		if (currectDirPath != null) {
			File path = new File(currectDirPath);
			if (path.exists())
				dialog.setFilterPath(currectDirPath);
		}
		String selectedDirPath = dialog.open();
		if (selectedDirPath != null)
			outputDirText.setText(selectedDirPath);
	}

	protected boolean isValidOutputSetting() {
		String name = getFieldContent(outputDirText.getText());
		if (name == null) {
			setErrorMessage(Messages.PTPPreferencesPage_5);
			setValid(false);
			return false;
		}
		File path = new File(name);
		if (!path.exists()) {
			File parent = path.getParentFile();
			if (parent == null || !parent.exists()) {
				setErrorMessage(Messages.PTPPreferencesPage_5);
				setValid(false);
				return false;
			}
		}
		if (!storeLineField.isValid()) {
			setErrorMessage(storeLineField.getErrorMessage());
			setValid(false);
			return false;
		}
		return true;
	}

	protected void updatePreferencePage() {
		setErrorMessage(null);
		setMessage(null);
		if (!isValidOutputSetting())
			return;
		setValid(true);
	}

	protected String getFieldContent(String text) {
		if (text.trim().length() == 0 || text.equals(EMPTY_STRING))
			return null;
		return text;
	}

	protected GridLayout createGridLayout(int columns, boolean isEqual, int mh, int mw) {
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = columns;
		gridLayout.makeColumnsEqualWidth = isEqual;
		gridLayout.marginHeight = mh;
		gridLayout.marginWidth = mw;
		return gridLayout;
	}

	protected GridData spanGridData(int style, int space) {
		GridData gd = null;
		if (style == -1)
			gd = new GridData();
		else
			gd = new GridData(style);
		gd.horizontalSpan = space;
		return gd;
	}
}
