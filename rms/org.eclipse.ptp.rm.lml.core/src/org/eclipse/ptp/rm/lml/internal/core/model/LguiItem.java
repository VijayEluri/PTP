/**
 * Copyright (c) 2011 Forschungszentrum Juelich GmbH
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * 		Claudia Knobloch, Carsten Karbach, FZ Juelich
 */
package org.eclipse.ptp.rm.lml.internal.core.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.eclipse.ptp.rm.lml.core.ILMLCoreConstants;
import org.eclipse.ptp.rm.lml.core.JobStatusData;
import org.eclipse.ptp.rm.lml.core.events.ILguiUpdatedEvent;
import org.eclipse.ptp.rm.lml.core.listeners.ILguiListener;
import org.eclipse.ptp.rm.lml.core.model.ILguiHandler;
import org.eclipse.ptp.rm.lml.core.model.ILguiItem;
import org.eclipse.ptp.rm.lml.core.model.IPattern;
import org.eclipse.ptp.rm.lml.core.util.JAXBUtil;
import org.eclipse.ptp.rm.lml.internal.core.elements.CellType;
import org.eclipse.ptp.rm.lml.internal.core.elements.ColumnType;
import org.eclipse.ptp.rm.lml.internal.core.elements.ComponentlayoutType;
import org.eclipse.ptp.rm.lml.internal.core.elements.InfoType;
import org.eclipse.ptp.rm.lml.internal.core.elements.InfodataType;
import org.eclipse.ptp.rm.lml.internal.core.elements.InformationType;
import org.eclipse.ptp.rm.lml.internal.core.elements.LayoutRequestType;
import org.eclipse.ptp.rm.lml.internal.core.elements.LguiType;
import org.eclipse.ptp.rm.lml.internal.core.elements.ObjectFactory;
import org.eclipse.ptp.rm.lml.internal.core.elements.ObjectType;
import org.eclipse.ptp.rm.lml.internal.core.elements.ObjectsType;
import org.eclipse.ptp.rm.lml.internal.core.elements.RequestType;
import org.eclipse.ptp.rm.lml.internal.core.elements.RowType;
import org.eclipse.ptp.rm.lml.internal.core.elements.TableType;
import org.eclipse.ptp.rm.lml.internal.core.events.LguiUpdatedEvent;

/**
 * Class of the interface ILguiItem
 */
public class LguiItem implements ILguiItem {

	/*
	 * Source of the XML-file from which the LguiType was generated.
	 */
	private String name;

	/*
	 * The generated LguiType
	 */
	private volatile LguiType lgui;

	/**
	 * collects listeners, which listen for changes in model
	 */
	private final List<ILguiListener> listeners = new LinkedList<ILguiListener>();

	private Map<String, List<IPattern>> filters = new HashMap<String, List<IPattern>>();

	/**
	 * List of encapsulated classes, which handle parts of the lml-hierarchy
	 */
	private final Map<Class<? extends ILguiHandler>, ILguiHandler> lguiHandlers = Collections
			.synchronizedMap(new HashMap<Class<? extends ILguiHandler>, ILguiHandler>());

	/*
	 * Map containing jobs under our control
	 */
	private final Map<String, JobStatusData> fJobMap = Collections.synchronizedMap(new TreeMap<String, JobStatusData>());

	private final JAXBUtil jaxbUtil = JAXBUtil.getInstance();

	private RequestType request;

	private String username = null;

	private boolean lockUpdate = true;

	private boolean lockPattern = false;

	/**
	 * Constructor with LML-model as argument
	 * 
	 * @param lgui
	 *            LML-model
	 */
	public LguiItem(LguiType lgui) {
		this.lgui = lgui;
		createLguiHandlers();
	}

	/**
	 * 
	 */
	public LguiItem(String name, String username) {
		this.name = name;
		this.username = username;
	}

	/**
	 * Add a lml-data-listener. It listens for data-changes.
	 * 
	 * @param listener
	 *            new listening instance
	 */
	public void addListener(ILguiListener listener) {
		listeners.add(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.rm.lml.core.model.ILguiItem#addUserJob(java.lang.String,
	 * org.eclipse.ptp.rm.lml.core.model.jobs.JobStatusData)
	 */
	public void addUserJob(String jobId, JobStatusData status, boolean force) {
		final JobStatusData jobStatus = fJobMap.get(jobId);

		/*
		 * If the job already exists, do nothing
		 */
		if (jobStatus == null) {
			if (force) {
				final OverviewAccess overview = getOverviewAccess();
				if (overview != null) {
					String oid = overview.getOIDByJobId(jobId);
					if (oid == null) {
						final TableHandler handler = getTableHandler();
						if (handler != null) {
							final TableType table = handler.getTable(getGidFromJobStatus(status.getState()));
							if (table != null) {
								oid = generateOid();
								status.setOid(oid);
								addJobToTable(table, oid, status);
							}
						}
					}
				}
			}
			fJobMap.put(jobId, status);
		}
	}

	public String[] getColumnTitlePattern(String gid) {
		final List<String> titles = new ArrayList<String>();
		final List<IPattern> patternList = filters.get(gid);
		if (patternList != null) {
			for (final IPattern pattern : patternList) {
				titles.add(pattern.getColumnTitle());
			}
		}
		return titles.toArray(new String[titles.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.rm.lml.core.model.ILguiItem#getCurrentLayout(java.io. OutputStream)
	 */
	public void getCurrentLayout(OutputStream output) {
		while (lockPattern) {
			// wait until the pattern have been set
			System.out.print(ILMLCoreConstants.EMPTY);
		}
		lockUpdate = true;
		LguiType layoutLgui = null;
		if (lgui == null) {
			layoutLgui = firstRequest();
		} else {
			layoutLgui = getLayoutAccess().getLayoutFromModel();
			layoutLgui.setRequest(request);
		}
		jaxbUtil.marshal(layoutLgui, output);
	}

	/**
	 * @return object to map component-ids to corresponding layout definitions
	 */
	public LayoutAccess getLayoutAccess() {
		if (lguiHandlers.get(LayoutAccess.class) == null) {
			return null;
		}
		return (LayoutAccess) lguiHandlers.get(LayoutAccess.class);
	}

	public LguiType getLguiType() {
		return lgui;
	}

	public String[] getMessageOfTheDay() {
		String type = new String();
		String message = new String();
		final List<String> oidList = new LinkedList<String>();
		if (getOverviewAccess().getObjects() != null) {
			for (final ObjectsType objects : getOverviewAccess().getObjects()) {
				for (final ObjectType object : objects.getObject()) {
					if (object.getType().value().equals(ILMLCoreConstants.SYSTEM)) {
						oidList.add(object.getId());
					}
				}
			}
		}
		for (final String oid : oidList) {
			for (final InfodataType data : getOverviewAccess().getInformation(oid).getData()) {
				if (data.getKey().equals(ILMLCoreConstants.MOTD)) {
					type = ILMLCoreConstants.MOTD;
					message = data.getValue();
				} else if (data.getKey().equals(ILMLCoreConstants.ERROR)) {
					return new String[] { ILMLCoreConstants.ERROR, data.getValue() };
				}
			}
		}
		return new String[] { type, message };
	}

	/**
	 * @return NodedisplayAccess-instance for accessing layouts of nodedisplays
	 */
	public NodedisplayAccess getNodedisplayAccess() {
		if (lguiHandlers.get(NodedisplayAccess.class) == null) {
			return null;
		}
		return (NodedisplayAccess) lguiHandlers.get(NodedisplayAccess.class);
	}

	/**
	 * @return a object, which saves which object has to be highlighted. All user interactions are saved globally for all components
	 *         in this object.
	 */
	public ObjectStatus getObjectStatus() {
		if (lguiHandlers.get(ObjectStatus.class) == null) {
			return null;
		}
		return (ObjectStatus) lguiHandlers.get(ObjectStatus.class);
	}

	/**
	 * @return object for getting infos for objects
	 */
	public OIDToInformation getOIDToInformation() {
		if (lguiHandlers.get(OIDToInformation.class) == null) {
			return null;
		}
		return (OIDToInformation) lguiHandlers.get(OIDToInformation.class);
	}

	/**
	 * @return a class, which provides an index for fast access to objects within the objects tag of LML. You can pass the id of the
	 *         objects to the returned object. It then returns the corresponding objects.
	 */
	public OIDToObject getOIDToObject() {
		if (lguiHandlers.get(OIDToObject.class) == null) {
			return null;
		}
		return (OIDToObject) lguiHandlers.get(OIDToObject.class);
	}

	public OverviewAccess getOverviewAccess() {
		if (lguiHandlers.get(OverviewAccess.class) == null) {
			return null;
		}
		return (OverviewAccess) lguiHandlers.get(OverviewAccess.class);
	}

	public Map<String, List<IPattern>> getPattern() {
		return filters;
	}

	public List<IPattern> getPattern(String gid) {
		if (filters.containsKey(gid)) {
			return filters.get(gid);
		}
		return new LinkedList<IPattern>();
	}

	public TableHandler getTableHandler() {
		if (lguiHandlers.get(TableHandler.class) == null) {
			return null;
		}
		return (TableHandler) lguiHandlers.get(TableHandler.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.rm.lml.core.model.ILguiItem#getUserJob(java.lang.String)
	 */
	public JobStatusData getUserJob(String jobId) {
		final JobStatusData status = fJobMap.get(jobId);
		if (status != null && !status.isRemoved()) {
			return status;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.rm.lml.core.model.ILguiItem#getUserJobs()
	 */
	public JobStatusData[] getUserJobs() {
		synchronized (fJobMap) {
			return fJobMap.values().toArray(new JobStatusData[fJobMap.values().size()]);
		}
	}

	public String getUsername() {
		if (username == null) {
			return new String();
		}
		return username;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.rm.lml.core.elements.ILguiItem#getVersion()
	 */
	public String getVersion() {
		return lgui.getVersion();
	}

	public boolean isEmpty() {
		final TableHandler handler = getTableHandler();
		if (handler != null) {
			return handler.getTables().get(0).getRow().size() == 0;
		}
		return true;
	}

	public boolean isFilterOwnJobActive(String gid) {
		final List<IPattern> filters = getPattern(gid);
		if (filters.size() > 0) {
			for (final IPattern filter : filters) {
				if (filter.getColumnTitle().equals("owner")) {
					if (filter.getRelationOperator().equals("=") && filter.getRelationValue().equals(username)) {
						return true;
					} else {
						break;
					}
				}
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.rm.lml.core.elemhents.ILguiItem#isLayout()
	 */
	public boolean isLayout() {
		return lgui != null && lgui.isLayout();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.rm.lml.core.model.ILguiItem#notifyListeners()
	 */
	public void notifyListeners() {
		final LguiUpdatedEvent event = new LguiUpdatedEvent(this, lgui);
		for (final ILguiListener listener : listeners) {
			listener.handleEvent(event);
		}
	}

	public void reloadLastLayout(StringBuilder layout) {
		LguiType lguiType = null;
		if (layout.length() > 0) {
			lguiType = jaxbUtil.unmarshal(layout.toString());
		}
		if (lguiType != null) {
			for (final Object object : jaxbUtil.getObjects(lguiType)) {
				if (object instanceof ComponentlayoutType) {
					lgui = lguiType;
					if (listeners.isEmpty()) {
						createLguiHandlers();
					}
					break;
				}
			}
		}
	}

	/**
	 * Remove a lml-data-listener.
	 * 
	 * @param listener
	 *            listening instance
	 */
	public void removeListener(ILguiListener listener) {
		listeners.remove(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.rm.lml.core.model.ILguiItem#removeUserJob(java.lang.String )
	 */
	public void removeUserJob(String jobId) {
		final JobStatusData status = fJobMap.get(jobId);
		if (status != null) {
			final TableHandler handler = getTableHandler();
			if (handler != null) {
				final TableType table = handler.getTable(getGidFromJobStatus(status.getState()));
				if (table != null) {
					int index = -1;
					for (int i = 0; i < table.getRow().size(); i++) {
						final RowType row = table.getRow().get(i);
						final String rowJobId = handler.getCellValue(table, row, JOB_ID);
						if (rowJobId.equals(jobId)) {
							index = i;
							break;
						}
					}
					if (index >= 0) {
						table.getRow().remove(index);
					}
				}
				status.setRemoved();
			}
		}
	}

	public String saveCurrentLayout() {

		final StringWriter writer = new StringWriter();
		LguiType layoutLgui = null;
		if (lgui == null) {
			layoutLgui = firstRequest();
		} else {
			layoutLgui = getLayoutAccess().getLayoutFromModel();
		}
		jaxbUtil.marshal(layoutLgui, writer);
		return writer.getBuffer().toString();
	}

	public void setPattern(Map<String, List<IPattern>> pattern) {
		if (pattern != null) {
			filters = pattern;
		}
	}

	public void setPattern(String gid, List<IPattern> filterValues) {
		while (lockUpdate) {
			// wait until the update with the server is finished
			System.out.print("");
		}
		lockPattern = true;
		if (filters.containsKey(gid)) {
			filters.remove(gid);
		}
		filters.put(gid, filterValues);
		lockPattern = false;
	}

	public void setRequest(RequestType request) {
		this.request = request;
		if (lgui != null) {
			final LayoutRequestType layoutReq = new LayoutRequestType();
			layoutReq.setGetDefaultData(true);

			this.request.setLayoutManagement(layoutReq);
			lgui.setRequest(this.request);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.rm.lml.core.elements.ILguiItem#toString
	 */
	@Override
	public String toString() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.rm.lml.core.model.ILguiItem#update(java.io.InputStream)
	 */
	public void update(InputStream stream) {

		final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		StringBuilder xmlStream = new StringBuilder();
		String s;
		try {
			while (null != (s = reader.readLine())) {
				xmlStream.append(s);
			}
		} catch (final IOException e) {
			xmlStream = new StringBuilder();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (final IOException e) {
					xmlStream = new StringBuilder();
				}
			}
		}
		if (xmlStream.length() > 0) {
			lgui = jaxbUtil.unmarshal(xmlStream.toString());
			if (listeners.isEmpty()) {
				createLguiHandlers();
			}
			fireUpdatedEvent();

			if (!cidSet()) {
				setCid();
			}
			updateJobData();
		}
		lockUpdate = false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ptp.rm.lml.core.model.ILguiItem#updateUserJob(java.lang.String , java.lang.String, java.lang.String)
	 */
	public void updateUserJob(String jobId, String status, String detail) {
		final JobStatusData jobStatus = fJobMap.get(jobId);
		if (jobStatus != null && status != null) {
			final String gidOld = getGidFromJobStatus(jobStatus.getState());
			final String gidNew = getGidFromJobStatus(status);
			final TableHandler handler = getTableHandler();
			if (handler != null) {
				final TableType tableOld = handler.getTable(gidOld);
				if (tableOld != null) {
					RowType rowOld = null;
					int index = -1;
					if (tableOld.getRow() != null) {
						for (int i = 0; i < tableOld.getRow().size(); i++) {
							final RowType row = tableOld.getRow().get(i);
							if (handler.getCellValue(tableOld, row, JOB_ID).equals(jobId)) {
								handler.setCellValue(tableOld, row, JOB_STATUS, status);
								rowOld = row;
								index = i;
								break;
							}
						}
					}
					if (!gidOld.equals(gidNew)) {
						final TableType tableNew = handler.getTable(gidNew);
						if (tableNew != null) {
							if (index >= 0 && rowOld != null) {
								final RowType rowNew = new RowType();
								rowNew.setOid(rowOld.getOid());
								for (final ColumnType columnOld : tableOld.getColumn()) {
									final CellType cellNew = new CellType();
									cellNew.setCid(columnOld.getId());
									boolean filled = false;
									for (final ColumnType columnNew : tableNew.getColumn()) {
										if (columnOld.getName().equals(columnNew.getName())) {
											for (final CellType cellOld : rowOld.getCell()) {
												if (cellOld.getCid().equals(columnOld.getId())) {
													cellNew.setValue(cellOld.getValue());
													filled = true;
													break;
												}
											}
										}
									}
									if (!filled) {
										cellNew.setValue("?"); //$NON-NLS-1$
									}
									rowNew.getCell().add(cellNew);
								}
								tableNew.getRow().add(rowNew);
								tableOld.getRow().remove(index);
							}
						}
					}

				}
				jobStatus.updateState(status, detail);
			}
		}
	}

	private void addCellToRow(RowType row, ColumnType column, String value) {
		final CellType cell = new CellType();
		cell.setCid(column.getId());
		cell.setValue(value);
		row.getCell().add(cell);
	}

	private void addJobToTable(TableType table, String oid, JobStatusData status) {
		final RowType row = new RowType();
		row.setOid(oid);

		for (final ColumnType column : table.getColumn()) {
			if (column.getName().equals(JOB_ID)) {
				addCellToRow(row, column, status.getJobId());
			} else if (column.getName().equals(JOB_STATUS)) {
				addCellToRow(row, column, status.getState());
			} else if (column.getName().equals(JOB_OWNER)) {
				addCellToRow(row, column, status.getOwner());
			} else if (column.getName().equals(JOB_QUEUE_NAME)) {
				addCellToRow(row, column, status.getQueueName());
			}
		}

		table.getRow().add(row);
	}

	private void checkTables(TableHandler handler) {
		if (handler != null) {
			if (handler.getTable(ACTIVE_JOB_TABLE) == null) {
				handler.generateDefaultTable(ACTIVE_JOB_TABLE);
			}
			if (handler.getTable(INACTIVE_JOB_TABLE) == null) {
				handler.generateDefaultTable(INACTIVE_JOB_TABLE);
			}
		}
	}

	private boolean cidSet() {
		final TableHandler handler = getTableHandler();
		if (handler != null) {
			for (final TableType table : handler.getTables()) {
				for (final RowType row : table.getRow()) {
					for (final CellType cell : row.getCell()) {
						if (cell.getCid() == null) {
							return false;
						}
					}
				}
			}
		}

		return true;
	}

	/**
	 * The instance lgui is filled with a new data-model. This method creates all modules, which handle the data. These modules can
	 * then be accessed by corresponding getter-functions.
	 */
	private void createLguiHandlers() {
		lguiHandlers.put(OverviewAccess.class, new OverviewAccess(this, lgui));
		lguiHandlers.put(LayoutAccess.class, new LayoutAccess(this, lgui));
		lguiHandlers.put(OIDToObject.class, new OIDToObject(this, lgui));
		lguiHandlers.put(ObjectStatus.class, new ObjectStatus(this, lgui));
		lguiHandlers.put(OIDToInformation.class, new OIDToInformation(this, lgui));
		lguiHandlers.put(TableHandler.class, new TableHandler(this, lgui));
		lguiHandlers.put(NodedisplayAccess.class, new NodedisplayAccess(this, lgui));
	}

	private void fireUpdatedEvent() {
		final ILguiUpdatedEvent e = new LguiUpdatedEvent(this, lgui);
		for (final ILguiListener listener : listeners) {
			listener.handleEvent(e);
		}
	}

	private LguiType firstRequest() {
		final ObjectFactory objectFactory = new ObjectFactory();

		final LguiType layoutLgui = objectFactory.createLguiType();
		layoutLgui.setVersion("1"); //$NON-NLS-1$
		layoutLgui.setLayout(true);

		final LayoutRequestType layoutReq = objectFactory.createLayoutRequestType();
		layoutReq.setGetDefaultData(true);

		request.setLayoutManagement(layoutReq);

		layoutLgui.setRequest(request);

		return layoutLgui;
	}

	private String generateOid() {
		return UUID.randomUUID().toString();
	}

	private String getGidFromJobStatus(String status) {
		if (status.equals(JobStatusData.RUNNING)) {
			return ACTIVE_JOB_TABLE;
		}
		return INACTIVE_JOB_TABLE;
	}

	private void setCid() {
		final TableHandler handler = getTableHandler();
		if (handler != null) {
			for (final TableType table : handler.getTables()) {
				for (final RowType row : table.getRow()) {
					int cid = 1;
					for (final CellType cell : row.getCell()) {
						if (cell.getCid() == null) {
							cell.setCid(BigInteger.valueOf(cid));
						} else {
							cid = cell.getCid().intValue();
						}
						cid++;
					}
				}
			}
		}
	}

	/**
	 * Update the job map with the new job data. On this refresh, the new job that was added to the table should have been
	 * "discovered" by the scheduler, so it should appear in one of the job tables. We need to find these jobs and update the OID
	 * and status information.
	 */
	private void updateJobData() {
		final Set<JobStatusData> jobsInTable = new HashSet<JobStatusData>();
		final List<String> oidsToRemove = new ArrayList<String>();

		/*
		 * First check for jobs that are in the table and update them
		 */
		for (final InformationType information : getOverviewAccess().getInformations()) {
			for (final InfoType info : information.getInfo()) {
				final String jobId = getOverviewAccess().getInfodataValue(info, JOB_ID);
				if (jobId != null) {
					final JobStatusData status = fJobMap.get(jobId);
					if (status != null) {
						if (!status.isRemoved() && !status.isCompleted()) {
							/*
							 * job exists in both map and LML, so update the map with the oid and latest status
							 */
							status.setOid(info.getOid());
							status.setState(getOverviewAccess().getInfodataValue(info, JOB_STATUS));
							/*
							 * Remember this job is in the table for later
							 */
							jobsInTable.add(status);
						} else {
							/*
							 * job has been removed by the user. remove it from the table
							 */
							oidsToRemove.add(info.getOid());
						}
					}
				}
			}
		}

		/*
		 * Remove any rows for removed jobs
		 */
		final TableHandler handler = getTableHandler();
		if (handler != null) {
			for (final TableType table : handler.getTables()) {
				for (final String row : oidsToRemove) {
					table.getRow().remove(row);
				}
				oidsToRemove.clear();
			}

			checkTables(handler);

			/*
			 * Next find any jobs that are no longer in any of the tables. We need to create a "fake" entry in the jobslistwait
			 * table for these. Note that these jobs are now considered "COMPLETED".
			 */
			TableType table = getTableHandler().getTable(INACTIVE_JOB_TABLE);
			if (table == null) {
				table = getTableHandler().generateDefaultTable(INACTIVE_JOB_TABLE);
			}

			synchronized (fJobMap) {
				for (final JobStatusData status : fJobMap.values()) {
					if (!status.isRemoved() && !jobsInTable.contains(status)) {
						if (!status.isCompleted()) {
							status.setState(JobStatusData.COMPLETED);
							status.setOid(generateOid());
						}
						addJobToTable(table, status.getOid(), status);
					}
				}
			}
		}

	}

}