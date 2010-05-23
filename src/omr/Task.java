package omr;

import java.util.Observable;
import java.util.Observer;

import omr.gui.StatusBar;


abstract public class Task extends Observable implements Runnable {
    
    //private LinkedList<ProgressListener> listeners;
    private StatusBar statusBar;
    
    private int estimatedOperationsCount;
    private int completedOperationsCount;
    
    public Task() {
        //this.listeners = new LinkedList<ProgressListener>();
        this.estimatedOperationsCount = 1;
        this.completedOperationsCount = 0;
    }
    
    /**
     * Constructor
     * @param observer Observer to be notified when task is finished
     */
    public Task(Observer observer) {
        super();
        
        if (observer != null) {
        	this.addObserver(observer);
        }
    }
    
    /**
     * Sets the statusbar for showing progress to the user.
     */
    public void setStatusBar(StatusBar statusBar) {
        this.statusBar = statusBar;
    }
    
    /*
    public void addProgressListener(ProgressListener listener) {
        this.listeners.add(listener);
    }
    */
    
    /**
     * Updates the statusbar text.
     */
    protected void setStatusText(String text) {
        if (statusBar != null) {
            statusBar.setStatusText(text);
        }
    }
    
    /**
     * Sets the number of operations needed to complete the task. Progress bar will show completedOperationsCount / estimatedOperationsCount.  
     */
    protected void setEstimatedOperationsCount(int n) {
        this.estimatedOperationsCount = n;
            
        if (statusBar != null) {
            statusBar.setProgressTarget(n);
        }
    }
    
    /**
     * Returns the total number of operations needed to complete the task. 
     */
    public int getEstimatedOperationsCount() {
        return estimatedOperationsCount;
    }
    
    /**
     * Sets the number of completed operation.
     */
    protected void setCompletedOperationsCount(int n) {
        this.completedOperationsCount = n;
        this.updateProgressBar();
    }
    
    /**
     * Increases the number of completed operations by one.
     */
    synchronized protected void increaseCompletedOperationsCount() {
        this.completedOperationsCount++;
        this.updateProgressBar();
    }
    
    private void updateProgressBar() {
        if (statusBar == null) {
            return;
        }
            
        statusBar.setProgress(completedOperationsCount);
    }
    
    /**
     * Called when the task is finished.
     */
    protected void finished() {
        if (statusBar != null) {
            statusBar.setStatusText(null);
            statusBar.setProgress(0);
        }
        
        // Notify observers
        setChanged();
        notifyObservers();
    }

    abstract public void run();
}
