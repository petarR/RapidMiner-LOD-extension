package lod.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lod.dataclasses.TreeNode;
import lod.sparql.SPARQLEndpointQueryRunner;
import lod.sparql.SPARQLQueryRunner.QuerryRunnerType;
import lod.sparql.URLBasedQueryRunner;

import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.config.ConfigurationException;

/**
 * Enables asyncronous retrieval of a set of sameAs links for a given link
 * 
 * @author Evgeny Mitichkin
 * 
 */
public class SameAsBunchExtractor {

	private static final String VARNAME = "x";
	private SPARQLEndpointQueryRunner queryRunner;
	private URLBasedQueryRunner urlBasedRunner;

	private Operator mCallingOperator;
	private String mSameAsConcept;
	private String[] mSearchPattern;

	/**
	 * link - variable in the query
	 */
	private Map<String, String> linksToFollow;

	public void setLinksToFollow(Map<String, String> linksToFollow) {
		this.linksToFollow = linksToFollow;
	}

	public SameAsBunchExtractor(Operator operator, String sameAsConcept,
			String[] searchPattern) {

		this.mCallingOperator = operator;
		this.mSameAsConcept = sameAsConcept;
		this.mSearchPattern = searchPattern;

		try {
			queryRunner = SPARQLEndpointQueryRunner.initRunner(
					mCallingOperator, queryRunner);
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OperatorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public List<TreeNode> getSameAsLinks(int mode) throws OperatorException {
		List<TreeNode> results = new ArrayList<TreeNode>();
		if (mode == 1) {
			if (queryRunner.getRunnerType() == QuerryRunnerType.URLBASED) {
				queryRunner.updateModel(mSameAsConcept);
			}
			if (queryRunner.mUIThreadRunning) {
				ResultSet linkingResult = queryRunner
						.runSelectQueryInterruptable(QueryFactory.create(
								coustructSPARQLQuery(mSameAsConcept))
								.toString());
				results = processResults(linkingResult, mSearchPattern);
			}
		} else {
			try {
				urlBasedRunner = new URLBasedQueryRunner(mSameAsConcept);
				ResultSet linkingResult = urlBasedRunner
						.runSelectQueryInterruptable(QueryFactory.create(
								coustructSPARQLQuery(mSameAsConcept))
								.toString());
				results = processResults(linkingResult, mSearchPattern);
			} catch (Exception e) {
			}
		}
		return results;
	}

	private List<TreeNode> processResults(ResultSet RS, String[] mSearchPattern) {
		List<TreeNode> results = new ArrayList<TreeNode>();
		if (queryRunner.mUIThreadRunning) {
			while (RS.hasNext()) {
				QuerySolution sol = RS.next();
				for (Entry<String, String> entry : linksToFollow.entrySet()) {
					if (!sol.contains(entry.getValue()))
						continue;
					String oneUrl = sol.get(entry.getValue()).toString();

					if (mSearchPattern != null && mSearchPattern.length != 0) {
						for (int i = 0; i < mSearchPattern.length; i++) {
							// filtering here
							// FIRSTLY find the exact match and stop
							if (oneUrl.equals(mSearchPattern[i])) {
								results.add(new TreeNode(oneUrl, -1,
										mSearchPattern[i]));
								break;
							} else {
								// IF NOT, check if the substring matches the
								// string
								if (oneUrl.contains(mSearchPattern[i])) {
									results.add(new TreeNode(oneUrl, -1,
											mSearchPattern[i]));
									break;
								}
							}
						}
					} else {
						String[] arr = oneUrl.split("/");
						String filterAsAHeader = arr[0]
								+ "/"
								+ "/"
								+ arr[2]
								+ "/"
								+ entry.getKey().replace("<", "")
										.replace(">", "");
						results.add(new TreeNode(oneUrl, -1, filterAsAHeader));
					}
				}
			}
		}
		return results;
	}

	// private String coustructSPARQLQuery(String conceptToLookup) {
	// String result =
	// "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> Prefix owl: <http://www.w3.org/2002/07/owl#> "
	// + "SELECT ?"
	// + VARNAME
	// + " "
	// + "WHERE {{?"
	// + VARNAME
	// + " owl:sameAs <"
	// + conceptToLookup
	// + ">} "
	// + "UNION {<"
	// + conceptToLookup
	// + "> owl:sameAs ?"
	// + VARNAME
	// + "} UNION {<"
	// + conceptToLookup + "> rdfs:seeAlso ?" + VARNAME + "}}";
	// return result;
	// }

	private String coustructSPARQLQuery(String conceptToLookup) {
		String query = "SELECT ";
		String vars = "";

		String optionals = "WHERE{";
		// add all variables
		for (Entry<String, String> entry : linksToFollow.entrySet()) {
			vars += "?" + entry.getValue() + " ";

			optionals += "{<" + conceptToLookup + "> " + entry.getKey() + " ?"
					+ entry.getValue() + "} UNION ";
		}
		optionals = optionals.replaceAll("UNION $", "}");

		query += vars + optionals;
		return query;
	}

	public void stop() {
		if (queryRunner != null) {
			queryRunner.mUIThreadRunning = false;
			queryRunner.finalizeAsyncThread();
		}
	}

	public String getmSameAsConcept() {
		return mSameAsConcept;
	}

	public void setmSameAsConcept(String mSameAsConcept) {
		this.mSameAsConcept = mSameAsConcept;
	}

	public SPARQLEndpointQueryRunner getQueryRunner() {
		return queryRunner;
	}

	public static void main(String[] args) {

	}
}
