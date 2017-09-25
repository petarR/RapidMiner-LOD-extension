package lod.gui.renderer;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import lod.rdf.model.RdfHolder;
import lod.rdf.model.RdfTriple;
import lod.utils.HierarchyPair;

import com.rapidminer.gui.renderer.AbstractTableModelTableRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.tools.container.Pair;

public class RdfDataRenderer extends AbstractTableModelTableRenderer {

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "RDF";
	}

	@Override
	public TableModel getTableModel(Object renderable, IOContainer ioContainer,
			boolean isReporting) {
		if (renderable instanceof RdfHolder) {
			RdfHolder rdfHolder = (RdfHolder) renderable;
			final List<RdfTriple> triples = rdfHolder.getTriples();

			return new AbstractTableModel() {

				@Override
				public Object getValueAt(int rowIndex, int columnIndex) {
					RdfTriple triple = triples.get(rowIndex);
					if (columnIndex == 0)
						return triple.getSubject();
					else if (columnIndex == 1)
						return triple.getPredicate();
					return triple.getObject();
				}

				@Override
				public int getRowCount() {
					// TODO Auto-generated method stub
					return triples.size();
				}

				@Override
				public int getColumnCount() {
					// TODO Auto-generated method stub
					return 3;
				}

				@Override
				public String getColumnName(int column) {
					if (column == 0)
						return "Subject";
					else if (column == 1)
						return "Predicate";
					return "Object";
				}

			};
		}
		// TODO Auto-generated method stub
		return new DefaultTableModel();
	}
}
