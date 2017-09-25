package lod.sparql;

import java.util.HashMap;
import java.util.Map;

import lod.async.AsyncRunnerThread;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.rapidminer.PluginInitLOD6;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.ParameterService;

public class URLBasedQueryRunner extends SPARQLEndpointQueryRunner {

	String currentUri = "";
	Model model;

	public URLBasedQueryRunner(String url) {
		// this should not be like this
		super(url);
		this.currentUri = url;

		model = ModelFactory.createDefaultModel();
		if (url != null)
			model.read(url);
	}

	/**
	 * Updates the model with data from new URL
	 * 
	 * @param url
	 */
	@Override
	public void updateModel(String url) {
		try {
			this.currentUri = url;
			model = ModelFactory.createDefaultModel();
			// model.read(url);
		} catch (Exception e) {
		}
	}

	@Override
	public ResultSet runSelectQuery(String query) {
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

	// Running Select with no caching.
	@Override
	public ResultSet runSelectQueryInterruptable(String query)
			throws OperatorException {
		// check in cash first
		ResultSet results = getFromCache(query);
		if (results != null)
			return results;
		try {
			model.read(currentUri);
		} catch (Exception e) {
		}
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
						.getParameterValue(PluginInitLOD6.PROPERTY_RMLOD_QUERY_CACHE_SIZE));
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

	public static void main(String[] args) {
		URLBasedQueryRunner r = new URLBasedQueryRunner(
				"http://wifo5-03.informatik.uni-mannheim.de/eurostat/resource/regions/Bayern");
		ResultSet rs = r.runSelectQuery("Select * where {?s ?p ?o }");
		// try {
		// BufferedOutputStream bf = new BufferedOutputStream(
		// new FileOutputStream("mann.txt"));
		//
		// } catch (FileNotFoundException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// ResultSetFormatter.out(System.out, rs);
		// Writer writer = null;
		// try {
		// writer = new FileWriter("mannYA.ttl");
		// } catch (IOException e1) {
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }
		//
		while (rs.hasNext()) {
			try {
				QuerySolution solution = rs.next();
				System.out.println(rs.next().get("?p") + "\t "
						+ rs.next().get("?o"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// if (!solution.get("?o").isLiteral()
		// && !solution.get("?o").toString().contains("^^"))
		// writer.write("<http://yago-knowledge.org/resource/Mannheim>"
		// + "\t<"
		// + solution.get("?p")
		// + ">\t<"
		// + solution.get("?o") + "> .\n");
		// else {
		// writer.write("<http://yago-knowledge.org/resource/Mannheim>"
		// + "\t<"
		// + solution.get("?p")
		// + ">\t"
		// + solution.get("?o") + " .\n");
		// }
		// } catch (Exception e) {
		//
		// }
		// }
		// try {
		// writer.close();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

	}

	// @Override
	// public int hashCode() {
	// return new HashCodeBuilder(17, 31).append(currentUri).toHashCode();
	//
	// }
	//
	// @Override
	// public boolean equals(Object obj) {
	// if (obj == null)
	// return false;
	// if (obj == this)
	// return true;
	// if (!(obj instanceof URLBasedQueryRunner))
	// return false;
	//
	// URLBasedQueryRunner rhs = (URLBasedQueryRunner) obj;
	// return new EqualsBuilder().append(currentUri, rhs.currentUri)
	// .isEquals();
	// }
}
