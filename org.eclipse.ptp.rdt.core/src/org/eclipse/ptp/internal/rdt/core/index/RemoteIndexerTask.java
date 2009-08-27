/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.internal.rdt.core.index;

import java.util.Arrays;

import org.eclipse.cdt.core.dom.IPDOMIndexer;
import org.eclipse.cdt.core.dom.IPDOMIndexerTask;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.internal.core.pdom.IndexerProgress;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ptp.internal.rdt.core.model.Scope;
import org.eclipse.ptp.rdt.core.serviceproviders.IIndexServiceProvider;

/**
 * @author crecoskie
 *
 */
public class RemoteIndexerTask implements IPDOMIndexerTask {

	protected RemoteFastIndexer fIndexer;
	
	protected IIndexServiceProvider fIndexServiceProvider;
	
	protected ITranslationUnit[] fAdded;
	protected ITranslationUnit[] fChanged;
	protected ITranslationUnit[] fRemoved;
	protected boolean fUpdate;
	
	private RemoteIndexerProgress fRemoteProgress = new RemoteIndexerProgress();
	
	public RemoteIndexerTask(RemoteFastIndexer indexer,
			IIndexServiceProvider indexingServiceProvider, 
			ITranslationUnit[] added, ITranslationUnit[] changed, ITranslationUnit[] removed, boolean update) {
		fIndexer = indexer;
		
		fIndexServiceProvider = indexingServiceProvider;
		
		fAdded = added;
		fChanged = changed;
		fRemoved = removed;
		fUpdate = update;
	}
	
	public IPDOMIndexer getIndexer() {
		return fIndexer;
	}

	public IndexerProgress getProgressInformation() {
		synchronized (fRemoteProgress) {
			return RemoteIndexerProgress.getIndexerProgress(fRemoteProgress);
		}
	}
	
	public void updateProgressInformation(RemoteIndexerProgress progress){
		synchronized (fRemoteProgress) {
			if (progress != null)
				fRemoteProgress = progress;
		}
	}


	public void run(IProgressMonitor monitor) throws InterruptedException {
		IIndexLifecycleService service = fIndexServiceProvider.getIndexLifeCycleService();
		IProject project = fIndexer.getProject().getProject();
		String name = project.getName();
		if (fUpdate)
			service.update(new Scope(name), Arrays.<ICElement>asList(fAdded), Arrays.<ICElement>asList(fChanged), Arrays.<ICElement>asList(fRemoved), monitor, this);
		else
			service.reindex(new Scope(name), fIndexServiceProvider.getIndexLocation(), Arrays.<ICElement>asList(fAdded), monitor, this);
	}

}
