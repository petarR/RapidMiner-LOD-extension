package de.dwslab.rmdi.gui.renderers;

import java.util.List;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.rapidminer.gui.renderer.AbstractTableModelTableRenderer;
import com.rapidminer.operator.IOContainer;

import de.dwslab.rmdi.schemamatching.models.MatchedColumnsInfo;
import de.dwslab.rmdi.schemamatching.models.MatcherResults;

public class MatchingResultsRenderer extends AbstractTableModelTableRenderer {

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "MatchingResults";
	}

	@Override
	public TableModel getTableModel(Object renderable, IOContainer ioContainer,
			boolean isReporting) {
		if (renderable instanceof MatcherResults) {
			MatcherResults matcherResults = (MatcherResults) renderable;
			final List<MatchedColumnsInfo> matchedColumns = matcherResults
					.getAllMatchedColumns();

			return new AbstractTableModel() {

				@Override
				public Object getValueAt(int rowIndex, int columnIndex) {
					MatchedColumnsInfo match = matchedColumns.get(rowIndex);
					if (columnIndex == 0)
						return match.getLeftColumn().getAttributeName();
					else if (columnIndex == 1)
						return match.getRightColumn().getAttributeName();
					return match.getMatchingProbability();
				}

				@Override
				public int getRowCount() {
					// TODO Auto-generated method stub
					return matchedColumns.size();
				}

				@Override
				public int getColumnCount() {
					// TODO Auto-generated method stub
					return 3;
				}

				@Override
				public String getColumnName(int column) {
					if (column == 0)
						return "Left Column";
					else if (column == 1)
						return "Rigth Column";
					return "Confidence";
				}

			};
		}
		// TODO Auto-generated method stub
		return new DefaultTableModel();
	}
}
