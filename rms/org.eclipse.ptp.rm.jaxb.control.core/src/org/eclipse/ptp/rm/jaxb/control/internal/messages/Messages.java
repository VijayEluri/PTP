/*******************************************************************************
 * Copyright (c) 2011 University of Illinois All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html 
 * 	
 * Contributors: 
 * 	Albert L. Rossi - design and implementation
 ******************************************************************************/
package org.eclipse.ptp.rm.jaxb.control.internal.messages;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.ptp.rm.jaxb.control.internal.messages.messages"; //$NON-NLS-1$
	public static String JAXBControlCorePlugin_Exception_InternalError;
	public static String RMVariableResolver_derefError;
	public static String StreamParserNoSuchVariableError;
	public static String StreamParserMissingTargetType;
	public static String StreamParserInconsistentMapValues;
	public static String ManagedFilesJob;
	public static String ManagedFilesJobError;

	public static String ScriptHandlerJob;
	public static String RMNoSuchCommandError;
	public static String MissingRunCommandsError;

	public static String MissingArglistFromCommandError;
	public static String CouldNotLaunch;
	public static String StdoutParserError;
	public static String StderrParserError;
	public static String ParserInternalError;
	public static String ProcessExitValueError;
	public static String CannotCompleteSubmitFailedStaging;
	public static String ReadSegmentError;
	public static String StreamTokenizerInstantiationError;
	public static String MalformedExpressionError;
	public static String UnsupportedWriteException;
	public static String ProcessRunError;
	public static String CommandJobStreamMonitor_label;
	public static String CommandJobNullMonitorStreamError;
	public static String RemoteConnectionError;
	public static String LocalConnectionError;
	public static String StreamParserInconsistentPropertyWarning;

	public static String JAXBServiceProvider_defaultDescription;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// Prevent instances.
	}
}