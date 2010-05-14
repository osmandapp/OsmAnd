package com.osmand.data.preparation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.tools.bzip2.CBZip2OutputStream;

import com.osmand.DefaultLauncherConstants;
import com.osmand.data.Amenity;
import com.osmand.data.Region;
import com.osmand.osm.io.OSMStorageWriter;


public class DataIndexBuilder {
	
	private final File workingDir;
	private final Region region;
	private boolean zipped = true;

	public DataIndexBuilder(File workingDir, Region region){
		this.workingDir = workingDir;
		this.region = region;
	}

	public void setZipped(boolean zipped) {
		this.zipped = zipped;
	}
	
	public boolean isZipped() {
		return zipped;
	}
	
	protected OutputStream checkFile(String name) throws IOException {
		if (zipped && !name.endsWith(".bz2")) {
			name += ".bz2";
		}
		File f = new File(workingDir, name);
		f.mkdirs();
		// remove existing file
		if (f.exists()) {
			f.delete();
		}
		OutputStream output = new FileOutputStream(f);
		if (DefaultLauncherConstants.writeTestOsmFile.endsWith(".bz2")) {
			output.write('B');
			output.write('Z');
			output = new CBZip2OutputStream(output);
		}
		return output;
	}
	
	
	public DataIndexBuilder buildPOI() throws XMLStreamException, IOException{
		
		List<Amenity> list = region.getAmenityManager().getAllObjects();
		List<Long> interestedObjects = new ArrayList<Long>(list.size());
		for(Amenity a : list)	{
			interestedObjects.add(a.getNode().getId());
		}
		OutputStream output = checkFile("POI/"+region.getName()+".osm");
		try {
			OSMStorageWriter writer = new OSMStorageWriter(region.getStorage().getRegisteredEntities());
			writer.saveStorage(output, interestedObjects, false);
		} finally {
			output.close();
		}
		return this;
	}
}
