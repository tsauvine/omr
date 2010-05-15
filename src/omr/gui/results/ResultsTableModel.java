package omr.gui.results;

import javax.swing.table.AbstractTableModel;

import omr.GradingScheme;
import omr.Project;
import omr.QuestionGroup;
import omr.Sheet;
import omr.QuestionGroup.Orientation;

public class ResultsTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    
    private Project project;
    
    public ResultsTableModel() {
    }
    
    public void setProject(Project project) {
        this.project = project;
        fireTableStructureChanged();
    }
    
    public int getColumnCount() {
        if (project == null) {
            return 2;
        } else {
            // TODO: cache this.
            
            int questionsCount = 0;
            for (QuestionGroup group : project.getSheetStructure().getQuestionGroups()) {
            	if (group.getOrientation() != Orientation.VERTICAL && group.getOrientation() != Orientation.HORIZONTAL) {
                    continue;
                }
            	
                questionsCount += group.getQuestionsCount();
            }
            
            // Student id, questions, total score
            return questionsCount + 2;
        }
        
    }
    
    public String getColumnName(int col) {
        if (col == 0) {
            return "Student ID";
        }
        
        if (project == null) {
            return "Total points";
        }
        
        col--;
        for (QuestionGroup group : project.getSheetStructure().getQuestionGroups()) {
            if (group.getOrientation() != Orientation.VERTICAL && group.getOrientation() != Orientation.HORIZONTAL) {
                continue;
            }
            
            if (col < group.getQuestionsCount()) {
                return Integer.toString(group.getQuestionNumber(col));
            } else {
                col -= group.getQuestionsCount();
            }
        }
        
        return "Total points";
    }
    
    public int getRowCount() {
        if (project == null) {
            return 0;
        }
        
        return project.getAnswerSheets().size();
    }
    
    public Object getValueAt(int row, int col) {
        if (project == null) {
            return null;
        }
        
        Sheet sheet = project.getAnswerSheets().get(row);
        GradingScheme grading = project.getGradingScheme();
        
        // Studentnumber in the left-most column
        if (col == 0) {
            return sheet.getStudentId();
        }

        // Points
        col--;
        for (QuestionGroup group : project.getSheetStructure().getQuestionGroups()) {
            if (group.getOrientation() != Orientation.VERTICAL && group.getOrientation() != Orientation.HORIZONTAL) {
                continue;
            }
            
            if (col < group.getQuestionsCount()) {
                return grading.getScore(sheet, group, col); 
            } else {
                col -= group.getQuestionsCount();
            }
        }
            
        // Total score in the last column
        return grading.getScore(sheet, project.getSheetStructure());
    }
    
    public boolean isCellEditable(int row, int col) {
        return false;
    }
    
    public void setValueAt(Object value, int row, int col) {
        return;
    }
    
    public void refreshStructure() {
        fireTableStructureChanged();
    }

}
