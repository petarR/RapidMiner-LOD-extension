/**
 * 
 */
package com.rapidminer;

import lod.gui.tools.dialogs.SPARQLConfigurable;
import lod.gui.tools.dialogs.SPARQLConfigurator;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.config.ConfigurationException;
import com.rapidminer.tools.config.ConfigurationManager;

/**
 * This class provides hooks for initialization
 * 
 * @author Sebastian Land
 */
public class PluginInitLOD {

	public static final String PROPERTY_RMLOD_QUERY_CACHE_SIZE = "rapidminer.lod.SPARQL.queries.results.cache.size";
	public static final String PROPERTY_RMLOD_QUERY_CACHE_REMOVE_AFTER_PROCESS = "rapidminer.lod.SPARQL.queries.results.cache.clean.after.process.is.completed";

	public static final String PROPERTY_RMLOD_MODEL_CACHE_SIZE = "rapidminer.lod.SPARQL.data.models.cache.size";
	public static final String PROPERTY_RMLOD_MODEL_REMOVE_AFTER_PROCESS = "rapidminer.lod.SPARQL.data.models.cache.clean.after.process.is.completed";

	/**
	 * This method will be called directly after the extension is initialized.
	 * This is the first hook during start up. No initialization of the
	 * operators or renderers has taken place when this is called.
	 */
	public static void initPlugin() {
		// init the preferences dialog
		initPreferencesDialog();
		ConfigurationManager manager = ConfigurationManager.getInstance();
		SPARQLConfigurator config = new SPARQLConfigurator();
		manager.register(config);
		setProxyParams();
		// manager.removeConfigurable(config.getI18NBaseKey(), "DBpedia");
		// manager.removeConfigurable(config.getI18NBaseKey(), "Eurostat");
		if (!manager.getAllConfigurableNames(config.getI18NBaseKey()).contains(
				"DBpedia")) {
			try {
				SPARQLConfigurable sparqlConfigurable = config.create(
						"DBpedia",
						SPARQLConfigurable.getDBpediaDefaultConnection());
				manager.registerConfigurable(config.getI18NBaseKey(),
						sparqlConfigurable);
			} catch (ConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (!manager.getAllConfigurableNames(config.getI18NBaseKey()).contains(
				"Eurostat")) {
			try {
				SPARQLConfigurable sparqlConfigurable = config.create(
						"Eurostat", SPARQLConfigurable.getEurostatConnection());
				manager.registerConfigurable(config.getI18NBaseKey(),
						sparqlConfigurable);
			} catch (ConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (!manager.getAllConfigurableNames(config.getI18NBaseKey()).contains(
				"URLbasedModel")) {
			try {
				SPARQLConfigurable sparqlConfigurable = config.create(
						"URLbasedModel",
						SPARQLConfigurable.getURLBasedConnection());
				manager.registerConfigurable(config.getI18NBaseKey(),
						sparqlConfigurable);
			} catch (ConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	/**
	 * creates the config dialog for RMLOD in Tools->preferences
	 */
	private static void initPreferencesDialog() {
		ParameterService.registerParameter(new ParameterTypeInt(
				PROPERTY_RMLOD_QUERY_CACHE_SIZE,
				"The size of the query results cache.", 0, Integer.MAX_VALUE,
				10000));

		ParameterService.registerParameter(new ParameterTypeBoolean(
				PROPERTY_RMLOD_QUERY_CACHE_REMOVE_AFTER_PROCESS,
				"The size of the data model cache.", false));

		ParameterService.registerParameter(new ParameterTypeInt(
				PROPERTY_RMLOD_MODEL_CACHE_SIZE,
				"The size of the data model cache.", 0, Integer.MAX_VALUE,
				10000));
		ParameterService.registerParameter(new ParameterTypeBoolean(
				PROPERTY_RMLOD_MODEL_REMOVE_AFTER_PROCESS,
				"The size of the data model cache.", false));
	}

	/**
	 * This method is called during start up as the second hook. It is called
	 * before the gui of the mainframe is created. The Mainframe is given to
	 * adapt the gui. The operators and renderers have been registered in the
	 * meanwhile.
	 */
	public static void initGui(MainFrame mainframe) {
	}

	/**
	 * The last hook before the splash screen is closed. Third in the row.
	 */
	public static void initFinalChecks() {
	}

	/**
	 * Will be called as fourth method, directly before the UpdateManager is
	 * used for checking updates. Location for exchanging the UpdateManager. The
	 * name of this method unfortunately is a result of a historical typo, so
	 * it's a little bit misleading.
	 */
	public static void initPluginManager() {
	}

	/**
	 * sets the proxy to the one specified in Tools -> Preferences http proxy
	 */
	private static void setProxyParams() {

		setProxyParameter(RapidMiner.PROPERTY_RAPIDMINER_HTTP_PROXY_SET);
		setProxyParameter(RapidMiner.PROPERTY_RAPIDMINER_HTTP_PROXY_HOST);
		setProxyParameter(RapidMiner.PROPERTY_RAPIDMINER_HTTP_PROXY_PORT);
		setProxyParameter(RapidMiner.PROPERTY_RAPIDMINER_HTTP_PROXY_USERNAME);
		setProxyParameter(RapidMiner.PROPERTY_RAPIDMINER_HTTP_PROXY_PASSWORD);
		setProxyParameter(RapidMiner.PROPERTY_RAPIDMINER_HTTP_PROXY_NON_PROXY_HOSTS);

		setProxyParameter(RapidMiner.PROPERTY_RAPIDMINER_HTTPS_PROXY_SET);
		setProxyParameter(RapidMiner.PROPERTY_RAPIDMINER_HTTPS_PROXY_HOST);
		setProxyParameter(RapidMiner.PROPERTY_RAPIDMINER_HTTPS_PROXY_PORT);
		setProxyParameter(RapidMiner.PROPERTY_RAPIDMINER_HTTPS_PROXY_USERNAME);
		setProxyParameter(RapidMiner.PROPERTY_RAPIDMINER_HTTPS_PROXY_PASSWORD);

	}

	private static void setProxyParameter(String param) {
		String paramValue = ParameterService.getParameterValue(param);
		if (paramValue != null && !paramValue.equals("")) {
			System.setProperty(param, paramValue);
		}

	}
}
