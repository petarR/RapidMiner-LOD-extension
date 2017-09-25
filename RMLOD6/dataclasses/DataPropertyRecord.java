package lod.dataclasses;

import lod.utils.AttributeTypeGuesser.attributeType;

/**
 * Represents a wrapper class used to keep one data entity. Used by
 * {@link DataPropertyInstanceWrapper}.
 * 
 * @author Evgeny Mitichkin
 * 
 */
public class DataPropertyRecord {
	private String name;
	private String value;
	private int ontologyType;

	public DataPropertyRecord() {

	}

	public DataPropertyRecord(String name, String value, int ontologyType) {
		this.setName(name);
		this.setValue(value);
		this.setOntologyType(ontologyType);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public int getOntologyType() {
		return ontologyType;
	}

	public void setOntologyType(int ontologyType) {
		this.ontologyType = ontologyType;
	}

}
