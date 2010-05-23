package omr;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Observable;
import java.util.Observer;

import omr.QuestionGroup.QuestionGroupEvent;

/**
 * Represents the structure of the question sheet, i.e. how the bubbles and registration markers are located. 
 * 
 * @author Tapio Auvinen
 */
public class SheetStructure extends Observable implements Observer {
	
	public enum SheetStructureEvent {
		REGISTRATION_CHANGED,
		BUBBLE_POSITIONS_CHANGED,
    	STRUCTURE_CHANGED,
    	ANSWER_KEY_CHANGED
    }
	
	private Sheet referenceSheet;
	
	private ArrayList<QuestionGroup> questionGroups;
    private ArrayList<RegistrationMarker> registrationMarkers;
    
    public SheetStructure() {
        this.questionGroups = new ArrayList<QuestionGroup>();
        this.registrationMarkers = new ArrayList<RegistrationMarker>();
    }
    
    /**
     * Sets the reference sheet over which the bubbles and markers are positioned. Alignment marker images are copied from the reference sheet.
     */
    public void setReferenceSheet(Sheet sheet) {
        if (sheet == this.referenceSheet) {
        	// Don't do anything if setting to the same value because this is a slow operation.
            return;
        }
    	
        this.referenceSheet = sheet;
    	
    	if (sheet != null) {
    	    BufferedImage sheetBuffer;
            try {
                sheetBuffer = sheet.getUnalignedBuffer();
            } catch (OutOfMemoryError e) {
                System.err.println(e);
                return;
            } catch (IOException e) {
                System.err.println(e);
                return;
            }
    	    
    	    // Update marker images
	    	for (RegistrationMarker marker : registrationMarkers) {
	    		marker.copyMarkerImage(sheetBuffer);
	    	}
    	}
    }
    
    /**
     * Returns the reference sheet over which the bubbles and markers have been positioned.
     */
    public Sheet getReferenceSheet() {
    	return this.referenceSheet;
    }
    
    /**
     * Adds a question group to the project.
     */
    public void addQuestionGroup(QuestionGroup group) {
        questionGroups.add(group);
        group.addObserver(this);
        sortGroups();
        
        // Notify observers
        setChanged();
        notifyObservers(SheetStructureEvent.STRUCTURE_CHANGED);
    }
    
    /**
     * Removes a question group from the project.
     */
    public void removeQuestionGroup(QuestionGroup group) {
        questionGroups.remove(group);
        group.deleteObserver(this);
        
        setChanged();
        notifyObservers(SheetStructureEvent.STRUCTURE_CHANGED);
    }
    
    /**
     * Returns a list of question groups in the sheet structure.
     */
    public AbstractList<QuestionGroup> getQuestionGroups() {
        return questionGroups;
    }
    
    /**
     * Adds a registration marker to the structure. Only two markers are supported.  
     * @return true if the marker was succesfully added. False if there is already two markers.
     */
    public boolean addRegistrationMarker(RegistrationMarker marker) {
        // Only take two markers
    	if (this.registrationMarkers.size() < 2) {
        	registrationMarkers.add(marker);
        	marker.addObserver(this);
        	
        	// Notify observers
        	setChanged();
            notifyObservers(SheetStructureEvent.REGISTRATION_CHANGED);
            
        	return true;
        } else {
        	return false;
        }
    }
    
    /**
     * Removes a registration marker from the project.
     */
    public void removeRegistrationMarker(RegistrationMarker marker) {
        registrationMarkers.remove(marker);
        marker.deleteObserver(this);
        
        // Notify observers
        setChanged();
        notifyObservers(SheetStructureEvent.REGISTRATION_CHANGED);
    }
    
    /**
     * Returns a list of registration markers in the sheet structure.
     */
    public AbstractList<RegistrationMarker> getRegistrationMarkers() {
        return registrationMarkers;
    }
    
    /**
     * Sorts question groups by index offset.
     */
    private void sortGroups() {
    	Collections.sort(this.questionGroups);
    }
    
    /**
     * Notified by structure components when they change.
     */
    public void update(Observable source, Object event) {
    	setChanged();
    	
    	if (QuestionGroupEvent.STRUCTURE_CHANGED == event) {
    		// Sort groups
    		this.sortGroups();
    		
    		notifyObservers(SheetStructureEvent.STRUCTURE_CHANGED);
    	} else if (QuestionGroupEvent.POSITION_CHANGED == event) {
    		notifyObservers(SheetStructureEvent.BUBBLE_POSITIONS_CHANGED);
    	} else if (source instanceof RegistrationMarker) {
    	    notifyObservers(SheetStructureEvent.REGISTRATION_CHANGED);
    	}
    }
}
