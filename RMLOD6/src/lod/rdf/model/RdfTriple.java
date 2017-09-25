package lod.rdf.model;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class RdfTriple implements Serializable {

	private String subject;

	private String predicate;

	private String object;

	/**
	 * holds the real rdf uri, so we can later identify the rmAttribute
	 */
	private String rdfAttribute;

	private String rmAttribute;

	private String rmValue;

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}

	public String getObject() {
		return object;
	}

	public void setObject(String object) {
		this.object = object;
	}

	public String getRdfAttribute() {
		return rdfAttribute;
	}

	public void setRdfAttribute(String rdfAttribute) {
		this.rdfAttribute = rdfAttribute;
	}

	public String getRmAttribute() {
		return rmAttribute;
	}

	public void setRmAttribute(String rmAttribute) {
		this.rmAttribute = rmAttribute;
	}

	public String getRmValue() {
		return rmValue;
	}

	public void setRmValue(String rmValue) {
		this.rmValue = rmValue;
	}

	public String toString() {
		return "<" + subject + ">\t<" + predicate + ">\t" + object + " .\n";
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).append(subject).append(predicate)
				.append(object).toHashCode();

	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof RdfTriple))
			return false;

		RdfTriple rhs = (RdfTriple) obj;
		return new EqualsBuilder().append(subject, rhs.getSubject())
				.append(predicate, rhs.getPredicate())
				.append(object, rhs.getObject()).isEquals();
	}

}
