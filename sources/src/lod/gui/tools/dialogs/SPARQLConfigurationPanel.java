package lod.gui.tools.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.config.gui.ConfigurationPanel;

public class SPARQLConfigurationPanel extends
		ConfigurationPanel<SPARQLConfigurable> {
	private JLabel nameFieldLabel = new JLabel();
	private JTextField nameField = new JTextField();
	private JLabel useLocalFileCboxLabel = new JLabel();
	private JComboBox<String> runnerType = new JComboBox<String>();
	// private JCheckBox useLocalFileCbox = new JCheckBox();
	private JLabel useCountLabel = new JLabel();
	private JCheckBox useCount = new JCheckBox();
	private JLabel uusePropertyPathsLabel = new JLabel();
	private JCheckBox usePropertyPaths = new JCheckBox();
	private JLabel sparqlEndpointLabel = new JLabel();
	private JTextField sparqlEndpoint = new JTextField();
	private JLabel sparqlAnnotationLabel = new JLabel();
	private JTextField sparqlAnnotation = new JTextField();
	private JLabel sparqlRetriesLabel = new JLabel();
	private JTextField sparqlRetries = new JTextField();
	private JLabel sparqlTimeoutLabel = new JLabel();
	private JTextField sparqlTimeout = new JTextField();
	private JLabel sparqlPageSizeLabel = new JLabel();
	private JTextField sparqlPageSize = new JTextField();

	private JLabel localFileNameLabel = new JLabel();
	private JButton localFileChooserButton = new JButton("Choose File");
	private JTextField localFileName = new JTextField();
	private JFileChooser localFileNameChooser = new JFileChooser();
	private JLabel localSchemaFileNameLabel = new JLabel();
	private JTextField localSchemaFileName = new JTextField();
	private JButton localSchemaFileChooserButton = new JButton("Choose File");
	private JFileChooser localSchemaFileNameChooser = new JFileChooser();

	private JLabel reasonerLabel = new JLabel("Choose reasoner");
	private JComboBox<String> reasonerComboBox = new JComboBox<String>();
	Component parent;

	@Override
	public JComponent getComponent() {
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.weighty = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;

		JPanel panel = new JPanel(new GridBagLayout());
		nameFieldLabel.setText("Name:");
		panel.add(nameFieldLabel, c);
		panel.add(nameField, c);

		JPanel chooseLocalPanel = new JPanel(new BorderLayout());
		useLocalFileCboxLabel.setText("Set the model type:");
		useLocalFileCboxLabel.setLabelFor(runnerType);
		// set type of runners
		runnerType.addItem("SPARQL endpoint");
		runnerType.addItem("Use local file");
		// runnerType.addItem("Use URL data");
		runnerType.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				changeComponentsState(runnerType.getSelectedIndex());
			}
		});
		;

		chooseLocalPanel.add(useLocalFileCboxLabel, BorderLayout.WEST);
		chooseLocalPanel.add(runnerType, BorderLayout.CENTER);
		panel.add(chooseLocalPanel, c);

		sparqlAnnotationLabel.setText("SPARQL annotation:");
		panel.add(sparqlAnnotationLabel, c);
		sparqlAnnotation.setText("DBpedia");
		panel.add(sparqlAnnotation, c);

		JPanel useCountPanel = new JPanel(new BorderLayout());
		useCountLabel.setText("Endpoint supports COUNT");
		useCountLabel.setLabelFor(useCount);
		useCountPanel.add(useCount, BorderLayout.WEST);
		useCountPanel.add(useCountLabel, BorderLayout.CENTER);
		panel.add(useCountPanel, c);

		JPanel usePropertyPathPanel = new JPanel(new BorderLayout());
		uusePropertyPathsLabel.setText("Endpoint supports Property Paths");
		uusePropertyPathsLabel.setLabelFor(usePropertyPaths);
		usePropertyPathPanel.add(usePropertyPaths, BorderLayout.WEST);
		usePropertyPathPanel.add(uusePropertyPathsLabel, BorderLayout.CENTER);
		panel.add(usePropertyPathPanel, c);

		panel.add(new JLabel(" "), c);
		panel.add(new JSeparator(SwingConstants.HORIZONTAL), c);
		panel.add(new JLabel(" "), c);

		sparqlEndpointLabel.setText("SPARQL endpoint:");
		panel.add(sparqlEndpointLabel, c);
		sparqlEndpoint.setText("http://dbpedia.org/sparql/");
		panel.add(sparqlEndpoint, c);

		sparqlRetriesLabel.setText("SPARQL query retries:");
		panel.add(sparqlRetriesLabel, c);
		sparqlRetries.setText("10");
		panel.add(sparqlRetries, c);

		sparqlTimeoutLabel.setText("SPARQL query timeout:");
		panel.add(sparqlTimeoutLabel, c);
		sparqlTimeout.setText("60000");
		panel.add(sparqlTimeout, c);

		sparqlPageSizeLabel.setText("SPARQL result page size:");
		panel.add(sparqlPageSizeLabel, c);
		sparqlPageSize.setText("0");
		panel.add(sparqlPageSize, c);

		panel.add(new JLabel(" "), c);
		panel.add(new JSeparator(SwingConstants.HORIZONTAL), c);
		panel.add(new JLabel(" "), c);

		localFileNameLabel.setText("Instance file:");
		panel.add(localFileNameLabel, c);

		localFileChooserButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				int returnVal = localFileNameChooser
						.showOpenDialog(new SPARQLConfigurationPanel()
								.getComponent());
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					localFileName.setText(localFileNameChooser
							.getSelectedFile().getAbsolutePath());
				}
			}
		});

		JPanel instanceFilePanel = new JPanel(new BorderLayout());
		instanceFilePanel.add(localFileName, BorderLayout.CENTER);
		instanceFilePanel.add(localFileChooserButton, BorderLayout.EAST);
		panel.add(instanceFilePanel, c);

		localFileNameLabel.setEnabled(false);
		localFileName.setEnabled(false);
		localFileChooserButton.setEnabled(false);

		localSchemaFileNameLabel.setText("Schema file:");
		panel.add(localSchemaFileNameLabel, c);
		localSchemaFileChooserButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				int returnVal = localSchemaFileNameChooser
						.showOpenDialog(new SPARQLConfigurationPanel()
								.getComponent());
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					localSchemaFileName.setText(localSchemaFileNameChooser
							.getSelectedFile().getAbsolutePath());
				}
			}
		});
		JPanel schemaFilePanel = new JPanel(new BorderLayout());
		schemaFilePanel.add(localSchemaFileName, BorderLayout.CENTER);
		schemaFilePanel.add(localSchemaFileChooserButton, BorderLayout.EAST);
		panel.add(schemaFilePanel, c);

		localSchemaFileChooserButton.setEnabled(false);

		localSchemaFileNameLabel.setEnabled(false);
		localSchemaFileName.setEnabled(false);

		panel.add(reasonerLabel, c);
		reasonerComboBox.addItem("None");
		reasonerComboBox.addItem("OWL Micro Reasoner");
		reasonerComboBox.addItem("OWL Mini Reasoner");
		reasonerComboBox.addItem("OWL Reasoner");
		reasonerComboBox.addItem("RDFS Reasoner");
		panel.add(reasonerComboBox, c);
		reasonerLabel.setEnabled(false);
		reasonerComboBox.setEnabled(false);

		c.weighty = 1;
		panel.add(new JPanel(), c);

		return panel;
	}

	@Override
	public void updateComponents(SPARQLConfigurable configurable) {
		// TODO Auto-generated method stub
		nameField.setText(configurable.getName());
		sparqlEndpoint.setText(configurable
				.getParameter(SPARQLConfigurator.PARAMETER_ENDPOINT));
		sparqlAnnotation.setText(configurable
				.getParameter(SPARQLConfigurator.PARAMETER_LINK_ANNOTATION));
		sparqlPageSize.setText(configurable
				.getParameter(SPARQLConfigurator.PARAMETER_PAGE_SIZE));
		sparqlRetries.setText(configurable
				.getParameter(SPARQLConfigurator.PARAMETER_RETRIES));
		sparqlTimeout.setText(configurable
				.getParameter(SPARQLConfigurator.PARAMETER_TIMEOUT));
		// decide what query runner do we have
		// 0=endpoint
		// 1=file based
		// 2=url based
		int runnerTypeSelection = 0;
		try {
			runnerTypeSelection = Integer.parseInt(configurable
					.getParameter(SPARQLConfigurator.RUNNER_TYPE));
		} catch (Exception e) {

		}
		runnerType.setSelectedIndex(runnerTypeSelection);

		boolean isChecked = Boolean.parseBoolean(configurable
				.getParameter(SPARQLConfigurator.USE_COUNT));
		useCount.setSelected(isChecked);

		isChecked = Boolean.parseBoolean(configurable
				.getParameter(SPARQLConfigurator.USE_PROPERTY_PATHS));
		usePropertyPaths.setSelected(isChecked);

		localFileName.setText(configurable
				.getParameter(SPARQLConfigurator.LOCAL_ENDPOINT_FILE));

		localSchemaFileName.setText(configurable
				.getParameter(SPARQLConfigurator.LOCAL_ENDPOINT_SCHEMA_FILE));
		if (configurable.getParameter(SPARQLConfigurator.LOCAL_REASONER) != null
				&& configurable.getParameter(SPARQLConfigurator.LOCAL_REASONER)
						.length() > 0)
			reasonerComboBox.setSelectedIndex(Integer.parseInt(configurable
					.getParameter(SPARQLConfigurator.LOCAL_REASONER)));
		else
			reasonerComboBox.setSelectedIndex(0);

		changeComponentsState(runnerTypeSelection);

	}

	private void changeComponentsState(int selectedState) {

		switch (selectedState) {
		case 0:
			// turn on the endpoint selection
			changeComponentsStateBoolean(false);

			break;
		case 1:
			// turn on the local file
			changeComponentsStateBoolean(true);
			break;
		default:
			// turn off everything is URL based type
			sparqlEndpoint.setEnabled(false);
			sparqlPageSize.setEnabled(false);
			sparqlRetries.setEnabled(false);
			sparqlTimeout.setEnabled(false);
			sparqlEndpointLabel.setEnabled(false);
			sparqlPageSizeLabel.setEnabled(false);
			sparqlRetriesLabel.setEnabled(false);
			sparqlTimeoutLabel.setEnabled(false);

			localSchemaFileName.setEnabled(false);
			localFileName.setEnabled(false);
			localSchemaFileNameLabel.setEnabled(false);
			localFileNameLabel.setEnabled(false);
			reasonerLabel.setEnabled(false);
			reasonerComboBox.setEnabled(false);
			localFileChooserButton.setEnabled(false);
			localSchemaFileChooserButton.setEnabled(false);
			break;
		}

	}

	private void changeComponentsStateBoolean(boolean isChecked) {
		sparqlEndpoint.setEnabled(!isChecked);
		sparqlPageSize.setEnabled(!isChecked);
		sparqlRetries.setEnabled(!isChecked);
		sparqlTimeout.setEnabled(!isChecked);
		sparqlEndpointLabel.setEnabled(!isChecked);
		sparqlPageSizeLabel.setEnabled(!isChecked);
		sparqlRetriesLabel.setEnabled(!isChecked);
		sparqlTimeoutLabel.setEnabled(!isChecked);

		localSchemaFileName.setEnabled(isChecked);
		localFileName.setEnabled(isChecked);
		localSchemaFileNameLabel.setEnabled(isChecked);
		localFileNameLabel.setEnabled(isChecked);
		reasonerLabel.setEnabled(isChecked);
		reasonerComboBox.setEnabled(isChecked);
		localFileChooserButton.setEnabled(isChecked);
		localSchemaFileChooserButton.setEnabled(isChecked);
	}

	private void resetComponents() {
		nameField.setText("");

		runnerType.setSelectedIndex(0);
		useCount.setSelected(false);
		usePropertyPaths.setSelected(false);

		sparqlEndpoint.setText("http://dbpedia.org/sparql/");

		sparqlAnnotation.setText("DBpedia");

		sparqlRetries.setText("10");

		sparqlTimeout.setText("60000");

		sparqlPageSize.setText("0");

		localFileNameLabel.setEnabled(false);
		localFileName.setEnabled(false);
		localFileName.setText("");

		localSchemaFileNameLabel.setEnabled(false);
		localSchemaFileName.setEnabled(false);
		localSchemaFileName.setText("");
		reasonerComboBox.setSelectedIndex(0);
		reasonerLabel.setEnabled(false);
		reasonerComboBox.setEnabled(false);
	}

	@Override
	public void updateConfigurable(SPARQLConfigurable configurable) {
		// TODO Auto-generated method stub

		configurable.setName(nameField.getText());
		configurable.setParameter(SPARQLConfigurator.PARAMETER_ENDPOINT,
				sparqlEndpoint.getText());
		configurable.setParameter(SPARQLConfigurator.PARAMETER_LINK_ANNOTATION,
				sparqlAnnotation.getText());
		configurable.setParameter(SPARQLConfigurator.PARAMETER_PAGE_SIZE,
				sparqlPageSize.getText());
		configurable.setParameter(SPARQLConfigurator.PARAMETER_RETRIES,
				sparqlRetries.getText());
		configurable.setParameter(SPARQLConfigurator.PARAMETER_TIMEOUT,
				sparqlTimeout.getText());

		configurable.setParameter(SPARQLConfigurator.RUNNER_TYPE,
				Integer.toString(runnerType.getSelectedIndex()));

		configurable.setParameter(SPARQLConfigurator.USE_COUNT,
				Boolean.toString(useCount.isSelected()));

		configurable.setParameter(SPARQLConfigurator.USE_PROPERTY_PATHS,
				Boolean.toString(usePropertyPaths.isSelected()));

		configurable.setParameter(SPARQLConfigurator.LOCAL_ENDPOINT_FILE,
				localFileName.getText());

		configurable.setParameter(
				SPARQLConfigurator.LOCAL_ENDPOINT_SCHEMA_FILE, "");

		if (localSchemaFileName.getText() != null
				&& localSchemaFileName.getText().length() > 0)
			configurable.setParameter(
					SPARQLConfigurator.LOCAL_ENDPOINT_SCHEMA_FILE,
					localSchemaFileName.getText());

		String comboValue = Integer.toString(reasonerComboBox
				.getSelectedIndex());
		configurable.setParameter(SPARQLConfigurator.LOCAL_REASONER,
				Integer.toString(reasonerComboBox.getSelectedIndex()));

	}

	@Override
	public boolean checkFields() {

		String key = "";
		if (nameField.getText() == null || nameField.getText().length() < 1) {
			key = "Name";
		} else if (sparqlAnnotation.getText() == null
				|| sparqlAnnotation.getText().length() < 1) {
			key = SPARQLConfigurator.PARAMETER_LINK_ANNOTATION;
		} else if (runnerType.getSelectedIndex() == 0) {
			if (sparqlEndpoint.getText() == null
					|| sparqlEndpoint.getText().length() < 1)
				key = SPARQLConfigurator.PARAMETER_ENDPOINT;

			if (sparqlPageSize.getText() == null
					|| sparqlPageSize.getText().length() < 1)
				key = SPARQLConfigurator.PARAMETER_PAGE_SIZE;
			try {
				int page = Integer.parseInt(sparqlPageSize.getText());
			} catch (Exception e) {
				key = SPARQLConfigurator.PARAMETER_PAGE_SIZE;
			}
			if (sparqlRetries.getText() == null
					|| sparqlRetries.getText().length() < 1)
				key = SPARQLConfigurator.PARAMETER_RETRIES;
			try {
				int page = Integer.parseInt(sparqlRetries.getText());
			} catch (Exception e) {
				key = SPARQLConfigurator.PARAMETER_RETRIES;
			}
			if (sparqlTimeout.getText() == null
					|| sparqlTimeout.getText().length() < 1)
				key = SPARQLConfigurator.PARAMETER_TIMEOUT;
			try {
				int page = Integer.parseInt(sparqlTimeout.getText());
			} catch (Exception e) {
				key = SPARQLConfigurator.PARAMETER_TIMEOUT;
			}
		} else {
			if (localFileName.getText() == null
					|| localFileName.getText().length() < 1)
				key = SPARQLConfigurator.LOCAL_ENDPOINT_FILE;
			// if (localSchemaFileName.getText() == null
			// || localSchemaFileName.getText().length() < 1)
			// return false;
		}
		if (!key.equals("")) {
			SwingTools.showVerySimpleErrorMessage(
					"configuration.dialog.missing", key);
		}
		return true;
	}
}
