/*******************************************************************************
 * Copyright (c) 2011 University of Illinois All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html 
 * 	
 * Contributors: 
 * 	Albert L. Rossi - design and implementation
 ******************************************************************************/
package org.eclipse.ptp.rm.jaxb.core.messages;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ptp.rm.jaxb.core.messages.messages"; //$NON-NLS-1$

	public static String FailedToCreateRmData;
	public static String PublicId;
	public static String SystemId;
	public static String LineNumber;
	public static String ColumnNumber;
	public static String Message;

	public static String UsingCachedDefinition;
	public static String CachedDefinitionWarning;

	public static String ForceXmlReload;
	public static String ShowSegmentPattern;
	public static String ShowMatchStatus;
	public static String ShowActionsForMatch;
	public static String ShowCreatedProperties;
	public static String ShowCommand;
	public static String ShowCommandOutput;
	public static String LogFile;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Prevent instances.
	}
}
