package lod.modelreader.threading;

import org.apache.jena.riot.RDFDataMgr;

import lod.async.AsyncRunner;
import lod.async.AsyncRunnerThread;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.rapidminer.operator.OperatorException;

public class ModelReader extends AsyncRunner{

	private String mFileUri = "";	
	public boolean mUIThreadRunning = true;
	
	public ModelReader(String uri) {
		this.mFileUri = uri;
	}
	
	public Model readModelSync() {
		return RDFDataMgr.loadModel(mFileUri);
	}
	
	public Model readModel() throws OperatorException {
		Model resultModel = ModelFactory.createDefaultModel();
		this.mAsyncRunnerThread = new AsyncRunnerThread(this.getClass(), "readModelSync", new Class[] {}, new Object[] {}, this);
		this.startAsyncRunner();
		this.enableWaiter();
		
		resultModel = (Model) this.getAsyncOperationResult();
		this.setAsyncOperationResultNull();
		finalizeAsyncThread();
		mAsyncRunnerThread = null;
		return resultModel;
	}
}
