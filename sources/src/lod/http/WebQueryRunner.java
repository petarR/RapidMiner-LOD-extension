package lod.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lod.async.AsyncRunner;
import lod.async.AsyncRunnerThread;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import com.rapidminer.operator.OperatorException;

/**
 * The class enables a set of methods for asynchronous HTTP querying. Based in {@link AsyncRunner}.
 * One can set custom HTTP headers as {@code
		ArrayList<String[]> headers = new ArrayList<String[]>();
		headers.add(new String[] {"Accept", "application/json"});
		runner.setHeaders(headers);
}

The default headers are: {@code
accept = application/xml,
ContentType = application/xml
}

 * @author Evgeny Mitichkin
 *
 */
public class WebQueryRunner extends AsyncRunner{

	private String mURL = "";
	private int mConnectionTime = 0;
	private String mDefaultHeaderAccept = "application/xml";
	private String mDefaultHeaderContentType = "application/xml";
	private List<String[]> headers = new ArrayList<String[]>();
	
	public WebQueryRunner(String url, int timeout) {
		this.setURL(url);
		this.setConnectionTime(timeout);		
	}
	
	public String getURL() {
		return mURL;
	}

	public void setURL(String URL) {
		this.mURL = URL;
	}

	public int getConnectionTime() {
		return mConnectionTime;
	}

	public void setConnectionTime(int ConnectionTime) {
		this.mConnectionTime = ConnectionTime;
	}
	
	public String makeGet() throws ClientProtocolException, IOException {
		HttpGet request = new HttpGet(mURL);
		if (getHeaders().size()==0) {
			request.setHeader("Accept", mDefaultHeaderAccept);
			request.setHeader("Content-type", mDefaultHeaderContentType);
		} else {
			for (String[] header : getHeaders()) {
				request.setHeader(header[0], header[1]);
			}
		}
		
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, mConnectionTime);
		DefaultHttpClient httpClient = new DefaultHttpClient(params);
		HttpResponse response = httpClient.execute(request);
		HttpEntity responseEntity = response.getEntity();
		
		String data = "";
		// it is important to reinitialize the object, otherwise an empty string
		// is returned.
		data = new String(EntityUtils.toString(responseEntity));
		EntityUtils.consume(responseEntity);
		httpClient.getConnectionManager().shutdown();
		return data;
	}
	
	public String makeGetInterruptable() throws OperatorException {
		String results = "";
		this.mAsyncRunnerThread = new AsyncRunnerThread(this.getClass(), "makeGet", new Class[] {}, new Object[] {}, this);
		this.startAsyncRunner();
		this.enableWaiter();
		
		results = (String) this.getAsyncOperationResult();
		this.setAsyncOperationResultNull();
		finalizeAsyncThread();
		mAsyncRunnerThread = null;
		return results;
	}

	public List<String[]> getHeaders() {
		return headers;
	}

	public void setHeaders(List<String[]> headers) {
		this.headers = headers;
	}
}
