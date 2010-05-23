package omr.gui;


import java.awt.*;
import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.*;

import omr.CsvSerializer;
import omr.Deserializer;
import omr.Project;
import omr.SendFeedbacksTask;
import omr.Serializer;
import omr.Task;
import omr.gui.calibration.CalibratePanel;
import omr.gui.results.ResultsPanel;
import omr.gui.structure.StructurePanel;

/**
 * Main window of the graphical user interface.
 * 
 * Most methods are called from the Menu.
 */
public class Gui extends JFrame {
    private static final long serialVersionUID = 1L;
    
    private Project project;            // Project being edited
    private File projectFile;           // Currently open project file
    
    private UndoSupport undoSupport;
    private Executor executor;          // Background tasks executor
    
    // Tabs
    private StructurePanel structurePanel;
    private CalibratePanel calibratePanel;
    private ResultsPanel resultsPanel;
    
    private StatusBar statusBar;
    
    /**
     * Initializes the GUI and shows the main window.
     */
    public Gui() {
        super("Optical Mark Recognition");

        // Background tasks executor
        this.executor = Executors.newSingleThreadExecutor();
        
        this.reset();

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(new Dimension(800, 600));

        //Add widgets
        addWidgets();
        
        // Initialize a new project
        this.setProject(new Project());

        //Display the window
        //this.pack();
        this.setVisible(true);
    }
    
    private void reset() {
        // Create undo manager
        this.undoSupport = new UndoSupport();
    }
    
    /**
     * Adds widgets to the main window.
     */
    private void addWidgets() {
        //this.setLayout(new BorderLayout());

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        this.add(tabs, BorderLayout.CENTER);
        
        structurePanel = new StructurePanel(this);
        tabs.addTab("Structure", null, structurePanel, "Define answer form structure");
        // setMnemonicAt(0, KeyEvent.VK_1);

        calibratePanel = new CalibratePanel(this);
        tabs.addTab("Calibrate", null, calibratePanel, "Calibrate OMR");
        // setMnemonicAt(0, KeyEvent.VK_1);
        tabs.addChangeListener(calibratePanel);  // Listen to tab change events
        
        resultsPanel = new ResultsPanel(this);
        tabs.addTab("Results", null, resultsPanel, "Results");
        // setMnemonicAt(0, KeyEvent.VK_1);
        tabs.addChangeListener(resultsPanel);
        
        // Statusbar at the bottom
        statusBar = new StatusBar();
        this.add(statusBar, BorderLayout.PAGE_END);
        
                
        // Menu
        setJMenuBar(new Menu(this));
    }
    
    /**
     * Sets the active project.
     * @param project Project to be edited. Never set to null.
     */
    private void setProject(Project project) {
        this.project = project;
        structurePanel.setProject(project);
        calibratePanel.setProject(project);
        resultsPanel.setProject(project);
    }
    
    public UndoSupport getUndoSupport() {
        return this.undoSupport;
    }

    /**
     * Executes the given Runnable task in a thread. Tasks are queued and executed sequentially.
     */
    public void execute(Task task) {
        task.setStatusBar(statusBar);
        executor.execute(task);
    }
    
    /**
     * Starts a new project. If current project has unsaved changes, user is prompted for confirmation.
     */
    public void newProject() {
    	if (project.isChanged()) {
    		// Prompt for confirmation
    		// TODO: offer "Save as" option
            Object[] options = {"Discard", "Cancel"};
            int answer = JOptionPane.showOptionDialog(this,
                    "The project has been modified. Do you want to discard changes?",
                    "Discard changes?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);
            
            if (answer != 0) {
                // Cancel
                return;
            }
    	}
    	
    	// Create new project
    	this.setProject(new Project());
        this.projectFile = null;
    }
    
    /**
     * Shows an "Open file" dialog, then loads the selected file as the current project.
     */
    public void openProject() {
        JFileChooser chooser = new JFileChooser();
        //chooser.addChoosableFileFilter(new OmrFileFilter()); 
        int returnVal = chooser.showOpenDialog(this);

        if (returnVal != JFileChooser.APPROVE_OPTION) {
            // Cancel
            return;
        }
        
        File file = chooser.getSelectedFile();
        
        // Load project
        try {
            Deserializer deserializer = new Deserializer();
            Project loadedProject = deserializer.loadProject(file);
            this.setProject(loadedProject);
            this.projectFile = file;
        } catch (Exception e) {
            // Show an error dialog
            JOptionPane.showMessageDialog(this,
                "Failed to load the project.\n" + e,
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Saves the current project to the same file where it was previously saved or where it was loaded from.
     * If current project is a new one (has not been saved), the "Save as" dialog is shown.  
     */
    public boolean saveProject() {
        if (this.projectFile == null) {
            return saveProjectAs(); 
        } else {
            return save(this.projectFile);
        }
    }
    
    /**
     * Shows a "Save as" dialog, then saves the current project to the chosen file. If the file exists, user is prompted for confirmation.
     * @return True if file was succesfully saved. False if user cancels or an saving fails.
     */
    public boolean saveProjectAs() {
        File file = showSaveAsDialog();
        if (file == null) {
            return false;
        }
        
        return this.save(file);
    }
    
    /**
     * Shows a "save as" dialog and prompts for confimation if selected file exists.
     * @return Selected file. Null if user cancels.
     */
    private File showSaveAsDialog() {
        JFileChooser chooser = new JFileChooser();
        //chooser.addChoosableFileFilter(new OmrFileFilter()); 

        int returnVal = chooser.showSaveDialog(this);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            // Cancel
            return null;
        }
        
        File file = chooser.getSelectedFile();

        // Prompt overwrite
        if (file.exists()) {
            Object[] options = {"Overwrite", "Cancel"};
            int answer = JOptionPane.showOptionDialog(this,
                    "A file named " + file.getName() + " already exists. Are you sure you want to overwrite it?",
                    "Overwrite file?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);
            
            if (answer != 0) {
                // Cancel overwrite
                return null;
            }
        }
        
        return file;
    }

    /**
     * Saves the current project to the given file. Shows an error message if something goes wrong.
     */
    private boolean save(File file) {
    	// Try to save the file
        try {
            Serializer serializer = new Serializer();
            serializer.saveProject(this.project, file);
            this.projectFile = file;
        } catch (Exception e) {
            // Show an error dialog
            JOptionPane.showMessageDialog(this,
                "Failed to save the project.\n" + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            
            return false;
        }
        
        return true;
    }
    
    /**
     * Shows an "open files" dialog for selecting answer sheets to be imported, then imports the selected files.
     */
    public void importSheets() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.addChoosableFileFilter(new ImageFileFilter()); 
        chooser.setMultiSelectionEnabled(true);
        //chooser.setDialogTitle(""); 

        int returnVal = chooser.showOpenDialog(this);

        if (returnVal != JFileChooser.APPROVE_OPTION) {
            // Cancel
            return;
        }
            
        File[] files = chooser.getSelectedFiles();
        
        try {
            project.addAnswerSheets(files);
        } catch (Exception e) {
            // Show an error dialog
            JOptionPane.showMessageDialog(this,
                "Failed to import sheets.\n" + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            
            e.printStackTrace();
        }
    }
    
    /**
     * Shows a "save as" dialog for exporting answers as CSV, then performs the export.
     */
    public void exportAnswers() {
        File file = showSaveAsDialog();
        if (file == null) {
            // Cancel
            return;
        }
        
        // Try to save the file
        try {
            CsvSerializer serializer = new CsvSerializer();
            serializer.saveAnswers(this.project, file);
        } catch (Exception e) {
            // Show an error dialog
            JOptionPane.showMessageDialog(this,
                "Failed to export answers.\n" + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Shows a "save as" dialog for exporting results as CSV, then performs the export.
     */
    public void exportResults() {
        File file = showSaveAsDialog();
        if (file == null) {
            return;
        }
        
        // Try to save the file
        try {
            CsvSerializer serializer = new CsvSerializer();
            serializer.saveResults(this.project, file);
        } catch (Exception e) {
            // Show an error dialog
            JOptionPane.showMessageDialog(this,
                "Failed to export results.\n" + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
        
    }
    
    /**
     * Sends all feedback emails.
     */
    public void mailFeedback() {
    	SendFeedbacksTask mailerTask = new SendFeedbacksTask(project);

        // Send emails in a background process
        execute(mailerTask);
    }
    
}
