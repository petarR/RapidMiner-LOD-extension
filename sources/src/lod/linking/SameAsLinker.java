package lod.linking;

import java.util.ArrayList;
import java.util.List;

import lod.generators.BaseGenerator;
import lod.gui.tools.dialogs.SPARQLConfigurator;
import lod.sparql.SPARQLEndpointQueryRunner;
import lod.sparql.SPARQLQueryRunner.QuerryRunnerType;

import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.config.ConfigurationException;
import com.rapidminer.tools.config.ParameterTypeConfigurable;

/**
 * Enbales SameAs linking
 * 
 * @author Evgeny Mitichkin
 * @author Petar Ristoski
 * 
 */
public class SameAsLinker extends Operator {

	private InputPort mInputPort;
	private OutputPort mOutputPort;
	private OutputPort mOutputPortAttrs;

	public static final String PARAMETER_SPARQL_MANAGER = "SPARQL connection";
	public static final String PARAMETER_ATRIBUTE_FOR_SEARCH = "Attribute to search for";
	public static final String PARAMETER_NEW_ATTRIBUTE_NAME = "New attribute name";
	public static final String PARAMETER_ENDPOINT_SEARCH_PATTERN = "Endpoint search pattern";
	public static final String PARAMETER_RESOLVE_BY_URI = "Use URI data model";

	private static final String NEW_ATTRIBUTES = "New Attributes";
	private static final String BYPASSING_ATTRIBUTES = "Bypassing Attributes";
	private static final String VARNAME = "x";

	private SPARQLEndpointQueryRunner queryRunner;

	public SameAsLinker(OperatorDescription description) {
		super(description);

		mInputPort = getInputPorts()
				.createPort("Example Set", ExampleSet.class);
		mInputPort.addPrecondition(new SimplePrecondition(mInputPort,
				new MetaData(ExampleSet.class)));

		mOutputPort = getOutputPorts().createPort("Appended Set");
		mOutputPortAttrs = getOutputPorts().createPort("Attributes Appended");

		getTransformer().addPassThroughRule(mInputPort, mOutputPort);
		getTransformer().addGenerationRule(mOutputPortAttrs, ExampleSet.class);
	}

	@Override
	public void doWork() throws OperatorException {

		ExampleSet exampleSet = BaseGenerator.cloneExampleSet(mInputPort
				.getData(ExampleSet.class));
		Attributes attrs = exampleSet.getAttributes();

		try {
			queryRunner = SPARQLEndpointQueryRunner.initRunner(this,
					queryRunner);
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new OperatorException(
					"Problem in SameAs Linker: cannot read the SPARQL endpoint connection");
		}

		String mAttrToAddName = getParameterAsString(PARAMETER_NEW_ATTRIBUTE_NAME);
		String mAttrSearch = getParameterAsString(PARAMETER_ATRIBUTE_FOR_SEARCH);
		String mSearchPattern = getParameterAsString(PARAMETER_ENDPOINT_SEARCH_PATTERN);

		ArrayList<String> newAttributeNames = new ArrayList<String>();// Endpoits
		ArrayList<String> bypassingAttributes = new ArrayList<String>();// Combined
																		// name
																		// of
																		// endpoint
																		// and
																		// constant

		Attribute attr = attrs.get(mAttrSearch);

		if (attr != null) {

			if (!mAttrToAddName.equals("")) {
				Attribute mAttributeCombined = null;

				mAttributeCombined = AttributeFactory.createAttribute(
						mAttrToAddName, Ontology.STRING);
				mAttributeCombined.setTableIndex(exampleSet.getAttributes()
						.allSize());

				// add new attribute
				exampleSet.getExampleTable().addAttribute(mAttributeCombined);
				exampleSet.getAttributes().addRegular(mAttributeCombined);

				// Adding the attribute to transferring arrays
				newAttributeNames.add(mAttributeCombined.getName());
				bypassingAttributes.add(mAttributeCombined.getName());

				for (Example ex : exampleSet) {
					if (queryRunner.mUIThreadRunning) {

						String uri = ex.getValueAsString(attr);
						if (queryRunner.getRunnerType() == QuerryRunnerType.URLBASED) {
							queryRunner.updateModel(uri);
						}
						ResultSet linkingResult = queryRunner
								.runSelectQueryInterruptable(QueryFactory
										.create(coustructSPARQLQuery(ex
												.getValueAsString(attr)))
										.toString());
						processResults(linkingResult, mSearchPattern, ex,
								mAttributeCombined);
					} else {
						break;
					}
				}

			} else {
				throw new OperatorException(
						"Problem in SameAsLinker: the name of new attribute is not defined");
			}

			// construct attribute set for metadata transfer
			Attribute[] attributes = new Attribute[2];
			attributes[0] = AttributeFactory.createAttribute(NEW_ATTRIBUTES,
					Ontology.STRING);
			attributes[1] = AttributeFactory.createAttribute(
					BYPASSING_ATTRIBUTES, Ontology.STRING);

			MemoryExampleTable table = new MemoryExampleTable(attributes);

			@SuppressWarnings("deprecation")
			DataRowFactory ROW_FACTORY = new DataRowFactory(
					DataRowFactory.TYPE_SPARSE_MAP);

			for (int i = 0; i < bypassingAttributes.size(); i++) {
				DataRow row = ROW_FACTORY.create(new String[] {
						newAttributeNames.get(i), bypassingAttributes.get(i) },
						attributes);
				table.addDataRow(row);
			}

			ExampleSet ioListResult = table.createExampleSet();

			mOutputPort.deliver(exampleSet);
			mOutputPortAttrs.deliver(ioListResult);
		} else {
			throw new OperatorException(
					"Problem in SameAsLinker: No attributes with the name '"
							+ getParameterAsString(PARAMETER_ATRIBUTE_FOR_SEARCH)
							+ "' found.");
		}

		super.doWork();
	}

	private void processResults(ResultSet RS, String mSearchPattern,
			Example ex, Attribute mAttributeCombined) {
		if (queryRunner.mUIThreadRunning) {
			String res = null;
			String value = "";
			while (RS.hasNext()) {
				QuerySolution sol = RS.next();
				String oneUrl = sol.get(VARNAME).toString();

				// filtering here
				// FIRSTLY find the exact match and stop
				if (oneUrl.equals(mSearchPattern)) {
					res = oneUrl;
					break;
				} else {
					// IF NOT, check if the substring matches the string
					if (oneUrl.contains(mSearchPattern)) {
						res = oneUrl;
						break;
					}
				}
			}
			if (res != null) {
				if (!res.equals("")) {
					value = res;
				}
			} else {
				value = null;
			}
			ex.setValue(mAttributeCombined, value);
		}
	}

	private String coustructSPARQLQuery(String conceptToLookup) {
		String result = "Prefix owl: <http://www.w3.org/2002/07/owl#> "
				+ "SELECT ?" + VARNAME + " " + "WHERE {{?" + VARNAME
				+ " owl:sameAs <" + conceptToLookup + ">} " + "UNION {<"
				+ conceptToLookup + "> owl:sameAs ?" + VARNAME + "}}";
		return result;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeBoolean(PARAMETER_RESOLVE_BY_URI,
				"If cheked, the operator will try to dereference the URIs",
				false, false));
		ParameterType type = new ParameterTypeConfigurable(
				PARAMETER_SPARQL_MANAGER, "Choose SPARQL endpoint connection",
				SPARQLConfigurator.I18N_BASE_KEY);

		type.registerDependencyCondition(new BooleanParameterCondition(this,
				PARAMETER_RESOLVE_BY_URI, true, false));
		types.add(type);

		types.add(new ParameterTypeString(PARAMETER_ATRIBUTE_FOR_SEARCH,
				"Name of the attribute to search extensions for", false, false));

		types.add(new ParameterTypeString(PARAMETER_NEW_ATTRIBUTE_NAME,
				"This parameter defines the name of the new attribute",
				"Added_Resource", false));

		types.add(new ParameterTypeString(
				PARAMETER_ENDPOINT_SEARCH_PATTERN,
				"This parameter defines a pattern that is used as a search query to the linked data endpoint",
				"http://rdf.freebase.com/"));

		return types;
	}

	@Override
	public void processFinished() throws OperatorException {
		if (queryRunner != null) {
			queryRunner.mUIThreadRunning = false;
			queryRunner.finalizeAsyncThread();
		}
		super.processFinished();
	}
}
