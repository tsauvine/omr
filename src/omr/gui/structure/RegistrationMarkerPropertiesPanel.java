package omr.gui.structure;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import omr.RegistrationMarker;
import omr.Project;
import omr.gui.RegistrationComponent;
import omr.gui.UndoSupport;

/**
 * A panel where the properties of an registration marker can be edited.
 * 
 * @author Tapio Auvinen
 */
public class RegistrationMarkerPropertiesPanel extends JPanel implements Scrollable, ChangeListener, ActionListener, Observer {
    private static final long serialVersionUID = 1L;
    
    protected UndoSupport undoSupport;
    
    private Project project;
    private RegistrationComponent selectedComponent; 
    private AttributeEdit currentEdit;                // Current undo object. Changes are accumulated until another component is selected. 
    
    private JSpinner widthSpinner; 
    private JSpinner heightSpinner;
    private JSpinner searchRadiusSpinner;
    private MarkerImageComponent markerImage;
    
    public RegistrationMarkerPropertiesPanel() {
        this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Registration marker properties"));
        
        // GridBagLayout
        this.setLayout(new GridBagLayout());
        GridBagConstraints left = new GridBagConstraints();
        GridBagConstraints right = new GridBagConstraints();
        GridBagConstraints wide = new GridBagConstraints();
        left.gridx = 0; left.anchor = GridBagConstraints.LINE_END;
        right.gridx = 1; right.anchor = GridBagConstraints.LINE_START;
        wide.gridwidth = 2; wide.fill = GridBagConstraints.HORIZONTAL;


        // Width
        left.gridy = right.gridy = 0;
        this.add(new JLabel("Marker width:"), left);
        widthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 64, 1));
        widthSpinner.addChangeListener(this);
        this.add(widthSpinner, right);

        // Height
        left.gridy = right.gridy = 1;
        this.add(new JLabel("Marker height:"), left);
        heightSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 64, 1));
        heightSpinner.addChangeListener(this);
        this.add(heightSpinner, right);

        // Search radius
        left.gridy = right.gridy = 2;
        this.add(new JLabel("Search radius:"), left);
        searchRadiusSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 256, 1));
        searchRadiusSpinner.addChangeListener(this);
        this.add(searchRadiusSpinner, right);
        
        // Marker image
        left.gridy = right.gridy = 3;
        this.add(new JLabel("Marker image:"), left);
        markerImage = new MarkerImageComponent();
        this.add(markerImage, right);
    }
    
    public void setProject(Project project) {
        this.project = project;
    }
    
    public void setUndoSupport(UndoSupport undo) {
        this.undoSupport = undo;
    }
    
    /**
     * Sets a components as the only selected component.
     * @param component Component to be selected
     */
    public void setSelectedComponent(RegistrationComponent component) {
        // Unsubscribe from old marker
        if (this.selectedComponent != null) {
            this.selectedComponent.getRegistrationMarker().deleteObserver(this);
        }
        
        this.selectedComponent = component;
        this.fetchValues();
        
        // Create a new undo object on next edit
        this.currentEdit = null;
        
        // Subscribe to new marker
        if (this.selectedComponent != null) {
            this.selectedComponent.getRegistrationMarker().addObserver(this);
        }
    }
    
    /**
     * Fetches the values from selected components and updates the components.
     */
    private void fetchValues() {
        if (selectedComponent == null) {
            heightSpinner.setValue(1);
            widthSpinner.setValue(1);
            searchRadiusSpinner.setValue(1);
            markerImage.setMarker(null);
            return;
        }

        RegistrationMarker marker = selectedComponent.getRegistrationMarker();
        
        widthSpinner.setValue(marker.getImageWidth());
        heightSpinner.setValue(marker.getImageHeight());
        searchRadiusSpinner.setValue(marker.getSearchRadius());
        markerImage.setMarker(marker);
        
        markerImage.revalidate();
    }
    
    
    public void stateChanged(ChangeEvent event) {
        if (selectedComponent == null) {
            return;
        }
        
        // Create a new undo object if this is the first edit. Otherwise accumulate changes in the old object.
        if (currentEdit == null) {
            this.currentEdit = new AttributeEdit(selectedComponent);
            undoSupport.postEdit(currentEdit);
        }
        
        // Update registration marker parameters
        Object source = event.getSource();
        RegistrationMarker marker = selectedComponent.getRegistrationMarker();
        
        if (source == widthSpinner) {
        	marker.setImageWidth(((SpinnerNumberModel)widthSpinner.getModel()).getNumber().intValue());
        	marker.copyMarkerImage(project.getSheetStructure().getReferenceSheet());
        } else if (source == heightSpinner) {
        	marker.setImageHeight(((SpinnerNumberModel)heightSpinner.getModel()).getNumber().intValue());
        	marker.copyMarkerImage(project.getSheetStructure().getReferenceSheet());
        } else if (source == searchRadiusSpinner) {
        	marker.setSearchRadius(((SpinnerNumberModel)searchRadiusSpinner.getModel()).getNumber().intValue());
        }
        
        // Update undo object to support redo
        currentEdit.updateAttributes();
        
        selectedComponent.updateBounds();
        selectedComponent.repaint();
    }
    
    public void actionPerformed(ActionEvent event) {
        
    }
    
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(Rectangle arg0, int arg1, int arg2) {
        return 32;
    }
    
    public int getScrollableBlockIncrement(Rectangle arg0, int arg1, int arg2) {
        return 128;
    }

    public boolean getScrollableTracksViewportHeight() {
        return true;
    }

    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    private class AttributeEdit extends AbstractUndoableEdit {
        private static final long serialVersionUID = 1L;
        
        private RegistrationComponent registrationComponent;
        private RegistrationMarker marker;
        
        private int previousWidth, previousHeight;
        private int previousSearchRadius;
        
        private int newWidth, newHeight;
        private int newSearchRadius;
        
        public AttributeEdit(RegistrationComponent registrationComponent) {
            this.registrationComponent = registrationComponent;
            this.marker = registrationComponent.getRegistrationMarker();
            
            previousWidth = marker.getImageWidth();
            previousHeight = marker.getImageHeight();
            previousSearchRadius = marker.getSearchRadius();
        }
        
        public void updateAttributes() {
            newWidth = marker.getImageWidth();
            newHeight = marker.getImageHeight();
            newSearchRadius = marker.getSearchRadius();
        }
        
        public void undo() throws CannotUndoException {
            super.undo();
            
            marker.setImageWidth(previousWidth);
            marker.setImageHeight(previousHeight);
            marker.setSearchRadius(previousSearchRadius);
            
            registrationComponent.repaint();
            fetchValues();
        }

        public void redo() throws CannotRedoException {
            super.redo();
            
            marker.setImageWidth(newWidth);
            marker.setImageHeight(newHeight);
            marker.setSearchRadius(newSearchRadius);
            
            registrationComponent.repaint();
            fetchValues();
        }

        public String getPresentationName() {
            return "edit registration marker";
        }

    }
    
    private class MarkerImageComponent extends JComponent {
    	private static final long serialVersionUID = 1L;
    	private RegistrationMarker marker;
    	
    	public MarkerImageComponent() {
    	}
    	
    	public void setMarker(RegistrationMarker marker) {
    		this.marker = marker;
    	}
    	
    	protected void paintComponent(Graphics g) {
    		if (marker == null) {
    			return;
    		}
    		
            BufferedImage buffer = marker.getBuffer();
    		
            if (buffer == null) {
            	g.setColor(Color.BLUE);
            	g.fillRect(0, 0, getWidth(), getHeight());
            	return;
            }
            
            g.drawImage(buffer, 0, 0, null);
        }
    	
    	public Dimension getPreferredSize() {
    		if (marker == null) {
    			return new Dimension(32, 32);
    		}
    		
            BufferedImage buffer = marker.getBuffer();
            if (buffer == null) {
    			return new Dimension(32, 32);
    		}
            
    		return new Dimension(buffer.getWidth(), buffer.getHeight());
        }
    }

    /**
     * Receives notification from RegistrationMarkers when they change
     */
    public void update(Observable source, Object arg1) {
        if (source instanceof RegistrationMarker) {
            markerImage.revalidate();  // Update component size
            markerImage.repaint();
        }
    }
}
