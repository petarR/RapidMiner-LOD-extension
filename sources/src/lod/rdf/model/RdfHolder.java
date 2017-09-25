package lod.rdf.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ResultObjectAdapter;

/**
 * @author petar
 * 
 */
/**
 * @author petar
 * 
 */
public class RdfHolder extends ResultObjectAdapter {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8743964579879082257L;

	public enum GeneratorType {
		CLASSES, RELATIONS, DATA
	}

	/**
	 * used for identifying how the data was generated
	 */
	private List<GeneratorType> generatorTypes;
	/**
	 * the source of this data
	 */
	private String source;

	/**
	 * raw representation of the data
	 */
	private String rawData;

	/**
	 * holds all triples for faster presentation and search
	 */
	private List<RdfTriple> triples;

	/**
	 * keeps ordered list of all entities, which might be used for aligning with
	 * entities from other genereators in paris
	 */
	private List<String> orderedEntities;

	/**
	 * keeps several holders from the same operator or source
	 */
	private List<RdfHolder> subRdfHolders;

	public void setSubRdfHolders(List<RdfHolder> subRdfHolders) {
		this.subRdfHolders = subRdfHolders;
	}

	public List<RdfHolder> getSubRdfHolders() {
		return subRdfHolders;
	}

	public List<String> getOrderedEntities() {
		return orderedEntities;
	}

	public void setOrderedEntities(List<String> orderedEntities) {
		this.orderedEntities = orderedEntities;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getRawData() {
		return rawData;
	}

	public void setRawData(String rawData) {
		this.rawData = rawData;
	}

	public List<RdfTriple> getTriples() {
		return triples;
	}

	public void setTriples(List<RdfTriple> triples) {
		this.triples = triples;
	}

	public List<GeneratorType> getGeneratorTypes() {
		return generatorTypes;
	}

	public void setGeneratorTypes(List<GeneratorType> generatorTypes) {
		this.generatorTypes = generatorTypes;
	}

	public RdfHolder() {
		triples = new LinkedList<RdfTriple>();
		generatorTypes = new ArrayList<RdfHolder.GeneratorType>();
		rawData = "";
		subRdfHolders = new LinkedList<RdfHolder>();
	}

	/**
	 * searches for the corresponding RM attribute for the given uri
	 * 
	 * @param uri
	 * @return
	 */
	public String getRMAttrByURI(String uri) {
		for (RdfTriple triple : triples) {
			if (triple.getRdfAttribute().equals(uri))
				return triple.getRmAttribute();
		}
		return null;
	}

	/**
	 * adds the entities in the ordered list
	 * 
	 * @param exampleSet
	 * @param attrName
	 */
	public void setOrderedEntities(ExampleSet exampleSet, String attrName) {
		orderedEntities = new LinkedList<String>();
		for (Example ex : exampleSet) {
			orderedEntities.add(ex.getValueAsString(exampleSet.getAttributes()
					.get(attrName)));
		}

	}

	public static RdfHolder mergeHolders(List<RdfHolder> allHolders) {
		if (allHolders.size() == 1)
			return allHolders.get(0);

		RdfHolder newHolder = new RdfHolder();
		newHolder.setSource(allHolders.get(0).getSource());
		newHolder.setOrderedEntities(allHolders.get(0).getOrderedEntities());

		List<GeneratorType> types = new ArrayList<RdfHolder.GeneratorType>();
		List<RdfTriple> triples = new ArrayList<RdfTriple>();

		String rawData = new String();
		for (RdfHolder holder : allHolders) {
			types.addAll(holder.getGeneratorTypes());
			for (RdfTriple triple : holder.getTriples())
				if (!triples.contains(triple)) {
					triples.add(triple);
				}

		}
		newHolder.setTriples(triples);

		for (RdfTriple tr : newHolder.getTriples()) {
			rawData += tr.toString();
		}
		newHolder.setRawData(rawData);
		return newHolder;

	}

	/**
	 * updates the raw data based on the current triples
	 */
	public void updateRawDataFromTriples() {
		String newRawData = "";
		for (RdfTriple t : triples) {
			newRawData += t.toString();
		}
		this.rawData = newRawData;
	}

	/**
	 * removes all triples that contains the given uri
	 * 
	 * @param uri
	 */
	public void removeTriplesByUri(String uri) {
		List<RdfTriple> trToRemove = new ArrayList<RdfTriple>();
		for (RdfTriple triple : triples) {
			if (triple.getRdfAttribute().equals(uri)
					&& !trToRemove.contains(triple)) {
				trToRemove.add(triple);
			}

		}
		for (RdfTriple t : trToRemove)
			triples.remove(t);
	}

	public void populateHolderFromSubHolders() {
		rawData = "";
		triples = new ArrayList<RdfTriple>();
		for (RdfHolder holder : subRdfHolders) {
			rawData += holder.getRawData() + "\n";
			triples.addAll(holder.getTriples());
		}
	}
}
