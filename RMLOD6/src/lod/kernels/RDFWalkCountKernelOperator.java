package lod.kernels;

import java.util.List;

import org.data2semantics.mustard.kernels.data.RDFData;
import org.data2semantics.mustard.kernels.graphkernels.FeatureVectorKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFGraphListWalkCountKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFRootWalkCountKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFTreeWalkCountKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFWalkCountKernel;

import lod.generators.BaseGenerator;
/*
 * -iterate all nodes starting from each instance, and generate feature for each path with a given depth in the subgraph(if the path exists, increase the counter for the given path in the feature vector) 
 -results in a feature vectors that contain all the possible paths starting from each node (with a given depth)\
 -simple dot product on the feature vectors gives you the kernel
 -the paths include the edges as well(nA->eAB->nB)
 */


import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;

public class RDFWalkCountKernelOperator extends AbstractKernelOperator {
	public static final String PARAMETER_KERNEL_TYPE = "Kernel type";
	public static final String PARAMETER_PATH_DEPTH = "Walk length";

	private String[] options = {"Fast", "Root", "Tree", "Full"};
	private int pathDepth;
	private int option;

	public RDFWalkCountKernelOperator(OperatorDescription description) {
		super(description);
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeCategory(
				PARAMETER_KERNEL_TYPE, "Select the kernel type", options, 0, false));

		types.add(new ParameterTypeInt(PARAMETER_PATH_DEPTH, "Walk length", 1,
				10, 1, false));
		types.add(new ParameterTypeInt(PARAMETER_GRAPH_DEPTH,
				"The depth of the graph for each instance.", 1, 10, 1, false));

		return types;
	}

	@Override
	protected void initAdditionalParams() throws UserError {
		graphDepth = getParameterAsInt(PARAMETER_GRAPH_DEPTH);
		pathDepth = getParameterAsInt(PARAMETER_PATH_DEPTH);
		option = getParameterAsInt(PARAMETER_KERNEL_TYPE);
	}

	@Override
	public void generateFeatureVectors() {
		switch (option) {
		case 0: kernel = new RDFWalkCountKernel(pathDepth, graphDepth, useInference, normalizeValues); break;
		case 1: kernel = new RDFRootWalkCountKernel(pathDepth, useInference, normalizeValues); break;
		case 2: kernel = new RDFTreeWalkCountKernel(pathDepth, graphDepth, useInference, normalizeValues); break;
		case 3: kernel = new RDFGraphListWalkCountKernel(pathDepth, graphDepth, useInference, normalizeValues); break;
		}

		featureVectors = ((FeatureVectorKernel<RDFData>) kernel).computeFeatureVectors(inputGraph.getGraphData());
	}
}
