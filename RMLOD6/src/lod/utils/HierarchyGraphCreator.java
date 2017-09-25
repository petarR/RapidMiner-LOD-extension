package lod.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lod.gui.renderer.HierarchyNodeLabelRenderer;
import lod.gui.renderer.HierarchyNodeRenderer;

import com.rapidminer.gui.graphs.GraphCreatorAdaptor;
import com.rapidminer.gui.graphs.TreeModelEdgeLabelRenderer;

import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.renderers.Renderer.EdgeLabel;
import edu.uci.ics.jung.visualization.renderers.Renderer.Vertex;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel;

public class HierarchyGraphCreator extends GraphCreatorAdaptor {

	OntologyHierarchy hierarchy;
	private Map<String, HierarchyPair> vertexMap = new HashMap<String, HierarchyPair>();

	private List<String> addedPairs = new ArrayList<String>();

	public OntologyHierarchy getModel() {
		return hierarchy;
	}

	public HierarchyGraphCreator(OntologyHierarchy hierarchy) {
		this.hierarchy = hierarchy;
	}

	@Override
	public Graph<String, String> createGraph() {
		DirectedOrderedSparseMultigraph<String, String> treeGraph = new DirectedOrderedSparseMultigraph<String, String>();
		buildTheTree(treeGraph);
		return treeGraph;
		// return new DelegateForest<String, String>(treeGraph);

	}

	public void buildTheTree(
			DirectedOrderedSparseMultigraph<String, String> treeGraph) {
		for (HierarchyPair pair : hierarchy.getHierarchyPairs()) {
			if (!treeGraph.containsVertex(pair.getBaseClass()))
				treeGraph.addVertex(pair.getBaseClass());
			for (String parent : pair.getDirectSuperClasses()) {
				if (!treeGraph.containsVertex(parent))
					treeGraph.addVertex(parent);
				// don't add the same pair again
				if (addedPairs.contains(parent + pair.getBaseClass()))
					continue;
				addedPairs.add(parent + pair.getBaseClass());
				treeGraph.addEdge(parent + pair.getBaseClass(),
						pair.getBaseClass(), parent);
				vertexMap.put(pair.getBaseClass(), pair);
			}
			// if (pair.getDirectSuperClasses().size() == 0) {
			// if (!treeGraph.containsVertex(pair.getBaseClass()))
			// treeGraph.addVertex(pair.getBaseClass());
			// }
		}
	}

	@Override
	public String getVertexName(String object) {

		return object;
	}

	@Override
	public String getVertexToolTip(String object) {
		// HierarchyPair tree = vertexMap.get(object);
		// if (tree != null) {
		// StringBuffer result = new StringBuffer();
		// if (tree.isLeaf()) {
		// String labelString = tree.getLabel();
		// if (labelString != null) {
		// result.append("<html><b>Class:</b>&nbsp;" + labelString + "<br>");
		// result.append("<b>Size:</b>&nbsp;" + tree.getFrequencySum() +
		// "<br>");
		// result.append("<b>Class frequencies:</b>&nbsp;" +
		// SwingTools.transformToolTipText(tree.getCounterMap().toString()) +
		// "</html>");
		// }
		// } else {
		// result.append("<html><b>Subtree Size:</b>&nbsp;" +
		// tree.getSubtreeFrequencySum() + "</html>");
		// }
		// return result.toString();
		// } else {
		// return null;
		// }
		return "";
	}

	@Override
	public String getEdgeName(String object) {
		// SplitCondition condition = edgeMap.get(object);
		// if (condition != null) {
		// return condition.getRelation() + " " + condition.getValueString();
		// } else {
		// return null;
		// }
		return "";
	}

	@Override
	public boolean isLeaf(String object) {
		HierarchyPair pair = vertexMap.get(object);
		if (pair != null && pair.isLeaf)
			return true;
		// Tree tree = vertexMap.get(object);
		// if (tree != null) {
		// return tree.isLeaf();
		// } else {
		// return false;
		// }
		return false;
	}

	@Override
	public Vertex<String, String> getVertexRenderer() {
		int maxSize = -1;
		// this.hierarchy.getHierarchyLeafs().get(0).getSuperClasses().size();
		return new HierarchyNodeRenderer<String, String>(this, maxSize);
	}

	@Override
	public EdgeLabel<String, String> getEdgeLabelRenderer() {
		return new TreeModelEdgeLabelRenderer<String, String>();
	}

	@Override
	public VertexLabel<String, String> getVertexLabelRenderer() {
		return new HierarchyNodeLabelRenderer<String, String>(this);
	}

	@Override
	public boolean isEdgeLabelDecorating() {
		return true;
	}

	@Override
	public int getMinLeafHeight() {
		return 26;
	}

	@Override
	public int getMinLeafWidth() {
		return 40;
	}

	@Override
	public boolean isBold(String id) {
		return isLeaf(id);
	}

	@Override
	public boolean isRotatingEdgeLabels() {
		return true;
	}

	@Override
	public double getEdgeStrength(String id) {
		return 0;
	}

	@Override
	public Object getObject(String id) {
		return vertexMap.get(id);
	}

	/**
	 * Returns 0 (for other values the edge label painting will not work).
	 */
	@Override
	public int getLabelOffset() {
		return 0;
	}

}
