package omr.gui.calibration;


import java.awt.BorderLayout;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import omr.AnalyzeSheetsTask;
import omr.Project;
import omr.Sheet;
import omr.Task;
import omr.Project.ThresholdingStrategy;
import omr.gui.Gui;

public class CalibratePanel extends JPanel implements ListSelectionListener, ChangeListener, Observer  {
    private static final long serialVersionUID = 1L;
    
    private Gui gui;
    private Project project;
    
    private SheetTableModel sheetTableModel;
    private JTable sheetList;
    
    private SheetCalibrationEditor sheetView; 
    private JScrollPane sheetViewScrollPane;
    
    private HistogramComponent histogram;
    
    
    private JScrollPane propertiesScroll;
    private JPanel propertiesPanel;
    private CalibrationPropertiesPanel calibrationProperties;        // Global properties

    
    private Task analyzeTask;
    
    
    private JPanel rightPanel;  // Contains the sheet editor and the properties panel
    
    public CalibratePanel(Gui gui) {
        this.gui = gui;
        
        this.setLayout(new BorderLayout());
        
        // Sheet list on the left
        sheetTableModel = new SheetTableModel();
        sheetList = new JTable(sheetTableModel);
        sheetList.setFillsViewportHeight(true);
        sheetList.getSelectionModel().addListSelectionListener(this);
        JScrollPane sheetListScrollPane = new JScrollPane(sheetList);
        
        // Sheet view in the middle
        sheetView = new SheetCalibrationEditor();
        sheetView.setUndoSupport(gui.getUndoSupport());
        sheetViewScrollPane = new JScrollPane(sheetView);

        // Histogram on the bottom
        histogram = new HistogramComponent(sheetView);
        histogram.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "Histogram"));

        // Properties on the right
        propertiesPanel = new JPanel();
        propertiesPanel.setLayout(new BoxLayout(propertiesPanel, BoxLayout.PAGE_AXIS));
        propertiesScroll = new JScrollPane(propertiesPanel);
        
        calibrationProperties = new CalibrationPropertiesPanel(sheetView, histogram);
        propertiesPanel.add(calibrationProperties);
        
        
        rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(sheetViewScrollPane, BorderLayout.CENTER);
        rightPanel.add(propertiesScroll, BorderLayout.LINE_END);
        rightPanel.add(histogram, BorderLayout.PAGE_END);
        
        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setOneTouchExpandable(false);
        splitPane.setDividerLocation(150);
        splitPane.setLeftComponent(sheetListScrollPane);
        splitPane.setRightComponent(rightPanel);
        
        // Toolbar
        JToolBar toolbar = new CalibrateToolBar(sheetView);
        
        // Add top level components
        this.add(toolbar, BorderLayout.PAGE_START);
        this.add(splitPane, BorderLayout.CENTER);
    }
    
    public void setProject(Project project) {
        this.project = project;
        
        sheetTableModel.setProject(project);
        sheetView.setProject(project);
        histogram.setProject(project);
        calibrationProperties.setProject(project);
        
        sheetView.setSheet(null);
    }
    
  
    /**
     * Called when table selection changes.
     */
    public void valueChanged(ListSelectionEvent event) {
        Object source = event.getSource();
        
        if (source == sheetList.getSelectionModel() && !event.getValueIsAdjusting()) {
            // Selection changed in sheet list
            int row = sheetList.getSelectedRow();
            if (row >= 0) {
                Sheet sheet = (Sheet)sheetList.getValueAt(row, 0);
                if (sheet != null) {
                    // Display selected sheet
                    sheetView.setSheet(sheet);
                    
                    if (project.getThresholdingStrategy() == ThresholdingStrategy.PER_SHEET) {
                        histogram.setHistogram(sheet.getHistogram());
                    }
                }
            }
        }
    }
    
    /**
     * Called when tab is switched.
     */
    public void stateChanged(ChangeEvent event) {
        JTabbedPane tabs = (JTabbedPane)event.getSource();
        
        // Switched to this tab?        
        if (this == tabs.getSelectedComponent() && analyzeTask == null) {
            // TODO: don't recalculate sheets or sheet structure have not changed
            analyzeTask = new AnalyzeSheetsTask(project, this);

            // Calculate brightnesses in a background process
            gui.execute(analyzeTask);
        }
    }

    
    public void update(Observable source, Object event) {
        // Called when brightness task is finished
        analyzeTask = null;
        repaint();
    }
    
    
}
