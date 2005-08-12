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
package org.eclipse.ptp.debug.ui;

import org.eclipse.cdt.debug.core.cdi.ICDISession;
import org.eclipse.cdt.debug.core.cdi.event.ICDICreatedEvent;
import org.eclipse.cdt.debug.core.cdi.event.ICDIEvent;
import org.eclipse.cdt.debug.core.cdi.event.ICDIEventListener;
import org.eclipse.cdt.debug.core.cdi.event.ICDISuspendedEvent;
import org.eclipse.cdt.debug.core.model.ICLineBreakpoint;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.ptp.core.IPJob;
import org.eclipse.ptp.core.IPProcess;
import org.eclipse.ptp.debug.core.PTPDebugCorePlugin;
import org.eclipse.ptp.debug.core.breakpoints.PBreakpointManager;
import org.eclipse.ptp.debug.core.cdi.IPCDISession;
import org.eclipse.ptp.debug.core.cdi.event.IPCDIEvent;
import org.eclipse.ptp.ui.JobManager;
import org.eclipse.ptp.ui.PTPUIPlugin;
import org.eclipse.ptp.ui.listeners.ISetListener;
import org.eclipse.ptp.ui.model.IElement;
import org.eclipse.ptp.ui.model.IElementSet;

/**
 * @author clement chu
 *
 */
public class UIDebugManager extends JobManager implements ISetListener, IBreakpointListener, ICDIEventListener {
	public final static int PROC_SUSPEND = 6;
	public final static int PROC_HIT = 7;
	
	private PBreakpointManager bptManager = null;

	public UIDebugManager() {
		PTPUIPlugin.getDefault().getUIManager().addSetListener(this);
		bptManager = PBreakpointManager.getDefault();
		bptManager.getBreakpointManager().addBreakpointListener(this);
		//PTPDebugCorePlugin.getDefault().addDebugSessionListener(this);
	}
	
	public void shutdown() {
		PTPUIPlugin.getDefault().getUIManager().removeSetListener(this);
		bptManager.getBreakpointManager().removeBreakpointListener(this);
		//PTPDebugCorePlugin.getDefault().removeDebugSessionListener(this);
		super.shutdown();
	}
	
	public void jobChangedEvent(String cur_jid, String pre_jid) {
		super.jobChangedEvent(cur_jid, pre_jid);
		createEventListener(cur_jid);
	}

	public void createEventListener(String job_id) {
		ICDISession session = getDebugSession(job_id);
		if (session != null)
			session.getEventManager().addEventListener(this);
	}
	
	public void removeEventListener(String job_id) {
		ICDISession session = getDebugSession(job_id);
		if (session != null)
			session.getEventManager().removeEventListener(this);		
	}
	
	public ICDISession getDebugSession(String job_id) {
		if (isNoJob(job_id))
			return null;

		IPJob job = findJobById(job_id);
		return PTPDebugCorePlugin.getDefault().getDebugSession(job);
	}

	public void unregisterElements(IElement[] elements) {
		for (int i=0; i<elements.length; i++) {
			//only unregister some registered elements
			if (elements[i].isRegistered()) {
				((IPCDISession)getDebugSession(getCurrentJobId())).unregisterTarget(Integer.parseInt(elements[i].getName()));
				//System.out.println("Unregister: " + elements[i].getID());
			}
		}
	}
	
	public void registerElements(IElement[] elements) {
		for (int i=0; i<elements.length; i++) {
			//only register some unregistered elements
			if (!elements[i].isRegistered()) {
				((IPCDISession)getDebugSession(getCurrentJobId())).registerTarget(Integer.parseInt(elements[i].getName()));
				//System.out.println("Register: " + elements[i].getID());
			}
		}
	}
	
	/******
	 * Breakpoint
	 ******/
	public void breakpointAdded(IBreakpoint breakpoint) {
		if (PTPDebugUIPlugin.getDefault().getCurrentPerspectiveID().equals(IPTPDebugUIConstants.PERSPECTIVE_DEBUG)) {
			if (breakpoint instanceof ICLineBreakpoint) {
				try {
					bptManager.addRemoveBreakpoint((ICLineBreakpoint)breakpoint, getCurrentSetId(), getCurrentJobId());
					bptManager.getBreakpointManager().removeBreakpoint(breakpoint, true);
				} catch (CoreException e) {
					System.out.println("Err: " + e.getMessage());
				}
			}
		}
	}
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {}
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {}	
	
    		/*
    		 * Cannot unregister the extension
    		final String CDT_DEBUG_UI_ID = "org.eclipse.cdt.debug.ui";    		
		Bundle bundle = Platform.getBundle(CDT_DEBUG_UI_ID);
		if (bundle != null && bundle.getState() == Bundle.ACTIVE) {
			//ExtensionRegistry reg = (ExtensionRegistry) Platform.getExtensionRegistry();
			//reg.remove(bundle.getBundleId());
			try {
				Platform.getBundle(CDT_DEBUG_UI_ID).uninstall();
				System.out.println("Remove: " + bundle.getState());
			} catch (BundleException e) {
				System.out.println("Err: " + e.getMessage());
			}
		} 
		if (bundle != null && bundle.getState() == Bundle.UNINSTALLED) {
			try {
				Platform.getBundle(CDT_DEBUG_UI_ID).start();
				System.out.println("Add: " + bundle.getState());
			} catch (BundleException e) {
				System.out.println("Err: " + e.getMessage());
			}
		}
		*/
	
	/*****
	 * Event
	 *****/
	public void handleDebugEvents(ICDIEvent[] events) {
		for (int i=0; i<events.length; i++) {
			IPCDIEvent event = (IPCDIEvent)events[i];
			
			int[] processes = event.getProcesses();			
			String job_id = getCurrentJobId();
			if (event instanceof ICDISuspendedEvent) {
				for (int j=0; j<processes.length; j++) {
					IPProcess process = findProcessbyName(job_id, String.valueOf(processes[j]));
					System.out.println("---------------- process suspend: " + process.getID());
				}
			}
			else if (event instanceof ICDICreatedEvent) {
				for (int j=0; j<processes.length; j++) {
					IPProcess process = findProcessbyName(job_id, String.valueOf(processes[j]));
					System.out.println("---------------- process created: " + process.getID());
				}
			}
			
			/*
			if (source != null && source.getTarget().equals(getDebugTarget().getCDITarget())) {
				if (event instanceof ICDICreatedEvent) {
					if (source instanceof ICDIBreakpoint)
						handleBreakpointCreatedEvent((ICDIBreakpoint)source);
				}
				else if (event instanceof ICDIDestroyedEvent) {
					if (source instanceof ICDIBreakpoint)
						handleBreakpointDestroyedEvent((ICDIBreakpoint)source);
				}
				else if (event instanceof ICDIChangedEvent) {
					if (source instanceof ICDIBreakpoint)
						handleBreakpointChangedEvent((ICDIBreakpoint)source);
				}
			}
			
			if (event instanceof ICDIExitedEvent) {
				Object obj = ((ICDIExitedEvent)event).getSource();
				
			}
			else if (event instanceof ICDICreatedEvent) {
				
			}
			else if (event instanceof ICDISuspendedEvent) {
				
			}
			else {
				
			}
			*/
		}
	}
	
	public void update(IPCDISession session) {
	}
	
	/*****
	 * Element Set
	 *****/
	public void updateBreakpointMarker() {
		IBreakpoint[] breakpoints = bptManager.getPBreakpoints();
		for (int i=1; i<breakpoints.length; i++) {
			try {
				breakpoints[i].setEnabled(true);
			} catch (CoreException e) {}
		}
	}
	public void changeSetEvent(IElementSet currentSet, IElementSet preSet) {
		
	}
	public void createSetEvent(IElementSet set, IElement[] elements) {
		
	}
	public void deleteSetEvent(IElementSet set) {
		
	}
	public void addElementsEvent(IElementSet set, IElement[] elements) {
		
	}
	public void removeElementsEvent(IElementSet set, IElement[] elements) {
		
	}
}
