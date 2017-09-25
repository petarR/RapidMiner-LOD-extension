package lod.utils;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lod.http.WebQueryRunner;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.rapidminer.operator.OperatorException;

public class SpotlightAnalyzer {

	// private final static String API_URL = "http://jodaiber.dyndns.org:2222/";
	private String endpoint = "http://spotlight.sztaki.hu:2222/";// "http://spotlight.dbpedia.org/";
	private double confidence = 0.2;
	private int support = 20;
	private double contextual_score = 0.2;
	private String disambiguator = "Default";
	private String spotter = "Default";// CoOccurrenceBasedSelector
	private String types;
	private int timeout = 45000;
	
	public boolean isRunning = true;

	private Map<String, List<DBpediaCustomSource>> cachedResults = new HashMap<String, List<DBpediaCustomSource>>();

	private HttpClient client = null;

	public int getTimeout() {
		return timeout;
	}

	public String getTypes() {
		return types;
	}

	public void setTypes(String types) {
		this.types = types;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public double getConfidence() {
		return confidence;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}

	public int getSupport() {
		return support;
	}

	public void setSupport(int support) {
		this.support = support;
	}

	public double getContextual_score() {
		return contextual_score;
	}

	public void setContextual_score(double contextual_score) {
		this.contextual_score = contextual_score;
	}

	public String getDisambiguator() {
		return disambiguator;
	}

	public void setDisambiguator(String disambiguator) {
		this.disambiguator = disambiguator;
	}

	public String getSpotter() {
		return spotter;
	}

	public void setSpotter(String spotter) {
		this.spotter = spotter;
	}

	public HttpClient getClient() {
		return client;
	}

	public void setClient(HttpClient client) {
		this.client = client;
	}

	public SpotlightAnalyzer(String endpoint, double confidence, int support,
			double contextual_score, String disambiguator, String spotter,
			String types) {
		super();
		this.endpoint = endpoint;
		this.confidence = confidence;
		this.support = support;
		this.contextual_score = contextual_score;
		this.disambiguator = disambiguator;
		this.types = types;
		this.spotter = spotter;
		client = new DefaultHttpClient();
	}

	public List<DBpediaCustomSource> extract(String text) throws Exception,
			OperatorException {

		String spotlightResponse = null;

		String getURL = endpoint + "rest/annotate/?" + "confidence="
				+ confidence + "&support=" + support + "&disambiguator="
				+ disambiguator + "&contextualScore=" + contextual_score
				+ "&spotter=" + spotter + "&types=" + types + "&text="
				+ URLEncoder.encode(text, "utf-8");

		if (cachedResults.containsKey(getURL)) {
			return cachedResults.get(getURL);
		}
		/*HttpGet get = new HttpGet(getURL);
		get.setHeader("Accept", "application/json");

		HttpResponse responseGet = client.execute(get);
		HttpEntity resEntityGet = responseGet.getEntity();
		if (resEntityGet != null) {
			// do something with the responses
			spotlightResponse = EntityUtils.toString(resEntityGet);

		}
		EntityUtils.consume(resEntityGet);*/
		
		WebQueryRunner runner = new WebQueryRunner(getURL, timeout);
		ArrayList<String[]> headers = new ArrayList<String[]>();
		headers.add(new String[] {"Accept", "application/json"});
		runner.setHeaders(headers);
		spotlightResponse = runner.makeGetInterruptable();
		
		assert spotlightResponse != null;		

		isRunning = runner.mUIThreadRunning;//If the user pressed "stop" button

		JSONObject resultJSON = null;
		JSONArray entities = null;
		if (spotlightResponse == null)
			return null;
		try {
			resultJSON = new JSONObject(spotlightResponse);
			if (resultJSON.has("Resources"))
				entities = resultJSON.getJSONArray("Resources");
			else
				return null;
		} catch (Exception e) {
			e.printStackTrace();
		}

		LinkedList<DBpediaCustomSource> resources = new LinkedList<DBpediaCustomSource>();

		if (entities == null)
			return null;
		for (int i = 0; i < entities.length(); i++) {
			try {
				JSONObject entity = entities.getJSONObject(i);
				String[] types = entity.getString("@types").split(",");
				List<String> entTypes = new ArrayList<String>();
				for (String typ : types) {
					entTypes.add(typ);
				}
				resources.add(new DBpediaCustomSource(entity.getString("@URI"),
						entTypes, entity.getString("@surfaceForm")));

			} catch (JSONException e) {
				e.printStackTrace();
			}

		}
		cachedResults.put(getURL, resources);
		return resources;
	}

	public List<String> getListOfConcepts(String document) throws Exception {
		// check for results in the cash

		List<String> documentConcepts = new ArrayList<String>();
		List<DBpediaCustomSource> concepts = extract(document);

		if (concepts == null)
			return null;
		for (DBpediaCustomSource source : concepts) {
			documentConcepts.add(source.getURI());
		}
		// put results in cache
		return documentConcepts;
	}
	/*
	 * public static void main(String[] args) throws Exception { String text =
	 * "Will Smith in Paris"; List<DBpediaCustomSource> concepts =
	 * extract(text); int i; }
	 */
}