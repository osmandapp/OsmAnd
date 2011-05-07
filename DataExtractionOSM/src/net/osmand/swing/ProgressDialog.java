package net.osmand.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.lang.reflect.InvocationTargetException;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import net.osmand.IProgress;
import net.osmand.LogUtil;

import org.apache.commons.logging.Log;



public class ProgressDialog extends JDialog implements IProgress {

	private static final long serialVersionUID = -3915486672514402269L;
	private final static Log log = LogUtil.getLog(ProgressDialog.class);
	private JProgressBar progressBar;
	private JLabel label;
	private Runnable run;
	private InvocationTargetException exception = null;
	 
	
	private Object result;
	
	

	// Progress variables
	private static final float deltaToChange = 0.001f;
	private String taskName;
	private int deltaWork;
	private WorkerThread workerThread;
	private String genProgress;
	
	private long previousTaskStarted = 0;

    
    public ProgressDialog(Component parent, String name){
    	super(JOptionPane.getFrameForComponent(parent), true);
    	setTitle(name);
        initDialog();
    }
    
    public boolean isInterrupted(){
    	return !isVisible();
    }
    
    @SuppressWarnings("deprecation")
	public Object run() throws InvocationTargetException, InterruptedException {
		result = null;
		workerThread = new WorkerThread();
		workerThread.start();
		setVisible(true);
		if(workerThread.checkIsLive()){
			// that's really bad solution unless we don't find any problems with that
			// means monitor objects & we continue to use because otherwise
			// we should protect all places where it is used regular checks that process is interrupted
			workerThread.stop();
			throw new InterruptedException();
		}
		if (exception != null) {
			throw exception;
		}
		return result;
	}
    
    private class WorkerThread extends Thread {
    	private boolean isLive = true;
    	
    	
    	public boolean checkIsLive(){
    		return isLive;
    	}
    	@Override
    	public void run() {
    		try {
				if (run != null) {
					run.run();
				}
			} catch (RuntimeException e) {
				exception = new InvocationTargetException(e);
			} finally {
				isLive = false;
			}
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					setVisible(false);
				}
			});
    	}
    	
    }
    
    private void initDialog() {
        JPanel pane = new JPanel(new BorderLayout());
        pane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        progressBar = new JProgressBar();
        pane.add(progressBar, BorderLayout.SOUTH);
        label = new JLabel();
        pane.add(label, BorderLayout.CENTER);
        add(pane);
        
        label.setText(Messages.getString("OsmExtractionUI.PLEASE.WAIT")); //$NON-NLS-1$
        progressBar.setIndeterminate(true);
        setSize(550, 100);
        double x = getParent().getBounds().getCenterX();
        double y = getParent().getBounds().getCenterY();
        setLocation((int) x - getWidth() / 2, (int) y - getHeight() / 2);
    }
    
    public Object getResult() {
		return result;
	}
    
    public void setResult(Object result) {
		this.result = result;
	}
    
    public void setRunnable(Runnable run) {
		this.run = run;
	}

	@Override
	public void progress(int deltaWork) {
		this.deltaWork += deltaWork;
		if(change(progressBar.getValue() + this.deltaWork)){
			progressBar.setValue(progressBar.getValue() + this.deltaWork);
			this.deltaWork = 0;
			updateMessage();
		}
	}
	
	private void updateMessage() {
		if(!progressBar.isIndeterminate()){
			String format = String.format("\t %.1f %%", progressBar.getValue() * 100f / ((float) progressBar.getMaximum())); //$NON-NLS-1$
			label.setText(taskName +  format + (genProgress == null ? "" : ("   " + genProgress))); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public boolean change(int newProgress) {
		if (newProgress < progressBar.getValue()) {
			return false;
		}
		if ((newProgress - progressBar.getValue()) / ((float) progressBar.getMaximum()) < deltaToChange) {
			return false;
		}
		return true;
	}
	@Override
	public void remaining(int remainingWork) {
		if(change(progressBar.getMaximum() - remainingWork)){
			progressBar.setValue(progressBar.getMaximum() - remainingWork);
			updateMessage();
		}
		deltaWork = progressBar.getMaximum() - remainingWork - this.progressBar.getValue();
	}
	
	public boolean isIndeterminate(){
		return progressBar.isIndeterminate();
	}
	
	@Override
	public void setGeneralProgress(String genProgress) {
		this.genProgress = genProgress;
		
	}

	@Override
	public void startTask(String taskName, int work) {
		if (log.isDebugEnabled()) {
			log.debug("Memory before task exec: " + Runtime.getRuntime().totalMemory() + " free : " + Runtime.getRuntime().freeMemory()); //$NON-NLS-1$ //$NON-NLS-2$
			if (previousTaskStarted == 0) {
				log.debug(taskName + " started"); //$NON-NLS-1$
			} else {
				log.debug(taskName + " started after " + (System.currentTimeMillis() - previousTaskStarted) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
		}
		previousTaskStarted = System.currentTimeMillis();
		if(taskName == null){
			taskName = ""; //$NON-NLS-1$
		}
		label.setText(taskName + (genProgress == null ? "" : ("   "+genProgress))); //$NON-NLS-1$ //$NON-NLS-2$
		this.taskName = taskName;
		startWork(work);
	}

	@Override
	public void finishTask() {
		if (taskName != null) {
			label.setText(taskName);
		}
		progressBar.setIndeterminate(true);
	}

	

	@Override
	public void startWork(int work) {
		if(work == 0){
			work = 1;
		}
		final int w = work;
		SwingUtilities.invokeLater(new Runnable(){

			@Override
			public void run() {
				if(w != -1){
					progressBar.setMinimum(0);
					progressBar.setMaximum(w);
					progressBar.setValue(0);
					progressBar.setIndeterminate(false);
				} else {
					progressBar.setIndeterminate(true);
				}
				
			} 
		});
		
		deltaWork = 0;
	}
}
