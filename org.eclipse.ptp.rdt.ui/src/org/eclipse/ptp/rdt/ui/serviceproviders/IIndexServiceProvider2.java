/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.rdt.ui.serviceproviders;

import org.eclipse.ptp.internal.rdt.ui.contentassist.IContentAssistService;
import org.eclipse.ptp.internal.rdt.ui.search.ISearchService;
import org.eclipse.ptp.rdt.core.serviceproviders.IIndexServiceProvider;

public interface IIndexServiceProvider2 extends IIndexServiceProvider {
	public ISearchService getSearchService();

	public IContentAssistService getContentAssistService();
}
