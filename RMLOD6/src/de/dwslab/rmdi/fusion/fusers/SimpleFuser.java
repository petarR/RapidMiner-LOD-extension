package de.dwslab.rmdi.fusion.fusers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lod.utils.AttributeTypeGuesser;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;

import de.dwslab.fusion.valueFusers.AbstractValueFuser;
import de.dwslab.fusion.valueFusers.NominalValueFuser;
import de.dwslab.fusion.valueFusers.NumericValueFuser;
import de.dwslab.rmdi.schemamatching.models.MatcherResults;
import de.dwslab.rmdi.utils.ExampleSetHelper;

public class SimpleFuser extends AbstractFuser {

	public SimpleFuser(Map<String, Object> fusingParameters, ExampleSet set,
			MatcherResults matcherResuts) {
		super(fusingParameters, set, matcherResuts);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void resolveDuplicatesForAttributes(List<Attribute> attributes,
			DataType datatype) {

		if (attributes == null || attributes.size() == 0)
			return;
		// add new attribute
		String attributeName = "";
		for (Attribute a : attributes) {
			attributeName += a.getName() + "_";
		}
		if (attributeName.equals(""))
			return;

		if (set.getAttributes().get(attributeName) != null) {
			return;
		}
		// create the fuser
		AbstractValueFuser fuser = null;
		if (datatype == DataType.numeric) {
			fuser = new NumericValueFuser();
			fuser.setSelectedApproach(numericFusionSelectedApproach);
		} else {
			fuser = new NominalValueFuser();
			fuser.setSelectedApproach(stringFusionSelectedApproach);
		}

		int ontology = com.rapidminer.tools.Ontology.NUMERICAL;
		if (datatype != DataType.numeric)
			ontology = com.rapidminer.tools.Ontology.NOMINAL;
		ExampleSetHelper.addAtribute(attributeName, ontology, set);

		for (Example e : set) {
			List<String> valuesToResolve = new ArrayList<String>();
			for (Attribute att : attributes) {
				if (e.getValue(att) != Double.NaN)
					valuesToResolve.add(e.getValueAsString(att));
			}
			String newValue = fuser.fuse(valuesToResolve);
			e.setValue(set.getAttributes().get(attributeName),
					AttributeTypeGuesser.getValueForAttribute(set
							.getAttributes().get(attributeName), newValue));
		}

	}

}
