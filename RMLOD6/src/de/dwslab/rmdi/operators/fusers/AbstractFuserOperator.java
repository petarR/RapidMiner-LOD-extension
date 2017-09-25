package de.dwslab.rmdi.operators.fusers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lod.rdf.model.RdfHolder;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;

import de.dwslab.rmdi.fusion.fusers.AbstractFuser;
import de.dwslab.rmdi.schemamatching.models.MatcherResults;

public abstract class AbstractFuserOperator extends Operator {

	/**
	 * the joined example set
	 */
	protected InputPort mInputPortExampleSet;

	/**
	 * delivers the ExampleSet
	 * 
	 */
	protected OutputPort exampleSetOutputPort;

	/**
	 * delivers the matching results of the operator
	 * 
	 */
	protected InputPort matchingResultsInputPort;

	/**
	 * delivers the matching results of the operator
	 * 
	 */
	protected InputPort rdfDataInputPort;

	/**
	 * delivers the joined RDF
	 */
	protected OutputPort joinedRdfOutputPort;

	// variables
	ExampleSet inputExampleSet;

	/**
	 * holds all rdfs
	 */
	MatcherResults matchingResults;

	/**
	 * holds all parameters for the given LOD matcher
	 */
	Map<String, Object> fusingParamteres;

	/**
	 * the fuser to be used
	 */
	AbstractFuser fuser;

	/**
	 * the passed holder from the matching operator
	 */
	RdfHolder rdfHolder;

	public static final String PARAMETER_RESOLVE_STRING_DUPLICATES = "Conflict resolution (String)";
	public static final String PARAMETER_RESOLVE_NUMERIC_DUPLICATES = "Conflict resolution (Numeric)";

	public static final String[] CONFLICT_STRING_VECTOR = new String[] {
			"First", "Voting", "Random", "Longest" };

	public static final String[] CONFLICT_NUMERIC_VECTOR = new String[] {
			"First", "Median", "Average", "Voting", "Random" };

	public AbstractFuserOperator(OperatorDescription description) {
		super(description);
		mInputPortExampleSet = getInputPorts().createPort("Example Set",
				ExampleSet.class);
		mInputPortExampleSet.addPrecondition(new SimplePrecondition(
				mInputPortExampleSet, new MetaData(ExampleSet.class)));

		exampleSetOutputPort = getOutputPorts().createPort("Example Set");
		getTransformer().addPassThroughRule(mInputPortExampleSet,
				exampleSetOutputPort);

		matchingResultsInputPort = getInputPorts().createPort(
				"Matching Results", MatcherResults.class);

		rdfDataInputPort = getInputPorts().createPort("RDF", RdfHolder.class);

		joinedRdfOutputPort = getOutputPorts().createPort("RDF joined");
	}

	@Override
	public void doWork() throws OperatorException {
		initVariables();
		try {
			getFuserParameters();
			initializeFuser();
			fuser.doFusing();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// create the joined RDF if needed
		RdfHolder joinedHolder = null;
		if (joinedRdfOutputPort.isConnected()) {
			joinedHolder = RdfHolder.mergeHolders(rdfHolder.getSubRdfHolders());
		}
		joinedRdfOutputPort.deliver(joinedHolder);
		exampleSetOutputPort.deliver(inputExampleSet);

	}

	protected abstract void initializeFuser();

	protected void getFuserParameters() throws UndefinedParameterError {
		fusingParamteres
				.put(PARAMETER_RESOLVE_STRING_DUPLICATES,
						CONFLICT_STRING_VECTOR[getParameterAsInt(PARAMETER_RESOLVE_STRING_DUPLICATES)]);
		fusingParamteres
				.put(PARAMETER_RESOLVE_NUMERIC_DUPLICATES,
						CONFLICT_NUMERIC_VECTOR[getParameterAsInt(PARAMETER_RESOLVE_NUMERIC_DUPLICATES)]);

	}

	protected void initVariables() throws UserError {

		inputExampleSet = mInputPortExampleSet.getData(ExampleSet.class);

		matchingResults = matchingResultsInputPort
				.getData(MatcherResults.class);

		fusingParamteres = new HashMap<String, Object>();
		rdfHolder = rdfDataInputPort.getData(RdfHolder.class);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeCategory(
				PARAMETER_RESOLVE_STRING_DUPLICATES,
				"Select String Resolution Strategy", CONFLICT_STRING_VECTOR, 0,
				false));
		types.add(new ParameterTypeCategory(
				PARAMETER_RESOLVE_NUMERIC_DUPLICATES,
				"Select Numeric Resolution Strategy", CONFLICT_NUMERIC_VECTOR,
				0, false));

		return types;
	}

}
