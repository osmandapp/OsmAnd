package net.osmand.plus.sherpafy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class TourInformation {
	
	private String name;
	private File folder;
	private String description = null;
	private List<StageInformation> stageInformation = new ArrayList<TourInformation.StageInformation>();

	public TourInformation(File f) {
		this.folder = f;
		this.name = f.getName();
	}
	
	
	private String loadDescription() {
		File fl = new File(folder, "description.txt");
		StringBuilder text = new StringBuilder();
		if (fl.exists()) {
			try {
				BufferedReader in = new BufferedReader(new FileReader(fl), 256); //$NON-NLS-1$
				String s;
				boolean f = true;
				while ((s = in.readLine()) != null) {
					if (!f) {
						text.append("\n"); //$NON-NLS-1$
					} else {
						f = false;
					}
					text.append(s);
				}
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return text.toString();
	}
	
	public void loadFullInformation(){
		description = loadDescription();		
	}
	
	
	public String getDescription() {
		return description == null ? "" : description;
	}
	
	public String getName() {
		return name;
	}
	
	public File getFolder() {
		return folder;
	}
	
	public List<StageInformation> getStageInformation() {
		return stageInformation;
	}


	public Bitmap getImageBitmap() {
		File fl = new File(folder, "images/Default.jpg");
		if(fl.exists()) {
			return BitmapFactory.decodeFile(fl.getAbsolutePath());
		}
		return null;
	}
	
	public static class StageInformation {
		
		File gpxFile;
		String name;
		String description;
		
		public String getName() {
			return name;
		}
		
		public String getDescription() {
			return description;
		}
		
		public File getGpxFile() {
			return gpxFile;
		}
		
	}

}
