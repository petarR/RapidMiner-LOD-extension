package lod.gui.renderer;

import lod.utils.HierarchyGraphCreator;
import lod.utils.OntologyHierarchy;

import com.rapidminer.gui.graphs.GraphCreator;
import com.rapidminer.gui.graphs.TreeModelGraphCreator;
import com.rapidminer.gui.renderer.AbstractGraphRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.learner.tree.TreeModel;

public class OntologyHierarchyGraphRenderer extends AbstractGraphRenderer {
	@Override
	public GraphCreator<String, String> getGraphCreator(Object renderable,
			IOContainer ioContainer) {
		OntologyHierarchy ontologyHierarchy = (OntologyHierarchy) renderable;
		return new HierarchyGraphCreator(ontologyHierarchy);

	}

	@Override
	public String getDefaultLayout() {
		return LAYOUT_TREE;
	}
}
