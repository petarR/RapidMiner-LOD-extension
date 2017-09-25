package de.dwslab.rmdi.operators.matchers;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lod.rdf.model.RdfHolder;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPortExtender;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;

import de.dwslab.rmdi.schemamatching.matchers.lod.AbstractLODMatcher;
import de.dwslab.rmdi.schemamatching.models.MatcherResults;

public abstract class AbstractLODMatcherOperator extends Operator {

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
	protected OutputPort matchingResultsOutputPort;

	/**
	 * delivers the joined RDF
	 * 
	 */
	protected OutputPort joinedRDFOutputPort;

	// protected OutputPort joinedRdfOutputPort;

	private final InputPortExtender inputExtender = new InputPortExtender(
			"RDF", getInputPorts(), new MetaData(RdfHolder.class), 1);

	// variables
	ExampleSet inputExampleSet;

	/**
	 * holds all rdfs
	 */
	List<RdfHolder> inputRDFs;

	/**
	 * holds all parameters for the given LOD matcher
	 */
	Map<String, Object> matchingParamteres;

	/**
	 * the matcher to be used for matching
	 */
	AbstractLODMatcher matcher;

	public AbstractLODMatcherOperator(OperatorDescription description) {
		super(description);
		mInputPortExampleSet = getInputPorts().createPort("Example Set",
				ExampleSet.class);
		mInputPortExampleSet.addPrecondition(new SimplePrecondition(
				mInputPortExampleSet, new MetaData(ExampleSet.class)));

		inputExtender.start();
		// getTransformer()
		// .addRule(
		// inputExtender
		// .makeFlatteningPassThroughRule(joinedRDFOutputPort));

		exampleSetOutputPort = getOutputPorts().createPort("Example Set");
		getTransformer().addPassThroughRule(mInputPortExampleSet,
				exampleSetOutputPort);

		matchingResultsOutputPort = getOutputPorts().createPort(
				"Matching Results");
		getTransformer().addRule(
				new GenerateNewMDRule(matchingResultsOutputPort,
						MatcherResults.class));
		joinedRDFOutputPort = getOutputPorts().createPassThroughPort("RDF");
		getTransformer().addRule(
				new GenerateNewMDRule(joinedRDFOutputPort, RdfHolder.class));
	}

	@Override
	public void doWork() throws OperatorException {
		initVariables();
		try {
			getMatcherParameters();
			initializeMatcher();
			matcher.doMatching();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		exampleSetOutputPort.deliver(inputExampleSet);
		matchingResultsOutputPort.deliver(matcher.getMatchingResults());

		// create the output rdf
		RdfHolder joinedHolder = new RdfHolder();
		joinedHolder.setSubRdfHolders(inputRDFs);
		joinedHolder.populateHolderFromSubHolders();
		joinedRDFOutputPort.deliver(joinedHolder);

	}

	protected void initVariables() throws UserError {
		inputExampleSet = mInputPortExampleSet.getData(ExampleSet.class);
		inputRDFs = new LinkedList<RdfHolder>();
		List<RdfHolder> tmpHolders = inputExtender.getData(RdfHolder.class,
				true);
		for (RdfHolder holder : tmpHolders) {
			if (holder.getSubRdfHolders().size() == 0) {
				inputRDFs.add(holder);
				continue;
			}
			for (RdfHolder subHolder : holder.getSubRdfHolders())
				inputRDFs.add(subHolder);
		}

		// inputRDFs = inputExtender.getData(RdfHolder.class, true);

		matchingParamteres = new HashMap<String, Object>();

	}

	/**
	 * sets all needed parameters
	 * 
	 * @throws Exception
	 */
	protected abstract void getMatcherParameters() throws Exception;

	/**
	 * creates new matcher object
	 */
	protected abstract void initializeMatcher();

}
