package omr.gui.structure;


import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.AbstractList;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import omr.RegistrationMarker;
import omr.QuestionGroup;
import omr.SheetStructure;
import omr.gui.RegistrationComponent;
import omr.gui.QuestionGroupComponent;
import omr.gui.SheetEditor;
import omr.gui.SheetViewComponent;

/**
 * A component that allows user to define the structure of the answer sheet. A sheet image is displayed underneath, and question groups can be added on the sheet.
 */
public class SheetStructureEditor extends SheetEditor implements MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;
    
    private SheetStructure sheetStructure;                      // Sheet structure being edited
    
    private SheetViewComponent newComponent;                    // Component being created
    private LinkedList<SheetViewComponent> selectedComponents;  // Components currently selected
    private LinkedList<ChangeListener> changeListeners;         // Selection change listeners
    
    public SheetStructureEditor() {
        super();
        
        this.selectedComponents = new LinkedList<SheetViewComponent>();
        this.changeListeners = new LinkedList<ChangeListener>();
        
        addMouseMotionListener(this);
        addMouseListener(this);
        
        // Delete key action
        this.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DELETE"), "delete");
        this.getActionMap().put("delete", new DeleteAction(this));
    }
    
    public SheetStructureEditor(SheetStructure sheetStructure) {
        this();
        
        this.setSheetStructure(sheetStructure);
    }
    
    public void setSheetStructure(SheetStructure sheetStructure) {
        this.sheetStructure = sheetStructure;
        
        // Remove all components as they may not reflect the new sheetStructure
        this.removeAll();
        
        // Add questionGroups from the new sheetStructure to the view
        for (QuestionGroup group : sheetStructure.getQuestionGroups()) {
            QuestionGroupComponent component = new QuestionGroupComponent(this, group);
            component.setUndoSupport(undoSupport);
            this.add(component);
        }
        
        // Add registration markers from the new sheetStructure to the view
        for (RegistrationMarker marker : sheetStructure.getRegistrationMarkers()) {
            RegistrationComponent component = new RegistrationComponent(this, marker);
            component.setUndoSupport(undoSupport);
            this.add(component);
        }
    }
    
    public void addSelectionListener(ChangeListener listener) {
        changeListeners.add(listener);
    }
    
    public void mousePressed(MouseEvent event) {
        int button = event.getButton();
        int x = (int)(event.getX() / zoomLevel);
        int y = (int)(event.getY() / zoomLevel);
        
        // Deselect
        if (currentTool == Tool.SELECT && event.getButton() == MouseEvent.BUTTON1 && !event.isControlDown()) {
            clearSelection();
        }

        // Create question group
        if (currentTool == Tool.QUESTION && button == MouseEvent.BUTTON1) {
        	clearSelection();
        	
            QuestionGroupComponent groupComponent = new QuestionGroupComponent(this, new QuestionGroup(x,y,x,y));
            groupComponent.setUndoSupport(undoSupport);
            this.newComponent = groupComponent;
            addQuestionGroup(groupComponent);  // Adds group to the view and model
            undoSupport.postEdit(new CreateQuestionGroupEdit(this, groupComponent));
        }
        
        // Create registration marker
        if (currentTool == Tool.REGISTRATION && button == MouseEvent.BUTTON1) {
        	clearSelection();
        	
        	RegistrationMarker marker = new RegistrationMarker(x,y);
        	marker.copyMarkerImage(this.sheet);
        	
        	RegistrationComponent markerComponent = new RegistrationComponent(this, marker);
            
        	// Add marker to the view and model
            if (addRegistrationMarker(markerComponent)) {
		        this.newComponent = markerComponent;
		        markerComponent.setUndoSupport(undoSupport);
		        undoSupport.postEdit(new CreateRegistrationMarkerEdit(this, markerComponent));
            }
        }
    }
    
    public void mouseReleased(MouseEvent event) {
        // Finished drawing a new component?
        if (newComponent != null) {
            setSelectedComponent(newComponent);
        }
        
        newComponent = null;
    }

    public void mouseDragged(MouseEvent event) {
        // Autoscroll
        Rectangle r = new Rectangle(event.getX(), event.getY(), 1, 1);
        scrollRectToVisible(r);
        
        int x = (int)(event.getX() / zoomLevel);
        int y = (int)(event.getY() / zoomLevel);
        
        // Resizing newly created question group
        if (currentTool == Tool.QUESTION && newComponent != null) {
            QuestionGroupComponent groupComponent = (QuestionGroupComponent)newComponent;
            QuestionGroup group = groupComponent.getQuestionGroup();
            group.setRightX(x);
            group.setBottomY(y);
            groupComponent.updateBounds();
        }
    }
    
    public void mouseClicked(MouseEvent event) {
    }

    public void mouseMoved(MouseEvent event) {
    }
    
    /**
     * Sets a components as the only selected component.
     * @param component Component to be selected
     */
    public void setSelectedComponent(SheetViewComponent component) {
        this.clearSelection();
        this.addSelectedComponent(component);
    }
    
    /**
     * Adds a component to the group of selected components. 
     * @param component Component to be selected
     */
    public void addSelectedComponent(SheetViewComponent component) {
        this.selectedComponents.add(component);
        component.setSelected(true);
        repaint();
        
        // Notify listeners
        for (ChangeListener listener : changeListeners) {
            listener.stateChanged(new ChangeEvent(this));
        }
    }
    
    /**
     * Clears the selection so that no component is selected.
     */
    public void clearSelection() {
        // Set components unselected
        for (SheetViewComponent component : this.selectedComponents) {
            component.setSelected(false);
        }
        
        // Clear the list of selected components
        this.selectedComponents.clear();
        
        // Notify listeners
        for (ChangeListener listener : changeListeners) {
            listener.stateChanged(new ChangeEvent(this));
        }
        
        repaint();
    }

    public AbstractList<SheetViewComponent> getSelectedComponents() {
        return this.selectedComponents;
    }
    
    /**
     * Add a question group to the SheetStructure and the view.
     */
    public void addQuestionGroup(QuestionGroupComponent groupComponent) {
        this.sheetStructure.addQuestionGroup(groupComponent.getQuestionGroup()); // Add question group to the sheet structure model
        this.add(groupComponent);                                                // Add component to the view
        this.repaint(groupComponent.getBounds());
    }
    
    /**
     * Removes a question group to the SheetStructure and the view.
     */
    public void removeQuestionGroup(QuestionGroupComponent groupComponent) {
        this.sheetStructure.removeQuestionGroup(groupComponent.getQuestionGroup());  // Remove question group from the sheet structure model
        this.remove(groupComponent);                                                 // Remove component from the view
        this.repaint(groupComponent.getBounds());
    }
    
    /**
     * Add a question group to the SheetStructure and the view.
     */
    public boolean addRegistrationMarker(RegistrationComponent registrationComponent) {
    	// Add marker to the sheet structure model
    	if (this.sheetStructure.addRegistrationMarker(registrationComponent.getRegistrationMarker())) {
    		// Add component to the view
    		this.add(registrationComponent);                                                    
        	this.repaint(registrationComponent.getBounds());
        	return true;
    	} else {
    		return false;
    	}
    }
    
    /**
     * Removes a question group to the SheetStructure and the view.
     */
    public void removeRegistrationMarker(RegistrationComponent registrationComponent) {
        this.sheetStructure.removeRegistrationMarker(registrationComponent.getRegistrationMarker());  // Remove marker from the sheet structure model
        this.remove(registrationComponent);                                                     // Remove component from the view
        this.repaint(registrationComponent.getBounds());
    }
    
    public void mouseEntered(MouseEvent arg0) {
    }

    public void mouseExited(MouseEvent arg0) {
    }

    
    private class CreateQuestionGroupEdit extends AbstractUndoableEdit {
        private static final long serialVersionUID = 1L;
        
        private SheetStructureEditor sheetView;
        private QuestionGroupComponent groupComponent;
        
        public CreateQuestionGroupEdit(SheetStructureEditor sheetView, QuestionGroupComponent groupComponent) {
            this.sheetView = sheetView;
            this.groupComponent = groupComponent;
        }

        public void undo() throws CannotUndoException {
            super.undo();
            sheetView.removeQuestionGroup(this.groupComponent);
        }

        public void redo() throws CannotRedoException {
            super.redo();
            sheetView.addQuestionGroup(this.groupComponent);
        }

        public String getPresentationName() {
            return "create question group";
        }
    }
    
    private class CreateRegistrationMarkerEdit extends AbstractUndoableEdit {
        private static final long serialVersionUID = 1L;
        
        private SheetStructureEditor sheetView;
        private RegistrationComponent registrationComponent;
        
        public CreateRegistrationMarkerEdit(SheetStructureEditor sheetView, RegistrationComponent registrationComponent) {
            this.sheetView = sheetView;
            this.registrationComponent = registrationComponent;
        }

        public void undo() throws CannotUndoException {
            super.undo();
            sheetView.removeRegistrationMarker(this.registrationComponent);
        }

        public void redo() throws CannotRedoException {
            super.redo();
            sheetView.addRegistrationMarker(this.registrationComponent);
        }

        public String getPresentationName() {
            return "create question group";
        }

    }

    private class DeleteAction extends AbstractAction {
    	private static final long serialVersionUID = 1L;
    	
    	private SheetStructureEditor sheetEditor;
    	
        public DeleteAction(SheetStructureEditor sheetEditor) {
        	this.sheetEditor = sheetEditor;
        }
        
        public void actionPerformed(ActionEvent e) {
        	// Delete all selected components
        	for (SheetViewComponent component : selectedComponents) {
        		if (component instanceof RegistrationComponent) {
        			removeRegistrationMarker((RegistrationComponent)component);
                    undoSupport.postEdit(new DeleteRegistrationMarkerEdit(sheetEditor, (RegistrationComponent)component));
        		} else if (component instanceof QuestionGroupComponent) {
        			removeQuestionGroup((QuestionGroupComponent)component);
                    undoSupport.postEdit(new DeleteQuestionGroupEdit(sheetEditor, (QuestionGroupComponent)component));
        		}
        	}
        }
    }

    private class DeleteQuestionGroupEdit extends AbstractUndoableEdit {
        private static final long serialVersionUID = 1L;
        
        private SheetStructureEditor sheetView;
        private QuestionGroupComponent groupCompnent;
        
        public DeleteQuestionGroupEdit(SheetStructureEditor sheetView, QuestionGroupComponent groupCompnent) {
            this.sheetView = sheetView;
            this.groupCompnent = groupCompnent;
        }
      
        public void undo() throws CannotUndoException {
            super.undo();
            sheetView.addQuestionGroup(this.groupCompnent);
        }

        public void redo() throws CannotRedoException {
            super.redo();
            sheetView.removeQuestionGroup(this.groupCompnent);
        }

        public String getPresentationName() {
            return "delete question group";
        }

    }
    
    private class DeleteRegistrationMarkerEdit extends AbstractUndoableEdit {
        private static final long serialVersionUID = 1L;
        
        private SheetStructureEditor sheetView;
        private RegistrationComponent registrationComponent;
        
        public DeleteRegistrationMarkerEdit(SheetStructureEditor sheetView, RegistrationComponent registrationComponent) {
            this.sheetView = sheetView;
            this.registrationComponent = registrationComponent;
        }
      
        public void undo() throws CannotUndoException {
            super.undo();
            sheetView.addRegistrationMarker(this.registrationComponent);
        }

        public void redo() throws CannotRedoException {
            super.redo();
            sheetView.removeRegistrationMarker(this.registrationComponent);
        }

        public String getPresentationName() {
            return "delete registration marker";
        }

    }
    
}
