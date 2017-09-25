package lod.kernels;

import java.util.List;
/*
 * -iterate all nodes starting from each instance, and generate feature for each path with a given depth in the subgraph(if the path exists, increase the counter for the given path in the feature vector) 
 -results in a feature vectors that contain all the possible paths starting from each node (with a given depth)\
 -simple dot product on the feature vectors gives you the kernel
 -the paths include the edges as well(nA->eAB->nB)
 */

import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFTreeWLSubTreeKernel;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;

public class RDFTreeWLSubTreeKernelOperator extends AbstractKernelOperator {

	public static final String PARAMETER_NM_ITERATIONS = "Number of Iterations";

	private int nmIterations = 0;

	public RDFTreeWLSubTreeKernelOperator(OperatorDescription description) {
		super(description);
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeInt(PARAMETER_NM_ITERATIONS,
				"Number of Iterations", 0, 5, 1, false));
		types.add(new ParameterTypeInt(PARAMETER_GRAPH_DEPTH,
				"The depth of the graph for each instance.", 1, 5, 1, false));

		return types;
	}

	@Override
	protected void initAdditionalParams() throws UserError {
		graphDepth = getParameterAsInt(PARAMETER_GRAPH_DEPTH);
		nmIterations = getParameterAsInt(PARAMETER_NM_ITERATIONS);
	}

	@Override
	public void generateFeatureVectors() {

		kernel = new RDFTreeWLSubTreeKernel(nmIterations, graphDepth,
				useInference, normalizeValues);

		featureVectors = kernel
				.computeFeatureVectors(inputGraph.getGraphData());

	}
}
