package de.dwslab.rmdi.configurations;

import java.util.Map;

public class ConfUtils {

	public enum DATATYPE {
		INT, STRING, DOUBLE, BOOLEAN
	}

	public static Object readParameter(Map<String, String> parameters,
			String paramName, DATATYPE datatype) {
		if (!parameters.containsKey(paramName))
			return null;
		switch (datatype) {

		case INT:
			try {
				return Integer.parseInt(parameters.get(paramName));
			} catch (Exception e) {
				// TODO: handle exception
			}
			break;
		case DOUBLE:
			try {
				return Double.parseDouble(parameters.get(paramName));
			} catch (Exception e) {
				// TODO: handle exception
			}
			break;
		case BOOLEAN:
			try {
				return Boolean.parseBoolean(parameters.get(paramName));
			} catch (Exception e) {
				// TODO: handle exception
			}
			break;
		case STRING:
			return parameters.get(paramName);

		default:
			break;
		}

		return null;
	}
}
