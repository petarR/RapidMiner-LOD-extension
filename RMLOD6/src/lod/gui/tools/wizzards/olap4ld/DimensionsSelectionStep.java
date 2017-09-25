package lod.gui.tools.wizzards.olap4ld;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import lod.olap4ld.CubeSPARQLExplorer;
import lod.olap4ld.OLAPModel;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.tools.Tools;

/**
 * @author petar
 * @author Eugene
 */
public class DimensionsSelectionStep extends WizardStep {

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

	private final JPanel rootPanel = new JPanel(new GridBagLayout());
	private JList initialDimensionsList;
	private JList initialMeasuresList;
	private JList selectedRowDimensionsList;
	private JList selectedColumnDimensionsList;
	private JList selectedMeasures;
	private JButton btFromDimsToRowDims = new JButton(RIGHT_ICON);
	private JButton btFromRowDimsToDims = new JButton(LEFT_ICON);
	private JButton btRowDimsUp = new JButton(UP_ICON);
	private JButton btRowDimsDown = new JButton(DOWN_ICON);

	private JButton btFromDimsToColDims = new JButton(RIGHT_ICON);
	private JButton btFromColDimsTowDims = new JButton(LEFT_ICON);
	private JButton btColsDimsUp = new JButton(UP_ICON);
	private JButton btColsDimsDown = new JButton(DOWN_ICON);

	private JButton btFromMeasuresToSelMeasures = new JButton(RIGHT_ICON);
	private JButton btFromSelMeasuresToMeasures = new JButton(LEFT_ICON);
	private JButton btMeasuresUp = new JButton(UP_ICON);
	private JButton btMeasuresDown = new JButton(DOWN_ICON);

	OLAPModel olapModelLocal;
	CubeSPARQLExplorer cubeExplorer;

	public void setCubeExplorer(CubeSPARQLExplorer cubeSpExplorer) {
		this.cubeExplorer = cubeSpExplorer;
	}

	public void setOlapModel(OLAPModel olapModel) {
		this.olapModelLocal = olapModel;
	}

	private JPanel loadingPanel;

	private boolean canContinue = false;

	private JPanel panelMeasures;
	private JPanel panelDims;

	public DimensionsSelectionStep() {
		super("olap4ldgui.dimselectorone");

		panelDims = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		// Raw dimensions
		JPanel rawDimPanel = new JPanel();
		rawDimPanel.setLayout(new BoxLayout(rawDimPanel, BoxLayout.Y_AXIS));
		JLabel labelDims = new JLabel("Dimensions");
		initialDimensionsList = new JList(new DefaultListModel());
		JScrollPane listScroller = new JScrollPane(initialDimensionsList);
		listScroller.setPreferredSize(new Dimension(350, 160));
		rawDimPanel.add(labelDims);
		rawDimPanel.add(listScroller);

		// Manipulating buttons
		JPanel btPanel = new JPanel();
		btPanel.setLayout(new BoxLayout(btPanel, BoxLayout.Y_AXIS));
		btPanel.add(btFromDimsToRowDims);
		btPanel.add(btFromRowDimsToDims);
		btPanel.add(Box.createRigidArea(new Dimension(0, 18)));
		btPanel.add(btFromDimsToColDims);
		btPanel.add(btFromColDimsTowDims);
		// up down buttons
		JPanel btPanelUpDownDims = new JPanel();
		btPanelUpDownDims.setLayout(new BoxLayout(btPanelUpDownDims,
				BoxLayout.Y_AXIS));
		btPanelUpDownDims.add(btRowDimsUp);
		btPanelUpDownDims.add(btRowDimsDown);
		btPanelUpDownDims.add(Box.createRigidArea(new Dimension(0, 18)));
		btPanelUpDownDims.add(btColsDimsUp);
		btPanelUpDownDims.add(btColsDimsDown);

		// Selected dims
		JPanel selDimPanel = new JPanel();
		selDimPanel.setLayout(new BoxLayout(selDimPanel, BoxLayout.Y_AXIS));
		JLabel RowDims = new JLabel("Row dimensions");
		selectedRowDimensionsList = new JList(new DefaultListModel());
		JScrollPane listScrollerRows = new JScrollPane(
				selectedRowDimensionsList);
		listScrollerRows.setPreferredSize(new Dimension(350, 70));
		JLabel ColDims = new JLabel("Column dimensions");
		selectedColumnDimensionsList = new JList(new DefaultListModel());
		JScrollPane listScrollerCols = new JScrollPane(
				selectedColumnDimensionsList);
		listScrollerCols.setPreferredSize(new Dimension(350, 70));
		selDimPanel.add(RowDims);
		selDimPanel.add(listScrollerRows);
		selDimPanel.add(ColDims);
		selDimPanel.add(listScrollerCols);

		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		panelDims.add(rawDimPanel);

		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 0.0;
		panelDims.add(btPanel);

		c.gridx = 2;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 0.0;
		panelDims.add(selDimPanel);

		c.gridx = 3;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 0.0;
		panelDims.add(btPanelUpDownDims);

		// Raw measures
		panelMeasures = new JPanel(new GridBagLayout());

		JPanel rawMeasuresPanel = new JPanel();
		rawMeasuresPanel.setLayout(new BoxLayout(rawMeasuresPanel,
				BoxLayout.Y_AXIS));
		JLabel labelMess = new JLabel("Measures");
		initialMeasuresList = new JList(new DefaultListModel());
		JScrollPane listScrollerMeasures = new JScrollPane(initialMeasuresList);
		listScrollerMeasures.setPreferredSize(new Dimension(350, 160));
		rawMeasuresPanel.add(labelMess);
		rawMeasuresPanel.add(listScrollerMeasures);

		// Buttons
		JPanel btMesPanel = new JPanel();
		btMesPanel.setLayout(new BoxLayout(btMesPanel, BoxLayout.Y_AXIS));
		btMesPanel.add(btFromMeasuresToSelMeasures);
		btMesPanel.add(btFromSelMeasuresToMeasures);

		// up down buttons
		JPanel btPanelUpDownMeasures = new JPanel();
		btPanelUpDownMeasures.setLayout(new BoxLayout(btPanelUpDownMeasures,
				BoxLayout.Y_AXIS));
		btPanelUpDownMeasures.add(btMeasuresUp);
		btPanelUpDownMeasures.add(btMeasuresDown);

		// Selected measures
		JPanel selMeasuresPanel = new JPanel();
		selMeasuresPanel.setLayout(new BoxLayout(selMeasuresPanel,
				BoxLayout.Y_AXIS));
		JLabel labelSelMess = new JLabel("Selected Measures");
		selectedMeasures = new JList(new DefaultListModel());
		JScrollPane listScrollerMeasuresSel = new JScrollPane(selectedMeasures);
		listScrollerMeasuresSel.setPreferredSize(new Dimension(350, 160));
		selMeasuresPanel.add(labelSelMess);
		selMeasuresPanel.add(listScrollerMeasuresSel);

		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 0.0;
		panelMeasures.add(rawMeasuresPanel);

		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 0.0;
		panelMeasures.add(btMesPanel);

		c.gridx = 2;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 0.0;
		panelMeasures.add(selMeasuresPanel);

		c.gridx = 3;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 0.0;
		panelMeasures.add(btPanelUpDownMeasures);

		// set animation
		loadingPanel = new JPanel();
		// ImageIcon loading = new ImageIcon(
		// "../RapidMiner_Unuk/resources/com/rapidminer/resources/icons/48/hourglass.png");
		ImageIcon loading = new ImageIcon(
				Tools.getResource("icons/48/hourglass.png"));

		loadingPanel.add(new JLabel("loading... ", loading, JLabel.CENTER),
				BorderLayout.CENTER);

		// Packing all UI together
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0.0;
		c.weighty = 1.0;
		rootPanel.add(panelDims, c);

		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0.0;
		c.weighty = 1.0;
		rootPanel.add(panelMeasures, c);

		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 0.0;
		c.weighty = 1.0;
		rootPanel.add(loadingPanel, c);

		// Initializing buttons
		initControls();

	}

	private void initControls() {
		btFromDimsToRowDims.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pushEl(initialDimensionsList, selectedRowDimensionsList);
			}
		});
		btFromDimsToColDims.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pushEl(initialDimensionsList, selectedColumnDimensionsList);
			}
		});
		btFromRowDimsToDims.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pushEl(selectedRowDimensionsList, initialDimensionsList);
			}
		});
		btFromColDimsTowDims.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pushEl(selectedColumnDimensionsList, initialDimensionsList);
			}
		});
		// set up down rowdims
		btRowDimsUp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				moveUpDown(selectedRowDimensionsList, 0);
			}
		});

		btRowDimsDown.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				moveUpDown(selectedRowDimensionsList, 1);
			}
		});

		// up down colDims

		btColsDimsUp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				moveUpDown(selectedColumnDimensionsList, 0);
			}
		});

		btColsDimsDown.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				moveUpDown(selectedColumnDimensionsList, 1);
			}
		});

		btFromMeasuresToSelMeasures.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pushEl(initialMeasuresList, selectedMeasures);
			}
		});
		btFromSelMeasuresToMeasures.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pushEl(selectedMeasures, initialMeasuresList);
			}
		});

		// up down colDims

		btMeasuresUp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				moveUpDown(selectedMeasures, 0);
			}
		});

		btMeasuresDown.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				moveUpDown(selectedMeasures, 1);
			}
		});

	}

	public static void pushEl(JList listFrom, JList listTo) {
		if (listFrom.getSelectedIndex() != -1) {

			Object[] vals = listFrom.getSelectedValues();

			DefaultListModel dmlTo = (DefaultListModel) listTo.getModel();
			for (Object obj : vals) {

				dmlTo.addElement(obj.toString());
			}

			DefaultListModel dmlFrom = (DefaultListModel) listFrom.getModel();

			for (Object str : vals)
				dmlFrom.removeElement(str);
		}
	}

	public static void moveUpDown(JList listFrom, int direction) {
		if (listFrom.getSelectedIndex() != -1) {
			DefaultListModel dmlFrom = (DefaultListModel) listFrom.getModel();
			String str = listFrom.getSelectedValue().toString();
			int index = listFrom.getSelectedIndex();

			if (dmlFrom.size() > 1) {

				switch (direction) {

				// move up
				case 0:
					if (index > 0) {
						dmlFrom.removeElement(listFrom.getSelectedValue());
						dmlFrom.add(index - 1, str);
					}
					break;
				// move down
				default:
					if (index < dmlFrom.size() - 1) {
						dmlFrom.removeElement(listFrom.getSelectedValue());
						dmlFrom.add(index + 1, str);
					}
					break;
				}
			}

		}
	}

	// private DefaultListModel getListContent() {
	// final DefaultListModel model = new DefaultListModel();
	// model.addElement("Dim1");
	// model.addElement("Dim2");
	// model.addElement("Dim3");
	// model.addElement("Dim4");
	// model.addElement("Dim5");
	// return model;
	// }

	/**
	 * populates the dimensions from the OlapModel
	 * 
	 * @return
	 */
	private void populateDimensions() {
		final DefaultListModel model = new DefaultListModel();
		// populateDimensions
		if (cubeExplorer == null) {
			for (String name : olapModelLocal.getDimensionEasyMap().keySet())
				model.addElement(name);
		} else {
			for (String name : cubeExplorer.getAllDimensions().keySet()) {
				model.addElement(name);
			}
		}
		initialDimensionsList.setModel(model);
	}

	/**
	 * populates the meassures
	 * 
	 * @return
	 */
	private void populateMeassures() {
		final DefaultListModel model = new DefaultListModel();
		// populate meassures
		if (cubeExplorer == null) {
			for (String name : olapModelLocal.getMeasureEasyMap().keySet())
				model.addElement(name);
		} else {
			for (String name : cubeExplorer.getAllMeasures().keySet()) {
				model.addElement(name);
			}
		}
		initialMeasuresList.setModel(model);

	}

	@Override
	protected JComponent getComponent() {
		return rootPanel;
	}

	@Override
	protected boolean canProceed() {
		return true;
	}

	@Override
	protected boolean canGoBack() {
		return true;
	}

	/**
	 * used to populate all lists in the Step
	 */
	public void populateAllLists() {
		populateDimensions();
		populateMeassures();
		selectedColumnDimensionsList.setModel(new DefaultListModel());
		selectedRowDimensionsList.setModel(new DefaultListModel());
		selectedMeasures.setModel(new DefaultListModel());
		canContinue = true;
	}

	public void changeVisibilityOfLoadingScreen(boolean visible) {
		loadingPanel.setVisible(visible);
		panelMeasures.setVisible(!visible);
		panelDims.setVisible(!visible);
	}

	/**
	 * clears the lists
	 */
	public void clearLists() {
		clearList(selectedColumnDimensionsList);
		clearList(selectedRowDimensionsList);
		clearList(selectedMeasures);
		clearList(initialDimensionsList);
		clearList(initialMeasuresList);

	}

	private void clearList(JList list) {
		DefaultListModel listModel = (DefaultListModel) list.getModel();
		listModel.removeAllElements();
	}

	/**
	 * checks if all lists are populated
	 * 
	 * @return
	 */
	public String checkIfValid() {
		String message = "";
		if (selectedColumnDimensionsList.getModel().getSize() == 0) {
			message = "You have to select at least one column dimension!";

		}
		if (selectedRowDimensionsList.getModel().getSize() == 0) {
			message = "You have to select at least one row dimension!";

		}
		if (selectedMeasures.getModel().getSize() == 0) {
			message = "You have to select at least one meassure!";

		}
		return message;
	}

	/**
	 * sets the values of the olapModel, so it can be used directly in the olap
	 * importer
	 * 
	 */
	public void populateNewOlapValues(OLAPModel olapModel) {

		List<String> nRows = new LinkedList<String>();
		List<String> nColumns = new LinkedList<String>();
		List<String> newMeasures = new LinkedList<String>();
		for (int i = 0; i < selectedRowDimensionsList.getModel().getSize(); i++) {
			String strS = (String) selectedRowDimensionsList.getModel()
					.getElementAt(i);
			nRows.add(strS);
		}
		for (int i = 0; i < selectedColumnDimensionsList.getModel().getSize(); i++) {
			String strS = (String) selectedColumnDimensionsList.getModel()
					.getElementAt(i);
			nColumns.add(strS);
		}

		for (int i = 0; i < selectedMeasures.getModel().getSize(); i++) {
			String strS = (String) selectedMeasures.getModel().getElementAt(i);
			newMeasures.add(strS);
		}

		olapModel.setCurrentPlan(olapModel.getPlanForDimensions(nRows,
				nColumns, newMeasures));
	}

	/**
	 * sets the values of the olapModel, so it can be used directly in the olap
	 * importer
	 * 
	 */
	public void populateNewCubeSpValues(CubeSPARQLExplorer cubeExplorer) {

		List<String> nRows = new LinkedList<String>();
		List<String> nColumns = new LinkedList<String>();
		Map<String, String> newMeasures = new LinkedHashMap<String, String>();
		for (int i = 0; i < selectedRowDimensionsList.getModel().getSize(); i++) {
			String strS = (String) selectedRowDimensionsList.getModel()
					.getElementAt(i);
			nRows.add(strS);
		}
		cubeExplorer.setSelectedRowsByUser(nRows);

		for (int i = 0; i < selectedColumnDimensionsList.getModel().getSize(); i++) {
			String strS = (String) selectedColumnDimensionsList.getModel()
					.getElementAt(i);
			nColumns.add(strS);
		}
		cubeExplorer.setSelectedColumnsByUser(nColumns);

		for (int i = 0; i < selectedMeasures.getModel().getSize(); i++) {
			String strS = (String) selectedMeasures.getModel().getElementAt(i);
			newMeasures.put(strS, cubeExplorer.getAllMeasures().get(strS));
		}
		cubeExplorer.setSelectedMeasures(newMeasures);

		cubeExplorer.populateMembers();

	}
}
