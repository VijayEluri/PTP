/*******************************************************************************
 * Copyright (c) 2013 The University of Tennessee and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    John Eblen - initial implementation
 *******************************************************************************/
package org.eclipse.ptp.internal.rdt.sync.cdt.core;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;

import org.eclipse.cdt.core.language.settings.providers.ICBuildOutputParser;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsEditableProvider;
import org.eclipse.cdt.core.language.settings.providers.IWorkingDirectoryTracker;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Language settings provider for projects that build with CMake
 */
public class CMakeSettingsProvider extends AbstractXMLSettingsProvider implements ICBuildOutputParser, ILanguageSettingsEditableProvider {
	private IConfiguration config = null;
	private IPath buildDir = null;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.cdt.core.language.settings.providers.LanguageSettingsSerializableProvider#clone()
	 */
	@Override
	public CMakeSettingsProvider clone() throws CloneNotSupportedException {
		return (CMakeSettingsProvider) super.clone();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.cdt.core.language.settings.providers.LanguageSettingsSerializableProvider#cloneShallow()
	 */
	@Override
	public CMakeSettingsProvider cloneShallow() throws CloneNotSupportedException {
		return (CMakeSettingsProvider) super.cloneShallow();
	}

	@Override
	public Document getXML() throws IOException, SAXException {
		IPath CProjectFile = new Path(config.getBuilder().getBuildPath()).append(".cproject"); //$NON-NLS-1$
		Document XMLDoc = XMLConversionUtil.XMLFileToDOM(CProjectFile);
		DocumentBuilderFactory builderFactory =
		        DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
		    builder = builderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// Should never happen since no configuring is done
			throw new RuntimeException(e);
		}
		Document doc = builder.newDocument();
		Element providerNode = doc.createElement("provider"); //$NON-NLS-1$
		String compiler = config.getToolChain().getTargetToolList()[0];
		Element compilerNode = doc.createElement("language"); //$NON-NLS-1$
		compilerNode.setAttribute("id", compiler); //$NON-NLS-1$
		providerNode.appendChild(compilerNode);
		NodeList entries = XMLDoc.getElementsByTagName("pathEntry"); //$NON-NLS-1$
		for (int i=0; i<entries.getLength(); i++) {
			NamedNodeMap attr = entries.item(i).getAttributes();
		}
		return XMLDoc;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ptp.internal.rdt.sync.cdt.core.AbstractXMLSettingsProvider#getXSLT()
	 */
	@Override
	public Source getXSLT() {
		// No XSLT, because transformations are done internally
		return null;
	}

	/**
	 * Get the build directory
	 * @return build directory
	 */
	public IPath getBuildDir() {
		return buildDir;
	}

	/**
	 * Set the build directory
	 * @param path to build directory or null for no build directory
	 */
	public void setBuildDir(IPath dir) {
		buildDir = dir;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.cdt.core.language.settings.providers.ICBuildOutputParser#startup(org.eclipse.cdt.core.settings.model.ICConfigurationDescription, org.eclipse.cdt.core.language.settings.providers.IWorkingDirectoryTracker)
	 */
	@Override
	public void startup(ICConfigurationDescription cfgDescription,
			IWorkingDirectoryTracker cwdTracker) throws CoreException {
		config = ManagedBuildManager.getConfigurationForDescription(cfgDescription);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.cdt.core.language.settings.providers.ICBuildOutputParser#processLine(java.lang.String)
	 */
	@Override
	public boolean processLine(String line) {
		// This is not really a build output parser, so ignore all lines
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.cdt.core.language.settings.providers.ICBuildOutputParser#shutdown()
	 */
	@Override
	public void shutdown() {
		// nothing to do
	}
}
