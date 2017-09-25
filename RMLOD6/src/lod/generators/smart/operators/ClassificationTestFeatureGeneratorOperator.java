package lod.generators.smart.operators;

import lod.generators.BaseGenerator;
import lod.generators.smart.ClassificationTestFeatureGenerator;
import lod.generators.vectorcreation.VectorCreator;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;

/**
 * @author petar
 * 
 *         generates features for classification task
 */
public class ClassificationTestFeatureGeneratorOperator extends BaseGenerator {

	private static final String CLASS_NAME = "classification_feature_generator_operator";

	protected InputPort mTestInputPort;

	public ClassificationTestFeatureGeneratorOperator(
			OperatorDescription description) {
		super(description);
		mTestInputPort = getInputPorts().createPort("training Example Set",
				ExampleSet.class);
		mTestInputPort.addPrecondition(new SimplePrecondition(mTestInputPort,
				new MetaData(ExampleSet.class)));
	}

	@Override
	public void doWork() throws OperatorException {
		initAttributesAndParams(false, CLASS_NAME);

		// throw exception if the label is not defined
		if (exampleSet.getAttributes().getLabel() == null) {
			throw new OperatorException(
					"Problem in Simple Filter: label attribute must be defined");
		}

		ClassificationTestFeatureGenerator generator = new ClassificationTestFeatureGenerator(
				exampleSet, mTestInputPort.getData(ExampleSet.class),
				queryRunner, attrsBypsass);

		generator.calculateFeatures();

		// change the vector representation if needed
		int option = getParameterAsInt(PARAMETER_VECTOR_REPRESENTATION);

		// the count representation will be skipped later if needed

		VectorCreator vc = new VectorCreator();
		vc.createVector(exampleSet, option, generator.getUniqueAttrs().keySet());

		mOutputPort.deliver(exampleSet);

	}

	@Override
	public String getSPARQLQueryOutgoing(String uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSPARQLQueryIncoming(String uri) {
		// TODO Auto-generated method stub
		return null;
	}
}