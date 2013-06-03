package org.eclipse.ptp.internal.rdt.sync.git.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.ignore.IgnoreNode;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.IndexDiffFilter;
import org.eclipse.ptp.rdt.sync.core.AbstractSyncFileFilter;
import org.eclipse.ptp.rdt.sync.core.SyncManager;

public class GitSyncFileFilter extends AbstractSyncFileFilter {
	// Map of projects to file filters along with basic getter and setter methods. These static data and methods operate
	// independently of the rest of the class and could be moved into a separate class if desired.
	private static Map<IProject, GitSyncFileFilter> projectToFilterMap = new HashMap<IProject, GitSyncFileFilter>();
	public static GitSyncFileFilter getFilter(IProject project) {
		return projectToFilterMap.get(project);
	}

	public static void setFilter(IProject project, AbstractSyncFileFilter filter) {
		// TODO: Figure out how to convert filter to a Git filter.
		projectToFilterMap.put(project, filter);
	}

	private Repository repository;
	
	public class GitIgnoreRule extends AbstractIgnoreRule {
		private org.eclipse.jgit.ignore.IgnoreRule rule;
		
		public GitIgnoreRule(String pattern, boolean exclude) {
			if (!exclude) pattern = "!" + pattern; //$NON-NLS-1$
			rule = new org.eclipse.jgit.ignore.IgnoreRule(pattern);
		}
		
		public GitIgnoreRule(IResource resource, boolean exclude) {
			//TODO: is toString the correct method? or should it be toPortableString
			//TODO: !,*,[,? need to be escaped 
			String pattern = resource.getProjectRelativePath().toString();
			if (resource.getType()==IResource.FOLDER) pattern += "/"; //$NON-NLS-1$
			if (!exclude) pattern = "!" + pattern; //$NON-NLS-1$
			rule = new org.eclipse.jgit.ignore.IgnoreRule(pattern);
		}
		
		private GitIgnoreRule(org.eclipse.jgit.ignore.IgnoreRule rule) {
			this.rule = rule;
		}
		
		@Override
		public boolean isMatch(IResource target) {
			return rule.isMatch(target.getProjectRelativePath().toString(), 
					target.getType()==IResource.FOLDER);
		}
		
		@Override
		public boolean isMatch(String target, boolean isFolder) {
			return rule.isMatch(target,isFolder);
		}

		@Override
		public boolean getResult() {
			return rule.getResult();
		}
		
		@Override
		public String toString() {
			return (rule.getNegation()?"!":"") + rule.getPattern() + (rule.dirOnly()?"/":""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		
		@Override
		public String getPattern() {
			return rule.getPattern();
		}
	}
	
	GitSyncFileFilter(Repository repository) {
		this.repository = repository;
	}
	
	@Override
	public void addPattern(String pattern, boolean exclude, int index) {
		rules.add(index, new GitIgnoreRule(pattern, exclude));

	}

	@Override
	public void addPattern(IResource resource, boolean exclude, int index) {
		rules.add(index, new GitIgnoreRule(resource, exclude));

	}

	public void saveFilter() throws IOException  {
		File exclude = repository.getFS().resolve(repository.getDirectory(),
				Constants.INFO_EXCLUDE);
		FileOutputStream file = new FileOutputStream(exclude);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(file,Constants.CHARSET));
		try {
			for (AbstractIgnoreRule rule : rules) {
				out.write(rule.toString());
				out.newLine();
			}
		} finally {
			out.close();
		}
	}
	
	public void loadFilter() throws IOException {
		File exclude = repository.getFS().resolve(repository.getDirectory(),
				Constants.INFO_EXCLUDE);
		if (exclude.exists()) {
			FileInputStream in = new FileInputStream(exclude);
			try {
				IgnoreNode node = new IgnoreNode();
				node.parse(in);
				for (org.eclipse.jgit.ignore.IgnoreRule rule : node.getRules())
					rules.add(new GitIgnoreRule(rule));
			} finally {
				in.close();
			}
		} else {
			clone(SyncManager.getDefaultFileFilter());
			rules.add(new GitIgnoreRule("/.ptp-sync/", true)); //$NON-NLS-1$
		}
	}

	@Override
	public void clone(AbstractSyncFileFilter fileFilter) {
		//
		for (AbstractIgnoreRule rule : fileFilter.rules)
			rules.add(new GitIgnoreRule(rule.getPattern(),rule.getResult()));
	}
	
	/* returns ignored files in the index */
	public List<String> getIgnoredFiles() throws IOException {
		TreeWalk treeWalk = new TreeWalk(repository);
		DirCache dirCache = repository.readDirCache();
		treeWalk.addTree(new DirCacheIterator(dirCache));
		List<String> ignoredFiles = new ArrayList<String>();
		int ignoreDepth = Integer.MAX_VALUE; //if the current subtree is ignored - than this is the depth at which to ignoring starts
		while (treeWalk.next()) {
			boolean isSubtree = treeWalk.isSubtree();
			int depth = treeWalk.getDepth();
			String path = treeWalk.getPathString();
			if (isSubtree) treeWalk.enterSubtree();
			if (depth > ignoreDepth) {
				if (!isSubtree) ignoredFiles.add(path);
				continue;
			}
			if (depth <= ignoreDepth) { //sibling or parent of ignore subtree => reset
				ignoreDepth = Integer.MAX_VALUE;
			}
			if (shouldIgnore(path, isSubtree)) {
				if (isSubtree) ignoreDepth = depth;
				else ignoredFiles.add(path);
			}
		}
		return ignoredFiles;
	}
	
	/* get all different files (modified/changed, missing/removed, untracked/added)
	 * 
	 * assumes that no files are in conflict (don't call during merge) 
	 * */
	public List<String> getDiffFiles() throws IOException {
		final int INDEX = 0;
		final int WORKDIR = 1;
		
		TreeWalk treeWalk = new TreeWalk(repository);
		treeWalk.addTree(new DirCacheIterator(repository.readDirCache()));
		treeWalk.addTree(new FileTreeIterator(repository));

		//don't honor ignores - we do it manual instead. Doing it all with the filter
		//would require a WorkingTreeIteraotr which does the ignore handing correct 
		//(both directory including bugs 401161 and only using info/exclude not .gitignore)
		treeWalk.setFilter(new IndexDiffFilter(INDEX, WORKDIR,false)); 
		List<String> diffFiles = new ArrayList<String>();
		int ignoreDepth = Integer.MAX_VALUE; //if the current subtree is ignored - than this is the depth at which to ignoring starts
		while (treeWalk.next()) {
			DirCacheIterator dirCacheIterator = treeWalk.getTree(INDEX,
					DirCacheIterator.class);
			String path = treeWalk.getPathString();
			boolean isSubtree = treeWalk.isSubtree();
			int depth = treeWalk.getDepth();
			if (dirCacheIterator != null ||          //in index => either missing or modified
					!shouldIgnore(path, isSubtree)) { //not in index => untracked
				if (depth <= ignoreDepth)  //sibling or parent of ignore subtree => reset
					ignoreDepth = Integer.MAX_VALUE;
				if (dirCacheIterator != null && isSubtree && ignoreDepth==Integer.MAX_VALUE &&
						shouldIgnore(path, isSubtree))
					ignoreDepth = depth;
				if (isSubtree) 
					treeWalk.enterSubtree(); 
				else if(dirCacheIterator != null || ignoreDepth==Integer.MAX_VALUE) 
					diffFiles.add(path);
			}
		}
		return diffFiles;
	}
	
	
	//for testing. args: work folder, git folder
	public static void main(String [] args) throws IOException
	{
		final File localDir = new File(args[0]);
		final FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder();
		File gitDirFile = new File(localDir + File.separator + args[1]); 
		Repository repository = repoBuilder.setWorkTree(localDir).setGitDir(gitDirFile).build();
		GitSyncFileFilter filter = new GitSyncFileFilter(repository);
		filter.loadFilter();
		//List<String> files = filter.getIgnoredFiles();
		List<String> files = filter.getDiffFiles();
		for (String path : files) 
			System.out.println(path);
	}
}
