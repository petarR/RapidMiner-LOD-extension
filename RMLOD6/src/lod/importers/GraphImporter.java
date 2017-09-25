package lod.importers;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lod.generators.BaseGenerator;
import lod.generators.RelationValueNumericFeatureGeneratorOperator;
import lod.gui.tools.dialogs.SPARQLConfigurable;
import lod.gui.tools.dialogs.SPARQLConfigurator;
import lod.kernels.models.GraphHolder;
import lod.sparql.FileBasedQueryRunner;
import lod.sparql.SPARQLEndpointQueryRunner;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.data2semantics.mustard.kernels.data.RDFData;
import org.data2semantics.mustard.rdf.DataSetUtils;
import org.data2semantics.mustard.rdf.RDFDataSet;
import org.data2semantics.mustard.rdf.RDFFileDataSet;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.rio.RDFFormat;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.util.FmtUtils;
import com.rapidminer.Process;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeText;
import com.rapidminer.parameter.TextType;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.config.ConfigurationException;
import com.rapidminer.tools.config.ConfigurationManager;
import com.rapidminer.tools.config.ParameterTypeConfigurable;

public class GraphImporter extends Operator {
	private static final Logger LOGGER = Logger.getLogger(Process.class
			.getName());
	// private static final String QUERY_OUTGOING_DEPTH1 =
	// "CONSTRUCT {?s ?p ?o} where {values ?s {VALUES}. ?s ?p ?o}";
	//
	// private static final String QUERY_INGOING_DEPTH1 =
	// "CONSTRUCT {?s ?p ?o} where {values ?s {VALUES}. ?p ?o ?s }";

	private static final String QUERY_OUTGOING_DEPTH1 = "CONSTRUCT {VALUE ?p ?o} where {VALUE ?p ?o}";

	private static final String QUERY_INGOING_DEPTH1 = "CONSTRUCT {?p ?o VALUE} where {?p ?o VALUE }";

	private static final String CLASS_NAME = "graph_import_operator";

	public static final String PARAMETER_QUERY = "SPARQL query";

	public static final String PARAMETER_SPARQL_MANAGER = "SPARQL connection";

	public static final String PARAMETER_DEPTH = "Graph depth";

	public static final String PARAMETER_BLACKLIST = "Properties to be ignored";
	public static final String PARAMETER_BLACKLIST_PROPS = "Property name";
	private InputPort mInputPort;

	private OutputPort mOutputPort;
	private OutputPort mOutputPortGraph;
	/**
	 * Input port containing metadata for the generators, i.e. which attributes
	 * should be used as a target
	 */
	protected InputPort mInputPortAddedAttrs;

	SPARQLEndpointQueryRunner queryRunner;

	/**
	 * the data to be delivered
	 */
	RDFData data;
	GraphHolder graphHolder;

	// temporary objects to populate the graph
	private static List<Resource> instances;
	private static List<Value> labels;
	private static List<Statement> blackList;
	private static List<String> blackListString;
	private static List<Double> target;
	private static RDFDataSet dataset;

	/**
	 * the input set
	 */
	private ExampleSet exampleSet;

	private List<String> inputInstances;
	protected ArrayList<String> attrNames = null;
	protected ArrayList<String> attrsBypsass = null;

	public GraphImporter(OperatorDescription description) {
		super(description);
		mInputPort = getInputPorts().createPort("Example Set");

		mInputPortAddedAttrs = getInputPorts()
				.createPort("Attributes Appended");

		mOutputPort = getOutputPorts().createPort("Example Set");
		mOutputPortGraph = getOutputPorts().createPort("Graph");

		getTransformer().addPassThroughRule(mInputPort, mOutputPort);

		getTransformer().addRule(
				new GenerateNewMDRule(mOutputPortGraph, GraphHolder.class));

		// mInputPort.addPrecondition(new SimplePrecondition(mInputPort,
		// new MetaData(ExampleSet.class)));
	}

	@Override
	public void doWork() throws OperatorException {
		super.doWork();
		init();

		try {
			populateGraph();

		} catch (Exception e) {
			e.printStackTrace();
			throw new UserError(this, 1008, CLASS_NAME, e.getMessage());
		}
		if (data == null)

		{
			LOGGER.info("---------->DATA");
			System.out.println("---------->DATA");
			throw new UserError(this, 1008, CLASS_NAME, "DATA");
		}
		if (data.getInstances() == null)

		{
			LOGGER.info("---------->INSTANCES");
			System.out.println("---------->INSTANCES");
			throw new UserError(this, 1008, CLASS_NAME, "instances");
		}

		if (data.getDataset() == null)

		{
			LOGGER.info("---------->DATASET");
			System.out.println("---------->DATASET");
			throw new UserError(this, 1008, CLASS_NAME, "DATASET");
		}

		deliver();

	}

	/**
	 * delivers the results
	 */
	private void deliver() {
		graphHolder.setGraphData(data);
		mOutputPort.deliver(exampleSet);
		mOutputPortGraph.deliver(graphHolder);
	}

	/**
	 * inits the variables
	 * 
	 * @throws OperatorException
	 */
	private void init() throws OperatorException {
		instances = new ArrayList<Resource>();
		blackList = new ArrayList<Statement>();
		graphHolder = new GraphHolder();
		data = null;
		inputInstances = null;

		String dialog = this
				.getParameter(BaseGenerator.PARAMETER_SPARQL_MANAGER);
		SPARQLConfigurable conf = null;
		try {
			conf = (SPARQLConfigurable) ConfigurationManager.getInstance()
					.lookup(SPARQLConfigurator.I18N_BASE_KEY, dialog,
							this.getProcess().getRepositoryAccessor());
		} catch (ConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// int runnerType = Integer.parseInt(conf
		// .getParameter(SPARQLConfigurator.RUNNER_TYPE));
		int runnerType = 0;
		try {
			runnerType = getRunnerType();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (runnerType == 0) {
			try {
				queryRunner = SPARQLEndpointQueryRunner.initRunner(this,
						queryRunner);
				// queryRunner.setCachedResults(cachedResults);
			} catch (Exception e) {
				e.printStackTrace();
				throw new UserError(this, 2001, CLASS_NAME, e.getMessage());
			}
		} else {
			queryRunner = new FileBasedQueryRunner(
					conf.getParameter(SPARQLConfigurator.LOCAL_ENDPOINT_FILE));
		}
		if (mInputPort.isConnected()) {
			exampleSet = mInputPort.getData(ExampleSet.class);
			attrNames = new ArrayList<String>();
			attrsBypsass = new ArrayList<String>();
			getAttsNames(attrNames, attrsBypsass);
		}

	}

	private int getRunnerType() throws Exception {
		String dialog = this
				.getParameter(BaseGenerator.PARAMETER_SPARQL_MANAGER);
		SPARQLConfigurable conf = (SPARQLConfigurable) ConfigurationManager
				.getInstance().lookup(SPARQLConfigurator.I18N_BASE_KEY, dialog,
						this.getProcess().getRepositoryAccessor());
		// int runType = conf.get
		return conf.getTypeOfRunner();

	}

	/**
	 * populates the graph
	 * 
	 * @throws Exception
	 */
	private void populateGraph() throws Exception {
		// if there is provided an example set
		if (attrsBypsass != null && attrsBypsass.size() > 0) {
			for (int i = 0; i < attrsBypsass.size(); i++) {
				inputInstances = new LinkedList<String>();
				for (Example ex : exampleSet)
					inputInstances.add(ex.getValueAsString(exampleSet
							.getAttributes().get(attrsBypsass.get(i))));

				if (queryRunner instanceof FileBasedQueryRunner) {
					populateGraphFromFile();
				} else if (queryRunner instanceof SPARQLEndpointQueryRunner) {
					populateGraphFromEndpoint();
				}
				/*****
				 * IT WORKS ONLY FOR ONE ATTRIBUTE
				 */
				break;
			}
		} else { // otherwise use the sparql query
			if (getParameterAsString(PARAMETER_QUERY) == null
					|| getParameterAsString(PARAMETER_QUERY).equals("")) {
				throw new UserError(
						this,
						1009,
						CLASS_NAME,
						"Please provide an input example set, or define a SPARQL query to retrieve instances!");

			}
			if (queryRunner instanceof FileBasedQueryRunner) {
				populateGraphFromFile();
			} else if (queryRunner instanceof SPARQLEndpointQueryRunner) {
				populateGraphFromEndpoint();
			}
		}

	}

	private void populateGraphFromEndpoint() throws Exception {
		// if no input set then use the query
		if (inputInstances == null || inputInstances.size() == 0) {
			inputInstances = new LinkedList<String>();
			ResultSet RS = queryRunner
					.runSelectQueryInterruptable(getParameterAsString(PARAMETER_QUERY));
			while (RS != null && RS.hasNext()) {
				QuerySolution sol = RS.next();
				String instanceName = sol.get("s").toString();
				if (!inputInstances.contains(instanceName))
					inputInstances.add(instanceName);
			}

		}

		// get the depth of the graph
		int depth = getParameterAsInt(PARAMETER_DEPTH);

		// final model that merges all graphs
		Model finalModel = null;// tmpModelIn.union(tmpModelOut);

		// used in the recursion
		List<String> toBeExplored = new ArrayList<String>();
		List<String> alreadyExplored = new ArrayList<String>();
		List<String> statements = new ArrayList<String>();

		getBlackListString();
		// finalModel = generateModelForInstances(inputInstances, finalModel,
		// toBeExplored, alreadyExplored);
		generateStatementsforInstances(inputInstances, statements,
				toBeExplored, alreadyExplored);

		// we already queried one hop
		depth -= 1;
		if (depth > 0) {

			// start with the recursion
			recurseGraphGenerator(statements, depth, toBeExplored,
					alreadyExplored);
		}

		// write the model to tmp file
		File temp = File.createTempFile("tmpModel", ".nt");
		// Delete temp file when program exits.
		temp.deleteOnExit();
		FileWriter out = new FileWriter(temp);

		FileUtils.writeLines(temp, statements);

		dataset = new RDFFileDataSet(temp.getAbsolutePath(), RDFFormat.NTRIPLES);

		// populate the instances
		List<Statement> stmts = dataset.getStatementsFromStrings(null, null,
				null);
		for (Statement st : stmts) {
			if (inputInstances.contains(st.getSubject().stringValue()))
				if (!instances.contains(st.getSubject()))
					instances.add(st.getSubject());
		}
		data = new RDFData(dataset, instances, blackList);

	}

	/**
	 * queries each instance
	 * 
	 * @param inputInstances
	 * @param statements
	 * @param toBeExplored
	 * @param alreadyExplored
	 * @throws OperatorException
	 */
	private void generateStatementsforInstances(List<String> inputInstances,
			List<String> statements, List<String> toBeExplored,
			List<String> alreadyExplored) throws OperatorException {

		for (String str : inputInstances) {
			alreadyExplored.add(str);
			if (toBeExplored.contains(str))
				toBeExplored.remove(str);
			String queryOUT = "SELECT DISTINCT ?p ?o " + "WHERE {<" + str
					+ "> ?p ?o .}";
			queryOUT = createFilteredQuery(queryOUT, "?p", blackListString);

			if (queryRunner.mUIThreadRunning) {
				ResultSet RS2 = queryRunner
						.runSelectQueryInterruptable(queryOUT.toString());
				while (RS2 != null && RS2.hasNext()) {
					QuerySolution sol = RS2.next();
					if (sol.get("p") == null)
						continue;
					if (sol.get("o") == null)
						continue;
					// remove non english
					if (!RelationValueNumericFeatureGeneratorOperator
							.isEnglish(sol.get("o")))
						continue;

					String statement = "<" + str + ">\t";
					String predicate = sol.get("p").toString();
					if (!checkBlackList(predicate)) {
						continue;
					}
					statement += "<" + predicate + ">\t";

					try {
						if (sol.get("o").isLiteral()) {

							String value = sol.get("o").asLiteral().getValue()
									.toString().replace("\n", "")
									.replace("\t", "").replace("\"", "")
									.replace("<", "").replace(">", "");
							value = StringEscapeUtils.escapeJava(value);
							value = FmtUtils.stringEsc(value);
							String type = sol.get("o").asLiteral()
									.getDatatypeURI();
							if (type == null || type.equals(""))
								type = "http://www.w3.org/2001/XMLSchema#string";
							statement += "\"" + value + "\"^^<" + type + ">\t.";
						} else {
							String object = sol.get("o").toString();
							if (!object.startsWith("http"))
								continue;
							statement += "<" + object + ">\t.";
							if (!alreadyExplored.contains(object)
									&& !toBeExplored.contains(object)) {
								toBeExplored.add(object);
							}

						}
					} catch (Exception e) {
						e.printStackTrace();
						continue;
					}
					statements.add(statement);
				}

			} else {

			}

			// String queryIN = "SELECT DISTINCT ?p ?s " + "WHERE {?s ?p <" +
			// str
			// + ">.}";
			//
			// queryIN = createFilteredQuery(queryIN, "?p", blackListString);
			//
			// if (queryRunner.mUIThreadRunning) {
			// ResultSet RS2 = queryRunner
			// .runSelectQueryInterruptable(queryIN.toString());
			// while (RS2 != null && RS2.hasNext()) {
			// QuerySolution sol = RS2.next();
			// if (sol.get("p") == null)
			// continue;
			// if (sol.get("s") == null)
			// continue;
			//
			// String predicate = sol.get("p").toString();
			// if (!checkBlackList(predicate)) {
			// continue;
			// }
			// String object = sol.get("s").toString();
			//
			// if (!alreadyExplored.contains(object)
			// && !toBeExplored.contains(object)) {
			// toBeExplored.add(object);
			// }
			//
			// String statement = "<" + object + ">\t"; //
			// statement += "<" + predicate + ">\t";
			// statement += "<" + str + ">\t.";
			// statements.add(statement);
			// }
			//
			// } else {
			//
			// }

		}

	}

	/**
	 * creates query with regexes
	 * 
	 * @param query
	 * @param var
	 * @param regexList
	 * @return
	 */
	public String createFilteredQuery(String query, String var,
			List<String> regexList) {
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
	 * NOT USED generates the model for a list of instances
	 * 
	 * @param inputInstances
	 * @param finalModel
	 * @param toBeExplored
	 * @param alreadyExplored
	 * @throws OperatorException
	 */
	private Model generateModelForInstances(List<String> inputInstances,
			Model finalModel, List<String> toBeExplored,
			List<String> alreadyExplored) throws OperatorException {

		for (String str : inputInstances) {
			alreadyExplored.add(str);
			if (toBeExplored.contains(str))
				toBeExplored.remove(str);

			String val = "<" + str + ">";
			if (str.startsWith("<") && str.endsWith(">")) {
				val = str;
			}

			Model tmpModelOut = queryRunner
					.runConstructQuery(QUERY_OUTGOING_DEPTH1.replace("VALUE",
							val));
			Model tmpModelIn = queryRunner
					.runConstructQuery(QUERY_INGOING_DEPTH1.replace("VALUE",
							val));
			if (finalModel == null) {
				if (tmpModelOut != null && tmpModelIn != null)
					finalModel = tmpModelIn.union(tmpModelOut);

			} else {
				if (tmpModelIn != null)
					finalModel = finalModel.union(tmpModelIn);
				if (tmpModelOut != null)
					finalModel = finalModel.union(tmpModelOut);
			}

		}

		return finalModel;
	}

	/**
	 * NOT USED gets all object instances from the graph
	 * 
	 * @param finalModel
	 * @param toBeExplored
	 * @param alreadyExplored
	 */
	private void queryNewInstances(Model finalModel, List<String> toBeExplored,
			List<String> alreadyExplored) {

		StmtIterator iter = finalModel.listStatements();
		List<com.hp.hpl.jena.rdf.model.Statement> toRemoveStatement = new ArrayList<com.hp.hpl.jena.rdf.model.Statement>();

		while (iter.hasNext()) {
			com.hp.hpl.jena.rdf.model.Statement stmt = iter.nextStatement(); // get
																				// next
			// statement
			com.hp.hpl.jena.rdf.model.Resource subjectR = stmt.getSubject(); // get
																				// the
																				// subject

			Property predicate = stmt.getPredicate();

			if (!checkBlackList(predicate.toString())) {
				toRemoveStatement.add(stmt);
				continue;
			}

			String subject = subjectR.toString();
			if (!alreadyExplored.contains(subject)
					&& !toBeExplored.contains(subject)) {
				toBeExplored.add(subject);
			}

			RDFNode objectR = (RDFNode) stmt.getObject(); // get the object

			if (objectR instanceof Resource) {
				String object = objectR.toString();
				if (!alreadyExplored.contains(object)
						&& !toBeExplored.contains(subject)) {
					toBeExplored.add(object);
				}

			}
		}

		// remove all blacklist statements

		finalModel.remove(toRemoveStatement);

	}

	/**
	 * checks if the current predicate is allowed
	 * 
	 * @param string
	 * @return
	 */
	private boolean checkBlackList(String predicate) {
		for (String str : blackListString) {

			// Create a Pattern object
			Pattern r = Pattern.compile(str);

			// Now create matcher object.
			Matcher m = r.matcher(predicate);
			if (m.find()) {
				return false;
			}
		}

		return true;
	}

	/**
	 * it generates the graph from an endpoint recursevly
	 * 
	 * @param finalModel
	 * @param depth
	 * @param toBeExplored
	 * @param alreadyExplored
	 * @throws OperatorException
	 */
	private void recurseGraphGenerator(List<String> statements, int depth,
			List<String> toBeExplored, List<String> alreadyExplored)
			throws OperatorException {
		if (depth > 0) {

			// get new instances
			// queryNewInstances(finalModel, toBeExplored, alreadyExplored);
			//
			List<String> b = new ArrayList(toBeExplored);
			generateStatementsforInstances(b, statements, toBeExplored,
					alreadyExplored);

			// generateModelForInstances(b, finalModel, toBeExplored,
			// alreadyExplored);

			recurseGraphGenerator(statements, depth - 1, toBeExplored,
					alreadyExplored);
		}

	}

	private String generateValuesString(List<String> inputInstances) {
		String values = "";
		for (String str : inputInstances) {
			String val = "<" + str + ">";
			if (str.startsWith("<") && str.endsWith(">")) {
				val = str;
			}
			values += val + " ";
		}
		return values;
	}

	/**
	 * populates the graph from a file
	 * 
	 * @throws Exception
	 */
	// curently reads the complete file
	private void populateGraphFromFile() throws Exception {

		// get the complete model from the file
		// Model model = ((FileBasedQueryRunner) queryRunner).getModel();

		// ****convert the model to the mustard data model

		// write the model to tmp file
		// Create temp file.
		// File temp = File.createTempFile("tmpModel", ".rdf");
		//
		// // Delete temp file when program exits.
		// temp.deleteOnExit();
		// FileWriter out = new FileWriter(temp);
		// try {
		// model.write(out, "RDF/XML-ABBREV");
		// } finally {
		// try {
		// out.close();
		// } catch (IOException closeException) {
		// // ignore
		// }
		// }
		LOGGER.info("---------->BEFORE READING");
		String fileName = ((FileBasedQueryRunner) queryRunner).getFileName();
		if (fileName.endsWith(".nt"))
			dataset = new RDFFileDataSet(fileName, RDFFormat.NTRIPLES);
		else if (fileName.endsWith(".n3"))
			dataset = new RDFFileDataSet(fileName, RDFFormat.N3);
		else if (fileName.endsWith(".ttl"))
			dataset = new RDFFileDataSet(fileName, RDFFormat.TURTLE);
		else
			dataset = new RDFFileDataSet(fileName, RDFFormat.RDFXML);
		System.out.println("DONE READING");
		LOGGER.info("---------->DONE READING");

		// if there is no input exampleSet, use the sparql query
		if (inputInstances == null || inputInstances.size() == 0) {
			try {
				queryRunner = SPARQLEndpointQueryRunner.initRunner(this,
						queryRunner);
				// queryRunner.setCachedResults(cachedResults);
			} catch (Exception e) {
				e.printStackTrace();
				throw new UserError(this, 2001, CLASS_NAME, e.getMessage());
			}
			inputInstances = new LinkedList<String>();
			// get only the statements that we need by the query
			ResultSet RS = queryRunner
					.runSelectQueryInterruptable(getParameterAsString(PARAMETER_QUERY));
			while (RS != null && RS.hasNext()) {
				QuerySolution sol = RS.next();
				String instanceName = sol.get("s").toString();
				if (!inputInstances.contains(instanceName))
					inputInstances.add(instanceName);
			}
		}

		for (String inputIns : inputInstances) {
			List<Statement> stmts = dataset.getStatementsFromStrings(inputIns,
					null, null);
			if (stmts.size() > 0)
				instances.add(stmts.get(0).getSubject());
		}

		// for (Statement st : stmts) {
		// if (inputInstances.contains(st.getSubject().stringValue()))
		// if (!instances.contains(st.getSubject()))
		// instances.add(st.getSubject());
		//
		// }
		LOGGER.info("---------->DONE STATEMENTS");

		getBlackList();

		data = new RDFData(dataset, instances, blackList);

		// labels.add(stmt2.getObject());

		// target = EvaluationUtils.createTarget(labels);

	}

	public void getBlackList() throws UndefinedParameterError {
		List<Resource> instances = new ArrayList<Resource>();
		List<Value> labels = new ArrayList<Value>();

		if (isParameterSet(PARAMETER_BLACKLIST)) {
			String[] macroNames = ParameterTypeEnumeration
					.transformString2Enumeration(getParameterAsString(PARAMETER_BLACKLIST));

			for (String macroName : macroNames) {
				List<Statement> stmts = dataset.getStatementsFromStrings(null,
						macroName, null);
				for (Statement stmt : stmts) {
					instances.add(stmt.getSubject());
					labels.add(stmt.getObject());
				}
			}
			blackList = DataSetUtils
					.createBlacklist(dataset, instances, labels);
		}
	}

	private void getBlackListString() throws UndefinedParameterError {
		blackListString = new ArrayList<String>();

		if (isParameterSet(PARAMETER_BLACKLIST)) {
			String[] macroNames = ParameterTypeEnumeration
					.transformString2Enumeration(getParameterAsString(PARAMETER_BLACKLIST));

			for (String macroName : macroNames) {
				blackListString.add("!" + macroName);
			}

		}

	}

	@Override
	public List<ParameterType> getParameterTypes() {

		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeConfigurable(PARAMETER_SPARQL_MANAGER,
				"Choose SPARQL endpoint connection",
				SPARQLConfigurator.I18N_BASE_KEY));
		types.add(new ParameterTypeEnumeration(
				BaseGenerator.PARAMETER_ATTRIBUTE_TO_EXTEND,
				"Attributes that contain LOD links", new ParameterTypeString(
						BaseGenerator.PARAMETER_ATTRIBUTE_TO_EXTEND_NAME,
						"Attribute name"), false));
		types.add(new ParameterTypeText(PARAMETER_QUERY, "The SPARQL Query.",
				TextType.SQL, false));

		types.add(new ParameterTypeInt(
				PARAMETER_DEPTH,
				"The depth of the subgraph for each instance. (if 0, the complete graph will be retrieved)",
				0, 5, 0, false));

		types.add(new ParameterTypeEnumeration(PARAMETER_BLACKLIST,
				"Properties to be ignored", new ParameterTypeString(
						PARAMETER_BLACKLIST_PROPS, "Property"), false));

		return types;
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
						BaseGenerator.NEW_ATTRIBUTES)));// Endpoints
				attsBypass.add(ex.getValueAsString(set.getAttributes().get(
						BaseGenerator.BYPASSING_ATTRIBUTES)));
			}
		} else {
			boolean isParamFound = true;

			if (isParameterSet(BaseGenerator.PARAMETER_ATTRIBUTE_TO_EXTEND)) {
				String[] macroNames = ParameterTypeEnumeration
						.transformString2Enumeration(getParameterAsString(BaseGenerator.PARAMETER_ATTRIBUTE_TO_EXTEND));
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

	public static void main(String[] args) {

		String value = "Bretagne/ччччч";

		System.out.println(value);
		value = StringEscapeUtils.escapeJava(value);
		System.out.println(value);
		value = FmtUtils.stringEsc(value);
		System.out.println(value);
	}

}
