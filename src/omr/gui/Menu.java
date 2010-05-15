package omr.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

public class Menu extends JMenuBar implements ActionListener {
    private static final long serialVersionUID = 1L;
    private Gui gui;
    
    private JMenuItem newProject;
    private JMenuItem openProject;
    private JMenuItem saveProject;
    private JMenuItem saveProjectAs;
    private JMenuItem importSheets;
    private JMenuItem exportAnswers;
    private JMenuItem exportResults;
    private JMenuItem mailFeedback;
    
    public Menu(Gui gui) {
        this.gui = gui;
        UndoSupport undoSupport = gui.getUndoSupport();
        
        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        //menu.getAccessibleContext().setAccessibleDescription("The only menu in this program that has menu items");
        add(fileMenu);

        // New project
        newProject = new JMenuItem("New project", KeyEvent.VK_N);
        newProject.addActionListener(this);
        fileMenu.add(newProject);
        
        // Open project
        openProject = new JMenuItem("Open project...", KeyEvent.VK_O);
        openProject.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        openProject.addActionListener(this);
        fileMenu.add(openProject);

        // Save project
        saveProject = new JMenuItem("Save project", KeyEvent.VK_A);
        saveProject.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        saveProject.addActionListener(this);
        fileMenu.add(saveProject);

        // Save project as
        saveProjectAs = new JMenuItem("Save project as...", KeyEvent.VK_S);
        //saveProjectAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        saveProjectAs.addActionListener(this);
        fileMenu.add(saveProjectAs);

        fileMenu.addSeparator();
        
        // Import sheets
        importSheets = new JMenuItem("Import sheets...", KeyEvent.VK_I);
        importSheets.getAccessibleContext().setAccessibleDescription("Imports answer sheets to the project");
        importSheets.addActionListener(this);
        fileMenu.add(importSheets);

        // Export answers
        exportAnswers = new JMenuItem("Export answers...", KeyEvent.VK_C);
        exportAnswers.getAccessibleContext().setAccessibleDescription("Exports answers to a file");
        exportAnswers.addActionListener(this);
        fileMenu.add(exportAnswers);
        
        // Export results
        exportResults = new JMenuItem("Export results...", KeyEvent.VK_R);
        exportResults.getAccessibleContext().setAccessibleDescription("Exports results to a file");
        exportResults.addActionListener(this);
        fileMenu.add(exportResults);
        
        fileMenu.addSeparator();
        
        // Mail feedback
        mailFeedback = new JMenuItem("Send feedback emails", KeyEvent.VK_M);
        mailFeedback.getAccessibleContext().setAccessibleDescription("Sends feedback emails to students");
        mailFeedback.addActionListener(this);
        fileMenu.add(mailFeedback);
        
        
        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        add(editMenu);
        
        // Undo
        JMenuItem undo = new JMenuItem("Undo", KeyEvent.VK_U);
        undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
        undo.addActionListener(undoSupport.getUndoAction());
        editMenu.add(undo);
        
        // Redo
        JMenuItem redo = new JMenuItem("Redo", KeyEvent.VK_R);
        redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
        redo.addActionListener(undoSupport.getRedoAction());
        editMenu.add(redo);
        
        editMenu.addSeparator();

        // Cut
        JMenuItem cut = new JMenuItem("Cut", KeyEvent.VK_T);
        cut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
        //cut.addActionListener(new CutAction(undoManager));
        //editMenu.add(cut);

        // Copy
        JMenuItem copy = new JMenuItem("Copy", KeyEvent.VK_C);
        copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        //copy.addActionListener(new CopyAction(undoManager));
        //editMenu.add(copy);

        // Paste
        JMenuItem paste = new JMenuItem("Paste", KeyEvent.VK_P);
        paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
        //paste.addActionListener(new PasteAction(undoManager));
        //editMenu.add(paste);

        // Delete
        JMenuItem delete = new JMenuItem("Delete", KeyEvent.VK_D);
        delete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        //delete.addActionListener(new DeleteAction(undoManager));
        editMenu.add(delete);

    }

    public void actionPerformed(ActionEvent event) {
        JMenuItem source = (JMenuItem)(event.getSource());
        
        if (source == newProject) {
            gui.newProject();
        } else if (source == openProject) {
            gui.openProject();
        } else if (source == saveProject) {
            gui.saveProject();
        } else if (source == saveProjectAs) {
            gui.saveProjectAs();
        } else if (source == importSheets) {
            gui.importSheets();
        } else if (source == exportAnswers) {
            gui.exportAnswers();
        } else if (source == exportResults) {
            gui.exportResults();
        } else if (source == mailFeedback) {
            gui.mailFeedback();
        }
    }

}
