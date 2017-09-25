package lod.gui.tools.wizzards.olap4ld;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import com.rapidminer.gui.tools.SwingTools;

public class DimensionWrapper extends JPanel {

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

	private String dimensionName;
	private Set<String> attributes;
	private List<String> selectedAttributes;

	private JLabel dimLabel;
	private JLabel listFullHeader = new JLabel("Values");
	private JLabel listSelectedHeader = new JLabel("Selected values");
	private JList rawAttributeSet;
	private JList selectedAttributeSet;
	private JButton moveTo = new JButton(RIGHT_ICON);
	private JButton moveFrom = new JButton(LEFT_ICON);
	private JButton moveUp = new JButton(UP_ICON);
	private JButton moveDown = new JButton(DOWN_ICON);

	public DimensionWrapper(String dimensionName, Set<String> attributes,
			List<String> selectedAttributes) {
		super(new GridBagLayout());
		this.dimensionName = dimensionName;
		this.attributes = attributes;
		this.selectedAttributes = selectedAttributes;

		dimLabel = new JLabel(dimensionName);
		DefaultListModel mdl = new DefaultListModel();
		for (String attr : attributes) {
			mdl.addElement(attr);
		}

		rawAttributeSet = new JList(mdl);

		DefaultListModel mdlSel = new DefaultListModel();
		if (selectedAttributes != null) {
			for (String attr : selectedAttributes) {
				mdlSel.addElement(attr);
			}
		}
		selectedAttributeSet = new JList(mdlSel);

		moveTo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DimensionsSelectionStep.pushEl(rawAttributeSet,
						selectedAttributeSet);
			}
		});

		moveFrom.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DimensionsSelectionStep.pushEl(selectedAttributeSet,
						rawAttributeSet);
			}
		});

		moveUp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DimensionsSelectionStep.moveUpDown(selectedAttributeSet, 0);
			}
		});

		moveDown.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DimensionsSelectionStep.moveUpDown(selectedAttributeSet, 1);
			}
		});

		initGUI();
	}

	private void initGUI() {

		GridBagConstraints c = new GridBagConstraints();
		// Place Dimension name
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		this.add(dimLabel, c);

		// Raw measures
		JPanel dims = new JPanel();
		dims.setLayout(new BoxLayout(dims, BoxLayout.Y_AXIS));
		JScrollPane listScrollerMeasures = new JScrollPane(rawAttributeSet);
		listScrollerMeasures.setPreferredSize(new Dimension(400, 160));
		listScrollerMeasures
				.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		dims.add(listFullHeader);
		dims.add(listScrollerMeasures);
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		this.add(dims, c);

		// Buttons
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
		buttons.add(moveTo);
		buttons.add(moveFrom);
		c.gridx = 2;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 0.0;
		this.add(buttons, c);

		// Selected items
		JPanel selected = new JPanel();
		selected.setLayout(new BoxLayout(selected, BoxLayout.Y_AXIS));
		JScrollPane listScrollerSelectedMeasures = new JScrollPane(
				selectedAttributeSet);
		listScrollerSelectedMeasures.setPreferredSize(new Dimension(400, 160));
		listScrollerSelectedMeasures
				.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		selected.add(listSelectedHeader);
		selected.add(listScrollerSelectedMeasures);
		c.gridx = 3;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 0.0;
		this.add(selected, c);

		// up down buttons
		JPanel btPanelUpDownMeasures = new JPanel();
		btPanelUpDownMeasures.setLayout(new BoxLayout(btPanelUpDownMeasures,
				BoxLayout.Y_AXIS));
		btPanelUpDownMeasures.add(moveUp);
		btPanelUpDownMeasures.add(moveDown);
		c.gridx = 4;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 0.0;
		this.add(btPanelUpDownMeasures, c);
	}

	/**
	 * Returns a set of attribute after selection
	 * 
	 * @return
	 */
	public List<String> getSelectedAttributes() {
		List<String> result = new ArrayList<String>();
		DefaultListModel mdl = (DefaultListModel) selectedAttributeSet
				.getModel();
		int size = mdl.getSize();
		for (int i = 0; i < size; i++) {
			result.add((String) mdl.getElementAt(i));
		}
		return result;
	}

	public void setSelectedAttributes(List<String> selectedAttributes) {
		this.selectedAttributes = selectedAttributes;
	}

	public Set<String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Set<String> attributes) {
		this.attributes = attributes;
	}

	public String getDimensionName() {
		return dimensionName;
	}

	public void setDimensionName(String dimensionName) {
		this.dimensionName = dimensionName;
	}

}
