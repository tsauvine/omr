package omr.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JComponent;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import omr.Sheet;

public class SheetEditor extends JComponent implements Scrollable, Observer {
    private static final long serialVersionUID = 1L;
    
    public enum Tool { SELECT, QUESTION, REGISTRATION }
    
    protected Tool currentTool;
    protected double zoomLevel;
    protected Sheet sheet;                        // Currently displayed sheet. May be null.
    protected BufferedImage sheetBuffer;          // Local copy of the current sheet image at current zoom level.
    
    protected UndoSupport undoSupport;
    
    public SheetEditor() {
        this.zoomLevel = 1.0;

        setAutoscrolls(true);          // scroll when dragging
        setOpaque(true);
    }
    
    public void setUndoSupport(UndoSupport undo) {
        this.undoSupport = undo;
    }
    
    public void setCurrentTool(Tool tool) {
        this.currentTool = tool;
    }
    
    public Tool getCurrentTool() {
        return this.currentTool;
    }
    
    /**
     * Sets the sheet to be displayed. Set to null to display nothing.
     */
    public void setSheet(Sheet sheet) {
        // Unsubscribe from previous sheet
        if (this.sheet != null) {
            this.sheet.deleteObserver(this);
        }
        
        this.sheet = sheet;
        
        if (sheet == null) {
            sheetBuffer = null;
            revalidate();  // Needed to reset the scroll bars
            return;
        }
        
        // Subscribe to the new sheet
        this.sheet.addObserver(this);
        
        this.updateBuffer();
    }
    
    public void updateBuffer() {
    	// FIXME: this shouldn't be public. We could use the observer pattern here. 
    	
        try {
            sheetBuffer = this.getSheetBuffer();
        } catch (OutOfMemoryError e) {
            sheetBuffer = null;
            // TODO: display better error message in the view
            // -Xms32m -Xmx2048m
            System.err.println("Unable to render sheet image. Out of memory.");
        } catch (IOException e) {
            sheetBuffer = null;
            System.err.println("Unable to render sheet image.\n" + e);
        }

        // Update children
        for (Component child : getComponents()) {
            ((SheetViewComponent)child).updateBounds();
        }
        
        super.revalidate();  // Needed to reset the scroll bars
        repaint();
    }
    
    protected BufferedImage getSheetBuffer() throws OutOfMemoryError, IOException {
        if (this.sheet == null) {
            return null;
        }
        
        return sheet.getUnalignedBuffer(zoomLevel);
    }
    
    /**
     * Returns the currently displayed sheet.
     */
    public Sheet getSheet() {
    	return this.sheet;
    }
    
    public void setZoomLevel(double zoomLevel) {
        this.zoomLevel = zoomLevel;

        // Update view
        this.updateBuffer();
    }
    
    
    public double getZoomLevel() {
        return this.zoomLevel;
    }
    
    public void setSelectedComponent(SheetViewComponent component) {
    }
    
    public void addSelectedComponent(SheetViewComponent component) {
    }

    protected void paintComponent(Graphics g) {
        if (sheet == null) {
            // No sheet selected
            g.setColor(Color.GRAY);                                                  
            g.fillRect(0, 0, getWidth(), getHeight());
        } else if (sheetBuffer == null) {
            // Unable to get sheet image
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.BLACK);
            g.drawString("Unable to render sheet image", 10, 20); 
        } else {
            // Paint active sheet
            g.drawImage(sheetBuffer, 0, 0, null);
        }
    }
    
    public Dimension getPreferredSize() {
        if (sheet == null) {
            return new Dimension(1, 1);
        } else if (sheetBuffer == null) {
            return new Dimension(256, 414);
        } else {
            return new Dimension(sheetBuffer.getWidth(), sheetBuffer.getHeight());
        }
    }

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 32;
    }
    
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.HORIZONTAL) {
            return getWidth() / 4;
        } else {
            return getHeight() / 4;
        }
    }
    
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    public void update(Observable source, Object event) {
    }

}
