package lod.filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import lod.generators.BaseGenerator;
import lod.utils.HierarchyPair;
import lod.utils.OntologyHierarchy;
import lod.utils.ValueComparator;

import org.apache.commons.lang.SerializationUtils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.tree.NumericalSplitter;
import com.rapidminer.operator.learner.tree.criterions.Criterion;
import com.rapidminer.operator.learner.tree.criterions.InfoGainCriterion;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.parameter.conditions.OrParameterCondition;
import com.rapidminer.tools.container.Tupel;
import com.rapidminer.tools.math.MathFunctions;
import com.rapidminer.tools.math.container.BoundedPriorityQueue;

/**
 * 
 * @author Petar Ristoski
 * 
 */
public class SimpleFilter extends Operator {
	public static final String PARAMETER_FILTER_ATTRS = "Algorithm";

	public static final String[] ROW_TYPES_VALUES = new String[] { "None",
			"SHSEL C", "SHSEL IG", "pruneSHSEL C", "pruneSHSEL IG",
			"TSEL Lift", "TSEL Lift IG", "Hill Climbing", "Greedy Top-Down" };

	public static final String PARAMETER_THRESHOLD = "Threshold";
	public static final String PARAMETER_USE_AVERAGE = "Use Average for Prunning";
	public static final String PARAMETER_PRUNING_THRESHOLD = "Prunning Threshold";

	protected InputPort mInputPortExampleSet;
	protected InputPort mInputPortHierarchy;

	protected OutputPort mOutputPortFilteredSet;
	protected OutputPort mOutputPortTypesHierarchy;

	public SimpleFilter(OperatorDescription description) {
		super(description);

		mInputPortExampleSet = getInputPorts().createPort("Example Set",
				ExampleSet.class);
		mInputPortExampleSet.addPrecondition(new SimplePrecondition(
				mInputPortExampleSet, new MetaData(ExampleSet.class)));

		mInputPortHierarchy = getInputPorts().createPort("Hierarchy",
				OntologyHierarchy.class);

		mOutputPortFilteredSet = getOutputPorts().createPort("Filtered Set");
		getTransformer().addPassThroughRule(mInputPortExampleSet,
				mOutputPortFilteredSet);
		mOutputPortTypesHierarchy = getOutputPorts().createPort(
				"Hierarchy pairs");

		getTransformer().addRule(
				new GenerateNewMDRule(mOutputPortTypesHierarchy,
						OntologyHierarchy.class));

	}

	@Override
	public void doWork() throws OperatorException {

		// clone the exampleSet
		ExampleSet exampleSet = BaseGenerator
				.cloneExampleSet(mInputPortExampleSet.getData(ExampleSet.class));
		OntologyHierarchy hierarchyOriginal = mInputPortHierarchy
				.getData(OntologyHierarchy.class);
		// clone the hierarchys
		OntologyHierarchy hierarchy = null;
		try {
			hierarchy = OntologyHierarchy.clone(hierarchyOriginal);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int selectedFilter = getParameterAsInt(PARAMETER_FILTER_ATTRS);
		Attribute label = exampleSet.getAttributes().getLabel();
		if (label == null
				&& (selectedFilter == 2 || selectedFilter == 4
						|| selectedFilter == 5 || selectedFilter == 6
						|| selectedFilter == 7 || selectedFilter == 8)) {
			throw new OperatorException(
					"Problem in Simple Filter: label attribute must be defined");
		}

		double threshold = getParameterAsDouble(PARAMETER_THRESHOLD);
		switch (selectedFilter) {
		case 1:
			filterAttributesSimpleCorrelation(hierarchy, threshold, exampleSet,
					false);
			break;
		case 2:
			filterAttributesSimpleInfoGain(hierarchy, threshold, exampleSet,
					false);
			break;

		case 3:
			filterAttributesSimpleCorrelation(hierarchy, threshold, exampleSet,
					true);
			break;
		case 4:
			filterAttributesSimpleInfoGain(hierarchy, threshold, exampleSet,
					true);
			break;

		case 5:
			filterAttributesUsingLift(hierarchy, threshold, exampleSet, 1);
			break;
		case 6:
			filterAttributesUsingLift(hierarchy, threshold, exampleSet, 2);
			break;
		case 7:
			exampleSet = filterOutHillClimbing(hierarchy, threshold, exampleSet);
			break;
		case 8:
			filterAttributesUsingGreedy(hierarchy, threshold, exampleSet);
			break;
		default:
			break;
		}

		mOutputPortFilteredSet.deliver(exampleSet);
		mOutputPortTypesHierarchy.deliver(hierarchy);
		super.doWork();
	}

	private void filterAttributesUsingLift(OntologyHierarchy hierarchy,
			double threshold, ExampleSet exampleSet, int option) {
		List<String> selectedAttributes = new ArrayList<String>();
		switch (option) {
		case 1:
			selectedAttributes = selectAttributesFromHierarchyLift(hierarchy,
					calculateLift(exampleSet));
			break;
		case 2:
			selectedAttributes = selectAttributesFromHierarchyLift(hierarchy,
					calculateInfoGainWN(exampleSet));
			break;

		default:
			break;
		}

		List<String> attsToRemove = new ArrayList<String>();
		for (Attribute att : exampleSet.getAttributes()) {
			if (!selectedAttributes.contains(att.getName())) {
				attsToRemove.add(att.getName());
			}
		}
		removeAttributes(exampleSet, attsToRemove, hierarchy);

	}

	/**
	 * this is select_from_tree from the paper
	 * 
	 * @param hierarchy
	 * @param exampleSet
	 * @param calculateLift
	 * @return
	 */
	private List<String> selectAttributesFromHierarchyLift(
			OntologyHierarchy hierarchy, Map<String, Double> calculatedLift) {

		List<String> tmpSelectedAttributes = new ArrayList<String>();
		for (HierarchyPair pair : hierarchy.getHierarchyLeafs()) {

			String selctedFromPath = selectFromPath(pair.getSuperClasses(),
					pair.getCorrespondingAttr().get(0), calculatedLift,
					hierarchy);
			tmpSelectedAttributes.add(selctedFromPath);
		}

		List<String> attsToRemove = new ArrayList<String>();

		List<String> attsToAdd = new ArrayList<String>();
		// line 18-27 from the paper
		for (String selAttr : tmpSelectedAttributes) {

			HierarchyPair pair = hierarchy.getPairByAttributeName(selAttr);
			if (pair == null)
				continue;
			for (String subAttr : pair.getSubClasses()) {
				if (tmpSelectedAttributes.contains(subAttr)) {
					attsToRemove.add(selAttr);
					attsToAdd.addAll(selectAttributesFromHierarchyLift(
							createNewHierarchyFromPair(pair, hierarchy),
							calculatedLift));
					break;
				}
			}
		}

		for (String att : attsToRemove) {
			tmpSelectedAttributes.remove(att);
		}
		for (String att : attsToAdd) {
			tmpSelectedAttributes.add(att);
		}
		return tmpSelectedAttributes;
	}

	/**
	 * create the subtree of the current node
	 * 
	 * @param pair
	 * @param hierarchy
	 * @return
	 */
	private OntologyHierarchy createNewHierarchyFromPair(HierarchyPair pair,
			OntologyHierarchy hierarchy) {
		OntologyHierarchy newhierarchy = new OntologyHierarchy();

		for (String str : pair.getSubClasses()) {
			HierarchyPair childnNode = hierarchy.getPairByAttributeName(str);
			if (childnNode == null) {
				System.out.println("NO chinld node for: " + str);
				continue;
			}
			// remove upper supperclasses
			List<String> classesToRemove = new ArrayList<String>();
			for (String superClass : childnNode.getSuperClasses()) {
				if (!pair.getSubClasses().contains(superClass)
						&& !pair.getBaseClass().equals(superClass)) {
					classesToRemove.add(superClass);
				}
			}
			for (String clazz : classesToRemove) {
				childnNode.getSuperClasses().remove(clazz);
			}
			newhierarchy.addNewPair(childnNode);
		}

		return newhierarchy;
	}

	private String selectFromPath(List<String> attributes, String leafAttr,
			Map<String, Double> calculatedLift, OntologyHierarchy hierarchy) {
		int i = -1;
		if (attributes.size() <= 2) {
			return leafAttr;
		}
		String representableAttr = hierarchy
				.getPairByClassName(attributes.get(1), true)
				.getCorrespondingAttr().get(0);
		double maxLift = calculatedLift.get(representableAttr);
		for (String attr : attributes) {
			i++;
			// skip the root
			if (i <= 1)
				continue;
			attr = hierarchy.getPairByClassName(attr, true)
					.getCorrespondingAttr().get(0);
			// the lift is 0
			if (!calculatedLift.containsKey(attr))
				return representableAttr;
			if (maxLift > calculatedLift.get(attr)) {
				return representableAttr;
			}
			maxLift = calculatedLift.get(attr);
			representableAttr = attr;
		}

		// the lift is 0
		if (!calculatedLift.containsKey(leafAttr))
			return representableAttr;
		if (maxLift > calculatedLift.get(leafAttr))
			return representableAttr;

		return leafAttr;
	}

	/**
	 * returns info gain for using in wordnet filtering
	 * 
	 * @param exampleSet
	 * @return
	 */
	private Map<String, Double> calculateInfoGainWN(ExampleSet exampleSet) {
		Map<String, Double> igFinal = new HashMap<String, Double>();

		try {
			AttributeWeights weights = calculateWeights(exampleSet);

			for (Attribute attr : exampleSet.getAttributes()) {
				igFinal.put(attr.getName(), weights.getWeight(attr.getName()));
			}
		} catch (OperatorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return igFinal;
	}

	/**
	 * calculates lift for all attributes
	 * 
	 * @param exampleSet
	 * @return
	 */
	private Map<String, Double> calculateLift(ExampleSet exampleSet) {
		Map<String, Double> liftFinal = new HashMap<String, Double>();

		Map<Attribute, List<Double>> liftFreq = new HashMap<Attribute, List<Double>>();
		boolean labelIsNominal = exampleSet.getAttributes().getLabel()
				.isNominal() ? true : false;
		// iteratate all attributes
		for (Example currentExample : exampleSet) {
			int i = 0;
			for (Attribute attribute : exampleSet.getAttributes()) {
				boolean isAttPositive = false;
				try {
					if (currentExample.getNominalValue(attribute)
							.equals("true")) {

						isAttPositive = true;
					}
				} catch (Exception e) {
					if (currentExample.getValue(attribute) == 1.0) {
						isAttPositive = true;
					}
				}
				// if att is positive increase the frequecny
				if (isAttPositive) {
					List<Double> curValues = new LinkedList<Double>();
					curValues.add(0, 0.0);
					curValues.add(1, 0.0);
					if (liftFreq.containsKey(attribute)) {
						curValues = liftFreq.get(attribute);
					}
					curValues.add(0, curValues.get(0) + 1);
					if (labelIsNominal
							&& (currentExample.getNominalValue(
									exampleSet.getAttributes().getLabel())
									.equals("true") || currentExample
									.getNominalValue(
											exampleSet.getAttributes()
													.getLabel()).equals(
											"positive"))) {
						curValues.add(1, curValues.get(1) + 1);
					} else if (!labelIsNominal
							&& currentExample.getValue(exampleSet
									.getAttributes().getLabel()) == 1.0) {
						curValues.add(1, curValues.get(1) + 1);
					}
					liftFreq.put(attribute, curValues);
				}
				i++;
			}
		}

		// get the positive values in the label attribute
		double positiveLabelNm = 0;
		for (Example currentExample : exampleSet) {
			if (labelIsNominal
					&& (currentExample.getNominalValue(
							exampleSet.getAttributes().getLabel()).equals(
							"true") || currentExample.getNominalValue(
							exampleSet.getAttributes().getLabel()).equals(
							"positive"))) {
				positiveLabelNm++;
			} else if (!labelIsNominal
					&& currentExample.getValue(exampleSet.getAttributes()
							.getLabel()) == 1.0) {
				positiveLabelNm++;
			}
		}
		// calc positive label probability
		double totalSetSize = exampleSet.getExampleTable().size();
		double classProbability = positiveLabelNm / totalSetSize;

		// calculate lift for all attributes
		for (Attribute att : exampleSet.getAttributes()) {
			if (liftFreq.containsKey(att)) {
				double attrProbability = liftFreq.get(att).get(0)
						/ totalSetSize;
				double jointProbabiliy = liftFreq.get(att).get(1)
						/ totalSetSize;
				double lift = jointProbabiliy
						/ (classProbability * attrProbability);
				liftFinal.put(att.getName(), lift);
			}
		}

		return liftFinal;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeCategory(PARAMETER_FILTER_ATTRS,
				"Select the filtering method", ROW_TYPES_VALUES, 0, false));
		types.add(new ParameterTypeDouble(PARAMETER_THRESHOLD, "Set threshold",
				-1, 1, 0, false));

		ParameterType type = new ParameterTypeBoolean(
				PARAMETER_USE_AVERAGE,
				"The threshold for prunning will be the average of all nodes on each path",
				true);

		type.registerDependencyCondition(new OrParameterCondition(this, true,
				new EqualTypeCondition(this, PARAMETER_FILTER_ATTRS,
						ROW_TYPES_VALUES, true, 3),
				new EqualTypeCondition(this, PARAMETER_FILTER_ATTRS,
						ROW_TYPES_VALUES, true, 4)));
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_PRUNING_THRESHOLD,
				"Pruning Threshold", 0, 1, 0.5, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this,
				PARAMETER_USE_AVERAGE, true, false));
		types.add(type);

		return types;
	}

	private void filterAttributesSimpleInfoGain(OntologyHierarchy hieararchy,
			double threshold, ExampleSet exampleSet, boolean usePrunning)
			throws OperatorException {
		List<String> attrsToRemove = new ArrayList<String>();

		List<HierarchyPair> pairsToCheck = hieararchy.getHierarchyLeafs();
		List<HierarchyPair> tmpPairsToCheck = new ArrayList<HierarchyPair>();

		// calculate the information gain of each attribute
		AttributeWeights weights = calculateWeights(exampleSet);

		while (pairsToCheck.size() > 0) {
			for (HierarchyPair currentPair : pairsToCheck) {
				currentPair.setChecked(true);
				List<String> baseClassAttrs = currentPair
						.getCorrespondingAttr();

				List<Attribute> attrs1 = new ArrayList<Attribute>();
				for (String attStr : baseClassAttrs) {
					attrs1.add(exampleSet.getAttributes().get(attStr));
				}
				double infoGainAtts1 = 0;
				for (Attribute attr : attrs1) {
					// the attribute might be missing
					try {
						if (weights.getWeight(attr.getName()) != Double.NaN)
							infoGainAtts1 += weights.getWeight(attr.getName());
					} catch (Exception e) {

					}
				}
				for (String superClass : currentPair.getDirectSuperClasses()) {
					HierarchyPair pairSuper = hieararchy.getPairByClassName(
							superClass, true);

					List<String> superClassAttrs = pairSuper
							.getCorrespondingAttr();
					List<Attribute> attrs2 = new ArrayList<Attribute>();
					for (String attStr : superClassAttrs) {
						attrs2.add(exampleSet.getAttributes().get(attStr));
					}

					double infoGainAtts2 = 0;

					for (Attribute attr : attrs2) {
						try {
							if (weights.getWeight(attr.getName()) != Double.NaN)
								infoGainAtts2 += weights.getWeight(attr
										.getName());
						} catch (Exception e) {

						}
					}

					if ((1 - Math.abs(infoGainAtts1 - infoGainAtts2)) >= threshold) {
						for (Attribute attr : attrs1) {

							if (attr != null
									&& !attrsToRemove.contains(attr.getName()))
								attrsToRemove.add(attr.getName());

						}
						try {
							System.out.println("REMOVING: "
									+ attrs2.get(0).getName() + " -> "
									+ attrs1.get(0).getName());
						} catch (Exception e) {
							// TODO: handle exception
						}
						// remove the type from the hierarchy
						hieararchy.removeLeafPairFromHierarchy(currentPair);// removePairFromHierarchy(currentPair);//
						break;
					} else {
						System.out.println("they are not similar");
					}
				}
				// add the directParents to be checked
				addDirectParentsForNextCheck(tmpPairsToCheck, currentPair,
						hieararchy);
			}

			pairsToCheck = new ArrayList<HierarchyPair>();
			for (HierarchyPair p : tmpPairsToCheck)
				pairsToCheck.add(p);
			// pairsToCheck = hieararchy.getHierarchyLeafs();

			tmpPairsToCheck = new ArrayList();
		}
		if (usePrunning) {
			// remove attrs with smaller IG from the max node
			removeAttrsWithIGSmallerThanAverage(hieararchy, attrsToRemove,
					pairsToCheck, weights);
		}

		// remove attributes with information gain 0
		for (Attribute attr : exampleSet.getAttributes()) {
			if (weights.getWeight(attr.getName()) == Double.NaN
					|| weights.getWeight(attr.getName()) == 0) {
				if (!attrsToRemove.contains(attr.getName()))
					attrsToRemove.add(attr.getName());
			}
		}
		// if it is qualified hierarchy remove inside nodes
		if (hieararchy.getType() == OntologyHierarchy.HierarchyType.QualifiedRelation) {
			{
				for (HierarchyPair pair : hieararchy.getHierarchyPairs()) {
					pairsToCheck = hieararchy.getInsideLeafsFromNode(pair);

					while (pairsToCheck.size() > 0) {
						for (HierarchyPair currentPair : pairsToCheck) {
							currentPair.setChecked(true);
							String baseClassAttr = currentPair
									.getCorrespondingAttr().get(0);
							if (attrsToRemove.contains(baseClassAttr))
								continue;
							Attribute attr1 = exampleSet.getAttributes().get(
									baseClassAttr);
							for (String superClass : currentPair
									.getSuperClasses()) {
								HierarchyPair pairSuper = hieararchy
										.getInsidePairByClassName(pair,
												superClass);
								String superClassAttr = pairSuper
										.getCorrespondingAttr().get(0);
								Attribute attr2 = exampleSet.getAttributes()
										.get(superClassAttr);
								double infoGainAtt1 = weights.getWeight(attr1
										.getName());
								double infoGainAtt2 = weights.getWeight(attr2
										.getName());
								if ((1 - Math.abs(infoGainAtt1 - infoGainAtt2)) >= threshold) {

									attrsToRemove.add(attr1.getName());
									// remove the type from the hierarchy
									hieararchy.removeInsidePairFromPair(pair,
											currentPair);
									break;
								}
							}
						}
						pairsToCheck = hieararchy.getInsideLeafsFromNode(pair);
					}
				}
			}
		}

		removeAttributes(exampleSet, attrsToRemove, hieararchy);

		// remove standard unusful attrs
		removeUnValuableAttrs(exampleSet, hieararchy);
	}

	private void removeAttrsWithIGSmallerThanAverage(
			OntologyHierarchy hieararchy, List<String> attrsToRemove,
			List<HierarchyPair> pairsToCheck, AttributeWeights weights) {
		pairsToCheck = hieararchy.getHierarchyLeafsPost();

		for (HierarchyPair leafPair : pairsToCheck) {
			double maxIG = Double.MIN_VALUE;
			double pruningThreshold = 0;

			if (getParameterAsBoolean(PARAMETER_USE_AVERAGE)) {
				double avg = 0;
				for (String superPair : leafPair.getSuperClasses()) {
					HierarchyPair sPair = hieararchy.getPairByClassName(
							superPair, true);
					try {
						avg += weights.getWeight(sPair.getCorrespondingAttr()
								.get(0));
						if (weights.getWeight(sPair.getCorrespondingAttr().get(
								0)) > maxIG) {
							maxIG = weights.getWeight(sPair
									.getCorrespondingAttr().get(0));
						}
					} catch (Exception e) {

					}
				}
				// remove attrs with IG smaller than the maxIG - threshold
				avg = (avg + weights.getWeight(leafPair.getCorrespondingAttr()
						.get(0))) / (double) leafPair.getSuperClasses().size();
				// something went wrong or maybe the node doesn't have super
				// classes
				// or child nodes
				if (avg > 1 || avg < 0 || avg == Double.NaN)
					avg = 0;
				pruningThreshold = avg;
			}
			// we need to take the user specified threhold
			else {
				try {
					pruningThreshold = getParameterAsDouble(PARAMETER_PRUNING_THRESHOLD);
				} catch (UndefinedParameterError e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			for (String superPair : leafPair.getSuperClasses()) {
				HierarchyPair sPair = hieararchy.getPairByClassName(superPair,
						true);
				if (weights.getWeight(sPair.getCorrespondingAttr().get(0)) < pruningThreshold) {// maxIG
					// - threshold) {
					String attrName = sPair.getCorrespondingAttr().get(0);
					if (!attrsToRemove.contains(attrName))
						attrsToRemove.add(attrName);
				}
			}

			// remove the leaf if necessarry
			if (weights.getWeight(leafPair.getCorrespondingAttr().get(0)) < pruningThreshold) {// maxIG
				// - threshold) {
				String attrName = leafPair.getCorrespondingAttr().get(0);
				if (!attrsToRemove.contains(attrName))
					attrsToRemove.add(attrName);
			}
		}

	}

	private void filterAttributesUsingGreedy(OntologyHierarchy hierarchy,
			double threshold, ExampleSet exampleSet) throws OperatorException {
		List<String> attrsToRemove = new ArrayList<String>();
		// calculate the information gain of each attribute
		AttributeWeights weights = calculateWeights(exampleSet);
		List<HierarchyPair> pairsToCheck = hierarchy.getHierarchyLeafs();

		// build the path from each leaf node
		for (HierarchyPair leafPair : pairsToCheck) {
			double maxIG = Double.MIN_VALUE;
			double avg = 0;
			Map<String, Double> candidateConcepts = new HashMap<String, Double>();
			for (String superPair : leafPair.getSuperClasses()) {
				HierarchyPair sPair = hierarchy.getPairByClassName(superPair,
						true);

				candidateConcepts.put(superPair,
						weights.getWeight(sPair.getCorrespondingAttr().get(0)));
			}
			ValueComparator bvc = new ValueComparator(candidateConcepts);
			TreeMap<String, Double> sorted_map = new TreeMap<String, Double>(
					bvc);
			sorted_map.putAll(candidateConcepts);
			List<String> tmpAttsToRemove = new ArrayList<String>();
			List<String> tmpAddedNodes = new ArrayList<String>();
			for (Entry<String, Double> entry : sorted_map.entrySet()) {

				HierarchyPair sPair = hierarchy.getPairByClassName(
						entry.getKey(), true);
				String attName = sPair.getCorrespondingAttr().get(0);
				// it was marked to be removed
				if (tmpAttsToRemove.contains(entry.getKey())) {
					if (!attrsToRemove.contains(attName))
						attrsToRemove.add(attName);
					continue;
				}
				// check if a parent was already added
				boolean shouldBreak = false;
				for (String superClass : sPair.getDirectSuperClasses()) {

					if (tmpAddedNodes.contains(superClass)) {
						if (!attrsToRemove.contains(attName))
							attrsToRemove.add(attName);
						shouldBreak = true;
						break;
					}
				}
				if (shouldBreak)
					continue;
				// add the direct parents to be removed
				tmpAttsToRemove.addAll(sPair.getDirectSuperClasses());
				tmpAddedNodes.add(entry.getKey());

			}
		}
		removeAttributes(exampleSet, attrsToRemove, hierarchy);
	}

	private void addDirectParentsForNextCheck(List<HierarchyPair> nodes,
			HierarchyPair currentPair, OntologyHierarchy hieararchy) {
		for (String directParent : currentPair.getDirectSuperClasses()) {
			HierarchyPair directParentNode = hieararchy.getPairByClassName(
					directParent, true);
			if (!directParentNode.isChecked()
					&& !nodes.contains(directParentNode))
				nodes.add(directParentNode);
		}
	}

	private void filterAttributesSimpleCorrelation(
			OntologyHierarchy hieararchy, double threshold,
			ExampleSet exampleSet, boolean usePrunning)
			throws OperatorException {
		List<String> attrsToRemove = new ArrayList<String>();

		List<HierarchyPair> pairsToCheck = hieararchy.getHierarchyLeafs();

		List<HierarchyPair> tmpPairsToCheck = new ArrayList<HierarchyPair>();

		while (pairsToCheck.size() > 0) {
			for (HierarchyPair currentPair : pairsToCheck) {
				currentPair.setChecked(true);
				String baseClassAttr = currentPair.getCorrespondingAttr()
						.get(0);
				if (attrsToRemove.contains(baseClassAttr))
					continue;
				Attribute attr1 = exampleSet.getAttributes().get(baseClassAttr);
				for (String superClass : currentPair.getDirectSuperClasses()) {

					HierarchyPair pairSuper = hieararchy.getPairByClassName(
							superClass, true);
					String superClassAttr = pairSuper.getCorrespondingAttr()
							.get(0);
					Attribute attr2 = exampleSet.getAttributes().get(
							superClassAttr);
					if (isCorrelationGreaterThanTreshold(exampleSet, attr1,
							attr2, threshold)) {
						System.out.println("REMOVING: " + superClassAttr
								+ " -> " + baseClassAttr);
						// double infoGainAtt1 = weights
						// .getWeight(attr1.getName());
						// double infoGainAtt2 = weights
						// .getWeight(attr2.getName());
						// if (infoGainAtt1 > infoGainAtt2) {
						// attrsToRemove.add(attr2.getName());
						// HierarchyPair pairSuper = hieararchy
						// .getPairByClassName(superClass);
						// if (pairSuper != null)
						// hieararchy.removePairFromHierarchy(pairSuper);
						// } else {
						attrsToRemove.add(attr1.getName());
						// remove the type from the hierarchy
						hieararchy.removeLeafPairFromHierarchy(currentPair);
						break;
						// }

					}

				}
				addDirectParentsForNextCheck(tmpPairsToCheck, currentPair,
						hieararchy);
			}
			pairsToCheck = new ArrayList<HierarchyPair>();
			for (HierarchyPair p : tmpPairsToCheck)
				pairsToCheck.add(p);
			// pairsToCheck = tmpPairsToCheck;// hieararchy.getHierarchyLeafs();

			tmpPairsToCheck = new ArrayList();
		}
		// calculate the information gain of each attribute
		if (usePrunning) {
			AttributeWeights weights = calculateWeights(exampleSet);
			// remove the rest of the attributes
			removeAttrsWithIGSmallerThanAverage(hieararchy, attrsToRemove,
					pairsToCheck, weights);
		}

		removeAttributes(exampleSet, attrsToRemove, hieararchy);

		// remove standard unusful attrs
		removeUnValuableAttrs(exampleSet, hieararchy);
	}

	/**
	 * uses information gain to decide which correlated node to remove
	 * 
	 * @param hieararchy
	 * @param threshold
	 * @param exampleSet
	 * @throws OperatorException
	 */
	private void filterAttributesSimpleCorrelationWithIfoGain(
			OntologyHierarchy hieararchy, double threshold,
			ExampleSet exampleSet) throws OperatorException {
		List<String> attrsToRemove = new ArrayList<String>();

		List<HierarchyPair> pairsToCheck = hieararchy.getHierarchyLeafs();

		// calculate the information gain of each attribute
		AttributeWeights weights = calculateWeights(exampleSet);

		while (pairsToCheck.size() > 0) {
			for (HierarchyPair currentPair : pairsToCheck) {
				currentPair.setChecked(true);
				String baseClassAttr = currentPair.getCorrespondingAttr()
						.get(0);
				if (attrsToRemove.contains(baseClassAttr))
					continue;
				Attribute attr1 = exampleSet.getAttributes().get(baseClassAttr);
				for (String superClass : currentPair.getDirectSuperClasses()) {

					HierarchyPair pairSuper = hieararchy.getPairByClassName(
							superClass, true);
					String superClassAttr = pairSuper.getCorrespondingAttr()
							.get(0);
					Attribute attr2 = exampleSet.getAttributes().get(
							superClassAttr);
					if (isCorrelationGreaterThanTreshold(exampleSet, attr1,
							attr2, threshold)) {
						double infoGainAtt1 = weights
								.getWeight(attr1.getName());
						double infoGainAtt2 = weights
								.getWeight(attr2.getName());
						if (infoGainAtt1 > infoGainAtt2) {
							attrsToRemove.add(attr2.getName());

							if (pairSuper != null)
								hieararchy.removePairFromHierarchy(pairSuper);
						} else {
							attrsToRemove.add(attr1.getName());
							// remove the type from the hierarchy
							hieararchy.removePairFromHierarchy(currentPair);
							break;
						}

					}

				}
			}
			pairsToCheck = hieararchy.getHierarchyLeafs();
		}
		// remove attributes with information gain 0
		// for (Attribute attr : exampleSet.getAttributes()) {
		// if (weights.getWeight(attr.getName()) == 0)
		// attrsToRemove.add(attr.getName());
		// }

		removeAttributes(exampleSet, attrsToRemove, hieararchy);

		// remove standard unusful attrs
		removeUnValuableAttrs(exampleSet, hieararchy);
	}

	/**
	 * Removes attrs that have same values for all examples it should remove
	 * attrs with missing values, or all different values also
	 * 
	 * @param exampleSet
	 */
	private void removeUnValuableAttrs(ExampleSet exampleSet,
			OntologyHierarchy hierarchy) {
		// TODO: remove also useless attributes for qualified relations
		List<String> attrsToRemove = new ArrayList<String>();
		for (Attribute attr : exampleSet.getAttributes()) {
			boolean toRemove = true;

			Example ex1 = exampleSet.getExample(0);

			for (Example ex : exampleSet) {
				if (ex.getValue(attr) == ex1.getValue(attr))
					continue;
				toRemove = false;
				break;
			}
			if (toRemove)
				attrsToRemove.add(attr.getName());
		}
		removeAttributes(exampleSet, attrsToRemove, hierarchy);
	}

	private void removeAttributes(ExampleSet exampleSet,
			List<String> attrsToRemove, OntologyHierarchy hierarchy) {
		for (String attr : attrsToRemove) {
			Attribute attribute = exampleSet.getAttributes().get(attr);
			if (attribute != null && exampleSet.getAttributes().size() > 0
					&& exampleSet.getAttributes().contains(attribute)) {
				exampleSet.getAttributes().remove(attribute);

				// TODO: remove also inside pairs if needed
				HierarchyPair pairToRemove = hierarchy
						.getPairByAttributeName(attribute.getName());
				if (pairToRemove != null) {
					pairToRemove.getCorrespondingAttr().remove(
							attribute.getName());
					if (pairToRemove.getCorrespondingAttr().size() == 0)
						hierarchy.removePairFromHierarchy(pairToRemove);
				}

			}
		}
	}

	public boolean isCorrelationGreaterThanTreshold(ExampleSet exampleSet,
			Attribute attr1, Attribute attr2, double threshold) {
		double correlation = MathFunctions.correlation(exampleSet, attr1,
				attr2, false);
		if (correlation >= threshold) {// if
										// (correlation
										// >
										// threshold
										// ||
			// Double.isNaN(correlation)) {
			return true;
		}
		return false;
	}

	// this is copied from infoGainOperator
	protected AttributeWeights calculateWeights(ExampleSet exampleSet)
			throws OperatorException {
		Attribute label = exampleSet.getAttributes().getLabel();
		if (!label.isNominal()) {
			throw new UserError(this, 101, getName(), label.getName());
		}

		// calculate the actual information gain values and assign them to
		// weights
		Criterion criterion = new InfoGainCriterion(0);
		NumericalSplitter splitter = new NumericalSplitter(criterion);
		AttributeWeights weights = new AttributeWeights(exampleSet);
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNominal()) {
				double weight = criterion.getNominalBenefit(exampleSet,
						attribute);
				weights.setWeight(attribute.getName(), weight);
			} else {
				double splitValue = splitter
						.getBestSplit(exampleSet, attribute);
				double weight = criterion.getNumericalBenefit(exampleSet,
						attribute, splitValue);
				weights.setWeight(attribute.getName(), weight);
			}
		}
		return weights;
	}

	public static void main(String[] args) {
		OntologyHierarchy h = new OntologyHierarchy();
		OntologyHierarchy h2 = (OntologyHierarchy) SerializationUtils.clone(h);
	}

	public ExampleSet filterOutHillClimbing(OntologyHierarchy hieararchy,
			double threshold, ExampleSet set) {
		int leafPairsSize = hieararchy.getHierarchyLeafs().size();
		double optimalFitness = Double.MIN_VALUE;
		// iterate all pairs
		for (HierarchyPair pair : hieararchy.getHierarchyPairs()) {
			// copy the set
			ExampleSet newSet = (ExampleSet) set.clone();// LOD2ExampleSet.generateCleanMemoryTable(set)
			// .createExampleSet();
			if (pair.isChecked())
				continue;
			if (pair.getDirectSuperClasses().size() == 0)
				continue;
			HierarchyPair parentNode = hieararchy.getPairByClassName(pair
					.getDirectSuperClasses().get(0), true);
			int tmpLeafSize = leafPairsSize;
			for (String subclasses : parentNode.getSubClasses()) {
				HierarchyPair subNode = hieararchy.getPairByClassName(
						subclasses, true);
				if (subNode.isLeaf())
					tmpLeafSize--;
				Attribute attribute = newSet.getAttributes().get(
						subNode.getCorrespondingAttr().get(0));
				if (attribute != null && newSet.getAttributes().size() > 0
						&& newSet.getAttributes().contains(attribute)) {
					newSet.getAttributes().remove(attribute);
				}
			}

			double beta = 0.05;
			double fitness = (double) (1 - ((double) (tmpLeafSize - newSet
					.getAttributes().size()) / (double) tmpLeafSize) * beta)
					* calculatePurity(newSet);
			if (fitness >= optimalFitness) {
				leafPairsSize = tmpLeafSize;
				optimalFitness = fitness;
				for (String subclasses : parentNode.getSubClasses()) {
					HierarchyPair subNode = hieararchy.getPairByClassName(
							subclasses, true);
					subNode.setChecked(true);
					set = newSet;
				}
			}

			pair.setChecked(true);
		}
		return set;

	}

	public Collection<Integer> getNearestValues(int k, double[] values,
			List<double[]> samples) {
		BoundedPriorityQueue<Tupel<Double, Integer>> queue = new BoundedPriorityQueue<Tupel<Double, Integer>>(
				k);
		int i = 0;
		for (double[] sample : samples) {
			queue.add(new Tupel<Double, Integer>(calculateSimilarity(sample,
					values), i));
			i++;
		}

		Collection<Integer> result = new ArrayList<Integer>(k);
		for (Tupel<Double, Integer> tupel : queue) {
			result.add(tupel.getSecond());
		}
		return result;
	}

	public double calculateSimilarity(double[] value1, double[] value2) {
		double sum = 0.0;
		double sum1 = 0.0;
		double sum2 = 0.0;
		for (int i = 0; i < value1.length; i++) {
			double v1 = value1[i];
			double v2 = value2[i];
			if ((!Double.isNaN(v1)) && (!Double.isNaN(v2))) {
				sum += v2 * v1;
				sum1 += v1 * v1;
				sum2 += v2 * v2;
			}
		}
		if ((sum1 > 0) && (sum2 > 0)) {
			double result = sum / (Math.sqrt(sum1) * Math.sqrt(sum2));
			// result can be > 1 (or -1) due to rounding errors for equal
			// vectors, but must be between -1 and 1
			return Math.min(Math.max(result, -1d), 1d);
			// return result;
		} else if (sum1 == 0 && sum2 == 0) {
			return 1d;
		} else {
			return 0d;
		}
	}

	public int calculatePurity(ExampleSet exampleSet) {
		int sumOfPurity = -1;

		List<double[]> samples = new LinkedList<double[]>();
		List<Integer> samplesLabels = new LinkedList<Integer>();

		Attributes attributes = exampleSet.getAttributes();
		Attribute label = exampleSet.getAttributes().getLabel();
		int valuesSize = attributes.size();
		for (Example example : exampleSet) {
			double[] values = new double[valuesSize];
			int i = 0;
			for (Attribute attribute : attributes) {
				values[i] = example.getValue(attribute);
				i++;
			}
			int labelValue = (int) example.getValue(label);
			samples.add(values);
			samplesLabels.add(labelValue);

		}
		for (Example example : exampleSet) {
			int curLabel = (int) example.getValue(label);
			double[] values = new double[valuesSize];
			int i = 0;
			for (Attribute attribute : attributes) {
				values[i] = example.getValue(attribute);
				i++;
			}
			Collection<Integer> neighbours = getNearestValues(11, values,
					samples);
			for (int n : neighbours) {
				if (curLabel == samplesLabels.get(n)) {
					sumOfPurity++;
				}
			}

		}
		return sumOfPurity;
	}

}
