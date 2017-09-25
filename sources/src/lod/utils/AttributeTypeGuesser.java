package lod.utils;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import com.hp.hpl.jena.rdf.model.Literal;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeTypeException;
import com.rapidminer.tools.Ontology;

/**
 * Gets a literal, guesses an appropriate value for the corresponding WEKA
 * attribute
 * 
 * @author paulheim
 */

public class AttributeTypeGuesser {

	private Collection<String> numericTypes;
	private Collection<String> dateTypes;

	public AttributeTypeGuesser() {
		numericTypes = new HashSet<String>();
		numericTypes.add("http://www.w3.org/2001/XMLSchema#decimal");
		numericTypes.add("http://www.w3.org/2001/XMLSchema#integer");
		numericTypes.add("http://www.w3.org/2001/XMLSchema#long");
		numericTypes.add("http://www.w3.org/2001/XMLSchema#int");
		numericTypes.add("http://www.w3.org/2001/XMLSchema#short");
		numericTypes.add("http://www.w3.org/2001/XMLSchema#byte");
		numericTypes.add("http://www.w3.org/2001/XMLSchema#float");
		numericTypes.add("http://www.w3.org/2001/XMLSchema#double");
		numericTypes.add("http://www.w3.org/2001/XMLSchema#unsignedLong");
		numericTypes.add("http://www.w3.org/2001/XMLSchema#unsignedInt");
		numericTypes.add("http://www.w3.org/2001/XMLSchema#unsignedShort");
		numericTypes.add("http://www.w3.org/2001/XMLSchema#unsignedByte");
		numericTypes.add("http://www.w3.org/2001/XMLSchema#positiveInteger");
		numericTypes.add("http://www.w3.org/2001/XMLSchema#negativeInteger");
		numericTypes.add("http://www.w3.org/2001/XMLSchema#nonPositiveInteger");
		numericTypes.add("http://www.w3.org/2001/XMLSchema#nonNegativeInteger");
		dateTypes = new HashSet<String>();
		dateTypes.add("http://www.w3.org/2001/XMLSchema#date");
		dateTypes.add("http://www.w3.org/2001/XMLSchema#dateTime");
		dateTypes.add("http://www.w3.org/2001/XMLSchema#time");
		dateTypes.add("http://www.w3.org/2001/XMLSchema#g");
		dateTypes.add("http://www.w3.org/2001/XMLSchema#gDay");
		dateTypes.add("http://www.w3.org/2001/XMLSchema#gMonth");
		dateTypes.add("http://www.w3.org/2001/XMLSchema#gYear");
		dateTypes.add("http://www.w3.org/2001/XMLSchema#gYearMonth");
		dateTypes.add("http://www.w3.org/2001/XMLSchema#gMonthDay");
	}

	public static enum attributeType {
		numeric, string, nominal, date
	};

	/**
	 * Returns the literal type, as set using XSD if the type is missing, simple
	 * heuristic for type detection is used
	 * 
	 * @param literal
	 * @return
	 */
	public int getLiteralType(Literal literal) {
		// check for numerical
		if (numericTypes.contains(literal.getDatatypeURI()))
			return Ontology.NUMERICAL;
		// check for dates
		if (dateTypes.contains(literal.getDatatypeURI()))
			return Ontology.DATE;
		// check string
		if (literal.getDatatypeURI()!=null&&literal.getDatatypeURI().equals(
				"http://www.w3.org/2001/XMLSchema#string"))
			return Ontology.STRING;

		// if the datatypeURI is missing try converting
		try {
			Double.parseDouble(literal.getString());

			return Ontology.NUMERICAL;
		} catch (NumberFormatException e) {
		}
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			sdf.parse(literal.toString());

			return Ontology.DATE;
		} catch (ParseException e) {
		}

		return Ontology.NOMINAL;

	}

	/**
	 * converts the string value to double value based on the attribute's type
	 * 
	 * @param attr
	 * @param value
	 * @return
	 */
	public static Double getValueForAttribute(Attribute attr, String value) {
		double valueDouble = Double.NaN;
		try {

			if (value == null) {
				valueDouble = Double.NaN;
			} else if (attr.getValueType() == Ontology.DATE
					|| attr.getValueType() == Ontology.TIME
					|| attr.getValueType() == Ontology.DATE_TIME
					|| Ontology.ATTRIBUTE_VALUE_TYPE.isA(attr.getValueType(),
							Ontology.DATE_TIME)) {
				Calendar dateValue = javax.xml.bind.DatatypeConverter
						.parseDateTime(value);
				valueDouble = dateValue.getTime().getTime();

			} else if (attr.getValueType() == Ontology.INTEGER
					|| attr.getValueType() == Ontology.REAL
					|| attr.getValueType() == Ontology.NUMERICAL
					|| Ontology.ATTRIBUTE_VALUE_TYPE.isA(attr.getValueType(),
							Ontology.NUMERICAL)) {
				Number numberValue = NumberFormat.getInstance().parse(value);
				valueDouble = numberValue.doubleValue();
			} else if (attr.getValueType() == Ontology.BINOMINAL
					|| attr.getValueType() == Ontology.NOMINAL
					|| Ontology.ATTRIBUTE_VALUE_TYPE.isA(attr.getValueType(),
							Ontology.NOMINAL)) {
				try {
					valueDouble = attr.getMapping().mapString(value);
				} catch (AttributeTypeException e) {
					valueDouble = Double.NaN;
					e.printStackTrace();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			valueDouble = Double.NaN;

		}
		return valueDouble;
	}

	public static void main(String[] args) {

		Calendar dateValue = javax.xml.bind.DatatypeConverter
				.parseDateTime("2002-10-10T17:00:00Z");
		dateValue.getTime().getTime();
		System.out.println(dateValue.toString());
	}
}
