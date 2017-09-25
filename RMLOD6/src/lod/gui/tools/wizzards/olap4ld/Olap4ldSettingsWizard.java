package lod.gui.tools.wizzards.olap4ld;

import java.sql.SQLException;

import javax.swing.SwingWorker;

import lod.importers.OLAPDataImporter;
import lod.olap4ld.CubeSPARQLExplorer;
import lod.olap4ld.OLAPModel;
import lod.utils.MapUtils;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard;
import com.rapidminer.tools.io.Encoding;

/**
 * @author petar
 * 
 */
public class Olap4ldSettingsWizard extends AbstractWizard {

	public static final String MESSAGES_FAILED_TO_LOAD_DATASET = "Failed to load the dataset: ";

	public static final String MESSAGES_ENTER_URL = "You have to set a dataset URL!";
	public static final String MESSAGES_ENTER_ENDPOINT = "You have to set the SPARQL endpoint!";

	private final OLAPDataImporter importerOperator;

	SwingWorker worker;

	// keep the whole dataset information
	OLAPModel olapModel;

	CubeSPARQLExplorer cubeSpExplorer;

	boolean isSPARQLLoader = false;

	boolean canFinish = false;

	// steps
	ValuesSelectionStep valuesStep;

	public Olap4ldSettingsWizard(OLAPDataImporter importer, String key,
			Object... arguments) {
		super(RapidMinerGUI.getMainFrame(), key, arguments);
		this.importerOperator = importer;
		addSteps();
		layoutDefault(ButtonDialog.HUGE);
	}

	private void addSteps() {
		addStep(new DatasetSelectionStep(importerOperator) {
			@Override
			protected boolean performLeavingAction(WizardStepDirection direction) {
				// check if the user selected a dataset
				String datasetURI = getSelectedDataset();
				if (datasetURI == null || datasetURI.equals("")) {
					// SwingTools.showMessageDialog(key, keyArguments);
					SwingTools.showVerySimpleErrorMessage(MESSAGES_ENTER_URL,
							MESSAGES_ENTER_URL);
					return false;

				} else if (isSPARQL
						&& (getSPARQLEndpoint() == null || getSPARQLEndpoint()
								.equals("")))

				{
					SwingTools.showVerySimpleErrorMessage(
							MESSAGES_ENTER_ENDPOINT, MESSAGES_ENTER_ENDPOINT);
					return false;
				} else {

					if (isSPARQL) {
						isSPARQLLoader = true;
						cubeSpExplorer = new CubeSPARQLExplorer(
								getSelectedDataset(), getSPARQLEndpoint());
					} else {
						// set the dataset url
						olapModel = new OLAPModel(datasetURI);
					}
					// olapModel.setDatasetURL(datasetURI);
					return performLeavingAction();
				}

			}
		});

		addStep(new DimensionsSelectionStep() {

			@Override
			protected boolean performLeavingAction(WizardStepDirection direction) {
				if (direction.equals(direction.FORWARD)) {
					// check if all lists were populated
					String message = checkIfValid();
					if (message.equals("")) {
						if (isSPARQLLoader) {
							populateNewCubeSpValues(cubeSpExplorer);
						} else {
							populateNewOlapValues(olapModel);
						}
						valuesStep.setCanProoceedFlag(true);
						return super.performLeavingAction(direction);
					} else {
						SwingTools.showVerySimpleErrorMessage(message, message);
						return false;
					}
				} else {
					return super.performLeavingAction(direction);
				}

			}

			@Override
			protected boolean performEnteringAction(
					WizardStepDirection direction) {
				// if it is going forward, and the model was initialized, load
				// the complete dataset
				if (direction.name().equals("FORWARD")
						&& (olapModel != null || cubeSpExplorer != null)) {
					changeVisibilityOfLoadingScreen(true);
					clearLists();
					worker = new SwingWorker<String, Void>() {
						@Override
						public String doInBackground() {
							if (isSPARQLLoader) {
								try {
									cubeSpExplorer.loadDataset();
								} catch (Exception e) {
									SwingTools.showVerySimpleErrorMessage(
											e.getMessage(), e);
								}
								return "";

							} else {
								String datasetUri = olapModel.getDatasetURL();
								try {
									// init the model completely
									olapModel = new OLAPModel();
								} catch (SQLException e) {
									// TODO Auto-generated catch block
									SwingTools.showVerySimpleErrorMessage(
											e.getMessage(), e);
								}
								// load the dataset
								olapModel.loadDataset(datasetUri);
								return "";
							}
						}

						@Override
						public void done() {
							// Remove the "Loading images" label.
							changeVisibilityOfLoadingScreen(false);
							// loopslot = -1;
							try {
								// populate the Step lists
								if (isSPARQLLoader) {
									setCubeExplorer(cubeSpExplorer);
								} else {
									setOlapModel(olapModel);

								}
								populateAllLists();
							} catch (Exception e) {

								SwingTools.showVerySimpleErrorMessage(
										MESSAGES_FAILED_TO_LOAD_DATASET
												+ e.getMessage(),
										MESSAGES_FAILED_TO_LOAD_DATASET
												+ e.getMessage());
							}
						}
					};
					worker.execute();

				} else {
					changeVisibilityOfLoadingScreen(false);
				}
				return performEnteringAction();
			}

		});

		valuesStep = new ValuesSelectionStep() {
			@Override
			protected boolean performEnteringAction(
					WizardStepDirection direction) {

				if (isSPARQLLoader) {
					populateListslWithCubeValues(cubeSpExplorer);
				} else {
					populateTheListsWithOlapValues(olapModel);
				}
				return performEnteringAction();
			}

			@Override
			protected boolean performLeavingAction(WizardStepDirection direction) {
				// check if all lists were populated
				if (direction.equals(direction.FINISH)) {
					String message = "";
					if (isSPARQLLoader) {
						message = getSelectedAttributeSet(cubeSpExplorer);
					} else {
						message = getSelectedAttributeSet(olapModel);
					}
					if (message.equals("")) {
						importerOperator.setSPARQLexplorer(isSPARQLLoader);
						importerOperator.setParameter(
								OLAPDataImporter.PARAMETER_IS_SPARQL,
								Boolean.toString(isSPARQLLoader));
						if (isSPARQLLoader) {
							setOperatorSettings(cubeSpExplorer);
						} else {
							setOperatorSettings(olapModel);
						}
						return super.performLeavingAction(direction);
					} else {
						SwingTools.showVerySimpleErrorMessage(message, message);
						return false;
					}
				} else {
					setCanProoceedFlag(false);
					return super.performLeavingAction(direction);
				}
			}

			private void setOperatorSettings(OLAPModel olapModel) {
				importerOperator.setParameter(
						OLAPDataImporter.PARAMETER_SELECTED_URI,
						olapModel.getDatasetURL());
				importerOperator.setParameter(
						OLAPDataImporter.PARAMETER_SELECTED_ROWS, OLAPModel
								.convertListToString(olapModel
										.getSelectedRows()));
				importerOperator.setParameter(
						OLAPDataImporter.PARAMETER_SELECTED_COLS, OLAPModel
								.convertListToString(olapModel
										.getSelectedColumns()));
				importerOperator.setParameter(
						OLAPDataImporter.PARAMETER_SELECTED_MEASURES, OLAPModel
								.convertListToString(olapModel
										.getSelectedMeasures()));

				importerOperator.setParameter(
						OLAPDataImporter.PARAMETER_SELECTED_VALUES, OLAPModel
								.convertMapToString(olapModel
										.getSelectedValuesPerDimension()));
				importerOperator.setOlapModel(olapModel);
			}

			private void setOperatorSettings(CubeSPARQLExplorer cubeExplorer) {
				importerOperator.setParameter(
						OLAPDataImporter.PARAMETER_SELECTED_URI,
						cubeExplorer.getDatasetURI());

				importerOperator.setParameter(
						OLAPDataImporter.PARAMETER_ENDPOINT,
						cubeExplorer.getEndpoint());

				importerOperator.setParameter(
						OLAPDataImporter.PARAMETER_SELECTED_ROWS, OLAPModel
								.convertListToString(cubeExplorer
										.getSelectedRowsByUser()));
				importerOperator.setParameter(
						OLAPDataImporter.PARAMETER_SELECTED_COLS, OLAPModel
								.convertListToString(cubeExplorer
										.getSelectedColumnsByUser()));

				importerOperator
						.setParameter(
								OLAPDataImporter.PARAMETER_SELECTED_MEASURES,
								MapUtils.mapToString(cubeExplorer
										.getSelectedMeasures()));

				importerOperator.setParameter(
						OLAPDataImporter.PARAMETER_SELECTED_ROWS_VALUES,
						MapUtils.mapDoubleToString(cubeExplorer
								.getSelectedRows()));

				importerOperator.setParameter(
						OLAPDataImporter.PARAMETER_SELECTED_COLS_VALUES,
						MapUtils.mapDoubleToString(cubeExplorer
								.getSelectedColumns()));

				importerOperator.setCubeExplorer(cubeExplorer);
			}

		};

		addStep(valuesStep);

	}

	@Override
	protected void finish() {

		super.finish();
	}

	@Override
	protected void cancel() {
		if (worker != null)
			worker.cancel(true);
		super.cancel();
	}
}
