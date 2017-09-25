package lod.dataclasses;

import java.util.List;

import com.rapidminer.example.Example;

/**
 * Wrapper class for a tree. Used to store a tree for a single {@link Example} in {@link URLBasedQueryRunnerContainer}. 
 * @author Evgeny Mitichkin
 *
 */
public class ExampleTreeMapping {
	
	private List<TreeNode> sameAsTree;
	private Example ex;
	
	public ExampleTreeMapping(List<TreeNode> tree, Example ex) {
		this.setSameAsTree(tree);
		this.setEx(ex);
	}

	public List<TreeNode> getSameAsTree() {
		return sameAsTree;
	}

	public void setSameAsTree(List<TreeNode> sameAsTree) {
		this.sameAsTree = sameAsTree;
	}

	public Example getEx() {
		return ex;
	}

	public void setEx(Example ex) {
		this.ex = ex;
	}

}
