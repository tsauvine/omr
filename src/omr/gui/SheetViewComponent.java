package omr.gui;

import javax.swing.JComponent;

public abstract class SheetViewComponent extends JComponent {
    private static final long serialVersionUID = 1L;
    
    protected SheetEditor sheetEditor;
    protected boolean isSelected;
    
    public SheetViewComponent(SheetEditor editor) {
        this.sheetEditor = editor;
    }
    
    public double getZoomLevel() {
        return sheetEditor.getZoomLevel();
    }
    
    /**
     * Updates the size and location of the component according to the zoom level.
     */
    public abstract void updateBounds();
    
    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }
    
    public boolean isSelected() {
        return this.isSelected;
    }

}
