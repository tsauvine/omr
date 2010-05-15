package omr.gui;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class StatusBar extends JPanel {
    private static final long serialVersionUID = 1L;

    private JLabel statusText;
    private JProgressBar progressBar;
    
    public StatusBar() {
        this.setLayout(new BorderLayout());
        
        progressBar = new JProgressBar();
        statusText = new JLabel();
        statusText.setHorizontalAlignment(JLabel.RIGHT);
        
        this.add(progressBar, BorderLayout.LINE_END);
        this.add(statusText, BorderLayout.CENTER);
    }
    
    public void setStatusText(String text) {
        statusText.setText(text);
    }
    
    public void setProgressTarget(int n) {
        progressBar.setMaximum(n);
    }
    
    public void setProgress(int n) {
        progressBar.setValue(n);
    }
}
