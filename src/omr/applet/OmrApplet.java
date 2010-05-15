package omr.applet;


import javax.swing.*;

public class OmrApplet extends JApplet {
    private static final long serialVersionUID = 1L;

    public void init() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    createGUI();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void createGUI() {
    }

}
