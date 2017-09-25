package lod.gui.tools.utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import lod.olap4ld.OLAPModel;

import com.rapidminer.gui.tools.ExtendedJList;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedListModel;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;

//THIS IS NOT USED *******************
/**
 * @author petar
 * 
 */
public class OLAPLoaderDialog extends JDialog implements ActionListener {
	private OLAPLoaderWizard parent;

	public OLAPLoaderDialog(OLAPLoaderWizard wizard) {
		parent = wizard;
		super.setTitle("Import OLAP dataset");
		createMainPanel();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 3082879618326993290L;

	// keep the whole dataset information
	OLAPModel olapModel;

	private JTextField inputDataset = new JTextField();
	JButton loadDataset = new JButton("Load Dataset");

	ExtendedListModel dimensionListModel;
	private ExtendedJList dimensionList;

	ExtendedListModel measuresListMode;
	private ExtendedJList measureList;

	ExtendedListModel selectedDimensionListModel;
	private ExtendedJList selectedDimensionList;

	ExtendedListModel selectedMeasuresListMode;
	private ExtendedJList selectedMeasureList;

	private JButton addValueButton;
	private JButton removeValueButton;

	private JButton addValueMButton;
	private JButton removeValueMButton;

	public static final int GAP = 6;

	protected static final Insets INSETS = new Insets(GAP, GAP, GAP, GAP);

	private static final String ADD_ICON_NAME = "add2.png";
	private static final String LEFT_ICON_NAME = "nav_left_green.png";
	private static final String RIGHT_ICON_NAME = "nav_right_green.png";
	private static final String UP_ICON_NAME = "nav_up_green.png";
	private static final String DOWN_ICON_NAME = "nav_down_green.png";

	private static Icon ADD_ICON;
	private static Icon LEFT_ICON;
	private static Icon RIGHT_ICON;
	private static Icon UP_ICON;
	private static Icon DOWN_ICON;

	static {
		ADD_ICON = SwingTools.createIcon("16/" + ADD_ICON_NAME);
		LEFT_ICON = SwingTools.createIcon("16/" + LEFT_ICON_NAME);
		RIGHT_ICON = SwingTools.createIcon("16/" + RIGHT_ICON_NAME);
		UP_ICON = SwingTools.createIcon("16/" + UP_ICON_NAME);
		DOWN_ICON = SwingTools.createIcon("16/" + DOWN_ICON_NAME);
	}

	private void createMainPanel() {
		JPanel panel = new JPanel(new BorderLayout());

		JPanel uriPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		// generate example datasets URIs

		JList exampleDSlist = new JList(getExampleSetUris());

		// add the load button
		loadDataset.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ae) {
				String datasetURI = inputDataset.getText();
				if (datasetURI != null && !datasetURI.equals("")) {
					// TODO throw exception to the user
					try {
						olapModel = new OLAPModel();
						olapModel.loadDataset(datasetURI);
						populateLists();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				} else {
					JOptionPane.showMessageDialog((Component) ae.getSource(),
							"You have to select at least one dimension!");
				}
			}

			private void populateLists() {
				// remove old elements
				dimensionListModel.clear();
				measuresListMode.clear();
				selectedDimensionListModel.clear();
				selectedMeasuresListMode.clear();
				// populateDimensions
				for (String name : olapModel.getDimensionEasyMap().keySet())
					dimensionListModel.addElement(name);

				// populate meassures
				for (String name : olapModel.getMeasureEasyMap().keySet())
					measuresListMode.addElement(name);
			}
		});

		JLabel uriLabel = new JLabel("Dataset URI");
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		uriLabel.setLabelFor(inputDataset);
		uriPanel.add(uriLabel, c);
		// add the text field

		JLabel exampleListLabel = new JLabel("Example Datasets:");
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		exampleListLabel.setLabelFor(exampleDSlist);
		uriPanel.add(exampleListLabel, c);

		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.BOTH;
		uriPanel.add(inputDataset, c);

		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.NONE;
		uriPanel.add(exampleDSlist, c);
		// add the button
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 1;
		c.weighty = 0;
		uriPanel.add(loadDataset, c);

		panel.add(uriPanel, BorderLayout.NORTH);

		JPanel selectionPanel = new JPanel(new GridBagLayout());

		dimensionListModel = new ExtendedListModel();
		dimensionList = new ExtendedJList(dimensionListModel);
		dimensionList.setLayoutOrientation(JList.VERTICAL);
		dimensionList.setEnabled(true);

		JScrollPane dimnensionListScrollPane = new ExtendedJScrollPane(
				dimensionList) {
			private static final long serialVersionUID = 8474453689364798720L;

			@Override
			public Dimension getPreferredSize() {
				return new Dimension((int) (getWidth() * 0.25d),
						super.getPreferredSize().height);
			}
		};
		dimnensionListScrollPane.setEnabled(true);

		selectedDimensionListModel = new ExtendedListModel();
		selectedDimensionList = new ExtendedJList(selectedDimensionListModel);
		selectedDimensionList.setLayoutOrientation(JList.VERTICAL);
		selectedDimensionList.setEnabled(true);

		JScrollPane selectedDimnensionListScrollPane = new ExtendedJScrollPane(
				selectedDimensionList) {
			private static final long serialVersionUID = 8474453689364798720L;

			@Override
			public Dimension getPreferredSize() {
				return new Dimension((int) (getWidth() * 0.25d),
						super.getPreferredSize().height);
			}
		};
		selectedDimnensionListScrollPane.setEnabled(true);

		measuresListMode = new ExtendedListModel();
		measureList = new ExtendedJList(measuresListMode);
		measureList.setLayoutOrientation(JList.VERTICAL);
		measureList.setEnabled(true);

		JScrollPane measureListScrollPane = new ExtendedJScrollPane(measureList) {
			private static final long serialVersionUID = 8474453689364798720L;

			@Override
			public Dimension getPreferredSize() {
				return new Dimension((int) (getWidth() * 0.25d),
						super.getPreferredSize().height);
			}
		};
		measureListScrollPane.setEnabled(true);
		selectedMeasuresListMode = new ExtendedListModel();
		selectedMeasureList = new ExtendedJList(selectedMeasuresListMode);
		selectedMeasureList.setLayoutOrientation(JList.VERTICAL);
		selectedMeasureList.setEnabled(true);

		JScrollPane selectedMeasureListScrollPane = new ExtendedJScrollPane(
				selectedMeasureList) {
			private static final long serialVersionUID = 8474453689364798720L;

			@Override
			public Dimension getPreferredSize() {
				return new Dimension((int) (getWidth() * 0.25d),
						super.getPreferredSize().height);
			}
		};
		selectedMeasureListScrollPane.setEnabled(true);

		JPanel valueSelectionButtonsPanel = new JPanel(new BorderLayout());

		addValueButton = new JButton(RIGHT_ICON);

		addValueButton
				.setToolTipText("Select dimension from list of available values.");
		addValueButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Object[] selectedValues = dimensionList.getSelectedValues();
				for (int i = 0; i < selectedValues.length; i++) {
					dimensionListModel.removeElement(selectedValues[i]);
					selectedDimensionListModel.addElement(selectedValues[i]);
				}
			}
		});
		addValueButton.setEnabled(true);
		removeValueButton = new JButton(LEFT_ICON);

		removeValueButton.setToolTipText("Remove value from selection.");
		removeValueButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Object[] selectedValues = selectedDimensionList
						.getSelectedValues();
				for (int i = 0; i < selectedValues.length; i++) {
					dimensionListModel.addElement(selectedValues[i]);
					selectedDimensionListModel.removeElement(selectedValues[i]);
				}
			}
		});
		removeValueButton.setEnabled(true);

		valueSelectionButtonsPanel.add(addValueButton, BorderLayout.CENTER);
		valueSelectionButtonsPanel.add(removeValueButton, BorderLayout.SOUTH);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		dimnensionListScrollPane.setBorder(createTitledBorder("Dimensions"));
		selectionPanel.add(dimnensionListScrollPane, c);

		c.insets = new Insets(0, 0, 0, GAP);
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 0.0;
		c.gridheight = 2;
		c.fill = GridBagConstraints.NONE;
		selectionPanel.add(valueSelectionButtonsPanel, c);

		c.gridx = 2;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		selectedDimnensionListScrollPane
				.setBorder(createTitledBorder("Selected Dimensions"));
		selectionPanel.add(selectedDimnensionListScrollPane, c);

		// add the second selection panel

		JPanel selectionPanel2 = new JPanel(new GridBagLayout());

		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		measureListScrollPane.setBorder(createTitledBorder("Measures"));
		selectionPanel2.add(measureListScrollPane, c);

		c.insets = new Insets(0, 0, 0, GAP);
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 0.0;
		c.gridheight = 2;
		c.fill = GridBagConstraints.NONE;
		selectionPanel2.add(addSecondButtonPanel(), c);

		c.gridx = 2;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		selectedMeasureListScrollPane
				.setBorder(createTitledBorder("Selected Measures"));
		selectionPanel2.add(selectedMeasureListScrollPane, c);

		JPanel completeSelectionPanel = new JPanel(new BorderLayout());
		completeSelectionPanel.add(selectionPanel, BorderLayout.NORTH);
		completeSelectionPanel.add(selectionPanel2, BorderLayout.SOUTH);
		panel.add(completeSelectionPanel, BorderLayout.CENTER);

		// addd the OK panel
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okButton = new JButton(new ResourceAction("ok") {

			private static final long serialVersionUID = -5102786702723664410L;

			public void actionPerformed(ActionEvent e) {
				ok();
			}

			/**
			 * saves the olap model and disposes the dialog
			 * 
			 */

		});
		buttonPanel.add(okButton);
		JButton cancelButton = makeCancelButton("cancel");
		buttonPanel.add(cancelButton);
		getRootPane().setDefaultButton(okButton);

		panel.add(buttonPanel, BorderLayout.PAGE_END);
		this.add(panel, BorderLayout.NORTH);
	}

	/**
	 * set the model
	 * 
	 */
	private void ok() {
		if (olapModel == null) {
			JOptionPane.showMessageDialog(this,
					"Error generating the OLAP model!");

		} else if (selectedDimensionListModel.size() == 0) {
			JOptionPane.showMessageDialog(this,
					"You have to select at least one dimension!");

		} else if (selectedMeasuresListMode.size() == 0) {
			JOptionPane.showMessageDialog(this,
					"You have to select at least one meassure!");

		} else {
			populateNewOlapValues();
			parent.onResultDelivered(olapModel);
			dispose();
		}

	}

	/**
	 * sets the values of the olapModel, so it can be used directly in the olap
	 * importer
	 * 
	 */
	private void populateNewOlapValues() {

		List<String> newDimenssions = new LinkedList<String>();
		List<String> newMeasures = new LinkedList<String>();
		for (int i = 0; i < selectedDimensionListModel.size(); i++) {
			String strS = (String) selectedDimensionListModel.get(i);
			newDimenssions.add(strS);
		}

		for (int i = 0; i < selectedMeasuresListMode.size(); i++) {
			String strS = (String) selectedMeasuresListMode.get(i);
			newMeasures.add(strS);
		}
		olapModel.setCurrentPlan(olapModel.getPlanForDimensions(newDimenssions,
				newMeasures, null));
	}

	/**
	 * Dispose the dialog
	 * 
	 * @param i18nKey
	 * @return
	 */
	protected JButton makeCancelButton(String i18nKey) {
		Action cancelAction = new ResourceAction(i18nKey) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				olapModel = null;
				dispose();
			}
		};
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CANCEL");
		getRootPane().getActionMap().put("CANCEL", cancelAction);
		return new JButton(cancelAction);
	}

	public static TitledBorder createTitledBorder(String title) {
		TitledBorder border = new TitledBorder(createBorder(), title) {
			private static final long serialVersionUID = 3113821577644055057L;

			@Override
			public void paintBorder(Component c, Graphics g, int x, int y,
					int width, int height) {
				super.paintBorder(c, g, x - EDGE_SPACING, y, width + 2
						* EDGE_SPACING, height);
			}
		};
		return border;
	}

	public static Border createBorder() {
		return BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY);
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
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub

	}

	/**
	 * Populates the second set of buttons
	 * 
	 * @return
	 */
	public JPanel addSecondButtonPanel() {
		JPanel valueSelectionButtonsPanel1 = new JPanel(new BorderLayout());
		addValueMButton = new JButton(RIGHT_ICON);

		addValueMButton
				.setToolTipText("Select measure from list of available values.");
		addValueMButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Object[] selectedValues = measureList.getSelectedValues();
				for (int i = 0; i < selectedValues.length; i++) {
					measuresListMode.removeElement(selectedValues[i]);
					selectedMeasuresListMode.addElement(selectedValues[i]);
				}
			}
		});
		addValueMButton.setEnabled(true);

		removeValueMButton = new JButton(LEFT_ICON);
		removeValueMButton.setToolTipText("Remove value from selection.");
		removeValueMButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Object[] selectedValues = selectedMeasureList
						.getSelectedValues();
				for (int i = 0; i < selectedValues.length; i++) {
					measuresListMode.addElement(selectedValues[i]);
					selectedMeasuresListMode.removeElement(selectedValues[i]);
				}
			}
		});
		removeValueMButton.setEnabled(true);

		valueSelectionButtonsPanel1.add(addValueMButton, BorderLayout.CENTER);
		valueSelectionButtonsPanel1.add(removeValueMButton, BorderLayout.SOUTH);

		return valueSelectionButtonsPanel1;
	}

}
