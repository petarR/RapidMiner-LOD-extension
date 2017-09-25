package lod.sparql;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import lod.async.AsyncRunnerThread;
import lod.modelreader.threading.ModelReader;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.rapidminer.PluginInitLOD;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.ParameterService;

public class FileBasedQueryRunner extends SPARQLEndpointQueryRunner {

	private final int NO_REASONER = -1;
	private final int OWL_MICRO_REASONER = 1;
	private final int OWL_MINI_REASONER = 2;
	private final int OWL_REASONER = 3;
	private final int RDFS_REASONER = 4;

	private String fileName;

	private String schemaFileName;

	private int selectedReasoner;

	public String getFileName() {
		return fileName;
	}

	public String getSchemaFileName() {
		return schemaFileName;
	}

	public int getSelectedReasoner() {
		return selectedReasoner;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public void setSchemaFileName(String schemaFileName) {
		this.schemaFileName = schemaFileName;
	}

	public void setSelectedReasoner(int selectedReasoner) {
		this.selectedReasoner = selectedReasoner;
	}

	/**
	 * holds all models
	 */
	public static Map<String, FileBasedQueryRunner> cachedRunners = new HashMap<String, FileBasedQueryRunner>();

	Model model;
	Model defaultInstanceModel = ModelFactory.createDefaultModel();

	public Model getModel() {
		return model;
	}

	public Model getDefaultInstanceModel() {
		return defaultInstanceModel;
	}

	/**
	 * checks if the runner can be found in the cache. If not found, new runner
	 * will be created
	 * 
	 * @param filename
	 * @param schemaFilename
	 * @param reasonerIndex
	 * @param alias
	 * @return
	 */
	public static FileBasedQueryRunner getRunner(String filename,
			String schemaFilename, int reasonerIndex, String alias) {
		FileBasedQueryRunner runner = null;
		String key = alias + "\t" + filename + "\t" + schemaFilename + "\t"
				+ reasonerIndex;
		if (cachedRunners.containsKey(key)) {
			runner = cachedRunners.get(key);
			runner.mUIThreadRunning = true;
			return runner;
		}
		if (schemaFilename.equals("")) {
			try {
				runner = new FileBasedQueryRunner(filename, reasonerIndex);
			} catch (OperatorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				runner = new FileBasedQueryRunner(filename, schemaFilename,
						reasonerIndex);
			} catch (OperatorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		putRunnerInCache(key, runner);

		return runner;

	}

	private static void putRunnerInCache(String key, FileBasedQueryRunner runner) {

		int cacheSize = Integer
				.parseInt(ParameterService
						.getParameterValue(PluginInitLOD.PROPERTY_RMLOD_MODEL_CACHE_SIZE));
		if (cachedRunners.size() >= cacheSize && cachedRunners.size() > 0)
			cachedRunners.remove(cachedRunners.keySet().iterator().next());
		if (cacheSize > 0) {
			cachedRunners.put(key, runner);
		}

	}

	public FileBasedQueryRunner(String filename, String schemaFilename,
			int reasonerIndex) throws OperatorException {
		super(filename);
		this.fileName = filename;
		this.schemaFileName = schemaFilename;
		this.selectedReasoner = reasonerIndex;

		Model instanceModel = defaultInstanceModel;
		Model schemaModel = defaultInstanceModel;

		try {
			File file = new File(filename);
			// TODO threading
			ModelReader reader = new ModelReader(file.toURI().toString());
			if (reader != null)
				instanceModel = reader.readModel();
			if (!reader.mUIThreadRunning) // the process was interrupted by the
											// user. Set default model.
			{
				instanceModel = defaultInstanceModel;
				this.mUIThreadRunning = reader.mUIThreadRunning;
			}
		} catch (Exception e) {

			throw new OperatorException(
					"Problem with the SPARQL endpoint: no compatible file: "
							+ filename);
		}

		try {
			File file = new File(schemaFilename);
			ModelReader reader = new ModelReader(file.toURI().toString());
			if (reader != null)
				schemaModel = reader.readModel();
			if (!reader.mUIThreadRunning) // the process was interrupted by the
											// user. Set default model.
			{
				schemaModel = defaultInstanceModel;
				this.mUIThreadRunning = reader.mUIThreadRunning;
			}

		} catch (Exception e) {

			throw new OperatorException(
					"Problem with the SPARQL endpoint: no compatible file: "
							+ filename);
		}

		try {
			if (reasonerIndex != NO_REASONER && reasonerIndex != 0) {
				Reasoner reasoner = getReasonerByIndex(reasonerIndex);
				reasoner = reasoner.bindSchema(schemaModel);
				model = ModelFactory.createInfModel(reasoner, instanceModel);
			} else {
				model = ModelFactory
						.createRDFSModel(schemaModel, instanceModel);
			}
		} catch (NullPointerException e) {
			this.mUIThreadRunning = false;
			System.out.println("Null Pointer Exception: process interrupted");
		}
	}

	/**
	 * Used onlu in the kernels
	 * 
	 * @param fileName
	 */
	public FileBasedQueryRunner(String fileName) {
		this.fileName = fileName;
		this.schemaFileName = "";
		this.selectedReasoner = -1;
	}

	public FileBasedQueryRunner(String filename, int reasonerIndex)
			throws OperatorException {
		super(filename);

		this.fileName = filename;
		this.schemaFileName = "";
		this.selectedReasoner = reasonerIndex;

		Model instanceModel = ModelFactory.createDefaultModel();
		FileInputStream FIS;
		try {
			FIS = new FileInputStream(filename);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("File " + filename
					+ " does not exist.");
		}
		File file = new File(filename);
		ModelReader reader = new ModelReader(file.toURI().toString());
		if (reader != null)
			instanceModel = reader.readModel();
		if (!reader.mUIThreadRunning) // the process was interrupted by the
										// user. Set default model.
		{
			instanceModel = defaultInstanceModel;
			this.mUIThreadRunning = reader.mUIThreadRunning;
		}

		try {
			if (reasonerIndex != NO_REASONER && reasonerIndex != 0) {
				Reasoner reasoner = getReasonerByIndex(reasonerIndex);
				model = ModelFactory.createInfModel(reasoner, instanceModel);
			} else {
				model = instanceModel;
			}
		} catch (NullPointerException e) {
			this.mUIThreadRunning = false;
			System.out.println("Null Pointer Exception: process interrupted");
		}
	}

	private Reasoner getReasonerByIndex(int index) {
		if (index == OWL_MICRO_REASONER)
			return ReasonerRegistry.getOWLMicroReasoner();
		else if (index == OWL_MINI_REASONER)
			return ReasonerRegistry.getOWLMiniReasoner();
		else if (index == OWL_REASONER)
			return ReasonerRegistry.getOWLReasoner();
		else if (index == RDFS_REASONER)
			return ReasonerRegistry.getRDFSReasoner();
		return null;
	}

	@Override
	public Model runConstructQuery(String query) {
		// check in cash first
		Query q = QueryFactory.create(query);
		QueryExecution qexec = QueryExecutionFactory.create(q, model);

		return qexec.execConstruct();

	}

	@Override
	public ResultSet runSelectQuery(String query) {
		// check in cash first
		ResultSet results = getFromCache(query);
		if (results != null)
			return results;
		Query q = QueryFactory.create(query);
		QueryExecution qexec = QueryExecutionFactory.create(q, model);

		return qexec.execSelect();

	}

	@Override
	public boolean runAskQuery(String query) {
		Query q = QueryFactory.create(query);
		QueryExecution qexec = QueryExecutionFactory.create(q, model);
		return qexec.execAsk();
	}

	@Override
	public ResultSet runSelectQueryInterruptable(String query)
			throws OperatorException {
		// check in cash first
		ResultSet results = getFromCache(query);
		if (results != null)
			return results;

		this.mAsyncRunnerThread = new AsyncRunnerThread(this.getClass(),
				"runSelectQuery", new Class[] { String.class },
				new Object[] { query }, this);
		this.startAsyncRunner();
		this.enableWaiter();

		if (this.getAsyncOperationResult() instanceof String) {
			this.finalizeAsyncThread();
			throw new OperatorException((String) this.getAsyncOperationResult());
		}

		results = (ResultSet) this.getAsyncOperationResult();
		this.setAsyncOperationResultNull();

		return addToCache(query, results);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).append(fileName)
				.append(schemaFileName).append(usePropertyPaths).toHashCode();

	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof FileBasedQueryRunner))
			return false;

		FileBasedQueryRunner rhs = (FileBasedQueryRunner) obj;
		return new EqualsBuilder().append(fileName, rhs.fileName)
				.append(schemaFileName, rhs.schemaFileName)
				.append(selectedReasoner, rhs.selectedReasoner).isEquals();
	}
}
