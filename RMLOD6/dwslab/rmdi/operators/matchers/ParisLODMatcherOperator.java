package de.dwslab.rmdi.operators.matchers;

import java.util.List;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;

import de.dwslab.rmdi.schemamatching.matchers.lod.ParisMatcher;

public class ParisLODMatcherOperator extends AbstractLODMatcherOperator {
	// operator parameters
	public static final String PARAMETER_LITERAL_SIMILARITY_DISTANCE = "Literal Similarity Distance";
	public static final String PARAMTER_NUMBER_OF_ITERATIONS = "Number of iterations";
	public static final String PARAMETER_ALLIGNMENT_THRESHOLD = "Alignment Acceptance Threshold";
	public static final String PARAMETER_JOIN_LENGTH_LIMIT = "Join Length Limit";
	public static final String PARAMETER_BOTH_WAYS = "Match Both Ways";
	public static final String PARAMETER_NORMALIZE_STRINGS = "Normalize Strings";
	public static final String PARAMETER_NORMALIZE_DATES = "Normalize Dates";
	public static final String PARAMETER_POST_LITERAL_DISTANCE_THRESHOLD = "Post Literal Distance Threshold";

	public static final String PARAMETER_TABLE_ORDER_EQUALITIES = "Entities in same row are equal";

	public static final String[] LITERAL_DISTANCE_VECTOR = new String[] {
			"IDENTITY", "BAGOFCHARS", "NORMALIZE", "BAGOFWORDS", "LEVENSHTEIN",
			"SHINGLING", "SHINGLINGLEVENSHTEIN" };

	public ParisLODMatcherOperator(OperatorDescription description) {
		super(description);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void initializeMatcher() {
		matcher = new ParisMatcher(matchingParamteres, inputRDFs);

	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeCategory(
				PARAMETER_LITERAL_SIMILARITY_DISTANCE,
				"Select literal similarity funcion", LITERAL_DISTANCE_VECTOR,
				0, false));
		types.add(new ParameterTypeInt(PARAMTER_NUMBER_OF_ITERATIONS,
				"Number of matching iteration", 1, 999, 10, false));

		types.add(new ParameterTypeDouble(

		PARAMETER_ALLIGNMENT_THRESHOLD,
				"Defines the probability threshold for accpeting alignment ",
				0, 1, 0.8, false));

		types.add(new ParameterTypeInt(PARAMETER_JOIN_LENGTH_LIMIT,
				"Join Lenght Limit", 1, 9, 1, false));
		// types.add(new ParameterTypeBoolean(PARAMETER_BOTH_WAYS,
		// "If checked, it will match the data in both directions", true,
		// false));

		types.add(new ParameterTypeBoolean(PARAMETER_TABLE_ORDER_EQUALITIES,
				"Check if the entities that appear in the same row are equal",
				true, false));

		types.add(new ParameterTypeBoolean(PARAMETER_NORMALIZE_STRINGS,
				"Normalize strings", true, false));

		types.add(new ParameterTypeBoolean(PARAMETER_NORMALIZE_DATES,
				"Normalize dates", true, false));

		types.add(new ParameterTypeDouble(
				PARAMETER_POST_LITERAL_DISTANCE_THRESHOLD,
				"Defines the threshold for similar literarls", 0, 1, 0.78,
				false));

		return types;
	}

	@Override
	protected void getMatcherParameters() throws Exception {
		int litOption = getParameterAsInt(PARAMETER_LITERAL_SIMILARITY_DISTANCE);
		matchingParamteres.put(PARAMETER_LITERAL_SIMILARITY_DISTANCE,
				LITERAL_DISTANCE_VECTOR[litOption]);

		matchingParamteres.put(PARAMTER_NUMBER_OF_ITERATIONS,
				getParameterAsInt(PARAMTER_NUMBER_OF_ITERATIONS));

		matchingParamteres.put(PARAMETER_JOIN_LENGTH_LIMIT,
				getParameterAsInt(PARAMETER_JOIN_LENGTH_LIMIT));

		matchingParamteres.put(PARAMETER_NORMALIZE_DATES,
				getParameterAsBoolean(PARAMETER_NORMALIZE_DATES));

		matchingParamteres.put(PARAMETER_NORMALIZE_STRINGS,
				getParameterAsBoolean(PARAMETER_NORMALIZE_STRINGS));

		matchingParamteres.put(PARAMETER_NORMALIZE_STRINGS,
				getParameterAsBoolean(PARAMETER_NORMALIZE_STRINGS));

		matchingParamteres
				.put(PARAMETER_POST_LITERAL_DISTANCE_THRESHOLD,
						getParameterAsDouble(PARAMETER_POST_LITERAL_DISTANCE_THRESHOLD));

		matchingParamteres.put(PARAMETER_TABLE_ORDER_EQUALITIES,
				getParameterAsBoolean(PARAMETER_TABLE_ORDER_EQUALITIES));

		matchingParamteres.put(PARAMETER_ALLIGNMENT_THRESHOLD,
				getParameterAsDouble(PARAMETER_ALLIGNMENT_THRESHOLD));
	}

}
