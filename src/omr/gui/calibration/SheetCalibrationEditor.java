package omr.gui.calibration;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Observable;

import omr.RegistrationMarker;
import omr.Project;
import omr.QuestionGroup;
import omr.Sheet;
import omr.SheetStructure;
import omr.gui.SheetEditor;

/**
 * Shows the sheet with question groups. Filled, unfilled and uncertain bubbles are rendered differently.
 * User can override answers by clicking bubbles.
 */
public class SheetCalibrationEditor extends SheetEditor implements MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;
    
    private Project project;      // The model, never null.
    
    //private Tool currentTool;
    
    public SheetCalibrationEditor() {
        super();
        
        addMouseMotionListener(this);
        addMouseListener(this);
    }
    
    
    public void setProject(Project project) {
        this.project = project;
    }
    
    protected BufferedImage getSheetBuffer() throws OutOfMemoryError, IOException {
        if (this.sheet == null) {
            return null;
        }
        
        return sheet.getAlignedBuffer(zoomLevel);
    }
    
    protected void paintComponent(Graphics g) {
        // Draw sheet
    	super.paintComponent(g);
    	
    	if (sheet == null) {
    		return;
    	}
    	
    	double zoom = getZoomLevel();
    	
    	SheetStructure structure = project.getSheetStructure(); 
    	
    	// Draw registration markers
        for (RegistrationMarker marker : structure.getRegistrationMarkers()) {
        	Point markerLocation = sheet.getRegistrationMarkerLocation(marker);
        	
        	if (markerLocation == null) {
        		g.setColor(Color.RED);   // Marker location not known
        	} else {
        		g.setColor(Color.BLUE);  // Marker location known
        	}
        	
        	g.drawRect((int)((marker.getX() - marker.getImageWidth() / 2.0) * zoom),
                    (int)((marker.getY() - marker.getImageHeight() / 2.0) * zoom),
                    (int)(marker.getImageWidth() * zoom),
                    (int)(marker.getImageHeight() * zoom));
        }
        
        // Draw bubbles
        for (QuestionGroup group : structure.getQuestionGroups()) {
            int columnCount = group.getColumnCount();
            int rowCount = group.getRowCount();
            int bubbleWidth = (int)(group.getBubbleWidth() * zoom);
            int bubbleHeight = (int)(group.getBubbleHeight() * zoom);
            int xOffset = (int)((group.getLeftX() - group.getBubbleWidth() / 2.0) * zoom);
            int yOffset = (int)((group.getTopY() - group.getBubbleHeight() / 2.0) * zoom);
            double columnSpacing = columnCount <= 1 ? 0 : group.getWidth() * zoom / (columnCount - 1);
            double rowSpacing = rowCount <= 1 ? 0 : group.getHeight() * zoom / (rowCount - 1);
            
            for (int row = 0; row < rowCount; row++) {
                int bubbleTopY = (int)(row * rowSpacing) + yOffset;
                int bubbleBottomY = bubbleTopY + bubbleHeight;
                
                for (int col = 0; col < columnCount; col++) {
                    int bubbleLeftX = (int)(col * columnSpacing) + xOffset;
                    int bubbleRightX = bubbleLeftX + bubbleWidth;
                    
                    int answer = sheet.getAnswer(group, row, col);
                    int override = sheet.getAnswerOverride(group, row, col);
                    
                    if (answer < 0) {
                    	// Selected bubble
                    	g.setColor(Color.BLUE);
                        g.drawRect(bubbleLeftX, bubbleTopY, bubbleWidth, bubbleHeight);
                    } else if (answer > 0) {
                    	// Unselected bubble
                    	g.setColor(Color.GRAY);
                        
                        // Draw corners
                        g.drawLine(bubbleLeftX, bubbleTopY, bubbleLeftX + 2, bubbleTopY);
                        g.drawLine(bubbleLeftX, bubbleBottomY, bubbleLeftX + 2, bubbleBottomY);
                        g.drawLine(bubbleRightX - 2, bubbleTopY, bubbleRightX, bubbleTopY);
                        g.drawLine(bubbleRightX - 2, bubbleBottomY, bubbleRightX, bubbleBottomY);
                        g.drawLine(bubbleLeftX, bubbleTopY, bubbleLeftX, bubbleTopY + 2);
                        g.drawLine(bubbleRightX, bubbleTopY, bubbleRightX, bubbleTopY + 2);
                        g.drawLine(bubbleLeftX, bubbleBottomY - 2, bubbleLeftX, bubbleBottomY);
                        g.drawLine(bubbleRightX, bubbleBottomY - 2, bubbleRightX, bubbleBottomY);
                    } else {
                    	// Uncertain bubble
                        g.setColor(Color.RED);
                        g.drawRect(bubbleLeftX - 2, bubbleTopY - 2, bubbleWidth + 4, bubbleHeight + 4);
                        g.drawRect(bubbleLeftX, bubbleTopY, bubbleWidth, bubbleHeight);
                        
                        g.drawString("?", bubbleRightX + 5, bubbleBottomY); 
                    }
                    
                    if (override < 0) {
                    	g.setColor(Color.RED);
                    	g.drawString("F", bubbleRightX + 2, bubbleBottomY);
                    } else if (override > 0) {
                        g.setColor(Color.RED);
                        g.drawString("U", bubbleRightX + 2, bubbleBottomY);
                    }
                    
                    //g.drawRect(bubbleLeftX, bubbleTopY, bubbleWidth, bubbleHeight);
                }   
            }
        }
        
    }
    
    /**
     * Recalculates answers after thresholding has changed.
     */
    public void calculateAnswers() {
        for (QuestionGroup group : project.getSheetStructure().getQuestionGroups()) {
            sheet.calculateAnswers(group, sheet.getHistogram().getBlackThreshold(), sheet.getHistogram().getWhiteThreshold());
        }
    }
    
    /**
     * Called when sheet changes.
     */
    @Override
    public void update(Observable source, Object event) {
        if (source instanceof Sheet) {
            repaint();
        }
    }

    public void mousePressed(MouseEvent event) {
    	if (sheet == null) {
    		return;
    	}
    	
    	int x = (int)(event.getX() / zoomLevel);
    	int y = (int)(event.getY() / zoomLevel);
    	
    	// Find out which bubble was clicked
    	for (QuestionGroup group : project.getSheetStructure().getQuestionGroups()) {
    		if (x < group.getLeftX() - group.getBubbleWidth()
    				|| x > group.getLeftX() + group.getWidth() + group.getBubbleWidth()
    				|| y < group.getTopY() - group.getBubbleHeight()
    				|| y > group.getTopY() + group.getHeight() + group.getBubbleHeight()) {
    			continue;
    		}
    		
    		int localX = x - group.getLeftX();
    		int localY = y - group.getTopY();
    		int row = (int)Math.round(localY / ((double)group.getHeight() / (group.getRowCount() - 1)));
    		int col = (int)Math.round(localX / ((double)group.getWidth() / (group.getColumnCount() - 1)));
    		
    		// Check bounds
    		if (row < 0) row = 0;
    		if (col < 0) col = 0;
    		if (row >= group.getRowCount()) row = group.getRowCount() - 1;
    		if (col >= group.getColumnCount()) col = group.getColumnCount() - 1;
    		
    		sheet.toggleAnswer(group, row, col);

    		repaint();
    		
    		break;
    	}
    	
    }
    
    public void mouseReleased(MouseEvent event) {
    }

    public void mouseDragged(MouseEvent event) {
        // Autoscroll
        Rectangle r = new Rectangle(event.getX(), event.getY(), 1, 1);
        scrollRectToVisible(r);
    }

    public void mouseMoved(MouseEvent event) {
    }


    public void mouseClicked(MouseEvent arg0) {
    }

    public void mouseEntered(MouseEvent arg0) {
    }

    public void mouseExited(MouseEvent arg0) {
    }

}
