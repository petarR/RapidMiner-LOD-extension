package de.dwslab.rmdi.fusion.fusers;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;

import de.dwslab.rmdi.operators.fusers.AbstractFuserOperator;
import de.dwslab.rmdi.schemamatching.models.ColumnInfo;
import de.dwslab.rmdi.schemamatching.models.MatcherResults;

public abstract class AbstractFuser {

	public enum FusionApproach {
		First, Median, Average, Random, Voting, Longest
	}

	public static enum DataType {
		numeric, string, coordinate, date, link, bool, unknown, unit, list
	};

	protected FusionApproach numericFusionSelectedApproach;

	protected FusionApproach stringFusionSelectedApproach;

	protected ExampleSet set;

	protected MatcherResults matcherResuts;

	public AbstractFuser(Map<String, Object> fusingParameters, ExampleSet set,
			MatcherResults matcherResuts) {
		super();
		this.numericFusionSelectedApproach = FusionApproach
				.valueOf((String) fusingParameters
						.get(AbstractFuserOperator.PARAMETER_RESOLVE_NUMERIC_DUPLICATES));
		this.stringFusionSelectedApproach = FusionApproach
				.valueOf((String) fusingParameters
						.get(AbstractFuserOperator.PARAMETER_RESOLVE_STRING_DUPLICATES));
		this.set = set;
		this.matcherResuts = matcherResuts;
	}

	/**
	 * resolves the duplicates inside the table
	 */
	public void doFusing() {
		List<List<ColumnInfo>> groupedMatchedColumn = matcherResuts
				.getGroupedMatchedColumn();

		for (List<ColumnInfo> columnInfos : groupedMatchedColumn) {
			resoloveDuplicatesForColumns(columnInfos);
		}
	}

	/**
	 * resolves one list of duplicates
	 * 
	 * @param columnInfos
	 */
	protected void resoloveDuplicatesForColumns(List<ColumnInfo> columnInfos) {
		// holds the corresponding RM attributes that needs to be merged
		List<Attribute> rmAttributes = new LinkedList<Attribute>();

		DataType areColumnsNumerical = DataType.numeric;
		for (ColumnInfo colInfo : columnInfos) {
			Attribute attribute = set.getAttributes().get(
					colInfo.getRdfHolder().getRMAttrByURI(colInfo.getUri()));
			if (attribute != null && set.getAttributes().contains(attribute)) {
				rmAttributes.add(attribute);
				if (attribute.getValueType() != com.rapidminer.tools.Ontology.NUMERICAL)
					areColumnsNumerical = DataType.string;
			}

			// remove the triples for this uri
		//	colInfo.getRdfHolder().removeTriplesByUri(colInfo.getUri());
		//	colInfo.getRdfHolder().updateRawDataFromTriples();
		}
		resolveDuplicatesForAttributes(rmAttributes, areColumnsNumerical);

		// remove the attributes from the table

		for (Attribute att : rmAttributes) {
			if (set.getAttributes().contains(att))
				set.getAttributes().remove(att);
		}
		// TODO add the new triples
	}

	protected abstract void resolveDuplicatesForAttributes(
			List<Attribute> attributes, DataType datatype);

}
