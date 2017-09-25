package lod.generators.smart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

public class ClassificationFeatureGenerator {

	Map<String, TargetClass> classes;

	ExampleSet exampleSet;

	/**
	 * defines the min frequency for one feature in one class to be included in
	 * the feature set
	 */
	double minFrequency;

	/**
	 * defines the max frequency for one feature in the other classes to be
	 * included in the feature set
	 */
	double maxFrequency;

	/**
	 * holds the unique attributes
	 */
	Map<String, Integer> uniqueAttrs;

	ArrayList<String> attrsBypsass;

	QueryRunner runnerWrapper;

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

	public Map<String, TargetClass> getClasses() {
		return classes;
	}

	public void setClasses(Map<String, TargetClass> classes) {
		this.classes = classes;
	}

	public ExampleSet getExampleSet() {
		return exampleSet;
	}

	public void setExampleSet(ExampleSet exampleSet) {
		this.exampleSet = exampleSet;
	}

	public double getMinFrequency() {
		return minFrequency;
	}

	public void setMinFrequency(double minFrequency) {
		this.minFrequency = minFrequency;
	}

	public double getMaxFrequency() {
		return maxFrequency;
	}

	public void setMaxFrequency(double maxFrequency) {
		this.maxFrequency = maxFrequency;
	}

	public Map<String, Integer> getUniqueAttrs() {
		return uniqueAttrs;
	}

	public void setUniqueAttrs(Map<String, Integer> uniqueAttrs) {
		this.uniqueAttrs = uniqueAttrs;
	}

	public ClassificationFeatureGenerator(ExampleSet exampleSet,
			double minFrequency, double maxFrequency,
			SPARQLEndpointQueryRunner queryRunner,
			ArrayList<String> attrsBypsass) {

		this.classes = new HashMap<String, TargetClass>();
		this.exampleSet = exampleSet;
		this.minFrequency = minFrequency;
		this.maxFrequency = maxFrequency;
		this.uniqueAttrs = new HashMap<String, Integer>();
		this.runnerWrapper = new QueryRunner(queryRunner);
		this.attrsBypsass = attrsBypsass;
	}

	public void calculateFeatures() throws OperatorException {

		for (Example ex : exampleSet) {
			String label = ex.getValueAsString(ex.getAttributes().getLabel());
			TargetClass targetClass = new TargetClass(label);
			if (classes.containsKey(label)) {
				targetClass = classes.get(label);
			} else {
				classes.put(label, targetClass);
			}
			Instance inst = new Instance(ex.getValueAsString(ex.getAttributes()
					.get(attrsBypsass.get(0))), ex);

			targetClass.getInstances().put(inst.getUri(), inst);
			runnerWrapper.processInstance(inst, targetClass.getClassFeatures());
		}

		removeFeaturesBasedOnFrequency();

		//pairFeatures();

		// decide all new features
		for (Entry<String, TargetClass> entry : classes.entrySet()) {
			for (String feature : entry.getValue().getClassFeatures().keySet()) {
				uniqueAttrs.put(feature, 0);
			}
		}

		addNewAttributes();

	}

	private void removeFeaturesBasedOnFrequency() {
		// keeps all features that need to be removed

		Map<TargetClass, List<String>> featuresToRemoveFromClasses = new HashMap<TargetClass, List<String>>();

		for (Entry<String, TargetClass> entry : classes.entrySet()) {

			List<String> toRemoveFeatures = new ArrayList<String>();
			int minFeatureInt = (int) (entry.getValue().getInstances().size() * minFrequency);
			minFeatureInt = (minFeatureInt == 0) ? 2 : minFeatureInt;

			for (Entry<String, Integer> feature : entry.getValue()
					.getClassFeatures().entrySet()) {
				if (feature.getValue() < minFeatureInt) {
					toRemoveFeatures.add(feature.getKey());

				} else {

					// check for max frequency in the other classes
					for (Entry<String, TargetClass> entry2 : classes.entrySet()) {
						if (entry2.getValue() != entry.getValue()) {
							int maxFreq = (int) (entry2.getValue()
									.getInstances().size() * maxFrequency);
							if (entry2.getValue().getClassFeatures()
									.containsKey(feature.getKey())
									&& entry2.getValue().getClassFeatures()
											.get(feature.getKey()) > maxFreq) {
								toRemoveFeatures.add(feature.getKey());
							}
						}
					}
				}
			}

			featuresToRemoveFromClasses.put(entry.getValue(), toRemoveFeatures);

		}

		for (Entry<TargetClass, List<String>> pair : featuresToRemoveFromClasses
				.entrySet()) {

			for (String toRemove : pair.getValue()) {
				pair.getKey().getClassFeatures().remove(toRemove);

			}
		}

	}

	private void addNewAttributes() {

		for (String newAttr : uniqueAttrs.keySet()) {
			Attribute attr = BaseGenerator.addAtribute(newAttr,
					Ontology.NUMERICAL, exampleSet);

			for (Example ex : exampleSet) {
				String label = ex.getValueAsString(ex.getAttributes()
						.getLabel());
				TargetClass clazz = classes.get(label);
				if (clazz.getClassFeatures().containsKey(newAttr)) {
					int value = 0;
					try {
						value = classes
								.get(label)
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

	public void pairFeatures() {
		for (Entry<String, TargetClass> entry : classes.entrySet()) {

			for (Instance inst : entry.getValue().getInstances().values()) {
				for (Entry<String, List<String>> lastNode : inst
						.getEndingNodeFeatures().entrySet())

				{
					for (Instance inst2 : entry.getValue().getInstances()
							.values()) {
						if (inst != inst2) {

							if (inst2.getEndingNodeFeatures().containsKey(
									lastNode.getKey()))
								for (String feat1 : lastNode.getValue()) {

									for (String feat2 : inst2
											.getEndingNodeFeatures().get(
													lastNode.getKey())) {
										if (!feat1.equals(feat2)) {
											String newFeature = feat1 + "_"
													+ feat2;
											int n = inst.addFeature(newFeature);
											if (n == 1)
												addToClassFeatures(entry
														.getValue()
														.getClassFeatures(),
														newFeature);
											n = inst2.addFeature(newFeature);
											if (n == 1)
												addToClassFeatures(entry
														.getValue()
														.getClassFeatures(),
														newFeature);
										}

									}
								}
						}
					}
				}
			}
		}
	}

	public void addToClassFeatures(Map<String, Integer> classUniques,
			String feature) {

		int n = 1;
		if (classUniques.containsKey(feature))
			n += classUniques.get(feature);

		classUniques.put(feature, n);
	}

	public static void main(String[] args) {
		ClassificationFeatureGenerator cl = new ClassificationFeatureGenerator(
				null, 0, 0, new SPARQLEndpointQueryRunner(
						"http://dbpedia.org/sparql", "backupRunner", 60000, 10,
						10000, true, true), null);
		try {
			cl.calculateFeatures();
		} catch (OperatorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
