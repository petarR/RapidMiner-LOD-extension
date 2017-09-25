package lod.generators.smart.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.rapidminer.example.Example;

public class Instance {

	String uri;
	Example ex;

	Map<String, Integer> instanceFeatures;

	/**
	 * holds the features for the last node
	 */
	Map<String, List<String>> endingNodeFeatures;

	public Example getEx() {
		return ex;
	}

	public void setEx(Example ex) {
		this.ex = ex;
	}

	public Map<String, List<String>> getEndingNodeFeatures() {
		return endingNodeFeatures;
	}

	public void setEndingNodeFeatures(
			Map<String, List<String>> endingNodeFeatures) {
		this.endingNodeFeatures = endingNodeFeatures;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public Map<String, Integer> getInstanceFeatures() {
		return instanceFeatures;
	}

	public void setInstanceFeatures(Map<String, Integer> instanceFeatures) {
		this.instanceFeatures = instanceFeatures;
	}

	public Instance(String uri, Example ex) {
		super();
		this.uri = uri;
		this.instanceFeatures = new HashMap<String, Integer>();
		endingNodeFeatures = new HashMap<String, List<String>>();
	}

	public int addFeature(String feature) {
		int n = 1;
		if (instanceFeatures.containsKey(feature)) {
			n += instanceFeatures.get(feature);
		}

		instanceFeatures.put(feature, n);
		return n;
	}

	public void addLatNodeFeature(String node, String feature) {
		List<String> features = new ArrayList<String>();
		if (endingNodeFeatures.containsKey(node)) {
			features = endingNodeFeatures.get(node);
		}
		features.add(feature);

		endingNodeFeatures.put(node, features);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).append(uri).toHashCode();

	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof TargetClass))
			return false;

		Instance rhs = (Instance) obj;
		return new EqualsBuilder().append(uri, rhs.uri).isEquals();
	}

}
