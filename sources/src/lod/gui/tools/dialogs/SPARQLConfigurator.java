package lod.gui.tools.dialogs;

import java.util.ArrayList;
import java.util.List;

import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.config.Configurator;
import com.rapidminer.tools.config.gui.ConfigurationPanel;

public class SPARQLConfigurator extends Configurator<SPARQLConfigurable> {
	public static final String I18N_BASE_KEY = "sparqlconfig";

	public static final String TYPE_ID = "sparqlconfig";

	// types
	public static final String PARAMETER_LINK_ANNOTATION = "SPARQL_annotation";

	public static final String PARAMETER_ENDPOINT = "SPARQL_endpoint";

	public static final String PARAMETER_RETRIES = "SPARQL_query_retries";

	public static final String PARAMETER_TIMEOUT = "SPARQL_query_timeout";

	public static final String PARAMETER_PAGE_SIZE = "SPARQL_result_page_size";

	public static final String RUNNER_TYPE = "Runner_Type";

	public static final String LOCAL_ENDPOINT_FILE = "Local_file_name";

	public static final String LOCAL_ENDPOINT_SCHEMA_FILE = "Local_file_schema_name";

	public static final String LOCAL_REASONER = "Choose_reasoner";

	public static final String USE_COUNT = "use_count";

	public static final String USE_PROPERTY_PATHS = "use_property_paths";

	@Override
	public Class<SPARQLConfigurable> getConfigurableClass() {
		// TODO Auto-generated method stub
		return SPARQLConfigurable.class;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new ArrayList<ParameterType>();

		types.add(new ParameterTypeInt(RUNNER_TYPE, "Runner type", 0, 2, 0,
				true));

		types.add(new ParameterTypeString(LOCAL_ENDPOINT_FILE,
				"The URL of the SPARQL endpoint to use.", "", false));

		types.add(new ParameterTypeString(LOCAL_ENDPOINT_SCHEMA_FILE,
				"The URL of the SPARQL endpoint to use.", "", false));

		types.add(new ParameterTypeString(PARAMETER_ENDPOINT,
				"The URL of the SPARQL endpoint to use.",
				"http://dbpedia.org/sparql/", false));

		types.add(new ParameterTypeString(
				PARAMETER_LINK_ANNOTATION,
				"This parameter is an annotation for the link, which is used for constructing an attribute name.",
				"DBpedia", false));

		types.add(new ParameterTypeBoolean(USE_COUNT,
				"The endpoint supports COUNT", false, false));

		types.add(new ParameterTypeBoolean(USE_PROPERTY_PATHS,
				"The endpoint supports Property Paths", false, false));

		types.add(new ParameterTypeInt(PARAMETER_PAGE_SIZE,
				"This parameter defines the SPARQL results page size.", 0,
				10000, 0, true));

		types.add(new ParameterTypeInt(
				PARAMETER_TIMEOUT,
				"This parameter defines the timeout for the SPARQL queries in milliseconds",
				1, 100000, 60000, true));

		types.add(new ParameterTypeInt(
				PARAMETER_RETRIES,
				"This parameter defines number of retries for the SPARQL queries",
				1, 100, 10, true));

		types.add(new ParameterTypeInt(LOCAL_REASONER, "Local Reasoner", 0, 4,
				0, true));
		return types;
	}

	@Override
	public String getTypeId() {
		// TODO Auto-generated method stub
		return TYPE_ID;
	}

	@Override
	public String getI18NBaseKey() {
		// TODO Auto-generated method stub
		return I18N_BASE_KEY;
	}

	@Override
	public ConfigurationPanel<SPARQLConfigurable> createConfigurationPanel() {
		return new SPARQLConfigurationPanel();
	}
}
