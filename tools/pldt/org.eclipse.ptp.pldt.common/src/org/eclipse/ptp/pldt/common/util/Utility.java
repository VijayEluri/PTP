/**********************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.common.util;

import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNodeLocation;
import org.eclipse.cdt.core.parser.ParserUtil;
import org.eclipse.cdt.internal.core.dom.parser.ASTNode;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Various function/methods of use
 * 
 * @author pazel
 *
 */
public class Utility
{
    
    // These are the marker types used for showing concurrency and non-concurrency
    public static final String ConcurrencyType = "org.eclipse.ptp.pldt.openmp.analysis.concurrency";
    public static final String NonConcurrencyType = "org.eclipse.ptp.pldt.openmp.analysis.nonconcurrency";
    
    /**
     * compute the location relative to file, ignoring includes
     * @param node - IASTNode
     * @return Location
     */
    public static Location getLocation(IASTNode node)
    {
        ASTNode astnode = (node instanceof ASTNode ? (ASTNode)node : null);
        if (astnode==null)  return null;

        IASTFileLocation ifl         = node.getFileLocation();
        // offset calculation is tricky - we used the following since it seems to cover the most cases
        int offset = 0;
        int length = 0;
        if (ifl!=null) {
            offset = ifl.getNodeOffset();   
            length = ifl.getNodeLength();
        }
        else {  // this happens in "omp sections", apparently due to pragmas splitting the region
            IASTNodeLocation [] locs = node.getNodeLocations(); 
            if (locs==null || locs.length==0)  return null;
            offset = locs[0].getNodeOffset();
            length = astnode.getLength();
        }
        return new Location(node, offset, offset+length-1);
    }

    //-------------------------------------------------------------------------
    // Member
    //-------------------------------------------------------------------------
    public static class Location
    {
        public IASTNode node_ = null;
        public int low_=0;
        public int high_=0;
    
        public Location(IASTNode node, int low, int high)
        {
            node_ = node;
            low_  = low;
            high_ = high;
        }
        
        public int getLow()  { return low_;  }
        public int getHigh() { return high_; }
    }
    
    /**
     * remove the concurrency markers from the screen
     * @param am - IAnnotationModel
     */
    public static void removeConcurrencyMarkers(IAnnotationModel am)
    {
        LinkedList ais = new LinkedList();
        for(Iterator ai=am.getAnnotationIterator(); ai.hasNext();) { ais.add(ai.next()); }
        for(Iterator it=ais.iterator(); it.hasNext();) {
            Annotation a = (Annotation)it.next();
            if (a.getType().equals(Utility.ConcurrencyType) || a.getType().equals(Utility.NonConcurrencyType)) {
                am.removeAnnotation(a);
            }
        }
    }
    
    /**
     * get document using full path name
     * @param fullPathName - String
     * @return IDocument
     */
    public static IDocument getDocument(String fullPathName)
    {
        IResource r = ParserUtil.getResourceForFilename(fullPathName);
        IFile     f = (r instanceof IFile ? (IFile)r : null);
        if (f==null)  return null;
        return getDocument(f);
    }
    
    /**
     * getDocument - get document using IFile
     * @param file - IFile
     * @return IDocument
     */
    public static IDocument getDocument(IFile file)
    {
        ITextFileBufferManager bufferManager= FileBuffers.getTextFileBufferManager();
        ITextFileBuffer textFileBuffer= bufferManager.getTextFileBuffer(file.getFullPath());
        IDocument document = null;
        if (textFileBuffer != null) {
            document= textFileBuffer.getDocument();
        }
        return document;
    }
	/**
	 * From an editor, determine the file (absolute path name) open in the editor.
	 * @param editor
	 * @return null if none, but the string of the absolute file system location if available
	 * 
	 */
    //cdt40 since editor.getInputFile() is now missing, this compensates
	public static String getInputFile(ITextEditor editor) {
		IEditorInput input= editor.getEditorInput();
		if (input == null) {
			return null;
		}
		IFile file= ResourceUtil.getFile(input);
		if (file != null) {
			return file.getLocation().toOSString();
		}
		if (input instanceof IPathEditorInput) {
			IPath location= ((IPathEditorInput)input).getPath();
			if (location != null) {
				return location.toOSString();
			}
		}
		ILocationProvider locationProvider= (ILocationProvider)input.getAdapter(ILocationProvider.class);
		if (locationProvider != null) {
			IPath location= locationProvider.getPath(input);
			if (location != null) {
				return location.toOSString();
			}
		}
		return null;
	}


}
