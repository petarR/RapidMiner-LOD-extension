package lod.generators.smart.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lod.generators.smart.model.Instance;
import lod.sparql.SPARQLEndpointQueryRunner;
import lod.sparql.SPARQLQueryRunner.QuerryRunnerType;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.UndefinedParameterError;

public class QueryRunner {
	public static final String OUT_1 = "SELECT * WHERE { <$ENTITY$> ?pf1 ?value1 .  FILTER (  STRSTARTS(STR(?pf1), \"http://dbpedia.org/ontology/\") || ( str(?pf1) = \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\" )  || ( str(?pf1) = \"http://purl.org/dc/terms/subject\" ) )}";

	public static final String OUT_2 = "SELECT * WHERE { <$ENTITY$> ?pf1 ?value1 . ?value1 ?pf2 ?value2. FILTER (?value2 != <$ENTITY$>).  FILTER (  STRSTARTS(STR(?pf1), \"http://dbpedia.org/ontology/\") || ( str(?pf1) = \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\" )  || ( str(?pf1) = \"http://purl.org/dc/terms/subject\" ) ) .  FILTER (  STRSTARTS(STR(?pf2), \"http://dbpedia.org/ontology/\") || ( str(?pf2) = \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\" )  || ( str(?pf2) = \"http://purl.org/dc/terms/subject\" ) )} ";

	public static final String OUT_3 = "SELECT * WHERE { <$ENTITY$> ?pf1 ?value1 . ?value1 ?pf2 ?value2 . ?value2 ?pf3 ?value3. FILTER ((?value2 != <$ENTITY$>) &&  (?value3 != <$ENTITY$>)). FILTER (  STRSTARTS(STR(?pf1), \"http://dbpedia.org/ontology/\") || ( str(?pf1) = \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\" )  || ( str(?pf1) = \"http://purl.org/dc/terms/subject\" ) ) .  FILTER (  STRSTARTS(STR(?pf2), \"http://dbpedia.org/ontology/\") || ( str(?pf2) = \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\" )  || ( str(?pf2) = \"http://purl.org/dc/terms/subject\" ) ).  FILTER (  STRSTARTS(STR(?pf3), \"http://dbpedia.org/ontology/\") || ( str(?pf3) = \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\" )  || ( str(?pf3) = \"http://purl.org/dc/terms/subject\" ) )} ";

	public static final String IN_1 = "SELECT * WHERE { ?value1  ?pf1  <$ENTITY$>.  FILTER (  STRSTARTS(STR(?pf1), \"http://dbpedia.org/ontology/\") || ( str(?pf1) = \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\" )  || ( str(?pf1) = \"http://purl.org/dc/terms/subject\" ) )} ";

	public static final String IN_2 = "SELECT * WHERE { ?value1  ?pf1 ?value2. ?value2 ?pf2  <$ENTITY$> . FILTER (?value1 != <$ENTITY$>).  FILTER (  STRSTARTS(STR(?pf1), \"http://dbpedia.org/ontology/\") || ( str(?pf1) = \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\" )  || ( str(?pf1) = \"http://purl.org/dc/terms/subject\" ) ) .  FILTER (  STRSTARTS(STR(?pf2), \"http://dbpedia.org/ontology/\") || ( str(?pf2) = \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\" )  || ( str(?pf2) = \"http://purl.org/dc/terms/subject\" ) )} ";

	public static final String IN_3 = "SELECT * WHERE { ?value1  ?pf1 ?value2. ?value2 ?pf2 ?value3 . ?value3 ?pf3 <$ENTITY$>. FILTER ((?value1 != <$ENTITY$>) &&  (?value2 != <$ENTITY$>)).   FILTER (  STRSTARTS(STR(?pf1), \"http://dbpedia.org/ontology/\") || ( str(?pf1) = \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\" )  || ( str(?pf1) = \"http://purl.org/dc/terms/subject\" ) ) .  FILTER (  STRSTARTS(STR(?pf2), \"http://dbpedia.org/ontology/\") || ( str(?pf2) = \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\" )  || ( str(?pf2) = \"http://purl.org/dc/terms/subject\" ) ).   FILTER (  STRSTARTS(STR(?pf3), \"http://dbpedia.org/ontology/\") || ( str(?pf3) = \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\" )  || ( str(?pf3) = \"http://purl.org/dc/terms/subject\" ) )} ";

	SPARQLEndpointQueryRunner sparqlQueryRunner;

	public SPARQLEndpointQueryRunner getSparqlQueryRunner() {
		return sparqlQueryRunner;
	}

	public void setSparqlQueryRunner(SPARQLEndpointQueryRunner sparqlQueryRunner) {
		this.sparqlQueryRunner = sparqlQueryRunner;
	}

	public QueryRunner(SPARQLEndpointQueryRunner sparqlQueryRunner) {
		super();
		this.sparqlQueryRunner = sparqlQueryRunner;
	}

	public void processInstance(Instance instance,
			Map<String, Integer> classUniques) throws OperatorException {
		runQuery(instance, OUT_1, classUniques);
		// runQuery(instance, OUT_2, classUniques);
		// runQuery(instance, OUT_3, classUniques);
		runQuery(instance, IN_1, classUniques);
		// runQuery(instance, IN_2, classUniques);
		// runQuery(instance, IN_3, classUniques);
	}

	public ArrayList<String> runQuery(Instance instance, String query,
			Map<String, Integer> classUniques) throws OperatorException {

		int offset = 0;
		ArrayList<String> result = new ArrayList<String>();

		// incoming
		try {

			Query queryQ = QueryFactory.create(query.replace("$ENTITY$",
					instance.getUri()));
			if (sparqlQueryRunner.getPageSize() > 0) {
				queryQ = SPARQLEndpointQueryRunner.addOrderByToQuery(queryQ
						.toString());
				queryQ.setLimit(sparqlQueryRunner.getPageSize());
			}
			if (sparqlQueryRunner.getRunnerType() == QuerryRunnerType.URLBASED) {
				sparqlQueryRunner.updateModel(instance.getUri());
			}

			ResultSet RS = sparqlQueryRunner.runSelectQueryInterruptable(queryQ
					.toString());
			if (sparqlQueryRunner.mUIThreadRunning) {
				while (true) {
					int currentResSize = 0;
					while (RS != null && RS.hasNext()) {
						QuerySolution sol = RS.next();

						// TODO refactor this piece of shit code
						// this is only for out relations
						if (query.startsWith("SELECT * WHERE { <$ENTITY$>")) {
							String prefix = "_out_";

							String attName = prefix;

							// keeps the last two hops to create generic
							// features
							List<String> newHops = new LinkedList<String>();
							int counter = 1;
							for (String var : RS.getResultVars()) {
								attName += sol.get(var).toString() + prefix;

								if (counter > RS.getResultVars().size() - 2) {
									// add it in the unique map
									// uniqueAtts.put(attName, 0);

									// add it to the features of the class
									if (!result.contains(attName)) {
										addToClassFeatures(classUniques,
												attName);
									}
									result.add(attName);
									// add the last two hops
									newHops.add(sol.get(var).toString());
									// add the feature to the instance
									instance.addFeature(attName);
									// if it is the last node add it to the map
									// if (var.contains("value"))
									// instance.addLatNodeFeature(sol.get(var)
									// .toString(), attName);
								}
								counter++;

							}

							// generate the generic features with level
							// find the level
							int levelCnt = RS.getResultVars().size() - 2;
							// create the prefix
							String levelPrefix = prefix + "level_" + levelCnt;
							// if we are on level 0 don't do anything
							if (levelCnt > 0) {
								String attre = levelPrefix;
								for (String hop : newHops) {
									attre += "_" + hop;
									// uniqueAtts.put(attre, 0);
									// add it to the features of the class
									if (!result.contains(attre)) {
										addToClassFeatures(classUniques, attre);
									}
									result.add(attre);
									instance.addFeature(attre);

								}
								// instance.addLatNodeFeature(newHops.get(1),
								// attre);

							}
							// add the generic feature for the last node
							levelPrefix = prefix + "level_" + (levelCnt + 1)
									+ "_";
							String attre = levelPrefix + newHops.get(1);
							// uniqueAtts.put(attre, 0);
							// add it to the features of the class
							if (!result.contains(attre)) {
								addToClassFeatures(classUniques, attre);
							}
							result.add(attre);
							instance.addFeature(attre);
							instance.addLatNodeFeature(newHops.get(1), attre);
						} else {
							String prefix = "_in_";

							String attName = prefix;

							// keeps the first two hops to create generic
							// features
							List<String> newHops = new LinkedList<String>();
							newHops.add(sol.get(RS.getResultVars().get(1))
									.toString());
							newHops.add(sol.get(RS.getResultVars().get(0))
									.toString());
							int counter = 0;
							for (String var : RS.getResultVars()) {

								if (counter >= 2) {
									attName += sol.get(var).toString() + prefix;
									// add it in the unique map
									// uniqueAtts.put(attName, 0);

									// if it is the last node add it to the map
									// if (var.contains("value"))
									// instance.addLatNodeFeature(sol.get(var)
									// .toString(), attName);
								}
								counter++;

							}
							// add both hops
							attName = newHops.get(0) + prefix + attName;
							// add it to the features of the class
							if (!result.contains(attName)) {
								addToClassFeatures(classUniques, attName);
							}
							result.add(attName);
							// add the feature to the instance
							instance.addFeature(attName);
							attName = newHops.get(1) + prefix + attName;
							// add it to the features of the class
							if (!result.contains(attName)) {
								addToClassFeatures(classUniques, attName);
							}
							result.add(attName);
							// add the feature to the instance
							instance.addFeature(attName);

							// generate the generic features with level
							// find the level
							int levelCnt = RS.getResultVars().size() - 2;
							// create the prefix
							String levelPrefix = prefix + "level_" + levelCnt;
							// if we are on level 0 don't do anything
							if (levelCnt > 0) {
								String attre = levelPrefix;
								for (String hop : newHops) {
									attre += "_" + hop;
									// uniqueAtts.put(attre, 0);
									// add it to the features of the class
									if (!result.contains(attre)) {
										addToClassFeatures(classUniques, attre);
									}
									result.add(attre);
									instance.addFeature(attre);

								}
								// instance.addLatNodeFeature(newHops.get(1),
								// attre);

							}
							// add the generic feature for the last node
							levelPrefix = prefix + "level_" + (levelCnt + 1)
									+ "_";
							String attre = levelPrefix + newHops.get(1);
							// uniqueAtts.put(attre, 0);
							// add it to the features of the class
							if (!result.contains(attre)) {
								addToClassFeatures(classUniques, attre);
							}
							result.add(attre);
							instance.addFeature(attre);
							instance.addLatNodeFeature(newHops.get(1), attre);
						}

						currentResSize++;
					}
					if (sparqlQueryRunner.getPageSize() == 0
							|| currentResSize < sparqlQueryRunner.getPageSize()
							|| offset >= 10000)
						break;
					offset += sparqlQueryRunner.getPageSize();
					queryQ.setOffset(offset);
					queryQ.setLimit(sparqlQueryRunner.getPageSize());

					RS = sparqlQueryRunner.runSelectQueryInterruptable(queryQ
							.toString());
					if (sparqlQueryRunner.mUIThreadRunning) {
						if (RS == null || !RS.hasNext())
							break;
					} else {
						return null;
					}
				}
			} else {
				return null;
			}

		} catch (UndefinedParameterError e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}

		return result;
	}

	public void addToClassFeatures(Map<String, Integer> classUniques,
			String feature) {

		int n = 1;
		if (classUniques.containsKey(feature))
			n += classUniques.get(feature);

		classUniques.put(feature, n);
	}

	public static void main(String[] args) {
		if (OUT_2.startsWith("SELECT * WHERE { <$ENTITY$>"))
			System.out.println("dads");
	}
}
