package lod.sparql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lod.async.AsyncRunner;
import lod.async.AsyncRunnerThread;
import lod.generators.BaseGenerator;
import lod.gui.tools.dialogs.SPARQLConfigurable;
import lod.gui.tools.dialogs.SPARQLConfigurator;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.rapidminer.PluginInitLOD;
import com.rapidminer.RapidMiner;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.config.ConfigurationException;
import com.rapidminer.tools.config.ConfigurationManager;

public class SPARQLEndpointQueryRunner extends AsyncRunner implements
		SPARQLQueryRunner {// Extend this with AsyncRunner

	private static final String CLASS_NAME = "sparql_endpoint_query_runner";

	public static final String GET_SUPERCLASSES_QUERY = "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> select ?superclass where { ?class rdfs:subClassOf* ?superclass}";

	public static final String GET_DIRECT_SUPERCLASSES_QUERY = "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> select ?superclass where { ?class rdfs:subClassOf ?superclass}";

	public static final String GET_SUBCLASSES_QUERY = "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> select ?subclass where { ?subclass rdfs:subClassOf* ?class  } OPTION (transitive, t_distinct, t_max(1))";

	public static final String GET_SUPER_PROPERTIES = "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> select ?superclass where { ?class rdfs:subPropertyOf* ?superclass}";

	public static final String GET_DIRECT_SUPER_PROPERTIES = "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> select ?superclass where { ?class rdfs:subPropertyOf ?superclass}";

	public static final String GET_RELATION_IN_HIERARCHY = "SELECT ?subject WHERE {?classUri ?hierarchyrelation ?subject OPTION (TRANSITIVE, T_DISTINCT,T_MAX(?NM))}";

	public static final String GET_RELATION_IN_COMPLETE_HIERARCHY = "SELECT ?subject WHERE {?classUri ?hierarchyrelation ?subject OPTION (TRANSITIVE, T_DISTINCT,T_MIN(0))}";

	private QuerryRunnerType runnerType;

	protected String endpoint;

	protected String alias;

	protected int timeout;

	protected int retries;

	protected int pageSize;

	protected boolean useCount;

	protected boolean usePropertyPaths;

	public static Map<String, Map<SPARQLEndpointQueryRunner, ResultSet>> cachedResults = new HashMap<String, Map<SPARQLEndpointQueryRunner, ResultSet>>();

	// public static Map<String, Map<SPARQLEndpointQueryRunner, ResultSet>>
	// getCachedResults() {
	// return cachedResults;
	// }
	//
	// public static void setCachedResults(
	// Map<String, Map<SPARQLEndpointQueryRunner, ResultSet>> cachedResults) {
	// cachedResults = cachedResults;
	// }

	public QuerryRunnerType getRunnerType() {
		return runnerType;
	}

	public void setRunnerType(QuerryRunnerType runnerType) {
		this.runnerType = runnerType;
	}

	public boolean isUseCount() {
		return useCount;
	}

	public void setUseCount(boolean useCount) {
		this.useCount = useCount;
	}

	public boolean isUsePropertyPaths() {
		return usePropertyPaths;
	}

	public void setUsePropertyPaths(boolean usePropertyPaths) {
		this.usePropertyPaths = usePropertyPaths;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getRetries() {
		return retries;
	}

	public void setRetries(int retries) {
		this.retries = retries;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public SPARQLEndpointQueryRunner(String endpoint, String alias,
			int timeout, int retries, int pageSize, boolean useCount,
			boolean usePropertyPaths) {
		super();
		this.endpoint = endpoint;
		this.alias = alias;
		this.timeout = timeout;
		this.retries = retries;
		this.pageSize = pageSize;
		this.useCount = useCount;
		this.usePropertyPaths = usePropertyPaths;
		// cachedResults = new HashMap<String, Map<SPARQLEndpointQueryRunner,
		// ResultSet>>();
	}

	public SPARQLEndpointQueryRunner(String endpoint, int timeout, int retries) {
		super();
		this.endpoint = endpoint;
		this.timeout = timeout;
		this.retries = retries;
		// cachedResults = new HashMap<String, Map<SPARQLEndpointQueryRunner,
		// ResultSet>>();
	}

	public SPARQLEndpointQueryRunner(String endpoint) {
		super();
		this.endpoint = endpoint;
		this.timeout = 60 * 1000;
		this.retries = 10;
		this.pageSize = 0;
		this.useCount = true;
		this.usePropertyPaths = false;
		this.endpoint = "";
		this.alias = "";
		// cachedResults = new HashMap<String, Map<SPARQLEndpointQueryRunner,
		// ResultSet>>();
	}

	public SPARQLEndpointQueryRunner() {
		this.endpoint = endpoint;
		this.alias = alias;
		this.timeout = timeout;
		this.retries = retries;
		this.pageSize = pageSize;
		this.useCount = useCount;
		this.usePropertyPaths = usePropertyPaths;
	}

	public boolean runAskQuery(String query) throws OperatorException {
		boolean response = false;
		QueryExecution qexec = null;
		try {
			Query q = QueryFactory.create(query);
			qexec = QueryExecutionFactory.sparqlService(endpoint, q);
			qexec.setTimeout(timeout);
		} catch (Exception e) {
			return false;
		}

		// retry every 100 millis if the endpoint goes down
		int localRetries = 0;

		boolean shouldThrowExc = false;
		while (true) {
			try {
				// TODO debug and catch node not available
				response = qexec.execAsk();
				break;
			} catch (Exception ex) {
				localRetries++;
				if (localRetries >= retries) {
					ex.printStackTrace();
					shouldThrowExc = true;
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		}
		if (shouldThrowExc)
			throw new OperatorException(
					"Problem with the SPARQL endpoint: timeout");
		return response;
	}

	public Model runConstructQuery(String query) throws OperatorException {

		Model model = null;
		Query q = QueryFactory.create(query);
		QueryExecution objectToExec = QueryExecutionFactory.sparqlService(
				endpoint, q.toString());
		objectToExec.setTimeout(timeout);
		// retry every 1000 millis if the endpoint goes down
		int localRetries = 0;
		boolean shouldThrowExc = false;

		while (true) {
			try {
				model = objectToExec.execConstruct();
				break;
			} catch (Exception ex) {
				if (!ex.getMessage().contains("Read timed out"))
					throw new OperatorException(
							"Problem with the SPARQL endpoint: "
									+ ex.getLocalizedMessage());
				System.out.println(ex.getClass());
				localRetries++;
				if (localRetries >= retries) {
					ex.printStackTrace();
					shouldThrowExc = true;
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		}
		if (shouldThrowExc)
			throw new OperatorException(
					"Problem with the SPARQL endpoint: timeout");

		objectToExec.close();

		return model;

	}

	public ResultSet runSelectQuery(String query) throws OperatorException {

		// check in cash first
		ResultSet results = getFromCache(query);
		if (results != null)
			return results;

		Query q = QueryFactory.create(query);
		QueryExecution objectToExec = QueryExecutionFactory.sparqlService(
				endpoint, q.toString());
		objectToExec.setTimeout(timeout);
		// retry every 1000 millis if the endpoint goes down
		int localRetries = 0;
		boolean shouldThrowExc = false;

		while (true) {
			try {
				results = objectToExec.execSelect();
				break;
			} catch (Exception ex) {
				if (!ex.getMessage().contains("Read timed out"))
					throw new OperatorException(
							"Problem with the SPARQL endpoint: "
									+ ex.getLocalizedMessage());
				System.out.println(ex.getClass());
				localRetries++;
				if (localRetries >= retries) {
					ex.printStackTrace();
					shouldThrowExc = true;
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		}
		if (shouldThrowExc)
			throw new OperatorException(
					"Problem with the SPARQL endpoint: timeout");

		return addToCache(query, results);

	}

	public ResultSet runSelectQueryInterruptable(String query)
			throws OperatorException {

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
		ResultSet resultsToCache = null;
		ResultSet resultsToReturn = null;

		try {
			resultsToCache = ResultSetFactory.copyResults(results);
			resultsToReturn = ResultSetFactory.copyResults(resultsToCache);
		} catch (NullPointerException ex) {
		}

		this.setAsyncOperationResultNull();

		return addToCache(query, results);

	}

	/**
	 * returns results from the cache (if exist)
	 * 
	 * @param query
	 * @return
	 */
	public ResultSet getFromCache(String query) {
		if (cachedResults.containsKey(query))
			if (cachedResults.get(query).containsKey(this))
				return ResultSetFactory.copyResults(cachedResults.get(query)
						.get(this));
		return null;
	}

	/**
	 * adds new entry to the cache based on the cache size limit
	 * 
	 * @param query
	 * @param newEntry
	 */
	protected ResultSet addToCache(String query, ResultSet results) {
		// put results in cash
		ResultSet resultsToCache = ResultSetFactory.copyResults(results);
		ResultSet resultsToReturn = ResultSetFactory
				.copyResults(resultsToCache);
		int cacheSize = Integer
				.parseInt(ParameterService
						.getParameterValue(PluginInitLOD.PROPERTY_RMLOD_QUERY_CACHE_SIZE));
		if (cachedResults.size() >= cacheSize && cachedResults.size() > 0)
			cachedResults.remove(cachedResults.keySet().iterator().next());
		if (cacheSize > 0) {

			Map<SPARQLEndpointQueryRunner, ResultSet> newEntry = new HashMap<SPARQLEndpointQueryRunner, ResultSet>();
			if (cachedResults.containsKey(query))
				newEntry = cachedResults.get(query);
			newEntry.put(this, resultsToCache);

			cachedResults.put(query, newEntry);
		}
		return resultsToReturn;
	}

	public Boolean runAskQueryInterruptable(String query)
			throws OperatorException {
		Boolean result = null;
		this.mAsyncRunnerThread = new AsyncRunnerThread(this.getClass(),
				"runAskQuery", new Class[] { String.class },
				new Object[] { query }, this);
		this.startAsyncRunner();
		this.enableWaiter();

		if (this.getAsyncOperationResult() instanceof String) {
			this.finalizeAsyncThread();
			throw new OperatorException((String) this.getAsyncOperationResult());
		}

		result = (Boolean) this.getAsyncOperationResult();
		this.setAsyncOperationResultNull();

		return result;
	}

	public static Query addOrderByToQuery(String queryStr) {
		Query querQ = QueryFactory.create(queryStr);
		for (String str : querQ.getResultVars()) {
			querQ.addOrderBy(str, 0);
		}
		// remove the prefixes from the subquery
		String prefixes = "";
		String noPrefixQuery = querQ.toString();
		if (querQ.toString().toLowerCase().contains("select")) {
			prefixes = querQ.toString().substring(0,
					querQ.toString().toLowerCase().indexOf("select"));
			if (prefixes.toLowerCase().contains("prefix")) {
				noPrefixQuery = querQ.toString().replace(prefixes, "");

			}
		}
		// add the subquery
		String outsideQuery = "SELECT";
		for (String str : querQ.getResultVars()) {
			outsideQuery += " ?" + str;
		}
		String finalQuery = outsideQuery + " WHERE { {" + noPrefixQuery + "} }";

		querQ = QueryFactory.create(prefixes + finalQuery);
		return querQ;
	}

	public static SPARQLEndpointQueryRunner initRunner(Operator operator,
			SPARQLEndpointQueryRunner currentRunner)
			throws ConfigurationException, OperatorException {

		SPARQLEndpointQueryRunner queryRunner = null;

		if (!operator
				.getParameterAsBoolean(BaseGenerator.PARAMETER_RESOLVE_BY_URI)) {
			String dialog = operator
					.getParameter(BaseGenerator.PARAMETER_SPARQL_MANAGER);

			SPARQLConfigurable conf = (SPARQLConfigurable) ConfigurationManager
					.getInstance().lookup(SPARQLConfigurator.I18N_BASE_KEY,
							dialog,
							operator.getProcess().getRepositoryAccessor());
			queryRunner = conf.getTheRunnerFromDialog(currentRunner);
		} else {
			queryRunner = new URLBasedQueryRunner(null);
			queryRunner.setRunnerType(QuerryRunnerType.URLBASED);
		}

		setProxyParams();

		return queryRunner;
	}

	/**
	 * sets the proxy to the one specified in Tools -> Preferences http proxy
	 */
	private static void setProxyParams() {

		setProxyParameter(RapidMiner.PROPERTY_RAPIDMINER_HTTP_PROXY_SET);
		setProxyParameter(RapidMiner.PROPERTY_RAPIDMINER_HTTP_PROXY_HOST);
		setProxyParameter(RapidMiner.PROPERTY_RAPIDMINER_HTTP_PROXY_PORT);
		setProxyParameter(RapidMiner.PROPERTY_RAPIDMINER_HTTP_PROXY_USERNAME);
		setProxyParameter(RapidMiner.PROPERTY_RAPIDMINER_HTTP_PROXY_PASSWORD);
		setProxyParameter(RapidMiner.PROPERTY_RAPIDMINER_HTTP_PROXY_NON_PROXY_HOSTS);

	}

	private static void setProxyParameter(String param) {
		String paramValue = ParameterService.getParameterValue(param);
		if (paramValue != null && !paramValue.equals("")) {
			System.setProperty(param, paramValue);
		}

	}

	public List<String> getSuperClasses(String classUri, List<String> allTypes,
			String query) throws OperatorException {
		List<String> superClasses = new ArrayList<String>();
		ParameterizedSparqlString queryString = new ParameterizedSparqlString(
				query);
		queryString.setIri("?class", classUri);
		ResultSet results = runSelectQuery(queryString.toString());
		while (results.hasNext()) {
			QuerySolution solution = results.next();
			String superClass = solution.get("superclass").toString();
			if (!superClasses.contains(superClass)
					&& allTypes.contains(superClass))
				superClasses.add(superClass);
		}
		if (superClasses.contains(classUri))
			superClasses.remove(classUri);
		return superClasses;
	}

	public List<String> getSuperRelations(String classUri,
			List<String> allTypes, String hierarchyRelation, int maxDepth)
			throws OperatorException {
		List<String> superClasses = new ArrayList<String>();
		ParameterizedSparqlString queryString = null;
		if (maxDepth == 0) {
			queryString = new ParameterizedSparqlString(
					GET_RELATION_IN_COMPLETE_HIERARCHY);
			queryString.setIri("?classUri", classUri);
			queryString.setIri("?hierarchyrelation", hierarchyRelation);
		} else {
			queryString = new ParameterizedSparqlString(
					GET_RELATION_IN_HIERARCHY);
			queryString.setIri("?classUri", classUri);
			queryString.setIri("?hierarchyrelation", hierarchyRelation);
			queryString = new ParameterizedSparqlString(queryString.toString()
					.replace("?NM", Integer.toString(maxDepth)));
		}
		ResultSet results = executeNonStandardQuery(queryString.toString());
		while (results.hasNext()) {
			QuerySolution solution = results.next();
			String superClass = solution.get("subject").toString();
			if (!superClasses.contains(superClass)
					&& allTypes.contains(superClass))
				superClasses.add(superClass);
		}
		if (superClasses.contains(classUri))
			superClasses.remove(classUri);
		return superClasses;
	}

	public List<String> getSubClassesWithSPARQL(String classUri,
			List<String> allTypes) throws OperatorException {
		List<String> superClasses = new ArrayList<String>();
		ParameterizedSparqlString queryString = new ParameterizedSparqlString(
				GET_SUBCLASSES_QUERY);
		queryString.setIri("?class", classUri);
		ResultSet results = runSelectQuery(queryString.toString());
		while (results.hasNext()) {
			QuerySolution solution = results.next();
			String superClass = solution.get("subclass").toString();
			if (!superClasses.contains(superClass)
					&& allTypes.contains(superClass))
				superClasses.add(superClass);
		}
		if (superClasses.contains(classUri))
			superClasses.remove(classUri);
		return superClasses;
	}

	public static List<String> getSubClasses(String classUri,
			List<String> allTypes, Map<String, List<String>> superClassses) {
		List<String> subClasses = new ArrayList<String>();

		for (Entry entry : superClassses.entrySet()) {
			List<String> superClassesList = (List<String>) entry.getValue();
			if (superClassesList.contains(classUri))
				subClasses.add((String) entry.getKey());
		}
		return subClasses;
	}

	public static void main(String[] args) throws OperatorException {
		// TODO Auto-generated method stub
		SPARQLEndpointQueryRunner qr = new SPARQLEndpointQueryRunner(
				"http://dbpedia.org/sparql");
		// qr.getSubClasses("http://dbpedia.org/class/yago/Object100002684",
		// new ArrayList<String>());
	}

	public String setOffsetAndLimit(String queryQStr, int offset, int pageSize2) {
		if (queryQStr.contains("OFFSET")) {
			queryQStr = queryQStr.substring(0, queryQStr.indexOf(" OFFSET"));
		}
		queryQStr += " OFFSET " + offset + " LIMIT " + pageSize2;
		return queryQStr;
	}

	public ResultSet executeNonStandardQuery(String querystring)
			throws OperatorException {
		int localRetries = 0;
		boolean shouldThrowExc = false;
		QueryExecution qexec = null;
		ResultSet results = null;
		qexec = new QueryEngineHTTP(endpoint, querystring);
		qexec.setTimeout(timeout);

		while (true) {
			try {
				results = qexec.execSelect();
				break;
			} catch (Exception ex) {
				if (!ex.getMessage().contains("Read timed out"))
					throw new OperatorException(
							"Problem with the SPARQL endpoint: "
									+ ex.getLocalizedMessage());
				System.out.println(ex.getClass());
				localRetries++;
				if (localRetries >= retries) {
					ex.printStackTrace();
					shouldThrowExc = true;
					break;
				}
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
				}
			}
		}
		if (shouldThrowExc)
			throw new OperatorException(
					"Problem with the SPARQL endpoint: timeout");
		return results;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).append(endpoint).append(timeout)
				.append(retries).append(pageSize).append(useCount)
				.append(usePropertyPaths).toHashCode();

	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof SPARQLEndpointQueryRunner))
			return false;

		SPARQLEndpointQueryRunner rhs = (SPARQLEndpointQueryRunner) obj;
		return new EqualsBuilder().append(endpoint, rhs.endpoint)
				.append(timeout, rhs.timeout).append(retries, rhs.retries)
				.append(pageSize, rhs.pageSize).append(useCount, rhs.useCount)
				.append(usePropertyPaths, rhs.usePropertyPaths).isEquals();
	}

	/**
	 * it is only used in the URLbasedQueryRUnner and should be used in
	 * FileBasedQueryRunner
	 * 
	 * @param url
	 */
	public void updateModel(String url) {
	}
}