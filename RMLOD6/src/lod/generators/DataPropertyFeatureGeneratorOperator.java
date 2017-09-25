package lod.generators;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lod.dataclasses.DataPropertyInstanceWrapper;
import lod.dataclasses.DataPropertyRecord;
import lod.rdf.model.RdfHolder;
import lod.rdf.model.RdfTriple;
import lod.rdf.model.RdfHolder.GeneratorType;
import lod.sparql.SPARQLEndpointQueryRunner;
import lod.sparql.SPARQLQueryRunner.QuerryRunnerType;
import lod.sparql.URLBasedQueryRunner;
import lod.utils.AttributeTypeGuesser;
import lod.utils.AttributeTypeGuesser.attributeType;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.rapidminer.example.Example;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;

/**
 * Data Properties generator
 * 
 * @author Heiko Paulheim
 * @author Evgeny Mitichkin
 * @author Petar Ristoski
 * 
 */
public class DataPropertyFeatureGeneratorOperator extends BaseGenerator {

	private static final String CLASS_NAME = "data_property_feature_generator_operator";
	private static final String PARAMETER_DATA_TYPE = "Data properties type";

	// data properties types options
	public static final String[] DATA_TYPE_OPTIONS = new String[] {
			"All data type properties",
			"All data type properties excluding Strings",
			"All data type properties including object properties as strings" };

	private AttributeTypeGuesser attributeTypeGuesser = new AttributeTypeGuesser();
	private String splitter = "\\^";

	public DataPropertyFeatureGeneratorOperator(OperatorDescription description) {
		super(description);
	}

	private Map<String, DataPropertyRecord> uniqueAtts;

	private int tmpHolderIndex;

	@Override
	public void doWork() throws OperatorException {
		this.uniqueAtts = new HashMap<String, DataPropertyRecord>();
		initAttributesAndParams(false, CLASS_NAME);

		// get the data properties types
		int selectedDataPropertiesTypes = getParameterAsInt(PARAMETER_DATA_TYPE);
		attrSeparator = "_data_";
		boolean isUrlBased = false;
		if (queryRunner.getRunnerType() == QuerryRunnerType.URLBASED) {
			isUrlBased = true;
		}
		for (int i = 0; i < attrsBypsass.size(); i++) {
			tmpHolderIndex = i;
			ArrayList<DataPropertyInstanceWrapper> entityLinkCorespondence = new ArrayList<DataPropertyInstanceWrapper>();
			String attributeName = attrsBypsass.get(i);
			attrPrefix = attributeName;
			int valueId = 0;

			boolean processInterrupted = false;

			// if it is url based restart the runner
			if (isUrlBased) {
				queryRunner = new URLBasedQueryRunner(null);
				queryRunner.setRunnerType(QuerryRunnerType.URLBASED);
			}
			boolean anythingIsAdded = false;

			if (attributeName.contains("yago")
					|| attributeName.contains("openei.org")
					|| attributeName.contains("linkedgeodata"))

			{
				queryRunner = new SPARQLEndpointQueryRunner(
						SimpleTypeFeatureGeneratorOperator.getSparqlFromURI(exampleSet
								.getExample(0).getValueAsString(
										attrs.get(attributeName))),
						"backupRunner", 60000, 10, 10000, true, true);
			}

			for (Example ex : exampleSet) {
				// if we are using url model, then we want to check if any
				// results
				// were added if not, we will change it to sparql endpoint
				String value = ex.getValueAsString(attrs.get(attributeName));
				if (value == null || value.equals("?"))
					value = null;

				ArrayList<DataPropertyRecord> classes = processInstance(value,
						selectedDataPropertiesTypes);

				// something was added so we don't change the query runner
				// if (classes != null && classes.size() > 0) {
				// anythingIsAdded = true;
				// }
				//
				// // if nothing was added and we are using url based model, we
				// // should switch to sparql
				// if (anythingIsAdded == false && value != null)
				// if (queryRunner.getRunnerType() == QuerryRunnerType.URLBASED
				// && (classes == null || classes.size() == 0)) {
				// queryRunner = new SPARQLEndpointQueryRunner(
				// SimpleTypeFeatureGeneratorOperator.getSparqlFromURI(ex
				// .getValueAsString(attrs
				// .get(attributeName))),
				// "backupRunner", 60000, 10, 10000, true, true);
				// classes = processInstance(
				// ex.getValueAsString(attrs.get(attributeName)),
				// selectedDataPropertiesTypes);
				//
				// }
				if (!queryRunner.mUIThreadRunning) {
					processInterrupted = true;
					break;
				}

				DataPropertyInstanceWrapper valueClassPair = new DataPropertyInstanceWrapper(
						ex.getValueAsString(attrs.get(attributeName)), valueId,
						classes);
				entityLinkCorespondence.add(valueClassPair);
				valueId++;
			}
			if (processInterrupted)
				break;
			// determining a set of uniqie attributes and adding them
			Collection<DataPropertyRecord> uniqueAttributesToAdd = uniqueAtts
					.values();
			uniqueAtts = new HashMap<String, DataPropertyRecord>();

			for (DataPropertyRecord rec : uniqueAttributesToAdd) {
				String attrName = rec.getName();
				if (!addedAttributeOverall.containsKey(attrName))
					addedAttributeOverall.put(attrName, attributeName
							+ "_data_" + attrName);
				addAtribute(attributeName + "_data_" + attrName,
						rec.getOntologyType(), exampleSet);
				int h = 0;
				// Looking for the values for this attribute and setting them
				for (Example ex : exampleSet) {
					// for every record retrieving the set of values
					DataPropertyInstanceWrapper vpair = entityLinkCorespondence
							.get(h);
					ArrayList<DataPropertyRecord> records = vpair.getClasses();
					// checking if the example record has a type from entities
					boolean typeExists = false;
					for (DataPropertyRecord recrd : records) {
						String recName = recrd.getName();
						if (attrName.equals(recName)) {
							typeExists = true;

							double value = AttributeTypeGuesser
									.getValueForAttribute(attrs
											.get(attributeName + "_data_"
													+ attrName), recrd
											.getValue().split(splitter)[0]);
							ex.setValue(
									attrs.get(attributeName + "_data_"
											+ attrName), value);
							break;
						}
					}
					if (!typeExists) {
						ex.setValue(
								attrs.get(attributeName + "_data_" + attrName),
								AttributeTypeGuesser.getValueForAttribute(
										attrs.get(attributeName + "_data_"
												+ attrName), null));
					}
					h++;
				}
			}
			// /////END

		}
		mOutputPort.deliver(exampleSet);
		if (createRDF) {
			rdfHolder.getGeneratorTypes().add(GeneratorType.DATA);
			rdfHolder.populateHolderFromSubHolders();
		}
		mOutputPortRdfData.deliver(rdfHolder);
		// hierarchy = generateHierarchy(addedAttributeOverall);
		// mOutputPortTypesHierarchy.deliver(hierarchy);
		super.doWork();
	}

	protected ArrayList<DataPropertyRecord> processInstance(
			String attributeToExpandValue, int selectedDataTypes)
			throws OperatorException {
		String uri = attributeToExpandValue;
		ArrayList<DataPropertyRecord> result = new ArrayList<DataPropertyRecord>();
		int offset = 0;
		try {
			if (queryRunner.getRunnerType() == QuerryRunnerType.URLBASED) {
				queryRunner.updateModel(uri);
			}
			Query queryQ = QueryFactory.create(getSPARQLQuery(uri,
					selectedDataTypes));
			if (queryRunner.getPageSize() > 0) {
				queryQ = SPARQLEndpointQueryRunner.addOrderByToQuery(queryQ
						.toString());
				queryQ.setLimit(queryRunner.getPageSize());
			}

			ResultSet RS = queryRunner.runSelectQueryInterruptable(queryQ
					.toString());

			if (queryRunner.mUIThreadRunning) {
				while (true) {
					while (RS != null && RS.hasNext()) {
						QuerySolution sol = RS.next();
						String attName = sol.get("p").toString();

						int ontologyType = Ontology.STRING;
						String value = "";
						if (sol.get("v").isLiteral()) {
							Literal attValueLiteral = sol.getLiteral("v");

							ontologyType = attributeTypeGuesser
									.getLiteralType(attValueLiteral);
							value = attValueLiteral.toString();
						} else {
							value = sol.get("v").toString();
						}
						DataPropertyRecord rec = new DataPropertyRecord(
								attName, value, ontologyType);
						result.add(rec);
						uniqueAtts.put(attName, rec);

						// add in the RDF holder if needed

						addNewTripleInRdfHolder(sol, uri);

					}
					if (queryRunner.getPageSize() == 0)
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
			tmpHolder.getGeneratorTypes().add(GeneratorType.DATA);
			setAliasForURLbasedRunner(tmpHolderIndex, tmpHolder);
			rdfHolder.getSubRdfHolders().add(tmpHolder);
		}

		RdfTriple triple = new RdfTriple();
		triple.setSubject(subject);
		String object = "";
		if (sol.get("v").isLiteral())
			object = sol.getLiteral("v").asNode().toString();
		else
			object = sol.get("v").toString();
		triple.setObject(object);
		String predicate = sol.get("p").toString();
		triple.setPredicate(predicate);
		if (sol.get("v").isLiteral())
			triple.setRmValue(sol.getLiteral("v").toString());
		else
			triple.setRmValue(object);
		triple.setRdfAttribute(predicate);
		triple.setRmAttribute(attrPrefix + attrSeparator + predicate);
		tmpHolder.getTriples().add(triple);

		String tripleStr = "<" + subject + ">\t<" + predicate + ">\t" + object
				+ " .\n";
		tmpHolder.setRawData(tmpHolder.getRawData() + tripleStr);

	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeCategory(PARAMETER_DATA_TYPE,
				"Data properties type", DATA_TYPE_OPTIONS, 0, false));

		// remove the vector represenatation parameter
		int indexToremove = 0;
		for (ParameterType t : types) {
			if (t.getKey().equals(PARAMETER_VECTOR_REPRESENTATION))
				break;
			indexToremove++;
		}
		types.remove(indexToremove);

		return types;
	}

	private String getSPARQLQuery(String uri, int selectedDataPropertiesTypes) {
		String filters = "";
		switch (selectedDataPropertiesTypes) {
		case 0:
			filters += " FILTER(isLITERAL(?v)).";
			break;
		case 1:
			filters += " FILTER(isLITERAL(?v) && DATATYPE(?v)!=<http://www.w3.org/2001/XMLSchema#string>).";
			break;
		case 2:
			break;
		default:
			break;
		}

		String query = "SELECT ?p ?v WHERE {<" + uri + "> ?p ?v" + filters
				+ "}";
		return createFilteredQuery(query, "?p", null);
	}

	@Override
	public String getSPARQLQueryOutgoing(String uri) {
		return null;
	}

	@Override
	public String getSPARQLQueryIncoming(String uri) {
		return null;
	}

}
