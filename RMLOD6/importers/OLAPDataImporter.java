package lod.importers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lod.gui.tools.wizzards.olap4ld.Olap4ldSettingsWizardCreator;
import lod.olap4ld.CubeSPARQLExplorer;
import lod.olap4ld.OLAPModel;
import lod.utils.AttributeTypeGuesser;
import lod.utils.AttributeTypeGuesser.attributeType;
import lod.utils.MapUtils;

import org.apache.commons.collections15.map.HashedMap;
import org.olap4j.driver.olap4ld.helper.Olap4ldLinkedDataUtil;
import org.semanticweb.yars.nx.Node;

import com.hp.hpl.jena.query.QuerySolution;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeConfiguration;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeText;
import com.rapidminer.parameter.TextType;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;

/**
 * 
 * @author Petar Ristoski
 * 
 */
public class OLAPDataImporter extends Operator {
	private static final String CLASS_NAME = "olap_data_importer";

	public static final String PARAMETER_SELECTED_URI = "Dataset URL";

	public static final String PARAMETER_SELECTED_ROWS = "Selected Row Dimensions";

	public static final String PARAMETER_SELECTED_COLS = "Selected Columns Dimensions";

	public static final String PARAMETER_SELECTED_MEASURES = "Selected Measures";

	public static final String PARAMETER_SELECTED_VALUES = "Selected Values";

	public static final String PARAMETER_IS_SPARQL = "IS Sparql";

	public static final String PARAMETER_ENDPOINT = "endpoint";

	public static final String PARAMETER_SELECTED_ROWS_VALUES = "Selected Row Dimensions values";

	public static final String PARAMETER_SELECTED_COLS_VALUES = "Selected Columns Dimensions values";

	private OLAPModel olapModel;

	private CubeSPARQLExplorer cubeExplorer;

	private boolean isSPARQLexplorer = false;

	private OutputPort mOutputPort;

	// statics
	public static final String DIMENSION_ID = "dimension_id";

	public void setOlapModel(OLAPModel olapModel) {
		this.olapModel = olapModel;
	}

	public void setCubeExplorer(CubeSPARQLExplorer cubeExplorer) {
		this.cubeExplorer = cubeExplorer;
	}

	public void setSPARQLexplorer(boolean isSPARQLexplorer) {
		this.isSPARQLexplorer = isSPARQLexplorer;
	}

	public OLAPDataImporter(OperatorDescription description) {
		super(description);

		mOutputPort = getOutputPorts().createPort("Example Set");
		getTransformer().addGenerationRule(mOutputPort, ExampleSet.class);

	}

	@Override
	public void doWork() throws OperatorException {
		MemoryExampleTable table = null;
		if (olapModel == null && cubeExplorer == null) {

			initTheModelFromParams();
		}
		if (isSPARQLexplorer) {
			table = cubeExplorer.getMemoryTableForSPARQL();
		} else {
			// throw new UserError(this, 2004, CLASS_NAME,
			// "The OLAP model is not configured!");
			table = getMemoryTableForOLAP();
		}
		mOutputPort.deliver(table.createExampleSet());
	}

	/**
	 * initializes the model from params (when the model is not loaded from the
	 * wizard)
	 * 
	 * @throws UndefinedParameterError
	 */
	private void initTheModelFromParams() throws UndefinedParameterError {
		isSPARQLexplorer = getParameterAsBoolean(PARAMETER_IS_SPARQL);
		if (isSPARQLexplorer) {
			getSPARQLCubeLoaderFromParams();
		} else {
			getOlapFromParams();
		}
	}

	/**
	 * returns the cube loader from parameters
	 * 
	 * @throws UndefinedParameterError
	 */
	private void getSPARQLCubeLoaderFromParams() throws UndefinedParameterError {
		cubeExplorer = new CubeSPARQLExplorer(
				getParameterAsString(PARAMETER_SELECTED_URI),
				getParameterAsString(PARAMETER_ENDPOINT));

		cubeExplorer
				.setSelectedColumnsByUser(OLAPModel
						.convertStringToList(getParameterAsString(PARAMETER_SELECTED_COLS)));
		cubeExplorer
				.setSelectedRowsByUser(OLAPModel
						.convertStringToList(getParameterAsString(PARAMETER_SELECTED_ROWS)));
		cubeExplorer
				.setSelectedMeasures(MapUtils
						.stringToMap(getParameterAsString(PARAMETER_SELECTED_MEASURES)));

		cubeExplorer
				.setSelectedColumns(MapUtils
						.stringToDoubleMap(getParameterAsString(PARAMETER_SELECTED_COLS_VALUES)));
		cubeExplorer
				.setSelectedRows(MapUtils
						.stringToDoubleMap(getParameterAsString(PARAMETER_SELECTED_ROWS_VALUES)));
	}

	/**
	 * retrieves only the olap model
	 * 
	 * @throws UndefinedParameterError
	 */
	private void getOlapFromParams() throws UndefinedParameterError {
		try {
			olapModel = new OLAPModel();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		olapModel.loadDataset(getParameterAsString(PARAMETER_SELECTED_URI));
		List<String> nRows = OLAPModel
				.convertStringToList(getParameterAsString(PARAMETER_SELECTED_ROWS));
		List<String> nColumns = OLAPModel
				.convertStringToList(getParameterAsString(PARAMETER_SELECTED_COLS));
		List<String> newMeasures = OLAPModel
				.convertStringToList(getParameterAsString(PARAMETER_SELECTED_MEASURES));

		olapModel.setCurrentPlan(olapModel.getPlanForDimensions(nRows,
				nColumns, newMeasures));

		Map<String, Map<String, Map<Integer, String>>> result = OLAPModel
				.convertStringToMap(getParameterAsString(PARAMETER_SELECTED_VALUES));
		olapModel.setSelectedValuesPerDimension(result);
	}

	@Override
	public List<ParameterType> getParameterTypes() {

		List<ParameterType> types = super.getParameterTypes();
		// ParameterType type = new ParameterTypeConfiguration(
		// OLAPLoaderWizard.class, this);
		// type.setExpert(false);
		// types.add(type);

		ParameterType type = new ParameterTypeConfiguration(
				Olap4ldSettingsWizardCreator.class, this);
		type.setExpert(false);
		types.add(type);

		ParameterType typeU = new ParameterTypeText(PARAMETER_SELECTED_URI,
				"The uri of the dataset", TextType.PLAIN, false);
		typeU.setHidden(true);
		types.add(typeU);

		ParameterType typeR = new ParameterTypeText(PARAMETER_SELECTED_ROWS,
				"The selected row dimnesions", TextType.PLAIN, false);
		typeR.setHidden(true);
		types.add(typeR);

		ParameterType typeC = new ParameterTypeText(PARAMETER_SELECTED_COLS,
				"The selected column dimnesions", TextType.PLAIN, false);
		typeC.setHidden(true);
		types.add(typeC);

		typeC = new ParameterTypeText(PARAMETER_SELECTED_MEASURES,
				"The selected column dimnesions", TextType.PLAIN, false);
		typeC.setHidden(true);
		types.add(typeC);

		ParameterType typeV = new ParameterTypeText(PARAMETER_SELECTED_VALUES,
				"The selected values per dimension", TextType.PLAIN, true);
		typeV.setHidden(true);
		types.add(typeV);

		typeC = new ParameterTypeBoolean(PARAMETER_IS_SPARQL,
				"is SPARQL explorer", false, false);
		typeC.setHidden(true);
		types.add(typeC);

		typeV = new ParameterTypeText(PARAMETER_ENDPOINT, "Endpoint",
				TextType.PLAIN, true);
		typeV.setHidden(true);
		types.add(typeV);

		typeV = new ParameterTypeText(PARAMETER_SELECTED_ROWS_VALUES,
				"rows values", TextType.PLAIN, true);
		typeV.setHidden(true);
		types.add(typeV);

		typeV = new ParameterTypeText(PARAMETER_SELECTED_COLS_VALUES,
				"cols values", TextType.PLAIN, true);
		typeV.setHidden(true);
		types.add(typeV);

		return types;
	}

	/**
	 * Generates complete MemoryTable for the given query
	 * 
	 * @param runner
	 * @param query
	 * @return
	 * @throws OperatorException
	 */
	private MemoryExampleTable getMemoryTableForOLAP() throws OperatorException {
		// execute statement
		List<Node[]> results = olapModel.executeStatement(olapModel
				.getCurrentPlan());
		// structure the results by example
		List<String> uniqueAttributes = new ArrayList<String>();
		Map<String, Map<String, String>> resultsByExample = strucutreTheResults(
				results, uniqueAttributes);

		// add the attributes
		AttributeTypeGuesser attributeTypeGuesser = new AttributeTypeGuesser();
		Attribute[] attributes = new Attribute[uniqueAttributes.size() + 1];
		// add the id attribute
		attributes[0] = AttributeFactory.createAttribute(DIMENSION_ID,
				Ontology.STRING);
		for (int i = 0; i < uniqueAttributes.size(); i++) {
			attributes[i + 1] = AttributeFactory.createAttribute(
					uniqueAttributes.get(i), Ontology.STRING);
		}
		MemoryExampleTable table = null;
		table = new MemoryExampleTable(attributes);

		// populate the memorytable
		for (Entry<String, Map<String, String>> entry : resultsByExample
				.entrySet()) {
			DoubleArrayDataRow rowFirst = getDataRow(attributes, entry,
					attributeTypeGuesser);
			table.addDataRow(rowFirst);
		}
		return table;
	}

	private Map<String, Map<String, String>> strucutreTheResults(
			List<Node[]> results, List<String> uniqueAttributes) {

		Map<String, Map<String, String>> resultingMap = new HashedMap<String, Map<String, String>>();

		Map<String, Integer> measuremap = Olap4ldLinkedDataUtil
				.getNodeResultFields(olapModel.getMeasures().get(0));
		// skip the first result, because it's just headers
		boolean firstResult = true;
		for (Node[] nodes : results) {
			if (firstResult) {
				firstResult = false;
				continue;
			}
			boolean first = true;

			int index = -1;
			String exampleName = "";
			int nmDimensions = olapModel.getSelectedDimensions().size()
					- olapModel.getSelectedRows().size();
			int nmRows = olapModel.getSelectedRows().size();
			int nmMeassures = 0;
			String newAttr = "";
			try {
				for (Node node : nodes) {
					index++;
					// check if the value is inside the selected values
					if (nmDimensions > 0
							&& !olapModel.isValidValue(index, node.toString())) {
						break;
					}
					// get the sample
					if (nmRows > 0) {
						exampleName += node.toString() + "_";
						first = false;
						nmRows--;
						continue;
					} else {
						if (exampleName.endsWith("_"))
							exampleName = exampleName.substring(0,
									exampleName.length() - 1);
					}
					// add the attributes
					if (nmDimensions > 0) {
						newAttr += node.toString() + "_";
						nmDimensions--;
						continue;
					}
					// add the measure
					String attrToAdd = newAttr
							+ olapModel.getSelectedMeasures().get(nmMeassures);
					// add the attr
					if (!uniqueAttributes.contains(attrToAdd)) {
						uniqueAttributes.add(attrToAdd);
					}
					Map<String, String> insideValues = new HashedMap<String, String>();

					if (resultingMap.containsKey(exampleName))
						insideValues = resultingMap.get(exampleName);

					insideValues.put(attrToAdd, node.toString());
					resultingMap.put(exampleName, insideValues);
					nmMeassures++;

				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		return resultingMap;
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
			Entry<String, Map<String, String>> entry,
			AttributeTypeGuesser attributeTypeGuesser) {
		DoubleArrayDataRow row = new DoubleArrayDataRow(
				new double[attributes.length]);
		for (Attribute attr : attributes) {
			double value = Double.NaN;

			// set the id
			if (attr.getName().equals(DIMENSION_ID)) {
				value = AttributeTypeGuesser.getValueForAttribute(attr,
						entry.getKey());
			}
			// set standard value
			else if (entry.getValue().containsKey(attr.getName())) {
				value = AttributeTypeGuesser.getValueForAttribute(attr, entry
						.getValue().get(attr.getName()));
			} else {
				// set missing value
				value = AttributeTypeGuesser.getValueForAttribute(attr, null);
			}
			row.set(attr, value);
		}
		return row;
	}

	/**
	 * generates new data row for the given values
	 * 
	 * @param variables
	 * @param attributes
	 * @param solution
	 * @param attributeTypeGuesser
	 * @return
	 */
	private DoubleArrayDataRow getDataRow(List<String> variables,
			Attribute[] attributes, QuerySolution solution,
			AttributeTypeGuesser attributeTypeGuesser) {
		DoubleArrayDataRow row = new DoubleArrayDataRow(
				new double[variables.size()]);
		int attrNm = 0;
		for (String var : variables) {
			double value = Double.NaN;
			if (solution.get(var) != null) {

				if (solution.get(var).isLiteral()) {
					String strValue = "";
					// if it is literal and it is not a string, split the value
					if (attributeTypeGuesser.getLiteralType(solution
							.getLiteral(var)) != Ontology.NUMERICAL) {
						strValue = solution.getLiteral(var).toString()
								.split("")[0];
					} else {
						strValue = solution.getLiteral(var).getString();
					}
					value = AttributeTypeGuesser.getValueForAttribute(
							attributes[attrNm], strValue);
				} else { // if it is not a literal then use it as it is
					value = AttributeTypeGuesser.getValueForAttribute(
							attributes[attrNm], solution.get(var).toString());
				}
			}

			row.set(attributes[attrNm], value);
			attrNm++;
		}
		return row;
	}
}
