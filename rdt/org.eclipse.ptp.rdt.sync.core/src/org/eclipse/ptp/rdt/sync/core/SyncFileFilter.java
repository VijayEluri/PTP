/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    John Eblen - initial implementation
 *******************************************************************************/
package org.eclipse.ptp.rdt.sync.core;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IMemento;

/**
 * Class for filtering files during synchronization. Instead of a constructor, the user can create an empty filter or a filter that
 * has decent default behavior, filtering, for example, configuration files like .project and .cproject.
 * 
 * Facilities are then provided for adding and removing files and directories from filtering.
 */
public class SyncFileFilter {
	private static final String FILE_FILTER_PATH_ELEMENT_NAME = "file-filter-path"; //$NON-NLS-1$
	private static final String ATTR_PROJECT_NAME = "project"; //$NON-NLS-1$
	private static final String ATTR_FILTER_BINARIES = "filter-binaries"; //$NON-NLS-1$
	private static final String ATTR_FILE_FILTER_PATH = "filter-path"; //$NON-NLS-1$
	
	private final IProject project;
	private final LinkedList<PatternMatcher> filteredPaths = new LinkedList<PatternMatcher>();
	private final Map<PatternMatcher, PatternType> patternToTypeMap = new HashMap<PatternMatcher, PatternType>();
	
	public enum PatternType {
		EXCLUDE, INCLUDE
	}

	// Private constructor - create instances with "create" methods.
	private SyncFileFilter(IProject p) {
		project = p;
	}
	
	// Copy constructor
	public SyncFileFilter(SyncFileFilter oldFilter) {
		project = oldFilter.project;
		filteredPaths.addAll(oldFilter.filteredPaths);
		patternToTypeMap.putAll(oldFilter.patternToTypeMap);
	}
	
	/**
	 * Constructor for an empty filter for a given project. Most clients will want to use "createDefaultFilter"
	 *
	 * @param project
	 * @return the new filter
	 */
	public static SyncFileFilter createEmptyFilter(IProject project) {
		return new SyncFileFilter(project);
	}
	
	/**
	 * Constructor for a filter with all of the usual default paths already included.
	 *
	 * @param project
	 * @return the new filter
	 */
	public static SyncFileFilter createDefaultFilter(IProject project) {
		SyncFileFilter sff = new SyncFileFilter(project);
		sff.addDefaults();
		return sff;
	}
	
	/**
	 * Get filter's project
	 * @return project
	 */
	public IProject getProject() {
		return project;
	}
	
	/**
	 * Get all patterns for this filter
	 * @return patterns
	 */
	public PatternMatcher[] getPatterns() {
		return filteredPaths.toArray(new PatternMatcher[filteredPaths.size()]);
	}
	
	/**
	 * Get the pattern type for the pattern
	 * @param pattern
	 * @return the type or null if this pattern is unknown in this filter.
	 */
	public PatternType getPatternType(PatternMatcher pattern) {
		return patternToTypeMap.get(pattern);
	}
	
	/**
	 * Add the common, default list of paths to be filtered.
	 */
	public void addDefaults() {
		this.addPattern(new RegexPatternMatcher(".project"), PatternType.EXCLUDE); //$NON-NLS-1$
		this.addPattern(new RegexPatternMatcher(".cproject"), PatternType.EXCLUDE); //$NON-NLS-1$
		this.addPattern(new RegexPatternMatcher(".settings"), PatternType.EXCLUDE); //$NON-NLS-1$
		// TODO: This Git-specific directory is defined in multiple places - need to refactor.
		this.addPattern(new RegexPatternMatcher(".ptp-sync"), PatternType.EXCLUDE); //$NON-NLS-1$
	}
	
	/**
	 * Return whether this is a binary file. Note that this only works for files recognized by CDT. This function does not yet
	 * recognize binary files in general.
	 * @param project
	 * @param path
	 * @return whether file is a binary file
	 */
	public boolean isBinaryFile(IProject project, IPath path) {
		try {
			ICElement fileElement = CoreModel.getDefault().create(project.getFile(path));
			if (fileElement == null) {
				return false;
			}
			int resType = fileElement.getElementType();
			if (resType == ICElement.C_BINARY) {
				return true;
			} else {
				return false;
			}
		} catch (NullPointerException e) {
			// CDT throws this exception for files not recognized. For now, be conservative and allow these files.
			return false;
		}
	}
	
	/**
	 * Add a new pattern to the filter of the specified type
	 * @param pattern
	 * @param type
	 */
	public void addPattern(PatternMatcher pattern, PatternType type) {
		filteredPaths.add(pattern);
		patternToTypeMap.put(pattern, type);
	}
	
	/**
	 * Remove a pattern from the filter
	 * @param pattern
	 */
	public void removePattern(PatternMatcher pattern) {
		filteredPaths.remove(pattern);
		patternToTypeMap.remove(pattern);
	}

	/**
	 * Apply the filter to the given string
	 * @param s - the string
	 * @return whether the string should be ignored
	 */
	public boolean shouldIgnore(String s) {
		for (PatternMatcher pm : filteredPaths) {
			if (pm.match(s)) {
				PatternType type = patternToTypeMap.get(pm);
				assert(pm != null);
				if (type == PatternType.EXCLUDE) {
					return true;
				} else {
					return false;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Store filter in a given memento
	 *
	 * @param memento
	 */
	public void saveFilter(IMemento memento) {
		memento.putString(ATTR_PROJECT_NAME, project.getName());
			// ptf.pattern.savePattern(pathMemento);
	}
	
	/**
	 * Load filter from a given memento
	 *
	 * @param memento
	 * @return the restored filter
	 */
	public static SyncFileFilter loadFilter(IMemento memento) {
		String projectName = memento.getString(ATTR_PROJECT_NAME);
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (project == null) {
			throw new RuntimeException("Project not found for filter data: " + project); //$NON-NLS-1$
		}

		SyncFileFilter filter = createEmptyFilter(project);
		
		for (IMemento pathMemento : memento.getChildren(FILE_FILTER_PATH_ELEMENT_NAME)) {
			// filter.filteredPaths.add(PatternMatcher.loadFilter(pathMemento));
		}
		
		return filter;
	}
}
