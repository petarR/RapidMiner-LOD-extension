package lod.generators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import lod.dataclasses.ValueClassesPair;
import lod.generators.dataclasses.Generators;
import lod.generators.vectorcreation.VectorCreator;
import lod.sparql.SPARQLEndpointQueryRunner;
import lod.sparql.SPARQLQueryRunner.QuerryRunnerType;
import lod.utils.AttributeTypeGuesser;
import lod.utils.HierarchyPair;
import lod.utils.OntologyHierarchy;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.rapidminer.example.Example;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;

/**
 * Relation Numeric generator indludes the values of the relation's object or
 * subject
 * 
 * @author Heiko Paulheim
 * 
 * @author Petar Ristoski
 * 
 */
public class RelationValueNumericFeatureGeneratorOperator extends BaseGenerator {

	private static final String CLASS_NAME = "relation_value_numeric_feature_generator_operator";

	private static final String IN_REPLACE = "in_numeric_";

	private static final String OUT_REPLACE = "out_numeric_";

	public RelationValueNumericFeatureGeneratorOperator(
			OperatorDescription description) {
		super(description);
	}

	@Override
	public void doWork() throws OperatorException {

		initAttributesAndParams(true, CLASS_NAME);

		for (int i = 0; i < attrsBypsass.size(); i++) {
			ArrayList<ValueClassesPair> entityLinkCorespondence = new ArrayList<ValueClassesPair>();
			String attributeName = attrsBypsass.get(i);
			String attributeNamePrefix = attrsBypsass.get(i);
			int valueId = 0;

			boolean processInterrupted = false;
			for (Example ex : exampleSet) {
				ArrayList<String> classes = processInstance(
						ex.getValueAsString(attrs.get(attributeName)),
						countRelations, valueId,
						getParameterAsInt(PARAMETER_PROPERTIES_DIRECTION));
				if (!queryRunner.mUIThreadRunning) {
					processInterrupted = true;
					break;
				}

				ValueClassesPair valueClassPair = new ValueClassesPair(
						ex.getValueAsString(attrs.get(attributeName)), valueId,
						classes);
				entityLinkCorespondence.add(valueClassPair);
				valueId++;
			}
			if (processInterrupted)
				break;
			// determining a set of uniqie attributes and adding them
			Set<String> uniqueAttributesToAdd = uniqueAtts.keySet();

			uniqueAtts = new HashMap<String, Integer>();

			// adding attributes
			extendAttributeSet(uniqueAttributesToAdd, attributeNamePrefix,
					entityLinkCorespondence, Ontology.NUMERICAL,
					Generators.SIMPLE_TYPE, "_");
			// /////END
		}

		// change the vector representation if needed
		int option = getParameterAsInt(PARAMETER_VECTOR_REPRESENTATION);
		// the count representation will be skipped later if needed
		VectorCreator vc = new VectorCreator();
		vc.createVector(exampleSet, option, addedAttributeOverall);

		mOutputPort.deliver(exampleSet);
		if (createHierarchy)
			hierarchy = generateHierarchy(addedAttributeOverall, queryRunner,
					false);
		mOutputPortTypesHierarchy.deliver(hierarchy);
		super.doWork();
	}

	protected ArrayList<String> processInstance(String attributeToExpandValue,
			Map<Integer, Map<String, Integer>> countRelations, int valueId,
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
			if (propertiesDirection == 0 || propertiesDirection == 1) {

				queryQ = QueryFactory.create(getSPARQLQueryIncoming(uri));

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
							if (sol.get("p") == null)
								continue;
							if (sol.get("s") == null)
								continue;
							String attName = OUT_REPLACE
									+ sol.get("p").toString() + "_"
									+ sol.get("s").toString();

							result.add(attName);
							uniqueAtts.put(attName, 0);
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
			if (propertiesDirection == 0 || propertiesDirection == 2) {
				offset = 0;

				queryQ = QueryFactory.create(getSPARQLQueryOutgoing(uri));

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
							if (sol.get("p") == null)
								continue;
							if (sol.get("o") == null)
								continue;
							// remove non english
							if (!isEnglish(sol.get("o")))
								continue;
							String attName = OUT_REPLACE
									+ sol.get("p").toString() + "_"
									+ sol.get("o").toString();

							result.add(attName);
							uniqueAtts.put(attName, 0);
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

	/**decides if the literal is english or not
	 * @param rdfNode
	 * @return
	 */
	public static boolean isEnglish(RDFNode rdfNode) {
		if (rdfNode.isLiteral()) {
			String value = rdfNode.asLiteral().toString();
			if (value.contains("@") && !value.contains("@en"))
				return false;
		}

		return true;
	}

	@Override
	public String getSPARQLQueryOutgoing(String uri) {
		String query = "SELECT DISTINCT ?p ?o " + "WHERE {<" + uri
				+ "> ?p ?o .}";

		return createFilteredQuery(query, "?p", null);
	}

	@Override
	public String getSPARQLQueryIncoming(String uri) {
		String query = "SELECT DISTINCT ?p ?s " + "WHERE {?s ?p <" + uri
				+ ">.}";
		return createFilteredQuery(query, "?p", null);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeCategory(PARAMETER_PROPERTIES_DIRECTION,
				"Properties Direction", PROPERTY_DIRECTION_NAMES, 0, false));

		return types;
	}

	@Override
	public OntologyHierarchy generateHierarchy(
			Map<String, String> addedAttributeOverall,
			SPARQLEndpointQueryRunner localQueryRunner, boolean inferEndpoint)
			throws OperatorException {
		OntologyHierarchy hierarchy = new OntologyHierarchy();
		Map<String, List<String>> overallSuperClasses = new HashMap<String, List<String>>();
		List<String> allAttributesList = new ArrayList<String>();
		for (Entry entry : addedAttributeOverall.entrySet()) {
			String attrName = (String) entry.getKey();
			String attrNameClean = attrName.replace(IN_REPLACE, "");
			attrNameClean = attrNameClean.replace(OUT_REPLACE, "");
			allAttributesList.add(attrNameClean);
		}
		for (Entry entry : addedAttributeOverall.entrySet()) {
			String attrName = (String) entry.getKey();
			String attrNameClean = attrName.replace(IN_REPLACE, "");
			attrNameClean = attrNameClean.replace(OUT_REPLACE, "");
			List<String> superClasses = queryRunner.getSuperClasses(
					attrNameClean, allAttributesList,
					queryRunner.GET_SUPER_PROPERTIES);
			List<String> directSuperClasses = queryRunner.getSuperClasses(
					attrNameClean, allAttributesList,
					queryRunner.GET_DIRECT_SUPER_PROPERTIES);
			overallSuperClasses.put(attrNameClean, superClasses);
			String supperClassesAppended = "";
			for (String superClass : superClasses) {
				supperClassesAppended += superClass + ", ";
			}
			if (supperClassesAppended.length() > 2) {
				supperClassesAppended = supperClassesAppended.substring(0,
						supperClassesAppended.length() - 2);
			}
			HierarchyPair pair = new HierarchyPair(attrNameClean,
					supperClassesAppended);
			pair.setSuperClasses(superClasses);
			pair.setDirectSuperClasses(directSuperClasses);
			List<String> correspondAttr = new ArrayList<String>();
			correspondAttr.add((String) entry.getValue());
			pair.setCorrespondingAttr(correspondAttr);
			hierarchy.addNewPair(pair);
		}
		for (HierarchyPair pair : hierarchy.getHierarchyPairs()) {
			List<String> subClasses = queryRunner
					.getSubClasses(pair.getBaseClass(), allAttributesList,
							overallSuperClasses);
			pair.setSubClasses(subClasses);
			if (subClasses.size() == 0)
				pair.setLeaf(true);
		}
		return hierarchy;
	}
}
