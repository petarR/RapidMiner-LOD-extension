package lod.generators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import lod.dataclasses.ValueClassesPair;
import lod.generators.dataclasses.Generators;
import lod.generators.vectorcreation.VectorCreator;
import lod.rdf.model.RdfHolder;
import lod.rdf.model.RdfHolder.GeneratorType;
import lod.rdf.model.RdfTriple;
import lod.sparql.SPARQLEndpointQueryRunner;
import lod.sparql.SPARQLQueryRunner.QuerryRunnerType;
import lod.utils.HierarchyPair;
import lod.utils.OntologyHierarchy;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.rapidminer.example.Example;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;

/**
 * Direct Types generator
 * 
 * @author Heiko Paulheim
 * @author Evgeny Mitichkin
 * @author Petar Ristoski
 * 
 */
public class SimpleTypeFeatureGeneratorOperator extends BaseGenerator {

	private static final String CLASS_NAME = "simple_type_feature_generator_operator";

	/**
	 * Direct Types generator
	 * 
	 * @param description
	 */
	public SimpleTypeFeatureGeneratorOperator(OperatorDescription description) {
		super(description);

	}

	private SPARQLEndpointQueryRunner backupRunner;

	private int tmpHolderIndex;

	@Override
	public void doWork() throws OperatorException {
		initAttributesAndParams(false, CLASS_NAME);
		attrSeparator = "_type_";
		for (int i = 0; i < attrsBypsass.size(); i++) {
			tmpHolderIndex = i;
			ArrayList<ValueClassesPair> entityLinkCorespondence = new ArrayList<ValueClassesPair>();
			String attributeName = attrsBypsass.get(i);
			attrPrefix = attrsBypsass.get(i);
			int valueId = 0;
			entityLinkCorespondence = new ArrayList<ValueClassesPair>();
			for (Example ex : exampleSet) {
				ArrayList<String> classes = processInstance(ex
						.getValueAsString(attrs.get(attributeName)));
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
			extendAttributeSet(uniqueAttributesToAdd, attrPrefix,
					entityLinkCorespondence, Ontology.NUMERICAL,
					Generators.SIMPLE_TYPE, attrSeparator);
			// /////END

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
		if (createHierarchy) {
			if (queryRunner.getEndpoint() == null
					|| queryRunner.getEndpoint().equals("")) {
				backupRunner = new SPARQLEndpointQueryRunner(
						"http://dbpedia.org/sparql", "backupRunner", 60000, 10,
						10000, true, true);
				hierarchy = generateHierarchy(addedAttributeOverall,
						backupRunner, true);
			} else {
				hierarchy = generateHierarchy(addedAttributeOverall,
						queryRunner, false);
			}
		}
		mOutputPortTypesHierarchy.deliver(hierarchy);

		if (createRDF) {
			rdfHolder.getGeneratorTypes().add(GeneratorType.CLASSES);
			rdfHolder.populateHolderFromSubHolders();

		}
		mOutputPortRdfData.deliver(rdfHolder);
		super.doWork();
	}

	protected ArrayList<String> processInstance(String attributeToExpandValue)
			throws OperatorException {
		String uri = attributeToExpandValue;
		int offset = 0;
		ArrayList<String> result = new ArrayList<String>();
		// incoming
		try {

			Query queryQ = QueryFactory.create(getSPARQLQuery(uri));
			if (queryRunner.getPageSize() > 0) {
				queryQ = SPARQLEndpointQueryRunner.addOrderByToQuery(queryQ
						.toString());
				queryQ.setLimit(queryRunner.getPageSize());
			}
			if (queryRunner.getRunnerType() == QuerryRunnerType.URLBASED) {
				queryRunner.updateModel(uri);
			}

			ResultSet RS = queryRunner.runSelectQueryInterruptable(queryQ
					.toString());
			if (queryRunner.mUIThreadRunning) {
				while (true) {
					int currentResSize = 0;
					while (RS != null && RS.hasNext()) {
						QuerySolution sol = RS.next();
						String attName = sol.get("t").toString();
						// add it in the unique map
						uniqueAtts.put(attName, 0);
						result.add(attName);

						// add in the RDF holder if needed

						addNewTripleInRdfHolder(sol, uri);
						currentResSize++;
					}
					if (queryRunner.getPageSize() == 0
							|| currentResSize < queryRunner.getPageSize())
						break;
					offset += queryRunner.getPageSize();
					queryQ.setOffset(offset);
					queryQ.setLimit(queryRunner.getPageSize());

					RS = queryRunner.runSelectQueryInterruptable(queryQ
							.toString());
					if (queryRunner.mUIThreadRunning) {
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

	private void addNewTripleInRdfHolder(QuerySolution sol, String subject) {
		RdfHolder tmpHolder = null;
		try {
			tmpHolder = rdfHolder.getSubRdfHolders().get(tmpHolderIndex);
		} catch (Exception e) {

		}

		if (tmpHolder == null) {
			tmpHolder = new RdfHolder();
			tmpHolder.getGeneratorTypes().add(GeneratorType.CLASSES);
			setAliasForURLbasedRunner(tmpHolderIndex, tmpHolder);
			rdfHolder.getSubRdfHolders().add(tmpHolder);
		}
		RdfTriple triple = new RdfTriple();
		triple.setSubject(subject);
		String object = sol.get("t").toString();
		triple.setObject(object);
		triple.setPredicate(RDF_TYPE);

		triple.setRmValue("1");
		triple.setRdfAttribute(object);
		triple.setRmAttribute(attrPrefix + attrSeparator + object);
		tmpHolder.getTriples().add(triple);

		String tripleStr = "<" + subject + ">\t<" + triple.getPredicate()
				+ ">\t<" + object + "> .\n";
		tmpHolder.setRawData(tmpHolder.getRawData() + tripleStr);

	}

	private String getSPARQLQuery(String uri) {
		String query = "SELECT DISTINCT ?t WHERE {<" + uri
				+ "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t .}";
		return createFilteredQuery(query, "?t", null);
	}

	@Override
	protected String getSPARQLQueryOutgoing(String uri) {
		return null;
	}

	@Override
	protected String getSPARQLQueryIncoming(String uri) {
		return null;
	}

	// @Override
	// public List<ParameterType> getParameterTypes() {
	// List<ParameterType> types = super.getParameterTypes();
	//
	// types.add(new ParameterTypeConfigurable(PARAMETER_SPARQL_MANAGER,
	// "Choose SPARQL endpoint connection",
	// SPARQLConfigurator.I18N_BASE_KEY));
	//
	// return types;
	// }

	@Override
	public OntologyHierarchy generateHierarchy(
			Map<String, String> addedAttributeOverall,
			SPARQLEndpointQueryRunner localQueryRunner, boolean inferEndpoint)
			throws OperatorException {
		OntologyHierarchy hierarchy = new OntologyHierarchy();
		Map<String, List<String>> overallSuperClasses = new HashMap<String, List<String>>();
		List<String> allAttributesList = new ArrayList<String>();
		allAttributesList.addAll(addedAttributeOverall.keySet());
		for (Entry entry : addedAttributeOverall.entrySet()) {
			if (inferEndpoint)
				localQueryRunner.setEndpoint(getSparqlFromURI((String) entry
						.getKey()));
			// get all super classes that are also in the feature space
			List<String> superClasses = new LinkedList<String>();
			List<String> directSuperClasses = new LinkedList<String>();
			try {
				superClasses = localQueryRunner.getSuperClasses(
						(String) entry.getKey(), allAttributesList,
						localQueryRunner.GET_SUPERCLASSES_QUERY);
				// get all direct classes that are in the feature space
				directSuperClasses = localQueryRunner.getSuperClasses(
						(String) entry.getKey(), allAttributesList,
						localQueryRunner.GET_DIRECT_SUPERCLASSES_QUERY);
				overallSuperClasses.put((String) entry.getKey(), superClasses);
			} catch (OperatorException e) {
				if (!inferEndpoint) {
					throw new OperatorException(e.getLocalizedMessage());
				}
			}
			String supperClassesAppended = "";

			// create a string for RM output
			for (String superClass : superClasses) {
				supperClassesAppended += superClass + ", ";
			}
			if (supperClassesAppended.length() > 2) {
				supperClassesAppended = supperClassesAppended.substring(0,
						supperClassesAppended.length() - 2);
			}
			// create the hierarchy pair
			HierarchyPair pair = new HierarchyPair((String) entry.getKey(),
					supperClassesAppended);
			// set all superclasses
			pair.setSuperClasses(superClasses);
			// set all direct classes
			pair.setDirectSuperClasses(directSuperClasses);

			// set the correspodning feature name from the exampleTable
			List<String> correspondAttr = new ArrayList<String>();
			correspondAttr.add((String) entry.getValue());
			pair.setCorrespondingAttr(correspondAttr);
			hierarchy.addNewPair(pair);
		}

		for (HierarchyPair pair : hierarchy.getHierarchyPairs()) {
			List<String> subClasses = localQueryRunner
					.getSubClasses(pair.getBaseClass(), allAttributesList,
							overallSuperClasses);
			pair.setSubClasses(subClasses);
			if (subClasses.size() == 0)
				pair.setLeaf(true);
		}
		return hierarchy;
	}

	public static String getSparqlFromURI(String uri) {
		String endpoint = uri.replace("http://", "");
		if (endpoint.contains("/"))
			endpoint = endpoint.substring(0, endpoint.indexOf("/"));
		endpoint = "http://" + endpoint + "/sparql";
		return endpoint;

	}

	public static void main(String[] args) {
		System.out
				.println(getSparqlFromURI("http://dbpedia.org/class/yago/Object100002684"));
	}
}
