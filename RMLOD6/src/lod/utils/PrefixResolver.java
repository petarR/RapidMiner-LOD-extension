package lod.utils;

import java.io.IOException;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

public class PrefixResolver {
	public static String resolveQuery(String query,
			HashMap<String, String> cachedPrefixes) {
		final String TOKEN_DELIMITER = " ";
		final String ALIAS_DELIMITER = ":";
		final String NORMAL_QUERY_FLAG = ">";

		String resultingQuery = "";

		String[] tokens = query.split(TOKEN_DELIMITER);
		for (int i = 0; i < tokens.length; i++) {
			String assemblyPart = "";

			if (tokens[i].contains(ALIAS_DELIMITER)) {
				String[] tokenParts = tokens[i].split(ALIAS_DELIMITER);
				if (tokenParts.length > 1
						&& !tokenParts[tokenParts.length - 1]
								.contains(NORMAL_QUERY_FLAG)
						&& !tokenParts[tokenParts.length - 1].contains("//")) {
					assemblyPart = tokenParts[0] + ALIAS_DELIMITER
							+ tokenParts[1];
					try {
						String newAlias = resolveAlias(
								tokenParts[tokenParts.length - 2],
								cachedPrefixes);
						assemblyPart = "<"
								+ newAlias
								+ tokenParts[tokenParts.length - 1].trim()
										.replace("{", "").replace("}", "")
								+ ">";
						if (tokenParts[tokenParts.length - 1].contains("}"))
							assemblyPart += "}";
						if (tokenParts[tokenParts.length - 2].contains("{")) {
							assemblyPart = "{" + assemblyPart;
						}
						if (tokenParts[tokenParts.length - 2].contains("}")) {
							assemblyPart = "}" + assemblyPart;
						}
					} catch (Exception e) {

					}
				} else {
					assemblyPart = tokens[i];
				}
			} else {
				assemblyPart = tokens[i];
			}
			resultingQuery += assemblyPart + " ";
		}

		return resultingQuery;
	}

	private static String resolveAlias(String alias,
			HashMap<String, String> cachedPrefixes)
			throws ClientProtocolException, IOException {
		// remove specialCharachters and convert to lower cases, because
		// prefix.cc doesn't support them
		alias = alias.replaceAll("[^a-zA-Z0-9]", "");
		alias = alias.toLowerCase();
		final String SERVICE_URL = "http://prefix.cc/";
		final String FORMAT = "csv";
		final String DELIMITER = ",";

		if (cachedPrefixes.containsKey(alias)) {
			return cachedPrefixes.get(alias);
		} else {
			String resolvedAlias = makeGet(SERVICE_URL + alias + ".file."
					+ FORMAT);
			String results = resolvedAlias.split(DELIMITER)[1].trim();
			int len = results.length();
			String resolvedAliasToReturn = results.substring(1, len - 1);
			cachedPrefixes.put(alias, resolvedAliasToReturn);
			return resolvedAliasToReturn;
		}
	}

	private static String makeGet(String url) throws ClientProtocolException,
			IOException {
		final int CONNECTION_TIMEOUT = 5000;
		HttpGet request = new HttpGet(url);
		request.setHeader("Accept", "application/xml");
		request.setHeader("Content-type", "application/xml");
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
		DefaultHttpClient httpClient = new DefaultHttpClient(params);
		HttpResponse response = httpClient.execute(request);
		HttpEntity responseEntity = response.getEntity();

		if (!Integer.toString(response.getStatusLine().getStatusCode())
				.startsWith("2"))
			throw new ClientProtocolException();
		String data = "";
		// it is important to reinitialize the object, otherwise an empty string
		// is returned.
		data = new String(EntityUtils.toString(responseEntity));
		return data;
	}

	public static void main(String[] args) {
		System.out.println(lod.utils.PrefixResolver.resolveQuery("owl:SameAs",
				new HashMap<String, String>()));
	}
}
