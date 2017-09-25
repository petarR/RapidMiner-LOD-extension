package lod.generators.dataclasses;

import lod.generators.BaseGenerator;

/**
 * Contains a set of possible generators that are extending {@link BaseGenerator}
 * These codes are used to determine shich generator is used for feature extraction
 * @author Evgeny Mitichkin
 *
 */
public class Generators {

	public final static int RELATION_NUMERIC_FEATURE = 1;
	public final static int RELATION_PRESENCE_FEATURE = 2;
	public final static int RELATION_TYPE_NUMERIC_FEATURE = 3;
	public final static int RELATION_TYPE_PRESENCE = 4;
	public final static int DATA_PROPERTIES = 5;
	public final static int SIMPLE_TYPE = 6;
	public final static int RELATION_VALUE_FEATURE = 7;
	
}
