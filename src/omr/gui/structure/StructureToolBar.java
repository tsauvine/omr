package omr.gui.structure;


import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.border.Border;

import omr.gui.Gui;
import omr.gui.ZoomPanel;
import omr.gui.SheetEditor.Tool;

public class StructureToolBar extends JToolBar implements ActionListener {
    private static final long serialVersionUID = 1L;

    private SheetStructureEditor sheetView;
    
    private JButton selectButton;
    private JButton questionsButton;
    private JButton registrationButton;
    
    private JButton selectedButton;   // Currently active tool
    
    private Border selectedBorder, emptyBorder;

    
    public StructureToolBar(SheetStructureEditor sheetView) {
        this.setFloatable(false);
        this.setRollover(true);
        
        this.sheetView = sheetView;
        
        // Create borders to indicate selected tool
        selectedBorder = BorderFactory.createLineBorder(Color.BLUE, 1);
        emptyBorder = BorderFactory.createEmptyBorder(1,1,1,1);
        
        // Create toolbar border
        this.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));

        // Zoom
        ZoomPanel zoomPanel = new ZoomPanel();
        zoomPanel.addActionListener(this);
        
        // Select
        selectButton = makeToolbarButton("select.png", "select", "Select", "Select");
        
        // Question group
        questionsButton = makeToolbarButton("questions.png", "question", "Add question groups", "Questions");
        
        // Registration
        registrationButton = makeToolbarButton("align.png", "registration", "Add registration markers", "Registration");
        
        this.selectedButton = selectButton;
        sheetView.setCurrentTool(Tool.SELECT);
        
        this.add(selectButton);
        this.add(questionsButton);
        this.add(registrationButton);
        this.addSeparator();
        this.add(zoomPanel);
    }
    
    
    protected JButton makeToolbarButton(String imageName, String actionCommand, String toolTipText, String altText) {
        String imgPath = "/" + imageName;
        URL imageURL = Gui.class.getResource(imgPath);

        JButton button = new JButton();
        button.setActionCommand(actionCommand);
        button.setToolTipText(toolTipText);
        button.addActionListener(this);
        button.setBorder(emptyBorder);

        if (imageURL != null) {
            button.setIcon(new ImageIcon(imageURL, altText));
        } else {
            button.setText(altText);
        }

        return button;
    }
    
    public void actionPerformed(ActionEvent event) {
        JComponent source = (JComponent)event.getSource();
        String command = event.getActionCommand();
        
        // De-hilight previous button
        if (selectedButton != null) {
            selectedButton.setBorder(emptyBorder);
        }

        if (source instanceof ZoomPanel) {
            // Zoom level changed
            double zoomLevel = ((ZoomPanel)source).getCurrentZoomLevel();
            sheetView.setZoomLevel(zoomLevel);
        } else if(command != null) {
            // Tool buttons
            if (command.equals("select")) {
                selectedButton = selectButton;
                sheetView.setCurrentTool(Tool.SELECT);
            } else if (command.equals("question")) {
                selectedButton = questionsButton;
                sheetView.setCurrentTool(Tool.QUESTION);
            } else if (command.equals("registration")) {
                selectedButton = registrationButton;
                sheetView.setCurrentTool(Tool.REGISTRATION);
            }
        }
        
        // Hilight new button
        if (selectedButton != null) {
            selectedButton.setBorder(selectedBorder);
        }
    }
    
}
