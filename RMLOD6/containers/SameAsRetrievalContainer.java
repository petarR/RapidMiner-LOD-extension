package lod.containers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lod.dataclasses.ExampleTreeMapping;
import lod.dataclasses.TreeNode;
import lod.generators.BaseGenerator;
import lod.gui.tools.dialogs.SPARQLConfigurator;
import lod.utils.AttributeTypeGuesser;
import lod.utils.SameAsBunchExtractor;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.io.Base64;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.config.ParameterTypeConfigurable;

/**
 * Enables semi-automatic search of sameAs links with a specified depth. Appends
 * the input {@link ExampleSet} with a set of sameAs links grouped by base URLs
 * of these links and passes this set to all the objects of
 * {@link BaseGenerator} class that are included to the subprocess.
 * 
 * @author Evgeny Mitichkin
 * 
 */
public class SameAsRetrievalContainer extends OperatorChain {
	private static final String CLASS_NAME = "same_as_retrieveal_contiainer";

	public static final String BASE_GENERATOR_PARAMETER_ATTRIBUTE_TO_EXTEND = "Attribute to extend";

	public static final String BASE_GENERATOR_PARAMETER_ATTRIBUTE_TO_EXTEND_NAME = "Attribute to extend name";

	public static final String PARAMETER_SHOULD_PASS_PARAMETER_VALUES = "Override parameter values of nested operators";

	public static final String PARAMETER_RESOLVE_BY_URI = "Use URI data model";

	public static final String PARAMETER_SPARQL_MANAGER = "SPARQL connection";

	public static final String PARAMETER_NUMBER_OF_HOPS = "Number of hops";

	public static final String PARAMETER_USE_OPTIONAL_LINKS = "Use LOD sources filter";

	public static final String PARAMETER_OPTIONAL_LINKS = "LOD sources";

	public static final String PARAMETER_LIST_URL = "URL";

	public static final String PARAMETER_LIST_LINK = "Link";

	public static final String PARAMETER_LIST_LINKS_TO_FOLLOW = "List of links to follow";

	public static final String PARAMETER_ATRIBUTE_FOR_SEARCH = "Link Attribute's Name";

	private static final String NEW_ATTRIBUTES = "New Attributes";
	private static final String BYPASSING_ATTRIBUTES = "Bypassing Attributes";

	protected PortPairExtender outputExtender = new PortPairExtender("out",
			getSubprocess(0).getInnerSinks(), getOutputPorts());

	// input
	private InputPort mInputPort;
	/**
	 * Input port containing metadata for the generators, i.e. which attributes
	 * should be used as a target
	 */
	protected InputPort mInputPortAddedAttrs;

	// Modified input
	private OutputPort mInputPortAppended;
	// private OutputPort mInputPortAttrs;

	// Output
	private OutputPort mOutputPort;

	private SameAsBunchExtractor extractor;

	/**
	 * initial example set
	 */
	private ExampleSet exampleSet;

	/**
	 * link - variable in the query
	 */
	private Map<String, String> linksToFollow;

	/** This constructor allows subclasses to change the subprocess' name. */
	protected SameAsRetrievalContainer(OperatorDescription description,
			String subProcessName) {
		super(description, subProcessName);
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		initPorts();
	}

	public SameAsRetrievalContainer(OperatorDescription description) {
		this(description, "Nested Chain");
	}

	private void initPorts() {
		mInputPort = getInputPorts()
				.createPort("Example Set", ExampleSet.class);
		mInputPort.addPrecondition(new SimplePrecondition(mInputPort,
				new MetaData(ExampleSet.class)));

		mInputPortAddedAttrs = getInputPorts()
				.createPort("Attributes Appended");

		mInputPortAppended = getSubprocess(0).getInnerSources().createPort(
				"links");
		// mInputPortAttrs =
		// getSubprocess(0).getInnerSources().createPort("Attributes Added");

		mOutputPort = getOutputPorts().createPort("links");

		getTransformer().addPassThroughRule(mInputPort, mOutputPort);
		// getTransformer().addPassThroughRule(mInputPort, mInputPortAppended);

		getTransformer()
				.addGenerationRule(mInputPortAppended, ExampleSet.class);
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		// getTransformer().addGenerationRule(mInputPortAttrs,
		// ExampleSet.class);

		outputExtender.start();
		getTransformer().addRule(outputExtender.makePassThroughRule());
	}

	@Override
	public void doWork() throws OperatorException {
		clearAllInnerSinks();

		if (getParameterAsBoolean(PARAMETER_SHOULD_PASS_PARAMETER_VALUES)) {
			setParametersToOperators();
		}

		exampleSet = mInputPort.getData(ExampleSet.class);
		Attributes attrs = exampleSet.getAttributes();

		String mAttrSearch = getAttsNames();
		if (exampleSet.getAttributes().get(mAttrSearch) == null)
			throw new UserError(this, 1006, CLASS_NAME,
					"The specified attribute \"" + mAttrSearch
							+ "\" is not defined for the ExampleSet");
		Attribute initialAttribute = attrs.get(mAttrSearch);

		// get the links to search for
		getLinksToFollow();

		ArrayList<String> newAttributeNames = new ArrayList<String>();// Endpoits
		ArrayList<String> bypassingAttributes = new ArrayList<String>();// Combined
																		// name
																		// of
																		// endpoint
																		// and
																		// constant

		List<ExampleTreeMapping> exampleToTreeMappings = getListOfSameAsLinks(
				initialAttribute, exampleSet);
		List<Attribute> newAttributes = createNewFeatures(
				exampleToTreeMappings, exampleSet.getAttributes().allSize());

		addNewFeaturesToExampleSet(exampleSet, newAttributes);
		addBypassingValues(initialAttribute, newAttributes, newAttributeNames,
				bypassingAttributes);

		setFeatureValues(exampleSet, exampleToTreeMappings, newAttributes);
		ExampleSet metaDataForGenerators = createBypassingExampleTable(
				newAttributeNames, bypassingAttributes);

		mInputPortAppended.deliver(exampleSet);
		// mInputPortAttrs.deliver(metaDataForGenerators);
		presetGeneratorValues(metaDataForGenerators);
		mOutputPort.deliver((IOObject) exampleSet.clone());

		super.doWork();
		outputExtender.passDataThrough();
	}

	/**
	 * gets all links to be followed with the var name
	 * 
	 * @throws UserError
	 */
	private void getLinksToFollow() throws UserError {
		String[] searchPatterns = null;

		searchPatterns = ParameterTypeEnumeration
				.transformString2Enumeration(getParameterAsString(PARAMETER_LIST_LINKS_TO_FOLLOW));
		if (searchPatterns == null || searchPatterns.length == 0) {
			searchPatterns = new String[2];
			searchPatterns[0] = "owl:sameAs";
			searchPatterns[1] = "rdfs:seeAlso";
		}

		linksToFollow = new HashMap<String, String>();

		for (String link : searchPatterns) {
			String resolvedLink = resolve(link).trim();
			if (!resolvedLink.startsWith("<"))
				resolvedLink = "<" + resolvedLink + ">";
			linksToFollow.put(resolvedLink, getVarFromLink(link).trim());
		}

	}

	private String resolve(String link) {
		return lod.utils.PrefixResolver.resolveQuery(link,
				new HashMap<String, String>());

	}

	/**
	 * Creates a valid variable name from a link (links may contain characters
	 * not allowed in a variable name)
	 * 
	 * @param link
	 * @return
	 */
	private String getVarFromLink(String link) {

		return Base64.encodeBytes(link.getBytes()).replace("=", "_");

	}

	private void presetGeneratorValues(ExampleSet metaDataForGenerators) {
		String str = "";
		String sep = ",";
		int cnt = 0;
		for (Example ex : metaDataForGenerators) {
			if (cnt == metaDataForGenerators.size() - 1)
				sep = "";
			str += ex.getValueAsString(metaDataForGenerators.getAttributes()
					.get(NEW_ATTRIBUTES)) + sep;
			cnt++;
		}

		List<Operator> operators = getAllInnerOperators();
		for (Operator op : operators) {
			if (op instanceof BaseGenerator)
				op.setParameter(BASE_GENERATOR_PARAMETER_ATTRIBUTE_TO_EXTEND,
						str);
		}
	}

	/**
	 * Creates a set of attributes to be transferred to Generators
	 * 
	 * @param newAttributeNames
	 * @param bypassingAttributes
	 * @return
	 */
	private ExampleSet createBypassingExampleTable(
			ArrayList<String> newAttributeNames,
			ArrayList<String> bypassingAttributes) {
		// construct attribute set for metadata transfer
		Attribute[] attributes = new Attribute[2];
		attributes[0] = AttributeFactory.createAttribute(NEW_ATTRIBUTES,
				Ontology.STRING);
		attributes[1] = AttributeFactory.createAttribute(BYPASSING_ATTRIBUTES,
				Ontology.STRING);

		MemoryExampleTable table = new MemoryExampleTable(attributes);

		@SuppressWarnings("deprecation")
		DataRowFactory ROW_FACTORY = new DataRowFactory(
				DataRowFactory.TYPE_SPARSE_MAP);

		for (int i = 0; i < bypassingAttributes.size(); i++) {
			DataRow row = ROW_FACTORY.create(
					new String[] { newAttributeNames.get(i),
							bypassingAttributes.get(i) }, attributes);
			table.addDataRow(row);
		}

		ExampleSet ioListResult = table.createExampleSet();
		return ioListResult;
	}

	/**
	 * Sets the values of feature to the ExampleSet
	 * 
	 * @param exampleSet
	 * @param mappings
	 * @param features
	 */
	private void setFeatureValues(ExampleSet exampleSet,
			List<ExampleTreeMapping> mappings, List<Attribute> features) {
		// Set the values for each Example in the ExampleSet
		for (Example ex : exampleSet) {
			for (ExampleTreeMapping mapping : mappings) {
				boolean mappingsFound = false;
				// Lookup the mapping for the Example in the data (tree)
				if (mapping.getEx().toString().equals(ex.toString())) {
					mappingsFound = true;
					// Set the values for every attribute
					for (Attribute attr : features) {
						// Lookup the values of the attribute in the tree
						boolean valueFound = false;
						for (TreeNode node : mapping.getSameAsTree()) {
							if (node.getLevel() > 1
									&& node.getFilter().equals(attr.getName())) {
								ex.setValue(attr, node.getValue());
								valueFound = true;
								break;
							}
						}
						if (!valueFound) {
							ex.setValue(attr, AttributeTypeGuesser
									.getValueForAttribute(attr, null));
						}
					}
				}
				if (mappingsFound)
					break;
			}
		}
	}

	/**
	 * Adds information about the new attributes created to the arrays that are
	 * used in Generators to distinguish old and new attributes.
	 * 
	 * @param features
	 * @param newAttributeNames
	 * @param bypassingAttributes
	 */
	private void addBypassingValues(Attribute initialAttribute,
			List<Attribute> features, ArrayList<String> newAttributeNames,
			ArrayList<String> bypassingAttributes) {
		newAttributeNames.add(initialAttribute.getName());
		bypassingAttributes.add(initialAttribute.getName());
		for (Attribute feature : features) {
			// Adding the attribute to transferring arrays
			newAttributeNames.add(feature.getName());
			bypassingAttributes.add(feature.getName());
		}
	}

	/**
	 * Adds new {@link Attribute} objects (features) to a given
	 * {@link ExampleSet}
	 * 
	 * @param eSet
	 * @param features
	 */
	private void addNewFeaturesToExampleSet(ExampleSet eSet,
			List<Attribute> features) {
		for (Attribute feature : features) {
			eSet.getExampleTable().addAttribute(feature);
			eSet.getAttributes().addRegular(feature);
		}
	}

	/**
	 * Creates a set of new unique features based on {@link Example}-to-link
	 * mappings
	 * 
	 * @param exampleToTreeMappings
	 * @param exampleSet
	 * @return
	 */
	private List<Attribute> createNewFeatures(
			List<ExampleTreeMapping> exampleToTreeMappings, int exampleSetSize) {
		// Create a set of features to extend the given ExampleSet
		List<String> attributesToAdd = new ArrayList<String>();
		for (ExampleTreeMapping mapping : exampleToTreeMappings) {
			for (TreeNode node : mapping.getSameAsTree()) {
				if (node.getLevel() > 1) {
					if (attributesToAdd.size() == 0) {
						attributesToAdd.add(node.getFilter());
					} else {
						boolean attributeExists = false;
						for (String attrStr : attributesToAdd) {
							if (node.getFilter().equals(attrStr)) {
								attributeExists = true;
								break;
							}
						}
						if (!attributeExists) {
							attributesToAdd.add(node.getFilter().trim());
						}
					}
				}
			}
		}

		List<Attribute> newFeatures = new ArrayList<Attribute>();

		for (int i = 0; i < attributesToAdd.size(); i++) {
			Attribute newFeature = null;
			newFeature = AttributeFactory.createAttribute(
					attributesToAdd.get(i), Ontology.STRING);
			newFeature.setTableIndex(exampleSetSize + i);
			newFeatures.add(newFeature);
		}

		return newFeatures;
	}

	/**
	 * Creates a {@link List} of trees mapped to a particular Examples. (One
	 * tree per {@link Example}).
	 * 
	 * @param initialAttribute
	 * @param exampleSet
	 * @return
	 * @throws OperatorException
	 */
	private List<ExampleTreeMapping> getListOfSameAsLinks(
			Attribute initialAttribute, ExampleSet exampleSet)
			throws OperatorException {

		String[] searchPatterns = null;
		if (getParameterAsBoolean(PARAMETER_USE_OPTIONAL_LINKS)) {
			searchPatterns = ParameterTypeEnumeration
					.transformString2Enumeration(getParameterAsString(PARAMETER_OPTIONAL_LINKS));
		}

		extractor = new SameAsBunchExtractor(this, "", searchPatterns);
		extractor.setLinksToFollow(linksToFollow);

		// Retrieve the list of links for current iteration for all entries
		List<ExampleTreeMapping> exampleToTreeMappings = new ArrayList<ExampleTreeMapping>();
		if (initialAttribute != null) {
			if (!initialAttribute.equals("")) {
				for (Example ex : exampleSet) {
					if (extractor.getQueryRunner().mUIThreadRunning) {
						List<TreeNode> initialSeed = new ArrayList<TreeNode>();
						initialSeed.add(new TreeNode(ex
								.getValueAsString(initialAttribute), 1));
						List<TreeNode> treeOfLinks = getTreeOfLinks(
								initialSeed, 1,
								getParameterAsInt(PARAMETER_NUMBER_OF_HOPS) + 1);
						exampleToTreeMappings.add(new ExampleTreeMapping(
								treeOfLinks, ex));
					} else {
						break;
					}
				}
			} else {
				throw new OperatorException(
						"Problem in SameAsLinker: the name of new attribute is not defined");
			}
		}

		return exampleToTreeMappings;
	}

	/**
	 * Create a tree of links for a given {@link Example}
	 * 
	 * @param links
	 * @param level
	 * @param maxDepth
	 * @return
	 * @throws OperatorException
	 */
	public List<TreeNode> getTreeOfLinks(List<TreeNode> links, int level,
			int maxDepth) throws OperatorException {
		if (level == maxDepth) {
			return links;
		} else {
			// Retrieve links for a particular level
			for (int i = 0; i < links.size(); i++) {
				if (links.get(i).getLevel() == level
						&& !links.get(i).isPassed()) {
					extractor.setmSameAsConcept(links.get(i).getValue());
					links.get(i).setPassed(true);
					List<TreeNode> localSameAsLinksFound = extractor
							.getSameAsLinks(level);
					// Find and remove duplicates. Then add new items.
					if (localSameAsLinksFound.size() > 0) {
						for (TreeNode strLoc : localSameAsLinksFound) {
							boolean linkExists = false;
							for (TreeNode strGl : links) {
								if (strLoc.getValue().equals(strGl.getValue())) {
									linkExists = true;
									break;
								}
							}
							if (!linkExists)
								links.add(new TreeNode(strLoc.getValue(),
										level + 1, strLoc.getFilter()));
						}
					}
				}
			}
			level++;
			return getTreeOfLinks(links, level, maxDepth);
		}
	}

	/**
	 * Sets the values of parameters ({@literal "SPARQL connection"} in this
	 * case) to all nested operators if their subclass is {@link BaseGenerator}.
	 * 
	 * @throws OperatorException
	 */
	protected void setParametersToOperators() throws OperatorException {
		List<Operator> operators = getAllInnerOperators();
		for (Operator op : operators) {
			if (op instanceof BaseGenerator) {
				op.setParameter(PARAMETER_RESOLVE_BY_URI,
						Boolean.toString(true));
				if (op.isParameterSet(BASE_GENERATOR_PARAMETER_ATTRIBUTE_TO_EXTEND)) {
					op.setParameter(
							BASE_GENERATOR_PARAMETER_ATTRIBUTE_TO_EXTEND, "");
				}
			}
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeBoolean(
				PARAMETER_SHOULD_PASS_PARAMETER_VALUES,
				"Determines whether the container parameters should be applied to the nested generators",
				true, true));

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

		types.add(new ParameterTypeString(
				PARAMETER_ATRIBUTE_FOR_SEARCH,
				"Name of the attribute in the Example Set that will be used as starting point for exploring. ",
				false, false));

		// set the default links
		ParameterTypeEnumeration typeList1 = new ParameterTypeEnumeration(
				PARAMETER_LIST_LINKS_TO_FOLLOW,
				"List of links to follow. (if empty, owl:SameAs and rdfs:SeeAlso will be used	)",
				new ParameterTypeString(PARAMETER_LIST_LINK,
						"Specify the links to follow. "), false);
		// List<String> defaultValues = new ArrayList<String>();
		// defaultValues.add("owl:SameAs");
		// // defaultValues.add("rdfs:seeAlso");
		// typeList1.setDefaultValue(ParameterTypeEnumeration
		// .transformEnumeration2String(defaultValues));
		// String[] defaultValues = new String[2];
		// defaultValues[0] = "owl:SameAs";
		// defaultValues[1] = "rdfs:seeAlso";
		// typeList1.setDefaultValue(defaultValues);
		types.add(typeList1);

		types.add(new ParameterTypeInt(PARAMETER_NUMBER_OF_HOPS,
				"Number of hops to follow links", 1, 999, 2, false));

		types.add(new ParameterTypeBoolean(
				PARAMETER_USE_OPTIONAL_LINKS,
				"If checked, a list of LOD sources will be used for exploring new links",
				false, false));
		ParameterTypeEnumeration typeList = new ParameterTypeEnumeration(
				PARAMETER_OPTIONAL_LINKS,
				"List of sources to be used (i.e. to use links from only from DBpedia, enter: http://dbpedia.org/)",
				new ParameterTypeString(PARAMETER_LIST_URL, "URL"), false);

		typeList.registerDependencyCondition(new BooleanParameterCondition(
				this, PARAMETER_USE_OPTIONAL_LINKS, false, true));

		types.add(typeList);

		return types;
	}

	@Override
	public void processFinished() throws OperatorException {
		if (extractor != null)
			extractor.stop();
		super.processFinished();
	}

	/**
	 * Gets the name of the attributes that are used as a target for feature
	 * extraction
	 * 
	 * @param attsNames
	 * @param attsBypass
	 * @throws OperatorException
	 */
	public String getAttsNames() throws OperatorException {
		if (mInputPortAddedAttrs.isConnected()) {
			ExampleSet set = null;
			try {
				set = mInputPortAddedAttrs.getData(ExampleSet.class);
			} catch (Exception e) {
				throw new UserError(this, 1002, CLASS_NAME, e.getMessage());
			}
			for (Example ex : set) {
				return ex.getValueAsString(set.getAttributes().get(
						BYPASSING_ATTRIBUTES));
			}
		} else {
			boolean isParamFound = true;

			if (isParameterSet(PARAMETER_ATRIBUTE_FOR_SEARCH)) {
				String attName = getParameterAsString(PARAMETER_ATRIBUTE_FOR_SEARCH);

				if (exampleSet.getAttributes().get(attName.trim()) == null)
					throw new UserError(this, 1006, CLASS_NAME,
							"The specified attribute \"" + attName
									+ "\" is not defined for the ExampleSet");
				return attName;

			}
			if (isParamFound)
				throw new UserError(this, 1003, CLASS_NAME);
		}
		return "";
	}

}