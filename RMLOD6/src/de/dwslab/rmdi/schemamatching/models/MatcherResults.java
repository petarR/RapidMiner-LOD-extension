package de.dwslab.rmdi.schemamatching.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lod.rdf.model.RdfHolder;

import com.rapidminer.operator.ResultObjectAdapter;

public class MatcherResults extends ResultObjectAdapter implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3380407615908398019L;

	List<MatchedColumnsInfo> allMatchedColumns = new ArrayList<MatchedColumnsInfo>();

	List<List<ColumnInfo>> groupedMatchedColumn = null;

	public List<MatchedColumnsInfo> getAllMatchedColumns() {
		return allMatchedColumns;
	}

	public void setAllMatchedColumns(List<MatchedColumnsInfo> allMatchedColumns) {
		this.allMatchedColumns = allMatchedColumns;
	}

	/**
	 * creates all pairs for fusing
	 * 
	 * @return
	 */
	public List<List<ColumnInfo>> getGroupedMatchedColumn() {
		if (groupedMatchedColumn == null) {
			groupedMatchedColumn = new ArrayList<List<ColumnInfo>>();

			// find all
			Map<ColumnInfo, Boolean> visitedColumns = new HashMap<ColumnInfo, Boolean>();
			// all matches per column
			Map<ColumnInfo, List<ColumnInfo>> allMatches = new HashMap<ColumnInfo, List<ColumnInfo>>();
			for (MatchedColumnsInfo info : allMatchedColumns) {
				List<ColumnInfo> leftColumnInfos = new ArrayList<ColumnInfo>();

				if (allMatches.containsKey(info.getLeftColumn())) {
					leftColumnInfos
							.addAll(allMatches.get(info.getLeftColumn()));
				}
				if (!leftColumnInfos.contains(info.getRightColumn())) {
					leftColumnInfos.add(info.getRightColumn());
				}

				allMatches.put(info.getLeftColumn(), leftColumnInfos);

				// same for the right

				List<ColumnInfo> rightColumnInfos = new ArrayList<ColumnInfo>();

				if (allMatches.containsKey(info.getRightColumn())) {
					rightColumnInfos.addAll(allMatches.get(info
							.getRightColumn()));
				}
				if (!rightColumnInfos.contains(info.getLeftColumn())) {
					rightColumnInfos.add(info.getLeftColumn());

				}
				allMatches.put(info.getRightColumn(), rightColumnInfos);

			}

			// generate unique groups to fuse
			for (Entry<ColumnInfo, List<ColumnInfo>> entry : allMatches
					.entrySet()) {

				if (visitedColumns.containsKey(entry.getKey())) {
					continue;
				}
				visitedColumns.put(entry.getKey(), true);

				List<ColumnInfo> currentColumns = new ArrayList<ColumnInfo>();
				// add the column itself
				currentColumns.add(entry.getKey());

				// get the direct matches and transitive matches
				getNextMatches(visitedColumns, currentColumns,
						entry.getValue(), allMatches);
				groupedMatchedColumn.add(currentColumns);
			}
		}

		return groupedMatchedColumn;
	}

	/**
	 * recursively adds new column matches to the given column
	 * 
	 * @param visitedColumns
	 * @param currentColumns
	 * @param currentMatches
	 * @param allMatches
	 */
	private static void getNextMatches(Map<ColumnInfo, Boolean> visitedColumns,
			List<ColumnInfo> currentColumns, List<ColumnInfo> currentMatches,
			Map<ColumnInfo, List<ColumnInfo>> allMatches) {
		// add all matches of the matches of the current column
		for (ColumnInfo c : currentMatches) {
			if (visitedColumns.containsKey(c))
				continue;
			// add the direct matches
			if (!currentColumns.contains(c))
				currentColumns.add(c);
			visitedColumns.put(c, true);
			if (allMatches.containsKey(c)) {
				getNextMatches(visitedColumns, currentColumns,
						allMatches.get(c), allMatches);
			}

		}
	}
}
