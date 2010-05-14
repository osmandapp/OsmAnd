package com.osmand.swing;

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

import com.osmand.IProgress;


public class ProgressDialog extends JDialog implements IProgress {

	private static final long serialVersionUID = -3915486672514402269L;
	private JProgressBar progressBar;
	private JLabel label;
	private Runnable run;
	private InvocationTargetException exception = null;
	
	private Object result;
	private boolean finished;
	
	

	// Progress variables
	private static final float deltaToChange = 0.01f;
	private String taskName;
	private int deltaWork;

    
    public ProgressDialog(Component parent, String name){
    	super(JOptionPane.getFrameForComponent(parent), true);
    	setTitle(name);
        initDialog();
    }
    
    public Object run() throws InvocationTargetException, InterruptedException {
		finished = false;
		result = null;
		new WorkerThread().start();
		setVisible(true);
		if (!finished) {
			
		}
		if (exception != null) {
			throw exception;
		}
		return result;
	}
    
    private class WorkerThread extends Thread {
    	
    	@Override
    	public void run() {
    		try {
				if (run != null) {
					run.run();
				}
			} catch (RuntimeException e) {
				exception = new InvocationTargetException(e);
			} finally {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						setVisible(false);
					}
				});
			}
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
        
        label.setText("Please waiting...");
        progressBar.setIndeterminate(true);
        setSize(550, 100);
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
			label.setText(taskName + String.format("\t %.1f %%", progressBar.getValue() * 100f / ((float) progressBar.getMaximum())));
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
	public void startTask(String taskName, int work) {
		if(taskName == null){
			taskName = "";
		}
		label.setText(taskName);
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
