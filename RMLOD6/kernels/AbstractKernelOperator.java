package lod.kernels;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lod.generators.BaseGenerator;
import lod.generators.vectorcreation.VectorCreator;
import lod.kernels.models.GraphHolder;
import lod.utils.AttributeTypeGuesser;

import org.data2semantics.mustard.kernels.graphkernels.FeatureVectorKernel;
import org.data2semantics.mustard.learners.SparseVector;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;

public abstract class AbstractKernelOperator extends Operator {

	// params
	private static final String CLASS_NAME = "abstract_kernel_import_operator";

	public static final String PARAMETER_PATH_DEPTH = "Path length";
	public static final String PARAMETER_GRAPH_DEPTH = "Graph depth";
	public static final String PARAMETER_USE_INFERENCE = "Use inference";
	// public static final String PARAMETER_NORMALIZE = "Normalize values";

	public static final String INSTANCE_COLUMN_NAME = "Instance";

	private InputPort mInputPort;

	private OutputPort mOutputPort;
	private OutputPort mOutputPortExampleSet;

	// variables
	protected GraphHolder inputGraph;

	protected int graphDepth;
	protected int pathDepth;
	protected boolean useInference = false;
	protected boolean normalizeValues = false;

	/**
	 * this is the kernel
	 */
	protected FeatureVectorKernel kernel;

	// the intermediate feature vectors
	protected SparseVector[] featureVectors;

	// inverse dictionary of labels
	Map<String, Integer> inverseLabels;

	public AbstractKernelOperator(OperatorDescription description) {
		super(description);
		mInputPort = getInputPorts().createPort("Graph", GraphHolder.class);
		mOutputPortExampleSet = getOutputPorts().createPort("Example Set");
		mOutputPort = getOutputPorts().createPort("Graph");

		getTransformer().addPassThroughRule(mInputPort, mOutputPort);

		getTransformer().addRule(
				new GenerateNewMDRule(mOutputPort, GraphHolder.class));
		getTransformer().addRule(
				new GenerateNewMDRule(mOutputPortExampleSet, ExampleSet.class));
	}

	@Override
	public void doWork() throws OperatorException {
		// TODO Auto-generated method stub
		super.doWork();
		init();
		initAdditionalParams();

		generateFeatureVectors();

		mOutputPortExampleSet.deliver(generateMemoryTableFromVectors());
		mOutputPort.deliver(inputGraph);

	}

	public abstract void generateFeatureVectors();

	/**
	 * generates the RapidMiner memoryTable from the given sparseVectors
	 */
	public ExampleSet generateMemoryTableFromVectors() {

		List<String> uniqueAttributes = generateUniqueAttributesNames();
		// add the attributes
		AttributeTypeGuesser attributeTypeGuesser = new AttributeTypeGuesser();
		Attribute[] attributes = new Attribute[uniqueAttributes.size() + 1];
		// add the id attribute
		attributes[0] = AttributeFactory.createAttribute(INSTANCE_COLUMN_NAME,
				Ontology.STRING);
		int i = 0;
		for (String att : uniqueAttributes) {
			attributes[i + 1] = AttributeFactory.createAttribute(att,
					Ontology.NUMERICAL);
			i++;
		}
		MemoryExampleTable table = null;
		table = new MemoryExampleTable(attributes);

		int vecNM = 0;
		for (SparseVector vector : featureVectors) {
			table.addDataRow(generateDataRowForVector(vector, attributes, vecNM));
			vecNM++;
		}
		ExampleSet set = table.createExampleSet();

		// change the vector representation if needed
		int option = 0;
		try {
			option = getParameterAsInt(BaseGenerator.PARAMETER_VECTOR_REPRESENTATION);
		} catch (UndefinedParameterError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// the count representation will be skipped later if needed

		VectorCreator vc = new VectorCreator();
		vc.createVector(set, option, uniqueAttributes);
		return set;
	}

	private DataRow generateDataRowForVector(SparseVector vector,
			Attribute[] attributes, int vectNM) {
		DoubleArrayDataRow row = new DoubleArrayDataRow(
				new double[attributes.length]);

		for (Attribute attr : attributes) {
			double value = AttributeTypeGuesser.getValueForAttribute(attr, "0");

			// set the id
			if (attr.getName().equals(INSTANCE_COLUMN_NAME)) {
				value = AttributeTypeGuesser.getValueForAttribute(attr,
						inputGraph.getGraphData().getInstances().get(vectNM)
								.stringValue());
			} else {
				int ind = inverseLabels.get(attr.getName());
				if (vector.getIndices().contains(ind)) {
					value = AttributeTypeGuesser.getValueForAttribute(attr,
							Double.toString(vector.getValue(ind)));
				}
			}
			row.set(attr, value);

		}
		return row;
	}

	private List<String> generateUniqueAttributesNames() {
		List<String> atts = new LinkedList<String>();
		for (SparseVector vect : featureVectors) {
			for (Integer ind : vect.getIndices()) {
				String attName = Integer.toString(ind);
				if (kernel.getOriginalLabels().containsKey(ind))
					attName = (String) kernel.getOriginalLabels().get(ind);
				if (!atts.contains(attName))
					atts.add(attName);
				if (!inverseLabels.containsKey(attName))
					inverseLabels.put(attName, ind);
			}
		}
		return atts;
	}

	private void init() throws UserError {
		inputGraph = mInputPort.getData(GraphHolder.class);

		useInference = getParameterAsBoolean(PARAMETER_USE_INFERENCE);
		// we don't want to normalize the values ever, beacuase we offer
		// different representation methods
		normalizeValues = false; // getParameterAsBoolean(PARAMETER_NORMALIZE);

		inverseLabels = new HashMap<String, Integer>();
	}

	@Override
	public List<ParameterType> getParameterTypes() {

		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeBoolean(PARAMETER_USE_INFERENCE,
				"Use inference", false, false));

		types.add(new ParameterTypeCategory(
				BaseGenerator.PARAMETER_VECTOR_REPRESENTATION,
				"Select the schema for creating the word vector",
				BaseGenerator.VECTOR_CREATOR_NAMES, 0, false));

		// types.add(new ParameterTypeBoolean(PARAMETER_NORMALIZE,
		// "Normalize the feature vectors values", true, false));

		return types;
	}

	protected void initAdditionalParams() throws UserError {
	};

}
