package lod.kernels.models;

import org.data2semantics.mustard.kernels.data.RDFData;

import com.rapidminer.operator.ResultObjectAdapter;

public class GraphHolder extends ResultObjectAdapter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private RDFData graphData;

	public RDFData getGraphData() {
		return graphData;
	}

	public void setGraphData(RDFData graphData) {
		this.graphData = graphData;
	}

}
