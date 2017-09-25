package lod.dataclasses;

import java.util.ArrayList;

import lod.linking.WebValidator;

/**
 * Represents a wrapper used in {@link WebValidator}
 * @author eomit_000
 *
 */
public class ValueClassesPair {
	
	private String valueName;
	private Integer valueId;
	private ArrayList<String> classes;
	
	public ValueClassesPair()
	{
		
	}
	
	public ValueClassesPair(String vname, Integer vId, ArrayList<String> classes)
	{
		this.setValueId(vId);
		this.setValueName(vname);
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

	public ArrayList<String> getClasses() {
		return classes;
	}

	public void setClasses(ArrayList<String> classes) {
		this.classes = classes;
	}
}
