package lod.generators;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import lod.dataclasses.ValueClassesPair;
import lod.generators.dataclasses.Generators;
import lod.gui.tools.dialogs.SPARQLConfigurator;
import lod.rdf.model.RdfHolder;
import lod.sparql.FileBasedQueryRunner;
import lod.sparql.SPARQLEndpointQueryRunner;
import lod.sparql.URLBasedQueryRunner;
import lod.utils.AttributeTypeGuesser;
import lod.utils.HierarchyPair;
import lod.utils.OntologyHierarchy;

import com.rapidminer.PluginInitLOD6;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.DataRowReader;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.operator.preprocessing.MaterializeDataInMemory;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.config.ParameterTypeConfigurable;

/**
 * A base class used for all feature generators. Contains a set of methods for
 * working with incoming {@link ExampleSet} object, adding the
 * {@link Attributes} to the ExampleSet, setting the values of the attributes.
 * 
 * @author Evgeny Mitichkin
 * @author Petar Ristoski
 * 
 */
public abstract class BaseGenerator extends Operator {

	public static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

	private static final String CLASS_NAME = "base_generator";

	/**
	 * Used as a threshold of correlation between {@link Ontology} classes
	 */
	public static final double CLASS_CORRELATION_THRESHOLD = 0.8;
	/**
	 * Used as a threshold of info gain between {@link Ontology} classes
	 */
	public static final double CLASS_INFO_GAIN_THRESHOLD = 0.1;

	/*
	 * These constants are used as names of operator's parameters
	 */
	public static final String NEW_ATTRIBUTES = "New Attributes";
	public static final String BYPASSING_ATTRIBUTES = "Bypassing Attributes";
	public static final String PARAMETER_SPARQL_MANAGER = "SPARQL connection";
	public static final String PARAMETER_ATTRIBUTE_TO_EXTEND = "Attribute to extend";
	public static final String PARAMETER_ATTRIBUTE_TO_EXTEND_NAME = "Attribute to extend name";
	public static final String PARAMETER_VECTOR_REPRESENTATION = "Vector Creation";
	public static final String PARAMETER_REGEX_FILTERS = "Properties Regex Filters";

	public static final String PARAMETER_RESOLVE_BY_URI = "Use URI data model";

	public static final String PARAMETER_PROPERTIES_DIRECTION = "Properties Direction";
	public static final String PARAMETER_REGEX = "Regex";
	/**
	 * Input port containing {@link ExampleSet} with the data
	 */
	protected InputPort mInputPort;

	/**
	 * Input port containing metadata for the generators, i.e. which attributes
	 * should be used as a target
	 */
	protected InputPort mInputPortAddedAttrs;

	/**
	 * Output port containing appended {@link ExampleSet} with the data
	 */
	protected OutputPort mOutputPort;

	/**
	 * Output port containing {@link OntologyHierarchy}
	 */
	protected OutputPort mOutputPortTypesHierarchy;

	/**
	 * outputs the generated data in RDF
	 */
	protected OutputPort mOutputPortRdfData;

	/**
	 * Query runner that enables synchronous and asynchronous queries to SPARQL
	 * endpoint.
	 */
	protected SPARQLEndpointQueryRunner queryRunner;

	/*
	 * These variables are used to enable base functionality of generators, i.e.
	 * feature extraction, attribute generation, attribute value changing and
	 * etc.
	 */
	protected Map<String, Integer> uniqueAtts = new HashMap<String, Integer>();
	protected ExampleSet exampleSet = null;
	protected Attributes attrs = null;
	protected OntologyHierarchy hierarchy = null;
	protected Map<String, String> addedAttributeOverall = null;
	protected boolean createHierarchy = false;
	protected boolean createRDF = false;
	protected Map<Integer, Map<String, Integer>> countRelations = null;
	protected ArrayList<String> attrNames = null;
	protected ArrayList<String> attrsBypsass = null;

	/**
	 * holds the RDF data from the opertaror (id connected)
	 */
	protected RdfHolder rdfHolder = null;

	/**
	 * both used for generating dynamic attributes names
	 */
	protected String attrPrefix = "";
	protected String attrSeparator = "";

	// used for caching results over multiple operators
	// Map<String, Map<SPARQLEndpointQueryRunner, ResultSet>> cachedResults =
	// new HashMap<String, Map<SPARQLEndpointQueryRunner, ResultSet>>();

	// Used to cache prefixes
	protected HashMap<String, String> cachedPrefixes = new HashMap<String, String>();

	// Caching data models
	List<SPARQLEndpointQueryRunner> cachedDataModels = new ArrayList<SPARQLEndpointQueryRunner>();

	// vector representation options
	public static final String[] VECTOR_CREATOR_NAMES = new String[] {
			"Binary", "Count", "Relative Count", "TF-IDF" };

	// property direction options
	public static final String[] PROPERTY_DIRECTION_NAMES = new String[] {
			"In and Out", "Incoming", "Outgoing" };

	protected List<String> regexes;

	/**
	 * Creates {@link BaseGenerator} object
	 * 
	 * @param description
	 */
	public BaseGenerator(OperatorDescription description) {
		super(description);

		mInputPort = getInputPorts()
				.createPort("Example Set", ExampleSet.class);
		mInputPort.addPrecondition(new SimplePrecondition(mInputPort,
				new MetaData(ExampleSet.class)));

		mInputPortAddedAttrs = getInputPorts()
				.createPort("Attributes Appended");

		mOutputPort = getOutputPorts().createPort("Appended Set");
		getTransformer().addPassThroughRule(mInputPort, mOutputPort);
		mOutputPortTypesHierarchy = getOutputPorts().createPort(
				"Hierarchy pairs");

		getTransformer().addRule(
				new GenerateNewMDRule(mOutputPortTypesHierarchy,
						OntologyHierarchy.class));
		// init the rdf port
		mOutputPortRdfData = getOutputPorts().createPort("RDF Data");

		getTransformer().addRule(
				new GenerateNewMDRule(mOutputPortRdfData, RdfHolder.class));

	}

	/**
	 * Returns a formatted outgoing SPARQL query
	 * 
	 * @param uri
	 * @return {@link String} object.
	 */
	public abstract String getSPARQLQueryOutgoing(String uri);

	/**
	 * Returns a formatted ingoing SPARQL query
	 * 
	 * @param uri
	 * @return {@link String} object.
	 */
	public abstract String getSPARQLQueryIncoming(String uri);

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeBoolean(
				PARAMETER_RESOLVE_BY_URI,
				"If checked, it will try to generate the features directly from the URI",
				false, false));
		ParameterType type = new ParameterTypeConfigurable(
				PARAMETER_SPARQL_MANAGER, "Choose SPARQL endpoint connection",
				SPARQLConfigurator.I18N_BASE_KEY);

		type.registerDependencyCondition(new BooleanParameterCondition(this,
				PARAMETER_RESOLVE_BY_URI, true, false));
		types.add(type);

		types.add(new ParameterTypeEnumeration(PARAMETER_ATTRIBUTE_TO_EXTEND,
				"Attributes that contain LOD links", new ParameterTypeString(
						PARAMETER_ATTRIBUTE_TO_EXTEND_NAME, "Attribute name"),
				false));

		types.add(new ParameterTypeEnumeration(
				PARAMETER_REGEX_FILTERS,
				"Only properties that match the defined regex patterns will be used. (To define excluding regex pattersns, start the regex with \"!\"",
				new ParameterTypeString(PARAMETER_REGEX, "Regex"), false));

		types.add(new ParameterTypeCategory(PARAMETER_VECTOR_REPRESENTATION,
				"Select the schema for creating the word vector",
				VECTOR_CREATOR_NAMES, 0, false));

		return types;
	}

	/**
	 * Adds new attribute to a given {@link ExampleSet}
	 * 
	 * @param attributeName
	 *            {@link String} Name of the attribute that is going to be
	 *            included into the {@link ExampleSet}.
	 * @param ontology
	 *            Type of {@link Ontology}
	 * @param exampleSet
	 *            The given {@link ExampleSet}
	 */
	public static Attribute addAtribute(String attributeName, int ontology,
			ExampleSet exampleSet) {

		Attribute attribute = AttributeFactory.createAttribute(attributeName,
				ontology);
		exampleSet.getAttributes().addRegular(attribute);
		exampleSet.getExampleTable().addAttribute(attribute);

		return attribute;
	}

	/**
	 * A wrapper method for extending the feature set with new features
	 * extracted by the generator.
	 * 
	 * @param uniqueAttributesToAdd
	 *            A set of unique attributes (their names as {@link String})
	 * @param attributeNamePrefix
	 *            Prefix as {@link String} used to construct the full name of
	 *            the attribute(s)
	 * @param entityLinkCorespondence
	 *            An array of {@link ValueClassesPair} objects containing new
	 *            features and their values
	 * @param ontology
	 *            type of {@link Ontology}
	 * @param generator
	 *            Type of the generator ({@link Generators} enum value)
	 * @param nameSeparator
	 *            Separator in the full name of new attribute(s) as
	 *            {@link String}
	 * @throws UndefinedParameterError
	 */
	protected void extendAttributeSet(Set<String> uniqueAttributesToAdd,
			String attributeNamePrefix,
			ArrayList<ValueClassesPair> entityLinkCorespondence, int ontology,
			int generator, String nameSeparator) throws UndefinedParameterError {
		if (generator != Generators.DATA_PROPERTIES) {
			// for (String attrName : uniqueAttributesToAdd) {

			int h = 0;

			
			// Looking for the values for this attribute and setting them
			for (Example ex : exampleSet) {

				// ex.get
				// for every record retrieving the set of values
				ValueClassesPair vpair = entityLinkCorespondence.get(h);
				ArrayList<String> entities = vpair.getClasses();

				for (String attrName : entities) {
					boolean cont = false;
					// if the same attribute was added once don't add it again
					// even
					// if it is coming from different link
					boolean shouldChange = false;
					String existingAttrName = null;
					if (addedAttributeOverall.containsKey(attrName)) {
						existingAttrName = addedAttributeOverall.get(attrName);
						shouldChange = true;
					} else {
						addedAttributeOverall.put(attrName, attributeNamePrefix
								+ nameSeparator + attrName);
						addAtribute(attributeNamePrefix + nameSeparator
								+ attrName, ontology, exampleSet);
					}

					int contains = 0;
					switch (generator) {
					case Generators.SIMPLE_TYPE:
						if (entities.contains(attrName))
							contains = 1;
						if (shouldChange) {
							// change it only if it is positive
							// TODO check entities.contains(attrName)
							if (contains == 1) {
								setValueToExampleDbl(ex, attributeNamePrefix,
										nameSeparator, attrName, contains,
										existingAttrName);
							} else {
								h++;
								cont = true;
							}
						} else {
							setValueToExampleInt(ex, attributeNamePrefix,
									nameSeparator, attrName, contains);
						}

						if (cont) {
							cont = false;
							continue;
						}

						break;
					case Generators.RELATION_NUMERIC_FEATURE:
						// checking how many records exist
						int count = getCount(entities, attrName, h);
						if (shouldChange) {
							setValueToExampleDbl(ex, attributeNamePrefix,
									nameSeparator, attrName, count,
									existingAttrName);
						} else {
							setValueToExampleInt(ex, attributeNamePrefix,
									nameSeparator, attrName, count);
						}
						break;

					case Generators.RELATION_TYPE_NUMERIC_FEATURE:
						int count1 = getCount(entities, attrName, h);
						if (shouldChange) {
							if (entities.contains(attrName)) {
								setValueToExampleDbl(ex, attributeNamePrefix,
										nameSeparator, attrName, count1,
										existingAttrName);
							} else {
								h++;
								continue;
							}
						} else {
							setValueToExampleInt(ex, attributeNamePrefix,
									nameSeparator, attrName, count1);
						}
						break;
					}
				}
				h++;
			}
		}

	}

	/**
	 * Sets boolean value to the given example
	 * 
	 * @param ex
	 *            {@link Example} Given example.
	 * @param attributeNamePrefix
	 *            Prefix as {@link String} used to construct the full name of
	 *            the attribute(s)
	 * @param nameSeparator
	 *            Separator in the full name of new attribute(s) as
	 *            {@link String}
	 * @param attrName
	 *            Old name of the Attribute as {@link String}
	 * @param exists
	 *            The {@link boolean} value to be set
	 */
	private void setValueToExampleBool(Example ex, String attributeNamePrefix,
			String nameSeparator, String attrName, boolean exists) {
		ex.setValue(
				attrs.get(attributeNamePrefix + nameSeparator + attrName),
				AttributeTypeGuesser.getValueForAttribute(
						attrs.get(attributeNamePrefix + nameSeparator
								+ attrName), Boolean.toString(exists)));
	}

	/**
	 * Sets integer value to the given example
	 * 
	 * @param ex
	 *            {@link Example} Given example.
	 * @param attributeNamePrefix
	 *            Prefix as {@link String} used to construct the full name of
	 *            the attribute(s)
	 * @param nameSeparator
	 *            Separator in the full name of new attribute(s) as
	 *            {@link String}
	 * @param attrName
	 *            Old name of the Attribute as {@link String}
	 * @param exists
	 *            The {@link int} value to be set
	 */
	private void setValueToExampleInt(Example ex, String attributeNamePrefix,
			String nameSeparator, String attrName, int count) {
		ex.setValue(
				attrs.get(attributeNamePrefix + nameSeparator + attrName),
				AttributeTypeGuesser.getValueForAttribute(
						attrs.get(attributeNamePrefix + nameSeparator
								+ attrName), Integer.toString(count)));
	}

	/**
	 * Sets double value to the given example
	 * 
	 * @param ex
	 *            {@link Example} Given example.
	 * @param attributeNamePrefix
	 *            Prefix as {@link String} used to construct the full name of
	 *            the attribute(s)
	 * @param nameSeparator
	 *            Separator in the full name of new attribute(s) as
	 *            {@link String}
	 * @param attrName
	 *            Old name of the Attribute as {@link String}
	 * @param exists
	 *            The {@link int} value to be set (will be converted to double)
	 */
	private void setValueToExampleDbl(Example ex, String attributeNamePrefix,
			String nameSeparator, String attrName, int count,
			String fullAttrName) {
		if (fullAttrName == null) {
			fullAttrName = attributeNamePrefix + nameSeparator + attrName;
		}
		ex.setValue(
				attrs.get(fullAttrName),
				AttributeTypeGuesser.getValueForAttribute(
						attrs.get(fullAttrName),
						Double.toString(count
								+ Double.parseDouble(ex.getValueAsString(attrs
										.get(fullAttrName))))));
	}

	// Checks whether the name exists in a list
	private int checkExistance(ArrayList<String> entities, String attrName) {
		int typeExists = 0;
		for (String type : entities) {
			if (attrName.equals(type)) {
				typeExists = 1;
				break;
			}
		}
		return typeExists;
	}

	// Returns the number of relations
	private int getCount(ArrayList<String> entities, String attrName,
			int hCounter) {
		int count = 0;
		if (!queryRunner.isUseCount() || countRelations.size() == 0)
			for (String type : entities) {
				if (attrName.equals(type)) {
					count++;
				}
			}
		else {
			if (countRelations.get(hCounter).containsKey(attrName))
				count = countRelations.get(hCounter).get(attrName);
		}
		return count;
	}

	/**
	 * Sets the value of example for a given attribute
	 * 
	 * @param shoudlChange
	 *            Determines whether we the value should be written into
	 *            ExampleSet in case of MULTIPLE attributes
	 * @param entities
	 *            Set if values
	 * @param ex
	 *            Current example Current {@link Example}
	 * @param attrName
	 *            Old name of the attribute as {@link String}
	 * @param attributeNamePrefix
	 *            Prefix as {@link String} used to construct the full name of
	 *            the attribute(s)
	 * @param nameSeparator
	 *            Separator in the full name of new attribute(s) as
	 *            {@link String}
	 * @param hCounter
	 *            Counter - technical parameter passed
	 * @return
	 */
	private Object[] setExampleValueForAttribute(boolean shoudlChange,
			ArrayList<String> entities, Example ex, String attrName,
			String attributeNamePrefix, String nameSeparator, int hCounter) {
		int resCount = hCounter;
		boolean continueFlag = false;
		boolean contains = entities.contains(attrName);
		if (shoudlChange) {
			// change it only if it is positive
			// TODO check entities.contains(attrName)
			if (contains) {
				setValueToExampleBool(ex, attributeNamePrefix, nameSeparator,
						attrName, contains);
			} else {
				resCount++;
				continueFlag = true;
			}
		} else {
			setValueToExampleBool(ex, attributeNamePrefix, nameSeparator,
					attrName, contains);
		}
		return new Object[] { resCount, continueFlag };
	}

	/**
	 * Generates an {@link OntologyHierarchy}
	 * 
	 * @param addedAttributeOverall
	 * @return {@link OntologyHierarchy} hierarchy
	 * @throws OperatorException
	 */
	public OntologyHierarchy generateHierarchy(
			Map<String, String> addedAttributeOverall,
			SPARQLEndpointQueryRunner localQueryRunner, boolean inferEndpoint)
			throws OperatorException {
		OntologyHierarchy hierarchy = new OntologyHierarchy();
		Map<String, List<String>> overallSuperClasses = new HashMap<String, List<String>>();
		List<String> allAttributesList = new ArrayList<String>();
		allAttributesList.addAll(addedAttributeOverall.keySet());
		for (Entry entry : addedAttributeOverall.entrySet()) {
			List<String> superClasses = queryRunner.getSuperClasses(
					(String) entry.getKey(), allAttributesList,
					queryRunner.GET_SUPERCLASSES_QUERY);
			List<String> directSuperClasses = queryRunner.getSuperClasses(
					(String) entry.getKey(), allAttributesList,
					queryRunner.GET_DIRECT_SUPERCLASSES_QUERY);
			overallSuperClasses.put((String) entry.getKey(), superClasses);
			String supperClassesAppended = "";
			for (String superClass : superClasses) {
				supperClassesAppended += superClass + ", ";
			}
			if (supperClassesAppended.length() > 2) {
				supperClassesAppended = supperClassesAppended.substring(0,
						supperClassesAppended.length() - 2);
			}
			HierarchyPair pair = new HierarchyPair((String) entry.getKey(),
					supperClassesAppended);
			pair.setSuperClasses(superClasses);
			pair.setDirectSuperClasses(directSuperClasses);
			List<String> correspondAttr = new ArrayList<String>();
			correspondAttr.add((String) entry.getValue());
			pair.setCorrespondingAttr(correspondAttr);
			hierarchy.addNewPair(pair);
		}
		for (HierarchyPair pair : hierarchy.getHierarchyPairs()) {
			List<String> subClasses = queryRunner
					.getSubClasses(pair.getBaseClass(), allAttributesList,
							overallSuperClasses);
			pair.setSubClasses(subClasses);
			if (subClasses.size() == 0)
				pair.setLeaf(true);
		}
		return hierarchy;
	}

	/**
	 * Provides initization of basic parameters and values peculiar to all
	 * generators
	 * 
	 * @param isCountRelations
	 *            A {@link boolean} flag enabling counting relations
	 * @param subclassId
	 * @throws OperatorException
	 */
	protected void initAttributesAndParams(boolean isCountRelations,
			String subclassId) throws OperatorException {
		uniqueAtts = new HashMap<String, Integer>();
		exampleSet = cloneExampleSet(mInputPort.getData(ExampleSet.class));
		attrs = exampleSet.getAttributes();
		hierarchy = null;

		createHierarchy = mOutputPortTypesHierarchy.isConnected();

		createRDF = mOutputPortRdfData.isConnected();

		// concept URI with the corresponding attribute name
		addedAttributeOverall = new HashMap<String, String>();

		// if COUNT is enabled use this
		if (isCountRelations)
			countRelations = new HashMap<Integer, Map<String, Integer>>();

		attrNames = new ArrayList<String>();
		attrsBypsass = new ArrayList<String>();

		try {
			queryRunner = SPARQLEndpointQueryRunner.initRunner(this,
					queryRunner);
			// queryRunner.setCachedResults(cachedResults);
		} catch (Exception e) {
			e.printStackTrace();
			throw new UserError(this, 2001, CLASS_NAME, e.getMessage());
		}
		if (queryRunner.mUIThreadRunning)
			getAttsNames(attrNames, attrsBypsass);

		// init the rdf holder if needed
		// if (createRDF) {
		rdfHolder = new RdfHolder();
		// TODO what happens for the URL based
		rdfHolder.setSource(queryRunner.getAlias());
		// setAliasForURLbasedRunner();
		if (attrsBypsass.size() > 0)
			rdfHolder.setOrderedEntities(exampleSet, attrsBypsass.get(0));
		// }

		regexes = new ArrayList<String>();
		String[] regexesTmp = ParameterTypeEnumeration
				.transformString2Enumeration(getParameterAsString(PARAMETER_REGEX_FILTERS));
		for (int i = 0; i < regexesTmp.length; i++) {
			regexes.add(regexesTmp[i]);
		}
	}

	/**
	 * Gets the name of the attributes that are used as a target for feature
	 * extraction
	 * 
	 * @param attsNames
	 * @param attsBypass
	 * @throws OperatorException
	 */
	public void getAttsNames(List<String> attsNames, List<String> attsBypass)
			throws OperatorException {
		if (mInputPortAddedAttrs.isConnected()) {
			ExampleSet set = null;
			try {
				set = mInputPortAddedAttrs.getData(ExampleSet.class);
			} catch (Exception e) {
				throw new UserError(this, 1002, CLASS_NAME, e.getMessage());
			}
			for (Example ex : set) {
				attsNames.add(ex.getValueAsString(set.getAttributes().get(
						NEW_ATTRIBUTES)));// Endpoints
				attsBypass.add(ex.getValueAsString(set.getAttributes().get(
						BYPASSING_ATTRIBUTES)));
			}
		} else {
			boolean isParamFound = true;

			if (isParameterSet(PARAMETER_ATTRIBUTE_TO_EXTEND)) {
				String[] macroNames = ParameterTypeEnumeration
						.transformString2Enumeration(getParameterAsString(PARAMETER_ATTRIBUTE_TO_EXTEND));
				for (String macroName : macroNames) {
					if (exampleSet.getAttributes().get(macroName.trim()) == null)
						throw new UserError(
								this,
								1006,
								CLASS_NAME,
								"The specified attribute \""
										+ macroName
										+ "\" is not defined for the ExampleSet");
					attsBypass.add(macroName);
					attsNames.add(macroName);
				}
				isParamFound = false;
			}
			if (isParamFound)
				throw new UserError(this, 1003, CLASS_NAME);
		}
	}

	@Override
	public void processFinished() throws OperatorException {
		if (queryRunner != null) {
			queryRunner.mUIThreadRunning = false;
			queryRunner.finalizeAsyncThread();
		}
		// empty the query results cache if needed
		Boolean emptycache = Boolean
				.parseBoolean(ParameterService
						.getParameterValue(PluginInitLOD6.PROPERTY_RMLOD_QUERY_CACHE_REMOVE_AFTER_PROCESS));
		if (emptycache)
			SPARQLEndpointQueryRunner.cachedResults.clear();

		// empty the model results cache if needed
		emptycache = Boolean
				.parseBoolean(ParameterService
						.getParameterValue(PluginInitLOD6.PROPERTY_RMLOD_MODEL_REMOVE_AFTER_PROCESS));
		if (emptycache)
			FileBasedQueryRunner.cachedRunners.clear();

		super.processFinished();
	}

	public void setAliasForURLbasedRunner(int index, RdfHolder holder) {
		if (queryRunner instanceof URLBasedQueryRunner) {
			URI uri;
			try {
				uri = new URI(exampleSet.getExample(0)
						.getValueAsString(
								exampleSet.getAttributes().get(
										attrsBypsass.get(index))));
				String domain = uri.getHost();
				holder.setSource(domain);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else
			holder.setSource(queryRunner.getAlias());
	}

	public String createFilteredQuery(String query, String var,
			List<String> regexList) {
		if (regexList == null)
			regexList = regexes;
		if (regexList.size() > 0) {
			query = query.substring(0, query.length() - 1);
			for (String filter : regexList) {
				if (filter.startsWith("!")) {
					query += " FILTER(!REGEX(" + var + ",\""
							+ filter.substring(1) + "\", \"i\")).";
				} else {
					query += " FILTER(REGEX(" + var + ",\"" + filter
							+ "\", \"i\")).";
				}
			}
			query += "}";
		}

		return query;
	}

	/**
	 * Clones the input exampleSet; It should be used in each operator that
	 * alters the exampleSet
	 * 
	 * @param inputExampleSet
	 * @return
	 */
	public static ExampleSet cloneExampleSet(ExampleSet inputExampleSet) {
		ExampleSet applySet = null;

		int type = DataRowFactory.TYPE_DOUBLE_ARRAY;
		if (inputExampleSet.getExampleTable() instanceof MemoryExampleTable) {
			DataRowReader dataRowReader = inputExampleSet.getExampleTable()
					.getDataRowReader();
			if (dataRowReader.hasNext()) {
				type = dataRowReader.next().getType();
			}
		}
		// check if type is supported to be copied
		if (type >= 0) {
			try {
				applySet = MaterializeDataInMemory.materializeExampleSet(
						inputExampleSet, type);
			} catch (UndefinedParameterError e) {
				// TODO Auto-generated catch block
				applySet = null;
			}
		}

		if (applySet == null)
			applySet = (ExampleSet) inputExampleSet.clone();

		return applySet;
	}
}
