package omr;

import java.io.IOException;
import java.util.Observer;

/**
 * A background task that analyzes all Sheets in the project. This includes image registration and calculating average brightnesses of the bubbles. 
 */
public class AnalyzeSheetsTask extends Task {

    private Project project;

    public AnalyzeSheetsTask(Project project, Observer observer) {
        super(observer);
        
        this.project = project;
    }
    
    @Override
    public void run() {
        SheetStructure structure = project.getSheetStructure();
        SheetsContainer sheets = project.getSheetsContainer();

        Histogram histogram = project.getHistogram(); 
        histogram.reset();
        
        this.setEstimatedOperationsCount(sheets.size());
        this.setStatusText("Processing sheets");
        
        for (Sheet sheet : sheets) {
            try {
                sheet.analyze(structure, project.getHistogram());
            } catch (OutOfMemoryError e) {
                System.err.println("Out of memory when analyzing sheets.");
                return;
            } catch (IOException e) {
                System.err.println(e);
                break;
            }
            
            // Publish progress
            this.increaseCompletedOperationsCount();
        }
        
        // Calculate answers
        project.calculateThreshold();
        project.calculateAnswers();
        
        this.finished();
    }

}
