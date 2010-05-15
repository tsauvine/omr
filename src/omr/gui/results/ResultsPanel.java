package omr.gui.results;


import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import omr.Project;
import omr.gui.Gui;

public class ResultsPanel extends JPanel implements ChangeListener {
    private static final long serialVersionUID = 1L;
    
    private JTable table;  // Result table
    private ResultsTableModel resultsTableModel;
    
    public ResultsPanel(Gui gui) {
        this.setLayout(new BorderLayout());
        
        // Add widgets
        resultsTableModel = new ResultsTableModel();
        table = new JTable(resultsTableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);
        this.add(scrollPane, BorderLayout.CENTER);
    }
    
    public void setProject(Project project) {
        resultsTableModel.setProject(project);
    }
    
    /**
     * Called when tab is switched.
     */
    public void stateChanged(ChangeEvent event) {
        JTabbedPane tabs = (JTabbedPane)event.getSource();
        
        // Switched to this tab?        
        if (this == tabs.getSelectedComponent()) {
        	// Refresh results table model because number of questions may have changed
        	resultsTableModel.refreshStructure();
        }
    }
}
