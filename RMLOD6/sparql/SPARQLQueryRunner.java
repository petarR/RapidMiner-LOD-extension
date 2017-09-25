package lod.sparql;

import com.hp.hpl.jena.query.ResultSet;
import com.rapidminer.operator.OperatorException;

public interface SPARQLQueryRunner {
	public enum QuerryRunnerType {
		FILEBASED, ENDPOINTBASED, URLBASED
	}

	public ResultSet runSelectQuery(String query) throws OperatorException;

	public boolean runAskQuery(String query) throws OperatorException;
}
