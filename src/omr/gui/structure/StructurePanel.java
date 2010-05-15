package omr.gui.structure;


import java.awt.BorderLayout;
import java.util.AbstractList;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import omr.Project;
import omr.gui.RegistrationComponent;
import omr.gui.Gui;
import omr.gui.QuestionGroupComponent;
import omr.gui.SheetViewComponent;

/**
 * A panel for editing answer sheet structure. This is one of the top level tabs.
 * @author Tapio Auvinen
 */
public class StructurePanel extends JPanel implements ChangeListener {
    private static final long serialVersionUID = 1L;
    
    private SheetStructureEditor sheetEditor; 
    private JScrollPane sheetEditorScrollPane;
    
    private JScrollPane propertiesScroll;                                 // Scrollable view for the whole properties panel
    private JPanel propertiesPanel;                                       // Properties panel that contains structure properties and properties of the selected component (componentProperties)
    private StructurePropertiesPanel structureProperties;                 // Properties of the structure
    private QuestionGroupPropertiesPanel questionGroupProperties;         // Properties of the currently selected question group, to be put inside componentProperties  
    private RegistrationMarkerPropertiesPanel registrationMarkerProperties;     // Properties of the currently selected registration marker, to be put inside componentProperties
    
    public StructurePanel(Gui gui) {
        this.setLayout(new BorderLayout());
        
        // Sheet view in the middle
        sheetEditor = new SheetStructureEditor();
        sheetEditor.setUndoSupport(gui.getUndoSupport());
        sheetEditor.addSelectionListener(this);
        sheetEditorScrollPane = new JScrollPane(sheetEditor);

        // Properties on the right
        propertiesPanel = new JPanel();
        propertiesPanel.setLayout(new BoxLayout(propertiesPanel, BoxLayout.PAGE_AXIS));
        propertiesScroll = new JScrollPane(propertiesPanel);
        
        structureProperties = new StructurePropertiesPanel(sheetEditor);
        structureProperties.setUndoSupport(gui.getUndoSupport());
        propertiesPanel.add(structureProperties);
        
        questionGroupProperties = new QuestionGroupPropertiesPanel();
        questionGroupProperties.setUndoSupport(gui.getUndoSupport());
        registrationMarkerProperties = new RegistrationMarkerPropertiesPanel();
        registrationMarkerProperties.setUndoSupport(gui.getUndoSupport());

        this.add(sheetEditorScrollPane, BorderLayout.CENTER);
        this.add(propertiesScroll, BorderLayout.LINE_END);
        
        // Toolbar
        JToolBar toolbar = new StructureToolBar(sheetEditor);
        this.add(toolbar, BorderLayout.PAGE_START);
    }
    
    public void setProject(Project project) {
        sheetEditor.setSheetStructure(project.getSheetStructure());
        sheetEditor.setSheet(project.getSheetStructure().getReferenceSheet());
        
        structureProperties.setProject(project);
        registrationMarkerProperties.setProject(project);
    }
    
    /**
     * Listens for selection changes in the sheet editor.
     */
    public void stateChanged(ChangeEvent event) {
        Object source = event.getSource();
        
        if (source == sheetEditor) {
            AbstractList<SheetViewComponent> components = ((SheetStructureEditor)source).getSelectedComponents();
            
            if (components.size() == 1) {
                // Exactly one component is selected. Show properties panel.
                SheetViewComponent component = components.get(0);

                // Show question group properties
                if (component instanceof QuestionGroupComponent) {
                    questionGroupProperties.setSelectedComponent((QuestionGroupComponent)component);
                    propertiesPanel.removeAll();
                    propertiesPanel.add(structureProperties);
                    propertiesPanel.add(questionGroupProperties);
                }
                
                // Show registration marker properties
                if (component instanceof RegistrationComponent) {
                	registrationMarkerProperties.setSelectedComponent((RegistrationComponent)component);
                	propertiesPanel.removeAll();
                    propertiesPanel.add(structureProperties);
                    propertiesPanel.add(registrationMarkerProperties);
                }
            } else {
                // If no component or multiple components are selected, hide the component properties panel
            	propertiesPanel.removeAll();
            	propertiesPanel.add(structureProperties);
            }
            
            this.revalidate();
        }
    }
}
