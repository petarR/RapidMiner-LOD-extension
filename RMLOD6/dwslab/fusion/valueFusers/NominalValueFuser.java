package de.dwslab.fusion.valueFusers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class NominalValueFuser extends AbstractValueFuser {

	@Override
	public String fuse(List<String> values) {
		if (values == null || values.size() == 0)
			return null;
		switch (selectedApproach) {
		case First:
			return (String) values.get(0);
		case Voting:
			return voteForFinalValue(values);

		case Random:
			return getRandom(values);
		case Longest:
			return getLargestValue(values);
		default:
			break;
		}

		return null;
	}

	/**
	 * returns random value
	 * 
	 * @param values
	 * @return
	 */
	public static String getRandom(List<String> values) {
		Random randomGenerator = new Random();
		int n = randomGenerator.nextInt(values.size() - 1);
		if (n > 0 && n < values.size())
			return (String) values.get(n);
		return (String) values.get(0);
	}

	/**
	 * implements voting
	 * 
	 * @param rightValues
	 * @param leftValue
	 * @return
	 */
	public static String voteForFinalValue(List<String> valuesList) {
		Map<String, Integer> values = new HashMap<String, Integer>();
		Object maxVoteValue = valuesList.get(0);
		int max = 1;
		for (String rV : valuesList) {
			int curV = 1;
			if (values.containsKey(rV)) {
				curV += values.get(rV);
			}
			values.put(rV, curV);
			if (curV > max) {
				max = curV;
				maxVoteValue = rV;
			}
		}
		return (String) maxVoteValue;
	}

	/**
	 * returns the longest value
	 * 
	 * @param rightValues
	 * @param leftValue
	 * @return
	 */
	public String getLargestValue(List<String> valuesList) {
		int maxLength = Integer.MIN_VALUE;
		String finalString = (String) valuesList.get(0);
		for (int i = 0; i < valuesList.size(); i++) {
			String s = (String) valuesList.get(i);
			if (s.length() > maxLength) {
				maxLength = s.length();
				finalString = s;
			}
		}
		return finalString;
	}

}
