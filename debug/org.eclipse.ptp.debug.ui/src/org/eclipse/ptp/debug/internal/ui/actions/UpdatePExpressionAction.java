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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.ptp.debug.internal.ui.PDebugImage;
import org.eclipse.ptp.debug.internal.ui.views.variable.PVariableView;
import org.eclipse.ptp.debug.ui.messages.Messages;

/**
 * @author Clement chu
 */
public class UpdatePExpressionAction extends Action {
	public static final String name = Messages.UpdatePExpressionAction_0;
	private PVariableView view = null;

	/** Constructor
	 * @param view
	 */
	public UpdatePExpressionAction(PVariableView view) {
		super(name, IAction.AS_PUSH_BUTTON);
	    setImageDescriptor(PDebugImage.getDescriptor(PDebugImage.ICON_VAR_REFRESH_NORMAL));
	    //setDisabledImageDescriptor(PDebugImage.ID_ICON_VAR_REFRESH_DISABLE);
	    setToolTipText(name);
	    setId(name);
	    setEnabled(false);
	    this.view = view;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		view.getUIManager().updateCurrentJobVariableValues();
	}
}
