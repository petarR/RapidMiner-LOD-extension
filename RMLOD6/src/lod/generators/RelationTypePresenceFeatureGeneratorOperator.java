package lod.generators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import lod.dataclasses.ValueClassesPair;
import lod.generators.dataclasses.Generators;
import lod.sparql.SPARQLEndpointQueryRunner;
import lod.sparql.SPARQLQueryRunner.QuerryRunnerType;
import lod.utils.AttributeTypeGuesser;
import lod.utils.HierarchyPair;
import lod.utils.OntologyHierarchy;
import lod.utils.OntologyHierarchy.HierarchyType;
import lod.utils.RelationFeature;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;


//IT IS NOT USED
/**
 * Qualified Relations Boolean generator
 * @author Heiko Paulheim
 * @author Evgeny Mitichkin
 * @author Petar Ristoski
 *
 */
public class RelationTypePresenceFeatureGeneratorOperator extends BaseGenerator {

	private static final String CLASS_NAME = "relation_type_presence_feature_generator_operator";
	
	private static final String IN_REPLACE = "in_boolean_";
	
	private static final String OUT_REPLACE = "out_boolean_";

	public RelationTypePresenceFeatureGeneratorOperator(
			OperatorDescription description) {
		super(description);
	}

	@Override
	public void doWork() throws OperatorException {
		
		initAttributesAndParams(false, CLASS_NAME);		
		
		List<RelationFeature> addedRelations = new ArrayList<RelationFeature>();
		
		for (int i = 0; i < attrsBypsass.size(); i++) {
			ArrayList<ValueClassesPair> entityLinkCorespondence = new ArrayList<ValueClassesPair>();
			String attributeName = attrsBypsass.get(i);
			String attributeNamePrefix = attrsBypsass.get(i);
			int valueId = 0;

			for (Example ex : exampleSet) {
				ArrayList<String> classes = processInstance(
						ex.getValueAsString(attrs.get(attributeName)),
						addedRelations, attributeNamePrefix);
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

			extendAttributeSet(uniqueAttributesToAdd, attributeNamePrefix, entityLinkCorespondence, Ontology.BINOMINAL, Generators.RELATION_TYPE_PRESENCE, "_");
			///////END
		}
		mOutputPort.deliver(exampleSet);

		if (createHierarchy)
			hierarchy = generateHierarchy(addedRelations, queryRunner);
		mOutputPortTypesHierarchy.deliver(hierarchy);

		super.doWork();
	}

	protected ArrayList<String> processInstance(String attributeToExpandValue, List<RelationFeature> addedRelations, String attPrefix) throws OperatorException {

		String uri = attributeToExpandValue;
		ArrayList<String> result = new ArrayList<String>();
		int offset = 0;
		try {
			if (queryRunner.getRunnerType() == QuerryRunnerType.URLBASED) {
				queryRunner.updateModel(uri);
			}
			// incoming
			Query queryQ = QueryFactory.create(getSPARQLQueryIncoming(uri));
			if (queryRunner.getPageSize() > 0) {
				queryQ = SPARQLEndpointQueryRunner.addOrderByToQuery(queryQ
						.toString());
				queryQ.setLimit(queryRunner.getPageSize());
			}

			System.out.println(queryQ.toString());

			ResultSet RS1 = queryRunner.runSelectQueryInterruptable(queryQ
					.toString());
			if (queryRunner.mUIThreadRunning) {
				while (true) {
					while (RS1 != null && RS1.hasNext()) {
						QuerySolution sol = RS1.next();
						String attName = IN_REPLACE
								+ sol.get("p").toString() + "_type_"
								+ sol.get("t").toString();
						result.add(attName);
						uniqueAtts.put(attName, 0);
						RelationFeature newRelation = getRelationFeature(
								addedRelations, sol.get("p").toString(), "in");
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
							newRelation
									.setRelationName(sol.get("p").toString());
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
					System.out.println(queryQ.toString());

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

			// outgoing
			offset = 0;
			queryQ = QueryFactory.create(getSPARQLQueryOutgoing(uri));
			if (queryRunner.getPageSize() > 0) {
				queryQ = SPARQLEndpointQueryRunner.addOrderByToQuery(queryQ
						.toString());
				queryQ.setLimit(queryRunner.getPageSize());
			}

			System.out.println(queryQ.toString());

			ResultSet RS2 = queryRunner.runSelectQueryInterruptable(queryQ
					.toString());
			if (queryRunner.mUIThreadRunning) {
				while (true) {
					while (RS2 != null && RS2.hasNext()) {
						QuerySolution sol = RS2.next();
						String attName = OUT_REPLACE
								+ sol.get("p").toString() + "_type_"
								+ sol.get("t").toString();
						result.add(attName);
						uniqueAtts.put(attName, 0);
						RelationFeature newRelation = getRelationFeature(
								addedRelations, sol.get("p").toString(), "out");
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
							newRelation
									.setRelationName(sol.get("p").toString());
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
					System.out.println(queryQ.toString());

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
		} catch (UndefinedParameterError e) {
			e.printStackTrace();
		}
		return result;
	}

	public static OntologyHierarchy generateHierarchy(
			List<RelationFeature> addedRelations,
			SPARQLEndpointQueryRunner queryRunner) throws OperatorException {
		OntologyHierarchy hierarchy = new OntologyHierarchy();
		Map<String, List<String>> overallSuperClasses = new HashMap<String, List<String>>();
		List<String> allAttributesList = new ArrayList<String>();
		for (RelationFeature feature : addedRelations) {
			String attrName = feature.getRelationName();
			allAttributesList.add(attrName);
		}
		for (RelationFeature feature : addedRelations) {
			String attrName = feature.getRelationName();
			List<String> superClasses = queryRunner.getSuperClasses(attrName,
					allAttributesList, queryRunner.GET_SUPER_PROPERTIES);
			List<String> directSuperClasses = queryRunner.getSuperClasses(
					attrName, allAttributesList,
					queryRunner.GET_DIRECT_SUPER_PROPERTIES);
			overallSuperClasses.put(attrName, superClasses);
			String supperClassesAppended = "";
			for (String superClass : superClasses) {
				supperClassesAppended += superClass + ", ";
			}
			if (supperClassesAppended.length() > 2) {
				supperClassesAppended = supperClassesAppended.substring(0,
						supperClassesAppended.length() - 2);
			}
			HierarchyPair pair = new HierarchyPair(attrName,
					supperClassesAppended);
			pair.setSuperClasses(superClasses);
			pair.setDirectSuperClasses(directSuperClasses);
			List<String> correspondAttr = new ArrayList<String>();
			correspondAttr.addAll(feature.getAllTypes().values());
			pair.setCorrespondingAttr(correspondAttr);

			// create the hierarchy of the types of each relation
			Map<String, List<String>> overallSuperClassesTypes = new HashMap<String, List<String>>();
			List<String> allAttributesListTypes = new ArrayList<String>();
			allAttributesListTypes.addAll(feature.getAllTypes().keySet());
			for (Entry typeTmp : feature.getAllTypes().entrySet()) {

				List<String> superClassesTypes = queryRunner.getSuperClasses(
						(String) typeTmp.getKey(), allAttributesListTypes,
						queryRunner.GET_SUPERCLASSES_QUERY);
				List<String> directSuperClassesTypes = queryRunner
						.getSuperClasses((String) typeTmp.getKey(),
								allAttributesListTypes,
								queryRunner.GET_DIRECT_SUPERCLASSES_QUERY);
				overallSuperClassesTypes.put((String) typeTmp.getKey(),
						superClassesTypes);
				String supperClassesAppendedTypes = "";
				for (String superClass : superClassesTypes) {
					supperClassesAppendedTypes += superClass + ", ";
				}
				if (supperClassesAppendedTypes.length() > 2) {
					supperClassesAppendedTypes = supperClassesAppendedTypes
							.substring(0,
									supperClassesAppendedTypes.length() - 2);
				}
				HierarchyPair pairType = new HierarchyPair(
						(String) typeTmp.getKey(), supperClassesAppendedTypes);
				pairType.setSuperClasses(superClassesTypes);
				pairType.setDirectSuperClasses(directSuperClassesTypes);
				List<String> correspondAttrTypes = new ArrayList<String>();
				correspondAttr.add((String) typeTmp.getValue());
				pairType.setCorrespondingAttr(correspondAttr);
				pair.getInsideHierarchyPairs().add(pairType);
			}
			// add the subclasses of the inside type pair
			for (HierarchyPair insidePair : pair.getInsideHierarchyPairs()) {
				List<String> subClasses = queryRunner.getSubClasses(
						insidePair.getBaseClass(), allAttributesListTypes,
						overallSuperClassesTypes);
				insidePair.setSubClasses(subClasses);
				if (subClasses.size() == 0)
					insidePair.setLeaf(true);
			}

			hierarchy.addNewPair(pair);
		}

		// add the subclasses of the property pair
		for (HierarchyPair pair : hierarchy.getHierarchyPairs()) {
			List<String> subClasses = queryRunner
					.getSubClasses(pair.getBaseClass(), allAttributesList,
							overallSuperClasses);
			pair.setSubClasses(subClasses);
			if (subClasses.size() == 0)
				pair.setLeaf(true);
		}
		hierarchy.setType(HierarchyType.QualifiedRelation);
		return hierarchy;
	}
	
	@Override
	public String getSPARQLQueryIncoming(String uri) {
		return "SELECT DISTINCT ?p ?t "
				+ "WHERE {?s ?p <"
				+ uri
				+ ">. ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t .}";
	}

	@Override
	public String getSPARQLQueryOutgoing(String uri) {
		return "SELECT DISTINCT ?p ?t "
				+ "WHERE {<"
				+ uri
				+ "> ?p ?o . ?o <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t .}";
	}

	public static RelationFeature getRelationFeature(
			List<RelationFeature> addedRelations, String relation,
			String direction) {
		for (RelationFeature feature : addedRelations) {
			if (feature.getDirection().equals(direction)
					&& feature.getRelationName().equals(relation))
				return feature;
		}
		return null;
	}
}
