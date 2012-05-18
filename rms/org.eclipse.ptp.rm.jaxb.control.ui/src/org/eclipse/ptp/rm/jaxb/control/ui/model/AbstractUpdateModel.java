/*******************************************************************************
 * Copyright (c) 2011 University of Illinois All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html 
 * 	
 * Contributors: 
 * 	Albert L. Rossi - design and implementation
 ******************************************************************************/
package org.eclipse.ptp.rm.jaxb.control.ui.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ptp.remote.core.IRemoteFileManager;
import org.eclipse.ptp.remote.core.RemoteServicesDelegate;
import org.eclipse.ptp.rm.jaxb.control.ui.IUpdateModel;
import org.eclipse.ptp.rm.jaxb.control.ui.JAXBControlUIConstants;
import org.eclipse.ptp.rm.jaxb.control.ui.handlers.ValueUpdateHandler;
import org.eclipse.ptp.rm.jaxb.control.ui.launch.IJAXBParentLaunchConfigurationTab;
import org.eclipse.ptp.rm.jaxb.control.ui.utils.WidgetActionUtils;
import org.eclipse.ptp.rm.jaxb.control.ui.variables.LCVariableMap;
import org.eclipse.ptp.rm.jaxb.core.IVariableMap;
import org.eclipse.ptp.rm.jaxb.core.data.ValidatorType;
import org.eclipse.ptp.rm.jaxb.ui.JAXBUIConstants;
import org.eclipse.ui.progress.UIJob;

/**
 * Base class for implementations of the IUpdateModel controlling the data associated with a widget or cell editor.
 * 
 * @author arossi
 * 
 */
public abstract class AbstractUpdateModel implements IUpdateModel {

	/**
	 * Used with ModifyListeners so as to avoid a save on every keystroke.
	 * 
	 * @author arossi
	 */
	protected class ValidateJob extends UIJob {
		public ValidateJob() {
			super(JAXBControlUIConstants.VALIDATE);
			setSystem(true);
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			try {
				Object value = storeValue();
				handleUpdate(value);
			} catch (Exception ignored) {
			}
			return Status.OK_STATUS;
		}
	}

	protected final Job validateJob;

	protected boolean canSave;
	protected String name;
	protected LCVariableMap lcMap;
	protected ValueUpdateHandler handler;
	protected boolean refreshing;
	protected ValidatorType validator;
	protected IJAXBParentLaunchConfigurationTab tab;
	protected String defaultValue;
	protected String[] booleanToString;
	protected Object mapValue;

	/**
	 * @param name
	 *            name of the model, which will correspond to the name of a Property or Attribute if the widget value is to be saved
	 * @param handler
	 *            the handler for notifying other widgets to refresh their values
	 */
	protected AbstractUpdateModel(String name, ValueUpdateHandler handler) {
		this.name = name;
		canSave = (name != null && !JAXBControlUIConstants.ZEROSTR.equals(name));
		this.handler = handler;
		refreshing = false;
		validateJob = new ValidateJob();
	}

	/**
	 * @return The widget or cell editor.
	 */
	public abstract Object getControl();

	/**
	 * @return name of the model, which will correspond to the name of a Property or Attribute if the widget value is to be saved.
	 */
	public String getName() {
		return name;
	}

	/**
	 * If this widget saves its value to a Property or Attribute, then the default value here is retrieved. The widget value is then
	 * refreshed from the map, and if the value is <code>null</code>, the default value is restored to the map and another refresh
	 * is called on the actual value.
	 */
	public void initialize(IVariableMap rmMap, LCVariableMap lcMap) {
		this.lcMap = lcMap;
		if (name != null) {
			defaultValue = lcMap.getDefault(name);
		}
		refreshValueFromMap();
		if (mapValue == null) {
			restoreDefault();
			refreshValueFromMap();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.rm.jaxb.control.ui.IUpdateModel#isWritable()
	 */
	public boolean isWritable() {
		return canSave;
	}

	/**
	 * Sets the actual value in the current environment to the default value.
	 */
	public void restoreDefault() {
		lcMap.setDefault(name, defaultValue);
	}

	/**
	 * @param validator
	 *            JAXB data element describing either regex or efs validation for the widget value.
	 * @param tab
	 *            provided in case the validation is to be done on a file path; the delegate must be retrieved lazily as the tab is
	 *            initialized after the widgets are constructed
	 */
	public void setValidator(ValidatorType validator, IJAXBParentLaunchConfigurationTab tab) {
		this.validator = validator;
		this.tab = tab;
	}

	/**
	 * Used for boolean widgets. Checks for string translation
	 * 
	 * @param b
	 *            the boolean that may need to be translated
	 * @return either the boolean value or its translation
	 */
	protected Object getBooleanValue(Boolean b) {
		if (booleanToString != null) {
			return b ? booleanToString[0] : booleanToString[1];
		}
		return b;
	}

	/**
	 * Delegates to the handler update method.
	 * 
	 * @see org.eclipse.ptp.rm.jaxb.control.ui.handlers.ValueUpdateHandler#handleUpdate(Object, Object)
	 * 
	 * @param value
	 *            updated value (currently unused)
	 */
	protected void handleUpdate(Object value) {
		handler.handleUpdate(getControl(), value);
	}

	/**
	 * Used by boolean widgets. Checks for string translation.
	 * 
	 * @param mapValue
	 *            either the boolean value or its translation
	 * @return the actual boolean value
	 */
	protected boolean maybeGetBooleanFromString(Object mapValue) {
		boolean b = false;
		if (mapValue != null) {
			if (mapValue instanceof String) {
				if (booleanToString != null) {
					b = mapValue.equals(booleanToString[0]);
				} else {
					b = Boolean.parseBoolean((String) mapValue);
				}
			} else {
				b = (Boolean) mapValue;
			}
		}
		return b;
	}

	/**
	 * If this value type is boolean and it is mapped to a string pairing, set the mapping.
	 * 
	 * @param translateBooleanAs
	 *            comma-separated pair of values corresponding to T,F
	 */
	protected void setBooleanToString(String translateBooleanAs) {
		if (translateBooleanAs == null) {
			booleanToString = null;
		} else {
			String[] pair = translateBooleanAs.split(JAXBUIConstants.CM);
			if (pair.length == 2) {
				booleanToString = pair;
			} else {
				booleanToString = null;
			}
		}
	}

	/**
	 * Retrieves the value from the control, then writes to the current environment map and calls the update handler. <br>
	 * <br>
	 */
	protected Object storeValue() throws Exception {
		Object value = validate();
		lcMap.putValue(name, value);
		return value;
	}

	/**
	 * Retrieves manager lazily from tab.
	 * 
	 * @return remote file manager, or <code>null</code> if undefined
	 */
	private IRemoteFileManager getRemoteFileManager() {
		if (tab != null) {
			RemoteServicesDelegate d = tab.getRemoteServicesDelegate();
			if (d != null) {
				return d.getRemoteFileManager();
			}
		}
		return null;
	}

	/**
	 * Gets value from control and runs validator on it, if there is one. If there is an error, this is registered with the handler.
	 * 
	 * @return valid value
	 * @throws Exception
	 *             thrown if invalid
	 */
	private Object validate() throws Exception {
		Object value = getValueFromControl();
		String error = null;
		if (validator != null) {
			try {
				WidgetActionUtils.validate(String.valueOf(getValueFromControl()), validator, getRemoteFileManager());
			} catch (Exception t) {
				error = validator.getErrorMessage();
			}
		}
		if (error != null) {
			handler.addError(name, error);
			throw new Exception(error);
		} else {
			handler.removeError(name);
		}
		return value;
	}
}
