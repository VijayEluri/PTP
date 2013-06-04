package org.eclipse.ptp.internal.rdt.sync.git.core;

import java.io.IOException;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.util.RawParseUtils;

/** 
 * Identical to FileTreeIterator, besides that it uses GitSyncFileFilter (which uses only info/exclude)
 * for filtering (and not .gitignore). It does not fix 401161. Thus it does not ignore files which exclusion pattern
 * in an ignored folder.
 * 
 * @author Roland Schulz
 *
 */
public class SyncFileTreeIterator extends FileTreeIterator {
	GitSyncFileFilter filter;

	public SyncFileTreeIterator(Repository repo, GitSyncFileFilter filter) {
		super(repo);
		this.filter = filter;
	}
	
	@Override
	protected boolean isEntryIgnored(int pLen) throws IOException {
		int pOff = pathOffset;
		if (0 < pOff)
			pOff--;
		String p =  RawParseUtils.decode(Constants.CHARSET, path, pOff, pLen);
		return filter.shouldIgnore(p, FileMode.TREE.equals(mode));
	}
}
