package lod.olap4ld;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lod.importers.OLAPDataImporter;
import lod.sparql.SPARQLEndpointQueryRunner;
import lod.utils.AttributeTypeGuesser;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.Ontology;

public class CubeSPARQLExplorer implements Serializable {

	private static final String SELECT_DIMENSIONS = "select ?dim ?label where"
			+ " { ?dataset <http://purl.org/linked-data/cube#structure> ?s ."
			+ " ?s <http://purl.org/linked-data/cube#component> ?c ."
			+ " ?c <http://purl.org/linked-data/cube#dimension> ?dim."
			+ "OPTIONAL{ ?dim <http://www.w3.org/2000/01/rdf-schema#label> ?label}}";
	private static final String SELECT_DIMENSIONS_FALLBACK = "SELECT distinct ?dim ?label WHERE"
			+ "{ ?observation a <http://purl.org/linked-data/cube#Observation> ."
			+ "?observation <http://purl.org/linked-data/cube#dataSet> ?dataset ."
			+ "?observation ?dim ?Y . FILTER regex(str(?dim), \".*dimension.*\") "
			+ ".OPTIONAL{ ?dim <http://www.w3.org/2000/01/rdf-schema#label> ?label}}";

	private static final String SELECT_MEASURES = "select ?measure ?label where"
			+ " { ?dataset  <http://purl.org/linked-data/cube#structure> ?s . "
			+ "?s <http://purl.org/linked-data/cube#component> ?c . "
			+ "?c <http://purl.org/linked-data/cube#measure> ?measure "
			+ ".OPTIONAL{ ?measure <http://www.w3.org/2000/01/rdf-schema#label> ?label}}";

	private static final String SELECT_MEASURES_FALLBACK = "SELECT distinct ?measure ?label WHERE"
			+ "{ ?observation a <http://purl.org/linked-data/cube#Observation> ."
			+ "?observation <http://purl.org/linked-data/cube#dataSet> ?dataset ."
			+ "?observation ?measure ?Y . FILTER regex(str(?measure), \".*measure.*\") "
			+ ".OPTIONAL{ ?measure <http://www.w3.org/2000/01/rdf-schema#label> ?memberLabel}}";

	private static final String SELECT_MEMBERS = "SELECT distinct ?memberUri ?memberLabel WHERE "
			+ "{ ?observation a <http://purl.org/linked-data/cube#Observation> . "
			+ "?observation <http://purl.org/linked-data/cube#dataSet> ?dataset ."
			+ "  ?observation ?dimURI ?memberUri . "
			+ "OPTIONAL {?memberUri  <http://www.w3.org/2004/02/skos/core#prefLabel> ?memberLabel} }";

	private static final String SELCT_ROWS = "Select OBS_VALUES where"
			+ "{?observation a <http://purl.org/linked-data/cube#Observation> . "
			+ "?observation <http://purl.org/linked-data/cube#dataSet> ?dataset ."
			+ "ROW_SELECTION" + "COLUMN_SELECTION" + "OBS_SELECTION"
			+ "COLUMN_FILTERS}";

	/**
	 * 
	 */
	private static final long serialVersionUID = 3449893208770570062L;
	// <dimLabel,dimURI>
	Map<String, String> allDimensions;
	// <measureLabel,measureURI>
	Map<String, String> allMeasures;
	// <dimensionURI,<memberLabel, memberURI,>>
	Map<String, Map<String, String>> allMembers;

	// <measureLabel,measureURI>
	Map<String, String> selectedMeasures;
	// <dimensionURI,<memberLabel, memberURI,>>
	Map<String, Map<String, String>> selectedRows;
	// <dimensionURI,<memberLabel, memberURI,>>
	Map<String, Map<String, String>> selectedColumns;
	SPARQLEndpointQueryRunner runner;

	// tmpLists

	List<String> selectedRowsByUser;

	List<String> selectedColumnsByUser;

	String endpoint;

	String datasetURI;

	public CubeSPARQLExplorer(String dataset, String endpoint) {
		this.endpoint = endpoint;
		this.datasetURI = dataset;
		allDimensions = new LinkedHashMap<String, String>();
		allMeasures = new LinkedHashMap<String, String>();
		allMembers = new LinkedHashMap<String, Map<String, String>>();

		selectedMeasures = new LinkedHashMap<String, String>();
		selectedRows = new LinkedHashMap<String, Map<String, String>>();
		selectedColumns = new LinkedHashMap<String, Map<String, String>>();

		selectedColumnsByUser = new LinkedList<String>();

		selectedRowsByUser = new LinkedList<String>();

		runner = new SPARQLEndpointQueryRunner(endpoint, "Tmp", 100000, 3,
				10000, false, false);
	}

	/**
	 * populates the members of the selected dimensions
	 */
	public void populateMembers() {
		for (String dimUri : selectedColumnsByUser) {
			try {
				getMembersOfDimension(getAllDimensions().get(dimUri), dimUri);
			} catch (OperatorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		for (String dimUri : selectedRowsByUser) {
			try {
				getMembersOfDimension(getAllDimensions().get(dimUri), dimUri);
			} catch (OperatorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * queries all members of given dimension
	 * 
	 * @return
	 * @throws OperatorException
	 */
	public void getMembersOfDimension(String dimUri, String dimLabel)
			throws OperatorException {
		Map<String, String> currentMembers = new LinkedHashMap<String, String>();

		String query = SELECT_MEMBERS.replace("?dataset",
				"<" + datasetURI + ">").replace("?dimURI", "<" + dimUri + ">");

		Query queryQ = QueryFactory.create(query);

		int offset = 0;

		ResultSet RS = runner.runSelectQuery(query);
		while (true) {
			while (RS != null && RS.hasNext()) {
				QuerySolution sol = RS.next();
				String attName = sol.get("memberUri").toString();
				String label = "";
				if (sol.contains("memberLabel")) {
					label = sol.getLiteral("memberLabel").getValue().toString();
				} else {
					label = OLAPModel.generateFriendlyNameFromNS(attName);
				}

				// add to the map of members
				currentMembers.put(label, attName);

			}
			offset += runner.getPageSize();
			queryQ.setOffset(offset);
			queryQ.setLimit(runner.getPageSize());

			RS = runner.runSelectQuery(queryQ.toString());

			if (RS == null || !RS.hasNext())
				break;

		}
		allMembers.put(dimLabel, currentMembers);

	}

	/**
	 * queries all dimensions of the dataset
	 * 
	 * @return
	 * @throws OperatorException
	 */
	public Map<String, String> queryDimensions(boolean fallBack)
			throws OperatorException {

		String query = SELECT_DIMENSIONS.replace("?dataset", "<" + datasetURI
				+ ">");
		if (fallBack)
			query = SELECT_DIMENSIONS_FALLBACK.replace("?dataset", "<"
					+ datasetURI + ">");
		ResultSet RS = runner.runSelectQuery(query);

		while (RS != null && RS.hasNext()) {
			QuerySolution sol = RS.next();
			String attName = sol.get("dim").toString();
			String label = "";
			if (sol.contains("label")) {
				label = sol.getLiteral("label").getString();
			} else {
				label = OLAPModel.generateFriendlyNameFromNS(attName);
			}

			// add to the map of dimensions
			if (!allDimensions.values().contains(attName))
				allDimensions.put(label, attName);

		}

		return allDimensions;
	}

	/**
	 * queries all dimensions of the dataset
	 * 
	 * @return
	 * @throws OperatorException
	 */
	public Map<String, String> queryMeasures(boolean fallBack)
			throws OperatorException {
		String query = SELECT_MEASURES.replace("?dataset", "<" + datasetURI
				+ ">");

		if (fallBack)
			query = SELECT_MEASURES_FALLBACK.replace("?dataset", "<"
					+ datasetURI + ">");

		ResultSet RS = runner.runSelectQuery(query);
		while (RS != null && RS.hasNext()) {
			QuerySolution sol = RS.next();
			String attName = sol.get("measure").toString();
			String label = "";
			if (sol.contains("label")) {
				label = sol.getLiteral("label").getString();
			}
			if (label == null || label.equals("")) {
				label = OLAPModel.generateFriendlyNameFromNS(attName);
			}

			// add to the map of dimensions
			if (!allMeasures.values().contains(attName))
				allMeasures.put(label, attName);

		}
		return allMeasures;
	}

	public SPARQLEndpointQueryRunner getRunner() {
		return runner;
	}

	public List<String> getSelectedColumnsByUser() {
		return selectedColumnsByUser;
	}

	public List<String> getSelectedRowsByUser() {
		return selectedRowsByUser;
	}

	public static String getSelectDimensions() {
		return SELECT_DIMENSIONS;
	}

	public static String getSelectMeasures() {
		return SELECT_MEASURES;
	}

	public static String getSelectMembers() {
		return SELECT_MEMBERS;
	}

	public void setRunner(SPARQLEndpointQueryRunner runner) {
		this.runner = runner;
	}

	public void setSelectedColumnsByUser(List<String> selectedColumnsByUser) {
		this.selectedColumnsByUser = selectedColumnsByUser;
	}

	public void setSelectedRowsByUser(List<String> selectedRowsByUser) {
		this.selectedRowsByUser = selectedRowsByUser;
	}

	public String getDatasetURI() {
		return datasetURI;
	}

	public void setDatasetURI(String datasetURI) {
		this.datasetURI = datasetURI;
	}

	public Map<String, String> getAllDimensions() {
		return allDimensions;
	}

	public void setAllDimensions(Map<String, String> allDimensions) {
		this.allDimensions = allDimensions;
	}

	public Map<String, String> getAllMeasures() {
		return allMeasures;
	}

	public void setAllMeasures(Map<String, String> allMeasures) {
		this.allMeasures = allMeasures;
	}

	public Map<String, Map<String, String>> getAllMembers() {
		return allMembers;
	}

	public void setAllMembers(Map<String, Map<String, String>> allMembers) {
		this.allMembers = allMembers;
	}

	public Map<String, String> getSelectedMeasures() {
		return selectedMeasures;
	}

	public void setSelectedMeasures(Map<String, String> selectedMeasures) {
		this.selectedMeasures = selectedMeasures;
	}

	public Map<String, Map<String, String>> getSelectedRows() {
		return selectedRows;
	}

	public void setSelectedRows(Map<String, Map<String, String>> selectedRows) {
		this.selectedRows = selectedRows;
	}

	public Map<String, Map<String, String>> getSelectedColumns() {
		return selectedColumns;
	}

	public void setSelectedColumns(
			Map<String, Map<String, String>> selectedColumns) {
		this.selectedColumns = selectedColumns;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public void loadDataset() throws OperatorException {
		queryDimensions(false);
		if (allDimensions.size() == 0)
			queryDimensions(true);
		queryMeasures(false);
		if (allMeasures.size() == 0)
			queryMeasures(true);

	}

	/**
	 * Generates complete MemoryTable for the given query
	 * 
	 * @param runner
	 * @param query
	 * @return
	 * @throws OperatorException
	 */
	public MemoryExampleTable getMemoryTableForSPARQL()
			throws OperatorException {

		Map<String, String> uniqueAttributes = generateUniqueAttributesNames();
		// add the attributes
		AttributeTypeGuesser attributeTypeGuesser = new AttributeTypeGuesser();
		Attribute[] attributes = new Attribute[uniqueAttributes.size() + 1];
		// add the id attribute
		attributes[0] = AttributeFactory.createAttribute(
				OLAPDataImporter.DIMENSION_ID, Ontology.STRING);
		int i = 0;
		for (Entry<String, String> entry : uniqueAttributes.entrySet()) {
			attributes[i + 1] = AttributeFactory.createAttribute(
					entry.getKey(), Ontology.STRING);
			i++;
		}
		MemoryExampleTable table = null;
		table = new MemoryExampleTable(attributes);

		// generate unique rows
		Map<String, String> uniqueRows = new LinkedHashMap<String, String>();
		generatePermutations(selectedRows, uniqueRows, 0, new LinkedList(), "",
				new HashMap<String, String>(), true);

		// itr
		for (Entry<String, String> entry : uniqueRows.entrySet()) {
			Map<String, String> currentRow = new HashMap<String, String>();
			String[] selRows = entry.getKey()
					.substring(1, entry.getKey().length() - 1).split("><");

			for (String selR : selRows) {
				String[] insideS = selR.split("\\[");
				currentRow.put(insideS[1].replace("]", ""), insideS[0]);
			}

			Map<String, String> solutions = getRowMeassures(currentRow);
			table.addDataRow(getDataRow(attributes, solutions,
					attributeTypeGuesser, entry.getValue()));
		}

		return table;
	}

	/**
	 * generates new data row for the given values
	 * 
	 * @param attributes
	 * @param entry
	 * @param attributeTypeGuesser
	 * @return
	 */
	private DoubleArrayDataRow getDataRow(Attribute[] attributes,
			Map<String, String> solutions,
			AttributeTypeGuesser attributeTypeGuesser, String rowName) {
		DoubleArrayDataRow row = new DoubleArrayDataRow(
				new double[attributes.length]);
		if (rowName.startsWith("_"))
			rowName = rowName.substring(1, rowName.length());
		if (rowName.endsWith("_"))
			rowName = rowName.substring(0, rowName.length() - 1);
		for (Attribute attr : attributes) {
			double value = Double.NaN;

			// set the id
			if (attr.getName().equals(OLAPDataImporter.DIMENSION_ID)) {
				value = AttributeTypeGuesser
						.getValueForAttribute(attr, rowName);
			}
			// set standard value
			else if (solutions.containsKey(attr.getName())) {
				value = AttributeTypeGuesser.getValueForAttribute(attr,
						solutions.get(attr.getName()));
			} else {
				// set missing value
				value = AttributeTypeGuesser.getValueForAttribute(attr, null);
			}
			row.set(attr, value);
		}
		return row;
	}

	private Map<String, String> generateUniqueAttributesNames() {
		Map<String, String> uniqueAttributes = new LinkedHashMap<String, String>();
		generatePermutations(selectedColumns, uniqueAttributes, 0,
				new LinkedList(), "", selectedMeasures, false);
		return uniqueAttributes;
	}

	/**
	 * generates the permutations for the attributes
	 * 
	 * @param maps
	 * @param result
	 * @param depth
	 * @param currentFullUri
	 * @param currentLabels
	 * @param selectedMeassures
	 */
	static void generatePermutations(Map<String, Map<String, String>> maps,
			Map<String, String> result, int depth, List<String> currentFullUri,
			String currentLabels, Map<String, String> selectedMeassures,
			boolean isRow) {
		if (depth == maps.size()) {
			java.util.Collections.sort(currentFullUri);
			String newAtt = "";
			for (String str : currentFullUri) {
				newAtt += str;
			}
			for (Entry<String, String> etnry : selectedMeassures.entrySet()) {

				if (!result.containsKey(newAtt + "<" + etnry.getValue() + ">"))
					result.put(newAtt + "<" + etnry.getKey().replace(" ", "_")
							+ ">",
							currentLabels + etnry.getKey().replace(" ", "_"));
			}
			if (selectedMeassures.size() == 0) {
				if (!result.containsKey(newAtt))
					result.put(newAtt, currentLabels);
			}
			return;
		}
		// find the curent dim
		int i = 0;
		Map<String, String> curMap = new LinkedHashMap<String, String>();
		String curdim = "";
		for (Entry<String, Map<String, String>> entry : maps.entrySet()) {
			if (i == depth) {
				curMap = entry.getValue();
				curdim = entry.getKey();
				break;
			}
			i++;
		}

		for (Entry<String, String> entry : curMap.entrySet()) {

			List<String> newCurr = new LinkedList<String>();
			newCurr.addAll(currentFullUri);
			String dimName = curdim;
			if (!isRow)
				dimName = OLAPModel.generateFriendlyNameFromNS(curdim);
			newCurr.add("<" + entry.getValue() + "["
					+ dimName.replace(" ", "_") + "]" + ">");
			generatePermutations(maps, result, depth + 1, newCurr,
					currentLabels + "_" + entry.getKey() + "_",
					selectedMeassures, isRow);

		}
	}

	/**
	 * runs SPARQL query to get all measures for the current row
	 * 
	 * @param rowDims
	 * @return
	 * @throws OperatorException
	 */
	public Map<String, String> getRowMeassures(Map<String, String> rowDims)
			throws OperatorException {

		Map<String, String> solutions = new HashMap<String, String>();
		ResultSet RS = runner
				.runSelectQueryInterruptable(createQueryForRow(rowDims));
		{
			while (RS.hasNext()) {
				List<String> colsMembers = new ArrayList<String>();
				QuerySolution sol = RS.next();
				for (Entry<String, String> entry : selectedMeasures.entrySet()) {
					String obsVar = entry.getKey().replace(" ", "_");
					String obsVal = sol.getLiteral(obsVar).getValue()
							.toString();

					for (Entry<String, Map<String, String>> entryCol : selectedColumns
							.entrySet()) {
						String colVar = OLAPModel.generateFriendlyNameFromNS(
								entryCol.getKey()).replace(" ", "_");
						String colVal = sol.get(colVar).toString();
						String listValue = "<" + colVal + "["
								+ OLAPModel.generateFriendlyNameFromNS(colVar)
								+ "]>";
						colsMembers.add(listValue);
					}
					java.util.Collections.sort(colsMembers);
					String newAtt = "";
					for (String str : colsMembers) {
						newAtt += str;
					}
					newAtt += "<" + obsVar + ">";

					// make sum of all values if there are more
					if (solutions.containsKey(newAtt)) {
						try {
							int val = Integer.parseInt(solutions.get(newAtt));
							val += Integer.parseInt(obsVal);
							obsVal = Integer.toString(val);
						} catch (Exception e) {

						}
					}

					solutions.put(newAtt, obsVal);

				}

			}
		}

		return solutions;
	}

	private String createQueryForRow(Map<String, String> rowDims) {
		String query = SELCT_ROWS.replace("?dataset", "<" + datasetURI + ">");

		String rowSelection = "";
		for (Entry<String, String> entry : rowDims.entrySet()) {
			rowSelection += "?observation " + "<" + entry.getKey() + "> <"
					+ entry.getValue().replace(" ", "_") + ">" + ".";
		}
		query = query.replace("ROW_SELECTION", rowSelection);

		String obsSelection = "";
		String obsVars = "";

		for (Entry<String, String> entry : selectedMeasures.entrySet()) {
			String obsVar = " ?" + entry.getKey().replace(" ", "_");
			obsVars += obsVar + " ";
			obsSelection += "?observation " + "<" + entry.getValue() + ">"
					+ obsVar + ".";
		}

		String columnSelection = "";
		String columnFilters = "";
		String columnVars = " ";

		for (Entry<String, Map<String, String>> entry : selectedColumns
				.entrySet()) {
			String var = "?"
					+ OLAPModel.generateFriendlyNameFromNS(entry.getKey());
			columnVars += var.replace(" ", "_") + " ";
			columnSelection += "?observation " + "<" + entry.getKey() + "> "
					+ var + ".";
			columnFilters += "FILTER(";
			for (Entry<String, String> insideEntry : entry.getValue()
					.entrySet()) {
				String varValue = " = <" + insideEntry.getValue() + "> || ";
				if (insideEntry.getValue().contains("^^")) {
					String parts[] = insideEntry.getValue().split("\\^\\^");
					varValue = " = \"" + parts[0] + "\"^^<" + parts[1]
							+ "> || ";
				}
				columnFilters += var + varValue;
			}
			columnFilters = columnFilters.substring(0,
					columnFilters.length() - 4) + ").";
		}

		query = query.replace("COLUMN_SELECTION", columnSelection);
		query = query.replace("COLUMN_FILTERS", columnFilters);
		query = query.replace("OBS_SELECTION", obsSelection);

		query = query.replace("OBS_VALUES", obsVars + columnVars);

		return query;
	}

	public static void main(String[] args) {
		// Map<String, String> map = new HashMap<String, String>();
		// map.put("F", "F");
		// map.put("M", "M");
		//
		// Map<String, String> map2 = new HashMap<String, String>();
		// map2.put("2012", "2012");
		// map2.put("2013", "2013");
		//
		// Map<String, String> map3 = new HashMap<String, String>();
		// map3.put("A", "A");
		// map3.put("Y", "Y");
		// // map.put("empty", "is not ");
		// // String output = MapUtils.mapToString(map);
		// // System.out.println(output);
		// // Map<String, String> parsedMap = MapUtils.stringToMap(output);
		// Map<String, Map<String, String>> mapBig = new LinkedHashMap<String,
		// Map<String, String>>();
		// mapBig.put("first", map);
		// mapBig.put("second", map2);
		// mapBig.put("third", map3);
		//
		// Map<String, String> meaMap = new HashMap<String, String>();
		// meaMap.put("mea1", "mea1");
		// meaMap.put("mea2", "mea2");
		//
		// Map<String, String> results = new LinkedHashMap<String, String>();
		// generatePermutations(mapBig, results, 0, new LinkedList(), "",
		// meaMap);
		//
		// for (Entry<String, String> entry : results.entrySet()) {
		// System.out.println(entry.getKey() + "=" + entry.getValue());
		// }

		CubeSPARQLExplorer exp = new CubeSPARQLExplorer(
				"http://worldbank.270a.info/dataset/world-bank-climates/year-average-anomaly",
				"http://worldbank.270a.info/sparql");

		Map<String, Map<String, String>> selectedRows = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> rowsValues = new LinkedHashMap<String, String>();
		rowsValues.put("http://worldbank.270a.info/classification/basin/100",
				"http://worldbank.270a.info/classification/basin/100");
		rowsValues.put("http://worldbank.270a.info/classification/basin/101",
				"http://worldbank.270a.info/classification/basin/101");
		rowsValues.put("http://worldbank.270a.info/classification/basin/102",
				"http://worldbank.270a.info/classification/basin/102");
		selectedRows.put(
				"http://purl.org/linked-data/sdmx/2009/dimension#refArea",
				rowsValues);

		Map<String, Map<String, String>> selectedColumns = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> colsValues = new LinkedHashMap<String, String>();
		colsValues
				.put("http://reference.data.gov.uk/id/gregorian-interval/2020-01-01T00:00:00/P20Y",
						"http://reference.data.gov.uk/id/gregorian-interval/2020-01-01T00:00:00/P20Y");
		colsValues
				.put("http://reference.data.gov.uk/id/gregorian-interval/2060-01-01T00:00:00/P20Y",
						"http://reference.data.gov.uk/id/gregorian-interval/2060-01-01T00:00:00/P20Y");
		colsValues
				.put("http://reference.data.gov.uk/id/gregorian-interval/2040-01-01T00:00:00/P20Y",
						"http://reference.data.gov.uk/id/gregorian-interval/2040-01-01T00:00:00/P20Y");

		selectedColumns.put(
				"http://purl.org/linked-data/sdmx/2009/dimension#refPeriod",
				colsValues);
		Map<String, String> selMeasures = new LinkedHashMap<String, String>();
		selMeasures.put("measure",
				"http://worldbank.270a.info/property/year-average");

		exp.setSelectedColumns(selectedColumns);
		exp.setSelectedMeasures(selMeasures);
		exp.setSelectedRows(selectedRows);

		try {
			exp.getMemoryTableForSPARQL();
		} catch (OperatorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// dimensions.put(key, value)
	}
}
