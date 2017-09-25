package lod.gui.tools.utils;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import lod.importers.OLAPDataImporter;
import lod.olap4ld.OLAPModel;

import com.rapidminer.gui.wizards.AbstractConfigurationWizardCreator;
import com.rapidminer.gui.wizards.ConfigurationListener;
import com.rapidminer.parameter.ParameterType;
//THIS IS NOT USED
/**
 * A creator class that is used to create and show
 * {@link OntologySelectionWizard} interface, and to pass its result as a
 * perameter value to the calling operator.
 * 
 * @author petar
 */
public class OLAPLoaderWizard extends AbstractConfigurationWizardCreator {
	OLAPDataImporter sourceOperator = null;

	@Override
	public String getI18NKey() {
		// TODO Auto-generated method stub
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
		OLAPLoaderDialog dialog = new OLAPLoaderDialog(this);
		dialog.setSize(9 * width / 15, 2 * height / 3);
		dialog.setLocationRelativeTo(null);
		dialog.setResizable(true);
		dialog.setVisible(true);
		dialog.setVisible(true);

	}

	// called to set the final model
	public void onResultDelivered(OLAPModel value) {

		sourceOperator.setOlapModel(value);
	}
}
