package omr.gui.calibration;


import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import omr.Histogram;
import omr.Project;

public class HistogramComponent extends JComponent implements MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;
    
    private Project project;
    private Histogram histogram;
    private SheetCalibrationEditor sheetEditor;

    private int histogramViewMin;   // Minimum index to be shown 
    private int histogramViewMax;   // Maximum index to be shown
    
    private double barWidth;        // Width of one histogram bar in pixels
    private int blackThresholdX;
    private int whiteThresholdX;
    
    private Cursor defaultCursor;
    private Cursor moveCursor;
    private boolean dragBlackThreshold;
    private boolean dragWhiteThreshold;
    
    public HistogramComponent(SheetCalibrationEditor sheetEditor) {
        this.sheetEditor = sheetEditor;
        this.histogramViewMin = 0;
        this.histogramViewMax = 256;
        this.dragBlackThreshold = false;
        this.dragWhiteThreshold = false;
        
        setOpaque(true);
        addMouseMotionListener(this);
        addMouseListener(this);
        
        this.defaultCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        this.moveCursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
    }
    
    public void setProject(Project project) {
        this.project = project;
        this.setHistogram(project.getHistogram());
    }
    
    public void setHistogram(Histogram histogram) {
        this.histogram = histogram;
        calculateViewport();
        repaint();
    }
    
    private void calculateViewport() {
        Insets insets = getInsets();
        
        // Only show the part of histogram that has occurrences
        this.histogramViewMin = histogram.getMinIndex() - 1;
        this.histogramViewMax = histogram.getMaxIndex() + 2;
        
        if (this.histogramViewMin < 0) this.histogramViewMin = 0;
        if (this.histogramViewMax > 255) this.histogramViewMin = 255;
        
        // Calculate bar width
        int histogramInterval = histogramViewMax - histogramViewMin;
        if (histogramInterval < 1) {
            histogramInterval = 1;
        }
        
        this.barWidth = (double)(getWidth() - insets.left - insets.right) / histogramInterval;
        
        // Calculate threshold positions in the view
        this.blackThresholdX = (int)((histogram.getBlackThreshold() - histogramViewMin) * barWidth);
        this.whiteThresholdX = (int)((histogram.getWhiteThreshold() - histogramViewMin) * barWidth);
    }
    
    protected void paintComponent(Graphics g) {
        calculateViewport();  // TODO: this could probably be done less often

        Insets insets = getInsets();
        
        // Do not proceed if histogram is not set
        if (histogram == null) {
            return;
        }
        
        int[] histogramArray = histogram.getHistogram();
        BufferedImage[] examples = histogram.getExamples();
        
        // Calcualte dimensions
        int histogramTopY = insets.top;
        int histogramBottomY = getHeight() - insets.bottom - 28;
        int histogramLeftX = insets.left;
        int histogramRightX = getWidth() - insets.right;
        int histogramHeight = histogramBottomY - histogramTopY;
        int histogramWidth = histogramRightX - histogramLeftX;
        
        int histogramInterval = this.histogramViewMax - this.histogramViewMin;
        if (histogramInterval < 1) histogramInterval = 1; 
        double barWidth = (double)histogramWidth / histogramInterval;
        

        // Clear background
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(histogramLeftX, histogramBottomY, histogramWidth, getHeight() - histogramBottomY - insets.bottom);

        
        // Draw thresholds
        g.setColor(Color.GRAY);
        g.fillRect(histogramLeftX, histogramTopY, blackThresholdX, histogramHeight);                                      // Black
        g.setColor(new Color(1.0f, 0.5f, 0.5f));
        g.fillRect(histogramLeftX + blackThresholdX, histogramTopY, whiteThresholdX - blackThresholdX, histogramHeight);  // Uncertain
        g.setColor(Color.WHITE);
        g.fillRect(histogramLeftX + whiteThresholdX, histogramTopY, histogramWidth - whiteThresholdX, histogramHeight);   // White
        
        
        // Draw histogram
        g.setColor(Color.BLACK);
        int bubbleFreeX = 0;      // Next x coordinate that has free space to draw an example bubble
        
        double heightMultiplier = histogramHeight / Math.log(histogram.getMaxValue());
        
        for (int index = histogramViewMin; index < histogramViewMax; index++) {
            int x = histogramLeftX + (int)((index - histogramViewMin) * barWidth);  // Left edge of the bar
            
            // Draw the bar
            int barHeight;
            if (histogramArray[index] <= 0) {
                barHeight = 0;
            } else {
                barHeight = (int)(heightMultiplier * (Math.log(histogramArray[index]) + 1));
            }
            if (barHeight > histogramHeight) {
                barHeight = histogramHeight; 
            }
            
            g.fillRect(x, histogramBottomY - barHeight, (int)Math.ceil(barWidth), barHeight);
            
            // Draw the example bubble
            if (examples != null && examples[index] != null && x >= bubbleFreeX) {
                BufferedImage example = examples[index]; 
                //g.drawImage(example, x - (int)((example.getWidth() - barWidth) / 2), histogramBottomY + 4, null);
                g.drawImage(example, x, histogramBottomY + 4, null);
                bubbleFreeX = x + example.getWidth() + 1;
            }
        }
        
        // Draw index numbers
    	int blackThreshold = histogram.getBlackThreshold();
    	int whiteThreshold = histogram.getWhiteThreshold();
    	//g.setXORMode(Color.BLACK);
    	g.setColor(Color.LIGHT_GRAY);
    	g.drawString(Integer.toString(histogramViewMin), histogramLeftX, histogramBottomY - 1);
    	g.drawString(Integer.toString(histogramViewMax), histogramLeftX + (int)((histogramViewMax - histogramViewMin - 1) * barWidth), histogramBottomY - 1);
    	g.drawString(Integer.toString(blackThreshold), histogramLeftX + (int)((blackThreshold - histogramViewMin) * barWidth), histogramBottomY - 1);
    	g.drawString(Integer.toString(whiteThreshold), histogramLeftX + (int)((whiteThreshold - histogramViewMin) * barWidth), histogramBottomY - 1);
    	//g.setPaintMode();
        
        // Draw rectangle around the histogram
        g.drawRect(histogramLeftX, histogramTopY, histogramWidth, histogramHeight);
    }
    
    public void mouseMoved(MouseEvent event) {
        int x = event.getX() - getInsets().left;
        
        // Change cursor if over threshold
        if (Math.abs(x - this.blackThresholdX) < 10 || Math.abs(x - this.whiteThresholdX) < 10) {
            setCursor(moveCursor);
        } else {
            setCursor(defaultCursor);
        }
    }

    public void mousePressed(MouseEvent event) {
        int x = event.getX() - getInsets().left;
        
        // Start drag if over threshold
        if (Math.abs(x - this.blackThresholdX) < 10) {
            this.dragBlackThreshold = true;
        } else if (Math.abs(x - this.whiteThresholdX) < 10) {
            this.dragWhiteThreshold = true;
        }
    }
    
    public void mouseDragged(MouseEvent event) {
        if (histogram == null) {
            return;
        }
        
        // Convert coordinates to index
        int index = (int)((event.getX() - getInsets().left) / this.barWidth + histogramViewMin);
        
        // Drag threshhold
        if (this.dragBlackThreshold) {
            histogram.setBlackThreshold(index);
        } else if (this.dragWhiteThreshold) {
            histogram.setWhiteThreshold(index);
        }
        
        // Calculate positions in viewport
        this.blackThresholdX = (int)((histogram.getBlackThreshold() - histogramViewMin) * barWidth);
        this.whiteThresholdX = (int)((histogram.getWhiteThreshold() - histogramViewMin) * barWidth);
        
        repaint();  // TODO: only repaint the damaged region
    }
    
    public void mouseReleased(MouseEvent arg0) {
        this.dragBlackThreshold = false;
        this.dragWhiteThreshold = false;
        
        // Thresholds have been changed. Recalculate answers.
        
        if (histogram == project.getHistogram()) {
            // Editing global histogram
            project.calculateAnswers();
        } else {
            // Editing sheet histogram
            sheetEditor.calculateAnswers();
        }
        
        // TODO: undo
    }

    public Dimension getPreferredSize() {
        return new Dimension(256, 128);
    }

    public void mouseClicked(MouseEvent arg0) {
    }

    public void mouseEntered(MouseEvent arg0) {
    }

    public void mouseExited(MouseEvent arg0) {
    }

}
