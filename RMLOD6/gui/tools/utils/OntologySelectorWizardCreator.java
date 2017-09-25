package lod.gui.tools.utils;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import lod.linking.LookupLinker;

import com.rapidminer.gui.wizards.AbstractConfigurationWizardCreator;
import com.rapidminer.gui.wizards.ConfigurationListener;
import com.rapidminer.parameter.ParameterType;

/**
 * A creator class that is used to create and show {@link OntologySelectionWizard} interface,
 * and to pass its result as a perameter value to the calling operator.
 * @author Evgeny Mitichkin
 *
 */
public class OntologySelectorWizardCreator extends AbstractConfigurationWizardCreator{

	private static OntologySelectionWizard wizard = null;
	private String selectionResult = "";
	LookupLinker sourceOperator = null;
	
	@Override
	public String getI18NKey() {
		return "ontology_selection_wizard";
	}

	@Override
	public void createConfigurationWizard(ParameterType type,
			ConfigurationListener listener) {
		sourceOperator = (LookupLinker) listener;
		
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		int width = gd.getDisplayMode().getWidth();
		int height = gd.getDisplayMode().getHeight();
		
		OntologySelectionWizard wizard = new OntologySelectionWizard(this);
		wizard.setName("Ontology Selection Wizard");
		wizard.setSize(9*width/20, 2*height/3);
		wizard.setLocationRelativeTo(null);		
		wizard.setResizable(true);		
		wizard.setVisible(true);			
	}
	
	public void onResultDelivered(String value) {
		selectionResult = value;
		sourceOperator.getParameters().setParameter("Query Class", value);
	}
}
