package lod.importers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import lod.generators.BaseGenerator;
import lod.gui.tools.dialogs.SPARQLConfigurable;
import lod.gui.tools.dialogs.SPARQLConfigurator;
import lod.kernels.models.GraphHolder;
import lod.sparql.FileBasedQueryRunner;
import lod.sparql.SPARQLEndpointQueryRunner;

import org.data2semantics.mustard.kernels.data.RDFData;
import org.data2semantics.mustard.rdf.RDFDataSet;
import org.data2semantics.mustard.rdf.RDFFileDataSet;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.rio.RDFFormat;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
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
		int runnerType = Integer.parseInt(conf
				.getParameter(SPARQLConfigurator.RUNNER_TYPE));

		// dont read the whole file if we are using file
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

		// get the props with a given depth
		// ***SO FAR WE ONLY CONSIDER DEPTH 1****

		// generate the valuse String
		// final model that merges all graphs
		Model finalModel = null;// tmpModelIn.union(tmpModelOut);

		for (String str : inputInstances) {
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
				finalModel = tmpModelIn.union(tmpModelOut);
			} else {
				finalModel = finalModel.union(tmpModelIn);
				finalModel = finalModel.union(tmpModelOut);
			}
		}

		// write the model to tmp file
		File temp = File.createTempFile("tmpModel", ".rdf");
		// Delete temp file when program exits.
		temp.deleteOnExit();
		FileWriter out = new FileWriter(temp);
		try {
			finalModel.write(out, "RDF/XML-ABBREV");
		} finally {
			try {
				out.close();
			} catch (IOException closeException) {
				// ignore
			}
		}

		dataset = new RDFFileDataSet(temp.getAbsolutePath(), RDFFormat.RDFXML);

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

		data = new RDFData(dataset, instances, blackList);

		// labels.add(stmt2.getObject());

		// target = EvaluationUtils.createTarget(labels);

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

}
