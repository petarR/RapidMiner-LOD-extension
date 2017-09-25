package lod.testcases;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections15.map.HashedMap;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import com.hp.hpl.jena.query.ResultSet;

public class TestLauncher {

	static HashMap<String, String> cachedPrefixes = new HashMap<String, String>();
	
	/*public static void main(String[] args) {
		try {
			System.out.print(resolveAlias("owl"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
	}*/
	
	private static String resolveAlias(String alias) throws ClientProtocolException, IOException {
		
		final String SERVICE_URL = "http://prefix.cc/";
		final String FORMAT = "csv";
		final String DELIMITER = ",";
		
		if (cachedPrefixes.containsKey(alias)) {
			return cachedPrefixes.get(alias);
		} else {
			String resolvedAlias = makeGet(SERVICE_URL+alias+".file."+FORMAT);
			String results = resolvedAlias.split(DELIMITER)[1].trim();
			int len = results.length();
			String resolvedAliasToReturn = results.substring(1,len-1);
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

		String data = "";
		// it is important to reinitialize the object, otherwise an empty string
		// is returned.
		data = new String(EntityUtils.toString(responseEntity));
		return data;
	}
}
