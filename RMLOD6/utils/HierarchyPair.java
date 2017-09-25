package lod.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class HierarchyPair implements Serializable {

	

	private static final long serialVersionUID = 7719788736766297420L;

	private String baseClass;

	private String superClass;

	private List<String> superClasses;

	private List<String> directSuperClasses;

	private List<String> subClasses;

	boolean isLeaf;

	boolean isChecked;

	private List<String> correspondingAttr;

	

	// used only for qualified relations
	private List<HierarchyPair> insideHierarchyPairs;

	public List<HierarchyPair> getInsideHierarchyPairs() {
		if (insideHierarchyPairs == null)
			insideHierarchyPairs = new ArrayList<HierarchyPair>();
		return insideHierarchyPairs;
	}

	public void setInsideHierarchyPairs(List<HierarchyPair> insideHierarchyPairs) {
		this.insideHierarchyPairs = insideHierarchyPairs;
	}

	public List<String> getDirectSuperClasses() {
		return directSuperClasses;
	}

	public void setDirectSuperClasses(List<String> directSuperClasses) {
		this.directSuperClasses = directSuperClasses;
	}

	public List<String> getCorrespondingAttr() {
		return correspondingAttr;
	}

	public void setCorrespondingAttr(List<String> correspondingAttr) {
		this.correspondingAttr = correspondingAttr;
	}

	public String getBaseClass() {
		return baseClass;
	}

	public String getSuperClass() {
		return superClass;
	}

	public List<String> getSuperClasses() {
		return superClasses;
	}

	public void setSuperClasses(List<String> superClasses) {
		this.superClasses = superClasses;
	}

	public List<String> getSubClasses() {
		return subClasses;
	}

	public void setSubClasses(List<String> subClasses) {
		this.subClasses = subClasses;
	}

	public boolean isLeaf() {
		return isLeaf;
	}

	public void setLeaf(boolean isLeaf) {
		this.isLeaf = isLeaf;
	}

	public boolean isChecked() {
		return isChecked;
	}

	public void setChecked(boolean isChecked) {
		this.isChecked = isChecked;
	}

	public void setBaseClass(String baseClass) {
		this.baseClass = baseClass;
	}

	public void setSuperClass(String superClass) {
		this.superClass = superClass;
	}

	public HierarchyPair(String baseClass, String superClass) {
		super();
		this.baseClass = baseClass;
		this.superClass = superClass;
		this.isChecked = false;
		this.isLeaf = false;
	}

}
