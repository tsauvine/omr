package omr.gui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

public class UndoSupport {
    private static final int UNDO_LIMIT = 20;
    
    private UndoManager undoManager;
    private UndoAction undoAction;
    private RedoAction redoAction;
    
    public UndoSupport() {
        this.undoManager = new UndoManager();
        this.undoManager.setLimit(UNDO_LIMIT);
        
        this.undoAction = new UndoAction(undoManager);
        this.redoAction = new RedoAction(undoManager);
    }
    
    public void postEdit(AbstractUndoableEdit edit) {
        undoManager.addEdit(edit);
        undoAction.updateUndoState();
        redoAction.updateRedoState();
    }
    
    public UndoAction getUndoAction() {
        return this.undoAction;
    }
    
    public RedoAction getRedoAction() {
        return this.redoAction;
    }
    
 
    public class UndoAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        private UndoManager undoManager;
        
        public UndoAction(UndoManager manager) {
            this.undoManager = manager;
        }

        public void actionPerformed(ActionEvent evt) {
            try {
                undoManager.undo();
            } catch (CannotUndoException e) {
                Toolkit.getDefaultToolkit().beep();
            }
        }
        
        protected void updateUndoState() {
            if (undoManager.canUndo()) {
                setEnabled(true);
                putValue(Action.NAME, undoManager.getUndoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Undo");
            }
        }
    }
    
    public class RedoAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        private UndoManager undoManager;
        
        public RedoAction(UndoManager manager) {
            this.undoManager = manager;
        }

        public void actionPerformed(ActionEvent evt) {
            try {
                undoManager.redo();
            } catch (CannotRedoException e) {
                Toolkit.getDefaultToolkit().beep();
            }
        }
        
        protected void updateRedoState() {
            if (undoManager.canRedo()) {
                setEnabled(true);
                putValue(Action.NAME, undoManager.getRedoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Redo");
            }
        }
    }

}
