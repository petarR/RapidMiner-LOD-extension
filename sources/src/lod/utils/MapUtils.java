package lod.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapUtils {
	public static String mapToString(Map<String, String> map) {
		StringBuilder stringBuilder = new StringBuilder();

		for (String key : map.keySet()) {
			if (stringBuilder.length() > 0) {
				stringBuilder.append("&");
			}
			String value = map.get(key);
			try {
				stringBuilder.append((key != null ? URLEncoder.encode(key,
						"UTF-8") : ""));
				stringBuilder.append("=");
				stringBuilder.append(value != null ? URLEncoder.encode(value,
						"UTF-8") : "");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(
						"This method requires UTF-8 encoding support", e);
			}
		}

		return stringBuilder.toString();
	}

	public static Map<String, String> stringToMap(String input) {
		Map<String, String> map = new LinkedHashMap<String, String>();

		String[] nameValuePairs = input.split("&");
		for (String nameValuePair : nameValuePairs) {
			String[] nameValue = nameValuePair.split("=");
			try {
				map.put(URLDecoder.decode(nameValue[0], "UTF-8"),
						nameValue.length > 1 ? URLDecoder.decode(nameValue[1],
								"UTF-8") : "");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(
						"This method requires UTF-8 encoding support", e);
			}
		}

		return map;
	}

	public static String mapDoubleToString(Map<String, Map<String, String>> map) {
		StringBuilder stringBuilder = new StringBuilder();

		for (String key : map.keySet()) {
			if (stringBuilder.length() > 0) {
				stringBuilder.append("&");
			}
			Map<String, String> value = map.get(key);
			try {
				stringBuilder.append((key != null ? URLEncoder.encode(key,
						"UTF-8") : ""));
				stringBuilder.append("=");
				stringBuilder.append(value != null ? URLEncoder.encode(
						mapToString(value), "UTF-8") : "");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(
						"This method requires UTF-8 encoding support", e);
			}
		}

		return stringBuilder.toString();
	}

	public static Map<String, Map<String, String>> stringToDoubleMap(
			String input) {
		Map<String, Map<String, String>> map = new LinkedHashMap<String, Map<String, String>>();

		String[] nameValuePairs = input.split("&");
		for (String nameValuePair : nameValuePairs) {
			String[] nameValue = nameValuePair.split("=");
			try {
				map.put(URLDecoder.decode(nameValue[0], "UTF-8"),
						stringToMap(URLDecoder.decode(nameValue[1])));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(
						"This method requires UTF-8 encoding support", e);
			}
		}

		return map;
	}

	public static void main(String[] args) {

		Map<String, String> map = new HashMap<String, String>();
		map.put("color", "red");
		map.put("symbols", "orange");
		map.put("empty", "is not ");
		// String output = MapUtils.mapToString(map);
		// System.out.println(output);
		// Map<String, String> parsedMap = MapUtils.stringToMap(output);
		Map<String, Map<String, String>> mapBig = new HashMap<String, Map<String, String>>();
		mapBig.put("first", map);
		mapBig.put("second", map);

		String output = MapUtils.mapDoubleToString(mapBig);
		System.out.println(output);

		mapBig = stringToDoubleMap(output);
		System.out.println(mapBig.size());

	}
}