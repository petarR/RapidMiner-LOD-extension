package de.dwslab.rmdi.schemamatching.matchers.lod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lod.rdf.model.RdfHolder;
import de.dwslab.rmdi.schemamatching.models.MatchedColumnsInfo;
import de.dwslab.rmdi.schemamatching.models.MatcherResults;

/**
 * @author petar
 * 
 */
public abstract class AbstractLODMatcher {

	public AbstractLODMatcher(Map<String, Object> parameters,
			List<RdfHolder> allRDFs) {
		this.allRDFs = allRDFs;
	}

	/**
	 * holds the matching results
	 */
	public MatcherResults matchingResults;
	/**
	 * holds all rdfs
	 */
	List<RdfHolder> allRDFs;

	public MatcherResults getMatchingResults() {
		return matchingResults;
	}

	/**
	 * Matches all rdfs
	 * 
	 */
	public void doMatching() {
		matchingResults = new MatcherResults();
		List<MatchedColumnsInfo> matchedColumns = new ArrayList<MatchedColumnsInfo>();
		List<RdfHolder> mergedHolders = mergeRDFsBySource(allRDFs);
		allRDFs = mergedHolders;
		// match two by two
		for (int i = 0; i < mergedHolders.size() - 1; i++) {
			RdfHolder leftTable = mergedHolders.get(i);
			for (int j = i + 1; j < mergedHolders.size(); j++) {
				RdfHolder rightTable = mergedHolders.get(j);
				matchedColumns.addAll(matchTwoRdfs(leftTable, rightTable));
			}
		}
		matchingResults.setAllMatchedColumns(matchedColumns);

	}

	/**
	 * matches two rdfs
	 * 
	 * @param leftTable
	 * @param rightTable
	 * @return
	 */
	public abstract List<MatchedColumnsInfo> matchTwoRdfs(RdfHolder leftTable,
			RdfHolder rightTable);

	/**
	 * groups the rdfs by source
	 * 
	 * @param allRdfs
	 * @return
	 */
	public static Map<String, List<RdfHolder>> groupRDFbySource(
			List<RdfHolder> allRdfs) {
		Map<String, List<RdfHolder>> groupedBySource = new HashMap<String, List<RdfHolder>>();

		int i = 0;
		for (RdfHolder holder : allRdfs) {
			i++;
			if (holder.getSource() == null || holder.getSource().equals(""))
				holder.setSource(Integer.toString(i));
			if (groupedBySource.containsKey(holder.getSource())) {
				groupedBySource.get(holder.getSource()).add(holder);
			} else {
				List<RdfHolder> newList = new ArrayList<RdfHolder>();
				newList.add(holder);
				groupedBySource.put(holder.getSource(), newList);
			}
		}

		return groupedBySource;

	}

	/**
	 * groups and merges rdfs by source
	 * 
	 * @param allRdfs
	 * @return
	 */
	public static List<RdfHolder> mergeRDFsBySource(List<RdfHolder> allRdfs) {
		List<RdfHolder> newRdfs = new ArrayList<RdfHolder>();
		Map<String, List<RdfHolder>> groupedBySource = groupRDFbySource(allRdfs);

		for (List<RdfHolder> listHolders : groupedBySource.values()) {
			newRdfs.add(RdfHolder.mergeHolders(listHolders));
		}

		return newRdfs;
	}
}
