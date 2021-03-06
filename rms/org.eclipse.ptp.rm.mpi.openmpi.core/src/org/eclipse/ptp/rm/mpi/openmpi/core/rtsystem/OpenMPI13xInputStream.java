/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ptp.rm.mpi.openmpi.core.rtsystem;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Attempt to clean up quasi-XML output by 1.3.x
 * 
 * We know the following:
 * - XML generated by 1.3.3 and below does not include a root element
 * - XML between the <map> and </map> tags should be well formed
 * - XML generated by 1.3.3 and below may embed bogus <stdout> tags or otherwise corrupt the XML
 *   after the </map> tag
 * - There may be non XML output before the first tag and after the final tag 
 * - Dstore connections interpret escaped &lt; and &gt; tags!
 * 
 * We need to:
 * - inject root tags if necessary
 * - ignore everything before the first and after the last XML tag
 * - for 1.3.3 and below (identified by lack of root element) the last XML tag is </map>
 * - check for invalid '<' and '>' characters and convert them back to the escaped forms
 * 
 */
public class OpenMPI13xInputStream extends FilterInputStream {
	protected class TagItem {
		private final Pattern pattern;
		
		public TagItem(String pattern) {
			this.pattern = Pattern.compile(pattern);
		}
		
		public boolean match(String tag) {
			return pattern.matcher(tag).matches();
		}
	}
	
	protected class TagSet {
		private final int len;
		private HashMap<String, TagItem> tags = new HashMap<String, TagItem>();
		
		public TagSet(int len) {
			this.len = len;
		}
		
		public int length() {
			return len;
		}
	
		public TagItem add(String tag) {
			TagItem item = new TagItem(tag);
			tags.put(tag, item);
			return item;
		}

		public TagItem match(String str) {
			if (str.length() >= len) {
				return tags.get(str.substring(0, len));
			}
			return null;
		}
	}
	
	protected class TagList {
		private int longest = 0;
		private int shortest = 0;
		private final ArrayList<TagItem> tags = new ArrayList<TagItem>();
		
		public TagItem add(String pattern) {
			TagItem item = new TagItem(pattern);
			tags.add(item);
			return item;
		}
		
		public TagItem match(String str) {
			for (TagItem item : tags) {
				if (item.match(str)) {
					return item;
				}
			}
			return null;
		}
		
		public int longest() {
			return longest;
		}
		
		public int shortest() {
			return shortest;
		}
	}
	
	private final static String rootStartTagString = "<mpirun>"; //$NON-NLS-1$
	private final static String rootEndTagString = "</mpirun>"; //$NON-NLS-1$
	private final static String mapStartTagString = "<map>"; //$NON-NLS-1$
	private final static String mapEndTagString = "</map>"; //$NON-NLS-1$
	
	private final TagItem rootStartTag;
	private final TagList validStartTags = new TagList();
	private final TagList validTags = new TagList();
	private final TagList finalTags = new TagList();

	private enum State {
		PROLOG, 
		START_TAG,
		XML,
		SEEN_TAG_START,
		SEEN_TAG_END,
		FINISHED_TAG,
		SEEN_FINAL,
		EPILOG
	}

	private State state = State.PROLOG;
	private boolean rootTag = true; // Assume root tag
	private StringBuilder buffer = new StringBuilder();
	
	protected OpenMPI13xInputStream(InputStream in) {
		super(in);
		rootStartTag = validStartTags.add(rootStartTagString);
		validStartTags.add(mapStartTagString);
		validTags.add(mapStartTagString);
		validTags.add(mapEndTagString);
		validTags.add("<host.*>"); //$NON-NLS-1$
		validTags.add("</host>"); //$NON-NLS-1$
		validTags.add("<process.*>"); //$NON-NLS-1$
		validTags.add("</process>"); //$NON-NLS-1$
		validTags.add("<noderesolve.*>"); //$NON-NLS-1$
		validTags.add("</noderesolve>"); //$NON-NLS-1$
		validTags.add("<stdout.*>"); //$NON-NLS-1$
		validTags.add("</stdout>"); //$NON-NLS-1$
		validTags.add("<stderr.*>"); //$NON-NLS-1$
		validTags.add("</stderr>"); //$NON-NLS-1$
		validTags.add("<stddiag.*>"); //$NON-NLS-1$
		validTags.add("</stddiag>"); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see java.io.FilterInputStream#available()
	 */
	@Override
	public int available() throws IOException {
		int available = 0;
		
		switch (state) {
		case PROLOG:
			available = rootStartTagString.length();
			break;
			
		case START_TAG:
			available = super.available();
			break;
			
		default:
			available = buffer.length() + super.available();
			break;
		}
		return available;
	}

	/* (non-Javadoc)
	 * @see java.io.FilterInputStream#markSupported()
	 */
	@Override
	public boolean markSupported() {
		return false;
	}

	/* (non-Javadoc)
	 * @see java.io.FilterInputStream#read()
	 */
	@Override
	public int read() throws IOException {
		int ch;
		while (state != State.EPILOG) {
			switch (state) {
			case PROLOG:
				// Ignore anything except a less-than
				ch = super.read();
				if (ch < 0) {
					return -1;
				}
				if (ch == '<') {
					buffer.append((char)ch);
					state = State.START_TAG;
				}
				break;
				
			case START_TAG:
				// Find first valid tag. If we find another '<' then
				// assume the first '<' was bogus. If we find a '>'
				// then check the tag is valid. If not, start again.
				ch = super.read();
				if (ch < 0) {
					return -1;
				}
				buffer.append((char)ch);
				switch (ch) {
				case '<':
					buffer.delete(0, buffer.length());
					state = State.PROLOG;
					break;
					
				case '>':
					TagItem item = validStartTags.match(buffer.toString());
					if (item != null) {
						if (item == rootStartTag) {
							finalTags.add(rootEndTagString);
						} else {
							finalTags.add(mapEndTagString);
							buffer.insert(0, rootStartTagString);
							rootTag = false;
						}
						state = State.SEEN_TAG_END;
					} else {
						buffer.delete(0, buffer.length());
						state = State.PROLOG;
					}
					break;
				}
				break;
				
			case XML:
				ch = super.read();
				if (ch < 0) {
					return -1;
				}
				if (ch < ' ') {
					// Skip embedded newlines (bug #287204)
					break;
				}
				
				switch (ch) {
				case '<':
					buffer.append((char)ch);
					state = State.SEEN_TAG_START;
					break;
					
				case '>':
					// Invalid tag end, replace it with escape sequence (bug #286671)
					buffer.append("&gt;"); //$NON-NLS-1$
					break;
					
				default:
					return ch;
				}
				break;
				
			case SEEN_TAG_START:
				// Process everything until we find the tag end
				ch = super.read();
				if (ch < 0) {
					return -1;
				}
				if (ch < ' ') {
					// Skip embedded newlines (bug #287204)
					break;
				}
				
				switch (ch) {
				case '<':
					// Not a known tag, so replace with escape sequence (bug #286671)
					buffer.append("&lt;"); //$NON-NLS-1$
					state = State.XML;
					break;
					
				case '>':
					buffer.append((char)ch);
					String val = buffer.toString();

					// If tag is a possible final tag, need to check
					// following characters to determine if we're at 
					// end of XML
					if (finalTags.match(val) != null) {
						state = State.SEEN_FINAL;
						break;
					}
					
					// If tag is valid go back to processing XML
					if (validTags.match(val) != null) {
						state = State.SEEN_TAG_END;
					}
					break;
					
				default:
					buffer.append((char)ch);
					break;
				}
				break;
			
			case SEEN_TAG_END:
				ch = buffer.charAt(0);
				buffer.deleteCharAt(0);
				if (buffer.length() == 0) {
					state = State.FINISHED_TAG;
				}
				return ch;
				
			case FINISHED_TAG:
				state = State.XML;
				break;
				
			case SEEN_FINAL:
				if (buffer.length() > 0) {
					ch = buffer.charAt(0);
					buffer.deleteCharAt(0);
					return ch;
				}
				
				if (!rootTag) {
					buffer.insert(0, rootEndTagString);
				}
				state = State.EPILOG;
				break;
				
			default:
				assert(false);
			}
		}
		
		if (buffer.length() == 0) {
			return -1;
		}
		ch = buffer.charAt(0);
		buffer.deleteCharAt(0);
		return ch;
	}

	/* (non-Javadoc)
	 * @see java.io.FilterInputStream#read(byte[], int, int)
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (len == 0) {
			return 0;
		}

		int pos = off;
		
		/*
		 * Only read available bytes so that we return as soon as possible.
		 * If there are no bytes available, then we read at least one to
		 * block until more are available.
		 * 
		 * This allows the parser to update immediately there is data
		 * available, rather than waiting for the end of document.
		 */
		int avail = available();
		if (avail > len) {
			avail = len;
		} else if (avail == 0) {
			avail = 1;
		}
		
		/*
		 * Read up to the end of a tag. This is needed so that we return after
		 * the </map> tag in order to let the XML parsing proceed.
		 */
		for (int i = 0; i < avail; i++) {
			int ch = read();
			if (ch < 0) {
				if (i > 0) {
					// return the current buffer
					break;
				}
				return -1;
			}
			b[pos++] = (byte) (ch & 0xff);
			if (state == State.FINISHED_TAG) {
				break;
			}
		}
		
		return pos - off;
	}
}