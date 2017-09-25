package lod.generators.smart.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class TargetClass {

	String classLabel;

	Map<String, Instance> instances;

	Map<String, Integer> classFeatures;

	public String getClassLabel() {
		return classLabel;
	}

	public void setClassLabel(String classLabel) {
		this.classLabel = classLabel;
	}

	public Map<String, Instance> getInstances() {
		return instances;
	}

	public void setInstances(Map<String, Instance> instances) {
		this.instances = instances;
	}

	public Map<String, Integer> getClassFeatures() {
		return classFeatures;
	}

	public void setClassFeatures(Map<String, Integer> classFeatures) {
		this.classFeatures = classFeatures;
	}

	public TargetClass(String classLabel) {
		super();
		this.classLabel = classLabel;
		this.instances = new HashMap<String, Instance>();
		this.classFeatures = new HashMap<String, Integer>();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).append(classLabel).toHashCode();

	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof TargetClass))
			return false;

		TargetClass rhs = (TargetClass) obj;
		return new EqualsBuilder().append(classLabel, rhs.classLabel)
				.isEquals();
	}

}
