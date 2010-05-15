package omr.gui.calibration;


import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.border.Border;

import omr.gui.ZoomPanel;

public class CalibrateToolBar extends JToolBar implements ActionListener {
    private static final long serialVersionUID = 1L;

    private SheetCalibrationEditor sheetView;
    
    //private JButton selectButton;
    
    private JButton selectedButton;
    
    private Border selectedBorder, emptyBorder;

    
    public CalibrateToolBar(SheetCalibrationEditor sheetView) {
        this.setFloatable(false);
        this.setRollover(true);
        
        this.sheetView = sheetView;
        this.selectedButton = null;
        
        // Create borders to indicate selected tool
        selectedBorder = BorderFactory.createLineBorder(Color.BLUE, 1);
        emptyBorder = BorderFactory.createEmptyBorder(1,1,1,1);
        
        // Create toolbar border
        this.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));

        // Zoom
        ZoomPanel zoomPanel = new ZoomPanel();
        zoomPanel.addActionListener(this);
        
        //this.addSeparator();
        this.add(zoomPanel);
    }
    
    public void actionPerformed(ActionEvent event) {
        JComponent source = (JComponent)event.getSource();
        
        // De-hilight previous button
        if (selectedButton != null) {
            selectedButton.setBorder(emptyBorder);
        }

        if (source instanceof ZoomPanel) {
            // Zoom level changed
            double zoomLevel = ((ZoomPanel)source).getCurrentZoomLevel();
            sheetView.setZoomLevel(zoomLevel);
        }
        
        // Hilight new button
        if (selectedButton != null) {
            selectedButton.setBorder(selectedBorder);
        }
    }
    
}
