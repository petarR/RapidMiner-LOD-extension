package lod.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rapidminer.operator.ResultObjectAdapter;

public class OntologyHierarchy extends ResultObjectAdapter implements
		Serializable {

	public enum HierarchyType {
		QualifiedRelation, Simple
	}

	private static final long serialVersionUID = 7719788736766297420L;

	private List<HierarchyPair> hierarchyPairs;

	public void setHierarchyPairs(List<HierarchyPair> hierarchyPairs) {
		this.hierarchyPairs = hierarchyPairs;
	}

	public OntologyHierarchy() {

		hierarchyPairs = new ArrayList<HierarchyPair>();
	}

	public void addNewPair(HierarchyPair pair) {
		hierarchyPairs.add(pair);
	}

	public List<HierarchyPair> getHierarchyPairs() {
		return hierarchyPairs;
	}

	Map<String, HierarchyPair> baseClassPairMap = null;

	Map<String, HierarchyPair> attrPairMap = null;

	public void generateMaps() {
		baseClassPairMap = new HashMap<String, HierarchyPair>();
		attrPairMap = new HashMap<String, HierarchyPair>();
		for (HierarchyPair pair : this.hierarchyPairs) {
			baseClassPairMap.put(pair.getBaseClass(), pair);
			attrPairMap.put(pair.getCorrespondingAttr().get(0), pair);
		}
	}

	public List<HierarchyPair> getHierarchyLeafs() {
		List<HierarchyPair> leafs = new ArrayList<HierarchyPair>();
		for (HierarchyPair pair : this.hierarchyPairs) {
			if ((pair.isLeaf() || pair.getSubClasses().size() == 0)
					&& !pair.isChecked())
				leafs.add(pair);
		}
		return leafs;
	}

	public List<HierarchyPair> getHierarchyLeafsPost() {
		List<HierarchyPair> leafs = new ArrayList<HierarchyPair>();
		for (HierarchyPair pair : this.hierarchyPairs) {
			if (pair.isLeaf() || pair.getSubClasses().size() == 0)
				leafs.add(pair);
		}
		return leafs;
	}

	public List<HierarchyPair> getInsideLeafsFromNode(HierarchyPair pair) {
		List<HierarchyPair> leafs = new ArrayList<HierarchyPair>();
		for (HierarchyPair pairIn : pair.getInsideHierarchyPairs()) {
			if (pairIn.isLeaf() && !pairIn.isChecked())
				leafs.add(pairIn);
		}
		return leafs;
	}

	private HierarchyType type = HierarchyType.Simple;

	public void setType(HierarchyType type) {
		this.type = type;
	}

	public HierarchyType getType() {
		return type;
	}

	public void removeLeafPairFromHierarchy(HierarchyPair basePair) {
		if (baseClassPairMap == null) {
			generateMaps();
		}

		// remove the node from its superclasses
		for (String superClazz : basePair.getSuperClasses()) {
			HierarchyPair superPair = baseClassPairMap.get(superClazz);
			superPair.getSubClasses().remove(basePair.getBaseClass());

			// set the leafs
			if (superPair.getSubClasses().size() == 0)
				superPair.setLeaf(true);
		}

		// remove the node from its subnodes' superclasses
		for (String subclass : basePair.getSubClasses()) {
			HierarchyPair subPair = baseClassPairMap.get(subclass);
			subPair.getSuperClasses().remove(basePair.getBaseClass());
			subPair.setSuperClass(subPair.getSuperClass().replace(
					basePair.getBaseClass(), ""));
		}
		hierarchyPairs.remove(basePair);

	}

	public void removePairFromHierarchy(HierarchyPair basePair) {
		List<HierarchyPair> pairsToremove = new ArrayList<HierarchyPair>();
		List<HierarchyPair> pairsToBeLeafes = new ArrayList<HierarchyPair>();
		for (HierarchyPair pair : getHierarchyPairs()) {
			if (pair.getBaseClass().equals(basePair.getBaseClass()))
				continue;
			// remove the type from any subclasses
			if (pair.getSubClasses().contains(pair.getBaseClass())) {
				pair.getSubClasses().remove(pair.getBaseClass());
			}
			// remove the subclass pair itself
			for (String subClass : basePair.getSubClasses()) {
				if (pair.getSubClasses().contains(subClass)) {
					pair.getSubClasses().remove(subClass);
				}
				if (pair.getBaseClass().equals(subClass)) {
					pairsToremove.add(pair);
				}
			}
			if (pair.getSubClasses().size() == 0) {
				pair.setLeaf(true);
				continue;
			}
			// determin if the pair became a leaf
			boolean isLeaf = true;
			for (String subClass : pair.getSubClasses()) {
				for (HierarchyPair pairIter2 : hierarchyPairs) {
					if (pairIter2.getBaseClass().equals(subClass)) {
						if (!pairIter2.isChecked()) {
							isLeaf = false;
							break;
						}
					}
				}
				if (!isLeaf)
					break;
			}
			if (isLeaf)
				pairsToBeLeafes.add(pair);
		}
		// add the base type to be removed
		pairsToremove.add(basePair);

		// remove attributes
		for (HierarchyPair pair : pairsToremove) {
			if (hierarchyPairs.contains(pair))
				hierarchyPairs.remove(pair);
		}

		// set leafes
		for (HierarchyPair pair : pairsToBeLeafes) {
			if (hierarchyPairs.contains(pair))
				pair.setLeaf(true);
		}
	}

	public void removeInsidePairFromPair(HierarchyPair basePair,
			HierarchyPair insidePair) {
		List<HierarchyPair> pairsToremove = new ArrayList<HierarchyPair>();
		List<HierarchyPair> pairsToBeLeafes = new ArrayList<HierarchyPair>();
		for (HierarchyPair pair : basePair.getInsideHierarchyPairs()) {
			if (pair.getBaseClass().equals(insidePair.getBaseClass()))
				continue;
			// remove the type from any subclasses
			if (pair.getSubClasses().contains(pair.getBaseClass())) {
				pair.getSubClasses().remove(pair.getBaseClass());
			}
			// remove the subclass pair itself
			for (String subClass : insidePair.getSubClasses()) {
				if (pair.getSubClasses().contains(subClass)) {
					pair.getSubClasses().remove(subClass);
				}
				if (pair.getBaseClass().equals(subClass)) {
					pairsToremove.add(pair);
				}
			}
			if (pair.getSubClasses().size() == 0) {
				pair.setLeaf(true);
				continue;
			}
			// determin if the pair became a leaf
			boolean isLeaf = true;
			for (String subClass : pair.getSubClasses()) {
				for (HierarchyPair pairIter2 : basePair
						.getInsideHierarchyPairs()) {
					if (pairIter2.getBaseClass().equals(subClass)) {
						if (!pairIter2.isChecked()) {
							isLeaf = false;
							break;
						}
					}
				}
				if (!isLeaf)
					break;
			}
			if (isLeaf)
				pairsToBeLeafes.add(pair);
		}
		// add the base type to be removed
		pairsToremove.add(insidePair);

		// remove attributes
		for (HierarchyPair pair : pairsToremove) {
			if (basePair.getInsideHierarchyPairs().contains(pair))
				basePair.getInsideHierarchyPairs().remove(pair);
		}

		// set leafes
		for (HierarchyPair pair : pairsToBeLeafes) {
			if (basePair.getInsideHierarchyPairs().contains(pair))
				pair.setLeaf(true);
		}
	}

	public HierarchyPair getPairByClassName(String className, boolean getFromMap) {
		if (getFromMap) {
			if (baseClassPairMap == null)
				generateMaps();
			if (baseClassPairMap.containsKey(className))
				return baseClassPairMap.get(className);
		}
		for (HierarchyPair pair : getHierarchyPairs()) {
			if (pair.getBaseClass().equals(className))
				return pair;
		}
		return null;
	}

	public HierarchyPair getPairByAttributeName(String attname) {
		if (attrPairMap == null)
			generateMaps();
		if (attrPairMap.containsKey(attname))
			return baseClassPairMap.get(attname);
		// for (HierarchyPair pair : getHierarchyPairs()) {
		// if (pair.getCorrespondingAttr().contains(attname))
		// return pair;
		// }
		return null;
	}

	public HierarchyPair getInsidePairByClassName(HierarchyPair basePair,
			String className) {

		for (HierarchyPair pair : basePair.getInsideHierarchyPairs()) {
			if (pair.getBaseClass().equals(className))
				return pair;
		}
		return null;
	}

	// public DataTable createDataTable() {
	// DataTable dataTable = new SimpleDataTable("Hierarchy Pairs",
	// new String[] { "Class", "Super Class" });
	// int i = 0;
	// for (Map.Entry<String, String> entry : hierarchyPairs.entrySet()) {
	// String attName = entry.getKey();
	//
	// double firstClass = dataTable.mapString(0, entry.getKey());
	// double secondClass = dataTable.mapString(1, entry.getValue());
	//
	// double[] data = new double[] { firstClass, secondClass };
	// dataTable.add(new SimpleDataTableRow(data, Integer.toString(i)));
	// i++;
	// }
	//
	// return dataTable;
	// }

	public static OntologyHierarchy clone(OntologyHierarchy objectToClone)
			throws Exception

	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(objectToClone);
		oos.flush();
		oos.close();
		bos.close();
		byte[] byteData = bos.toByteArray();
		ByteArrayInputStream bais = new ByteArrayInputStream(byteData);
		OntologyHierarchy object = (OntologyHierarchy) new ObjectInputStream(
				bais).readObject();
		return object;
	}
}
