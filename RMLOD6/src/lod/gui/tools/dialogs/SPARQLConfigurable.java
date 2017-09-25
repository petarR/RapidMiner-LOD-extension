package lod.gui.tools.dialogs;

import java.util.HashMap;
import java.util.Map;

import lod.sparql.FileBasedQueryRunner;
import lod.sparql.SPARQLEndpointQueryRunner;
import lod.sparql.SPARQLQueryRunner.QuerryRunnerType;
import lod.sparql.URLBasedQueryRunner;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.config.AbstractConfigurable;

public class SPARQLConfigurable extends AbstractConfigurable {

	@Override
	public String getTypeId() {
		// TODO Auto-generated method stub
		return SPARQLConfigurator.TYPE_ID;
	}

	public int getTypeOfRunner() {
		int runnerType = 0;
		String selectedType = getParameter(SPARQLConfigurator.RUNNER_TYPE);
		for (int i = 0; i < SPARQLConfigurator.READER_TYPE_OPTIONS.length; i++) {
			if (selectedType.equals(SPARQLConfigurator.READER_TYPE_OPTIONS[i])) {
				runnerType = i;
				break;
			}
		}
		return runnerType;
	}

	public SPARQLEndpointQueryRunner getTheRunnerFromDialog(
			SPARQLEndpointQueryRunner currentRunner) throws OperatorException {

		int runnerType = 0;
		String selectedType = getParameter(SPARQLConfigurator.RUNNER_TYPE);
		for (int i = 0; i < SPARQLConfigurator.READER_TYPE_OPTIONS.length; i++) {
			if (selectedType.equals(SPARQLConfigurator.READER_TYPE_OPTIONS[i])) {
				runnerType = i;
				break;
			}
		}
		String alias = getParameter(SPARQLConfigurator.PARAMETER_LINK_ANNOTATION);
		switch (runnerType) {
		case 0:
			String enpoint = getParameter(SPARQLConfigurator.PARAMETER_ENDPOINT);

			int pageSize = Integer
					.parseInt(getParameter(SPARQLConfigurator.PARAMETER_PAGE_SIZE));
			int timeout = Integer
					.parseInt(getParameter(SPARQLConfigurator.PARAMETER_TIMEOUT));
			int retries = Integer
					.parseInt(getParameter(SPARQLConfigurator.PARAMETER_RETRIES));

			boolean isCount = Boolean
					.parseBoolean(getParameter(SPARQLConfigurator.USE_COUNT));
			boolean isPropertyPaths = Boolean
					.parseBoolean(getParameter(SPARQLConfigurator.USE_PROPERTY_PATHS));

			SPARQLEndpointQueryRunner runner = new SPARQLEndpointQueryRunner(
					enpoint, alias, timeout, retries, pageSize, isCount,
					isPropertyPaths);
			runner.setRunnerType(QuerryRunnerType.ENDPOINTBASED);
			return runner;

		case 1:
			String localFile = getParameter(SPARQLConfigurator.LOCAL_ENDPOINT_FILE);
			String localSchemaFile = getParameter(SPARQLConfigurator.LOCAL_ENDPOINT_SCHEMA_FILE);
			int reasonerIndex = 0;

			selectedType = getParameter(SPARQLConfigurator.LOCAL_REASONER);
			for (int i = 0; i < SPARQLConfigurator.REASONER_OPTIONS.length; i++) {
				if (selectedType.equals(SPARQLConfigurator.REASONER_OPTIONS[i])) {
					reasonerIndex = i;
					break;
				}
			}
			return FileBasedQueryRunner.getRunner(localFile, localSchemaFile,
					reasonerIndex, alias);
		default:
			URLBasedQueryRunner uRunner = new URLBasedQueryRunner(null);
			uRunner.setRunnerType(QuerryRunnerType.URLBASED);
			return uRunner;
		}
	}

	public static Map<String, String> getDBpediaDefaultConnection() {

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(SPARQLConfigurator.PARAMETER_ENDPOINT,
				"http://dbpedia.org/sparql");
		parameters.put(SPARQLConfigurator.PARAMETER_LINK_ANNOTATION, "DBpedia");
		parameters.put(SPARQLConfigurator.PARAMETER_PAGE_SIZE, "10000");
		parameters.put(SPARQLConfigurator.PARAMETER_TIMEOUT, "60000");
		parameters.put(SPARQLConfigurator.PARAMETER_RETRIES, "10");

		parameters.put(SPARQLConfigurator.RUNNER_TYPE, "0");
		parameters.put(SPARQLConfigurator.LOCAL_ENDPOINT_FILE, "");
		parameters.put(SPARQLConfigurator.LOCAL_ENDPOINT_SCHEMA_FILE, "");
		parameters.put(SPARQLConfigurator.LOCAL_REASONER, "0");

		parameters.put(SPARQLConfigurator.USE_COUNT, "true");
		parameters.put(SPARQLConfigurator.USE_PROPERTY_PATHS, "true");

		return parameters;
	}

	public static Map<String, String> getEurostatConnection() {

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(SPARQLConfigurator.PARAMETER_ENDPOINT,
				"http://wifo5-04.informatik.uni-mannheim.de/eurostat/sparql");
		parameters
				.put(SPARQLConfigurator.PARAMETER_LINK_ANNOTATION, "Eurostat");
		parameters.put(SPARQLConfigurator.PARAMETER_PAGE_SIZE, "0");
		parameters.put(SPARQLConfigurator.PARAMETER_TIMEOUT, "60000");
		parameters.put(SPARQLConfigurator.PARAMETER_RETRIES, "10");

		parameters.put(SPARQLConfigurator.RUNNER_TYPE, "0");
		parameters.put(SPARQLConfigurator.LOCAL_ENDPOINT_FILE, "");
		parameters.put(SPARQLConfigurator.LOCAL_ENDPOINT_SCHEMA_FILE, "");
		parameters.put(SPARQLConfigurator.LOCAL_REASONER, "0");

		parameters.put(SPARQLConfigurator.USE_COUNT, "true");
		parameters.put(SPARQLConfigurator.USE_PROPERTY_PATHS, "false");

		return parameters;
	}

	public static Map<String, String> getURLBasedConnection() {

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(SPARQLConfigurator.PARAMETER_ENDPOINT, "");
		parameters.put(SPARQLConfigurator.PARAMETER_LINK_ANNOTATION,
				"URLbasedModel");
		parameters.put(SPARQLConfigurator.PARAMETER_PAGE_SIZE, "0");
		parameters.put(SPARQLConfigurator.PARAMETER_TIMEOUT, "0");
		parameters.put(SPARQLConfigurator.PARAMETER_RETRIES, "0");

		parameters.put(SPARQLConfigurator.RUNNER_TYPE, "2");
		parameters.put(SPARQLConfigurator.LOCAL_ENDPOINT_FILE, "");
		parameters.put(SPARQLConfigurator.LOCAL_ENDPOINT_SCHEMA_FILE, "");
		parameters.put(SPARQLConfigurator.LOCAL_REASONER, "0");

		parameters.put(SPARQLConfigurator.USE_COUNT, "true");
		parameters.put(SPARQLConfigurator.USE_PROPERTY_PATHS, "false");

		return parameters;
	}

}
