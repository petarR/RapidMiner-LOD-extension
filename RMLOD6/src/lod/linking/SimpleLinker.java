package lod.linking;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import lod.dataclasses.PatternValueHolder;
import lod.generators.BaseGenerator;
import lod.utils.LOD2ExampleSet;

import com.hp.hpl.jena.reasoner.rulesys.builtins.Regex;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;

/**
 * Pattern-based linker
 * 
 * @author Evgeny Mitichkin
 * @author Petar Ristoski
 * 
 */
public class SimpleLinker extends Operator {

	public static final String PARAMETER_PATTERN = "Merging pattern";

	public static final String PARAMETER_NEW_ATTRIBUTE_NAME = "New attribute";

	public static final String PARAMETER_URL_ENCODING = "Perform URL encoding";

	public static final String PARAMETER_DBPEDIA_FORMAT = "Use DBpedia link format";

	private static final String NEW_ATTRIBUTES = "New Attributes";
	private static final String BYPASSING_ATTRIBUTES = "Bypassing Attributes";
	private static final String PATTERN_LIMITER = "*";

	private InputPort mInputPort;
	private OutputPort mOutputPort;
	private OutputPort mOutputPortAttrs;

	public SimpleLinker(OperatorDescription description) {
		super(description);

		mInputPort = getInputPorts()
				.createPort("Example Set", ExampleSet.class);
		mInputPort.addPrecondition(new SimplePrecondition(mInputPort,
				new MetaData(ExampleSet.class)));

		mOutputPort = getOutputPorts().createPort("Appended Set");
		mOutputPortAttrs = getOutputPorts().createPort("Attributes Appended");

		getTransformer().addPassThroughRule(mInputPort, mOutputPort);
		getTransformer().addGenerationRule(mOutputPortAttrs, ExampleSet.class);
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = BaseGenerator.cloneExampleSet(mInputPort
				.getData(ExampleSet.class));

		Attributes attrs = exampleSet.getAttributes();
		PatternValueHolder mPatternHolder = getAttributesFromPattern(getParameterAsString(PARAMETER_PATTERN));
		String linkPrefix = getPrefixLinkFromPattern(getParameterAsString(PARAMETER_PATTERN));
		// String linkPrefixAnnotation =
		// getParameterAsString(PARAMETER_LINK_ANNOTATION);

		boolean doURLEncoding = getParameterAsBoolean(PARAMETER_URL_ENCODING);
		boolean useDBpediaFormat = getParameterAsBoolean(PARAMETER_DBPEDIA_FORMAT);

		if (mPatternHolder != null) {

			if (mPatternHolder.getPatternAttributeArraySize() == 0)
				throw new OperatorException(
						"Problem in SimpleLinker: No attributes found in the pattern:\n"
								+ getParameterAsString(PARAMETER_PATTERN));

			boolean patternMatchesExampleSet = true;
			String exceptionalAttribute = "";

			for (int i = 0; i < mPatternHolder.getDataPart().size(); i++) {
				if (mPatternHolder.getDataIndex().get(i) == PatternValueHolder.IS_ATTRIBUTE) {
					boolean attributeFound = false;
					for (Attribute attr : exampleSet.getAttributes()) {
						if (attr.getName().equals(
								mPatternHolder.getDataPart().get(i))) {
							attributeFound = true;
						}
					}
					if (!attributeFound) {
						patternMatchesExampleSet = false;
						exceptionalAttribute = mPatternHolder.getDataPart()
								.get(i);
						break;
					}
				}
			}

			if (!patternMatchesExampleSet)
				throw new OperatorException(
						"Problem in SimpleLinker: No attribute named '"
								+ exceptionalAttribute
								+ "' was found in the pattern:\n"
								+ getParameterAsString(PARAMETER_PATTERN));

			// Creating the attribute
			String attrToMergeName = "";
			attrToMergeName = getParameterAsString(PARAMETER_NEW_ATTRIBUTE_NAME);
			if (attrToMergeName.equals("")) {
				for (int i = 0; i < mPatternHolder.getDataPart().size(); i++) {
					if (mPatternHolder.getDataIndex().get(i) == PatternValueHolder.IS_ATTRIBUTE) {
						attrToMergeName += (mPatternHolder.getDataPart().get(i) + "_");
					}
				}
				attrToMergeName = attrToMergeName.substring(0,
						attrToMergeName.length() - 1);
			}

			ArrayList<String> newAttributeNames = new ArrayList<String>();// Endpoits
			ArrayList<String> bypassingAttributes = new ArrayList<String>();// Combined
																			// name
																			// of
																			// endpoint
																			// and
																			// constant
			if (true) {// if (linkPrefix != null && !linkPrefix.equals("")) {

				Attribute mAttributeCombined = null;

				mAttributeCombined = AttributeFactory.createAttribute(
						attrToMergeName, Ontology.STRING);
				mAttributeCombined.setTableIndex(exampleSet.getAttributes()
						.allSize());

				// add new attribute
				exampleSet.getExampleTable().addAttribute(mAttributeCombined);
				exampleSet.getAttributes().addRegular(mAttributeCombined);

				// Adding the attribute to transferring arrays
				newAttributeNames.add(mAttributeCombined.getName());
				bypassingAttributes.add(mAttributeCombined.getName());

				for (Example ex : exampleSet) {
					String value = "";
					for (int i = 0; i < mPatternHolder.getPatternLength(); i++) {
						if (mPatternHolder.getDataIndex().get(i) == PatternValueHolder.IS_ATTRIBUTE) {
							// Lookup and put the values
							for (Attribute attr : exampleSet.getAttributes()) {
								if (mPatternHolder.getDataPart().get(i)
										.equals(attr.getName())) {
									String portionValue = ex
											.getValueAsString(attr);

									if (useDBpediaFormat) {
										portionValue = handleCapitalization(portionValue);
										portionValue = portionValue.replace(
												" ", "_");
									}

									if (doURLEncoding)
										try {
											portionValue = URLEncoder.encode(
													portionValue, "UTF-8");
										} catch (UnsupportedEncodingException e) {
											e.printStackTrace();
											throw new OperatorException(
													"Encoding UTF-8 is not supported",
													e);
										}

									value += portionValue;
									break;
								}
							}
						} else {
							value += mPatternHolder.getDataPart().get(i);
						}
					}

					ex.setValue(mAttributeCombined, value);
				}
			}

			// construct attribute set
			Attribute[] attributes = new Attribute[2];
			attributes[0] = AttributeFactory.createAttribute(NEW_ATTRIBUTES,
					Ontology.STRING);
			attributes[1] = AttributeFactory.createAttribute(
					BYPASSING_ATTRIBUTES, Ontology.STRING);

			MemoryExampleTable table = new MemoryExampleTable(attributes);

			@SuppressWarnings("deprecation")
			DataRowFactory ROW_FACTORY = new DataRowFactory(
					DataRowFactory.TYPE_SPARSE_MAP);

			for (int i = 0; i < bypassingAttributes.size(); i++) {
				DataRow row = ROW_FACTORY.create(new String[] {
						newAttributeNames.get(i), bypassingAttributes.get(i) },
						attributes);
				table.addDataRow(row);
			}

			ExampleSet ioListResult = table.createExampleSet();

			mOutputPort.deliver(exampleSet);
			mOutputPortAttrs.deliver(ioListResult);
		} else {
			mOutputPort.deliver(exampleSet);
			mOutputPortAttrs.deliver(null);
		}
		super.doWork();
	}

	@Override
	public List<ParameterType> getParameterTypes() {

		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeString(PARAMETER_PATTERN,
				"This parameter defines the pattern to generate the link.",
				"http://dbpedia.org/resource/*AttributeName*", false));

		types.add(new ParameterTypeString(PARAMETER_NEW_ATTRIBUTE_NAME,
				"This parameter defines the name of the new attribute",
				"New_Link", false));

		types.add(new ParameterTypeBoolean(PARAMETER_URL_ENCODING,
				"Use this to perform URL encoding on the final link.", false));

		types.add(new ParameterTypeBoolean(
				PARAMETER_DBPEDIA_FORMAT,
				"Use this to create links in DBpedia format, e.g., replacing blanks with underscores.",
				false));

		return types;
	}

	private static List<String> nonCapitalizedWords = new LinkedList<String>();

	static {
		for (String s : new String[] { "a", "an", "the", "but", "as", "if",
				"and", "or", "nor", "at", "of", "in", "from", "to", "at" }) {
			nonCapitalizedWords.add(s);
		}
	}

	private String handleCapitalization(String s) {
		String ret = "";
		StringTokenizer stk = new StringTokenizer(s, " ");
		boolean first = true;
		while (stk.hasMoreElements()) {
			if (ret.length() > 0)
				ret += " ";
			String tok = stk.nextToken();
			if (nonCapitalizedWords.contains(tok) && !first)
				ret += lowerFirstChar(tok);
			else
				ret += upperFirstChar(tok);
			first = false;
		}
		return ret;
	}

	private String upperFirstChar(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	private String lowerFirstChar(String s) {
		return s.substring(0, 1).toLowerCase() + s.substring(1);
	}

	/**
	 * Returns an object wrapper for the pattern
	 * 
	 * @param addressString
	 * @return
	 */
	private PatternValueHolder getAttributesFromPattern(String addressString) {
		PatternValueHolder mPatternHolder = new PatternValueHolder();

		if (addressString.length() > 0
				&& addressString.contains(PATTERN_LIMITER)) {
			int num = 0; // number of delimiters
			String bufferString = "";
			int attributeDetected = 0;// 0 = not initialized, 1 = outside, 2 =
										// inside
			for (int i = 0; i < addressString.length(); i++) {
				if (addressString.charAt(i) == PATTERN_LIMITER.charAt(0))
					num++;

				if (num % 2 == 1)// inside the content
				{
					if (attributeDetected != 2) {
						// Dump the chunk and clear the buffer
						mPatternHolder.addData(bufferString,
								PatternValueHolder.IS_CHUNK);
						bufferString = "";
					}
					bufferString += addressString.charAt(i);
					attributeDetected = 2;
				} else// outside
				{
					if (attributeDetected == 2) {
						String attrName = bufferString.substring(1,
								bufferString.length());
						mPatternHolder.addData(attrName,
								PatternValueHolder.IS_ATTRIBUTE);
						bufferString = "";
						attributeDetected = 1;
					} else {
						// add the symbol to the buffer
						bufferString += addressString.charAt(i);
					}
				}
			}
			mPatternHolder.addData(bufferString, PatternValueHolder.IS_CHUNK);
		} else {
			mPatternHolder.addData(addressString, PatternValueHolder.IS_CHUNK);
		}

		return mPatternHolder;
	}

	/**
	 * Returns prefix link from pattern, e.g. for string
	 * "http://dbpedia.org/resource/*City*" returns
	 * "http://dbpedia.org/resource/"
	 * 
	 * @param pattern
	 * @return
	 */
	private String getPrefixLinkFromPattern(String pattern) {
		return pattern.split(Pattern.quote(PATTERN_LIMITER))[0];
	}
}
