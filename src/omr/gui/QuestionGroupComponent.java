package omr.gui;


import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import omr.QuestionGroup;
import omr.gui.SheetEditor.Tool;

public class QuestionGroupComponent extends SheetViewComponent implements MouseListener, MouseMotionListener {
    
    private static final long serialVersionUID = 1L;

    private static final int CENTER = 0;
    private static final int NW = 1;
    private static final int N = 2;
    private static final int NE = 3;
    private static final int W = 4;
    private static final int E = 5;
    private static final int SW = 6;
    private static final int S = 7;
    private static final int SE = 8;
    
    int cursors[] = {
            Cursor.MOVE_CURSOR,
            Cursor.NW_RESIZE_CURSOR,
            Cursor.N_RESIZE_CURSOR,
            Cursor.NE_RESIZE_CURSOR,
            Cursor.W_RESIZE_CURSOR,
            Cursor.E_RESIZE_CURSOR,
            Cursor.SW_RESIZE_CURSOR,
            Cursor.S_RESIZE_CURSOR,
            Cursor.SE_RESIZE_CURSOR,
    }; 
    
    private UndoSupport undoSupport;
    
    private QuestionGroup group;
    private Rectangle[] regions;     // Resize regions
    private int leftMargin;          // Distance between the left edge of the component and the left edge of the first bubble column
    private int topMargin;           // Distance between the top edge of the component and the top edge of the first bubble row
    private int rightMargin;         // Distance between the right edge of the component and the tight edge of the last bubble row
    private int bottomMargin;        // Distance between the bottom edge of the component and the bottom edge of the last bubble row
    //private Font font;
    
    private int resizeMode;
    private Point dragStartPoint;
    private Rectangle originalGroupRect;  // Original bounds when resizing
    
    
    public QuestionGroupComponent(SheetEditor editor, QuestionGroup group) {
        super(editor);

        setOpaque(false);
        setAutoscrolls(true);
        this.regions = new Rectangle[9];
        this.resizeMode = -1;
        //this.zoomLevel = 1.0;
        
        addMouseMotionListener(this);
        addMouseListener(this);

        this.group = group;
        
        // Create font
        //this.font = new Font("Dialog", Font.PLAIN, 12);
        this.leftMargin = 24;
        this.topMargin = 20;
        this.bottomMargin = 20;
        this.rightMargin = 0;
        
        updateBounds();
    }
    
    public void setUndoSupport(UndoSupport undo) {
        this.undoSupport = undo;
    }
    
    public QuestionGroup getQuestionGroup() {
        return this.group;
    }
    
    
    public void mousePressed(MouseEvent event) {
        Point point = event.getPoint();
        
        if (event.getButton() == MouseEvent.BUTTON1 && (sheetEditor.getCurrentTool() == Tool.SELECT || sheetEditor.getCurrentTool() == Tool.QUESTION)) {
        	// Select
            if (event.isControlDown()) {
                sheetEditor.addSelectedComponent(this);
            } else {
                sheetEditor.setSelectedComponent(this);
            }
        
        	// Start resize
            // Find out which edge whe are grabbing
            for (int i = 0; i < regions.length; i++) {
                Rectangle rect = regions[i]; 
                if (rect.contains(point)) {
                    this.resizeMode = i;
                    this.dragStartPoint = point;
                    dragStartPoint.translate(getX(), getY()); // Use parent coordinate system because local coordinate system will be dragged.
                    this.originalGroupRect = new Rectangle(group.getLeftX(), group.getTopY(), group.getWidth(), group.getHeight()); 
                    return;
                }
            }
        }
    }
    
    public void mouseDragged(MouseEvent event) {
        double zoomLevel = getZoomLevel();
        
        // Autoscroll
        Rectangle r = new Rectangle(event.getX(), event.getY(), 1, 1);
        scrollRectToVisible(r);

        if (dragStartPoint == null) {
        	return;
        }
        
        // Convert coordinates to parent coordinate system because local coordinate system is being dragged.
        event.translatePoint(getX(), getY());
        
        double originalLeftX = originalGroupRect.getX();
        double originalRightX = originalGroupRect.getX() + originalGroupRect.getWidth();
        double originalTopY = originalGroupRect.getY();
        double originalBottomY = originalGroupRect.getY() + originalGroupRect.getHeight();
        double dx = (event.getX() - dragStartPoint.getX()) / zoomLevel;
        double dy = (event.getY() - dragStartPoint.getY()) / zoomLevel;
        
        // Move the whole thing
        if (resizeMode == CENTER) {
            group.setTopY((int)(originalTopY + dy));
            group.setBottomY((int)(originalBottomY + dy));
            group.setLeftX((int)(originalLeftX + dx));
            group.setRightX((int)(originalRightX + dx));
        }

        // Move top edge
        if (resizeMode == NE || resizeMode == N || resizeMode == NW) {
            group.setTopY((int)(originalTopY + dy));
        }
        
        // Move bottom edge
        if (resizeMode == SE || resizeMode == S || resizeMode == SW) {
            group.setBottomY((int)(originalBottomY + dy));
        }
        
        // Move right edge
        if (resizeMode == NW || resizeMode == W || resizeMode == SW) {
            group.setLeftX((int)(originalLeftX + dx));
        }
        
        // Move right edge
        if (resizeMode == NE || resizeMode == E || resizeMode == SE) {
            group.setRightX((int)(originalRightX + dx));
        }

        // Update component size and location
        if (resizeMode >= 0) {
            updateBounds();
        }
    }
    
    public void mouseReleased(MouseEvent event) {
        // Save state for undo
        if (originalGroupRect != null) {
            undoSupport.postEdit(new ResizeQuestionGroupEdit(this, originalGroupRect));
        }
        
        this.resizeMode = -1;
        this.dragStartPoint = null;
        this.originalGroupRect = null;
    }

    public void mouseMoved(MouseEvent event) {
        Point point = event.getPoint();
        
        // Update cursor
        for (int i = 0; i < regions.length; i++) {
            Rectangle rect = regions[i];
            if (rect.contains(point)) {
                setCursor(Cursor.getPredefinedCursor(cursors[i])); 
            }
        }
    }
    
    public void mouseClicked(MouseEvent event) {
        
    }

    public void mouseEntered(MouseEvent arg0) {
    }

    public void mouseExited(MouseEvent arg0) {
    }

    /**
     * Sets component location and size according to the underlying model and zoomLevel.
     */
    @Override
    public void updateBounds() {
        double zoomLevel = getZoomLevel();
        
        // Calculate position and size according to current zoom level
        this.setBounds((int)((group.getLeftX() - group.getBubbleWidth() / 2) * zoomLevel) - leftMargin,
                (int)((group.getTopY() - group.getBubbleHeight() / 2) * zoomLevel) - topMargin,
                (int)((group.getWidth() + group.getBubbleWidth()) * zoomLevel) + 1 + leftMargin + rightMargin,
                (int)((group.getHeight() + group.getBubbleHeight()) * zoomLevel) + 1 + topMargin + bottomMargin);
        
        // Update resize regions
        updateResizeRegions();
    }
    
    /**
     * Positions resize regions to the edges of the component.
     */
    private void updateResizeRegions() {
        double zoomLevel = getZoomLevel();
        
        int width = getWidth();
        int height = getHeight();
        int xInset = Math.min((int)(24 * zoomLevel), width / 2);
        int yInset = Math.min((int)(24 * zoomLevel), height / 2);
        
        regions[CENTER] = new Rectangle(xInset, yInset, width - 2 * xInset, height - 2 * yInset);  // Center
        regions[NW] = new Rectangle(             0, 0,             xInset, yInset);  // NW
        regions[N]  = new Rectangle(        xInset, 0, width - 2 * xInset, yInset);  // N
        regions[NE] = new Rectangle(width - xInset, 0,             xInset, yInset);  // NE
        regions[W]  = new Rectangle(             0, yInset, xInset, height - 2 * yInset);  // W
        regions[E]  = new Rectangle(width - xInset, yInset, xInset, height - 2 * yInset);  // E
        regions[SW] = new Rectangle(             0, height - yInset,             xInset, yInset);  // SW
        regions[S]  = new Rectangle(        xInset, height - yInset, width - 2 * xInset, yInset);  // S
        regions[SE] = new Rectangle(width - xInset, height - yInset,             xInset, yInset);  // SE
    }
    
    protected void paintComponent(Graphics g) {
        double zoomLevel = getZoomLevel();
        
        // Graphics2D g2 = (Graphics2D)g;
        if (group == null) {
            System.err.println("Orphan question group component.");
            return;
        }
        
        int columnCount = group.getColumnCount();
        int rowCount = group.getRowCount();
        
        int bubbleWidth = (int)(group.getBubbleWidth() * zoomLevel);
        int bubbleHeight = (int)(group.getBubbleHeight() * zoomLevel);
        int halfBubbleWidth = (int)(group.getBubbleWidth() * zoomLevel / 2);
        int halfBubbleHeight = (int)(group.getBubbleHeight() * zoomLevel / 2);
        
        double columnSpacing = columnCount <= 1 ? 0 : group.getWidth() * zoomLevel / (columnCount - 1);
        double rowSpacing = rowCount <= 1 ? 0 : group.getHeight() * zoomLevel / (rowCount - 1);
        
        // Selection
        if (isSelected) {
            g.setColor(new Color(0.5f, 0.5f, 1.0f, 0.5f));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        
        // Draw question numbers
        for (int row = 0; row < group.getRowCount(); row++) {
            int y = (int)(row * rowSpacing) + topMargin + halfBubbleHeight + 5;
            g.drawString(group.getRowLabel(row), 0, y);
        }
        
        // Draw alternatives
        //char letter = 'A';
        for (int col = 0; col < group.getColumnCount(); col++) {
            int x = (int)(col * columnSpacing) + leftMargin + halfBubbleWidth - 4;
            g.drawString(group.getColumnLabel(col), x, topMargin - 6);
            //letter++;
        }
        
        // Draw bubbles
        g.setColor(Color.BLUE);
        for (int row = 0; row < rowCount; row++) {
            int bubbleY = (int)(row * rowSpacing) + topMargin;
            
            for (int col = 0; col < columnCount; col++) {
                int bubbleX = (int)(col * columnSpacing) + leftMargin;
                
                g.drawRect(bubbleX, bubbleY, bubbleWidth, bubbleHeight);
            }   
        }
    }
    
    private class ResizeQuestionGroupEdit extends AbstractUndoableEdit {
        private static final long serialVersionUID = 1L;
        
        private QuestionGroupComponent groupComponent;
        private Rectangle originalRect;
        private Rectangle newRect;
        
        public ResizeQuestionGroupEdit(QuestionGroupComponent groupComponent, Rectangle originalRect) {
            this.groupComponent = groupComponent;
            this.originalRect = originalRect;
            
            QuestionGroup group = groupComponent.getQuestionGroup();
            this.newRect = new Rectangle(group.getLeftX(), group.getTopY(), group.getWidth(), group.getHeight());
        }

        public void undo() throws CannotUndoException {
            super.undo();
            groupComponent.getQuestionGroup().setBounds(originalRect);
            groupComponent.updateBounds();
        }

        public void redo() throws CannotRedoException {
            super.redo();
            groupComponent.getQuestionGroup().setBounds(newRect);
            groupComponent.updateBounds();
        }

        public String getPresentationName() {
            return "resize question group";
        }

    }
    
}
