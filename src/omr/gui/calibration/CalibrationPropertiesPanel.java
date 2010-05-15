package omr.gui.calibration;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EtchedBorder;

import omr.Project;
import omr.Sheet;
import omr.Project.ThresholdingStrategy;
import omr.gui.SheetEditor;
import omr.gui.UndoSupport;

public class CalibrationPropertiesPanel extends JPanel implements ActionListener, ItemListener {
   private static final long serialVersionUID = 1L;
    
    protected UndoSupport undoSupport;
    
    private Project project;
    private HistogramComponent histogramComponent;
    private SheetEditor sheetEditor;
    
    private JComboBox rotationCombo;
    private JRadioButton globalThresholding;
    private JRadioButton perSheetThresholding;
    
    
    public CalibrationPropertiesPanel(SheetEditor sheetEditor, HistogramComponent histogramComponent) {
        this.histogramComponent = histogramComponent;
        this.sheetEditor = sheetEditor;
        
        this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Settings"));
        
        // GridBagLayout
        this.setLayout(new GridBagLayout());
        GridBagConstraints left = new GridBagConstraints();
        GridBagConstraints right = new GridBagConstraints();
        GridBagConstraints wide = new GridBagConstraints();
        left.gridx = 0; left.anchor = GridBagConstraints.LINE_END;
        right.gridx = 1; right.anchor = GridBagConstraints.LINE_START;
        wide.gridwidth = 2; wide.fill = GridBagConstraints.HORIZONTAL;

        // Rotation
        wide.gridy = 0;
        this.add(new JLabel("Rotation"), wide);
        
        wide.gridy++;
        Integer[] rotationStrings = { 0, 90, 180, 270 };
        rotationCombo = new JComboBox(rotationStrings);
        rotationCombo.addItemListener(this);
        this.add(rotationCombo, wide);
        
        // Thresholding
        wide.gridy++;
        this.add(new JLabel("Thresholding"), wide);
        
        wide.gridy++;
        globalThresholding = new JRadioButton("Global");
        globalThresholding.setMnemonic(KeyEvent.VK_M);
        globalThresholding.setActionCommand("manual");
        globalThresholding.setSelected(true);
        globalThresholding.addActionListener(this);
        this.add(globalThresholding, wide);
        
        wide.gridy++;
        perSheetThresholding = new JRadioButton("Per sheet");
        perSheetThresholding.setMnemonic(KeyEvent.VK_A);
        perSheetThresholding.setActionCommand("auto");
        perSheetThresholding.setSelected(false);
        perSheetThresholding.addActionListener(this);
        this.add(perSheetThresholding, wide);
        
        // Group the buttons
        ButtonGroup group = new ButtonGroup();
        group.add(globalThresholding);
        group.add(perSheetThresholding);
    }
    
    public void setUndoSupport(UndoSupport undo) {
        this.undoSupport = undo;
    }
    
    /**
     * Sets the sheet structure to be edited.
     */
    public void setProject(Project project) {
        this.project = project;
        
        if (project.getThresholdingStrategy() == ThresholdingStrategy.PER_SHEET) {
            perSheetThresholding.setSelected(true);
            globalThresholding.setSelected(false);
        } else {
            perSheetThresholding.setSelected(false);
            globalThresholding.setSelected(true);
        }
    }
    
    
    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        
        if (source == globalThresholding) {
            project.setThresholdingStrategy(ThresholdingStrategy.GLOBAL);
            histogramComponent.setHistogram(project.getHistogram());
        } else if (source == perSheetThresholding) {
            project.setThresholdingStrategy(ThresholdingStrategy.PER_SHEET);
            Sheet activeSheet = sheetEditor.getSheet();
            if (activeSheet != null) {
            	histogramComponent.setHistogram(sheetEditor.getSheet().getHistogram());
            }
        }
        
        // Recalculate answers
        project.calculateAnswers();
    }
    
    public void itemStateChanged(ItemEvent event) {
		if (event.getSource() == rotationCombo && event.getStateChange() == ItemEvent.SELECTED) {
			project.getSheetsContainer().setRotation((Integer)event.getItem());
			
			// FIXME: Need to recalculate the whole thing
			sheetEditor.updateBuffer();
	    }
    }
}
