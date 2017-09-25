package lod.generators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lod.dataclasses.ValueClassesPair;
import lod.gui.tools.dialogs.SPARQLConfigurator;
import lod.sparql.SPARQLEndpointQueryRunner;
import lod.sparql.SPARQLQueryRunner.QuerryRunnerType;
import lod.utils.AttributeTypeGuesser;
import lod.utils.AttributeTypeGuesser.attributeType;
import lod.utils.LOD2ExampleSet;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeText;
import com.rapidminer.parameter.TextType;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.config.ParameterTypeConfigurable;

/**
 * Custom SPARQL generator
 * 
 * @author Petar Ristoski
 * @author Evgeny Mitichkin
 * 
 */
public class CustomFeatureGeneratorOperator extends BaseGenerator {

	private static final String CLASS_NAME = "custom_feature_generator_operator";

	private AttributeTypeGuesser attributeTypeGuesser = new AttributeTypeGuesser();
	private String splitter = "\\^";

	public static final String PARAMETER_QUERY = "SPARQL query";

	public static final String PARAMETER_ROW_TYPES = "Attribute generation strategy";

	public static final String[] ROW_TYPES_VALUES = new String[] {
			"First result only", "Multiple entities",
			"Set of boolean attributes" };

	public static final String EXTEND_ALL_ATTRS = "ATT_INPUT_PORT";

	public CustomFeatureGeneratorOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	public void doWork() throws OperatorException {
		initAttributesAndParams(false, CLASS_NAME);
		uniqueAtts = new HashMap<String, Integer>();

		int selectedResultType = getParameterAsInt(PARAMETER_ROW_TYPES);

		exampleSet = mInputPort.getData(ExampleSet.class);
		// ExampleSet exampleSet = LOD2ExampleSet.cloneExampleSet(exampleSetIn);
		attrs = exampleSet.getAttributes();

		// read the passed attributes
		getAttsNames(attrNames, attrsBypsass);

		String sparqlQuery = getParameterAsString(PARAMETER_QUERY);

		// get the attribute to be extended
		String attributeName = getAttrNameToExpand(sparqlQuery, attrsBypsass);
		List<Attribute> attributesToExtend = new ArrayList<Attribute>();
		if (attributeName.equals(EXTEND_ALL_ATTRS)) {
			for (String attName : attrsBypsass) {
				attributesToExtend.add(attrs.get(attName));
			}
		} else {
			attributesToExtend.add(attrs.get(attributeName));
		}

		// if we don't want boolean types
		if (selectedResultType != 2) {
			// add new attributes to the ExampleSet
			List<String> newAtts = addNewAttributes(attrs,
					exampleSet.getExample(0), sparqlQuery, attributesToExtend,
					exampleSet);
			// add the values for the new attributes
			List<DataRow> newExamples = new ArrayList<DataRow>();
			for (Example ex : exampleSet) {
				processInstance(attributesToExtend, sparqlQuery, ex,
						newExamples, exampleSet, newAtts);

			}

			if (selectedResultType == 0) {
				mOutputPort.deliver(exampleSet);

			} else if (selectedResultType == 1) {
				ExampleSet outSet = LOD2ExampleSet.addDataToExampleSet(
						newExamples, exampleSet);

				mOutputPort.deliver(outSet);
			}
		} else {// add boolean values
			// concept URI with the corresponding attribute name
			Map<String, String> addedAttributeOverall = new HashMap<String, String>();
			for (Attribute attributeToExtend : attributesToExtend) {
				attributeName = attributeToExtend.getName();
				int valueId = 0;
				ArrayList<ValueClassesPair> entityLinkCorespondence = new ArrayList<ValueClassesPair>();
				boolean processInterrupted = false;
				for (Example ex : exampleSet) {
					ArrayList<String> classes = processInstance(
							ex.getValueAsString(attributeToExtend), sparqlQuery);
					if (!queryRunner.mUIThreadRunning) {
						processInterrupted = true;
						break;
					}
					ValueClassesPair valueClassPair = new ValueClassesPair(
							ex.getValueAsString(attrs.get(attributeName)),
							valueId, classes);
					entityLinkCorespondence.add(valueClassPair);
					valueId++;
				}
				if (processInterrupted)
					break;
				// determining a set of unique attributes and adding them
				Set<String> uniqueAttributesToAdd = uniqueAtts.keySet();
				uniqueAtts = new HashMap<String, Integer>();

				// adding attributes
				for (String attrName : uniqueAttributesToAdd) {
					attrName = attrName.replaceAll("\n", "");
					boolean shoudlChange = false;
					if (addedAttributeOverall.containsKey(attrName)) {
						shoudlChange = true;
					} else {
						addedAttributeOverall.put(attrName, attrName);
						addAtribute(attrName, Ontology.BINOMINAL, exampleSet);
					}
					int h = 0;
					// Looking for the values for this attribute and setting
					// them
					for (Example ex : exampleSet) {
						// for every record retrieving the set of values
						ValueClassesPair vpair = entityLinkCorespondence.get(h);
						ArrayList<String> entities = vpair.getClasses();
						// checking if the example record has a type from
						// entities
						boolean typeExists = false;
						for (String type : entities) {
							if (attrName.equals(type)) {
								typeExists = true;
								break;
							}
						}
						if (shoudlChange) {
							if (typeExists) {
								ex.setValue(attrs.get(attrName),
										AttributeTypeGuesser
												.getValueForAttribute(attrs
														.get(attrName), Boolean
														.toString(typeExists)));
							}
						} else {
							ex.setValue(attrs.get(attrName),
									AttributeTypeGuesser.getValueForAttribute(
											attrs.get(attrName),
											Boolean.toString(typeExists)));

						}
						h++;
					}
				}
			}
			mOutputPort.deliver(exampleSet);
			// OntologyHierarchy hierarchy =
			// generateHierarchy(addedAttributeOverall);
			// FmOutputPortTypesHierarchy.deliver(hierarchy);

		}

		super.doWork();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeConfigurable(PARAMETER_SPARQL_MANAGER,
				"Choose SPARQL endpoint connection",
				SPARQLConfigurator.I18N_BASE_KEY));

		types.add(new ParameterTypeText(PARAMETER_QUERY, "The SPARQL Query.",
				TextType.SQL, false));

		types.add(new ParameterTypeCategory(PARAMETER_ROW_TYPES,
				"Select the schema for creating the word vector",
				ROW_TYPES_VALUES, 0, false));

		return types;
	}

	/**
	 * Adds the new attributes to the ExampleSet
	 * 
	 * @param attrs
	 * @param ex
	 * @param sparqlQuery
	 * @param attributeToExtend
	 * @param exampleSet
	 * @throws OperatorException
	 */
	protected List<String> addNewAttributes(Attributes attrs, Example ex,
			String sparqlQuery, List<Attribute> attributesToExtend,
			ExampleSet exampleSet) throws OperatorException {
		List<String> newAtts = new ArrayList<String>();
		for (Attribute attributeToExtend : attributesToExtend) {
			String uri = ex.getValueAsString(attributeToExtend);
			if (queryRunner.getRunnerType() == QuerryRunnerType.URLBASED) {
				queryRunner.updateModel(uri);
			}
			try {
				ResultSet RS = queryRunner
						.runSelectQueryInterruptable(getSPARQLQuery(uri,
								sparqlQuery));
				if (queryRunner.mUIThreadRunning) {
					if (RS != null && RS.hasNext()) {
						QuerySolution firstRow = RS.next();
						// String entity = solution.get("s").toString();

						for (String var : RS.getResultVars()) {
							if (firstRow.get(var) != null
									&& firstRow.get(var).isLiteral()) {

								Attribute newAttr = AttributeFactory
										.createAttribute(
												attributeToExtend.getName()
														+ "_" + var,
												attributeTypeGuesser
														.getLiteralType(firstRow
																.getLiteral(var)));
								attrs.addRegular(newAttr);
								exampleSet.getExampleTable().addAttribute(
										newAttr);
								newAtts.add(newAttr.getName());
							} else {
								Attribute newAttr = AttributeFactory
										.createAttribute(
												attributeToExtend.getName()
														+ "_" + var,
												Ontology.STRING);
								attrs.addRegular(newAttr);
								exampleSet.getExampleTable().addAttribute(
										newAttr);
								newAtts.add(newAttr.getName());
							}
						}
					} else {
						if (RS != null) {
							for (String var : RS.getResultVars()) {
								Attribute newAttr = AttributeFactory
										.createAttribute(
												attributeToExtend.getName()
														+ "_" + var,
												Ontology.STRING);
								attrs.addRegular(newAttr);
								exampleSet.getExampleTable().addAttribute(
										newAttr);
								newAtts.add(newAttr.getName());
							}
						}
					}
				} else {
					return null;
				}
			} catch (UndefinedParameterError e) {
				e.printStackTrace();
			}
		}
		return newAtts;

	}

	/**
	 * Issues a SPARQL query and populates the values of the example
	 * 
	 * @param attributeToExtend
	 * @param sparqlQuery
	 * @param ex
	 * @throws OperatorException
	 */
	protected void processInstance(List<Attribute> attributesToExtend,
			String sparqlQuery, Example ex, List<DataRow> newExamples,
			ExampleSet exampleSet, List<String> newAtts)
			throws OperatorException {
		List<String> addedAtts = new ArrayList<String>();

		for (Attribute attributeToExtend : attributesToExtend) {
			String uri = ex.getValueAsString(attributeToExtend);
			if (queryRunner.getRunnerType() == QuerryRunnerType.URLBASED) {
				queryRunner.updateModel(uri);
			}
			int offset = 0;
			try {
				String replacedQuery = getSPARQLQuery(uri, sparqlQuery);
				// get the query and execute it
				// resovle prefixes
				try {
					replacedQuery = lod.utils.PrefixResolver.resolveQuery(
							replacedQuery, cachedPrefixes);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Query queryQ = QueryFactory.create(replacedQuery);
				if (queryRunner.getPageSize() > 0) {
					// queryQ = SPARQLEndpointQueryRunner
					// .addOrderByToQuery(replacedQuery);
					queryQ.setLimit(queryRunner.getPageSize());
				}

				ResultSet RS = queryRunner.runSelectQueryInterruptable(queryQ
						.toString());
				if (queryRunner.mUIThreadRunning) {
					// if there are no results for this entry write all
					// attributes
					// as
					// missing
					if (RS != null && !RS.hasNext()) {
						double value = Double.NaN;
						for (String var : RS.getResultVars()) {
							ex.setValue(
									ex.getAttributes().get(
											attributeToExtend.getName() + "_"
													+ var), value);

						}
						DataRow row = LOD2ExampleSet.getDataRowFromExample(ex);
						newExamples.add(row);
					}
					boolean mainLoopInterrupted = false;
					while (true) {
						offset += queryRunner.getPageSize();
						if (RS == null || !RS.hasNext())
							break;
						while (RS.hasNext()) {

							QuerySolution solution = RS.next();
							for (String var : RS.getResultVars()) {
								double value = Double.NaN;
								// if the attribute is literal then use the
								// primitve
								// type
								if (solution.get(var) != null) {
									if (solution.get(var).isLiteral()) {
										String strValue = "";
										// check if it is not a string
										if (attributeTypeGuesser
												.getLiteralType(solution
														.getLiteral(var)) != Ontology.NUMERICAL) {
											strValue = solution.getLiteral(var)
													.toString().split(splitter)[0];
										} else {
											strValue = solution.getLiteral(var)
													.getString();
										}
										// get the double value
										value = AttributeTypeGuesser
												.getValueForAttribute(
														ex.getAttributes()
																.get(attributeToExtend
																		.getName()
																		+ "_"
																		+ var),
														strValue);
									} else {// otherwise use it as a string
										value = AttributeTypeGuesser
												.getValueForAttribute(
														ex.getAttributes()
																.get(attributeToExtend
																		.getName()
																		+ "_"
																		+ var),
														solution.get(var)
																.toString());
									}
								}
								ex.setValue(
										ex.getAttributes().get(
												attributeToExtend.getName()
														+ "_" + var), value);

								addedAtts.add(attributeToExtend.getName() + "_"
										+ var);
							}
							for (String newAtt : newAtts) {
								if (!addedAtts.contains(newAtt)) {
									ex.setValue(ex.getAttributes().get(newAtt),
											Double.NaN);
								}
							}
							DataRow row = LOD2ExampleSet
									.getDataRowFromExample(ex);
							newExamples.add(row);
						}

						if (queryRunner.getPageSize() == 0)
							break;
						queryQ.setOffset(offset);
						queryQ.setLimit(queryRunner.getPageSize());

						RS = queryRunner.runSelectQueryInterruptable(queryQ
								.toString());
						if (queryRunner.mUIThreadRunning) {
							if (RS == null || !RS.hasNext())
								break;
						} else {
							mainLoopInterrupted = true;
							break;
						}
					}
					if (mainLoopInterrupted)
						break;
				} else {
					break;
				}
			} catch (UndefinedParameterError e) {
				e.printStackTrace();
			}
		}
	}

	protected ArrayList<String> processInstance(String attributeToExpandValue,
			String sparqlQuery) throws OperatorException {
		ArrayList<String> result = new ArrayList<String>();
		String uri = attributeToExpandValue;
		int offset = 0;
		try {
			String replacedQuery = getSPARQLQuery(uri, sparqlQuery);
			if (queryRunner.getRunnerType() == QuerryRunnerType.URLBASED) {
				queryRunner.updateModel(uri);
			}
			// get the query and execute it
			// resovle prefixes
			try {
				replacedQuery = lod.utils.PrefixResolver.resolveQuery(
						replacedQuery, cachedPrefixes);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Query queryQ = QueryFactory.create(replacedQuery);
			if (queryRunner.getPageSize() > 0) {
				// queryQ = SPARQLEndpointQueryRunner
				// .addOrderByToQuery(replacedQuery);
				queryQ.setLimit(queryRunner.getPageSize());
			}

			ResultSet RS = queryRunner.runSelectQueryInterruptable(queryQ
					.toString());
			if (queryRunner.mUIThreadRunning) {
				while (true) {
					offset += queryRunner.getPageSize();
					if (RS == null || !RS.hasNext())
						break;
					while (RS.hasNext()) {
						QuerySolution solution = RS.next();
						for (String var : RS.getResultVars()) {
							String strValue = "";
							// if the attribute is literal then use the primitve
							// type
							if (solution.get(var) != null) {
								if (solution.get(var).isLiteral()) {

									// check if it is not a string
									if (attributeTypeGuesser
											.getLiteralType(solution
													.getLiteral(var)) != Ontology.NUMERICAL) {
										strValue = solution.getLiteral(var)
												.toString().split(splitter)[0];
									} else {
										strValue = solution.getLiteral(var)
												.getString();
									}

								} else {// otherwise use it as a string
									strValue = solution.get(var).toString();
								}
							}
							String attName = var + "_" + strValue;
							result.add(attName);
							uniqueAtts.put(attName, 0);
						}
					}

					if (queryRunner.getPageSize() == 0)
						break;
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

	/**
	 * Returns the attribute name that needs to be used for the SPARQL query
	 * 
	 * @param query
	 * @param attrs
	 * @return
	 * @throws OperatorException
	 */
	private String getAttrNameToExpand(String query, ArrayList<String> attrs)
			throws OperatorException {
		boolean throwExeception = false;
		String attrNameToExpand = "";

		Pattern pattern = Pattern.compile("\\*(\\w+)\\*");
		Matcher matcher = pattern.matcher(query);
		if (matcher.find()) {
			attrNameToExpand = matcher.group(0); // prints /{item}/
			attrNameToExpand = attrNameToExpand.replaceAll("\\*", "");
			try {

				if (!attrs.contains(attrNameToExpand)
						&& !attrNameToExpand.equals(EXTEND_ALL_ATTRS)) {
					throwExeception = true;
				}
			} catch (Exception e) {
				e.printStackTrace();
				throwExeception = true;
			}
		} else {
			throw new UserError(this, 1004, CLASS_NAME);
		}
		if (throwExeception)
			throw new OperatorException(
					"Problem in CustomFeaturesGenerator: The specified attribute '"
							+ attrNameToExpand
							+ "' doesn't exist in the ExampleSet");
		return attrNameToExpand;
	}

	/**
	 * Returns the sparql query with replaced tags
	 * 
	 * @param uri
	 * @param query
	 * @return
	 */
	private String getSPARQLQuery(String uri, String query) {
		Pattern pattern = Pattern.compile("\\*(\\w+)\\*");
		Matcher matcher = pattern.matcher(query);
		if (matcher.find()) {
			String toBeReplaced = matcher.group(0);
			if (uri.startsWith("http"))
				query = query.replace(toBeReplaced, "<" + uri + ">");
			else
				query = query.replace(toBeReplaced, "\"" + uri + "\"");
		}
		return query;
	}

	@Override
	protected String getSPARQLQueryOutgoing(String uri) {
		return null;
	}

	@Override
	protected String getSPARQLQueryIncoming(String uri) {
		return null;
	}

}
