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
import lod.gui.tools.dialogs.SPARQLConfigurator;
import lod.sparql.SPARQLEndpointQueryRunner;
import lod.sparql.SPARQLQueryRunner.QuerryRunnerType;
import lod.utils.AttributeTypeGuesser;
import lod.utils.HierarchyPair;
import lod.utils.OntologyHierarchy;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.config.ParameterTypeConfigurable;

/**
 * Specific Relation generator
 * 
 * @author Petar Ristoski
 * @author Evgeny Mitichkin
 * 
 */
public class SpecificRelationGeneratorOperator extends BaseGenerator {

	private static final String CLASS_NAME = "specific_relation_generator_operator";

	public static final String QUERY_SIMPLE_RELATION = "SELECT ?subject WHERE {?instance ?relation ?subject}";

	public static final String QUERY_HIERARCHY_RELATION = "SELECT ?subject WHERE {?instance ?relation ?s.?s ?hierarchyrelation ?subject OPTION (TRANSITIVE, T_DISTINCT, T_IN(?s),T_OUT(?subject),T_MAX(?NM))}";

	public static final String QUERY_HIERARCHY_RELATION_COMPLETE = "SELECT ?subject WHERE {?instance ?relation ?s. ?s ?hierarchyrelation ?subject OPTION (TRANSITIVE, T_DISTINCT, T_IN(?s),T_OUT(?subject),T_MIN(0))}";

	public static final String PARAMETER_RELATION = "Direct Relation";

	public static final String PARAMETER_HIERARCHY_RELATION = "Hierarchy Relation";

	public static final String PARAMETER_HIERARCHY_DEPTH = "Max hierarchy depth";

	public SpecificRelationGeneratorOperator(OperatorDescription description) {
		super(description);

	}

	@Override
	public void doWork() throws OperatorException {
		initAttributesAndParams(false, CLASS_NAME);

		// concept URI with the corresponding attribute name
		Map<String, String> addedAttributeOverall = new HashMap<String, String>();

		// get the parameters
		String relation = this.getParameterAsString(PARAMETER_RELATION);
		String hierarchyRelation = this
				.getParameterAsString(PARAMETER_HIERARCHY_RELATION);
		int maxDepth = this.getParameterAsInt(PARAMETER_HIERARCHY_DEPTH);

		boolean createHierarchy = mOutputPortTypesHierarchy.isConnected();

		for (int i = 0; i < attrsBypsass.size(); i++) {
			ArrayList<ValueClassesPair> entityLinkCorespondence = new ArrayList<ValueClassesPair>();
			String attributeName = attrsBypsass.get(i);
			String attributeNamePrefix = attrsBypsass.get(0);
			int valueId = 0;

			for (Example ex : exampleSet) {
				ArrayList<String> classes = new ArrayList<String>();
				// if the value is empty don't run query
				if (!ex.getValueAsString(attrs.get(attributeName)).equals("?"))
					classes = processInstance(
							ex.getValueAsString(attrs.get(attributeName)),
							relation, hierarchyRelation, maxDepth);

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
			// determining a set of unique attributes and adding them
			Set<String> uniqueAttributesToAdd = uniqueAtts.keySet();

			uniqueAtts = new HashMap<String, Integer>();

			// adding attributes
			for (String attrName : uniqueAttributesToAdd) {
				// if the same attribute was added once don't add it again even
				// if it is coming from different link
				boolean shoudlChange = false;
				if (addedAttributeOverall.containsKey(attrName)) {
					shoudlChange = true;
				} else {
					addedAttributeOverall.put(attrName, attributeNamePrefix
							+ "_relation_" + attrName);
					addAtribute(attributeNamePrefix + "_relation_" + attrName,
							Ontology.NUMERICAL, exampleSet);
				}
				int h = 0;
				// Looking for the values for this attribute and setting them
				for (Example ex : exampleSet) {
					// for every record retrieving the set of values
					ValueClassesPair vpair = entityLinkCorespondence.get(h);
					ArrayList<String> entities = vpair.getClasses();
					// the value to be added
					int value = 0;
					if (entities.contains(attrName))
						value = 1;
					if (shoudlChange) {
						if (entities.contains(attrName)) {
							ex.setValue(attrs.get(attributeNamePrefix
									+ "_relation_" + attrName),
									AttributeTypeGuesser.getValueForAttribute(
											attrs.get(attributeNamePrefix
													+ "_relation_" + attrName),
											Integer.toString(value)));
						} else {
							h++;
							continue;
						}

					} else {
						ex.setValue(attrs.get(attributeNamePrefix
								+ "_relation_" + attrName),
								AttributeTypeGuesser.getValueForAttribute(
										attrs.get(attributeNamePrefix
												+ "_relation_" + attrName),
										Integer.toString(value)));
					}
					h++;
				}
			}

		}
		// change the vector representation if needed
		int option = getParameterAsInt(PARAMETER_VECTOR_REPRESENTATION);
		// if it is the normal binary representation we can skip the new
		// representation
		// the count representation will be skipped later if needed
		if (option != 0) {
			VectorCreator vc = new VectorCreator();
			vc.createVector(exampleSet, option, addedAttributeOverall);
		}
		mOutputPort.deliver(exampleSet);

		OntologyHierarchy hierarchy = null;
		if (hierarchyRelation != null && !hierarchyRelation.equals(""))
			if (createHierarchy)
				hierarchy = generateHierarchy(addedAttributeOverall,
						hierarchyRelation, maxDepth);
		mOutputPortTypesHierarchy.deliver(hierarchy);
		super.doWork();
	}

	protected ArrayList<String> processInstance(String attributeToExpandValue,
			String relation, String hierarchyRelation, int maxDepth)
			throws OperatorException {
		String uri = attributeToExpandValue;
		int offset = 0;
		ArrayList<String> result = new ArrayList<String>();
		// relations
		// we cannot use the standard sparql execution because we need to use
		// virtuoso OPTION function
		try {
			Query queryQ;
			String queryQStr = getSPARQLQuery(uri, relation, hierarchyRelation,
					maxDepth);
			if (queryRunner.getRunnerType() == QuerryRunnerType.URLBASED) {
				queryRunner.updateModel(uri);
			}
			// if (queryRunner.getPageSize() > 0) {
			// // queryQStr =
			// //SPARQLEndpointQueryRunner.addOrderByToQuery(queryQStr);
			// queryQStr = queryRunner.setOffsetAndLimit(queryQStr, offset,
			// queryRunner.getPageSize());
			//
			// }

			ResultSet RS = queryRunner.executeNonStandardQuery(queryQStr);
			if (queryRunner.mUIThreadRunning) {
				while (true) {
					while (RS.hasNext()) {
						QuerySolution sol = RS.next();
						String attName = sol.get("subject").toString();
						result.add(attName);
						uniqueAtts.put(attName, 0);
					}
					if (queryRunner.getPageSize() == 0)
						break;
					offset += queryRunner.getPageSize();
					queryQStr = queryRunner.setOffsetAndLimit(queryQStr,
							offset, queryRunner.getPageSize());
					RS = queryRunner.executeNonStandardQuery(queryQStr);
					if (queryRunner.mUIThreadRunning) {
						if (!RS.hasNext())
							break;
					} else {
						return null;
					}
				}
			} else {
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private String getSPARQLQuery(String uri, String relation,
			String hierarchyRelation, int maxDepth) {
		if (hierarchyRelation == null || hierarchyRelation.equals(""))
			return getSPARQLquerySimpleRelation(uri, relation);

		return getSPARQLqueryWithHierarchyRelation(uri, relation,
				hierarchyRelation, maxDepth);

	}

	private String getSPARQLquerySimpleRelation(String uri, String relation) {
		ParameterizedSparqlString queryStringGetInstances = new ParameterizedSparqlString(
				QUERY_SIMPLE_RELATION);
		queryStringGetInstances.setIri("?instance", uri);
		queryStringGetInstances.setIri("?relation", relation);
		return queryStringGetInstances.toString();
	}

	private String getSPARQLqueryWithHierarchyRelation(String uri,
			String relation, String hierarchyRelation, int maxDepth) {
		if (maxDepth == 0) {
			ParameterizedSparqlString queryStringGetInstances = new ParameterizedSparqlString(
					QUERY_HIERARCHY_RELATION_COMPLETE);
			queryStringGetInstances.setIri("?instance", uri);
			queryStringGetInstances.setIri("?relation", relation);
			queryStringGetInstances.setIri("?hierarchyrelation",
					hierarchyRelation);
			return queryStringGetInstances.toString();
		} else {

			ParameterizedSparqlString queryStringGetInstances = new ParameterizedSparqlString(
					QUERY_HIERARCHY_RELATION);
			queryStringGetInstances.setIri("?instance", uri);
			queryStringGetInstances.setIri("?relation", relation);
			queryStringGetInstances.setIri("?hierarchyrelation",
					hierarchyRelation);

			return queryStringGetInstances.toString().replace("?NM",
					Integer.toString(maxDepth));
		}
	}

	@Override
	protected String getSPARQLQueryOutgoing(String uri) {
		return null;
	}

	@Override
	protected String getSPARQLQueryIncoming(String uri) {
		return null;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeString(PARAMETER_RELATION,
				"The relation to extend", "http://purl.org/dc/terms/subject",
				false));

		types.add(new ParameterTypeString(PARAMETER_HIERARCHY_RELATION,
				"The hierarchy relation to extend", true, false));

		types.add(new ParameterTypeInt(
				PARAMETER_HIERARCHY_DEPTH,
				"Maximum hierarchy depth (if zero, whole hierarchy will be retrieved)",
				0, 100, 1, false));

		types.add(new ParameterTypeCategory(PARAMETER_VECTOR_REPRESENTATION,
				"Select the schema for creating the word vector",
				VECTOR_CREATOR_NAMES, 0, false));

		return types;
	}

	public OntologyHierarchy generateHierarchy(
			Map<String, String> addedAttributeOverall,
			String hierarchyRelation, int maxDepth) throws OperatorException {
		OntologyHierarchy hierarchy = new OntologyHierarchy();
		Map<String, List<String>> overallSuperClasses = new HashMap<String, List<String>>();
		List<String> allAttributesList = new ArrayList<String>();
		allAttributesList.addAll(addedAttributeOverall.keySet());
		for (Entry entry : addedAttributeOverall.entrySet()) {
			List<String> superClasses = queryRunner.getSuperRelations(
					(String) entry.getKey(), allAttributesList,
					hierarchyRelation, maxDepth);
			List<String> directSuperClasses = queryRunner.getSuperRelations(
					(String) entry.getKey(), allAttributesList,
					hierarchyRelation, 1);
			overallSuperClasses.put((String) entry.getKey(), superClasses);
			String supperClassesAppended = "";
			for (String superClass : superClasses) {
				supperClassesAppended += superClass + ", ";
			}
			if (supperClassesAppended.length() > 2) {
				supperClassesAppended = supperClassesAppended.substring(0,
						supperClassesAppended.length() - 2);
			}
			HierarchyPair pair = new HierarchyPair((String) entry.getKey(),
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
