package omr.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

public class ZoomPanel extends JPanel implements ActionListener {
    private static final long serialVersionUID = 1L;
    
    private LinkedList<ActionListener> listeners;
    private JButton zoomInButton;
    private JButton zoomOutButton;
    private JComboBox zoomDropdown;
    private ZoomLevel[] zoomLevels;
    private double currentZoomLevel;
    private final double minZoomLevel = .1;
    private final double maxZoomLevel = 10;
    
    /**
     * Constructor
     */
    public ZoomPanel() {
        this.setLayout(new BorderLayout());
        this.listeners = new LinkedList<ActionListener>();
        this.currentZoomLevel = 1;
        
        zoomInButton = new JButton("+");
        zoomInButton.addActionListener(this);
        
        zoomOutButton = new JButton("-");
        zoomOutButton.addActionListener(this);
        
        zoomLevels = new ZoomLevel[5]; 
        zoomLevels[0] = new ZoomLevel(.25);
        zoomLevels[1] = new ZoomLevel(.5);
        zoomLevels[2] = new ZoomLevel(1);
        zoomLevels[3] = new ZoomLevel(1.5);
        zoomLevels[4] = new ZoomLevel(2);
        
        zoomDropdown = new JComboBox(zoomLevels);
        zoomDropdown.setEditable(true);
        zoomDropdown.setSelectedIndex(2);
        zoomDropdown.addActionListener(this);
        
        this.add(zoomOutButton, BorderLayout.LINE_START);
        this.add(zoomDropdown, BorderLayout.CENTER);
        this.add(zoomInButton, BorderLayout.LINE_END);
    }
    
    /**
     * Returns the current zoom level. 1.0 means 100%
     */
    public double getCurrentZoomLevel() {
        return currentZoomLevel;
    }
    
    /**
     * Adds an action listener that will be notified when the zoom level changes.
     */
    public void addActionListener(ActionListener listener) {
        if (listener == null) {
            return;
        }
        
        this.listeners.add(listener);
    }
    
    /**
     * Notifies listeners that the zoom level has changed.
     */
    private void notifyListeners() {
        for (ActionListener listener : this.listeners) {
            listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
        }
    }
    
    /**
     * Event listener for zoom buttons and the dropdown.
     */
    public void actionPerformed(ActionEvent event) {
        JComponent source = (JComponent)event.getSource();
        
        if (source == zoomInButton) {
            // Find the next zoom level
            for (int i = 0; i < zoomDropdown.getItemCount(); i++) {
                double level = ((ZoomLevel)zoomDropdown.getItemAt(i)).getZoomLevel();
                if (level > currentZoomLevel) {
                    zoomDropdown.setSelectedIndex(i);
                    currentZoomLevel = level;
                    break;
                }
            }
        }
        else if (source == zoomOutButton) {
            // Find the previous zoom level
            for (int i = zoomDropdown.getItemCount() - 1; i >= 0; i--) {
                double level = ((ZoomLevel)zoomDropdown.getItemAt(i)).getZoomLevel();
                if (level < currentZoomLevel) {
                    zoomDropdown.setSelectedIndex(i);
                    currentZoomLevel = level;
                    break;
                }
            }
        }
        if (source == zoomDropdown) {
            JComboBox dropdown = (JComboBox)source;
            Object zoomLevel = dropdown.getSelectedItem();
            
            if (zoomLevel instanceof ZoomLevel) {
                // User selected existing zoom level
                currentZoomLevel = ((ZoomLevel)zoomLevel).getZoomLevel();
            } else {
                // User eneterd custom zoom level
                try {
                    double level = Double.parseDouble((String)zoomLevel);
                    currentZoomLevel = level / 100.0;
                } catch (NumberFormatException exception) {
                }
            }
        }
        
        // Check limits
        if (currentZoomLevel > maxZoomLevel) {
            currentZoomLevel = maxZoomLevel;
        } else if (currentZoomLevel < minZoomLevel) {
            currentZoomLevel = minZoomLevel;
        }
        
        // Inform listeners
        notifyListeners();
    }

    private class ZoomLevel {
        private double level;
        
        public ZoomLevel(double level) {
            this.level = level;
        }
        
        public double getZoomLevel() {
            return this.level;
        }
        
        public String toString() {
            return (int)(level * 100) + " %"; 
        }
    }
}
