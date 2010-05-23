package omr;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import omr.gui.Gui;



/**
 * Main class of the application. Starts the Gui.
 * 
 * @author Tapio Auvinen
 */
public class Omr {

    public static void main(String[] args) {
        new Omr();
    }

    /**
     * Constructor
     */
    public Omr() {
        //Schedule a job for the event-dispatching thread
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    createAndShowGUI();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates and shows GUI
     */
    private void createAndShowGUI() {
        // Set look and feel
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
        }
        
        // Create gui
        new Gui();
    }
}
