package omr;

import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.Observable;
import java.util.Observer;

import omr.SheetStructure.SheetStructureEvent;

/**
 * Main OMR project model. 
 * @author Tapio Auvinen
 */
public class Project implements Observer {
    public enum ThresholdingStrategy {PER_SHEET, GLOBAL};
    
    private SheetsContainer answerSheets;
    private SheetStructure sheetStructure;  // Contains information about the positions of question groups etc.
    
    private Histogram histogram;
    private ThresholdingStrategy thresholdingStrategy;
    private GradingScheme gradingScheme;
    
    public Project() {
        this.answerSheets = new SheetsContainer();
        
        this.sheetStructure = new SheetStructure();
        this.sheetStructure.addObserver(this);
        
        this.histogram = new Histogram();
        this.thresholdingStrategy = ThresholdingStrategy.PER_SHEET;
        this.gradingScheme = new GradingScheme();
    }
    
    /**
     * Tells whether the project has unsaved modifications or is it safe to quit.
     */
    public boolean isChanged() {
    	// TODO
        return true;
    }
    
    /**
     * Adds the specified answer sheets to the project.
     */
    public void addAnswerSheets(File[] files) throws IOException {
        this.answerSheets.importSheets(files);
    }
    
    /**
     * Removes the specified answer sheets from the project.
     */
    public void removeAnswerSheets(Sheet[] sheets) {
        this.answerSheets.removeSheets(sheets);
    }
    
    public AbstractList<Sheet> getAnswerSheets() {
        return this.answerSheets.getSheets();
    }
    
    public SheetsContainer getSheetsContainer() {
        return this.answerSheets;
    }
    
    public SheetStructure getSheetStructure() {
        return this.sheetStructure;
    }
    
    public GradingScheme getGradingScheme() {
        return this.gradingScheme;
    }
    
    /**
     * Returns the histogram.
     */
    public Histogram getHistogram() {
        return this.histogram;
    }
    
    public ThresholdingStrategy getThresholdingStrategy() {
        return thresholdingStrategy;
    }

    public void setThresholdingStrategy(ThresholdingStrategy thresholdingStrategy) {
        this.thresholdingStrategy = thresholdingStrategy;
    }
    
    
    /** 
     * Automatiacally sets the thresholds to sensible values. 
     */
    public void calculateThreshold() {
        histogram.guessThreshold();
    }
    
    /**
     * Calculates all answers using the global black and white thresholds.
     */
    public void calculateAnswers() {
        int blackThreshold = histogram.getBlackThreshold();
        int whiteThreshold = histogram.getWhiteThreshold();
        
        for (Sheet sheet : answerSheets) {
            if (thresholdingStrategy == ThresholdingStrategy.GLOBAL) {                
                for (QuestionGroup group : sheetStructure.getQuestionGroups()) {
                    sheet.calculateAnswers(group, blackThreshold, whiteThreshold);
                }
            } else {
                for (QuestionGroup group : sheetStructure.getQuestionGroups()) {
                    sheet.calculateAnswers(group, sheet.getHistogram().getBlackThreshold(), sheet.getHistogram().getWhiteThreshold());
                }
            }
        }
    }
    
    /**
     * Notified by sheet structure when it changes.
     */
    public void update(Observable source, Object event) {
    	if (SheetStructureEvent.STRUCTURE_CHANGED == event || 
    	        SheetStructureEvent.BUBBLE_POSITIONS_CHANGED == event) {
    	    this.answerSheets.invalidateBrightnesses();
    	} else if (SheetStructureEvent.REGISTRATION_CHANGED == event) {
    	    this.answerSheets.invalidateRegistration();
    	}
    }
    
}
