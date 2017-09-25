package lod.utils;

import java.util.List;

public class DBpediaCustomSource {

	public String URI;
	public List<String> types;
	public String surfaceFrom;

	public String getURI() {
		return URI;
	}

	public void setURI(String uRI) {
		URI = uRI;
	}

	public List<String> getTypes() {
		return types;
	}

	public void setTypes(List<String> types) {
		this.types = types;
	}

	public String getSurfaceFrom() {
		return surfaceFrom;
	}

	public void setSurfaceFrom(String surfaceFrom) {
		this.surfaceFrom = surfaceFrom;
	}

	public DBpediaCustomSource(String uRI, List<String> types,
			String surfaceFrom) {
		super();
		URI = uRI;
		this.types = types;
		this.surfaceFrom = surfaceFrom;
	}

}
