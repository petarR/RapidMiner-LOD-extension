package lod.generators.smart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import lod.generators.BaseGenerator;
import lod.generators.smart.model.Instance;
import lod.generators.smart.model.TargetClass;
import lod.generators.smart.utils.QueryRunner;
import lod.sparql.SPARQLEndpointQueryRunner;
import lod.utils.AttributeTypeGuesser;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.Ontology;

public class ClassificationTestFeatureGenerator {

	ExampleSet exampleSet;

	ExampleSet trainingExampleSet;

	/**
	 * holds the unique attributes
	 */
	Map<String, Integer> uniqueAttrs;

	ArrayList<String> attrsBypsass;

	QueryRunner runnerWrapper;

	TargetClass clazz;

	public ArrayList<String> getAttrsBypsass() {
		return attrsBypsass;
	}

	public QueryRunner getRunnerWrapper() {
		return runnerWrapper;
	}

	public void setAttrsBypsass(ArrayList<String> attrsBypsass) {
		this.attrsBypsass = attrsBypsass;
	}

	public void setRunnerWrapper(QueryRunner runnerWrapper) {
		this.runnerWrapper = runnerWrapper;
	}

	public ExampleSet getExampleSet() {
		return exampleSet;
	}

	public void setExampleSet(ExampleSet exampleSet) {
		this.exampleSet = exampleSet;
	}

	public Map<String, Integer> getUniqueAttrs() {
		return uniqueAttrs;
	}

	public void setUniqueAttrs(Map<String, Integer> uniqueAttrs) {
		this.uniqueAttrs = uniqueAttrs;
	}

	public ClassificationTestFeatureGenerator(ExampleSet exampleSet,
			ExampleSet trainingSet, SPARQLEndpointQueryRunner queryRunner,
			ArrayList<String> attrsBypsass) {

		this.exampleSet = exampleSet;

		this.trainingExampleSet = trainingSet;

		this.uniqueAttrs = new HashMap<String, Integer>();
		this.runnerWrapper = new QueryRunner(queryRunner);
		this.attrsBypsass = attrsBypsass;
	}

	public void calculateFeatures() throws OperatorException {

		clazz = new TargetClass("neutral");

		for (Example ex : exampleSet) {

			Instance inst = new Instance(ex.getValueAsString(ex.getAttributes()
					.get(attrsBypsass.get(0))), ex);
			clazz.getInstances().put(inst.getUri(), inst);
			runnerWrapper.processInstance(inst, clazz.getClassFeatures());
		}

		// decide all new features

		for (String feature : clazz.getClassFeatures().keySet()) {
			try {
				if (trainingExampleSet.getAttributes().get(feature) != null)

					uniqueAttrs.put(feature, 0);
			} catch (Exception e) {
				// do nothing
			}
		}

		addNewAttributes();

	}

	private void addNewAttributes() {

		for (String newAttr : uniqueAttrs.keySet()) {
			Attribute attr = BaseGenerator.addAtribute(newAttr,
					Ontology.NUMERICAL, exampleSet);

			for (Example ex : exampleSet) {
				String label = ex.getValueAsString(ex.getAttributes()
						.getLabel());

				if (clazz.getClassFeatures().containsKey(newAttr)) {
					int value = 0;
					try {
						value = clazz
								.getInstances()
								.get(ex.getValueAsString(ex.getAttributes()
										.get(attrsBypsass.get(0))))
								.getInstanceFeatures().get(newAttr);
					} catch (Exception e) {
						// the feature doesn't exists
						// do nothing
					}
					ex.setValue(attr,
							AttributeTypeGuesser.getValueForAttribute(attr,
									Integer.toString(value)));
				}

			}
		}

	}

}
