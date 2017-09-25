package lod.containers;

import java.util.List;

import lod.generators.BaseGenerator;
import lod.gui.tools.dialogs.SPARQLConfigurator;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.SimpleOperatorChain;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.tools.config.ParameterTypeConfigurable;

/**
 * Enables execution of a set of operators with a common specified set of parameters.
 * @author Evgeny Mitichkin
 *
 */
public class SimpleSPARQLPassContainer extends SimpleOperatorChain{
	
	public static final String PARAMETER_RESOLVE_BY_URI = "Use URI data model";
	
	public static final String PARAMETER_SPARQL_MANAGER = "SPARQL connection";
	
	public static final String PARAMETER_SHOULD_PASS_PARAMETER_VALUES = "Override parameter values";
	
	public SimpleSPARQLPassContainer(OperatorDescription description) {
		super(description);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void doWork() throws OperatorException {		
		if (getParameterAsBoolean(PARAMETER_SHOULD_PASS_PARAMETER_VALUES)) {
			setParametersToOperators();
		}					
		
		//deliver
		super.doWork();
	}
	
	/**
	 * Sets the values of parameters ({@literal "SPARQL connection"} in this case) to all nested operators if 
	 * their subclass is {@link BaseGenerator}.
	 * @throws OperatorException
	 */
	protected void setParametersToOperators() throws OperatorException {		
		List<Operator> operators = getAllInnerOperators();	
		for (Operator op : operators) {
			if (op instanceof BaseGenerator)
				op.setParameter(PARAMETER_RESOLVE_BY_URI, Boolean.toString(false));
				op.setParameter(PARAMETER_SPARQL_MANAGER, getParameter(PARAMETER_SPARQL_MANAGER));				
		}
	}
	
	@Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeConfigurable(PARAMETER_SPARQL_MANAGER,
				"Choose SPARQL endpoint connection",
				SPARQLConfigurator.I18N_BASE_KEY));
		types.add(new ParameterTypeBoolean(PARAMETER_SHOULD_PASS_PARAMETER_VALUES, "Determines whether the container parameters should be applied to the nested generators", true, true));
        return types;
    }
}
