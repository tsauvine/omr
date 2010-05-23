package omr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import omr.QuestionGroup.Orientation;

/**
 * Saves answers and results as comma separated value (CSV) files. 
 */
public class CsvSerializer {

    public void saveAnswers(Project project, File file) throws IOException {
        
        SheetStructure structure = project.getSheetStructure();
        
        PrintStream fout = null;
        try {
            fout = new PrintStream(new FileOutputStream (file));
            
            // Write header line
            fout.print("image,studentId");
            for (QuestionGroup group : structure.getQuestionGroups()) {
            	// Skip the studentnumber
            	if (group.getOrientation() != Orientation.VERTICAL && group.getOrientation() != Orientation.HORIZONTAL) {
            		continue;
            	}
            	
            	// Question numbers
                int questionsCount = group.getQuestionsCount();
                int indexOffset = group.getIndexOffset();
                for (int i = 0; i < questionsCount; i++) {
                    fout.print(",");
                    fout.print(indexOffset + i);
                }
            }
            fout.println();
            
            // Iterate throuh sheets
            for (Sheet sheet : project.getSheetsContainer()) {
                // Image filename
                fout.print(sheet.getFileName());
                fout.print(",");

                // Studentnumber
                String studentId = sheet.getStudentId(); 
                if (studentId == null || studentId.length() < 1) {
                	fout.print(sheet.getId());
                } else {
                	fout.print(studentId);
                }
                
                // Print answers
                for (QuestionGroup group : structure.getQuestionGroups()) {
                	// Skip the studentnumber
                	if (group.getOrientation() != Orientation.VERTICAL && group.getOrientation() != Orientation.HORIZONTAL) {
                		continue;
                	}
                	
                	int questionsCount = group.getQuestionsCount();
                    for (int i = 0; i < questionsCount; i++) {    
                        fout.print(",");
                        String choices = sheet.getChoices(group, i);
                        if (choices != null) {
                            fout.print(choices);
                        }
                    }
                }

                fout.println();
            }
        } finally {
            if (fout != null) {
                fout.close();
            }
        }
    }
    
    public void saveResults(Project project, File file) throws IOException {
    	SheetStructure structure = project.getSheetStructure();
    	GradingScheme grading = project.getGradingScheme();
        PrintStream fout = null;
        
        try {
            fout = new PrintStream(new FileOutputStream (file));
            
            // Write header line
            fout.print("image,studentId");
            for (QuestionGroup group : structure.getQuestionGroups()) {
            	// Skip the studentnumber
            	if (group.getOrientation() != Orientation.VERTICAL && group.getOrientation() != Orientation.HORIZONTAL) {
            		continue;
            	}
            	
                int questionsCount = group.getQuestionsCount();
                int indexOffset = group.getIndexOffset();
                for (int i = 0; i < questionsCount; i++) {
                    fout.print(",");
                    fout.print(indexOffset + i);
                }
            }
            fout.print(",total");
            fout.println();
            
            for (Sheet sheet : project.getSheetsContainer()) {
                // Image filename
                fout.print(sheet.getFileName());
                fout.print(",");
                
                // Student id
                String studentId = sheet.getStudentId(); 
                if (studentId == null || studentId.length() < 1) {
                	fout.print(sheet.getId());
                } else {
                	fout.print(studentId);
                }
                
                // Print points
                for (QuestionGroup group : structure.getQuestionGroups()) {
                	// Skip the studentnumber
                	if (group.getOrientation() != Orientation.VERTICAL && group.getOrientation() != Orientation.HORIZONTAL) {
                		continue;
                	}
                	
                	int questionsCount = group.getQuestionsCount();
                    for (int i = 0; i < questionsCount; i++) {    
                        double score = grading.getScore(sheet, group, i);
                        fout.print(",");
                        fout.print(score);
                    }
                }

                // Total score in the last column
                double totalScore = grading.getScore(sheet, project.getSheetStructure());
                fout.print(",");
                fout.print(totalScore);

                fout.println();
            }
        } finally {
            if (fout != null) {
                fout.close();
            }
        }
    
    }
}
