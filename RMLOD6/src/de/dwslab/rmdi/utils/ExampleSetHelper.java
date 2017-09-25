package de.dwslab.rmdi.utils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.tools.Ontology;

public class ExampleSetHelper {

	/**
	 * Adds new attribute to a given {@link ExampleSet}
	 * 
	 * @param attributeName
	 *            {@link String} Name of the attribute that is going to be
	 *            included into the {@link ExampleSet}.
	 * @param ontology
	 *            Type of {@link Ontology}
	 * @param exampleSet
	 *            The given {@link ExampleSet}
	 */
	public static void addAtribute(String attributeName, int ontology,
			ExampleSet exampleSet) {
		Attribute attribute = AttributeFactory.createAttribute(attributeName,
				ontology);
		exampleSet.getAttributes().addRegular(attribute);
		exampleSet.getExampleTable().addAttribute(attribute);
	}

}
