package lod.gui.tools.wizzards.olap4ld;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import lod.importers.OLAPDataImporter;
import lod.olap4ld.OLAPModel;

import com.rapidminer.gui.wizards.AbstractConfigurationWizardCreator;
import com.rapidminer.gui.wizards.ConfigurationListener;
import com.rapidminer.parameter.ParameterType;

public class Olap4ldSettingsWizardCreator extends
		AbstractConfigurationWizardCreator {

	OLAPDataImporter sourceOperator = null;

	@Override
	public String getI18NKey() {
		return "olapLoader";
	}

	@Override
	public void createConfigurationWizard(ParameterType type,
			ConfigurationListener listener) {
		sourceOperator = (OLAPDataImporter) listener;

		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getDefaultScreenDevice();
		int width = gd.getDisplayMode().getWidth();
		int height = gd.getDisplayMode().getHeight();

		Olap4ldSettingsWizard wizard = new Olap4ldSettingsWizard(
				sourceOperator, getI18NKey());
		// wizard.setSize(9 * width / 15, 2 * height / 2);
		wizard.setLocationRelativeTo(null);
		wizard.setResizable(true);
		wizard.setVisible(true);
	}

	// called to set the final model
	public void onResultDelivered(OLAPModel value) {
		sourceOperator.setOlapModel(value);
	}

}
