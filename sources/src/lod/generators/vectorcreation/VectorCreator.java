/*
 *  RapidMiner Text Processing Extension
 *
 *  Copyright (C) 2001-2013 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package lod.generators.vectorcreation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;

/**
 * @author petar
 * 
 */
public class VectorCreator {

	/**
	 * Create a word vector from term frequencies
	 */
	public void createVector(ExampleSet set, int option,
			Map<String, String> addedAttributeOverall) {

		switch (option) {
		case 0:
			// boolean and binary
			generateBinaryRepresentation(set, option,
					addedAttributeOverall.values());
			break;
		case 2:
			generateTForTfIDFrepresentation(set,
					addedAttributeOverall.values(), false);
			break;
		case 3:
			generateTForTfIDFrepresentation(set,
					addedAttributeOverall.values(), true);
			break;
		default:
			break;
		}

	}

	/**
	 * Create a word vector from term frequencies
	 */
	public void createVector(ExampleSet set, int option,
			Collection<String> addedAttrs) {

		switch (option) {
		case 0:
			// boolean and binary
			generateBinaryRepresentation(set, option, addedAttrs);
			break;
		case 2:
			generateTForTfIDFrepresentation(set, addedAttrs, false);
			break;
		case 3:
			generateTForTfIDFrepresentation(set, addedAttrs, true);
			break;
		default:
			break;
		}

	}

	private void generateTForTfIDFrepresentation(ExampleSet set,
			Collection<String> addedAttrs, boolean TFIDF) {
		// used for TF-IDF only
		Map<String, Integer> countAppearencesInDoc = new HashMap<String, Integer>();
		for (Example example : set) {
			// count total number of features in document
			int totalFeatures = 0;
			for (String addedAttr : addedAttrs) {
				Attribute attribute = set.getAttributes().get(addedAttr);

				int value = (int) Double.parseDouble(example
						.getValueAsString(attribute));
				totalFeatures += value;

				// add the document appearances if TFIDF
				if (value > 0 && TFIDF) {
					int apps = 1;
					if (countAppearencesInDoc.containsKey(attribute.getName()))
						apps += countAppearencesInDoc.get(attribute.getName());
					countAppearencesInDoc.put(attribute.getName(), apps);
				}

			}

			// calculate TF
			for (String addedAttr : addedAttrs) {
				Attribute attribute = set.getAttributes().get(addedAttr);
				double value = Double.parseDouble(example
						.getValueAsString(attribute));
				// calculate term frequency
				double tf = value / totalFeatures;

				example.setValue(attribute, tf);
			}
		}

		// set TFIDF if needed
		if (TFIDF) {
			for (Example example : set) {
				for (String addedAttr : addedAttrs) {
					Attribute attribute = set.getAttributes().get(addedAttr);

					// get term frequency
					double tf = 0;
					try {
						tf = Double.parseDouble(example
								.getValueAsString(attribute));
					} catch (Exception e) {

					}

					// calculate idf
					double idf = Math.log(((double) set.size())
							/ ((double) countAppearencesInDoc.get(attribute
									.getName())));

					// tf-idf
					double finalValue = tf * idf;

					example.setValue(attribute, finalValue);
				}

			}
		}

	}

	// /**
	// * @param set
	// * @param option
	// * @param addedAttributeOverall
	// */
	// private void generateBinaryRepresentation(ExampleSet set, int option,
	// Map<String, String> addedAttributeOverall) {
	// ge
	// for (Example example : set) {
	//
	// for (String addedAttr : addedAttributeOverall.values()) {
	// Attribute attribute = set.getAttributes().get(addedAttr);
	// int value = (int) Double.parseDouble(example
	// .getValueAsString(attribute));
	// if (value > 0)
	// example.setValue(attribute, 1);
	// else
	// example.setValue(attribute, 0);
	// }
	// }
	//
	// }

	/**
	 * generates the binary vector
	 * 
	 * @param set
	 * @param option
	 * @param addedAttrs
	 */
	private void generateBinaryRepresentation(ExampleSet set, int option,
			Collection<String> addedAttrs) {
		for (Example example : set) {

			for (String addedAttr : addedAttrs) {
				Attribute attribute = set.getAttributes().get(addedAttr);
				int value = (int) Double.parseDouble(example
						.getValueAsString(attribute));
				if (value > 0)
					example.setValue(attribute, 1);
				else
					example.setValue(attribute, 0);
			}
		}

	}

	private void generateTFIDF(ExampleSet set) {
		// TODO Auto-generated method stub

	}
}
