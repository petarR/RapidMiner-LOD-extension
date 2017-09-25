package lod.linking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import lod.generators.BaseGenerator;
import lod.gui.tools.dialogs.SPARQLConfigurator;
import lod.sparql.SPARQLEndpointQueryRunner;
import lod.utils.NGram;
import lod.utils.ValueComparator;

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
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.config.ParameterTypeConfigurable;
import com.wcohen.ss.Jaccard;
import com.wcohen.ss.Levenstein;
import com.wcohen.ss.tokens.NGramTokenizer;
import com.wcohen.ss.tokens.SimpleTokenizer;

/**
 * 
 * @author Petar Ristoski
 * 
 */
public class SPARQLbasedLinker extends Operator {

	private static final String CLASS_NAME = "sparql_based_linker";

	public static final String PARAMETER_ATTRIBUTE_TO_MERGE = "Attribute to merge";

	public static final String PARAMETER_USE_NGRAMS = "Search by N-Grams";

	public static final String PARAMETER_SPARQL_MANAGER = "SPARQL connection";

	public static final String PARAMETER_LANGUAGE_TAG = "Language tag for labels";

	private static final String NEW_ATTRIBUTES = "New Attributes";
	private static final String BYPASSING_ATTRIBUTES = "Bypassing Attributes";

	public static final String PARAMETER_DETECT_CLASS = "Detect column class type";

	private InputPort mInputPort;
	private OutputPort mOutputPort;
	private OutputPort mOutputPortAttrs;

	private SPARQLEndpointQueryRunner queryRunner;

	public SPARQLbasedLinker(OperatorDescription description) {
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
		try {
			queryRunner = SPARQLEndpointQueryRunner.initRunner(this,
					queryRunner);
		} catch (Exception e) {
			e.printStackTrace();
			throw new UserError(this, 2001, CLASS_NAME, e.getMessage());
		}
		ExampleSet exampleSet = BaseGenerator.cloneExampleSet(mInputPort
				.getData(ExampleSet.class));

		Attributes attrs = exampleSet.getAttributes();
		String attrToMergeName = getParameterAsString(PARAMETER_ATTRIBUTE_TO_MERGE);
		boolean includeNgrams = getParameterAsBoolean(PARAMETER_USE_NGRAMS);
		boolean searchForClass = getParameterAsBoolean(PARAMETER_DETECT_CLASS);

		if (!attrToMergeName.equals("")) {
			Attribute mAttributeToMerge = attrs.get(attrToMergeName);

			if (mAttributeToMerge == null)
				throw new OperatorException(
						"Problem in SPARQL Based Linker: No attribute named '"
								+ attrToMergeName + "'");

			ArrayList<String> newAttributeNames = new ArrayList<String>();// Endpoits
			ArrayList<String> bypassingAttributes = new ArrayList<String>();// Combined
																			// name
																			// of
																			// endpoint
																			// and
																			// constant

			String attributeDescription = queryRunner.getAlias();
			Attribute mAttributeCityCombined = null;
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(
					mAttributeToMerge.getValueType(), Ontology.NOMINAL)) {
				mAttributeCityCombined = AttributeFactory.createAttribute(
						mAttributeToMerge.getName() + "_link_to_"
								+ attributeDescription,
						mAttributeToMerge.getValueType());
			} else {
				mAttributeCityCombined = AttributeFactory.createAttribute(
						mAttributeToMerge.getName() + "_link_to_"
								+ attributeDescription, Ontology.STRING);
			}
			mAttributeCityCombined.setTableIndex(mAttributeToMerge
					.getTableIndex() + 1);

			newAttributeNames.add(attributeDescription);
			bypassingAttributes.add(mAttributeToMerge.getName() + "_link_to_"
					+ attributeDescription);

			// add new attribute
			exampleSet.getExampleTable().addAttribute(mAttributeCityCombined);
			exampleSet.getAttributes().addRegular(mAttributeCityCombined);

			// start search for class if needed
			String clazz = "";
			if (searchForClass) {
				// get the classes first
				// holds all retrieved classes
				Map<String, Integer> retrievedClasses = new HashMap<String, Integer>();
				// holds the classes for the best matches
				Map<String, Integer> bestMatchesClasses = new HashMap<String, Integer>();

				for (Example ex : exampleSet) {

					String city = ex.getValueAsString(mAttributeToMerge);

					getClassesForEntity(city, includeNgrams, 3,
							retrievedClasses, bestMatchesClasses);
				}
				// decide the most used class

				int maxOccuraces = 0;
				boolean isValidClass = true;
				for (Entry<String, Integer> entry : retrievedClasses.entrySet()) {
					if (entry.getValue() > maxOccuraces) {
						isValidClass = true;
						clazz = entry.getKey();
						maxOccuraces = entry.getValue();

					} else if (entry.getValue() == maxOccuraces)
						isValidClass = false;
				}
				if (!isValidClass)
					clazz = "";
				// if no class was defined, take the class from the best matches
				if (clazz.equals("")) {
					maxOccuraces = 0;
					isValidClass = true;
					for (Entry<String, Integer> entry : bestMatchesClasses
							.entrySet()) {
						if (entry.getValue() > maxOccuraces) {
							isValidClass = true;
							clazz = entry.getKey();
							maxOccuraces = entry.getValue();

						} else if (entry.getValue() == maxOccuraces)
							isValidClass = false;
					}
					if (!isValidClass)
						clazz = "";
				}
			}

			for (Example ex : exampleSet) {

				String city = ex.getValueAsString(mAttributeToMerge);

				String newValue = getLinkedEntityWithSPARQL(city,
						includeNgrams, clazz);

				ex.setValue(mAttributeCityCombined, newValue);
			}

			// construct attribute set
			Attribute[] attributes = new Attribute[2];
			attributes[0] = AttributeFactory.createAttribute(NEW_ATTRIBUTES,
					Ontology.STRING);
			attributes[1] = AttributeFactory.createAttribute(
					BYPASSING_ATTRIBUTES, Ontology.STRING);

			MemoryExampleTable table = new MemoryExampleTable(attributes);

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
			mOutputPort.deliver(exampleSet);
			mOutputPortAttrs.deliver(null);
		}
		super.doWork();
	}

	@Override
	public List<ParameterType> getParameterTypes() {

		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeConfigurable(PARAMETER_SPARQL_MANAGER,
				"Choose SPARQL endpoint connection",
				SPARQLConfigurator.I18N_BASE_KEY));

		types.add(new ParameterTypeString(
				PARAMETER_ATTRIBUTE_TO_MERGE,
				"This parameter defines the attribute name whose contents will be used for creating links.",
				"City", false));

		types.add(new ParameterTypeBoolean(
				PARAMETER_USE_NGRAMS,
				"Use this to also include n-grams in the search. If not set, the linker will search only for the whole string, such as 'United States of America', whereas n-gram search would also search for 'United States' etc.",
				false, false));
		types.add(new ParameterTypeBoolean(
				PARAMETER_DETECT_CLASS,
				"Use this to assign only one class type to all instances from the column",
				false, false));

		types.add(new ParameterTypeString(
				PARAMETER_LANGUAGE_TAG,
				"This parameter restricts the search to labels with the given language tag, e.g., \"en\". Leave empty for searching in all languages. If the data source you are using serves literals without language tags, you will also have to leave this empty.",
				"", true));

		return types;
	}

	public void getClassesForEntity(String label, boolean includeNgrams,
			int nmOfclasses, Map<String, Integer> candidateClasses,
			Map<String, Integer> bestClasses) throws OperatorException {
		// holds added classes
		List<String> addedClasses = new ArrayList<String>();
		String linkedEntity = null;
		Levenstein lev = new Levenstein();
		Map<String, Double> candidateConcepts = new HashMap<String, Double>();
		// holds the classes
		Map<String, List<String>> conceptClasses = new HashMap<String, List<String>>();
		List<String> ngrams = new ArrayList<String>();
		if (includeNgrams) {
			ngrams = getAllNgramsInBound(label);
		} else {
			ngrams.add(label);
		}
		for (String gram : ngrams) {

			ResultSet results = queryRunner
					.runSelectQueryInterruptable(getSPARQLQueryForClasses(gram));
			if (!queryRunner.mUIThreadRunning)
				break;
			if (results != null) {
				while (results.hasNext()) {
					QuerySolution solution = results.next();
					String entity = solution.get("s").toString();
					String labelEntity = solution.get("t").toString();
					// get the classes
					String classType = solution.get("type").toString();
					if (classType.startsWith("http://dbpedia.org/ontology/")) {
						List<String> classes = new ArrayList<String>();
						classes.add(classType);
						if (conceptClasses.containsKey(entity)) {
							classes = conceptClasses.get(entity);
							if (!classes.contains(classType))
								classes.add(classType);
						}
						conceptClasses.put(entity, classes);
					}
					candidateConcepts
							.put(entity, lev.score(labelEntity, label));
				}
			}

		}
		ValueComparator bvc = new ValueComparator(candidateConcepts);
		TreeMap<String, Double> sorted_map = new TreeMap<String, Double>(bvc);
		sorted_map.putAll(candidateConcepts);
		boolean isBestClassAdded = false;
		for (Entry<String, Double> entry : sorted_map.entrySet()) {
			if (!isBestClassAdded) {
				List<String> classesOfEntity = conceptClasses.get(entry
						.getKey());
				if (classesOfEntity == null)
					continue;
				isBestClassAdded = true;

				for (String clazz : classesOfEntity) {
					if (bestClasses.containsKey(clazz)) {
						bestClasses.put(clazz, bestClasses.get(clazz) + 1);
					} else {
						bestClasses.put(clazz, 1);
					}

				}
			}
			List<String> classesOfEntity = conceptClasses.get(entry.getKey());

			if (classesOfEntity == null)
				continue;
			for (String clazz : classesOfEntity) {
				if (addedClasses.contains(clazz))
					continue;
				if (candidateClasses.containsKey(clazz)) {
					candidateClasses
							.put(clazz, candidateClasses.get(clazz) + 1);
				} else {
					candidateClasses.put(clazz, 1);
				}
				addedClasses.add(clazz);
			}
			// nmOfclasses--;
			// if (nmOfclasses == 0)
			// break;
		}

	}

	public String getLinkedEntityWithSPARQL(String label,
			boolean includeNgrams, String clazz) throws OperatorException {
		String linkedEntity = null;
		NGramTokenizer tok = new NGramTokenizer(2, 4, true,
				new SimpleTokenizer(true, true));
		Jaccard sim = new Jaccard(tok);

		Map<String, Double> candidateConcepts = new HashMap<String, Double>();

		List<String> ngrams = new ArrayList<String>();
		if (includeNgrams) {
			ngrams = getAllNgramsInBound(label);
		} else {
			ngrams.add(label);
		}
		for (String gram : ngrams) {

			ResultSet results = queryRunner
					.runSelectQueryInterruptable(getSPARQLQuery(gram, clazz));
			if (queryRunner.mUIThreadRunning) {
				if (results != null) {
					while (results.hasNext()) {
						QuerySolution solution = results.next();
						String entity = solution.get("s").toString();
						String labelEntity = solution.getLiteral("t")
								.toString();
						labelEntity = labelEntity.replaceAll("@.*$", "");
						candidateConcepts.put(entity,
								sim.score(labelEntity, label));
					}
				}
			} else {
				break;
			}
		}
		ValueComparator bvc = new ValueComparator(candidateConcepts);
		TreeMap<String, Double> sorted_map = new TreeMap<String, Double>(bvc);
		sorted_map.putAll(candidateConcepts);
		if (sorted_map.size() > 0) {

			linkedEntity = sorted_map.firstKey();
		}
		return linkedEntity;
	}

	/**
	 * creates query that retrieves the type of the concept
	 * 
	 * @param label
	 * @return
	 * @throws UndefinedParameterError
	 */
	private String getSPARQLQueryForClasses(String label)
			throws UndefinedParameterError {
		String languageTag = getParameterAsString(PARAMETER_LANGUAGE_TAG);
		String filterLanguage = languageTag != null && languageTag.length() > 0 ? "FILTER(LANGMATCHES(LANG(?t), \""
				+ languageTag + "\")). "
				: "";

		return "SELECT DISTINCT * WHERE {"
				+ "?s a <http://www.w3.org/2002/07/owl#Thing>."
				+ "?s <http://www.w3.org/2000/01/rdf-schema#label> ?t ."
				+ "?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?type."
				+ " filter not exists {?subtype ^a ?s ; <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?type .}."
				+ " FILTER regex(?t, \"" + label + "\", \"i\") . "
				+ filterLanguage + "}";
	}

	private String getSPARQLQuery(String label, String clazz)
			throws UndefinedParameterError {
		String languageTag = getParameterAsString(PARAMETER_LANGUAGE_TAG);
		String filterLanguage = languageTag != null && languageTag.length() > 0 ? "FILTER(LANGMATCHES(LANG(?t), \""
				+ languageTag + "\")). "
				: "";
		if (clazz.equals("")) {
			return "SELECT DISTINCT * WHERE {"
					+ "?s <http://www.w3.org/2000/01/rdf-schema#label> ?t ."
					+ " FILTER regex(?t, \"" + label + "\", \"i\") . "
					+ filterLanguage + "}";
		}

		return "SELECT DISTINCT * WHERE {"
				+ "?s <http://www.w3.org/2000/01/rdf-schema#label> ?t . ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <"
				+ clazz + ">." + " FILTER regex(?t, \"" + label
				+ "\", \"i\") . " + filterLanguage + "} LIMIT 30";
	}

	public static List<String> getAllNgramsInBound(String sentence) {
		int lowerBound = 1;
		int upperBound = sentence.split(" |,|!|'|\\?|-|_|\\t").length;

		List<String> ngrams = new ArrayList<String>();

		for (int i = lowerBound; i <= upperBound; i++) {
			NGram ng = new NGram(sentence, i);
			ngrams.addAll(ng.list());
		}
		return ngrams;
	}

	@Override
	public void processFinished() throws OperatorException {
		System.out.println("ProcessFinished pressed...");
		if (queryRunner != null) {
			queryRunner.mUIThreadRunning = false;
			queryRunner.finalizeAsyncThread();
		}
		super.processFinished();
	}
}
