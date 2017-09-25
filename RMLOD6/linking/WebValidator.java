package lod.linking;

import java.util.ArrayList;
import java.util.List;

import lod.gui.tools.dialogs.SPARQLConfigurator;
import lod.sparql.SPARQLEndpointQueryRunner;
import lod.sparql.SPARQLQueryRunner.QuerryRunnerType;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.Condition;
import com.rapidminer.example.set.ConditionCreationException;
import com.rapidminer.example.set.ConditionedExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.config.ParameterTypeConfigurable;

/**
 * Web Validator that is checking the presence of a given attribute value.
 * Normally used together with {@link SimpleLinker}.
 * 
 * @author Evgeny Mitichkin
 * @author Petar Ristoski
 * 
 */
public class WebValidator extends Operator {

	private static final String CLASS_NAME = "web_validator";

	public static final String PARAMETER_ATTRIBUTE_TO_VERIFY = "Attribute to verify";

	public static final String PARAMETER_SPARQL_MANAGER = "SPARQL connection";

	private static final String NEW_ATTRIBUTES = "New Attributes";
	private static final String BYPASSING_ATTRIBUTES = "Bypassing Attributes";

	public static final String PARAMETER_RESOLVE_BY_URI = "Use URI data model";

	public static final String PARAMETER_ATTRIBUTE_TO_EXTEND = "Attribute to extend";

	public static final String PARAMETER_ATTRIBUTE_TO_EXTEND_NAME = "Attribute to extend name";

	private InputPort mInputPort;
	private InputPort mInputPortAddedAttrs;
	private OutputPort mOutputPort;
	private OutputPort mOutputPortResult;
	private OutputPort mOutputPortAddedAttrs;

	private SPARQLEndpointQueryRunner queryRunner;

	private ExampleSet exampleSet;

	public WebValidator(OperatorDescription description) {
		super(description);

		mInputPort = getInputPorts()
				.createPort("Example Set", ExampleSet.class);
		mInputPortAddedAttrs = getInputPorts()
				.createPort("Attributes Appended");

		mInputPort.addPrecondition(new SimplePrecondition(mInputPort,
				new MetaData(ExampleSet.class)));
		// mInputPortAddedAttrs.addPrecondition(new
		// ExampleSetPrecondition(mInputPortAddedAttrs, new String[]
		// {NEW_ATTRIBUTES, BYPASSING_ATTRIBUTES}, Ontology.ATTRIBUTE_VALUE));

		mOutputPort = getOutputPorts().createPort("Appended Set");
		mOutputPortResult = getOutputPorts().createPort("Filtered Set");
		mOutputPortAddedAttrs = getOutputPorts().createPort(
				"Attributes Appended");

		getTransformer().addPassThroughRule(mInputPort, mOutputPort);
		// getTransformer().addPassThroughRule(mInputPortAddedAttrs,
		// mOutputPortAddedAttrs);
		getTransformer().addGenerationRule(mOutputPortResult, ExampleSet.class);
	}

	@Override
	public void doWork() throws OperatorException {
		exampleSet = mInputPort.getData(ExampleSet.class);
		ArrayList<String> newAttributeNames = new ArrayList<String>();

		ArrayList<String> attrNames = new ArrayList<String>();
		ArrayList<String> attrsBypsass = new ArrayList<String>();
		getAttsNames(attrNames, attrsBypsass);

		try {
			queryRunner = SPARQLEndpointQueryRunner.initRunner(this,
					queryRunner);
		} catch (Exception e) {
			e.printStackTrace();
			throw new UserError(this, 2001, CLASS_NAME, e.getMessage());
		}

		if (attrNames != null) {
			Attributes attrs = exampleSet.getAttributes();
			for (int i = 0; i < attrNames.size(); i++) {
				Attribute mAttributeRecordExists = AttributeFactory
						.createAttribute("RecordExistsFor" + attrNames.get(i),
								Ontology.BINOMINAL);

				attrs.addRegular(mAttributeRecordExists);
				exampleSet.getExampleTable().addAttribute(
						mAttributeRecordExists);
				newAttributeNames.add("RecordExistsFor" + attrNames.get(i));
			}

			for (Example ex : exampleSet) {
				if (queryRunner.mUIThreadRunning) {
					String attributeName = ex.getValueAsString(attrs
							.get(attrsBypsass.get(0)));
					if (queryRunner.getRunnerType() == QuerryRunnerType.URLBASED) {
						queryRunner.updateModel(attributeName);
					}
					Boolean ress = queryRunner
							.runAskQueryInterruptable("ask {<" + attributeName
									+ "> ?x ?y}");
					Attribute attr = attrs.get("RecordExistsFor"
							+ attrNames.get(0));
					ex.setValue(attr, Boolean.toString((ress)));
				} else {
					break;
				}
			}
		}

		ExampleSet eset = null;
		for (int i = 0; i < newAttributeNames.size(); i++) {
			eset = applyFilter(exampleSet, newAttributeNames.get(i) + "=true");
		}

		for (int i = 0; i < newAttributeNames.size(); i++) {
			@SuppressWarnings("unused")
			boolean removal = eset.getAttributes().remove(
					eset.getAttributes().get(newAttributeNames.get(i)));
		}
		// list.getList().add(newAttributeNames);//New attributes with
		// "RecordExistsFor" combined with endpoint with index 2

		mOutputPort.deliver(exampleSet);
		mOutputPortResult.deliver(eset);

		// construct attribute set for metadata transfer
		Attribute[] attributes = new Attribute[2];
		attributes[0] = AttributeFactory.createAttribute(NEW_ATTRIBUTES,
				Ontology.STRING);
		attributes[1] = AttributeFactory.createAttribute(BYPASSING_ATTRIBUTES,
				Ontology.STRING);

		MemoryExampleTable table = new MemoryExampleTable(attributes);

		@SuppressWarnings("deprecation")
		DataRowFactory ROW_FACTORY = new DataRowFactory(
				DataRowFactory.TYPE_SPARSE_MAP);

		for (int i = 0; i < attrsBypsass.size(); i++) {
			DataRow row = ROW_FACTORY.create(
					new String[] { newAttributeNames.get(i),
							attrsBypsass.get(i) }, attributes);
			table.addDataRow(row);
		}

		ExampleSet ioListResult = table.createExampleSet();

		mOutputPortAddedAttrs.deliver(ioListResult);
		super.doWork();
	}

	public ExampleSet applyFilter(ExampleSet inputSet, String parameter_)
			throws OperatorException {
		getLogger()
				.fine(getName() + ": input set has " + inputSet.size()
						+ " examples.");

		String className = "attribute_value_filter";
		String parameter = parameter_;
		getLogger().fine(
				"Creating condition '" + className + "' with parameter '"
						+ parameter + "'");
		Condition condition = null;
		try {
			condition = ConditionedExampleSet.createCondition(className,
					inputSet, parameter);
		} catch (ConditionCreationException e) {
			throw new UserError(this, e, 904, className, e.getMessage());
		}
		ExampleSet result = new ConditionedExampleSet(inputSet, condition,
				false);
		return result;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_RESOLVE_BY_URI,
				"If cheked, the operator will try to dereference the URIs",
				false, false));
		ParameterType type = new ParameterTypeConfigurable(
				PARAMETER_SPARQL_MANAGER, "Choose SPARQL endpoint connection",
				SPARQLConfigurator.I18N_BASE_KEY);

		type.registerDependencyCondition(new BooleanParameterCondition(this,
				PARAMETER_RESOLVE_BY_URI, true, false));
		types.add(type);

		types.add(new ParameterTypeEnumeration(PARAMETER_ATTRIBUTE_TO_EXTEND,
				"Attributes that contain LOD links", new ParameterTypeString(
						PARAMETER_ATTRIBUTE_TO_EXTEND_NAME, "Attribute name"),
				false));
		return types;
	}

	@Override
	public void processFinished() throws OperatorException {
		if (queryRunner != null) {
			queryRunner.mUIThreadRunning = false;
			queryRunner.finalizeAsyncThread();
		}
		super.processFinished();
	}

	/**
	 * Gets the name of the attribut ethat is used as a target for feature
	 * extraction
	 * 
	 * @param attsNames
	 * @param attsBypass
	 * @throws OperatorException
	 */
	public void getAttsNames(List<String> attsNames, List<String> attsBypass)
			throws OperatorException {
		if (mInputPortAddedAttrs.isConnected()) {
			ExampleSet set = null;
			try {
				set = mInputPortAddedAttrs.getData(ExampleSet.class);
			} catch (Exception e) {
				throw new UserError(this, 1002, CLASS_NAME, e.getMessage());
			}
			for (Example ex : set) {
				attsNames.add(ex.getValueAsString(set.getAttributes().get(
						NEW_ATTRIBUTES)));// Endpoints
				attsBypass.add(ex.getValueAsString(set.getAttributes().get(
						BYPASSING_ATTRIBUTES)));
			}
		} else {
			boolean isParamFound = true;

			if (isParameterSet(PARAMETER_ATTRIBUTE_TO_EXTEND)) {
				String[] macroNames = ParameterTypeEnumeration
						.transformString2Enumeration(getParameterAsString(PARAMETER_ATTRIBUTE_TO_EXTEND));
				for (String macroName : macroNames) {
					if (exampleSet.getAttributes().get(macroName.trim()) == null)
						throw new UserError(
								this,
								1006,
								CLASS_NAME,
								"The specified attribute \""
										+ macroName
										+ "\" is not defined for the ExampleSet");
					attsBypass.add(macroName);
					attsNames.add(macroName);
				}
				isParamFound = false;
			}
			if (isParamFound)
				throw new UserError(this, 1003, CLASS_NAME);
		}
	}
}
