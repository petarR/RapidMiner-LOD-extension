package de.dwslab.rmdi.schemamatching.models;

import lod.rdf.model.RdfHolder;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class ColumnInfo implements java.io.Serializable {

	/**
	 * the uri from the RDF
	 */
	String uri;

	/**
	 * the attribute from the table
	 */
	String attributeName;

	/**
	 * the RDF where is comming from
	 */
	RdfHolder rdfHolder;

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public RdfHolder getRdfHolder() {
		return rdfHolder;
	}

	public void setRdfHolder(RdfHolder rdfHolder) {
		this.rdfHolder = rdfHolder;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).append(rdfHolder.getSource())
				.append(uri).toHashCode();

	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof ColumnInfo))
			return false;

		ColumnInfo rhs = (ColumnInfo) obj;
		return new EqualsBuilder()
				.append(rdfHolder.getSource(), rhs.getRdfHolder().getSource())
				.append(uri, rhs.getUri()).isEquals();
	}

}
