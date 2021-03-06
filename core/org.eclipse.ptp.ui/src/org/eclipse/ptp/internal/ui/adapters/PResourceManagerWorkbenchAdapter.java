/*******************************************************************************
 * Copyright (c) 2007 The Regents of the University of California. 
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
package org.eclipse.ptp.internal.ui.adapters;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ptp.core.elements.IPResourceManager;
import org.eclipse.ptp.internal.ui.ParallelImages;
import org.eclipse.ptp.rmsystem.IResourceManager;
import org.eclipse.ptp.ui.IRuntimeModelPresentation;
import org.eclipse.ptp.ui.PTPUIPlugin;
import org.eclipse.ptp.utils.ui.ImageImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchAdapter;

public class PResourceManagerWorkbenchAdapter extends WorkbenchAdapter {

	/**
	 * @param parentElement
	 * @return
	 */
	private IPResourceManager getResourceManager(Object parentElement) {
		IPResourceManager rm = null;
		if (parentElement instanceof IAdaptable) {
			rm = (IPResourceManager) ((IAdaptable) parentElement).getAdapter(IPResourceManager.class);
		}
		return rm;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.model.WorkbenchAdapter#getImageDescriptor(java.lang.Object
	 * )
	 */
	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		final IRuntimeModelPresentation presentation = PTPUIPlugin.getDefault().getRuntimeModelPresentation(
				((IPResourceManager) object).getResourceManager().getResourceManagerId());
		if (presentation != null) {
			final Image image = presentation.getImage(object);
			if (image != null) {
				return new ImageImageDescriptor(image);
			}
		}
		final String state = ((IPResourceManager) object).getResourceManager().getState();
		return new ImageImageDescriptor(ParallelImages.rmImages.get(state));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.model.WorkbenchAdapter#getLabel(java.lang.Object)
	 */
	@Override
	public String getLabel(Object object) {
		final IRuntimeModelPresentation presentation = PTPUIPlugin.getDefault().getRuntimeModelPresentation(
				((IPResourceManager) object).getResourceManager().getResourceManagerId());
		if (presentation != null) {
			final String label = presentation.getText(object);
			if (label != null) {
				return label;
			}
		}
		final IResourceManager resourceManager = ((IPResourceManager) object).getResourceManager();
		final String type = resourceManager.getConfiguration().getType();
		if (type == null) {
			return resourceManager.getName();
		}
		return resourceManager.getName() + " (" + type + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.model.WorkbenchAdapter#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object object) {
		IPResourceManager rm = getResourceManager(object);
		if (rm != null) {
			return rm.getParent();
		}
		return null;
	}
}
