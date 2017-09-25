package lod.generators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lod.dataclasses.ValueClassesPair;
import lod.generators.dataclasses.Generators;
import lod.generators.vectorcreation.VectorCreator;
import lod.sparql.SPARQLEndpointQueryRunner;
import lod.sparql.SPARQLQueryRunner.QuerryRunnerType;
import lod.utils.RelationFeature;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.rapidminer.example.Example;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;

/**
 * Qualified Relations Numeric generator This generator creates a numeric
 * attribute for incoming and outgoing relations from/to an object from a
 * certain type e.g. for the triples <http://foo.bar/s> <http://foo.bar/p>
 * <http://foo.bar/o> <http://foo.bar/o> rdf:type <http://foo.bar/t> the
 * relation uri_out_type_numeric_http://foo.bar/p_type_http://foo.bar/t would be
 * set to 1.
 * 
 * @author Heiko Paulheim
 * @author Evgeny Mitichkin
 * @author Petar Ristoski
 */
public class RelationTypeNumericFeatureGeneratorOperator extends BaseGenerator {

	private static final String CLASS_NAME = "relation_type_numeric_feature_generator_operator";

	public static final String PARAMETER_ENDPOINT = "SPARQL check endpoint";

	public static final String PARAMETER_REGEX_FILTERS_TYPES = "Types Regex Filters";

	private static final String IN_REPLACE = "in_numeric_";

	private static final String OUT_REPLACE = "out_numeric_";

	public RelationTypeNumericFeatureGeneratorOperator(
			OperatorDescription description) {
		super(description);
	}

	/**
	 * holds the types regexes
	 */
	private List<String> typesRegexes = new ArrayList<String>();

	@Override
	public void doWork() throws OperatorException {

		initAttributesAndParams(true, CLASS_NAME);

		List<RelationFeature> addedRelations = new ArrayList<RelationFeature>();

		readRegexes();

		for (int i = 0; i < attrsBypsass.size(); i++) {
			ArrayList<ValueClassesPair> entityLinkCorespondence = new ArrayList<ValueClassesPair>();
			String attributeName = attrsBypsass.get(i);
			String attributeNamePrefix = attrsBypsass.get(i);
			int valueId = 0;

			for (Example ex : exampleSet) {
				ArrayList<String> classes = processInstance(
						ex.getValueAsString(attrs.get(attributeName)),
						countRelations, valueId, addedRelations,
						attributeNamePrefix,
						getParameterAsInt(PARAMETER_PROPERTIES_DIRECTION));
				if (!queryRunner.mUIThreadRunning)
					break;
				ValueClassesPair valueClassPair = new ValueClassesPair(
						ex.getValueAsString(attrs.get(attributeName)), valueId,
						classes);
				entityLinkCorespondence.add(valueClassPair);
				valueId++;
			}
			if (!queryRunner.mUIThreadRunning)
				break;
			// determining a set of uniqie attributes and adding them
			Set<String> uniqueAttributesToAdd = uniqueAtts.keySet();

			uniqueAtts = new HashMap<String, Integer>();

			// adding attributes
			extendAttributeSet(uniqueAttributesToAdd, attributeNamePrefix,
					entityLinkCorespondence, Ontology.NUMERICAL,
					Generators.RELATION_TYPE_NUMERIC_FEATURE, "_");
			// /////END
		}

		// change the vector representation if needed
		int option = getParameterAsInt(PARAMETER_VECTOR_REPRESENTATION);
		// the count representation will be skipped later if needed
		VectorCreator vc = new VectorCreator();
		vc.createVector(exampleSet, option, addedAttributeOverall);

		mOutputPort.deliver(exampleSet);
		if (createHierarchy)
			hierarchy = RelationTypePresenceFeatureGeneratorOperator
					.generateHierarchy(addedRelations, queryRunner);
		mOutputPortTypesHierarchy.deliver(hierarchy);
		super.doWork();
	}

	public void readRegexes() throws UndefinedParameterError {
		typesRegexes = new ArrayList<String>();
		String[] regexesTmp = ParameterTypeEnumeration
				.transformString2Enumeration(getParameterAsString(PARAMETER_REGEX_FILTERS_TYPES));
		for (int i = 0; i < regexesTmp.length; i++) {
			typesRegexes.add(regexesTmp[i]);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType lastType = types.get(types.size() - 1);
		types.remove(types.size() - 1);

		types.add(new ParameterTypeEnumeration(
				PARAMETER_REGEX_FILTERS_TYPES,
				"Only types that match the defined regex patterns will be used. (To define excluding regex pattersns, start the regex with \"!\"",
				new ParameterTypeString(PARAMETER_REGEX, "Regex"), false));
		types.add(lastType);
		types.add(new ParameterTypeCategory(PARAMETER_PROPERTIES_DIRECTION,
				"Properties Direction", PROPERTY_DIRECTION_NAMES, 0, false));

		return types;
	}

	protected ArrayList<String> processInstance(String attributeToExpandValue,
			Map<Integer, Map<String, Integer>> countRelations, int valueId,
			List<RelationFeature> addedRelations, String attPrefix,
			int propertiesDirection) throws OperatorException {

		String uri = attributeToExpandValue;
		ArrayList<String> result = new ArrayList<String>();
		Map<String, Integer> localCountRelations = new HashMap<String, Integer>();
		int offset = 0;
		try {
			if (queryRunner.getRunnerType() == QuerryRunnerType.URLBASED) {
				queryRunner.updateModel(uri);
			}
			// incoming
			Query queryQ = null;
			// get incoming if the option is 0 or 1
			if (propertiesDirection == 0 || propertiesDirection == 1) {
				if (!queryRunner.isUseCount())
					queryQ = QueryFactory.create(getSPARQLQueryIncoming(uri));
				else
					queryQ = QueryFactory
							.create(getSPARQLQueryIncomingWithCount(uri));
				if (queryRunner.getPageSize() > 0) {
					queryQ = SPARQLEndpointQueryRunner.addOrderByToQuery(queryQ
							.toString());
					queryQ.setLimit(queryRunner.getPageSize());
				}

				ResultSet RS1 = queryRunner.runSelectQueryInterruptable(queryQ
						.toString());
				if (queryRunner.mUIThreadRunning) {
					while (true) {
						while (RS1 != null && RS1.hasNext()) {
							QuerySolution sol = RS1.next();
							if (sol.get("p") == null || sol.get("t") == null)
								continue;
							String attName = IN_REPLACE
									+ sol.get("p").toString() + "_type_"
									+ sol.get("t").toString();
							if (queryRunner.isUseCount()) {
								localCountRelations.put(attName, sol
										.getLiteral("relations").getInt());
							}
							result.add(attName);
							uniqueAtts.put(attName, 0);
							RelationFeature newRelation = RelationTypePresenceFeatureGeneratorOperator
									.getRelationFeature(addedRelations, sol
											.get("p").toString(), "in");
							if (newRelation != null) {
								if (!newRelation.getAllTypes().containsKey(
										sol.get("t").toString())) {
									newRelation.getAllTypes().put(
											sol.get("t").toString(),
											attPrefix + "_" + attName);
								}
							} else {
								newRelation = new RelationFeature();
								newRelation.setDirection("in");
								newRelation.setRelationName(sol.get("p")
										.toString());
								newRelation.getAllTypes().put(
										sol.get("t").toString(),
										attPrefix + "_" + attName);
								addedRelations.add(newRelation);
							}
						}
						if (queryRunner.getPageSize() == 0)
							break;
						offset += queryRunner.getPageSize();
						queryQ.setOffset(offset);
						queryQ.setLimit(queryRunner.getPageSize());

						RS1 = queryRunner.runSelectQueryInterruptable(queryQ
								.toString());
						if (queryRunner.mUIThreadRunning) {
							if (RS1 == null || !RS1.hasNext())
								break;
						} else {
							return null;
						}
					}
				} else {
					return null;
				}
			}
			// outgoing
			// get outgoing if option is 0 or 2
			if (propertiesDirection == 0 || propertiesDirection == 2) {
				offset = 0;
				if (!queryRunner.isUseCount())
					queryQ = QueryFactory.create(getSPARQLQueryOutgoing(uri));
				else
					queryQ = QueryFactory
							.create(getSPARQLQueryOutgoingWithCount(uri));

				if (queryRunner.getPageSize() > 0) {
					queryQ = SPARQLEndpointQueryRunner.addOrderByToQuery(queryQ
							.toString());
					queryQ.setLimit(queryRunner.getPageSize());
				}

				ResultSet RS2 = queryRunner.runSelectQueryInterruptable(queryQ
						.toString());
				if (queryRunner.mUIThreadRunning) {
					while (true) {
						while (RS2 != null && RS2.hasNext()) {
							QuerySolution sol = RS2.next();
							if (sol.get("p") == null || sol.get("t") == null)
								continue;
							String attName = OUT_REPLACE
									+ sol.get("p").toString() + "_type_"
									+ sol.get("t").toString();
							if (queryRunner.isUseCount()) {
								localCountRelations.put(attName, sol
										.getLiteral("relations").getInt());
							}
							result.add(attName);
							uniqueAtts.put(attName, 0);
							RelationFeature newRelation = RelationTypePresenceFeatureGeneratorOperator
									.getRelationFeature(addedRelations, sol
											.get("p").toString(), "out");
							if (newRelation != null) {
								if (!newRelation.getAllTypes().containsKey(
										sol.get("t").toString())) {
									newRelation.getAllTypes().put(
											sol.get("t").toString(),
											attPrefix + "_" + attName);
								}
							} else {
								newRelation = new RelationFeature();
								newRelation.setDirection("out");
								newRelation.setRelationName(sol.get("p")
										.toString());
								newRelation.getAllTypes().put(
										sol.get("t").toString(),
										attPrefix + "_" + attName);
								addedRelations.add(newRelation);
							}
						}
						if (queryRunner.getPageSize() == 0)
							break;
						offset += queryRunner.getPageSize();
						queryQ.setOffset(offset);
						queryQ.setLimit(queryRunner.getPageSize());

						RS2 = queryRunner.runSelectQueryInterruptable(queryQ
								.toString());
						if (queryRunner.mUIThreadRunning) {
							if (RS2 == null || !RS2.hasNext())
								break;
						} else {
							return null;
						}
					}
				} else {
					return null;
				}
			}
		} catch (UndefinedParameterError e) {
			e.printStackTrace();
		}
		countRelations.put(valueId, localCountRelations);
		return result;
	}

	@Override
	public String getSPARQLQueryOutgoing(String uri) {
		String query = "SELECT DISTINCT ?p ?t ?o "
				+ "WHERE {<"
				+ uri
				+ "> ?p ?o . ?o <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t .}";
		return createFilteredQuery(createFilteredQuery(query, "?p", null),
				"?t", typesRegexes);
	}

	@Override
	public String getSPARQLQueryIncoming(String uri) {
		String query = "SELECT DISTINCT ?p ?t ?s "
				+ "WHERE {?s ?p <"
				+ uri
				+ ">. ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t .}";
		return createFilteredQuery(createFilteredQuery(query, "?p", null),
				"?t", typesRegexes);
	}

	protected String getSPARQLQueryOutgoingWithCount(String uri) {
		String query = "SELECT DISTINCT (count(?o) as ?relations) ?p ?t "
				+ "WHERE {<"
				+ uri
				+ "> ?p ?o . ?o <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t .}";
		return createFilteredQuery(createFilteredQuery(query, "?p", null),
				"?t", typesRegexes) + " group by ?p ?t";
	}

	protected String getSPARQLQueryIncomingWithCount(String uri) {
		String query = "SELECT DISTINCT  (count(?s) as ?relations) ?p ?t "
				+ "WHERE {?s ?p <"
				+ uri
				+ ">. ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t .}";
		return createFilteredQuery(createFilteredQuery(query, "?p", null),
				"?t", typesRegexes) + " group by ?p ?t";
	}
}
