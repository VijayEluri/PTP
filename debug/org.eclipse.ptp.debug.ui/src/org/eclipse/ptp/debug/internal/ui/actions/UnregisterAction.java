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
package org.eclipse.ptp.debug.internal.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.debug.internal.ui.PDebugImage;
import org.eclipse.ptp.debug.ui.PTPDebugUIPlugin;
import org.eclipse.ptp.debug.ui.messages.Messages;
import org.eclipse.ptp.debug.ui.views.ParallelDebugView;
import org.eclipse.ptp.ui.model.IElement;
/**
 * @author clement chu
 *
 */
public class UnregisterAction extends DebugAction {
	public static final String name = Messages.UnregisterAction_0;
	
	/** Constructor
	 * @param view
	 */
	public UnregisterAction(ParallelDebugView view) {
		super(name, view);
	    setImageDescriptor(PDebugImage.getDescriptor(PDebugImage.ICON_UNREGISTER_NORMAL));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ptp.ui.actions.ParallelAction#run(org.eclipse.ptp.ui.model.IElement[])
	 */
	public void run(IElement[] elements) {
		if (validation(elements)) {
			try {
				view.unregisterSelectedElements();
				view.refresh(false);
			} catch (CoreException e) {
				PTPDebugUIPlugin.errorDialog(getShell(), Messages.UnregisterAction_1, e.getStatus());				
			}
		}
	}	
}
