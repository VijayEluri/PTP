/*******************************************************************************
 * Copyright (c) 2013 The University of Tennessee and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Roland Schulz - initial implementation
 *******************************************************************************/

package org.eclipse.ptp.internal.rdt.sync.git.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.ignore.IgnoreNode;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.IndexDiffFilter;
import org.eclipse.ptp.rdt.sync.core.AbstractSyncFileFilter;
import org.eclipse.ptp.rdt.sync.core.SyncManager;

/**
 * File filtering using git build in filtering. All file filter patterns are stored in .ptp-sync/info/exclude.
 * The git syntax is used (see man gitignore).
 * 
 */
public class GitSyncFileFilter extends AbstractSyncFileFilter {
	private final JGitRepo jgitRepo;

	private static Set<Character> escapifyCharSet = new HashSet<Character>();
	static {
		CharacterIterator it = new StringCharacterIterator("#\\!*?[]"); //$NON-NLS-1$
		for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
			escapifyCharSet.add(c);
		}
	}

	public class GitIgnoreRule extends AbstractIgnoreRule {
		private final org.eclipse.jgit.ignore.IgnoreRule rule;

		public GitIgnoreRule(String pattern, boolean exclude) {
			if (!exclude) {
				pattern = "!" + pattern; //$NON-NLS-1$
			}
			rule = new org.eclipse.jgit.ignore.IgnoreRule(pattern);
		}

		private String charEscapify(String inputString) {
			StringBuffer newString = new StringBuffer(""); //$NON-NLS-1$
			CharacterIterator it = new StringCharacterIterator(inputString);
			for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
				if (escapifyCharSet.contains(c)) { // Do not escape non-ASCII characters (> 127)
					newString.append("\\" + c); //$NON-NLS-1$
				} else {
					newString.append(c);
				}
			}
			inputString = newString.toString();
			return inputString;
		}

		public GitIgnoreRule(IResource resource, boolean exclude) {
			String pattern = charEscapify(resource.getProjectRelativePath().toString());
			if (pattern.charAt(0) != '/') {
				pattern = "/" + pattern; //$NON-NLS-1$
			}
			if (resource.getType() == IResource.FOLDER) {
				pattern += "/"; //$NON-NLS-1$
			}
			if (!exclude) {
				pattern = "!" + pattern; //$NON-NLS-1$
			}
			rule = new org.eclipse.jgit.ignore.IgnoreRule(pattern);
		}

		private GitIgnoreRule(org.eclipse.jgit.ignore.IgnoreRule rule) {
			this.rule = rule;
		}

		@Override
		public boolean isMatch(IResource target) {
			return isMatch(target.getProjectRelativePath().toString(), target.getType() == IResource.FOLDER);
		}

		@Override
		public synchronized boolean isMatch(String target, boolean isFolder) {
			return rule.isMatch(target, isFolder); // JGit's rule.isMatch isn't thread safe
		}

		@Override
		public boolean getResult() {
			return rule.getResult();
		}

		@Override
		public String toString() {
			return (rule.getNegation() ? "!" : "") + rule.getPattern() + (rule.dirOnly() ? "/" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}

		@Override
		public String getPattern() {
			return rule.getPattern() + (rule.dirOnly() ? "/" : ""); //$NON-NLS-1$ //$NON-NLS-2$ 
		}
	}

	/**
	 * Construct a new Git file filter for the given JGit repository
	 * @param repo
	 * 			the JGit repository
	 */
	GitSyncFileFilter(JGitRepo repo) {
		this.jgitRepo = repo;
	}

	/**
	 * Construct a new Git file filter for the given JGit repository, adding the rules from the given filter.
	 *
	 * @param repo
	 * 			the JGit repository
	 * @param filter
	 * 			an abstract file filter 
	 */
	public GitSyncFileFilter(JGitRepo repo, AbstractSyncFileFilter filter) {
		this.jgitRepo = repo;
		this.initialize(filter);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ptp.rdt.sync.core.AbstractSyncFileFilter#addPattern(java.lang.String, boolean, int)
	 */
	@Override
	public void addPattern(String pattern, boolean exclude, int index) {
		GitIgnoreRule newRule = new GitIgnoreRule(pattern, exclude);

		// Remove duplicate rules
		int nextIndex = 0;
		int numRulesRemoved = 0;
		for (Iterator<AbstractIgnoreRule> it = rules.iterator(); it.hasNext() && nextIndex < index; nextIndex++) {
			AbstractIgnoreRule existingRule = it.next();
			if (pattern.equals(existingRule.getPattern())) {
				it.remove();
				numRulesRemoved++;
			}
		}

		// Add new rule, adjusting index for removed rules.
		rules.add(index - numRulesRemoved, newRule);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ptp.rdt.sync.core.AbstractSyncFileFilter#addPattern(org.eclipse.core.resources.IResource, boolean, int)
	 */
	@Override
	public void addPattern(IResource resource, boolean exclude, int index) {
		GitIgnoreRule newRule = new GitIgnoreRule(resource, exclude);

		// Remove duplicate rules
		int nextIndex = 0;
		int numRulesRemoved = 0;
		for (Iterator<AbstractIgnoreRule> it = rules.iterator(); it.hasNext() && nextIndex < index; nextIndex++) {
			AbstractIgnoreRule existingRule = it.next();
			if (newRule.getPattern().equals(existingRule.getPattern())) {
				it.remove();
				numRulesRemoved++;
			}
		}

		// Add new rule, adjusting index for removed rules.
		// Do not add, however, if an existing rule was removed and the new rule has no effect.
		if (numRulesRemoved == 0) {
			rules.add(index - numRulesRemoved, newRule);
		} else {
			boolean resourceIgnored = this.shouldIgnore(resource);
			if (resourceIgnored != exclude) {
				rules.add(index - numRulesRemoved, newRule);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ptp.rdt.sync.core.AbstractSyncFileFilter#saveFilter()
	 */
	@Override
	public void saveFilter() throws IOException {
		Repository repo = jgitRepo.getRepository();
		File exclude = repo.getFS().resolve(repo.getDirectory(), Constants.INFO_EXCLUDE);
		exclude.getParentFile().mkdirs();
		FileOutputStream file = new FileOutputStream(exclude);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(file, Constants.CHARSET));
		try {
			for (AbstractIgnoreRule rule : rules) {
				out.write(rule.toString());
				out.newLine();
			}
		} finally {
			out.close();
		}
		final RmCommand rmCommand = new RmCommand(repo);
		rmCommand.setCached(true);
		for (String fileName : getIgnoredFiles(null)) {
			rmCommand.addFilepattern(fileName);
		}
		try {
			rmCommand.call();
		} catch (NoFilepatternException e) {
			new IOException(e); // TODO: a bit ugly to wrap it into IOExcpetion
		} catch (GitAPIException e) {
			new IOException(e);
		}
	}

	/**
	 * Load filtering rules from the file system
	 * @throws IOException
	 * 			on problems reading from the file system
	 */
	public void loadFilter() throws IOException {
		Repository repo = jgitRepo.getRepository();
		File exclude = repo.getFS().resolve(repo.getDirectory(), Constants.INFO_EXCLUDE);
		if (exclude.exists()) {
			FileInputStream in = new FileInputStream(exclude);
			try {
				IgnoreNode node = new IgnoreNode();
				node.parse(in);
				for (org.eclipse.jgit.ignore.IgnoreRule rule : node.getRules()) {
					rules.add(new GitIgnoreRule(rule));
				}
			} finally {
				in.close();
			}
		} else {
			initialize(SyncManager.getDefaultFileFilter());
		}
	}

	/**
	 * Initialize based on other filter. Adds provider dependent default.
	 * 
	 * @param fileFilter
	 *            filter to copy
	 */
	public void initialize(AbstractSyncFileFilter fileFilter) {
		if (fileFilter instanceof GitSyncFileFilter) {
			rules.addAll(fileFilter.getRules());
		} else { // convert rules and add git specific rule
			for (AbstractIgnoreRule rule : fileFilter.getRules()) {
				rules.add(new GitIgnoreRule(rule.getPattern(), rule.getResult()));
			}
			rules.add(new GitIgnoreRule("/.ptp-sync/", true)); //$NON-NLS-1$
		}
	}

	/**
	 * Returns ignored files in the index
	 *
	 * @param ref reference to compute list of files for. If null use index.
	 * 
	 * @return set of ignored files
	 * @throws IOException
	 * 			on file system problems
	 */
	public Set<String> getIgnoredFiles(RevTree ref) throws IOException {
		Repository repo = jgitRepo.getRepository();
		TreeWalk treeWalk = new TreeWalk(repo);
		if (ref == null) {
			DirCache dirCache = repo.readDirCache();
			treeWalk.addTree(new DirCacheIterator(dirCache));
		} else {
			treeWalk.addTree(ref);
		}
		HashSet<String> ignoredFiles = new HashSet<String>();
		int ignoreDepth = Integer.MAX_VALUE; // if the current subtree is ignored - than this is the depth at which to ignoring
												// starts
		while (treeWalk.next()) {
			boolean isSubtree = treeWalk.isSubtree();
			int depth = treeWalk.getDepth();
			String path = treeWalk.getPathString();
			if (isSubtree) {
				treeWalk.enterSubtree();
			}
			if (depth > ignoreDepth) {
				if (!isSubtree) {
					ignoredFiles.add(path);
				}
				continue;
			}
			if (depth <= ignoreDepth) { // sibling or parent of ignore subtree => reset
				ignoreDepth = Integer.MAX_VALUE;
			}
			if (shouldIgnore(path, isSubtree)) {
				if (isSubtree) {
					ignoreDepth = depth;
				} else {
					ignoredFiles.add(path);
				}
			}
		}
		return ignoredFiles;
	}

	/**
	 * Return the JGit repository for this filter
	 * @return JGit repository
	 */
	public JGitRepo getRepo() {
		return jgitRepo;
	}

	/**
	 * Class for storing names of files that have changed
	 */
	public class DiffFiles {
		public Set<String> added = new HashSet<String>(); // modified and added
		public Set<String> removed = new HashSet<String>();
		public Set<String> dirSet = new HashSet<String>(); // All non-ignored directories
	}

	/**
	 * Get all different files (modified/changed, missing/removed, untracked/added)
	 * 
	 * assumes that no files are in conflict (don't call during merge)
	 * 
	 * @return different files
	 * @throws IOException
	 * 			on file system problems
	 */
	public DiffFiles getDiffFiles() throws IOException {
		final int INDEX = 0;
		final int WORKDIR = 1;

		assert(!jgitRepo.inUnresolvedMergeState());

		Repository repo = jgitRepo.getRepository();
		TreeWalk treeWalk = new TreeWalk(repo);
		treeWalk.addTree(new DirCacheIterator(repo.readDirCache()));
		treeWalk.addTree(new FileTreeIterator(repo));

		// don't honor ignores - we do it manually instead. Doing it all with the filter
		// would require a WorkingTreeIterator that does the ignore handling correctly
		// (both directory including bugs 401161 and only using info/exclude, not .gitignore)
		treeWalk.setFilter(new IndexDiffFilter(INDEX, WORKDIR, false));
		DiffFiles diffFiles = new DiffFiles();
		int ignoreDepth = Integer.MAX_VALUE; // if the current subtree is ignored - than this is the depth at which ignoring
											 // starts
		while (treeWalk.next()) {
			DirCacheIterator dirCacheIterator = treeWalk.getTree(INDEX, DirCacheIterator.class);
			String path = treeWalk.getPathString();
			boolean isSubtree = treeWalk.isSubtree();
			int depth = treeWalk.getDepth();
			if (dirCacheIterator != null || // in index => either missing or modified
					!shouldIgnore(path, isSubtree)) { // not in index => untracked
				if (depth <= ignoreDepth) {
					ignoreDepth = Integer.MAX_VALUE;
				}
				if (dirCacheIterator != null && isSubtree && ignoreDepth == Integer.MAX_VALUE && shouldIgnore(path, isSubtree)) {
					ignoreDepth = depth;
				}
				if (isSubtree) {
					treeWalk.enterSubtree();
					diffFiles.dirSet.add(path);
				} else if (dirCacheIterator != null || ignoreDepth == Integer.MAX_VALUE) {
					WorkingTreeIterator workTreeIter = treeWalk.getTree(WORKDIR, WorkingTreeIterator.class);
					if (workTreeIter != null) {
						diffFiles.added.add(path);
					} else {
						diffFiles.removed.add(path);
					}
				}
			}
		}
		return diffFiles;
	}

	/**
	 * For testing
	 *
	 * @param args 
	 * 			work folder, Git folder
	 *
	 * @throws GitAPIException
	 * 			on JGit-specific problems
	 * @throws IOException
	 * 			on file system problems
	 */
	public static void main(String[] args) throws IOException, GitAPIException {
		JGitRepo jgitRepo = new JGitRepo(new Path(args[0]), null);
		GitSyncFileFilter filter = new GitSyncFileFilter(jgitRepo);
		filter.loadFilter();
		Set<String> files = filter.getDiffFiles().added;
		for (String path : files) {
			System.out.println(path);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ptp.rdt.sync.core.AbstractSyncFileFilter#clone()
	 */
	@Override
	public AbstractSyncFileFilter clone() {
		return new GitSyncFileFilter(jgitRepo, this);
	}
}
