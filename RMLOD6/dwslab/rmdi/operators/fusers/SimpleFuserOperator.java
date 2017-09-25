package de.dwslab.rmdi.operators.fusers;

import com.rapidminer.operator.OperatorDescription;

import de.dwslab.rmdi.fusion.fusers.SimpleFuser;

public class SimpleFuserOperator extends AbstractFuserOperator {

	public SimpleFuserOperator(OperatorDescription description) {
		super(description);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void initializeFuser() {
		fuser = new SimpleFuser(fusingParamteres, inputExampleSet,
				matchingResults);

	}
}
