package omr;

/**
 * A background taks that sends feedback emails. 
 */
public class SendFeedbacksTask extends Task {

    private Project project;

    public SendFeedbacksTask(Project project) {
        this.project = project;
    }
    
    @Override
    public void run() {
        SheetsContainer sheets = project.getSheetsContainer();

        this.setEstimatedOperationsCount(sheets.size());
        this.setStatusText("Sending feedback emails");
        
        Mailer mailer = new Mailer();
        
        for (Sheet sheet : sheets) {
            try {
            	mailer.sendFeedback(project, sheet);
            } catch (Exception e) {
                System.err.println(e);
                break;
            }
            
            // Publish progress
            this.increaseCompletedOperationsCount();
        }
        
        this.finished();
    }

}
