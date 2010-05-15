package omr.gui.calibration;

import java.util.AbstractList;
import java.util.Observable;
import java.util.Observer;

import javax.swing.table.AbstractTableModel;

import omr.Project;
import omr.Sheet;

public class SheetTableModel extends AbstractTableModel implements Observer {
    
    private static final long serialVersionUID = 1L;
    private Project project;
    
    public SheetTableModel() {
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
        }
        
        fireTableDataChanged();
    }
    
    public int getColumnCount() {
        return 2;
    }
    
    public String getColumnName(int col) {
        switch (col) {
            case 0:
                return "sheet";
            case 1:
                return "status";
        }
        
        return "";
    }
    
    public int getRowCount() {
        if (project != null) {
            return project.getAnswerSheets().size();
        } else {
            return 0;
        }
    }
    
    public Object getValueAt(int row, int col) {
        if (project == null) {
            return null;
        }
        
        AbstractList<Sheet> sheets = project.getAnswerSheets();
        Sheet sheet;
        try {
            sheet = sheets.get(row);
        } catch(IndexOutOfBoundsException e) {
            return null;
        }
        
        switch (col){
            case 0:
                return sheet;
            case 1:
                return sheet.getStatus();
        }
        
        return null;
    }
    
    public boolean isCellEditable(int row, int col) {
        return false;
    }
    
    public void setValueAt(Object value, int row, int col) {
        return;
    }

    /**
     * Notified by the SheetsContainer when the sheet list is updated.
     */
    public void update(Observable source, Object event) {
    	if (event instanceof Integer) {
        	int row = (Integer)event;
    		fireTableRowsUpdated(row, row);
        }
    }
}
