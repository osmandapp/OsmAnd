package net.osmand.plus;

import java.io.File;


public interface ClientContext {
	
	
	public String getFullVersion();
	
	public boolean isWifiConnected();

	public String getVersionAsURLParam();
	
	public String getString(int resId);
	
	public File getAppDir();
	
	public File getVoiceDir();
	
	public File getBackupDir();
	
	public void showToastMessage(int msgId);
	

}
