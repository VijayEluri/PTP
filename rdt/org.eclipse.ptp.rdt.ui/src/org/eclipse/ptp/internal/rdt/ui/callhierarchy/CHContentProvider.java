/*******************************************************************************
 * Copyright (c) 2006, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *    IBM Corporation
 *******************************************************************************/ 

/* -- ST-Origin --
 * Source folder: org.eclipse.cdt.ui/src
 * Class: org.eclipse.cdt.internal.ui.callhierarchy.CHContentProvider
 * Version: 1.21
 */
package org.eclipse.ptp.internal.rdt.ui.callhierarchy;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IEnumerator;
import org.eclipse.cdt.core.index.IIndexFile;
import org.eclipse.cdt.core.index.IIndexName;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.IMethod;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.core.model.IVariable;
import org.eclipse.cdt.internal.corext.util.CModelUtil;
import org.eclipse.cdt.internal.ui.callhierarchy.CHMultiDefNode;
import org.eclipse.cdt.internal.ui.callhierarchy.CHNode;
import org.eclipse.cdt.internal.ui.callhierarchy.CHReferenceInfo;
import org.eclipse.cdt.internal.ui.viewsupport.AsyncTreeContentProvider;
import org.eclipse.cdt.internal.ui.viewsupport.WorkingSetFilterUI;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ptp.internal.rdt.core.callhierarchy.CElementSet;
import org.eclipse.ptp.internal.rdt.core.callhierarchy.CalledByResult;
import org.eclipse.ptp.internal.rdt.core.callhierarchy.CallsToResult;
import org.eclipse.ptp.internal.rdt.core.callhierarchy.ICallHierarchyService;
import org.eclipse.ptp.internal.rdt.core.model.Scope;
import org.eclipse.ptp.rdt.core.serviceproviders.IIndexServiceProvider;
import org.eclipse.ptp.rdt.core.services.IRDTServiceConstants;
import org.eclipse.ptp.services.core.IService;
import org.eclipse.ptp.services.core.IServiceConfiguration;
import org.eclipse.ptp.services.core.IServiceModelManager;
import org.eclipse.ptp.services.core.IServiceProvider;
import org.eclipse.ptp.services.core.ServiceModelManager;
import org.eclipse.swt.widgets.Display;

/** 
 * This is the content provider for the call hierarchy.
 */
public class CHContentProvider extends AsyncTreeContentProvider {

	private static final IProgressMonitor NPM = new NullProgressMonitor();
	private boolean fComputeReferencedBy = true;
	private WorkingSetFilterUI fFilter;
	private RemoteCHViewPart fView;

	/**
	 * Constructs the content provider.
	 */
	public CHContentProvider(RemoteCHViewPart view, Display disp) {
		super(disp);
		fView= view;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof CHNode) {
			CHNode node = (CHNode) element;
			return node.getParent();
		}
		return super.getParent(element);
	}

	@Override
	protected Object[] syncronouslyComputeChildren(Object parentElement) {
		if (parentElement instanceof CHMultiDefNode) {
			return ((CHMultiDefNode) parentElement).getChildNodes();
		}
		if (parentElement instanceof CHNode) {
			CHNode node = (CHNode) parentElement;
			if (node.isRecursive() || node.getRepresentedDeclaration() == null) {
				return NO_CHILDREN;
			}
			if (fComputeReferencedBy) {
				if (node.isInitializer()) {
					return NO_CHILDREN;
				}
			}
			else if (node.isVariableOrEnumerator() || node.isMacro()) { 
				return NO_CHILDREN;
			}
			
		}
		// allow for async computation
		return null;
	}

	@Override
	protected Object[] asyncronouslyComputeChildren(Object parentElement, IProgressMonitor monitor) {
		try {
			if (parentElement instanceof ICElement) {
				return asyncComputeRoot((ICElement) parentElement);
			}

			if (parentElement instanceof CHNode) {
				CHNode node = (CHNode) parentElement;
				if (fComputeReferencedBy) {
					return asyncronouslyComputeReferencedBy(node);
				}
				else {
					return asyncronouslyComputeRefersTo(node);
				}
			}
		} catch (CoreException e) {
			CUIPlugin.log(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return NO_CHILDREN;
	}
	
	private Object[] asyncComputeRoot(final ICElement input) throws CoreException, InterruptedException {
		final ICElement element= input; //fNavigationService.findElement(null, input);

		// TODO:  put this back in when navigation service exists
		if (/*element == null*/ false) {
			getDisplay().asyncExec(new Runnable() {
				public void run() {
					fView.reportNotIndexed(input);
				}
			});
		} 
		else {
			getDisplay().asyncExec(new Runnable() {
				public void run() {
					fView.reportInputReplacement(input, element);
				}
			});
		}
		ITranslationUnit tu= CModelUtil.getTranslationUnit(element);
		/* -- ST-Origin --
		 * Source folder: org.eclipse.cdt.ui/src
		 * Class: org.eclipse.cdt.internal.ui.callhierarchy.CHContentProvider
		 * Version: 1.21
		 */
		if (!fComputeReferencedBy && element instanceof IMethod) {

			IProject project = input.getCProject().getProject();
			IServiceModelManager smm = ServiceModelManager.getInstance();
			IServiceConfiguration serviceConfig = smm.getActiveConfiguration(project);
	
			IService indexingService = smm.getService(IRDTServiceConstants.SERVICE_C_INDEX);
	
			IServiceProvider serviceProvider = serviceConfig.getServiceProvider(indexingService);
			Map<String, ICElement[]> overriders=null;
			if (serviceProvider instanceof IIndexServiceProvider) {
				Scope scope = new Scope(project);
				ICallHierarchyService chService = ((IIndexServiceProvider) serviceProvider).getCallHierarchyService();
				overriders = chService.findOverriders(scope, input, NPM);
			}
			if (overriders != null && overriders.size() > 0) {
				String linkageIDStr = overriders.keySet().iterator().next();
				int linkageID = Integer.parseInt(linkageIDStr);
				if(linkageID!=-1){
					return new Object[] { new CHMultiDefNode(null, tu, 0, overriders.get(linkageIDStr), linkageID) };
				}
			}
		}
		return new Object[] { new CHNode(null, tu, 0, element, -1) };
	}

	private Object[] asyncronouslyComputeReferencedBy(CHNode parent) throws CoreException, InterruptedException {
		ICElement callee = parent.getRepresentedDeclaration();
		CalledByResult result = null;
		
		IProject project = callee.getCProject().getProject();
		IServiceModelManager smm = ServiceModelManager.getInstance();
		if(smm.isConfigured(project)) {
			IServiceConfiguration serviceConfig = smm.getActiveConfiguration(project);
			IService indexingService = smm.getService(IRDTServiceConstants.SERVICE_C_INDEX);
			IServiceProvider serviceProvider = serviceConfig.getServiceProvider(indexingService);
	
			if (serviceProvider instanceof IIndexServiceProvider) {
				Scope scope = new Scope(project);
				ICallHierarchyService chService = ((IIndexServiceProvider) serviceProvider).getCallHierarchyService();
				result = chService.findCalledBy(scope, callee, NPM);
				return createNodes(parent, result);
			}
		}
		
		return new Object[0];
	}

	
	private Object[] asyncronouslyComputeRefersTo(CHNode parent) throws CoreException, InterruptedException {
		ICElement caller = parent.getRepresentedDeclaration();
		CallsToResult result = null;
		
		IProject project = caller.getCProject().getProject();
		IServiceModelManager smm = ServiceModelManager.getInstance();
		if(smm.isConfigured(project)) {
			IServiceConfiguration serviceConfig = smm.getActiveConfiguration(project);
			IService indexingService = smm.getService(IRDTServiceConstants.SERVICE_C_INDEX);
			IServiceProvider serviceProvider = serviceConfig.getServiceProvider(indexingService);
	
			if (serviceProvider instanceof IIndexServiceProvider) {
				Scope scope = new Scope(project);
				ICallHierarchyService chService = ((IIndexServiceProvider) serviceProvider).getCallHierarchyService();
				result = chService.findCalls(scope, caller, NPM);
				return createNodes(parent, result);
			}
		}
		
		return new Object[0];
	}

	public void setComputeReferencedBy(boolean value) {
		fComputeReferencedBy = value;
	}

	public boolean getComputeReferencedBy() {
		return fComputeReferencedBy;
	}

	public void setWorkingSetFilter(WorkingSetFilterUI filterUI) {
		fFilter= filterUI;
		recompute();
	}

	CHNode[] createNodes(CHNode node, CalledByResult result) throws CoreException {
		ArrayList<CHNode> nodes= new ArrayList<CHNode>();
		ICElement[] elements= result.getElements();
		for (ICElement element : elements) { 
			if (element != null) {
				if (fFilter == null || fFilter.isPartOfWorkingSet(element)) {
					IIndexName[] refs= result.getReferences(element);
					if (refs != null && refs.length > 0) {
						CHNode newNode = createRefbyNode(node, element, refs);
						nodes.add(newNode);
					}
				}
			}
		}
		return nodes.toArray(new CHNode[nodes.size()]);
	}
	
	private CHNode createRefbyNode(CHNode parent, ICElement element, IIndexName[] refs) throws CoreException {
		ITranslationUnit tu= CModelUtil.getTranslationUnit(element);
		final IIndexFile file = refs[0].getFile();
		CHNode node= new CHNode(parent, tu, refs[0].getFile().getTimestamp(), element, file.getLinkageID());
		if (element instanceof IVariable || element instanceof IEnumerator) {
			node.setInitializer(true);
		}
		boolean readAccess= false;
		boolean writeAccess= false;
		for (IIndexName reference : refs)  {
			node.addReference(new CHReferenceInfo(reference.getNodeOffset(), reference.getNodeLength()));
			readAccess= (readAccess || reference.isReadAccess());
			writeAccess= (writeAccess || reference.isWriteAccess());
		}
		node.setRWAccess(readAccess, writeAccess);
		node.sortReferencesByOffset();
		return node;
	}

	CHNode[] createNodes(CHNode node, CallsToResult callsTo) throws CoreException {
		ITranslationUnit tu= CModelUtil.getTranslationUnit(node.getRepresentedDeclaration());
		ArrayList<CHNode> result= new ArrayList<CHNode>();
		CElementSet[] elementSets= callsTo.getElementSets();
		for (CElementSet elementSet : elementSets) {
			CElementSet set = elementSet;
			if (!set.isEmpty()) {
				IIndexName[] refs= callsTo.getReferences(set);
				ICElement[] elements= set.getElements();
				if (elements.length > 0) {
					CHNode childNode = createReftoNode(node, tu, elements, refs);
					result.add(childNode);
				}
			}
		}
		return result.toArray(new CHNode[result.size()]);
	}

	private CHNode createReftoNode(CHNode parent, ITranslationUnit tu, ICElement[] elements, IIndexName[] references) throws CoreException {
		assert elements.length > 0;

		final IIndexFile file = references[0].getFile();
		final long timestamp= file.getTimestamp();
		final int linkageID= file.getLinkageID();

		CHNode node;
		
		if (elements.length == 1) {
			node= new CHNode(parent, tu, timestamp, elements[0], linkageID);
		}else {
			node= new CHMultiDefNode(parent, tu, timestamp, elements, linkageID);
		}
		
		boolean readAccess= false;
		boolean writeAccess= false;
		for (IIndexName reference : references) {
			node.addReference(new CHReferenceInfo(reference.getNodeOffset(), reference.getNodeLength()));
			readAccess= (readAccess || reference.isReadAccess());
			writeAccess= (writeAccess || reference.isWriteAccess());
		}
		node.sortReferencesByOffset();
		node.setRWAccess(readAccess, writeAccess);
		return node;
	}
}
