package lod.importers;

import java.util.HashMap;
import java.util.List;

import lod.gui.tools.dialogs.SPARQLConfigurable;
import lod.gui.tools.dialogs.SPARQLConfigurator;
import lod.sparql.SPARQLEndpointQueryRunner;
import lod.utils.AttributeTypeGuesser;
import lod.utils.AttributeTypeGuesser.attributeType;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeText;
import com.rapidminer.parameter.TextType;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.config.ConfigurationManager;
import com.rapidminer.tools.config.ParameterTypeConfigurable;

/**
 * 
 * @author Petar Ristoski
 * 
 */
public class LODDataImporter extends Operator {

	private static final String CLASS_NAME = "lod_data_importer";

	private String splitter = "\\^";

	private SPARQLEndpointQueryRunner queryRunner;

	private OutputPort mOutputPort;

	public static final String PARAMETER_QUERY = "SPARQL query";

	public static final String PARAMETER_SPARQL_MANAGER = "SPARQL connection";

	// used to cash prefixes
	protected HashMap<String, String> cachedPrefixes = new HashMap<String, String>();

	public LODDataImporter(OperatorDescription description) {
		super(description);

		mOutputPort = getOutputPorts().createPort("Example Set");
		getTransformer().addGenerationRule(mOutputPort, ExampleSet.class);

	}

	@Override
	public void doWork() throws OperatorException {
		// init the queryRunners
		try {
			queryRunner = SPARQLEndpointQueryRunner.initRunner(this,
					queryRunner);
		} catch (Exception e) {
			e.printStackTrace();
			throw new UserError(this, 2001, CLASS_NAME, e.getMessage());
		}

		String sparqlQuery = getParameterAsString(PARAMETER_QUERY);
		boolean withPagination = false;
		if (queryRunner.getPageSize() > 0)
			withPagination = true;
		MemoryExampleTable table = getMemoryTableForSPARQL(queryRunner,
				sparqlQuery, withPagination);
		mOutputPort.deliver(table.createExampleSet());
	}

	@Override
	public List<ParameterType> getParameterTypes() {

		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeConfigurable(PARAMETER_SPARQL_MANAGER,
				"Choose SPARQL endpoint connection",
				SPARQLConfigurator.I18N_BASE_KEY));
		types.add(new ParameterTypeText(PARAMETER_QUERY, "The SPARQL Query.",
				TextType.SQL, false));

		return types;
	}

	/**
	 * Generates complete MemoryTable for the given query
	 * 
	 * @param runner
	 * @param query
	 * @return
	 * @throws OperatorException
	 */
	private MemoryExampleTable getMemoryTableForSPARQL(
			SPARQLEndpointQueryRunner runner, String query,
			boolean withPagination) throws OperatorException {

		int offset = 0;
		// resovle prefixes
		try {
			query = lod.utils.PrefixResolver
					.resolveQuery(query, cachedPrefixes);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Query queryQ = QueryFactory.create(query);
		if (withPagination) {
			// queryQ = SPARQLEndpointQueryRunner.addOrderByToQuery(query);
			if (!query.toLowerCase().contains("limit"))
				queryQ.setLimit(runner.getPageSize());
			else
				withPagination = false;
		}
		AttributeTypeGuesser attributeTypeGuesser = new AttributeTypeGuesser();

		MemoryExampleTable table = null;

		ResultSet results = runner.runSelectQueryInterruptable(queryQ
				.toString());
		if (results != null && runner.mUIThreadRunning) {
			List<String> variables = results.getResultVars();
			Attribute[] attributes = new Attribute[variables.size()];

			if (results.hasNext()) {
				QuerySolution firstRow = results.next();

				int attrNm = 0;
				for (String var : variables) {
					if (firstRow.get(var) != null
							&& firstRow.get(var).isLiteral()) {
						attributes[attrNm] = AttributeFactory.createAttribute(
								var, attributeTypeGuesser
										.getLiteralType(firstRow
												.getLiteral(var)));
					} else {
						attributes[attrNm] = AttributeFactory.createAttribute(
								var, Ontology.STRING);
					}

					attrNm++;
				}
				table = new MemoryExampleTable(attributes);

				// add the first row for the first result row
				DoubleArrayDataRow rowFirst = getDataRow(variables, attributes,
						firstRow, attributeTypeGuesser);
				table.addDataRow(rowFirst);

				// add the rest of the rows
				while (results != null && results.hasNext()) {
					QuerySolution solution = results.next();
					DoubleArrayDataRow row = getDataRow(variables, attributes,
							solution, attributeTypeGuesser);
					table.addDataRow(row);
				}
				if (withPagination) {
					while (true) {
						offset += runner.getPageSize();
						queryQ.setOffset(offset);
						queryQ.setLimit(runner.getPageSize());

						results = runner.runSelectQueryInterruptable(queryQ
								.toString());

						if (runner.mUIThreadRunning) {
							if (results == null || !results.hasNext())
								break;
							while (results.hasNext()) {
								QuerySolution solution = results.next();
								DoubleArrayDataRow row = getDataRow(variables,
										attributes, solution,
										attributeTypeGuesser);
								table.addDataRow(row);
							}
						}
					}
				}

			} else {
				int attrNm = 0;
				for (String var : variables) {
					attributes[attrNm] = AttributeFactory.createAttribute(var,
							Ontology.STRING);
					attrNm++;
				}
				table = new MemoryExampleTable(attributes);
			}
		}

		return table;
	}

	/**
	 * generates new data row for the given values
	 * 
	 * @param variables
	 * @param attributes
	 * @param solution
	 * @param attributeTypeGuesser
	 * @return
	 */
	private DoubleArrayDataRow getDataRow(List<String> variables,
			Attribute[] attributes, QuerySolution solution,
			AttributeTypeGuesser attributeTypeGuesser) {
		DoubleArrayDataRow row = new DoubleArrayDataRow(
				new double[variables.size()]);
		int attrNm = 0;
		for (String var : variables) {
			double value = Double.NaN;
			if (solution.get(var) != null) {

				if (solution.get(var).isLiteral()) {
					String strValue = "";
					// if it is literal and it is not a string, split the value
					if (attributeTypeGuesser.getLiteralType(solution
							.getLiteral(var)) != Ontology.NUMERICAL) {
						strValue = solution.getLiteral(var).toString()
								.split(splitter)[0];
					} else {
						strValue = solution.getLiteral(var).getString();
					}
					value = AttributeTypeGuesser.getValueForAttribute(
							attributes[attrNm], strValue);
				} else { // if it is not a literal then use it as it is
					value = AttributeTypeGuesser.getValueForAttribute(
							attributes[attrNm], solution.get(var).toString());
				}
			}

			row.set(attributes[attrNm], value);
			attrNm++;
		}
		return row;
	}

	@Override
	public void processFinished() throws OperatorException {
		if (queryRunner != null) {
			queryRunner.mUIThreadRunning = false;
			queryRunner.finalizeAsyncThread();
		}
		super.processFinished();
	}
}
