package lod.kernels;

import java.util.List;

import org.data2semantics.mustard.kernels.graphkernels.FeatureVectorKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFIntersectionGraphEdgeVertexPathKernel;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;

/*
 -iterate all nodes starting from each instance, and generate feature for each path with a given depth in the subgraph(if the path exists, increase the counter for the given path in the feature vector),BUT also add the subpath of each created path (by removing element by elemnt from the begining of the path), also the path might end with the last predicate or the last node
 -E.g. if there is path pX=nA->eAB->nB and path pY:nA->eAC->nC->eCB->nB  there will be a feature: eAB->nB=1 (which is a subpath); nB=2 (because there are two subpaths that contain only the node nB)
 */

public class RDFIntersectionGraphEdgeVertexPathKernelOperator extends
		AbstractKernelOperator {

	public RDFIntersectionGraphEdgeVertexPathKernelOperator(
			OperatorDescription description) {
		super(description);
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(PARAMETER_PATH_DEPTH, "Path length", 1,
				5, 1, false));
		return types;
	}

	@Override
	protected void initAdditionalParams() throws UserError {

		pathDepth = getParameterAsInt(PARAMETER_PATH_DEPTH);
	}

	@Override
	public void generateFeatureVectors() {
		kernel = new RDFIntersectionGraphEdgeVertexPathKernel(pathDepth,
				useInference, normalizeValues);

		featureVectors = kernel
				.computeFeatureVectors(inputGraph.getGraphData());

	}
}
