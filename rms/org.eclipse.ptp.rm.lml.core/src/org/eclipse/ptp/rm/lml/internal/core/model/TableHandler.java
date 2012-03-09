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

package org.eclipse.ptp.rm.lml.internal.core.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.ptp.rm.lml.core.ILMLCoreConstants;
import org.eclipse.ptp.rm.lml.core.JobStatusData;
import org.eclipse.ptp.rm.lml.core.LMLCorePlugin;
import org.eclipse.ptp.rm.lml.core.LMLManager;
import org.eclipse.ptp.rm.lml.core.events.ILguiUpdatedEvent;
import org.eclipse.ptp.rm.lml.core.listeners.ILguiListener;
import org.eclipse.ptp.rm.lml.core.model.ILguiItem;
import org.eclipse.ptp.rm.lml.core.model.IPattern;
import org.eclipse.ptp.rm.lml.core.model.ITableColumnLayout;
import org.eclipse.ptp.rm.lml.internal.core.elements.CellType;
import org.eclipse.ptp.rm.lml.internal.core.elements.ColumnType;
import org.eclipse.ptp.rm.lml.internal.core.elements.ColumnlayoutType;
import org.eclipse.ptp.rm.lml.internal.core.elements.Columnsortedtype;
import org.eclipse.ptp.rm.lml.internal.core.elements.GobjectType;
import org.eclipse.ptp.rm.lml.internal.core.elements.LguiType;
import org.eclipse.ptp.rm.lml.internal.core.elements.PatternMatchType;
import org.eclipse.ptp.rm.lml.internal.core.elements.PatternType;
import org.eclipse.ptp.rm.lml.internal.core.elements.RowType;
import org.eclipse.ptp.rm.lml.internal.core.elements.SortingType;
import org.eclipse.ptp.rm.lml.internal.core.elements.TableType;
import org.eclipse.ptp.rm.lml.internal.core.elements.TablelayoutType;

public class TableHandler extends LguiHandler {

	public class TableListener implements ILguiListener {

		public void handleEvent(ILguiUpdatedEvent e) {
			update(e.getLgui());
		}

	}

	private final TableListener tableListener = new TableListener();

	public TableHandler(ILguiItem lguiItem, LguiType lgui) {
		super(lguiItem, lgui);
		lguiItem.addListener(tableListener);
	}

	/**
	 * Change the table column order
	 * 
	 * @param gid
	 *            ID of the table layout
	 * @param order
	 *            new order of columns
	 */
	public void changeTableColumnsOrder(String gid, int[] order) {
		final List<ColumnlayoutType> newColumnLayouts = new ArrayList<ColumnlayoutType>();
		final List<ColumnlayoutType> oldColumnLayouts = lguiItem
				.getLayoutAccess().getTableLayout(gid).getColumn();
		for (int i = 0; i < order.length; i++) {
			for (final ColumnlayoutType column : oldColumnLayouts) {
				if (BigInteger.valueOf(order[i]).equals(column.getPos())) {
					final ColumnlayoutType columnNew = column;
					columnNew.setPos(BigInteger.valueOf(i));
					newColumnLayouts.add(columnNew);
					lguiItem.getLayoutAccess().getTableLayout(gid).getColumn()
							.remove(column);
					break;
				}
			}
		}
		for (final ColumnlayoutType column : newColumnLayouts) {
			lguiItem.getLayoutAccess().getTableLayout(gid).getColumn()
					.add(column);
		}
	}

	/**
	 * Change the table column widths
	 * 
	 * @param gid
	 *            ID of the table layout
	 * @param widths
	 *            new column widths
	 */
	public void changeTableColumnsWidth(String gid, Double[] widths) {
		final BigInteger[] cids = getCids(gid);
		for (int i = 0; i < widths.length; i++) {
			for (final ColumnlayoutType column : lguiItem.getLayoutAccess()
					.getLayoutColumsToCids(cids, gid)) {
				if (column.getPos() != null
						&& BigInteger.valueOf(i).equals(column.getPos())) {
					column.setWidth(widths[i]);
					break;
				}
			}
		}
	}

	public TableType generateDefaultTable(String gid) {
		final TableType table = new TableType();
		table.setId(gid);
		table.setTitle(ILMLCoreConstants.TITLE_PREFIX + gid);
		table.setContenttype(ILguiItem.CONTENT_JOBS);

		for (final ColumnlayoutType columnLayout : lguiItem.getLayoutAccess()
				.getTableLayout(gid).getColumn()) {
			if (columnLayout.isActive()) {
				final ColumnType column = new ColumnType();
				column.setId(columnLayout.getCid());
				column.setName(columnLayout.getKey());
				generateDefaultSorting(column);
				if (columnLayout.getKey().equals(ILguiItem.JOB_OWNER)) {
					generateDefaultPattern(".*", column); //$NON-NLS-1$
				}
				if (columnLayout.getKey().equals(ILguiItem.JOB_STATUS)) {
					column.setType(ITableColumnLayout.COLUMN_TYPE_MANDATORY);
					if (gid.equals(ILguiItem.ACTIVE_JOB_TABLE)) {
						generateDefaultPattern(JobStatusData.RUNNING, column);
					} else {
						generateDefaultPattern(JobStatusData.SUBMITTED, column);
					}
				}
				table.getColumn().add(column);
			}
		}
		jaxbUtil.addTable(lgui, table);
		return table;
	}

	public ITableColumnLayout[] getActiveTableColumnLayout(String gid) {
		final ITableColumnLayout[] allColumns = getTableColumnLayout(gid);
		final List<ITableColumnLayout> activeColumns = new ArrayList<ITableColumnLayout>();
		for (final ITableColumnLayout allColumn : allColumns) {
			if (allColumn.isActive()) {
				activeColumns.add(allColumn);
			}
		}
		return activeColumns.toArray(new ITableColumnLayout[activeColumns.size()]);
	}

	public String getCellValue(TableType table, RowType row, String colName) {
		final BigInteger index = getColumnIndex(table, colName);
		if (index != null) {
			for (final CellType cell : row.getCell()) {
				if (cell.getCid().equals(index)) {
					return cell.getValue();
				}
			}
		}
		return null;
	}

	/**
	 * Currently unused
	 */
	public int getNumberOfTableColumns(String gid) {
		final TablelayoutType layout = lguiItem.getLayoutAccess()
				.getTableLayout(gid);
		int activeColumn = 0;
		for (final ColumnlayoutType column : layout.getColumn()) {
			if (column.isActive()) {
				activeColumn++;
			}
		}
		return activeColumn;
	}

	/**
	 * Currently unused
	 */
	public int getNumberOfTables() {
		return getTables().size();
	}

	public Object[] getSortProperties(String gid) {
		final Object[] values = new Object[2];
		values[0] = -1;
		values[1] = ITableColumnLayout.SORT_DIRECTION_NONE;

		final TablelayoutType tableLayout = lguiItem.getLayoutAccess()
				.getTableLayout(gid);
		for (final ColumnlayoutType columnLayout : tableLayout.getColumn()) {
			if (!columnLayout.getSorted().equals(Columnsortedtype.FALSE)) {
				values[0] = columnLayout.getPos().intValue();
				if (columnLayout.getSorted().equals(Columnsortedtype.ASC)) {
					values[1] = ITableColumnLayout.SORT_DIRECTION_UP;
				} else {
					values[1] = ITableColumnLayout.SORT_DIRECTION_DOWN;
				}
			}
		}
		return values;
	}

	/**
	 * Getting an element(Table Type) which has an equal title to the argument
	 * tableType.
	 * 
	 * @param gid
	 *            ID of the desired table
	 * @return Corresponding table to the desired table title
	 */
	public TableType getTable(String gid) {
		for (final TableType tag : getTables()) {
			if (tag.getId().equals(gid)) {
				return tag;
			}
		}
		LMLCorePlugin.log("No table found for gid \"" + gid + "\"!"); //$NON-NLS-1$ //$NON-NLS-2$
		return null;
	}

	/**
	 * Currently unused
	 */
	public String[] getTableColumnActive(String gid) {
		final TablelayoutType tableLayout = lguiItem.getLayoutAccess()
				.getTableLayout(gid);
		final ArrayList<String> tableColumnNonActive = new ArrayList<String>();
		for (int i = 0; i < tableLayout.getColumn().size(); i++) {
			if (tableLayout.getColumn().get(i).isActive()) {
				tableColumnNonActive.add(tableLayout.getColumn().get(i)
						.getKey());
			}
		}
		return tableColumnNonActive.toArray(new String[tableColumnNonActive
				.size()]);
	}

	/**
	 * Get the column layout for the table
	 * 
	 * @param gid
	 *            ID of the table layout
	 * @return column layout
	 */
	/**
	 * Get the column layout for the table
	 * 
	 * @param gid
	 *            ID of the table layout
	 * @return column layout
	 */
	public ITableColumnLayout[] getTableColumnLayout(String gid) {
		if (lguiItem == null && lguiItem.getLayoutAccess() == null) {
			return new ITableColumnLayout[0];
		}

		final BigInteger[] cids = getCids(gid);

		// layoutColumns are sorted by cids
		final ColumnlayoutType[] layoutColumns = lguiItem.getLayoutAccess()
				.getLayoutColumsToCids(cids, gid);
		final ITableColumnLayout[] tableColumnLayouts = new ITableColumnLayout[layoutColumns.length];

		for (int i = 0; i < layoutColumns.length; i++) {
			final ColumnlayoutType column = layoutColumns[i];
			if (column != null) {
				String style;
				final String sort = getColumnSortProperty(getTable(gid), cids,
						i);
				// when there is a change
				if (column.getSorted() != null
						&& column.getSorted().value() != null && sort != null
						&& sort.equals("numeric")) {
					style = ITableColumnLayout.COLUMN_STYLE_RIGHT;
				} else {
					style = ITableColumnLayout.COLUMN_STYLE_LEFT;
				}
				tableColumnLayouts[i] = new TableColumnLayout(column.getKey(),
						column.getWidth(), style, column.isActive(), sort);
			}
		}
		return tableColumnLayouts;
	}

	/**
	 * Not currently used
	 */
	public String[] getTableColumnNonActive(String gid) {
		final TablelayoutType tableLayout = lguiItem.getLayoutAccess()
				.getTableLayout(gid);
		final ArrayList<String> tableColumnNonActive = new ArrayList<String>();
		for (int i = 0; i < tableLayout.getColumn().size(); i++) {
			if (!tableLayout.getColumn().get(i).isActive()) {
				tableColumnNonActive.add(tableLayout.getColumn().get(i)
						.getKey());
			}
		}
		return tableColumnNonActive.toArray(new String[tableColumnNonActive
				.size()]);
	}

	public String[] getTableColumnTitles(String gid) {
		if (lguiItem == null && lguiItem.getLayoutAccess() == null) {
			return null;
		}
		final ColumnlayoutType[] layoutColumns = lguiItem.getLayoutAccess()
				.getLayoutColumsToCids(getCidsActiveColumns(gid), gid);
		final List<String> titles = new ArrayList<String>();
		for (final ColumnlayoutType layoutColumn : layoutColumns) {
			if (layoutColumn.getKey() != null) {
				titles.add(layoutColumn.getKey());
			}
		}

		return titles.toArray(new String[titles.size()]);
	}

	/**
	 * Get the table with color information added
	 * 
	 * @param gid
	 *            ID of the table layout
	 * @return rows with color information added
	 */
	public Row[] getTableDataWithColor(String gid, boolean addColor) {
		final BigInteger[] cids = getCids(gid);
		final TableType table = getTable(gid);
		if (table == null) {
			return new Row[0];
		}
		final Row[] tableData = new Row[table.getRow().size()];
		for (int i = 0; i < tableData.length; i++) {
			final RowType row = table.getRow().get(i);
			tableData[i] = new Row();
			tableData[i].setOid(row.getOid());
			if (addColor) {
				tableData[i].setColor(lguiItem.getOIDToObject().getColorById(
						row.getOid()));
			}

			final BigInteger jobIdIndex = getColumnIndex(table,
					ILguiItem.JOB_ID);
			final Cell[] tableDataRow = new Cell[cids.length];
			String jobId = null;
			for (int j = 0; j < cids.length; j++) {
				for (final CellType cell : row.getCell()) {
					if (cell.getCid() != null && cell.getCid().equals(cids[j])) {
						tableDataRow[j] = new Cell(cell.getValue(),
								tableData[i]);
						break;
					}
				}
				if (tableDataRow[j] == null) {
					tableDataRow[j] = new Cell("?", tableData[i]); //$NON-NLS-1$
				}
				if (cids[j].equals(jobIdIndex)) {
					jobId = tableDataRow[j].value;
				}
			}
			if (jobId != null) {
				JobStatusData status = LMLManager.getInstance().getUserJob(
						lguiItem.toString(), jobId);
				if (status == null) {
					final String queueName = getCellValue(table, row,
							ILguiItem.JOB_QUEUE_NAME);
					final String owner = getCellValue(table, row,
							ILguiItem.JOB_OWNER);
					status = new JobStatusData(lguiItem.toString(), jobId,
							queueName, owner, null, null, false);
				}
				tableData[i].setJobStatusData(status);
			}
			tableData[i].setCells(tableDataRow);
		}
		return tableData;
	}

	public Row[] getTableDataWithColor(String gid, boolean addColor, List<IPattern> filterTitlesValues) {
		final Row[] rows = getTableDataWithColor(gid, addColor);
		if (rows.length == 0) {
			return rows;
		}
		final BigInteger[] cids = getCids(gid);
		final TableType table = getTable(gid);

		// Convert Map filterTitlesValues into Map filterPosValues
		final Map<Integer, IPattern> filterPosValues = new HashMap<Integer, IPattern>();
		for (final IPattern pattern : filterTitlesValues) {
			BigInteger cid = BigInteger.valueOf(-1);
			for (final ColumnType column : table.getColumn()) {
				if (column.getName().equals(pattern.getColumnTitle())) {
					cid = column.getId();
					break;
				}
			}
			if (cid.equals(BigInteger.valueOf(-1))) {
				continue;
			}
			int position = -1;
			for (int i = 0; i < cids.length; i++) {
				if (cids[i].equals(cid)) {
					position = i;
					break;
				}
			}
			if (position == -1) {
				continue;
			}

			filterPosValues.put(position, pattern);
		}

		// Filter the rows
		final List<Row> filterRows = new ArrayList<Row>();
		for (final Row row : rows) {
			boolean allIncluded = true;
			for (final int position : filterPosValues.keySet()) {

				if (row.cells[position] == null) {
					continue;
				}
				final IPattern pattern = filterPosValues.get(position);
				final String rowValue = row.cells[position].value;

				final String type = pattern.getType();

				if (pattern.isRange()) {
					// Range
					final String minValue = pattern.getMinValueRange();
					final String maxValue = pattern.getMaxValueRange();
					if (type.equals("numeric")) {
						if ((Integer.valueOf(rowValue) < Integer.valueOf(minValue))
								|| (Integer.valueOf(maxValue) < Integer.valueOf(rowValue))) {
							allIncluded = false;
							break;
						}
					} else {
						if (rowValue.compareTo(minValue) < 0 || maxValue.compareTo(rowValue) < 0) {
							allIncluded = false;
							break;
						}
					}
				} else if (pattern.isRelation()) {
					// Relation
					final String compareValue = pattern.getRelationValue();
					final String compareOperator = pattern.getRelationOperator();
					if (type.equals("numeric")) {
						final int rowValueInt = Integer.valueOf(rowValue);
						final int compareValueInt = Integer.valueOf(compareValue);
						if (compareOperator.equals("=") && rowValueInt != compareValueInt) {
							allIncluded = false;
							break;
						} else if (compareOperator.equals("!=") && rowValueInt == compareValueInt) {
							allIncluded = false;
							break;
						} else if (compareOperator.equals("<") && rowValueInt >= compareValueInt) {
							allIncluded = false;
							break;
						} else if (compareOperator.equals("<=") && rowValueInt > compareValueInt) {
							allIncluded = false;
							break;
						} else if (compareOperator.equals(">") && rowValueInt <= compareValueInt) {
							allIncluded = false;
							break;
						} else if (compareOperator.equals(">=") && rowValueInt < compareValueInt) {
							allIncluded = false;
							break;
						}
					} else {
						if ((compareOperator.equals("=") && !compareValue.equals(rowValue))
								|| (compareOperator.equals("!=") && compareValue.equals(rowValue))) {
							allIncluded = false;
							break;
						} else if (type.equals("alpha")) {
							if ((compareOperator.equals("=~") && !rowValue.contains(compareValue))
									|| (compareOperator.equals("!~") && rowValue.contains(compareValue))) {
								allIncluded = false;
								break;
							}
						} else if (type.equals("date")) {
							if (compareOperator.equals("<") && rowValue.compareTo(compareValue) >= 0) {
								allIncluded = false;
								break;
							} else if (compareOperator.equals("<=") && rowValue.compareTo(compareValue) > 0) {
								allIncluded = false;
								break;
							} else if (compareOperator.equals(">") && rowValue.compareTo(compareValue) <= 0) {
								allIncluded = false;
								break;
							} else if (compareOperator.equals(">=") && rowValue.compareTo(compareValue) < 0) {
								allIncluded = false;
								break;
							}
						}
					}
				}
			}
			if (allIncluded) {
				filterRows.add(row);
			}
		}

		return filterRows.toArray(new Row[filterRows.size()]);
	}

	/**
	 * Getting a list of all elements of type TableType from LguiType.
	 * 
	 * @return list of elements(TableType)
	 */
	public List<TableType> getTables() {
		final List<TableType> tables = new LinkedList<TableType>();
		for (final GobjectType tag : lguiItem.getOverviewAccess()
				.getGraphicalObjects()) {
			if (tag instanceof TableType) {
				tables.add((TableType) tag);
			}
		}
		return tables;
	}

	/**
	 * Not currently used
	 */
	public String getTableTitle(String gid) {
		final TableType table = getTable(gid);
		if (table == null) {
			return ""; //$NON-NLS-1$
		}
		return table.getTitle();
	}

	/**
	 * Not currently used
	 */
	public boolean isEmpty(String gid) {
		return (getTable(gid) == null);
	}

	public String setCellValue(TableType table, RowType row, String colName,
			String value) {
		final BigInteger index = getColumnIndex(table, colName);
		if (index != null) {
			for (final CellType cell : row.getCell()) {
				if (cell.getCid().equals(index)) {
					final String oldVal = cell.getValue();
					cell.setValue(value);
					return oldVal;
				}
			}
		}
		return null;
	}

	public void setSortProperties(String gid, int sortIndex,
			String sortDirection) {
		final TablelayoutType tableLayout = lguiItem.getLayoutAccess()
				.getTableLayout(gid);

		// Delete old sorting settings
		for (final ColumnlayoutType column : tableLayout.getColumn()) {
			if (column.getSorted() != Columnsortedtype.FALSE) {
				column.setSorted(Columnsortedtype.FALSE);
			}
		}

		// Check if there is a sorting
		if (!sortDirection.equals(ITableColumnLayout.SORT_DIRECTION_NONE)
				&& sortIndex != -1) {
			// Find the column and check for the sorting direction
			for (final ColumnlayoutType column : tableLayout.getColumn()) {
				if (column.getPos().equals(BigInteger.valueOf(sortIndex))) {
					if (sortDirection
							.equals(ITableColumnLayout.SORT_DIRECTION_UP)) {
						column.setSorted(Columnsortedtype.ASC);
					} else {
						column.setSorted(Columnsortedtype.DESC);
					}
				}
			}
		}

	}

	public void setTableColumnActive(String gid, String text,
			boolean activeTableColumn) {
		final List<ColumnlayoutType> columnLayouts = lguiItem.getLayoutAccess()
				.getTableLayout(gid).getColumn();
		for (final ColumnlayoutType column : columnLayouts) {
			if (column.getKey().equals(text)) {
				column.setActive(activeTableColumn);
				break;
			}
		}
	}

	/**
	 * Sorting of the data which should be displayed in a table.
	 * 
	 * @param gid
	 *            ID of the table
	 * @param sortDirectionComparator
	 *            Should always be the int value of ascending sorting.
	 * @param sortIndex
	 * @param sortDirection
	 */
	public void sort(String gid, int sortDirectionComparator, int sortIndex,
			int sortDirection) {
		final TableType table = getTable(gid);
		if (table != null && sortIndex != -1) {
			final BigInteger[] cids = getCids(gid);
			if (cids.length > sortIndex) {
				final RowType[] jobTableData = getTableData(table, cids);
				Arrays.sort(jobTableData, new TableSorter(
						getColumnSortProperty(table, cids, sortIndex),
						sortDirectionComparator, sortIndex, sortDirection));
				table.getRow().clear();
				for (final RowType element : jobTableData) {
					table.getRow().add(element);
				}
			}
		}
	}

	private void generateDefaultPattern(String regexp, ColumnType column) {
		final PatternType pattern = new PatternType();
		final PatternMatchType patternMatch = new PatternMatchType();
		patternMatch.setRegexp(regexp);
		jaxbUtil.addPatternInclude(pattern, patternMatch);
		column.setPattern(pattern);
	}

	private void generateDefaultSorting(ColumnType column) {
		if (column.getName().equals(ILguiItem.JOB_ID)
				|| column.getName().equals(ILguiItem.JOB_OWNER)
				|| column.getName().equals(ILguiItem.JOB_QUEUE_NAME)
				|| column.getName().equals(ILguiItem.JOB_STATUS)) {
			column.setSort(SortingType.ALPHA);
		} else if (column.getName().equals(ILguiItem.JOB_WALL)
				|| column.getName().equals(ILguiItem.JOB_TOTAL_CORES)) {
			column.setSort(SortingType.NUMERIC);
		} else {
			column.setSort(SortingType.DATE);
		}

	}

	/**
	 * Get column indexes of active columns for the table layout
	 * 
	 * @param gid
	 *            ID of the table layout
	 * @return array of column indexes
	 */
	private BigInteger[] getCids(String gid) {
		final TablelayoutType layout = lguiItem.getLayoutAccess()
				.getTableLayout(gid);
		final BigInteger[] cids = new BigInteger[layout.getColumn().size()];
		for (int i = 0; i < layout.getColumn().size(); i++) {
			for (final ColumnlayoutType column : layout.getColumn()) {
				if (column.getPos() != null
						&& column.getPos().equals(BigInteger.valueOf(i))) {
					cids[i] = column.getCid();
				}
			}
		}
		return cids;
	}

	/**
	 * Get column indexes of active columns for the table layout
	 * 
	 * @param gid
	 *            ID of the table layout
	 * @return array of column indexes
	 */
	private BigInteger[] getCidsActiveColumns(String gid) {
		final TablelayoutType layout = lguiItem.getLayoutAccess()
				.getTableLayout(gid);
		final List<BigInteger> cids = new ArrayList<BigInteger>();
		for (int i = 0; i < layout.getColumn().size(); i++) {
			for (final ColumnlayoutType column : layout.getColumn()) {
				if (column.getPos() != null
						&& column.getPos().equals(BigInteger.valueOf(i))
						&& column.isActive()) {
					cids.add(column.getCid());
				}
			}
		}
		return cids.toArray(new BigInteger[cids.size()]);
	}

	private BigInteger getColumnIndex(TableType table, String colName) {
		if (table != null) {
			for (final ColumnType column : table.getColumn()) {
				if (colName.equals(column.getName())) {
					return column.getId();
				}
			}
		}
		return null;
	}

	/**
	 * Get the column sort property for the given column index
	 * 
	 * @param table
	 *            table containing columns
	 * @param cids
	 *            column indexes
	 * @param index
	 *            index of column
	 * @return sort property
	 */
	private String getColumnSortProperty(TableType table, BigInteger[] cids,
			int index) {
		for (final ColumnType column : table.getColumn()) {
			if (column != null) {
				if (column.getId().equals(cids[index])) {
					return column.getSort().value();
				}
			}
		}
		return "alpha";
	}

	/**
	 * 
	 * @return
	 */
	private RowType[] getTableData(TableType table, BigInteger[] cids) {
		final RowType[] tableData = new RowType[table.getRow().size()];
		for (int i = 0; i < tableData.length; i++) {
			final RowType row = table.getRow().get(i);
			tableData[i] = new RowType();
			if (row.getOid() != null) {
				tableData[i].setOid(row.getOid());
			}
			for (final BigInteger cid : cids) {
				boolean exists = false;
				for (final CellType cell : row.getCell()) {
					if (cell.getCid().equals(cid)) {
						tableData[i].getCell().add(cell);
						exists = true;
						break;
					}
				}
				if (!exists) {
					final CellType newCell = new CellType();
					newCell.setCid(cid);
					newCell.setValue("?"); //$NON-NLS-1$
					tableData[i].getCell().add(newCell);
				}
			}
		}
		return tableData;
	}

	/**
	 * Not currently used
	 */
	@SuppressWarnings("unused")
	private void reduceColumnPos(String gid, int pos, int i) {
		if (pos == i) {
			return;
		}
		final List<ColumnlayoutType> columnLayouts = lguiItem.getLayoutAccess()
				.getTableLayout(gid).getColumn();
		for (final ColumnlayoutType column : columnLayouts) {
			if (column.getPos() != null && column.getPos().intValue() == pos) {
				column.setPos(BigInteger.valueOf(pos - 1));
				reduceColumnPos(gid, pos + 1, i);
				break;
			}
		}
	}

}
