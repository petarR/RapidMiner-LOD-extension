package lod.kernels;

import java.util.List;
/*
 * -iterate all nodes starting from each instance, and generate feature for each path with a given depth in the subgraph(if the path exists, increase the counter for the given path in the feature vector) 
 -results in a feature vectors that contain all the possible paths starting from each node (with a given depth)\
 -simple dot product on the feature vectors gives you the kernel
 -the paths include the edges as well(nA->eAB->nB)
 */

import org.data2semantics.mustard.kernels.data.RDFData;
import org.data2semantics.mustard.kernels.graphkernels.FeatureVectorKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFGraphListWLSubTreeApproxKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFGraphListWLSubTreeKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFGraphListWalkCountApproxKernelMkII;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFGraphListWalkCountKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFRootWLSubTreeIDEQApproxKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFRootWLSubTreeKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFRootWalkCountIDEQApproxKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFRootWalkCountKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFTreeWLSubTreeIDEQApproxKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFTreeWLSubTreeIDEQKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFTreeWLSubTreeKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFTreeWalkCountIDEQApproxKernelMkII;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFTreeWalkCountIDEQKernelMkII;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFTreeWalkCountKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFWLSubTreeIDEQApproxKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFWLSubTreeIDEQKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFWLSubTreeKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFWalkCountIDEQApproxKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFWalkCountIDEQKernel;
import org.data2semantics.mustard.kernels.graphkernels.rdfdata.RDFWalkCountKernel;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;

public class RDFSubstructureCountingKernelOperator extends AbstractKernelOperator {
	public static final String PARAMETER_NM_ITERATIONS = "Substructure Size";	
	public static final String PARAMETER_NBH_TYPE = "Neighborhood Type";
	public static final String PARAMETER_SS_TYPE = "Substructure Type";
	public static final String PARAMETER_MINFREQ = "Minimal Label Frequency";

	private String[] optionsSS = {"Walk", "Tree"};
	private int optionSS;

	private String[] optionsNBH = {"Direct", "Tree", "Graph", "Root"};
	private int optionNBH;

	private int minFreq;

	private int nmIterations = 0;

	public RDFSubstructureCountingKernelOperator(OperatorDescription description) {
		super(description);
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeCategory(
				PARAMETER_SS_TYPE, "Select the type of substructure to count", optionsSS, 0, false));

		types.add(new ParameterTypeCategory(
				PARAMETER_NBH_TYPE, "Select the neighborhood extraction method", optionsNBH, 0, false));

		types.add(new ParameterTypeInt(PARAMETER_NM_ITERATIONS,
				"Maximum size of the substructures", 0, 10, 1, false));
		types.add(new ParameterTypeInt(PARAMETER_GRAPH_DEPTH,
				"The size of the neighborhood", 1, 10, 1, false));
		types.add(new ParameterTypeInt(PARAMETER_MINFREQ,
				"The minimal frequency of occurence of label among the instances", 0, 1000000, 0, false));

		return types;
	}

	@Override
	protected void initAdditionalParams() throws UserError {
		graphDepth = getParameterAsInt(PARAMETER_GRAPH_DEPTH);
		nmIterations = getParameterAsInt(PARAMETER_NM_ITERATIONS);
		optionSS = getParameterAsInt(PARAMETER_SS_TYPE);
		optionNBH = getParameterAsInt(PARAMETER_NBH_TYPE);
		minFreq = getParameterAsInt(PARAMETER_MINFREQ);
	}

	@Override
	public void generateFeatureVectors() {
		if (minFreq == 0) {
			switch ((optionSS*4) + optionNBH) {
			case 0: kernel = new RDFWalkCountIDEQKernel(nmIterations, graphDepth, useInference, normalizeValues); break;
			case 1: kernel = new RDFTreeWalkCountKernel(nmIterations, graphDepth, useInference, normalizeValues); break;
			case 2: kernel = new RDFGraphListWalkCountKernel(nmIterations, graphDepth, useInference, normalizeValues); break;
			case 3: kernel = new RDFRootWalkCountKernel(nmIterations, useInference, normalizeValues); break;
			case 4: kernel = new RDFWLSubTreeIDEQKernel(nmIterations, graphDepth, useInference, true, true, normalizeValues); break;
			case 5: kernel = new RDFTreeWLSubTreeKernel(nmIterations, graphDepth, useInference, true, true, normalizeValues); break;
			case 6: kernel = new RDFGraphListWLSubTreeKernel(nmIterations, graphDepth, useInference, true, true, normalizeValues); break;
			case 7: kernel = new RDFRootWLSubTreeKernel(nmIterations, useInference, normalizeValues); break;
			}
		} else {
			int[] dummyMN = {nmIterations};
			int[] dummyMC = {Integer.MAX_VALUE};
			int[] minFreqs = {minFreq};
			
			switch ((optionSS*4) + optionNBH) {
			case 0: kernel = new RDFWalkCountIDEQApproxKernel(nmIterations, graphDepth, useInference, minFreq, normalizeValues); break;
			case 1: kernel = new RDFTreeWalkCountIDEQApproxKernelMkII(nmIterations, graphDepth, useInference, minFreq, normalizeValues); break;
			case 2: kernel = new RDFGraphListWalkCountApproxKernelMkII(nmIterations, graphDepth, useInference, minFreq, normalizeValues); break;
			case 3: kernel = new RDFRootWalkCountIDEQApproxKernel(nmIterations, useInference, minFreq, normalizeValues); break;
			case 4: kernel = new RDFWLSubTreeIDEQApproxKernel(nmIterations, graphDepth, useInference, true, true, dummyMN, dummyMC, minFreqs, normalizeValues); break;
			case 5: kernel = new RDFTreeWLSubTreeIDEQApproxKernel(nmIterations, graphDepth, useInference, true, true, dummyMN, dummyMC, minFreqs, normalizeValues); break;
			case 6: kernel = new RDFGraphListWLSubTreeApproxKernel(nmIterations, graphDepth, useInference, true, true, dummyMN, dummyMC, minFreqs, normalizeValues); break;
			case 7: kernel = new RDFRootWLSubTreeIDEQApproxKernel(nmIterations, useInference, dummyMN, dummyMC, minFreqs, normalizeValues); break;
			}

		}

		featureVectors = ((FeatureVectorKernel<RDFData>) kernel).computeFeatureVectors(inputGraph.getGraphData());
	}
}
