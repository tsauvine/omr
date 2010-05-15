package omr.gui;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import omr.RegistrationMarker;
import omr.gui.SheetEditor.Tool;

public class RegistrationComponent extends SheetViewComponent implements MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;

    private UndoSupport undoSupport;
    
    private RegistrationMarker marker;
    private Point dragStartPoint;
    private Point originalPosition;     // Original position when dragging
    
    public RegistrationComponent(SheetEditor editor, RegistrationMarker marker) {
        super(editor);
        
        setOpaque(false);
        setAutoscrolls(true);
        addMouseMotionListener(this);
        addMouseListener(this);
        
        this.marker = marker;
        
        updateBounds();
    }

    public void setUndoSupport(UndoSupport undo) {
        this.undoSupport = undo;
    }
    
    public RegistrationMarker getRegistrationMarker() {
        return this.marker;
    }
    
    @Override
    public void updateBounds() {
        double zoom = getZoomLevel();
        
        int x = (int)((marker.getX() - marker.getImageWidth() / 2.0 - marker.getSearchRadius()) * zoom); 
        int y = (int)((marker.getY() - marker.getImageHeight() / 2.0 - marker.getSearchRadius()) * zoom);
        
        int width = (int)((marker.getImageWidth() + 2 * marker.getSearchRadius()) * zoom);
        int height = (int)((marker.getImageHeight() + 2 * marker.getSearchRadius()) * zoom);
        
        // Calculate position and size according to current zoom level
        this.setBounds(x, y, width, height);
    }

    protected void paintComponent(Graphics g) {
        double zoom = getZoomLevel();
        
        int markerX = (int)(getWidth() / 2.0 - marker.getImageWidth() * zoom / 2);
        int markerY = (int)(getHeight() / 2.0 - marker.getImageHeight() * zoom / 2);
        int markerWidth = (int)(marker.getImageWidth() * zoom) - 1;
        int markerHeight = (int)(marker.getImageHeight() * zoom) - 1;
        
        // Selection
        if (isSelected) {
            g.setColor(new Color(0.5f, 0.5f, 1.0f, 0.5f));
            g.fillRect(markerX, markerY, markerWidth, markerHeight);
        }
        
        // Draw search radius
        g.setColor(Color.GRAY);
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

        // Draw marker boundary
        g.setColor(Color.BLUE);
        g.drawRect(markerX, markerY, markerWidth, markerHeight);

        // Draw crosshair
        //g.drawLine(middleX, 0, middleX, getHeight() - 1);
        //g.drawLine(0, middleY, getWidth() - 1, middleX);
    }
    
    public void mouseClicked(MouseEvent arg0) {
    }

    public void mouseEntered(MouseEvent arg0) {
    }

    public void mouseExited(MouseEvent arg0) {
    }

    public void mousePressed(MouseEvent event) {
        
        if (event.getButton() == MouseEvent.BUTTON1 && (sheetEditor.getCurrentTool() == Tool.SELECT || sheetEditor.getCurrentTool() == Tool.REGISTRATION)) {
        	// Select
            if (event.isControlDown()) {
                sheetEditor.addSelectedComponent(this);
            } else {
                sheetEditor.setSelectedComponent(this);
            }
        
            // Start move
        	this.dragStartPoint = event.getPoint();
        	dragStartPoint.translate(getX(), getY()); // Use parent coordinate system because local coordinate system will be dragged.
        	this.originalPosition = new Point(marker.getX(), marker.getY()); 
        }
    }

    public void mouseDragged(MouseEvent event) {
    	 double zoomLevel = getZoomLevel();
         
         // Autoscroll
         Rectangle r = new Rectangle(event.getX(), event.getY(), 1, 1);
         scrollRectToVisible(r);

         if (dragStartPoint != null) {
	         // Convert coordinates to parent coordinate system because local coordinate system is being dragged.
	         event.translatePoint(getX(), getY());
	         
	         double dx = (event.getX() - dragStartPoint.getX()) / zoomLevel;
	         double dy = (event.getY() - dragStartPoint.getY()) / zoomLevel;
	         
	         // Move the whole thing
	         marker.setX((int)(originalPosition.getX() + dx));
	         marker.setY((int)(originalPosition.getY() + dy));
	
	         updateBounds();
         }
    }
    
    public void mouseReleased(MouseEvent event) {
        if (originalPosition != null) {
        	// Save state for undo
            undoSupport.postEdit(new MoveRegistrationMarkerEdit(this, originalPosition));
            
            // Update marker image
            marker.copyMarkerImage(this.sheetEditor.getSheet());
        }
        
        this.dragStartPoint = null;
        this.originalPosition = null;
    }

    public void mouseMoved(MouseEvent arg0) {
    }

    private class MoveRegistrationMarkerEdit extends AbstractUndoableEdit {
        private static final long serialVersionUID = 1L;
        
        private RegistrationComponent registrationComponent;
        private Point originalPosition;
        private Point newPosition;
        
        public MoveRegistrationMarkerEdit(RegistrationComponent registrationComponent, Point originalPosition) {
            this.registrationComponent = registrationComponent;
            this.originalPosition = originalPosition;
            
            RegistrationMarker marker = registrationComponent.getRegistrationMarker();
            this.newPosition = new Point(marker.getX(), marker.getY());
        }

        public void undo() throws CannotUndoException {
            super.undo();
            
            RegistrationMarker marker = registrationComponent.getRegistrationMarker();
            marker.setX((int)originalPosition.getX());
            marker.setY((int)originalPosition.getY());
            registrationComponent.updateBounds();
        }

        public void redo() throws CannotRedoException {
            super.redo();
            
            RegistrationMarker marker = registrationComponent.getRegistrationMarker();
            marker.setX((int)newPosition.getX());
            marker.setY((int)newPosition.getY());
            registrationComponent.updateBounds();
        }

        public String getPresentationName() {
            return "move registration marker";
        }

    }
}
