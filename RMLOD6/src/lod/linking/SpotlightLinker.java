package lod.linking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lod.generators.BaseGenerator;
import lod.utils.SpotlightAnalyzer;

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
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;

/**
 * Enables linking by utilizing DBPedia Spotlight search
 * 
 * @author Petar Ristoski
 * @author Evgeny Mitichkin
 * 
 */
public class SpotlightLinker extends Operator {

	private static final String CLASS_NAME = "spotlight_linker";

	public static final String PARAMETER_ATTRIBUTE_TO_MERGE = "Attribute";
	public static final String PARAMETER_SERVICE_URL_COMBO = "Service URL";
	public static final String PARAMETER_SERVICE_URL_CUSTOM = "Custom Service URL";
	public static final String PARAMETER_CONFIDENCE = "Confidence";
	public static final String PARAMETER_SUPPORT = "Support";
	public static final String PARAMETER_CONTEXTUAl_SCOREcontextual_score = "Contextual Score";
	public static final String PARAMETER_DISAMBIGUATOR = "Disambiguator";
	public static final String PARAMETER_SPOTTER = "Spotter";
	public static final String PARAMETER_TYPES = "Types";

	private static final String NEW_ATTRIBUTES = "New Attributes";
	private static final String BYPASSING_ATTRIBUTES = "Bypassing Attributes";

	// add different lanugages
	public static final String[] LANGUAGES = new String[] { "English",
			"German", "Dutch", "French", "Italian", "Russian", "Spanish",
			"Portuguese", "Hungarian", "Turkish", "Custom" };
	private static final Map<Integer, String> LANGUAGE_URL = new HashMap<Integer, String>() {
		{
			put(0, "http://spotlight.sztaki.hu:2222/");
			put(1, "http://de.dbpedia.org/spotlight/");
			put(2, "http://nl.dbpedia.org/spotlight/");
			put(3, "http://spotlight.sztaki.hu:2225/");
			put(4, "http://spotlight.sztaki.hu:2230/");
			put(5, "http://spotlight.sztaki.hu:2227/");
			put(6, "http://spotlight.sztaki.hu:2231/");
			put(7, "http://spotlight.sztaki.hu:2228/");
			put(8, "http://spotlight.sztaki.hu:2229/");
			put(9, "http://spotlight.sztaki.hu:2235/");
		}
	};

	private InputPort mInputPort;
	private OutputPort mOutputPort;
	private OutputPort mOutputPortAttrs;

	public SpotlightLinker(OperatorDescription description) {
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

	@SuppressWarnings("deprecation")
	@Override
	public void doWork() throws OperatorException {

		ExampleSet exampleSet = BaseGenerator.cloneExampleSet(mInputPort
				.getData(ExampleSet.class));

		Attributes attrs = exampleSet.getAttributes();
		String attrToMergeName = getParameterAsString(PARAMETER_ATTRIBUTE_TO_MERGE);

		String serviceUrl = "";
		int selectedLanguage = getParameterAsInt(PARAMETER_SERVICE_URL_COMBO);
		if (selectedLanguage < LANGUAGE_URL.size()) {
			serviceUrl = LANGUAGE_URL.get(selectedLanguage);
		} else {
			serviceUrl = getParameterAsString(PARAMETER_SERVICE_URL_CUSTOM);
		}

		SpotlightAnalyzer analyzer = getSpotlightAnalyzer(serviceUrl);
		if (!attrToMergeName.equals("")) {
			Attribute mAttributeToMerge = attrs.get(attrToMergeName);

			if (mAttributeToMerge == null)
				throw new OperatorException(
						"Problem in Spotlight Based Linker: No attribute named '"
								+ attrToMergeName + "'");

			ArrayList<String> newAttributeNames = new ArrayList<String>(); // Endpoints
			ArrayList<String> bypassingAttributes = new ArrayList<String>();// Combined
																			// name
																			// of
																			// endpoint
																			// and
																			// constant
			Map<String, Integer> addedConceptsNm = new HashMap<String, Integer>();

			for (Example ex : exampleSet) {
				if (analyzer.isRunning) {
					String document = ex.getValueAsString(mAttributeToMerge);
					List<String> dbConcepts = null;
					try {
						dbConcepts = analyzer.getListOfConcepts(document);
					} catch (Exception e) {
						if (e.getClass().equals(OperatorException.class)) {
							throw new UserError(this, 2003, CLASS_NAME,
									e.getMessage());
						}
						throw new UserError(this, 2002, CLASS_NAME,
								e.getMessage());
					}

					if (dbConcepts != null) {

						addedConceptsNm.put(document, dbConcepts.size());

						int i = 1;
						for (String concept : dbConcepts) {
							String attrName = mAttributeToMerge.getName()
									+ "_Concept_" + i;
							if (exampleSet.getAttributes().get(attrName) == null) {
								// add new attribute
								Attribute mAttributeCityCombined = null;

								mAttributeCityCombined = AttributeFactory
										.createAttribute(attrName,
												Ontology.STRING);

								mAttributeCityCombined
										.setTableIndex(mAttributeToMerge
												.getTableIndex() + 1);
								exampleSet.getExampleTable().addAttribute(
										mAttributeCityCombined);
								exampleSet.getAttributes().addRegular(
										mAttributeCityCombined);

								newAttributeNames.add(attrName);
								bypassingAttributes.add(attrName);
							}
							ex.setValue(exampleSet.getAttributes()
									.get(attrName), concept);

							i++;
						}
					} else
						addedConceptsNm.put(document, 0);
				}
			}

			if (analyzer.isRunning) {
				// set missing values to the attributes
				for (Example ex : exampleSet) {
					int nmAttributes = exampleSet.getAttributes().size()
							- bypassingAttributes.size()
							+ addedConceptsNm.get(ex
									.getValueAsString(mAttributeToMerge));
					int i = 0;
					for (Attribute att : exampleSet.getAttributes()) {
						i++;
						if (i <= nmAttributes)
							continue;
						ex.setValue(att, Double.NaN);
					}
				}

				// construct attribute set
				Attribute[] attributes = new Attribute[2];
				attributes[0] = AttributeFactory.createAttribute(
						NEW_ATTRIBUTES, Ontology.STRING);
				attributes[1] = AttributeFactory.createAttribute(
						BYPASSING_ATTRIBUTES, Ontology.STRING);

				MemoryExampleTable table = new MemoryExampleTable(attributes);

				DataRowFactory ROW_FACTORY = new DataRowFactory(
						DataRowFactory.TYPE_SPARSE_MAP);

				for (int i = 0; i < bypassingAttributes.size(); i++) {
					DataRow row = ROW_FACTORY.create(
							new String[] { newAttributeNames.get(i),
									bypassingAttributes.get(i) }, attributes);
					table.addDataRow(row);
				}

				ExampleSet ioListResult = table.createExampleSet();

				mOutputPort.deliver(exampleSet);
				mOutputPortAttrs.deliver(ioListResult);
			}
		} else {
			mOutputPort.deliver(exampleSet);
			mOutputPortAttrs.deliver(null);
		}
		super.doWork();
	}

	@Override
	public List<ParameterType> getParameterTypes() {

		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeString(
				PARAMETER_ATTRIBUTE_TO_MERGE,
				"This parameter defines the attribute name whose contents will be used for creating links.",
				"City", false));
		ParameterType type = new ParameterTypeCategory(
				PARAMETER_SERVICE_URL_COMBO,
				"The URL of the Spotlight Service. If you select \"Custom\", new tex box will appear, where you can enter custom service URL.",
				LANGUAGES, 0, false);
		types.add(type);

		type = new ParameterTypeString(PARAMETER_SERVICE_URL_CUSTOM,
				"Custom URL of the Spotlight Service",
				"http://spotlight.sztaki.hu:2222/", false);
		type.registerDependencyCondition(new EqualTypeCondition(this,
				PARAMETER_SERVICE_URL_COMBO, LANGUAGES, true, 10));
		types.add(type);
		/*
		 * types.add(new ParameterTypeString(PARAMETER_SERVICE_URL_CUSTOM,
		 * "Custom URL of the Spotlight Service",
		 * "http://spotlight.sztaki.hu:2222/", false));
		 */

		types.add(new ParameterTypeDouble(PARAMETER_CONFIDENCE,
				"Minimum confidence", 0, 1, 0.2, true));

		types.add(new ParameterTypeInt(PARAMETER_SUPPORT, "Minimum support", 0,
				100, 20, true));

		types.add(new ParameterTypeDouble(
				PARAMETER_CONTEXTUAl_SCOREcontextual_score, "Contextual score",
				0, 1, 0.2, true));
		types.add(new ParameterTypeString(
				PARAMETER_TYPES,
				"When types are provided, only entities of those types will be extracted",
				"", false));

		/**
		 * for the moment, those are not supported by the public Spotlight
		 * service
		 * 
		 * types.add(new ParameterTypeString(PARAMETER_DISAMBIGUATOR,
		 * "The disambiguator ", "Default", false));
		 * 
		 * types.add(new ParameterTypeString(PARAMETER_SPOTTER, "The spotter",
		 * "Default", false));
		 */
		return types;
	}

	public SpotlightAnalyzer getSpotlightAnalyzer(String serviceUrl)
			throws UndefinedParameterError {
		SpotlightAnalyzer analyzer = new SpotlightAnalyzer(
				serviceUrl,
				getParameterAsDouble(PARAMETER_CONFIDENCE),
				getParameterAsInt(PARAMETER_SUPPORT),
				getParameterAsDouble(PARAMETER_CONTEXTUAl_SCOREcontextual_score),
				"Default", "Default", getParameter(PARAMETER_TYPES));
		// getParameterAsString(PARAMETER_DISAMBIGUATOR),
		// getParameterAsString(PARAMETER_SPOTTER));
		return analyzer;
	}
}
