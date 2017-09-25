package lod.filters;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lod.sparql.SPARQLEndpointQueryRunner;
import lod.utils.HierarchyPair;
import lod.utils.OntologyHierarchy;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeTypeException;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.Ontology;

/**
 * 
 * @author Petar Ristoski
 *
 */
public class HierarchyOperator extends Operator {

	protected InputPort mInputPort;

	protected OutputPort mOutputPort;
	protected OutputPort mOutputPortTypesHierarchy;

	public static final String PARAMETER_HIERARCHY_DEPTH = "Max hierarchy depth";
	public static final String PARAMETER_HIERARCHY_BRANCH = "Max hierarchy branch";
	public static final String GenerateDMOZ = "generate dmoz";

	public HierarchyOperator(OperatorDescription description) {
		super(description);
		mInputPort = getInputPorts()
				.createPort("Example Set", ExampleSet.class);

		mOutputPort = getOutputPorts().createPort("Appended Set");
		mOutputPortTypesHierarchy = getOutputPorts().createPort(
				"Hierarchy pairs");
	}

	@Override
	public void doWork() throws OperatorException {
		OntologyHierarchy hierarchy = new OntologyHierarchy();
		int ifgenerateDMOZ = this.getParameterAsInt(GenerateDMOZ);
		if (ifgenerateDMOZ == 1) {
			ExampleSet set = parseURLFile(hierarchy);
			mOutputPort.deliver(set);
			mOutputPortTypesHierarchy.deliver(hierarchy);
			return;
		}
		ExampleSet set = mInputPort.getData(ExampleSet.class);

		int depthLevel = this.getParameterAsInt(PARAMETER_HIERARCHY_DEPTH);
		int branchLevel = this.getParameterAsInt(PARAMETER_HIERARCHY_BRANCH);

		Map<String, List<String>> overallSuperClasses = new HashMap<String, List<String>>();

		Attributes attrs = set.getAttributes();
		List<String> attrsNames = new LinkedList<String>();
		for (Attribute attr : attrs) {
			attrsNames.add(attr.getName());
		}

		// remove old attributes and add new numerical
		for (String attrName : attrsNames) {

			Attribute newAtrribute = AttributeFactory.createAttribute(attrName
					+ "N", Ontology.NUMERICAL);
			set.getAttributes().addRegular(newAtrribute);
			set.getExampleTable().addAttribute(newAtrribute);
			// add the values
			Attribute oldAttr = attrs.get(attrName);
			for (Example ex : set) {
				ex.setValue(newAtrribute,
						Double.parseDouble(ex.getValueAsString(oldAttr)));
			}
			set.getAttributes().remove(oldAttr);

		}
		attrs = set.getAttributes();
		attrsNames = new LinkedList<String>();
		for (Attribute attr : attrs) {
			attrsNames.add(attr.getName());
		}

		List<Attribute> attrsBranch = new LinkedList<Attribute>();
		// add the root
		createTheRoot(set, overallSuperClasses, hierarchy);
		int tmpBranch = branchLevel;
		for (String attrName : attrsNames) {

			Attribute attri = set.getAttributes().get(attrName);
			attrsBranch.add(attri);
			tmpBranch--;
			if (tmpBranch > 0)
				continue;

			List<String> superClasses = generatePredecessors(depthLevel, set,
					attrsBranch, overallSuperClasses, hierarchy);

			// add the pairs for each attribute
			for (Attribute at : attrsBranch) {

				HierarchyPair pair = addPairInhierarchy(at, superClasses,
						hierarchy);
				overallSuperClasses.put(at.getName(), pair.getSuperClasses());

			}
			// create succesors for each attribute
			for (Attribute at : attrsBranch) {
				List<String> tmpSuperClasses = new LinkedList<String>();
				for (String str : superClasses)
					tmpSuperClasses.add(str);
				tmpSuperClasses.add(at.getName());
				generateSuccessors(depthLevel, set, at, overallSuperClasses,
						hierarchy, tmpSuperClasses);
			}

			attrsBranch = new LinkedList<Attribute>();
			tmpBranch = branchLevel;
		}

		// generate nodes for the rest
		if (attrsBranch.size() > 0) {
			List<String> superClasses = generatePredecessors(depthLevel, set,
					attrsBranch, overallSuperClasses, hierarchy);

			// add the pairs for each attribute
			for (Attribute at : attrsBranch) {
				HierarchyPair pair = addPairInhierarchy(at, superClasses,
						hierarchy);
				overallSuperClasses.put(at.getName(), pair.getSuperClasses());

			}
			if (!superClasses.contains("ROOT"))
				superClasses.add("ROOT");
			// create succesors for each attribute
			for (Attribute at : attrsBranch) {
				List<String> tmpSuperClasses = new LinkedList<String>();
				for (String str : superClasses)
					tmpSuperClasses.add(str);
				tmpSuperClasses.add(at.getName());
				generateSuccessors(depthLevel, set, at, overallSuperClasses,
						hierarchy, tmpSuperClasses);
			}
		}
		for (HierarchyPair pair : hierarchy.getHierarchyPairs()) {
			List<String> subClasses = SPARQLEndpointQueryRunner.getSubClasses(
					pair.getBaseClass(), null, overallSuperClasses);
			pair.setSubClasses(subClasses);
			if (subClasses.size() == 0)
				pair.setLeaf(true);
		}
		mOutputPort.deliver(set);
		mOutputPortTypesHierarchy.deliver(hierarchy);

	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeInt(PARAMETER_HIERARCHY_DEPTH,
				"Maximum hierarchy depth", 0, 100, 1, false));

		types.add(new ParameterTypeInt(PARAMETER_HIERARCHY_BRANCH, "Branching",
				0, 100, 1, false));
		types.add(new ParameterTypeInt(GenerateDMOZ, "generate dmoz", 0, 100,
				0, false));

		return types;
	}

	private static void generateSuccessors(int depthLevel, ExampleSet set,
			Attribute attr, Map<String, List<String>> overallSuperClasses,
			OntologyHierarchy hierarchy, List<String> superClasses) {

		Attribute attribute = AttributeFactory.createAttribute(attr.getName()
				+ "_successor_" + depthLevel, Ontology.NUMERICAL);
		set.getAttributes().addRegular(attribute);
		set.getExampleTable().addAttribute(attribute);
		// add the values
		for (Example ex : set) {
			double parentValue = Double.parseDouble(ex.getValueAsString(attr));
			double currentValue = 0;
			if (parentValue == 0) {
				currentValue = 0;
			} else {
				double rand = Math.random();
				if (rand >= 0.5) {
					currentValue = 1;
				}
			}
			ex.setValue(attribute, currentValue);
		}

		depthLevel--;

		// add the pair in the hierarchy
		HierarchyPair pair = addPairInhierarchy(attribute, superClasses,
				hierarchy);
		overallSuperClasses.put(attribute.getName(), pair.getSuperClasses());

		superClasses.add(attribute.getName());
		if (depthLevel > 0)
			generateSuccessors(depthLevel, set, attribute, overallSuperClasses,
					hierarchy, superClasses);
		else
			pair.setLeaf(true);

	}

	private static void createTheRoot(ExampleSet set,
			Map<String, List<String>> overallSuperClasses,
			OntologyHierarchy hierarchy) {

		Attribute attribute = AttributeFactory.createAttribute("ROOT",
				Ontology.NUMERICAL);
		set.getAttributes().addRegular(attribute);
		set.getExampleTable().addAttribute(attribute);
		// add the values
		for (Example ex : set) {

			ex.setValue(attribute, 1.0);
		}
		// add the pair in the hierarchy
		HierarchyPair pair = addPairInhierarchy(attribute,
				new ArrayList<String>(), hierarchy);
		overallSuperClasses.put(attribute.getName(), pair.getSuperClasses());

	}

	private double getValueToAttribute(String value, Attribute attribute) {
		double valueDouble = Double.NaN;
		try {
			valueDouble = attribute.getMapping().mapString("1.0");
		} catch (AttributeTypeException e) {
			valueDouble = Double.NaN;
			e.printStackTrace();
		}
		return valueDouble;
	}

	private static List<String> generatePredecessors(int depthLevel,
			ExampleSet set, List<Attribute> attrsBranch,
			Map<String, List<String>> overallSuperClasses,
			OntologyHierarchy hierarchy) {
		// create the new attribute
		Attribute attribute = AttributeFactory.createAttribute(
				attrsBranch.get(0).getName() + "_predcessor_" + depthLevel,
				Ontology.NUMERICAL);
		set.getAttributes().addRegular(attribute);
		set.getExampleTable().addAttribute(attribute);
		// add the values
		for (Example ex : set) {
			double childValue = 0;
			for (Attribute attr : attrsBranch) {
				if (Double.parseDouble(ex.getValueAsString(attr)) == 1.0) {
					childValue = 1.0;
					break;
				}
			}
			double currentValue = 0;
			if (childValue == 1) {
				currentValue = 1;
			} else {
				double rand = Math.random();
				if (rand >= 0.5) {
					currentValue = 1;
				}
			}
			ex.setValue(attribute, currentValue);
		}

		depthLevel--;
		List<String> superClasses = new LinkedList<String>();
		// superClasses

		if (depthLevel > 0) {
			List<Attribute> attrsNew = new LinkedList<Attribute>();
			attrsNew.add(attribute);
			superClasses = generatePredecessors(depthLevel, set, attrsNew,
					overallSuperClasses, hierarchy);
		}
		// add the root
		if (!superClasses.contains("ROOT"))
			superClasses.add("ROOT");

		// add the pair in the hierarchy
		HierarchyPair pair = addPairInhierarchy(attribute, superClasses,
				hierarchy);
		overallSuperClasses.put(attribute.getName(), pair.getSuperClasses());

		superClasses.add(attribute.getName());
		return superClasses;
	}

	private static HierarchyPair addPairInhierarchy(Attribute attribute,
			List<String> superClasses, OntologyHierarchy hierarchy) {
		// create the super classes string
		String supperClassesAppended = "";
		List<String> newSuperClasses = new ArrayList<String>();
		// create a string for RM output
		for (String superClass : superClasses) {
			newSuperClasses.add(superClass);
			supperClassesAppended += superClass + ", ";
		}
		if (supperClassesAppended.length() > 2) {
			supperClassesAppended = supperClassesAppended.substring(0,
					supperClassesAppended.length() - 2);
		}
		HierarchyPair pair = new HierarchyPair(attribute.getName(),
				supperClassesAppended);

		// set all superclasses
		pair.setSuperClasses(newSuperClasses);
		// set all direct classes
		List<String> directSuperClasses = new ArrayList<String>();
		if (superClasses.size() > 0)
			directSuperClasses.add(superClasses.get(superClasses.size() - 1));
		pair.setDirectSuperClasses(directSuperClasses);

		List<String> correspondAttr = new ArrayList<String>();
		correspondAttr.add(attribute.getName());
		pair.setCorrespondingAttr(correspondAttr);
		hierarchy.addNewPair(pair);

		return pair;
	}

	/*
	 * 
	 * 
	 * HERE STARTS THE DMOS DATASET
	 */

	public static String getDomain(String input) {

		// Assuming that all urls start with "http://"
		int finish = input.indexOf("/", 7);
		if (finish == -1) {
			finish = input.length();
		}
		return input.substring(7, finish);
	}

	public static ExampleSet parseURLFile(OntologyHierarchy hierarchy) {
		Map<String, List<String>> allCategories = getCategoriesCash();
		ExampleSet set = null;

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(
					"C:\\Users\\petar\\workspace\\JavaRMExample\\train.tsv"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			// skip the header
			String line = br.readLine();
			// holds all classes
			Map<String, List<String>> overallSuperClasses = new HashMap<String, List<String>>();
			// holds all new attributes
			Map<String, List<Integer>> newAttributes = new LinkedHashMap<String, List<Integer>>();
			// holds all new examples
			Map<Site, List<String>> newExamples = new LinkedHashMap<Site, List<String>>();

			int addedExamples = 5000;
			while ((line = br.readLine()) != null) {

				String[] vals = line.split("\t");
				String id = vals[1].replaceAll("\"", "");
				String url = vals[0].replaceAll("\"", "");
				String label = vals[vals.length - 1].replaceAll("\"", "");
				Site curSite = new Site(Integer.parseInt(id), url,
						Integer.parseInt(label));

				// get the domain from the url
				String domain = getDomain(url);

				// get the categories from the rdf file
				if (!allCategories.containsKey(domain))
					continue;

				List<String> categories = allCategories.get(domain);// getCategoriesFromDMOZ(domain);

				createPairsFromCategories(categories, hierarchy,
						overallSuperClasses, newAttributes, newExamples,
						curSite);
				addedExamples--;
				if (addedExamples == 0)
					break;
			}
			for (HierarchyPair pair : hierarchy.getHierarchyPairs()) {
				List<String> subClasses = SPARQLEndpointQueryRunner
						.getSubClasses(pair.getBaseClass(), null,
								overallSuperClasses);
				pair.setSubClasses(subClasses);
				if (subClasses.size() == 0)
					pair.setLeaf(true);
			}
			set = generateNewExampleSet(newAttributes, newExamples);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return set;
	}

	private static void createPairsFromCategories(List<String> categories,
			OntologyHierarchy hierarchy,
			Map<String, List<String>> overallSuperClasses,
			Map<String, List<Integer>> newAttributes,
			Map<Site, List<String>> newExamples, Site curSite) {
		// keeps all attributes for the site
		List<String> cureAttributes = new LinkedList<String>();
		int maxCategories = 15;
		for (String category : categories) {
			String[] cats = category.split("/");
			for (int i = cats.length - 1; i >= 0; i--) {
				String cat = cats[i];
				if (!cureAttributes.contains(cat))
					cureAttributes.add(cat);
				// the pair is already in the hierarchy
				if (hierarchy.getPairByClassName(cat, false) != null)
					continue;
				List<String> superClasses = new LinkedList<String>();
				String superClassStr = "";
				for (int j = 0; j < i; j++) {
					superClasses.add(cats[j]);
					superClassStr += "," + cats[j];
				}
				overallSuperClasses.put(cat, superClasses);
				HierarchyPair pair = new HierarchyPair(cat, superClassStr);
				pair.setSuperClasses(superClasses);
				List<String> directSuperClasses = new ArrayList<String>();
				if (superClasses.size() > 0)
					directSuperClasses
							.add(superClasses.get(superClasses.size() - 1));
				pair.setDirectSuperClasses(directSuperClasses);

				List<String> correspondAttr = new ArrayList<String>();
				correspondAttr.add(cat);
				pair.setCorrespondingAttr(correspondAttr);
				hierarchy.addNewPair(pair);
			}
			maxCategories--;
			if (maxCategories == 0)
				break;
		}

		newExamples.put(curSite, cureAttributes);
		for (String att : cureAttributes) {
			List<Integer> curSites = new LinkedList<Integer>();
			if (newAttributes.containsKey(att)) {
				curSites = newAttributes.get(att);
			}
			curSites.add(curSite.getId());
			newAttributes.put(att, curSites);
		}

	}

	private static class Site implements java.io.Serializable {
		int id;
		String url;
		int label;

		public Site() {
			// TODO Auto-generated constructor stub
		}

		public void setId(int id) {
			this.id = id;
		}

		public void setLabel(int label) {
			this.label = label;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public int getId() {
			return id;
		}

		public int getLabel() {
			return label;
		}

		public String getUrl() {
			return url;
		}

		public Site(int id, String url, int label) {
			super();
			this.id = id;
			this.url = url;
			this.label = label;
		}

		@Override
		public int hashCode() {
			return new HashCodeBuilder(17, 31).append(id).toHashCode();

		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (obj == this)
				return true;
			if (!(obj instanceof Site))
				return false;

			Site rhs = (Site) obj;
			return new EqualsBuilder().append(id, rhs.id).isEquals();
		}
	}

	private static List<String> getCategoriesFromDMOZ(String domain) {
		List<String> categories = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(
					new FileReader(
							"C:\\Users\\petar\\Downloads\\content.rdf.u8\\content.rdf.u8"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			// skip the header
			String line = br.readLine();

			while ((line = br.readLine()) != null) {
				// we found our line
				if (line.contains("<ExternalPage about=\"h")
						&& line.contains(domain)) {
					while ((line = br.readLine()) != null) {
						if (line.contains("<topic>")
								&& line.contains("</topic>")) {
							String cat = line.replace("<topic>", "").replace(
									"</topic>", "");
							if (!categories.contains(cat))
								categories.add(cat);
							break;
						}
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return categories;
	}

	public static ExampleSet generateNewExampleSet(
			Map<String, List<Integer>> newAttributes,
			Map<Site, List<String>> newExamples) {
		Attribute[] newAttrs = new Attribute[newAttributes.size()];
		int i = 0;
		for (Entry entryA : newAttributes.entrySet()) {
			Attribute attributeN = AttributeFactory.createAttribute(
					(String) entryA.getKey(), Ontology.NUMERICAL);
			newAttrs[i] = attributeN;
			i++;
		}
		MemoryExampleTable table = new MemoryExampleTable(newAttrs);
		List<Site> sites = new LinkedList<HierarchyOperator.Site>();
		for (Entry entryS : newExamples.entrySet()) {
			sites.add((Site) entryS.getKey());
			double values[] = new double[newAttributes.size()];
			i = 0;
			for (Entry entryA : newAttributes.entrySet()) {
				values[i] = 0.0;
				List<String> curAttrs = (List<String>) entryS.getValue();
				if (curAttrs.contains(entryA.getKey())) {
					values[i] = 1.0;
				}
				i++;
			}
			DoubleArrayDataRow row = new DoubleArrayDataRow(values);
			table.addDataRow(row);
		}
		ExampleSet set = table.createExampleSet();
		// add the label
		Attribute attribute = AttributeFactory.createAttribute("label",
				Ontology.NOMINAL);
		set.getAttributes().addRegular(attribute);
		set.getExampleTable().addAttribute(attribute);

		// add the id
		Attribute attribute1 = AttributeFactory.createAttribute("ID",
				Ontology.NUMERICAL);
		set.getAttributes().addRegular(attribute1);
		set.getExampleTable().addAttribute(attribute1);

		// add the URL
		Attribute attribute2 = AttributeFactory.createAttribute("URL",
				Ontology.NOMINAL);
		set.getAttributes().addRegular(attribute2);
		set.getExampleTable().addAttribute(attribute2);

		i = 0;
		for (Example ex : set) {
			// double valueDouble = sites.get(i).getLabel(); //
			// attribute.getMapping().mapString(
			// sites.get(i).getLabel());
			ex.setValue(
					attribute,
					attribute.getMapping().mapString(
							Integer.toString(sites.get(i).getLabel())));

			ex.setValue(attribute1, sites.get(i).getId());

			ex.setValue(attribute2,
					attribute2.getMapping().mapString(sites.get(i).getUrl()));

			i++;
		}
		set.getAttributes().setLabel(attribute);
		return set;
	}

	public static Map<String, List<String>> getCategoriesCash() {
		Map<String, List<String>> allCategories = new HashMap<String, List<String>>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(
					new FileReader(
							"C:\\Users\\petar\\Downloads\\content.rdf.u8\\content.rdf.u8"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			// skip the header
			String line = br.readLine();
			int i = 0;
			while ((line = br.readLine()) != null) {
				i++;
				if (i % 1000000 == 0)
					System.out.println(i);
				// we found our line
				if (line.contains("<ExternalPage about=\"h")) {
					String page = line.substring(line.indexOf("http"));
					page = page.replace("\">", "");
					page = getDomain(page);
					List<String> categories = new LinkedList<String>();
					if (allCategories.containsKey(page))
						categories = allCategories.get(page);
					while ((line = br.readLine()) != null) {
						if (line.contains("<topic>")
								&& line.contains("</topic>")) {
							String cat = line.replace("<topic>", "").replace(
									"</topic>", "");
							if (!categories.contains(cat))
								categories.add(cat);
							break;
						}
					}

					allCategories.put(page, categories);

				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return allCategories;
	}

	public static void main(String[] args) {
		OntologyHierarchy h = new OntologyHierarchy();
		parseURLFile(h);
		System.out.println(h.getHierarchyPairs().size());
		// getCategoriesCash();
	}
}
