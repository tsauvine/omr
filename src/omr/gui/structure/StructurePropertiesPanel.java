package omr.gui.structure;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Observable;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import omr.Project;
import omr.Sheet;
import omr.gui.SheetEditor;
import omr.gui.UndoSupport;

/**
 * A panel where the properties of the sheet structure can be edited.
 * 
 * @author Tapio Auvinen
 */
public class StructurePropertiesPanel extends JPanel implements ItemListener {
    private static final long serialVersionUID = 1L;
    
    protected UndoSupport undoSupport;
    
    private Project project;
    private SheetEditor sheetEditor; 
    //private AttributeEdit currentEdit;                // Current undo object. Changes are accumulated until another component is selected. 
    
    private SheetComboModel sheetComboModel;
    private JComboBox referenceCombo;
    private JComboBox rotationCombo;
    
    public StructurePropertiesPanel(SheetEditor sheetEditor) {
        this.sheetEditor = sheetEditor;
    	
    	this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Structure properties"));
        
        // GridBagLayout
        this.setLayout(new GridBagLayout());
        GridBagConstraints left = new GridBagConstraints();
        GridBagConstraints right = new GridBagConstraints();
        GridBagConstraints wide = new GridBagConstraints();
        left.gridx = 0; left.anchor = GridBagConstraints.LINE_END;
        right.gridx = 1; right.anchor = GridBagConstraints.LINE_START;
        wide.gridwidth = 2; wide.fill = GridBagConstraints.HORIZONTAL;


        // Reference sheet
        wide.gridy = 0;
        this.add(new JLabel("Reference sheet:"), wide);
        
        wide.gridy++;
        sheetComboModel = new SheetComboModel();
        referenceCombo = new JComboBox(sheetComboModel);
        this.add(referenceCombo, wide);
        //referenceCombo.addActionListener(this);
        referenceCombo.addItemListener(this);
        
        // Rotation
        wide.gridy++;
        this.add(new JLabel("Rotation:"), wide);
        
        wide.gridy++;
        Integer[] rotationStrings = { 0, 90, 180, 270 };
        rotationCombo = new JComboBox(rotationStrings);
        rotationCombo.addItemListener(this);
        this.add(rotationCombo, wide);
    }
    
    public void setUndoSupport(UndoSupport undo) {
        this.undoSupport = undo;
    }
    
    /**
     * Sets the sheet structure to be edited.
     */
    public void setProject(Project project) {
        this.project = project;
        this.sheetComboModel.setProject(project);
        
        Sheet referenceSheet = project.getSheetStructure().getReferenceSheet();
		if (referenceSheet != null) {
			rotationCombo.setSelectedItem(referenceSheet.getRotation());
		}
    }
    
    
    public void actionPerformed(ActionEvent event) {
    }
    
    public void itemStateChanged(ItemEvent event) {
        
    	if (event.getSource() == referenceCombo && event.getStateChange() == ItemEvent.SELECTED) {
    		// New selection in reference sheet combo
    	    Sheet selectedSheet = (Sheet)event.getItem();
    	    
    	    // FIXME: Try to filter unnecessary events from this combo.
    	    
    		// Set new reference sheet
    	    selectedSheet.setRotation((Integer)rotationCombo.getSelectedItem());
    	    sheetEditor.setSheet(selectedSheet);  // Set view first so that buffer will be cached
    	    project.getSheetStructure().setReferenceSheet(selectedSheet);
    	}
    	else if (event.getSource() == rotationCombo && event.getStateChange() == ItemEvent.SELECTED) {
    	    // Rotation changed
    		Sheet referenceSheet = project.getSheetStructure().getReferenceSheet();
    	    int rotation = (Integer)event.getItem();
    		if (referenceSheet != null) {
    			// Rotate reference sheet
    			referenceSheet.setRotation(rotation);
    		}
    		
    		// Rotate all sheets
    		project.getSheetsContainer().setRotation(rotation);
    		
    		sheetEditor.updateBuffer();
    	}
    }
    
    public void fetchSheetList() {
    	// Update sheet list
    	referenceCombo.removeAllItems();
    	
    	for (Sheet sheet : project.getAnswerSheets()) {
    		referenceCombo.addItem(sheet);
    	}
    }
    
    /**
     * Notified by the SheetsContainer when the sheet list is updated.
     */
    public void update(Observable source, Object arg1) {
    	fetchSheetList();
    }
    
}
