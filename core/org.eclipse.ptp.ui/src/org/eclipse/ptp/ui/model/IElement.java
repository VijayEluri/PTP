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
package org.eclipse.ptp.ui.model;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ptp.core.elements.IPElement;

/**
 * @author clement chu
 *
 */
public interface IElement extends Cloneable, Comparable<IElement>, IAdaptable {
	/** 
	 * Clone a new element
	 * @return cloned element
	 */
	public IElement cloneElement();

	/** 
	 * Get element ID
	 * @return element ID in string format
	 */
	public String getID();
	
	/** 
	 * Get element name
	 * @return name of element
	 */
	public String getName();

	/**
	 * Get parent
	 * @return element
	 */
	public IElement getParent();
	
	/**
	 * Get the core model element associated with this element.
	 * @return IPElement
	 */
	public IPElement getPElement();
	
	/** 
	 * Test if element is registered
	 * @return true if element is registered
	 */
	public boolean isRegistered();
	
	/** 
	 * Test if element is selected
	 * @return true if element is selected
	 */
	public boolean isSelected();
	
	/** 
	 * Set element to register or not
	 * @param registered is register
	 */
	public void setRegistered(boolean registered);
	
	/** 
	 * Set element state to selected
	 * @param selected 
	 */
	public void setSelected(boolean selected);
}
