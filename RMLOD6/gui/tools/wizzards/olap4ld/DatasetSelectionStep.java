package lod.gui.tools.wizzards.olap4ld;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import lod.importers.OLAPDataImporter;

import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.parameter.UndefinedParameterError;

public class DatasetSelectionStep extends WizardStep {

	private JPanel corePanel = new JPanel();
	private String[] datasetURIs = new String[] { "", "Test URI value 1" };

	public DatasetSelectionStep(OLAPDataImporter operatorProps) {
		super("olap4ldgui.dataselector");

		setGUI(operatorProps);
	}

	private JTextField inputDatasetField;
	private JTable table;

	private JTextField endpointField;
	JLabel lblNewLabel;

	public boolean isSPARQL = false;

	private void setGUI(OLAPDataImporter operatorProps) {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		JLabel lblNewLabel = new JLabel("Dataset URL:");
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.weighty = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		panel.add(lblNewLabel, c);

		lblNewLabel.setLabelFor(inputDatasetField);

		inputDatasetField = new JTextField();
		inputDatasetField.setHorizontalAlignment(SwingConstants.LEFT);
		String oldValue = null;
		try {
			oldValue = operatorProps
					.getParameterAsString(OLAPDataImporter.PARAMETER_SELECTED_URI);
		} catch (UndefinedParameterError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (oldValue != null && !oldValue.equals(""))
			inputDatasetField.setText(oldValue);
		panel.add(inputDatasetField, c);
		inputDatasetField.setToolTipText("Enter the dataset URL");
		inputDatasetField.setColumns(50);

		panel.add(new JLabel(" "), c);
		panel.add(new JSeparator(SwingConstants.HORIZONTAL), c);
		panel.add(new JLabel(" "), c);
		JPanel panel_1 = new JPanel();
		// corePanel.add(panel_1, BorderLayout.CENTER);

		table = new JTable();
		table.setCellSelectionEnabled(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setToolTipText("Example Dataset URLs");
		table.setModel(new DefaultTableModel(
				new Object[][] {
						{
								"Revenue, discount and more for orders of products by customers in Example SSB dataset",
								"http://olap4ld.googlecode.com/git/OLAP4LD-trunk/tests/ssb001/ttl/example.ttl#ds" },
						{
								"Gross Domestic Product (GDP) per capita in Purchasing Power Standards (PPS) per Country from Eurostat",
								"http://olap4ld.googlecode.com/git/OLAP4LD-trunk/tests/estatwrap/tec00114_ds.rdf#ds" },
						{
								"Gross Domestic Product (GDP) per capita in Purchasing Power Standards (PPS) per Country from Estatwrap",
								"http://estatwrap.ontologycentral.com/id/tec00114#ds" },
						{
								"Employment rate, by sex from Eurostat",
								"http://olap4ld.googlecode.com/git/OLAP4LD-trunk/tests/estatwrap/tsdec420_ds.rdf#ds" },
						{ "Employment rate, by sex from Estatwrap",
								"http://estatwrap.ontologycentral.com/id/tsdec420#ds" },
						{
								"Real GDP growth rate from Eurostat",
								"http://olap4ld.googlecode.com/git/OLAP4LD-trunk/tests/estatwrap/tsieb020_ds.rdf#ds" },
						{
								"Stock Market Values on 2012-12-12 for Bank of America Corporation from Yahoo! Finance Wrap",
								"http://yahoofinancewrap.appspot.com/archive/BAC/2012-12-12#ds" },
						{
								"Historical HCO3 climate data at location AD0514 from SMART DB Wrap",
								"http://smartdbwrap.appspot.com/id/locationdataset/AD0514/HCO3" }, },
				new String[] { "Dataset Description", "Dataset URI" }) {
			Class[] columnTypes = new Class[] { String.class, Object.class };

			public Class getColumnClass(int columnIndex) {
				return columnTypes[columnIndex];
			}
		});
		table.getColumnModel().getColumn(0).setPreferredWidth(500);
		table.getColumnModel().getColumn(1).setPreferredWidth(500);

		JLabel lblNewLabel_1 = new JLabel("Example Datasets URLs:");
		lblNewLabel_1.setLabelFor(table);
		panel_1.add(lblNewLabel_1, BorderLayout.NORTH);
		panel_1.add(table, BorderLayout.CENTER);

		getSPARQLpanel(panel, c, operatorProps);
		corePanel.add(panel, BorderLayout.CENTER);

	}

	/**
	 * generates the sparql optional panel
	 * 
	 * @return
	 */
	private JPanel getSPARQLpanel(JPanel panel, GridBagConstraints c,
			OLAPDataImporter operatorProps) {

		JCheckBox box = new JCheckBox(
				"Use SPARQL Endpoint (Recommended when using large datasets)");
		ActionListener actionListener = new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				AbstractButton abstractButton = (AbstractButton) actionEvent
						.getSource();
				boolean selected = abstractButton.getModel().isSelected();

				lblNewLabel.setVisible(selected);
				endpointField.setVisible(selected);

				isSPARQL = selected;

				// abstractButton.setText(newLabel);
			}
		};

		String oldSparql = null;

		box.addActionListener(actionListener);
		panel.add(box, c);
		lblNewLabel = new JLabel("SPARQL Endpoint:");

		panel.add(lblNewLabel, c);
		lblNewLabel.setLabelFor(endpointField);
		lblNewLabel.setVisible(false);

		endpointField = new JTextField();

		panel.add(endpointField, c);
		endpointField.setToolTipText("Enter the SPARQL endpoint URL!");
		endpointField.setColumns(40);
		endpointField.setVisible(false);

		// check if the old parameters are already set

		boolean oldIsSelected = false;
		try {
			oldIsSelected = Boolean
					.parseBoolean(operatorProps
							.getParameterAsString(OLAPDataImporter.PARAMETER_IS_SPARQL));
			oldSparql = operatorProps
					.getParameterAsString(OLAPDataImporter.PARAMETER_ENDPOINT);
		} catch (UndefinedParameterError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (oldIsSelected && oldSparql != null && !oldSparql.equals("")) {
			box.setSelected(true);
			lblNewLabel.setVisible(true);
			endpointField.setVisible(true);
			isSPARQL = true;
			endpointField.setText(oldSparql);

		}

		return panel;
	}

	/**
	 * populates the list of example uris
	 * 
	 * @return
	 */
	private ListModel getExampleSetUris() {
		final DefaultListModel model = new DefaultListModel();
		model.addElement("http://olap4ld.googlecode.com/git/OLAP4LD-trunk/tests/ssb001/ttl/example.ttl#ds");
		model.addElement("http://olap4ld.googlecode.com/git/OLAP4LD-trunk/tests/estatwrap/tec00114_ds.rdf#ds");
		model.addElement("http://estatwrap.ontologycentral.com/id/tec00114#ds");
		model.addElement("http://olap4ld.googlecode.com/git/OLAP4LD-trunk/tests/estatwrap/tsdec420_ds.rdf#ds");
		model.addElement("http://estatwrap.ontologycentral.com/id/tsdec420#ds");
		return model;
	}

	@Override
	protected boolean performEnteringAction() {
		// TODO Auto-generated method stub
		return super.performEnteringAction();
	}

	@Override
	protected boolean performLeavingAction() {
		// TODO Auto-generated method stub
		return super.performLeavingAction();
	}

	@Override
	protected JComponent getComponent() {
		// TODO Auto-generated method stub
		return corePanel;
	}

	@Override
	protected boolean canProceed() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	protected boolean canGoBack() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean performEnteringAction(WizardStepDirection direction) {

		return performEnteringAction();
	}

	// @Override
	// protected boolean performLeavingAction(WizardStepDirection direction) {
	//
	// String datasetURI = inputDatasetField.getText();
	// if (datasetURI == null || datasetURI.equals("")) {
	// // SwingTools.showMessageDialog(key, keyArguments);
	// SwingTools.showVerySimpleErrorMessage(
	// "You have to set a dataset URL!",
	// "You have to set a dataset URL!");
	// return false;
	// // JOptionPane.showMessageDialog((Component) this.getSource(),
	// // "You have to select at least one dimension!");
	// } else {
	// // loadingPanel.setVisible(true);
	// // Thread t = new Thread() {
	// //
	// // public void run() {
	// // try {
	// // String datasetURI = inputDatasetField.getText();
	// // olapModel = new OLAPModel();
	// // olapModel.loadDataset(datasetURI);
	// // // populateLists();
	// //
	// // } catch (Exception e) {
	// // // TODO fix this
	// // SwingTools
	// // .showVerySimpleErrorMessage(e.getMessage(), e);
	// // return;
	// // }
	// //
	// // }
	// // };
	// // t.start();
	// // try {
	// // t.join();
	// // } catch (InterruptedException e) {
	// // SwingTools.showVerySimpleErrorMessage(e.getMessage(), e);
	// //
	// // }
	// olapModel = new OLAPModel(datasetURI);
	// // olapModel.setDatasetURL(datasetURI);
	// return performLeavingAction();
	// }
	//
	// }

	public String getSelectedDataset() {
		return inputDatasetField.getText().trim();
	}

	public String getSPARQLEndpoint() {
		return endpointField.getText().trim();
	}
}
