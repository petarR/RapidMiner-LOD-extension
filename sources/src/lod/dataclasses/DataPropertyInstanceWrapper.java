package lod.dataclasses;

import java.util.ArrayList;

import lod.generators.DataPropertyFeatureGeneratorOperator;

/**
 * Represents a wrapper class used in {@link DataPropertyFeatureGeneratorOperator}
 * @author Evgeny Mitichkin
 *
 */
public class DataPropertyInstanceWrapper {
	private String valueName;
	private Integer valueId;
	private ArrayList<DataPropertyRecord> classes;
	
	public DataPropertyInstanceWrapper()
	{
		
	}
	
	public DataPropertyInstanceWrapper(String valueName, Integer valueId, ArrayList<DataPropertyRecord> classes)
	{
		this.setValueId(valueId);
		this.setValueName(valueName);
		this.setClasses(classes);
	}

	public String getValueName() {
		return valueName;
	}

	public void setValueName(String valueName) {
		this.valueName = valueName;
	}

	public Integer getValueId() {
		return valueId;
	}

	public void setValueId(Integer valueId) {
		this.valueId = valueId;
	}

	public ArrayList<DataPropertyRecord> getClasses() {
		return classes;
	}

	public void setClasses(ArrayList<DataPropertyRecord> classes) {
		this.classes = classes;
	}
}
