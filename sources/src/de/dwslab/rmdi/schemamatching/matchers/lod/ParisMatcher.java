package de.dwslab.rmdi.schemamatching.matchers.lod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lod.rdf.model.RdfHolder;
import paris.Result;
import paris.Setting;
import paris.storage.FactStore;
import de.dwslab.rmdi.operators.matchers.ParisLODMatcherOperator;
import de.dwslab.rmdi.schemamatching.matchers.lod.utils.ParisHelper;
import de.dwslab.rmdi.schemamatching.models.MatchedColumnsInfo;

public class ParisMatcher extends AbstractLODMatcher {

	Setting setting;
	FactStore fsLeft;
	FactStore fsRight;

	/** Stores the equality */
	public static Result computed;

	public static boolean useEntitiesEqualityFromTable = false;
	Map<String, String> equalitiesFromTable;
	private double acceptanceThredhold;

	public ParisMatcher(Map<String, Object> matchingParamteres,
			List<RdfHolder> allRDFs) {
		super(matchingParamteres, allRDFs);

		// get user preferred settings
		setting = new Setting("", "", "", "", "", "", null);
		Object paramValue = null;

		paramValue = matchingParamteres
				.get(ParisLODMatcherOperator.PARAMETER_LITERAL_SIMILARITY_DISTANCE);
		if (paramValue != null)
			setting.literalDistance = Setting.LiteralDistance
					.valueOf((String) paramValue);

		// setting.bothWays = getParameterAsBoolean(PARAMETER_BOTH_WAYS);
		setting.bothWays = false;

		paramValue = matchingParamteres
				.get(ParisLODMatcherOperator.PARAMTER_NUMBER_OF_ITERATIONS);
		if (paramValue != null)
			setting.endIteration = (Integer) paramValue;

		paramValue = matchingParamteres
				.get(ParisLODMatcherOperator.PARAMETER_JOIN_LENGTH_LIMIT);
		if (paramValue != null)
			setting.joinLengthLimit = (Integer) paramValue;

		paramValue = matchingParamteres
				.get(ParisLODMatcherOperator.PARAMETER_NORMALIZE_DATES);
		if (paramValue != null)
			setting.normalizeDatesToYears = (Boolean) paramValue;

		paramValue = matchingParamteres
				.get(ParisLODMatcherOperator.PARAMETER_NORMALIZE_STRINGS);
		if (paramValue != null)
			setting.normalizeStrings = (Boolean) paramValue;

		paramValue = matchingParamteres
				.get(ParisLODMatcherOperator.PARAMETER_POST_LITERAL_DISTANCE_THRESHOLD);
		if (paramValue != null)
			setting.postLiteralDistanceThreshold = (Double) paramValue;

		paramValue = matchingParamteres
				.get(ParisLODMatcherOperator.PARAMETER_TABLE_ORDER_EQUALITIES);
		if (paramValue != null)
			useEntitiesEqualityFromTable = (Boolean) paramValue;

		paramValue = matchingParamteres
				.get(ParisLODMatcherOperator.PARAMETER_ALLIGNMENT_THRESHOLD);
		if (paramValue != null)
			acceptanceThredhold = (Double) paramValue;
		// TODO: get the data fusion approaches

		// generate the factories
		fsLeft = new FactStore(setting, "", "", setting.joinLengthLimit,
				setting.normalizeStrings, setting.normalizeDatesToYears);

		fsRight = new FactStore(setting, "", "", setting.joinLengthLimit,
				setting.normalizeStrings, setting.normalizeDatesToYears);

	}

	@Override
	public List<MatchedColumnsInfo> matchTwoRdfs(RdfHolder leftTable,
			RdfHolder rightTable) {
		// generate the factories
		fsLeft = new FactStore(setting, "", "", setting.joinLengthLimit,
				setting.normalizeStrings, setting.normalizeDatesToYears);

		fsRight = new FactStore(setting, "", "", setting.joinLengthLimit,
				setting.normalizeStrings, setting.normalizeDatesToYears);
		
		// initialize the paris variables
		fsLeft = ParisHelper.loadFactStoreFromRdfHolder(leftTable, fsLeft);
		fsRight = ParisHelper.loadFactStoreFromRdfHolder(rightTable, fsRight);
		try {
			computed = new Result(setting, fsLeft, fsRight, setting.tsvFolder);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		List<MatchedColumnsInfo> matchedColumns = new ArrayList<MatchedColumnsInfo>();
		try {

			for (int i = 0; i < setting.endIteration; i++) {
				matchedColumns = new ArrayList<MatchedColumnsInfo>();
				ParisHelper.oneIteration(1, fsLeft, fsRight, computed, setting,
						matchedColumns, null, acceptanceThredhold);
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	//	if (leftTable.getGeneratorTypes().contains(GeneratorType.CLASSES)) {

			ParisHelper.computeClassesOneWay(fsLeft, fsRight, computed,
					matchedColumns, acceptanceThredhold);

		//}

		// set the rdfHolders and the corresponding RM attributes
		for (MatchedColumnsInfo info : matchedColumns) {
			info.getLeftColumn().setRdfHolder(leftTable);
			info.getRightColumn().setRdfHolder(rightTable);

			info.getLeftColumn().setAttributeName(
					leftTable.getRMAttrByURI(info.getLeftColumn().getUri()));
			info.getRightColumn().setAttributeName(
					rightTable.getRMAttrByURI(info.getRightColumn().getUri()));
		}

		return matchedColumns;

	}

}
