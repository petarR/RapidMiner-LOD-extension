package lod.gui.tools.wizzards.olap4ld;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import lod.olap4ld.CubeSPARQLExplorer;
import lod.olap4ld.OLAPModel;

import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;

public class ValuesSelectionStep extends WizardStep {

	private final JPanel rootPanel = new JPanel(new BorderLayout());
	private List<DimensionWrapper> dimensions = new LinkedList<DimensionWrapper>();

	private JPanel dimensionPanel;
	private JScrollPane scrollPane;

	private boolean canProoceedFlag = false;

	public void setCanProoceedFlag(boolean canProoceedFlag) {
		this.canProoceedFlag = canProoceedFlag;
	}

	public ValuesSelectionStep() {
		super("olap4ldgui.attributeselection");

		dimensionPanel = new JPanel(new GridBagLayout());

		scrollPane = new JScrollPane(dimensionPanel);
		scrollPane
				.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		rootPanel.add(scrollPane, BorderLayout.CENTER);

		createTheStep();

	}

	/**
	 * Returns a set of values for every dimension
	 * 
	 * @return
	 */
	public String getSelectedAttributeSet(OLAPModel olapModel) {
		String message = "";
		Map<String, Map<String, Map<Integer, String>>> result = new LinkedHashMap<String, Map<String, Map<Integer, String>>>();
		for (DimensionWrapper dm : dimensions) {
			if (dm.getSelectedAttributes().size() == 0) {
				message = "Please select at least one value for the dimension \""
						+ dm.getName() + "\"";
				return message;
			}
			String dimensionName = dm.getDimensionName();
			Map<String, Map<Integer, String>> valsEasyMap = olapModel
					.getMembersDimension(olapModel.getDimensionEasyMap()
							.get(dimensionName).keySet().iterator().next());

			Map<String, Map<Integer, String>> selectedValues = new HashMap<String, Map<Integer, String>>();
			for (String sValue : dm.getSelectedAttributes()) {
				selectedValues.put(sValue, valsEasyMap.get(sValue));
			}

			result.put(dimensionName, selectedValues);
		}
		olapModel.setSelectedValuesPerDimension(result);
		return message;
	}

	/**
	 * Returns a set of values for every dimension
	 * 
	 * @return
	 */
	public String getSelectedAttributeSet(CubeSPARQLExplorer cubeExplorer) {
		String message = "";
		Map<String, Map<String, Map<Integer, String>>> result = new LinkedHashMap<String, Map<String, Map<Integer, String>>>();
		for (DimensionWrapper dm : dimensions) {
			if (dm.getSelectedAttributes().size() == 0) {
				message = "Please select at least one value for the dimension \""
						+ dm.getName() + "\"";
				return message;
			}
			Map<String, String> allValues = null;
			// get the row
			if (cubeExplorer.getAllMembers().containsKey(dm.getDimensionName()))
				allValues = cubeExplorer.getAllMembers().get(
						dm.getDimensionName());

			Map<String, String> selectedValues = new LinkedHashMap<String, String>();
			for (String sValue : dm.getSelectedAttributes()) {
				selectedValues.put(sValue, allValues.get(sValue));
			}
			if (cubeExplorer.getSelectedRowsByUser().contains(
					dm.getDimensionName()))
				cubeExplorer.getSelectedRows().put(
						cubeExplorer.getAllDimensions().get(
								dm.getDimensionName()), selectedValues);
			else
				cubeExplorer.getSelectedColumns().put(
						cubeExplorer.getAllDimensions().get(
								dm.getDimensionName()), selectedValues);
		}

		// olapModel.setSelectedValuesPerDimension(result);
		return message;
	}

	@Override
	protected JComponent getComponent() {
		return rootPanel;
	}

	@Override
	protected boolean canProceed() {
		// TODO Auto-generated method stub
		return canProoceedFlag;
	}

	@Override
	protected boolean canGoBack() {
		// TODO Auto-generated method stub
		return true;
	}

	/**
	 * populates the initial values
	 */
	protected void populateTheListsWithOlapValues(OLAPModel olapModel) {
		// clear the panel if dimensions already exist
		if (dimensions != null && dimensions.size() > 0) {
			dimensionPanel.removeAll();
		}

		dimensions = new LinkedList<DimensionWrapper>();
		for (String dim : olapModel.getSelectedDimensions()) {
			String dimName = dim;
			Map<Integer, String> realDim = olapModel.getDimensionEasyMap().get(
					dim);
			Map<String, Map<Integer, String>> values = olapModel
					.getMembersDimension(realDim.keySet().iterator().next());

			values.keySet();

			DimensionWrapper dimPanel = new DimensionWrapper(dimName,
					values.keySet(), null);
			dimensions.add(dimPanel);
		}
		createTheStep();
	}

	protected void populateListslWithCubeValues(
			CubeSPARQLExplorer cubeSpExplorer) {
		// clear the panel if dimensions already exist
		if (dimensions != null && dimensions.size() > 0) {
			dimensionPanel.removeAll();
		}

		dimensions = new LinkedList<DimensionWrapper>();
		for (String dimUri : cubeSpExplorer.getSelectedRowsByUser()) {

			String dimName = dimUri;

			DimensionWrapper dimPanel = new DimensionWrapper(dimName,
					cubeSpExplorer.getAllMembers().get(dimUri).keySet(), null);
			dimensions.add(dimPanel);
		}
		for (String dimUri : cubeSpExplorer.getSelectedColumnsByUser()) {
			String dimName = dimUri;

			DimensionWrapper dimPanel = new DimensionWrapper(dimName,
					cubeSpExplorer.getAllMembers().get(dimUri).keySet(), null);
			dimensions.add(dimPanel);
		}
		createTheStep();
	}

	private void createTheStep() {
		GridBagConstraints c = new GridBagConstraints();
		int i = 0;
		for (DimensionWrapper pnl : dimensions) {
			c.gridx = 0;
			c.gridy = i;
			c.weightx = 0.0;
			c.weighty = 0.0;
			c.fill = GridBagConstraints.BOTH;
			dimensionPanel.add(pnl, c);
			i++;
		}
	}
}
