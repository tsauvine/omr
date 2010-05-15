package omr.gui.structure;

import javax.swing.table.AbstractTableModel;

import omr.QuestionGroup;

/**
 * A table of checkboxes for setting the answer key.
 * @author Tapio Auvinen
 */
public class AnswerKeyTableModel extends AbstractTableModel {
    
    private static final long serialVersionUID = 1L;
    private QuestionGroup group;
    
    public AnswerKeyTableModel(QuestionGroup group) {
        this.group = group;
    }
    
    
    public int getColumnCount() {
        return group.getAlternativesCount() + 1;
    }
    
    public String getColumnName(int col) {
        if (col > 0) {
            return group.getAlternative(col - 1);
        }
        
        return "";
    }
    
    public int getRowCount() {
        return group.getQuestionsCount();
    }
    
    public Object getValueAt(int row, int col) {
        if (col < 1) {
            // Question number in the left-most column
            return new Integer(group.getQuestionNumber(row));
        } else {
            // Checkbox
            return new Boolean(group.getCorrectAnswer(row, col - 1));
        }
    }
    
    public boolean isCellEditable(int row, int col) {
        // All cells except the question number are editable
        return col > 0;
    }
    
    public Class getColumnClass(int col) {
        if (col < 1) {
            return Number.class;
        } else {
            return Boolean.class;
        }
    }

    public void setValueAt(Object value, int row, int col) {
        group.setCorrectAnswer(row, col - 1, (Boolean)value);
        fireTableCellUpdated(row, col);
    }

}
