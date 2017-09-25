package lod.gui.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import lod.utils.HierarchyPair;
import lod.utils.OntologyHierarchy;

import com.rapidminer.gui.renderer.AbstractTableModelTableRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.tools.container.Pair;

public class OntologyHierarchyRenderer extends AbstractTableModelTableRenderer {

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "OntologyHierarchy";
	}

	@Override
	public TableModel getTableModel(Object renderable, IOContainer ioContainer,
			boolean isReporting) {
		if (renderable instanceof OntologyHierarchy) {
			OntologyHierarchy ontologyHierarchy = (OntologyHierarchy) renderable;
			final List<HierarchyPair> ontologyPairs = ontologyHierarchy
					.getHierarchyPairs();

			final List<Pair<String, String>> values = new ArrayList<Pair<String, String>>();

			for (HierarchyPair hPair : ontologyPairs) {
				values.add(new Pair<String, String>(hPair.getBaseClass(), hPair
						.getSuperClass()));
			}

			return new AbstractTableModel() {

				@Override
				public Object getValueAt(int rowIndex, int columnIndex) {
					Pair<String, String> pair = values.get(rowIndex);
					if (columnIndex == 0)
						return pair.getFirst();
					return pair.getSecond();
				}

				@Override
				public int getRowCount() {
					// TODO Auto-generated method stub
					return values.size();
				}

				@Override
				public int getColumnCount() {
					// TODO Auto-generated method stub
					return 2;
				}

				@Override
				public String getColumnName(int column) {
					if (column == 0)
						return "Class";
					return "SuperClass";
				}

			};
		}
		// TODO Auto-generated method stub
		return new DefaultTableModel();
	}
}
