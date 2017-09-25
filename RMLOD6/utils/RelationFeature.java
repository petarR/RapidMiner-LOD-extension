package lod.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelationFeature {
	private String direction;

	private String relationName;

	private Map<String, String> allTypes;

	public RelationFeature(String direction, String relationName,
			Map<String, String> allTypes) {
		super();
		this.direction = direction;
		this.relationName = relationName;
		this.allTypes = allTypes;
	}

	public RelationFeature() {
		// TODO Auto-generated constructor stub
		allTypes = new HashMap<String, String>();
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public String getRelationName() {
		return relationName;
	}

	public void setRelationName(String relationName) {
		this.relationName = relationName;
	}

	public Map<String, String> getAllTypes() {
		return allTypes;
	}

	public void setAllTypes(Map<String, String> allTypes) {
		this.allTypes = allTypes;
	}

}
