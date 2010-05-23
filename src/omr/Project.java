package omr;

import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.Observable;
import java.util.Observer;

import omr.SheetStructure.SheetStructureEvent;

/**
 * Main OMR project model. 
 * 
 * @author Tapio Auvinen
 */
public class Project implements Observer {
    /**
     * Thresholding modes.
     * PER_SHEET: thresholds are set to each sheet independently 
     * GLOBAL: all sheets have same thresholds
     */
    public enum ThresholdingStrategy {PER_SHEET, GLOBAL};
    
    private SheetsContainer answerSheets;
    private SheetStructure sheetStructure;              // Contains information about the positions of question groups etc.
    
    private Histogram histogram;                        // Global histogram
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
     * Adds answer sheets to the project.
     * @param files Array of files to add
     */
    public void addAnswerSheets(File[] files) throws IOException {
        this.answerSheets.importSheets(files);
    }
    
    /**
     * Removes answer sheets from the project.
     */
    public void removeAnswerSheets(Sheet[] sheets) {
        this.answerSheets.removeSheets(sheets);
    }
    
    /**
     * Returns the list of answer sheets.
     */
    public AbstractList<Sheet> getAnswerSheets() {
        return this.answerSheets.getSheets();
    }

    /**
     * Returns the SheetsContainer which contains all answer sheets and is iterable.
     * @return
     */
    public SheetsContainer getSheetsContainer() {
        return this.answerSheets;
    }
    
    /**
     * Returns the SheetStructure which contains information about the positions and properties of question groups and registration markers.
     */
    public SheetStructure getSheetStructure() {
        return this.sheetStructure;
    }

    /**
     * Returns the GradingScheme that can be used to convert answers into points.
     */
    public GradingScheme getGradingScheme() {
        return this.gradingScheme;
    }
    
    /**
     * Returns the global histogram of all bubbles in all answer sheets.
     */
    public Histogram getHistogram() {
        return this.histogram;
    }
    
    /**
     * Returns the current thresholding mode. See enum ThresholdingStrategy.
     */
    public ThresholdingStrategy getThresholdingStrategy() {
        return thresholdingStrategy;
    }

    /**
     * Sets thresholding mode. See enum ThresholdingStrategy.
     */
    public void setThresholdingStrategy(ThresholdingStrategy thresholdingStrategy) {
        this.thresholdingStrategy = thresholdingStrategy;
    }
    
    
    /** 
     * Automatically sets thresholds to sensible values. 
     */
    public void calculateThreshold() {
        histogram.guessThreshold();
    }
    
    /**
     * Calculates all answers using the global black and white thresholds (set in the global histogram).
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
