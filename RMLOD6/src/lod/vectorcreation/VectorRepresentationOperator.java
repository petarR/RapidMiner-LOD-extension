package lod.vectorcreation;

import java.util.ArrayList;
import java.util.List;

import lod.generators.BaseGenerator;
import lod.generators.vectorcreation.VectorCreator;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;

public class VectorRepresentationOperator extends Operator {
	private InputPort mInputPort;
	private OutputPort mOutputPortCount;
	private OutputPort mOutputPortBinary;
	private OutputPort mOutputPortTF;
	private OutputPort mOutputPortTFIDF;

	public VectorRepresentationOperator(OperatorDescription description) {
		super(description);
		mInputPort = getInputPorts().createPort("Example Set");

		mOutputPortCount = getOutputPorts().createPort("Example Count");
		mOutputPortBinary = getOutputPorts().createPort("Example Bin");
		mOutputPortTF = getOutputPorts().createPort("Example TF");
		mOutputPortTFIDF = getOutputPorts().createPort("Example TFIDF");
	}

	@Override
	public void doWork() throws OperatorException {
		super.doWork();
		ExampleSet exampleSet = mInputPort.getData(ExampleSet.class);
		List<String> atts = new ArrayList<String>();
		for (Attribute attr : exampleSet.getAttributes()) {
			if (attr.isNumerical())
				atts.add(attr.getName());
		}

		VectorCreator vc = new VectorCreator();

		ExampleSet e1 = BaseGenerator.cloneExampleSet(exampleSet);
		vc.createVector(e1, 0, atts);
		mOutputPortBinary.deliver(e1);

		ExampleSet e2 = BaseGenerator.cloneExampleSet(exampleSet);
		vc.createVector(e2, 2, atts);
		mOutputPortTF.deliver(e2);

		ExampleSet e3 = BaseGenerator.cloneExampleSet(exampleSet);
		vc.createVector(e3, 3, atts);
		mOutputPortTFIDF.deliver(e3);

		mOutputPortCount.deliver(exampleSet);

	}

}
