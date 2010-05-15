package omr.gui.structure;

import java.util.AbstractList;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import omr.Project;
import omr.Sheet;

public class SheetComboModel implements ComboBoxModel, Observer {
    
    private static final long serialVersionUID = 1L;
    private Project project;
    private Sheet selectedSheet;
    
    private LinkedList<ListDataListener> listDataListeners;
    
    public SheetComboModel() {
        this.listDataListeners = new LinkedList<ListDataListener>();
    }
    
    public void setProject(Project project) {
        // Unsubscribe from previous model
        if (this.project != null) {
            this.project.getSheetsContainer().deleteObserver(this);
        }
        
        this.project = project;
        
        // Subscribe to new model
        if (project != null) {
            project.getSheetsContainer().addObserver(this);
            this.selectedSheet = project.getSheetStructure().getReferenceSheet();
        }
        
        notifyListeners();
    }
    
    /**
     * Notified by the SheetsContainer when the sheet list is updated.
     */
    public void update(Observable source, Object event) {
    	// Event is set to null when the whole list changes. Otherwise, it contains an Integer specifying the changed row.
    	if (event == null) {
    		notifyListeners();
    	}
    }
    
    private void notifyListeners() {
        // Notify listeners
        for (ListDataListener listener : this.listDataListeners) {
            listener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, this.getSize()));
        }
    }

    public Object getSelectedItem() {
        return this.selectedSheet;
    }

    public void setSelectedItem(Object sheet) {
        this.selectedSheet = (Sheet)sheet;
    }
    
    public int getSize() {
        if (project != null) {
            return project.getAnswerSheets().size();
        } else {
            return 0;
        }
    }
    
    public Object getElementAt(int row) {
        if (project != null) {
            AbstractList<Sheet> sheets = project.getAnswerSheets();
            
            try {
                return sheets.get(row);
            } catch(IndexOutOfBoundsException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    public void addListDataListener(ListDataListener listener) {
        this.listDataListeners.add(listener);
    }
    
    public void removeListDataListener(ListDataListener listener) {
        this.listDataListeners.remove(listener);
    }
    
}
