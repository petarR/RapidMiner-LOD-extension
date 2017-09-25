package lod.gui.tools.utils;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.tools.Tools;

/**
 * A tree view UI used to select the ontology
 * @author Evgeny Mitichkin
 *
 */
public class OntologySelectionWizard extends JDialog implements ActionListener{
	
	OntologySelectorWizardCreator mCreator = null;
	DefaultTreeModel mTreeModel = null;
	JTree treeView = null;
	JButton btConfirm = null;
	JButton btCancel = null;
	
	public OntologySelectionWizard(OntologySelectorWizardCreator creator) {		
		
		this.mCreator = creator;
		
		try {
			mTreeModel = getOntologyTreeModel();
		} catch (IOException e) { 
			System.out.println("Error reading the ontology"); 
		}		
		
		initUI();
	}
	
	private void initUI() {
		//Adding title
		super.setTitle("Ontology Selection Wizard");
			
		//Adding scrollable tree viewer
		treeView = new JTree(mTreeModel);
		treeView.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		ExtendedJScrollPane viewPane = new ExtendedJScrollPane(treeView);
		viewPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		this.add(viewPane);
				
		//Adding bottom panel with buttons
		JPanel panel = new JPanel();
		btConfirm = new JButton("Confirm");
		btConfirm.addActionListener(this);
		btCancel = new JButton("Cancel");
		btCancel.addActionListener(this);
		panel.add(btConfirm, BorderLayout.WEST);
		panel.add(btCancel, BorderLayout.EAST);
		this.add(panel, BorderLayout.SOUTH);		
	}
	
	private boolean hasSubClass(OntClass obj)
	{
		boolean result = false;
		if (obj.listSubClasses().hasNext())
			result = true;		
		return result;
	}
	
	private boolean hasSuperClass(OntClass obj)
	{
		boolean result = false;
		if (obj.listSuperClasses().hasNext())
			result = true;		
		return result;
	}
	
	private DefaultMutableTreeNode getTreeNode(OntClass obj)
	{
		DefaultMutableTreeNode result = null;
		boolean subClassEx = hasSubClass(obj);
		if (!subClassEx)
		{
			result = new DefaultMutableTreeNode(obj.getLocalName().toString());
		}
		else
		{
			DefaultMutableTreeNode topNode = new DefaultMutableTreeNode(obj.getLocalName().toString());
			List<OntClass> ontClassList = obj.listSubClasses(true).toList();
			Collections.sort(ontClassList, new Comparator<OntClass>() {

				@Override
				public int compare(OntClass o1, OntClass o2) {
					return o1.getLocalName().toString().compareToIgnoreCase(o2.getLocalName().toString());
				}
				
			});
			for (OntClass clazz : ontClassList)
			{
				DefaultMutableTreeNode mNode = getTreeNode(clazz);
				topNode.add(mNode);
			}
			result = topNode;
		}
		return result;
	}
	
	private DefaultTreeModel getOntologyTreeModel() throws IOException
	{
		String ontologyLocationInJar = "ontology/classes_dbpedia_3.9.owl";			
        //create the reasoning model using the base
        OntModel inf = ModelFactory.createOntologyModel();
        // use the FileManager to find the input file
        InputStream in = Tools.getResource(ontologyLocationInJar).openStream();
        if (in == null) {
            throw new IllegalArgumentException("Ontology file not found");
        }
        inf.read(in, "");	        
        
        ExtendedIterator<OntClass> classes = inf.listClasses(); 
        OntClass objj = classes.next();
        while (hasSuperClass(objj))
        {
        	objj = objj.getSuperClass();
        }
        
        DefaultMutableTreeNode ontologyTree = getTreeNode(objj);
		DefaultTreeModel treeModel = new DefaultTreeModel(ontologyTree);
        return treeModel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btCancel) {
			this.dispose();
		}
		if (e.getSource() == btConfirm) {
			TreePath mPath = treeView.getSelectionPath();
			if (mPath != null) {
				String selection = mPath.getLastPathComponent().toString();
				
				Object[] options = {"Yes", "No"};
				int n = JOptionPane.showOptionDialog(this, "You have chosen:\n " + " "+selection, "Please confirm class selection", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
				if (n==0) {
					passValue(selection);
				}
			} else {
				JOptionPane.showMessageDialog(this, "No concept was selected!");
			}
		}		
	}
	
	private void passValue(String value) {
		mCreator.onResultDelivered(value);
		this.dispose();
	}
}
