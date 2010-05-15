package omr;


import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import omr.gui.ImageFileFilter;

public class SheetsContainer extends Observable implements Observer, Iterable<Sheet> {
    private ArrayList<Sheet> sheets;  // TODO: should probably replace this with a more powerful data structure to enable search with id
    private HashMap<String, PdfDocument> pdfDocuments;
    
    public SheetsContainer() {
        this.sheets = new ArrayList<Sheet>();
        this.pdfDocuments = new HashMap<String, PdfDocument>();
    }
    
    /**
     * Returns the iterator.
     */
    public Iterator<Sheet> iterator() {
        return sheets.iterator();
    }
    
    /**
     * Returns the number of sheets.
     */
    public int size() {
        return sheets.size();
    }
    
    public void invalidateRegistration() {
    	for (Sheet sheet : sheets) {
    		sheet.invalidateRegistration();
    	}
    }
    
    public void invalidateBrightnesses() {
    	for (Sheet sheet : sheets) {
    		sheet.invalidateBrightnesses();
    	}
    }
    
    public void invalidateAnswers() {
    	for (Sheet sheet : sheets) {
    		sheet.invalidateAnswers();
    	}
    }
    
    /**
     * Imports multiple answer sheet files to the project. 
     * @param files
     */
    public void importSheets(File[] files) throws IOException {
    	for (File file : files) {
            if (file.isDirectory()) {
                // Add all files in the directory non-recursively
                File[] children = file.listFiles(new ImageFileFilter());
                
                for (File child : children) {
                    importSheet(child);
                }
            } else if (file.isFile()) {
                importSheet(file);
            }
        }
        
        // Notify listeners
        setChanged();
        notifyObservers();
    }
    
    /**
     * Adds an answer sheet file to the project. If the file contains multiple pages, each page is added as a separate sheet.
     */
    public void importSheet(File file) throws IOException {
        // Do not accept directories
        if (file.isDirectory()) {
            return;
        }
        
        if (getFileExtension(file.getName()).equals("pdf")) {
        	// If it's a PDF, add a new sheet for each page
        	// Do we already have this document?
        	PdfDocument pdf = pdfDocuments.get(file.getPath());
        	
        	// If not, then open it
        	if (pdf == null) {
        		pdf = new PdfDocument(file);
        		pdfDocuments.put(file.getPath(), pdf);
        	}

        	// Add pages
        	for (int page = 0; page < pdf.getPageCount(); page++) {
        		Sheet sheet = new PdfSheet(pdf, page, file.getPath(), file.getName());
        		sheet.addObserver(this);
        		sheets.add(sheet);
        	}
        } else {
        	// If it's an image, just add it as a sheet
        	Sheet sheet = new Sheet(file.getPath(), file.getName());
        	sheet.addObserver(this);
            sheets.add(sheet);
        }
    }
    
    public void importSheet(String fileName) throws IOException {
        this.importSheet(new File(fileName));
    }
    
    /**
     * Adds a single sheet to the project.
     * @param file File to add
     * @param page Page to take from a multi-page document. This parameter has no effect for single-page documents such as jpgs.
     * @return the imported sheet
     */
    public Sheet importSheet(String id, String fileName, int page) throws IOException {
    	File file = new File(fileName);
    	
    	Sheet sheet;
    	if (getFileExtension(fileName).equals("pdf")) {
    		// Do we already have this document?
        	PdfDocument pdf = pdfDocuments.get(file.getPath());
        	
        	// If not, then open it
        	if (pdf == null) {
        		pdf = new PdfDocument(file);
        		pdfDocuments.put(file.getPath(), pdf);
        	}
    		
    		sheet = new PdfSheet(pdf, page, file.getPath(), file.getName());
        } else {
        	sheet = new Sheet(file.getPath(), file.getName());
        }
    	
    	sheet.addObserver(this);
    	sheet.setId(id);
    	sheets.add(sheet);
    	
    	return sheet;
    }
        

    public void removeSheets(Sheet[] sheets) {
        for (Sheet sheet : sheets) {
        	sheet.deleteObserver(this);  // TODO: is this necessary?
        	this.sheets.remove(sheet);
        }
    }
    
    public AbstractList<Sheet> getSheets() {
        return this.sheets;
    }
    
    public Sheet getSheet(String sheetId) {
    	for (Sheet sheet : this.sheets) {
    		if (sheet.getId().equals(sheetId)) {
    			return sheet;
    		}
    	}
    	
    	return null;
    }

    private static String getFileExtension(String fileName) {
	    int index = fileName.lastIndexOf('.');
	    if (index > 0 && index < fileName.length() - 1) {
	        return fileName.substring(index + 1).toLowerCase();
	    } else {
	    	return "";
	    }
    }
    
    public void setRotation(int degrees) {
    	// FIXME: what if new sheets are added? 
    	
    	for (Sheet sheet : this.sheets) {
    		sheet.setRotation(degrees);
    	}
    }
    
    /**
     * Receives notifications from Sheets when they chance.
     */
    public void update(Observable source, Object event) {
    	if (source instanceof Sheet) {
    		// Relay forward (to SheetTableModel). Payload has the changed row.
    		setChanged();
            notifyObservers(this.sheets.indexOf(source));
    	}
    }
}
