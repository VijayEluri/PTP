/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.ptp.rdt.ui.wizards;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.ptp.rdt.ui.messages.Messages;
import org.eclipse.ptp.rdt.ui.wizards.DStoreServerWidget.FieldModifier;
import org.eclipse.ptp.remote.core.IRemoteConnection;
import org.eclipse.ptp.remote.core.IRemoteServices;
import org.eclipse.ptp.remote.ui.widgets.RemoteDirectoryWidget;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class DStoreIndexPage extends Composite {
	private String fDefaultPath = null;
	private RemoteDirectoryWidget fDirectoryWidget;
	private ListenerList modifyListeners = new ListenerList();
	
	public DStoreIndexPage(Composite parent, int style) {
		super(parent, style);
		
		GridLayout layout = new GridLayout(2, false);
		setLayout(layout);
		setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fDirectoryWidget = new RemoteDirectoryWidget(this, SWT.NONE, Messages.getString("IndexFileLocationWidget.0"), fDefaultPath); //$NON-NLS-1$
		fDirectoryWidget.setBrowseMessage("Select Index File Location"); //$NON-NLS-1$
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
        data.horizontalSpan = 3;
        fDirectoryWidget.setLayoutData(data); // set layout to grab horizontal space
        fDirectoryWidget.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fDefaultPath = fDirectoryWidget.getLocationPath();
				e.data = FieldModifier.VALUE_INDEX_LOCATION;
				notifyListeners(e);
			}
		});
	}
	
	public void addModifyListener(ModifyListener listener) {
		modifyListeners.add(listener);
	}

	public String getLocationPath() {
		return fDirectoryWidget.getLocationPath();
	}
	
	public void removeModifyListener(ModifyListener listener) {
		modifyListeners.remove(listener);
	}
	
	public void setConnection(IRemoteServices services, IRemoteConnection conn) {
		fDirectoryWidget.setConnection(services, conn);
	}

	public void setLocationPath(String path) {
		fDirectoryWidget.setLocationPath(path);
	}
	
	private void notifyListeners(ModifyEvent e) {
		for (Object listener : modifyListeners.getListeners()) {
			((ModifyListener)listener).modifyText(e);
		}
	}
}
