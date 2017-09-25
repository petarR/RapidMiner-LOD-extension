package lod.linking;

import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import lod.generators.BaseGenerator;
import lod.gui.tools.utils.OntologySelectorWizardCreator;
import lod.http.WebQueryRunner;
import lod.utils.DBSelection;
import lod.utils.ValueComparator;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

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
import com.rapidminer.parameter.ParameterTypeConfiguration;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;
import com.wcohen.ss.Jaccard;
import com.wcohen.ss.JaroWinkler;
import com.wcohen.ss.Levenstein;

/**
 * Enables DBPedia Lookup search by term. Supports ontology delector to specify
 * the search.
 * 
 * @author Evgeny Mitichkin
 * @author Petar Ristoski
 * 
 */
public class LookupLinker extends Operator {

	private static final String CLASS_NAME = "lookup_linker";

	private InputPort mInputPort;
	private OutputPort mOutputPort;
	private OutputPort mOutputPortAttrs;

	public static int CONNECTION_TIMEOUT = 5000;// msec

	public static final String PARAMETER_NEW_ATTRIBUTE_NAME = "New attribute";
	public static final String PARAMETER_QUERY_CLASS = "Query Class";
	// public static final String PARAMETER_QUERY_CLASS_FROM_DBPEDIA_ONTOLOGY =
	// "Query Class from Ontology";
	public static final String PARAMETER_DBPEDIA_ENDPOINT = "DBPedia Lookup API";
	public static final String PARAMETER_DBPEDIA_ENDPOINT_CONNECTION_TIMEOUT_MSEC = "Connection timeout msec";
	public static final String PARAMETER_POP_CONCEPT = "Attribute";
	public static final String PARAMETER_ADDITIONAL_STRING = "Additional String";
	public static final String PARAMETER_MAX_HITS = "Max Hits";
	public static final String PARAMETER_SELECTION_METHOD = "Selection Method";
	public static final String PARAMETER_ONTOLOGY_SELECTOR = "Ontology class selection";
	public static final String PARAMETER_CUSTOM_LOOKUP_API = "Custom API Endpoint";

	private static final String NEW_ATTRIBUTES = "New Attributes";
	private static final String BYPASSING_ATTRIBUTES = "Bypassing Attributes";

	private Map<String, String> cachedResults = new HashMap<String, String>();

	// add different lanugages
	public static final String[] LOOKUP_API = new String[] { "KeywordSearch",
			"PrefixSearch", "Custom" };
	private static final Map<Integer, String> LOOKUP_API_Selection = new HashMap<Integer, String>() {
		{
			put(0, "http://lookup.dbpedia.org/api/search/KeywordSearch");
			put(1, "http://lookup.dbpedia.org/api/search/KeywordSearch");
		}
	};

	public LookupLinker(OperatorDescription description) {
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

	/**
	 * retrieves place concept from DBpedia lookup service
	 * 
	 * @param popConcept
	 *            label
	 * @param addKeyword
	 *            department or region
	 * @param option
	 *            edit distance or first
	 * @returnO
	 */
	public String getConceptDbPedia(String popConcept, String addKeyword,
			DBSelection option, String dbpediaKeywordSearchEndpoint,
			int maxHits, String queryClass) {
		// check if we have cashed results
		if (cachedResults.containsKey(popConcept.toLowerCase() + "|"
				+ addKeyword + "|" + option.toString() + "|"
				+ dbpediaKeywordSearchEndpoint + "|" + maxHits + "|"
				+ queryClass)) {
			return cachedResults.get(popConcept.toLowerCase() + "|"
					+ addKeyword + "|" + option.toString() + "|"
					+ dbpediaKeywordSearchEndpoint + "|" + maxHits + "|"
					+ queryClass);

		}
		String dbPediaConcept = null;
		// construct the url
		try {
			if (queryClass == null)
				queryClass = "";
			String urlString = dbpediaKeywordSearchEndpoint + "?MaxHits="
					+ maxHits + "&QueryClass=" + queryClass + "&QueryString="
					+ URLEncoder.encode(popConcept);

			WebQueryRunner mWebQueryRunner = new WebQueryRunner(urlString,
					CONNECTION_TIMEOUT);
			String res = mWebQueryRunner.makeGetInterruptable();

			if (!res.equals("")) {
				// invoke DBpedia lookup service and parse the results
				InputSource is = new InputSource(new StringReader(res));
				DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document doc = builder.parse(is);

				try {
					dbPediaConcept = selectDBConcept(doc, option, popConcept,
							addKeyword);
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (dbPediaConcept != null && dbPediaConcept.equals(""))
					dbPediaConcept = null;
			}
			{
				// TODO there should be a missing value
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		// adds additional keyword to the search
		// put the results in the cache
		cachedResults.put(popConcept.toLowerCase() + "|" + addKeyword + "|"
				+ option.toString() + "|" + dbpediaKeywordSearchEndpoint + "|"
				+ maxHits + "|" + queryClass, dbPediaConcept);

		return dbPediaConcept;
	}

	/**
	 * selects the concept from lookup service based on the option
	 * 
	 * @param doc
	 * @param option
	 * @param popConcept
	 * @param additionalKeyword
	 * @return
	 * @throws XPathExpressionException
	 */
	public static String selectDBConcept(Document doc, DBSelection option,
			String popConcept, String additionalKeyword)
			throws XPathExpressionException {
		if (option == DBSelection.FIRST) {
			XPathFactory xFactory = XPathFactory.newInstance();
			XPath xpath = xFactory.newXPath();
			XPathExpression expr = xpath.compile("/ArrayOfResult/Result/URI");
			return (String) expr.evaluate(doc, XPathConstants.STRING);
		} else {
			XPathFactory xFactory = XPathFactory.newInstance();
			XPath xpath = xFactory.newXPath();
			XPathExpression expr = xpath.compile("/ArrayOfResult//Result");

			NodeList uriNodes = (NodeList) expr.evaluate(doc,
					XPathConstants.NODESET);
			// calculate the edit distance between the DB candiadateconcepts
			// and
			// the current concept
			Map<String, Double> candidateConcepts = new HashMap<String, Double>();
			Map<String, Double> candidateConceptsWithKeyword = new HashMap<String, Double>();
			for (int i = 0; i < uriNodes.getLength(); i++) {
				expr = xpath.compile("Label");
				String label = (String) expr.evaluate(uriNodes.item(i),
						XPathConstants.STRING);
				// System.out.println(label);
				expr = xpath.compile("URI");
				String uri = (String) expr.evaluate(uriNodes.item(i),
						XPathConstants.STRING);
				// System.out.println(uri);
				// calculate the edit distance
				candidateConcepts.put(uri, getScore(option, popConcept, label));
				candidateConceptsWithKeyword.put(
						uri,
						getScore(option, popConcept + " " + additionalKeyword,
								label));
			}
			// sort the map
			ValueComparator bvc = new ValueComparator(candidateConcepts);
			TreeMap<String, Double> sorted_map = new TreeMap<String, Double>(
					bvc);
			ValueComparator bvc2 = new ValueComparator(
					candidateConceptsWithKeyword);
			TreeMap<String, Double> sortedMapWithKeyword = new TreeMap<String, Double>(
					bvc2);
			sorted_map.putAll(candidateConcepts);
			sortedMapWithKeyword.putAll(candidateConceptsWithKeyword);
			if (sorted_map.size() > 0) {
				String tmpConcept = sorted_map.firstKey();
				if (sortedMapWithKeyword.firstEntry().getValue() > sorted_map
						.firstEntry().getValue())
					tmpConcept = sortedMapWithKeyword.firstKey();

				return tmpConcept;
			}
		}
		return null;
	}

	private static double getScore(DBSelection option, String s1, String s2) {
		double res = -1;
		if (option == DBSelection.EDIT_DISTANCE) {
			Levenstein lev = new Levenstein();
			res = lev.score(s1, s2);
		} else if (option == DBSelection.JARO_WINKLER) {
			JaroWinkler jw = new JaroWinkler();
			res = jw.score(s1, s2);
		} else if (option == DBSelection.JACCARD) {
			Jaccard jcrd = new Jaccard();
			res = jcrd.score(s1, s2);
		}
		return res;
	}

	/**
	 * Returns FIRST mode by default
	 * 
	 * @param strDbSelector
	 * @return DBSelection enum object
	 */
	private DBSelection getDbSelection(String strDbSelector) {
		DBSelection selector = DBSelection.FIRST;
		if (strDbSelector.equals("EDIT_DISTANCE")) {
			selector = DBSelection.EDIT_DISTANCE;
		}
		if (strDbSelector.equals("JARO_WINKLER")) {
			selector = DBSelection.JARO_WINKLER;
		}
		if (strDbSelector.equals("JACCARD")) {
			selector = DBSelection.JACCARD;
		}
		return selector;
	}

	public void doWork() throws OperatorException {
		ExampleSet exampleSet = BaseGenerator.cloneExampleSet(mInputPort.getData(ExampleSet.class));
		Attributes attrs = exampleSet.getAttributes();

		// select the lookup endpoint
		String mDBPediaLookup = "";
		int selectedAPI = getParameterAsInt(PARAMETER_DBPEDIA_ENDPOINT);
		if (selectedAPI < LOOKUP_API_Selection.size()) {
			mDBPediaLookup = LOOKUP_API_Selection.get(selectedAPI);
		} else {
			mDBPediaLookup = getParameterAsString(PARAMETER_CUSTOM_LOOKUP_API);
		}

		String mPopConcept = getParameterAsString(PARAMETER_POP_CONCEPT);
		String mAdditionalAttribute = getParameterAsString(PARAMETER_ADDITIONAL_STRING);
		String mQueryClass = getParameterAsString(PARAMETER_QUERY_CLASS);
		// String mQueryClass =
		// getParameterAsString(PARAMETER_QUERY_CLASS_FROM_DBPEDIA_ONTOLOGY);
		DBSelection mDbSelectionOption = getDbSelection(getParameterAsString(PARAMETER_SELECTION_METHOD));
		int mMaxHits = getParameterAsInt(PARAMETER_MAX_HITS);
		CONNECTION_TIMEOUT = getParameterAsInt(PARAMETER_DBPEDIA_ENDPOINT_CONNECTION_TIMEOUT_MSEC);

		ArrayList<String> newAttributeNames = new ArrayList<String>();// Endpoits
		ArrayList<String> bypassingAttributes = new ArrayList<String>();// Combined
																		// name
																		// of
																		// endpoint
																		// and
																		// constant

		Attribute attr = attrs.get(mPopConcept);

		if (attr != null) {
			// Creating the attribute
			String attrToCombineName = "";
			attrToCombineName = getParameterAsString(PARAMETER_NEW_ATTRIBUTE_NAME);

			if (!attrToCombineName.equals("")) {
				Attribute mAttributeCombined = null;

				mAttributeCombined = AttributeFactory.createAttribute(
						attrToCombineName, Ontology.STRING);
				mAttributeCombined.setTableIndex(exampleSet.getAttributes()
						.allSize());

				// add new attribute
				exampleSet.getExampleTable().addAttribute(mAttributeCombined);
				exampleSet.getAttributes().addRegular(mAttributeCombined);

				// Adding the attribute to transferring arrays
				newAttributeNames.add(mAttributeCombined.getName());
				bypassingAttributes.add(mAttributeCombined.getName());

				for (Example ex : exampleSet) {
					// Lookup and put the values
					String value = "";
					String mConceptValue = ex.getValueAsString(attr);
					String url = getConceptDbPedia(mConceptValue,
							mAdditionalAttribute, mDbSelectionOption,
							mDBPediaLookup, mMaxHits, mQueryClass);
					if (url != null) {
						if (!url.equals("")) {
							value = url;
						}
					} else {
						value = "?";
					}
					ex.setValue(mAttributeCombined, value);
				}
			} else {
				throw new UserError(this, 1005, CLASS_NAME);
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
					"Problem in LookupLinker: No attributes with the name '"
							+ getParameterAsString(PARAMETER_POP_CONCEPT)
							+ "' found.");
		}
		super.doWork();
	}

	@Override
	public List<ParameterType> getParameterTypes() {

		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeString(PARAMETER_NEW_ATTRIBUTE_NAME,
				"This parameter defines the name of the new attribute",
				"New_Link", false));

		types.add(new ParameterTypeString(PARAMETER_POP_CONCEPT,
				"Name of the attribute for lookup from table", false, false));

		types.add(new ParameterTypeString(PARAMETER_QUERY_CLASS,
				"Name of the class of the object queried", true, true));

		// TODO Add ontology selector here
		ParameterType type = new ParameterTypeConfiguration(
				OntologySelectorWizardCreator.class, this);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_DBPEDIA_ENDPOINT,
				"DBPedia endpoint address", LOOKUP_API, 0, false);
		types.add(type);

		type = new ParameterTypeString(PARAMETER_CUSTOM_LOOKUP_API,
				"Custom DBpedia Lookup endpoint", "", false);
		type.registerDependencyCondition(new EqualTypeCondition(this,
				PARAMETER_DBPEDIA_ENDPOINT, LOOKUP_API, true, 2));
		types.add(type);

		types.add(new ParameterTypeInt(
				PARAMETER_DBPEDIA_ENDPOINT_CONNECTION_TIMEOUT_MSEC,
				"Connection timeout", 10, 100000, CONNECTION_TIMEOUT, true));

		types.add(new ParameterTypeString(PARAMETER_ADDITIONAL_STRING,
				"Additional string to search for", true, true));

		types.add(new ParameterTypeInt(PARAMETER_MAX_HITS,
				"Maximum number of hits", 1, 99999, 5, true));

		types.add(new ParameterTypeCategory(PARAMETER_SELECTION_METHOD,
				"DBPedia selection method", DBPEDIA_METHODS, MTD_EDIT_DISTANCE,
				true));

		return types;
	}

	/** The types of connection methods to DBPedia */
	public static final String[] DBPEDIA_METHODS = {
			DBSelection.FIRST.toString(), DBSelection.EDIT_DISTANCE.toString(),
			DBSelection.JARO_WINKLER.toString(), DBSelection.JACCARD.toString() };

	/** Indicates basic method. */
	public static final int MTD_FIRST = 0;

	/** Indicates edit_distance method. */
	public static final int MTD_EDIT_DISTANCE = 1;

	/** Indicates jaro method. */
	public static final int MTD_JARO_WINKLER = 2;

	/** Indicates jaccard method. */
	public static final int MTD_JACCARD = 3;
}
