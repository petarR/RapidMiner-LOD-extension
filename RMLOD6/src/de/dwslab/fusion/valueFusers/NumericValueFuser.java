package de.dwslab.fusion.valueFusers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NumericValueFuser extends AbstractValueFuser {

	@Override
	public String fuse(List<String> values) {
		if (values == null || values.size() == 0)
			return null;
		switch (selectedApproach) {
		case First:
			return values.get(0);
		case Voting:
			return NominalValueFuser.voteForFinalValue(values);

		case Random:
			return NominalValueFuser.getRandom(values);
		case Median:
			return getMedianValue(values);

		case Average:
			return getAverage(values);
		default:
			break;
		}

		return null;
	}

	/**
	 * gets the median of all values
	 * 
	 * @param valuesList
	 * @return
	 */
	private String getMedianValue(List<String> valuesList) {
		try {
			List<Double> values = new ArrayList<Double>();

			for (String rV : valuesList) {
				values.add(Double.parseDouble(rV));
			}
			Collections.sort(values);
			double value = (values.size() % 2 == 0) ? values
					.get(values.size() / 2) : values.get(values.size() / 2 + 1);
			return Double.toString(value);
		} catch (Exception e) {
			return NominalValueFuser.getRandom(valuesList);
		}
	}

	/**
	 * returns average of all values
	 * 
	 * @param values
	 * @return
	 */
	private String getAverage(List<String> values) {

		double finalValue = 0;
		try {
			for (int i = 0; i < values.size(); i++) {
				double curValue = Double.parseDouble((String) values.get(i));
				finalValue += curValue;
			}
		} catch (Exception e) {
			return NominalValueFuser.getRandom(values);
		}
		return Double.toString(finalValue / values.size());
	}
}
