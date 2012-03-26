/**
 * Copyright (c) 2011 Forschungszentrum Juelich GmbH
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * 		Claudia Knobloch, FZ Juelich
 */

package org.eclipse.ptp.rm.lml.ui.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.ptp.rm.lml.core.LMLManager;
import org.eclipse.ptp.rm.lml.core.events.ILguiAddedEvent;
import org.eclipse.ptp.rm.lml.core.events.ILguiRemovedEvent;
import org.eclipse.ptp.rm.lml.core.events.IMarkObjectEvent;
import org.eclipse.ptp.rm.lml.core.events.ISelectObjectEvent;
import org.eclipse.ptp.rm.lml.core.events.ITableFilterEvent;
import org.eclipse.ptp.rm.lml.core.events.ITableSortedEvent;
import org.eclipse.ptp.rm.lml.core.events.IUnmarkObjectEvent;
import org.eclipse.ptp.rm.lml.core.events.IUnselectedObjectEvent;
import org.eclipse.ptp.rm.lml.core.events.IViewUpdateEvent;
import org.eclipse.ptp.rm.lml.core.listeners.ILMLListener;
import org.eclipse.ptp.rm.lml.internal.core.elements.InfodataType;
import org.eclipse.ptp.rm.lml.ui.ILMLUIConstants;
import org.eclipse.ptp.rm.lml.ui.UIUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.part.ViewPart;

public class InfoView extends ViewPart {

	public class LMLListener implements ILMLListener {

		public void handleEvent(ILguiAddedEvent event) {
			UIUtils.safeRunSyncInUIThread(new SafeRunnable() {
				public void run() throws Exception {
					if (parent != null && content == null) {
						setNewLabelText(ILMLUIConstants.INFO_MOTD, null);
					}
				}
			});
		}

		public void handleEvent(ILguiRemovedEvent event) {
			UIUtils.safeRunSyncInUIThread(new SafeRunnable() {
				public void run() throws Exception {
					if (parent != null && content != null) {
						removeLabelText();
					}
				}
			});
		}

		public void handleEvent(final IMarkObjectEvent event) {
			UIUtils.safeRunSyncInUIThread(new SafeRunnable() {
				public void run() throws Exception {
					if (parent != null) {
						removeLabelText();
						setNewLabelText(ILMLUIConstants.INFO_JOB, event.getOid());
					}
				}
			});
		}

		public void handleEvent(final ISelectObjectEvent event) {
		}

		public void handleEvent(ITableFilterEvent event) {
		}

		public void handleEvent(ITableSortedEvent event) {
		}

		public void handleEvent(IUnmarkObjectEvent event) {
		}

		public void handleEvent(IUnselectedObjectEvent event) {
		}

		public void handleEvent(IViewUpdateEvent event) {
		}

	}

	private final LMLManager lmlManager = LMLManager.getInstance();

	private final ILMLListener lmlListener = new LMLListener();

	private Composite parent;

	private Widget content = null;

	private IMenuManager viewMenuManager;

	private ActionContributionItem showMessageOfTheDayActionItem;

	@Override
	public void createPartControl(Composite parent) {
		lmlManager.addListener(lmlListener, this.getClass().getName());

		this.parent = parent;
		parent.setLayout(new FillLayout(SWT.VERTICAL));
		parent.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

		viewMenuManager = getViewSite().getActionBars().getMenuManager();
		final IAction showMessageOfTheDayAction = new Action("Show Message of the Day") {

			@Override
			public void run() {
				removeLabelText();
				setNewLabelText(ILMLUIConstants.INFO_MOTD, null);
			}
		};
		showMessageOfTheDayActionItem = new ActionContributionItem(showMessageOfTheDayAction);
		viewMenuManager.add(showMessageOfTheDayActionItem);
		showMessageOfTheDayActionItem.getAction().setEnabled(false);

		parent.layout();

	}

	@Override
	public void dispose() {
		lmlManager.removeListener(lmlListener);
	}

	public void removeLabelText() {
		if (!content.isDisposed()) {
			content.dispose();
		}
		content = null;

		showMessageOfTheDayActionItem.getAction().setEnabled(false);

		parent.layout();
	}

	@Override
	public void setFocus() {
	}

	public void setNewLabelText(String type, String oid) {
		if (type.equals(ILMLUIConstants.INFO_MOTD)) {
			content = new Text(parent, SWT.LEFT | SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL);
			((Text) content).setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
			final String[] message = lmlManager.getSelectedLguiItem().getMessageOfTheDay();

			if (message[0].length() == 0) {
				return;
			}
			if (message[0].equals(ILMLUIConstants.INFO_ERROR)) {
				((Text) content).append(message[1]);
			} else if (message[0].equals(ILMLUIConstants.INFO_MOTD)) {
				((Text) content).append(message[1]);
			}
		} else if (type.equals(ILMLUIConstants.INFO_JOB)) {
			content = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION);
			((Table) content).setLinesVisible(true);
			((Table) content).setHeaderVisible(true);
			final String[] titles = { ILMLUIConstants.INFO_KEY, ILMLUIConstants.INFO_VALUES };
			for (final String title : titles) {
				final TableColumn column = new TableColumn((Table) content, SWT.NONE);
				column.setText(title);
				column.setWidth(100);
			}
			if (lmlManager.getSelectedLguiItem().getOIDToInformation().getInfoByOid(oid) != null) {
				for (final InfodataType infodata : lmlManager.getSelectedLguiItem().getOIDToInformation().getInfoByOid(oid)
						.getData()) {
					final TableItem item = new TableItem(((Table) content), SWT.NONE);
					item.setText(0, infodata.getKey());
					item.setText(1, infodata.getValue());
				}
			}

			for (final TableColumn column : ((Table) content).getColumns()) {
				column.pack();
			}
		} else if (type.equals(ILMLUIConstants.INFO_NODE)) {
			content = new Text(parent, SWT.LEFT | SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
			((Text) content).setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
			((Text) content).append("Node");
		}

		showMessageOfTheDayActionItem.getAction().setEnabled(true);

		parent.layout();
	}

}
