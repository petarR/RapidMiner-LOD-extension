package lod.olap4ld;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.sound.midi.Patch;

import org.olap4j.OlapException;
import org.olap4j.driver.olap4ld.Olap4ldUtil;
import org.olap4j.driver.olap4ld.helper.Olap4ldLinkedDataUtil;
import org.olap4j.driver.olap4ld.linkeddata.BaseCubeOp;
import org.olap4j.driver.olap4ld.linkeddata.EmbeddedSesameEngine;
import org.olap4j.driver.olap4ld.linkeddata.LogicalOlapOp;
import org.olap4j.driver.olap4ld.linkeddata.LogicalOlapQueryPlan;
import org.olap4j.driver.olap4ld.linkeddata.Restrictions;
import org.olap4j.driver.olap4ld.linkeddata.SliceOp;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Resource;

public class OLAPModel implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5159344465364649060L;

	private EmbeddedSesameEngine lde;

	private HashMap<Integer, List<Node[]>> membersofdimensions;
	private List<Node[]> dimensions;
	private List<Node[]> measures;
	private List<Node[]> cubes;
	private List<Node[]> hierarchies;
	private List<Node[]> levels;
	private List<Node[]> members;
	private Map<String, Map<Integer, String>> dimensionEasyMap;
	private Map<String, Map<Integer, String>> measureEasyMap;

	private List<String> selectedColumns;

	private List<String> selectedRows;

	private List<String> selectedMeasures;

	private Map<String, Map<String, Map<Integer, String>>> selectedValuesPerDimension;

	public Map<String, Map<String, Map<Integer, String>>> getSelectedValuesPerDimension() {
		return selectedValuesPerDimension;
	}

	public void setSelectedValuesPerDimension(
			Map<String, Map<String, Map<Integer, String>>> selectedValuesPerDimension) {
		this.selectedValuesPerDimension = selectedValuesPerDimension;
	}

	/**
	 * keeps both rows and column
	 */
	private List<String> selectedDimensions;

	private LogicalOlapQueryPlan currentPlan;

	private String datasetURL;

	public List<String> getSelectedDimensions() {
		if (selectedDimensions == null)
			selectedDimensions = new LinkedList<String>();
		return selectedDimensions;
	}

	public void setSelectedDimensions(List<String> selectedDimensions) {
		this.selectedDimensions = selectedDimensions;
	}

	public void setDatasetURL(String datasetURL) {
		this.datasetURL = datasetURL;
	}

	public String getDatasetURL() {
		return datasetURL;
	}

	public LogicalOlapQueryPlan getCurrentPlan() {
		return currentPlan;
	}

	public void setCurrentPlan(LogicalOlapQueryPlan currentPlan) {
		this.currentPlan = currentPlan;
	}

	public void setSelectedColumns(List<String> selectedColumns) {
		this.selectedColumns = selectedColumns;
	}

	public List<String> getSelectedColumns() {
		return selectedColumns;
	}

	public void setSelectedRows(List<String> selectedRows) {
		this.selectedRows = selectedRows;
	}

	public List<String> getSelectedRows() {
		return selectedRows;
	}

	public List<String> getSelectedMeasures() {
		return selectedMeasures;
	}

	public void setSelectedMeasures(List<String> selectedMeasures) {
		this.selectedMeasures = selectedMeasures;
	}

	public EmbeddedSesameEngine getLde() {
		return lde;
	}

	public void setLde(EmbeddedSesameEngine lde) {
		this.lde = lde;
	}

	public HashMap<Integer, List<Node[]>> getMembersofdimensions() {
		return membersofdimensions;
	}

	public void setMembersofdimensions(
			HashMap<Integer, List<Node[]>> membersofdimensions) {
		this.membersofdimensions = membersofdimensions;
	}

	public List<Node[]> getDimensions() {
		return dimensions;
	}

	public void setDimensions(List<Node[]> dimensions) {
		this.dimensions = dimensions;
	}

	public List<Node[]> getMeasures() {
		return measures;
	}

	public void setMeasures(List<Node[]> measures) {
		this.measures = measures;
	}

	public List<Node[]> getCubes() {
		return cubes;
	}

	public void setCubes(List<Node[]> cubes) {
		this.cubes = cubes;
	}

	public List<Node[]> getHierarchies() {
		return hierarchies;
	}

	public void setHierarchies(List<Node[]> hierarchies) {
		this.hierarchies = hierarchies;
	}

	public List<Node[]> getLevels() {
		return levels;
	}

	public void setLevels(List<Node[]> levels) {
		this.levels = levels;
	}

	public List<Node[]> getMembers() {
		return members;
	}

	public void setMembers(List<Node[]> members) {
		this.members = members;
	}

	public Map<String, Map<Integer, String>> getDimensionEasyMap() {
		return dimensionEasyMap;
	}

	public void setDimensionEasyMap(
			Map<String, Map<Integer, String>> dimensionEasyMap) {
		this.dimensionEasyMap = dimensionEasyMap;
	}

	public Map<String, Map<Integer, String>> getMeasureEasyMap() {
		return measureEasyMap;
	}

	public void setMeasureEasyMap(
			Map<String, Map<Integer, String>> measureEasyMap) {
		this.measureEasyMap = measureEasyMap;
	}

	public List<Node[]> getNewDimensions() {
		return newDimensions;
	}

	public void setNewDimensions(List<Node[]> newDimensions) {
		this.newDimensions = newDimensions;
	}

	public List<Node[]> getNewMeasures() {
		return newMeasures;
	}

	public void setNewMeasures(List<Node[]> newMeasures) {
		this.newMeasures = newMeasures;
	}

	// used for reduced dimensions
	private List<Node[]> newDimensions = new LinkedList<Node[]>();
	// used for reduced meassures
	List<Node[]> newMeasures = new LinkedList<Node[]>();

	public OLAPModel(String datasetURI) {
		this.datasetURL = datasetURI;
	}

	public OLAPModel() throws SQLException {

		Olap4ldUtil.prepareLogging();
		// Logging
		// For debugging purposes
		// Olap4ldUtil._log.setLevel(Level.CONFIG);

		// For monitoring usage
		Olap4ldUtil._log.setLevel(Level.INFO);

		// For warnings (and errors) only
		// Olap4ldUtil._log.setLevel(Level.WARNING);

		try {
			// Must have settings without influence on query processing
			URL serverUrlObject = new URL("http://example.de");
			List<String> datastructuredefinitions = new ArrayList<String>();
			List<String> datasets = new ArrayList<String>();
			lde = new EmbeddedSesameEngine(serverUrlObject,
					datastructuredefinitions, datasets, "EMBEDDEDSESAME");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * loads the initial dataset into the OLAP4LD model
	 * 
	 * @param dsUri
	 */
	public void loadDataset(String dsUri) {
		this.datasetURL = dsUri;
		Restrictions restrictions = new Restrictions();
		restrictions.cubeNamePattern = new Resource(dsUri);

		try {
			// In order to fill the engine with data
			this.cubes = lde.getCubes(restrictions);

			Map<String, Integer> cubemap = Olap4ldLinkedDataUtil
					.getNodeResultFields(cubes.get(0));
			System.out.println("CUBE_NAME: "
					+ cubes.get(1)[cubemap.get("?CUBE_NAME")]);

			// Get all metadata

			this.measures = lde.getMeasures(restrictions);

			Map<String, Integer> measuremap = Olap4ldLinkedDataUtil
					.getNodeResultFields(measures.get(0));

			this.dimensions = lde.getDimensions(restrictions);

			Map<String, Integer> dimensionmap = Olap4ldLinkedDataUtil
					.getNodeResultFields(dimensions.get(0));

			this.hierarchies = lde.getHierarchies(restrictions);

			this.levels = lde.getLevels(restrictions);

			this.members = lde.getMembers(restrictions);

			// Collect members
			this.membersofdimensions = new HashMap<Integer, List<Node[]>>();
			for (int i = 1; i < dimensions.size(); i++) {
				restrictions = new Restrictions();
				restrictions.cubeNamePattern = new Resource(dsUri);
				restrictions.dimensionUniqueName = dimensions.get(i)[dimensionmap
						.get("?DIMENSION_UNIQUE_NAME")];
				// Fix all members: We get all members no matter from which
				// hierarchy?
				List<Node[]> members = lde.getMembers(restrictions);
				membersofdimensions.put(
						restrictions.dimensionUniqueName.hashCode(), members);

				Map<String, Integer> membermap = Olap4ldLinkedDataUtil
						.getNodeResultFields(members.get(0));
				// for (Node[] nodes : members) {
				// System.out.println(nodes[membermap
				// .get("?MEMBER_UNIQUE_NAME")]);
				// }

				// build the dimensions and meassures
				buildObservations();
				buildDimensions();
			}

		} catch (OlapException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * generate the observations in easy map
	 */
	public void buildObservations() {
		// this is used for easy translation of dimensions and ids
		measureEasyMap = new HashMap<String, Map<Integer, String>>();

		Map<String, Integer> measuremap = Olap4ldLinkedDataUtil
				.getNodeResultFields(measures.get(0));
		boolean first = true;
		int i = 0;
		for (Node[] nodes : measures) {

			// Not the the header
			if (first) {
				first = false;
				i++;
				continue;
			}
			String friendlyName = null; // nodes[measuremap.get("?MEASURE_CAPTION")]
			// .toString();
			if (friendlyName == null)
				friendlyName = generateFriendlyNameFromNS(nodes[measuremap
						.get("?MEASURE_UNIQUE_NAME")].toString());
			Map<Integer, String> entry = new HashMap<Integer, String>();
			entry.put(i,
					nodes[measuremap.get("?MEASURE_UNIQUE_NAME")].toString());
			measureEasyMap.put(friendlyName, entry);

			System.out.println(nodes[measuremap.get("?MEASURE_UNIQUE_NAME")]);
			i++;
		}
	}

	/**
	 * build the dimensions into easy map
	 * 
	 */
	public void buildDimensions() {
		// this is used for easy translation of dimensions and ids
		dimensionEasyMap = new HashMap<String, Map<Integer, String>>();

		Map<String, Integer> dimensionmap = Olap4ldLinkedDataUtil
				.getNodeResultFields(dimensions.get(0));
		boolean first = true;
		int i = 0;
		for (Node[] nodes : dimensions) {

			if (first) {
				first = false;
				i++;
				continue;
			}
			if (nodes[dimensionmap.get("?DIMENSION_UNIQUE_NAME")].toString()
					.equals(Olap4ldLinkedDataUtil.MEASURE_DIMENSION_NAME)) {
				continue;
			}

			String friendlyName = nodes[dimensionmap.get("?DIMENSION_CAPTION")]
					.toString();
			if (friendlyName == null || friendlyName.equals("null"))
				friendlyName = generateFriendlyNameFromNS(nodes[dimensionmap
						.get("?DIMENSION_UNIQUE_NAME")].toString());
			Map<Integer, String> entry = new HashMap<Integer, String>();
			entry.put(i, nodes[dimensionmap.get("?DIMENSION_UNIQUE_NAME")]
					.toString());
			dimensionEasyMap.put(friendlyName, entry);
			System.out
					.println(nodes[dimensionmap.get("?DIMENSION_UNIQUE_NAME")]);
			i++;
		}

	}

	/**
	 * gets the last fraction of the URI
	 * 
	 * @return
	 */
	public static String generateFriendlyNameFromNS(String inString) {
		// it is a literal
		if (inString.contains("^^"))
			return inString.substring(0, inString.indexOf("^^"));
		if (inString.contains("/"))
			inString = inString.substring(inString.lastIndexOf("/") + 1);
		if (inString.contains("#"))
			inString = inString.substring(inString.lastIndexOf("#") + 1);
		inString = inString.substring(0, 1).toUpperCase()
				+ inString.substring(1);

		return inString;

	}

	/**
	 * returns the members of any dimension by id
	 * 
	 * @param id
	 */
	public Map<String, Map<Integer, String>> getMembersDimension(int id) {
		Map<String, Integer> dimensionmap = Olap4ldLinkedDataUtil
				.getNodeResultFields(dimensions.get(0));
		List<Node[]> members = membersofdimensions
				.get(dimensions.get(id)[dimensionmap
						.get("?DIMENSION_UNIQUE_NAME")].hashCode());
		Map<String, Map<Integer, String>> membersEasyMap = new HashMap<String, Map<Integer, String>>();

		Map<String, Integer> membermap = Olap4ldLinkedDataUtil
				.getNodeResultFields(members.get(0));

		boolean first = true;
		int i = 0;
		for (Node[] nodes : members) {
			i++;
			if (first) {
				first = false;
				continue;
			}

			String friendlyName = nodes[membermap.get("?MEMBER_CAPTION")]
					.toString();
			if (friendlyName.equals("null") || friendlyName == null)
				friendlyName = generateFriendlyNameFromNS(nodes[membermap
						.get("?MEMBER_UNIQUE_NAME")].toString());
			Map<Integer, String> entry = new HashMap<Integer, String>();
			entry.put(i, nodes[membermap.get("?MEMBER_UNIQUE_NAME")].toString());
			membersEasyMap.put(friendlyName, entry);
			System.out.println(nodes[membermap.get("?MEMBER_UNIQUE_NAME")]);
		}
		return membersEasyMap;
	}

	/**
	 * generates the plan for the selected dimensions and measures
	 * 
	 * @param listOfDimensions
	 * @param listOfMeassures
	 * @return
	 */
	public LogicalOlapQueryPlan getPlanForDimensions(List<String> listOfRows,
			List<String> listOfColumns, List<String> listOfMeassures) {

		if (listOfColumns.size() == 0 || listOfMeassures.size() == 0
				|| listOfRows.size() == 0)
			return null;
		setSelectedRows(listOfRows);
		setSelectedColumns(listOfColumns);
		getSelectedDimensions().clear();
		getSelectedDimensions().addAll(listOfRows);
		getSelectedDimensions().addAll(listOfColumns);
		setSelectedMeasures(listOfMeassures);
		// this is the list of dimensions we don't want to use
		List<Node[]> sliceddimensions = new ArrayList<Node[]>();
		sliceddimensions.add(dimensions.get(0));

		// restart the lists

		newDimensions = new ArrayList<Node[]>();
		newMeasures = new ArrayList<Node[]>();

		// add the meta data
		newDimensions.add(dimensions.get(0));

		List<Integer> addedDims = new ArrayList<Integer>();
		addedDims.add(0);
		for (String dim : getSelectedDimensions()) {
			Map<Integer, String> realDim = dimensionEasyMap.get(dim);
			int id = realDim.keySet().iterator().next();
			newDimensions.add(dimensions.get(id));
			addedDims.add(id);
		}

		Map<String, Integer> dimensionmap = Olap4ldLinkedDataUtil
				.getNodeResultFields(dimensions.get(0));
		// add the remaining dimensions in the same order
		for (int i = 0; i < dimensions.size(); i++) {
			if (addedDims.contains(i))
				continue;
			newDimensions.add(dimensions.get(i));
			Node[] nodes = dimensions.get(i);
			if (nodes[dimensionmap.get("?DIMENSION_UNIQUE_NAME")].toString()
					.equals(Olap4ldLinkedDataUtil.MEASURE_DIMENSION_NAME)) {
				continue;
			}
			sliceddimensions.add(dimensions.get(i));
		}

		// add the meta data
		newMeasures.add(measures.get(0));
		for (String dim : listOfMeassures) {
			Map<Integer, String> realDim = measureEasyMap.get(dim);
			newMeasures.add(measures.get(realDim.keySet().iterator().next()));
		}

		LogicalOlapOp basecube = new BaseCubeOp(cubes, newMeasures,
				newDimensions, hierarchies, levels, members);

		LogicalOlapOp gdpslice = new SliceOp(basecube, sliceddimensions);
		// Header is included
		LogicalOlapQueryPlan myplan = new LogicalOlapQueryPlan(gdpslice);

		return myplan;
	}

	/**
	 * executes the plan
	 * 
	 * @param queryplan
	 * @return
	 */
	public List<Node[]> executeStatement(LogicalOlapQueryPlan queryplan) {
		try {
			// For now, we simply return plan
			System.out.println("--------------");
			System.out.println("Logical plan:" + queryplan.toString());
			// Execute query return representation of physical query plan
			List<Node[]> result = this.lde.executeOlapQuery(queryplan);

			return result;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

	//
	// public static void main(String[] args) {
	// try {
	// OLAPModel model = new OLAPModel();
	// model.loadDataset("http://olap4ld.googlecode.com/git/OLAP4LD-trunk/tests/estatwrap/tsdec420_ds.rdf#ds");
	// } catch (SQLException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }

	public boolean isValidValue(int index, String value) {

		int i = 0;
		for (Entry<String, Map<String, Map<Integer, String>>> entryDim : selectedValuesPerDimension
				.entrySet()) {
			if (i == index) {
				for (Entry<String, Map<Integer, String>> entryVal : entryDim
						.getValue().entrySet()) {
					String fullName = entryVal.getValue().values().iterator()
							.next();
					if (fullName.equals(value))
						return true;
				}
				return false;
			}
			i++;
		}

		return false;
	}

	/**
	 * returns list to string
	 * 
	 * @param list
	 * @return
	 */
	public static String serialieObjectToString(Object list) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream os = null;
		String newString = "";
		try {
			os = new ObjectOutputStream(bos);
			os.writeObject(list);
			newString = bos.toString();
			os.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newString;
	}

	/**
	 * returns deserialized object from string
	 * 
	 * @param str
	 * @return
	 */
	public static Object deserializeObjectFromString(String str) {
		ByteArrayInputStream bis = new ByteArrayInputStream(str.getBytes());
		ObjectInputStream oInputStream = null;
		Object restoredObj = null;
		try {
			oInputStream = new ObjectInputStream(bis);
			restoredObj = oInputStream.readObject();
			oInputStream.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return restoredObj;
	}

	public static String convertListToString(List<String> list) {
		String newStr = "";

		for (String str : list) {
			newStr += "\"" + str + "\"\t";
		}
		return newStr;

	}

	public static List<String> convertStringToList(String str) {
		List<String> newList = new LinkedList<String>();
		str = str.substring(1, str.length() - 2);
		if (!str.contains("\"\t\"")) {
			newList.add(str);
			return newList;
		}
		String[] vals = str.split("\"\t\"");

		for (String s : vals)
			newList.add(s);
		return newList;

	}

	public static String convertMapToString(
			Map<String, Map<String, Map<Integer, String>>> map) {
		String newStr = "{";
		for (Entry<String, Map<String, Map<Integer, String>>> entry : map
				.entrySet()) {
			newStr += entry.getKey() + "={";

			for (Entry<String, Map<Integer, String>> secondEntry : entry
					.getValue().entrySet()) {
				newStr += secondEntry.getKey() + "=["
						+ secondEntry.getValue().keySet().iterator().next()
						+ "\t"
						+ secondEntry.getValue().values().iterator().next()
						+ "],";

			}
			newStr = newStr + "},{";

		}
		return newStr.substring(0, newStr.length());

	}

	public static Map<String, Map<String, Map<Integer, String>>> convertStringToMap(
			String str) {
		Map<String, Map<String, Map<Integer, String>>> result = new LinkedHashMap<String, Map<String, Map<Integer, String>>>();

		String[] baseObjects = str.substring(1).split("\\},\\{");

		for (String baseObj : baseObjects) {
			String[] tmpVals = baseObj.split("=\\{");
			String dimName = tmpVals[0];

			Map<String, Map<Integer, String>> middleMap = new HashMap<String, Map<Integer, String>>();
			String[] insideObjs = tmpVals[1].substring(0,
					tmpVals[1].length() - 2).split("\\],");
			for (String insideObj : insideObjs) {
				String[] tmpObjs = insideObj.split("=\\[");
				String mValue = tmpObjs[0];

				String[] actvalues = tmpObjs[1].split("\t");
				int intV = Integer.parseInt(actvalues[0]);
				String fullName = actvalues[1];

				Map<Integer, String> mostInnerMap = new HashMap<Integer, String>();
				mostInnerMap.put(intV, fullName);
				middleMap.put(mValue, mostInnerMap);
			}
			result.put(dimName, middleMap);
		}
		return result;

	}

	public static void main(String[] args) {
		// Map<String, Map<String, Map<Integer, String>>> result = new
		// LinkedHashMap<String, Map<String, Map<Integer, String>>>();
		// Map<Integer, String> mostInnerMap = new HashMap<Integer, String>();
		// Map<String, Map<Integer, String>> middleMap = new HashMap<String,
		// Map<Integer, String>>();
		// mostInnerMap.put(1, "first");
		// middleMap.put("first", mostInnerMap);
		// mostInnerMap = new HashMap<Integer, String>();
		// mostInnerMap.put(2, "second");
		// middleMap.put("second", mostInnerMap);
		// result.put("first", middleMap);
		// middleMap = new HashMap<String, Map<Integer, String>>();
		// mostInnerMap = new HashMap<Integer, String>();
		// mostInnerMap.put(1, "first");
		// middleMap.put("first", mostInnerMap);
		// mostInnerMap = new HashMap<Integer, String>();
		// mostInnerMap.put(2, "second");
		// middleMap.put("second", mostInnerMap);
		// result.put("second", middleMap);
		// result =
		// result =
		// convertStringToMap("{first={second=[2	second],first=[1	first],},{second={second=[2	second],first=[1	first],},{");
		// System.out.println(convertMapToString(result));

		OLAPModel model = null;
		try {
			model = new OLAPModel();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		model.loadDataset("http://worldbank.270a.info/dataset/world-bank-finances/p65j-3upu");
		for (String str : model.getDimensionEasyMap().keySet())
			System.out.println("Dim: " + str);
		for (String str : model.getMeasureEasyMap().keySet())
			System.out.println("Meas: " + str);

	}
}
