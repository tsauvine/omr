package omr.gui.structure;


import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import omr.QuestionGroup;
import omr.QuestionGroup.Orientation;
import omr.gui.QuestionGroupComponent;
import omr.gui.UndoSupport;

/**
 * A panel where the properties of a question group can be edited.
 * 
 * @author Tapio Auvinen
 */
public class QuestionGroupPropertiesPanel extends JPanel implements Scrollable, ChangeListener, ActionListener, ItemListener {
    private static final long serialVersionUID = 1L;
    
    protected UndoSupport undoSupport;
    
    private QuestionGroupComponent selectedComponent; 
    private AttributeEdit currentEdit;                // Current undo object. Changes are accumulated until another component is selected. 
    
    private JSpinner rowsSpinner; 
    private JSpinner colsSpinner;
    private JSpinner bubbleWidthSpinner; 
    private JSpinner bubbleHeightSpinner;
    private JSpinner indexSpinner;
    private JButton answerKey;
    
    private JComboBox orientation;
    private Orientation[] orientations = {Orientation.VERTICAL, Orientation.STUDENT_NUMBER, Orientation.CHECK_LETTER};
    
    public QuestionGroupPropertiesPanel() {
        this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Question group properties"));
        
        // GridBagLayout
        this.setLayout(new GridBagLayout());
        GridBagConstraints left = new GridBagConstraints();
        GridBagConstraints right = new GridBagConstraints();
        GridBagConstraints wide = new GridBagConstraints();
        left.gridx = 0; left.anchor = GridBagConstraints.LINE_END;
        right.gridx = 1; right.anchor = GridBagConstraints.LINE_START;
        wide.gridwidth = 2; wide.fill = GridBagConstraints.HORIZONTAL;


        // Rows
        left.gridy = right.gridy = 0;
        this.add(new JLabel("Rows:"), left);
        rowsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 256, 1));
        rowsSpinner.addChangeListener(this);
        this.add(rowsSpinner, right);

        // Columns
        left.gridy = right.gridy = 1;
        this.add(new JLabel("Columns:"), left);
        colsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 256, 1));
        colsSpinner.addChangeListener(this);
        this.add(colsSpinner, right);

        // Bubble width
        left.gridy = right.gridy = 2;
        this.add(new JLabel("Bubble width:"), left);
        bubbleWidthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 256, 1));
        bubbleWidthSpinner.addChangeListener(this);
        this.add(bubbleWidthSpinner, right);
        
        // Bubble height
        left.gridy = right.gridy = 3;
        this.add(new JLabel("Bubble height:"), left);
        bubbleHeightSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 256, 1));
        bubbleHeightSpinner.addChangeListener(this);
        this.add(bubbleHeightSpinner, right);
        
        // Index
        left.gridy = right.gridy = 4;
        this.add(new JLabel("Start index:"), left);
        indexSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1024, 1));
        indexSpinner.addChangeListener(this);
        this.add(indexSpinner, right);

        // Orientation
        wide.gridy = 5;
        orientation = new JComboBox(orientations);
        orientation.addItemListener(this);
        this.add(orientation, wide);
        //orientation.addActionListener(this);
        
        // Answer key
        wide.gridy = 6;
        answerKey = new JButton("Answer key...");
        answerKey.addActionListener(this);
        this.add(answerKey, wide);
    }
    
    public void setUndoSupport(UndoSupport undo) {
        this.undoSupport = undo;
    }
    
    /**
     * Sets a components as the only selected component.
     * @param component Component to be selected
     */
    public void setSelectedComponent(QuestionGroupComponent component) {
        this.selectedComponent = component;
        
        SwingUtilities.invokeLater(new Runnable() {
        	public void run() {
        		fetchValues();
        	}
        });
        
        // Create a new undo object on next edit
        this.currentEdit = null;
    }
    
    /**
     * Fetches the values from selected components and updates the components.
     */
    private void fetchValues() {
        if (selectedComponent == null) {
            rowsSpinner.setValue(1);
            colsSpinner.setValue(1);
            bubbleWidthSpinner.setValue(1);
            bubbleHeightSpinner.setValue(1);
            indexSpinner.setValue(1);
            return;
        }

        QuestionGroup group = selectedComponent.getQuestionGroup();
        
        rowsSpinner.setValue(group.getRowCount());
        colsSpinner.setValue(group.getColumnCount());
        bubbleWidthSpinner.setValue(group.getBubbleWidth());
        bubbleHeightSpinner.setValue(group.getBubbleHeight());
        indexSpinner.setValue(group.getIndexOffset());
        orientation.setSelectedItem(group.getOrientation());
        
        // Enable/disable properties
        if (group.getOrientation() == Orientation.CHECK_LETTER || group.getOrientation() == Orientation.STUDENT_NUMBER) {
        	indexSpinner.setEnabled(false);
        	answerKey.setEnabled(false);
        } else {
        	indexSpinner.setEnabled(true);
        	answerKey.setEnabled(true);
        }
    }
    
    
    public void stateChanged(ChangeEvent event) {
        if (selectedComponent == null) {
            return;
        }
        
        // Create a new undo object if this is the first edit. Otherwise accumulate changes in the old object.
        if (currentEdit == null) {
            this.currentEdit = new AttributeEdit(selectedComponent);
            undoSupport.postEdit(currentEdit);
        }
        
        // Update question group parameters
        Object source = event.getSource();
        QuestionGroup group = selectedComponent.getQuestionGroup();
        
        if (source == rowsSpinner) {
            group.setRowCount(((SpinnerNumberModel)rowsSpinner.getModel()).getNumber().intValue());
        } else if (source == colsSpinner) {
            group.setColumnCount(((SpinnerNumberModel)colsSpinner.getModel()).getNumber().intValue());
        } else if (source == bubbleWidthSpinner) {
            group.setBubbleWidth(((SpinnerNumberModel)bubbleWidthSpinner.getModel()).getNumber().intValue());
        } else if (source == bubbleHeightSpinner) {
            group.setBubbleHeight(((SpinnerNumberModel)bubbleHeightSpinner.getModel()).getNumber().intValue());
        } else if (source == indexSpinner) {
            group.setIndexOffset(((SpinnerNumberModel)indexSpinner.getModel()).getNumber().intValue());
        }
        
        // Update undo object to support redo
        currentEdit.updateAttributes();
        
        selectedComponent.updateBounds();
        selectedComponent.repaint();
    }
    
    public void actionPerformed(ActionEvent event) {
        if (selectedComponent == null) {
            return;
        }
        
        // Create a new undo object if this is the first edit. Otherwise accumulate changes in the old object.
        if (currentEdit == null) {
            this.currentEdit = new AttributeEdit(selectedComponent);
            undoSupport.postEdit(currentEdit);
        }
        
        QuestionGroup group = selectedComponent.getQuestionGroup();
        
        if (event.getSource() == answerKey) {
            // Show the answer key dialog
            new AnswerKeyDialog(group);
        }
        
        // Update undo object to support redo
        currentEdit.updateAttributes();
        
        selectedComponent.repaint();
    }
    
    public void itemStateChanged(ItemEvent event) {
    	if (selectedComponent == null) {
            return;
        }
        
        // Create a new undo object if this is the first edit. Otherwise accumulate changes in the old object.
        if (currentEdit == null) {
            this.currentEdit = new AttributeEdit(selectedComponent);
            undoSupport.postEdit(currentEdit);
        }
        
        QuestionGroup group = selectedComponent.getQuestionGroup();
    	if (event.getSource() == orientation && event.getStateChange() == ItemEvent.SELECTED) {
    		Orientation newOrientation = (Orientation)event.getItem();
    		group.setOrientation(newOrientation);
    	}
    	
    	// Update undo object to support redo
        currentEdit.updateAttributes();
        
        selectedComponent.repaint();
    }
    
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(Rectangle arg0, int arg1, int arg2) {
        return 32;
    }
    
    public int getScrollableBlockIncrement(Rectangle arg0, int arg1, int arg2) {
        return 128;
    }

    public boolean getScrollableTracksViewportHeight() {
        return true;
    }

    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    private class AttributeEdit extends AbstractUndoableEdit {
        private static final long serialVersionUID = 1L;
        
        private QuestionGroupComponent groupComponent;
        private QuestionGroup group;
        
        private int previousRows, previousCols;
        private int previousWidth, previousHeight;
        private int previousIndex;
        private Orientation previousOrientation;
        
        private int newRows, newCols;
        private int newWidth, newHeight;
        private int newIndex;
        private Orientation newOrientation;
        
        public AttributeEdit(QuestionGroupComponent groupComponent) {
            this.groupComponent = groupComponent;
            this.group = groupComponent.getQuestionGroup();
            
            previousRows = group.getRowCount();
            previousCols = group.getColumnCount();
            previousWidth = group.getBubbleWidth();
            previousHeight = group.getBubbleHeight();
            previousIndex = group.getIndexOffset();
            previousOrientation = group.getOrientation();
        }
        
        public void updateAttributes() {
            newRows = group.getRowCount();
            newCols = group.getColumnCount();
            newWidth = group.getBubbleWidth();
            newHeight = group.getBubbleHeight();
            newIndex = group.getIndexOffset();
            newOrientation = group.getOrientation();
        }
        
        public void undo() throws CannotUndoException {
            super.undo();
            
            group.setRowCount(previousRows);
            group.setColumnCount(previousCols);
            group.setBubbleWidth(previousWidth);
            group.setBubbleHeight(previousHeight);
            group.setIndexOffset(previousIndex);
            group.setOrientation(previousOrientation);
            
            groupComponent.repaint();
            fetchValues();
        }

        public void redo() throws CannotRedoException {
            super.redo();
            
            group.setRowCount(newRows);
            group.setColumnCount(newCols);
            group.setBubbleWidth(newWidth);
            group.setBubbleHeight(newHeight);
            group.setIndexOffset(newIndex);
            group.setOrientation(newOrientation);
            
            groupComponent.repaint();
            fetchValues();
        }

        public String getPresentationName() {
            return "edit question group";
        }

    }
    
}
