package lod.dataclasses;

/**
 * Wrapper class for a tree node
 * @author Evgeny Mitichkin
 *
 */
public class TreeNode {

	private int level;
	private String value;
	private boolean passed;
	private String filter;
	
	public TreeNode(String value, int level, String filter) {
		this.setLevel(level);
		this.setValue(value);
		this.setPassed(false);
		this.filter = filter;
	}
	
	public TreeNode(String value, int level) {
		this.setLevel(level);
		this.setValue(value);
		this.setPassed(false);
		this.setFilter("");
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public boolean isPassed() {
		return passed;
	}

	public void setPassed(boolean passed) {
		this.passed = passed;
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}	
}
