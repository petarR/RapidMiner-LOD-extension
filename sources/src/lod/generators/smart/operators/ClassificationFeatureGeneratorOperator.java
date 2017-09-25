package lod.generators.smart.operators;

import java.util.List;

import lod.generators.BaseGenerator;
import lod.generators.smart.ClassificationFeatureGenerator;
import lod.generators.vectorcreation.VectorCreator;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;

/**
 * @author petar
 * 
 *         generates features for classification task
 */
public class ClassificationFeatureGeneratorOperator extends BaseGenerator {

	private static final String CLASS_NAME = "classification_feature_generator_operator";

	public static final String PARAMETER_MIN_FREQ = "Min frequncy";
	public static final String PARAMETER_MAX_FREQ = "Max frequncy";

	public ClassificationFeatureGeneratorOperator(
			OperatorDescription description) {
		super(description);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void doWork() throws OperatorException {
		initAttributesAndParams(false, CLASS_NAME);

		// throw exception if the label is not defined
		if (exampleSet.getAttributes().getLabel() == null) {
			throw new OperatorException(
					"Problem in Simple Filter: label attribute must be defined");
		}

		ClassificationFeatureGenerator generator = new ClassificationFeatureGenerator(
				exampleSet, getParameterAsDouble(PARAMETER_MIN_FREQ),
				getParameterAsDouble(PARAMETER_MAX_FREQ), queryRunner,
				attrsBypsass);

		generator.calculateFeatures();

		// change the vector representation if needed
		int option = getParameterAsInt(PARAMETER_VECTOR_REPRESENTATION);
		
		// the count representation will be skipped later if needed
		
			VectorCreator vc = new VectorCreator();
			vc.createVector(exampleSet, option, generator.getUniqueAttrs()
					.keySet());
		

		mOutputPort.deliver(exampleSet);

	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeDouble(PARAMETER_MIN_FREQ, "Min frequency",
				0.0, 1.0, 0.2, false));

		types.add(new ParameterTypeDouble(PARAMETER_MAX_FREQ, "Max frequency",
				0.0, 1.0, 0.2, false));
		return types;
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