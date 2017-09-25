package lod.kernels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import lod.generators.BaseGenerator;
import lod.generators.vectorcreation.VectorCreator;
import lod.kernels.models.GraphHolder;
import lod.utils.AttributeTypeGuesser;

import org.data2semantics.mustard.kernels.FeatureInspector;
import org.data2semantics.mustard.kernels.SparseVector;

import com.rapidminer.Process;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
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

	private static final Logger LOGGER = Logger.getLogger(Process.class
			.getName());

	// params
	private static final String CLASS_NAME = "abstract_kernel_import_operator";

	// public static final String PARAMETER_PATH_DEPTH = "Path length";
	public static final String PARAMETER_GRAPH_DEPTH = "Graph depth";
	public static final String PARAMETER_USE_INFERENCE = "Use inference";
	// public static final String PARAMETER_NORMALIZE = "Normalize values";

	public static final String INSTANCE_COLUMN_NAME = "Instance";

	private InputPort mInputPort;

	private InputPort mInputPortExampleSet;

	private OutputPort mOutputPort;
	private OutputPort mOutputPortExampleSet;

	// variables
	protected GraphHolder inputGraph;
	protected ExampleSet inputSet;

	protected int graphDepth;
	// protected int pathDepth;
	protected boolean useInference = false;
	protected boolean normalizeValues = false;

	/**
	 * this is the kernel
	 */
	protected FeatureInspector kernel;

	// the intermediate feature vectors
	protected SparseVector[] featureVectors;

	// inverse dictionary of labels
	Map<String, Integer> inverseLabels;

	public AbstractKernelOperator(OperatorDescription description) {
		super(description);
		mInputPort = getInputPorts().createPort("Graph", GraphHolder.class);

		mInputPortExampleSet = getInputPorts().createPort("Example Set",
				ExampleSet.class);
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
		long start = System.currentTimeMillis();
		generateFeatureVectors();
		LOGGER.info("TIME FOR CALCULATING THE KERNEL: "
				+ (System.currentTimeMillis() - start));
		LOGGER.info("KERNEL FEATURES: " + featureVectors[0].getLastIndex());
		// LOGGER.info("KERNEL FEATURES2: " + featureVectors[1].getLastIndex());
		// LOGGER.info("KERNEL FEATURES3: " + featureVectors[2].getLastIndex());
		// mOutputPortExampleSet.deliver(inputSet);
		mOutputPortExampleSet.deliver(generateMemoryTableFromVectors());
		mOutputPort.deliver(inputGraph);

	}

	public abstract void generateFeatureVectors();

	/**
	 * generates the RapidMiner memoryTable from the given sparseVectors
	 */
	// public ExampleSet generateMemoryTableFromVectorsOld() {
	//
	// List<String> uniqueAttributes = generateUniqueAttributesNames();
	// // add the attributes
	// AttributeTypeGuesser attributeTypeGuesser = new AttributeTypeGuesser();
	// Attribute[] attributes = new Attribute[uniqueAttributes.size() + 1];
	// // add the id attribute
	// attributes[0] = AttributeFactory.createAttribute(INSTANCE_COLUMN_NAME,
	// Ontology.STRING);
	// int i = 0;
	// for (String att : uniqueAttributes) {
	// attributes[i + 1] = AttributeFactory.createAttribute(att,
	// Ontology.NUMERICAL);
	// i++;
	// }
	// MemoryExampleTable table = null;
	// table = new MemoryExampleTable(attributes);
	//
	// int vecNM = 0;
	// for (SparseVector vector : featureVectors) {
	// table.addDataRow(generateDataRowForVector(vector, attributes, vecNM));
	// vecNM++;
	// }
	// ExampleSet set = table.createExampleSet();
	//
	// // change the vector representation if needed
	// int option = 0;
	// try {
	// option =
	// getParameterAsInt(BaseGenerator.PARAMETER_VECTOR_REPRESENTATION);
	// } catch (UndefinedParameterError e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// // the count representation will be skipped later if needed
	//
	// VectorCreator vc = new VectorCreator();
	// vc.createVector(set, option, uniqueAttributes);
	// return set;
	// }

	public ExampleSet generateMemoryTableFromVectors() {
		List<String> atts = new LinkedList<String>();
		int indexExample = 0;

		Map<Integer, String> labelCache = new HashMap<Integer, String>();
		for (SparseVector vector : featureVectors) {
			Example ex = inputSet.getExample(indexExample);
			long start = System.currentTimeMillis();
			for (Integer ind : vector.getIndices()) {
				Attribute attribute = null;
				String attName = "";
				attName = getAttName(labelCache, ind);

				if (!atts.contains(attName)) {
					atts.add(attName);
					attribute = BaseGenerator.addAtribute(attName,
							Ontology.NUMERICAL, inputSet);

				} else {
					attribute = inputSet.getAttributes().get(attName);
				}
				double value = AttributeTypeGuesser.getValueForAttribute(
						attribute, Double.toString(vector.getValue(ind)));
				setValueToExample(inputSet, ex, attribute, value);

			}
			indexExample++;
			LOGGER.info("SAMPLE: " + indexExample + "TIME FOR IT "
					+ (System.currentTimeMillis() - start));
		}

		int option = 0;
		try {
			option = getParameterAsInt(BaseGenerator.PARAMETER_VECTOR_REPRESENTATION);
		} catch (UndefinedParameterError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// the count representation will be skipped later if needed
		VectorCreator vc = new VectorCreator();
		vc.createVector(inputSet, option, atts);

		return inputSet;
	}

	private String getAttName(Map<Integer, String> labelCache, Integer ind) {
		String attname = "";
		if (labelCache.containsKey(ind))
			return labelCache.get(ind);
		else {
			List<String> indLabelL;
			List<Integer> indL = new ArrayList<Integer>();
			attname = Integer.toString(ind);
			indL = new ArrayList<Integer>();
			indL.add(ind);
			indLabelL = kernel.getFeatureDescriptions(indL);

			if (!indLabelL.isEmpty())
				attname = indLabelL.get(0); // it's the first element since
											// we asked for only one index
			labelCache.put(ind, attname);
		}
		return attname;
	}

	private void setValueToExample(ExampleSet inputSet2, Example ex,
			Attribute attribute, double value) {
		ex.setValue(
				attribute,
				AttributeTypeGuesser.getValueForAttribute(attribute,
						Double.toString(value)));

	}

	// private DataRow generateDataRowForVectorTmp(SparseVector vector,
	// Attribute[] attributes, int vectNM) {
	// DoubleArrayDataRow row = new DoubleArrayDataRow(
	// new double[attributes.length]);
	//
	// for (Attribute attr : attributes) {
	// double value = AttributeTypeGuesser.getValueForAttribute(attr, "0");
	//
	// // set the id
	// if (attr.getName().equals(INSTANCE_COLUMN_NAME)) {
	// value = AttributeTypeGuesser.getValueForAttribute(attr,
	// inputGraph.getGraphData().getInstances().get(vectNM)
	// .stringValue());
	// } else {
	// int ind = inverseLabels.get(attr.getName());
	// if (vector.getIndices().contains(ind)) {
	// value = AttributeTypeGuesser.getValueForAttribute(attr,
	// Double.toString(vector.getValue(ind)));
	// }
	// }
	// row.set(attr, value);
	//
	// }
	// return row;
	// }
	//
	// private DataRow generateDataRowForVector(SparseVector vector,
	// Attribute[] attributes, int vectNM) {
	// DoubleArrayDataRow row = new DoubleArrayDataRow(
	// new double[attributes.length]);
	//
	// for (Attribute attr : attributes) {
	// double value = AttributeTypeGuesser.getValueForAttribute(attr, "0");
	//
	// // set the id
	// if (attr.getName().equals(INSTANCE_COLUMN_NAME)) {
	// value = AttributeTypeGuesser.getValueForAttribute(attr,
	// inputGraph.getGraphData().getInstances().get(vectNM)
	// .stringValue());
	// } else {
	// int ind = inverseLabels.get(attr.getName());
	// if (vector.getIndices().contains(ind)) {
	// value = AttributeTypeGuesser.getValueForAttribute(attr,
	// Double.toString(vector.getValue(ind)));
	// }
	// }
	// row.set(attr, value);
	//
	// }
	// return row;
	// }
	//
	// private List<String> generateUniqueAttributesNames() {
	// List<String> atts = new LinkedList<String>();
	// List<Integer> indL;
	// List<String> indLabelL;
	// for (SparseVector vect : featureVectors) {
	// for (Integer ind : vect.getIndices()) {
	// String attName = Integer.toString(ind);
	// indL = new ArrayList<Integer>();
	// indL.add(ind);
	// indLabelL = kernel.getFeatureDescriptions(indL);
	//
	// if (!indLabelL.isEmpty())
	// attName = indLabelL.get(0); // it's the first element since
	// // we asked for only one index
	// if (!atts.contains(attName))
	// atts.add(attName);
	// if (!inverseLabels.containsKey(attName))
	// inverseLabels.put(attName, ind);
	// }
	// }
	// return atts;
	// }

	private void init() throws UserError {
		inputGraph = mInputPort.getData(GraphHolder.class);
		inputSet = mInputPortExampleSet.getData(ExampleSet.class);
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
