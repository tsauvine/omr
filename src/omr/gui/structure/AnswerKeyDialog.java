package omr.gui.structure;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;

import omr.QuestionGroup;

public class AnswerKeyDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 1L;

    private JTable table;          // Table with checkboxes to set the answers
    private JButton okButton;      // OK button to close the dialog
    
    public AnswerKeyDialog(QuestionGroup group) {
        // Add widgets
        table = new JTable(new AnswerKeyTableModel(group));
        
        JPanel buttonsPanel = new JPanel(new FlowLayout());
        okButton = new JButton("OK");
        okButton.addActionListener(this);
        buttonsPanel.add(okButton);
        
        this.add(table, BorderLayout.CENTER);
        this.add(table.getTableHeader(), BorderLayout.PAGE_START);
        this.add(buttonsPanel, BorderLayout.PAGE_END);
        
        
        // Show the dialog
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setModal(true);
        this.pack();
        this.setVisible(true);
    }
    
    public void actionPerformed(ActionEvent event) {
        // Close the dialog 
        this.dispose();
    }
    
}
