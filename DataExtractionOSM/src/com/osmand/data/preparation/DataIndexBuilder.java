package com.osmand.data.preparation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.XMLStreamException;

import com.osmand.data.Amenity;
import com.osmand.data.Region;
import com.osmand.osm.io.OsmStorageWriter;


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
		String fileName = name;
		if (zipped) {
			// change name
			name = new File(name).getName();
			fileName += ".zip";
		}
		File f = new File(workingDir, fileName);
		f.mkdirs();
		// remove existing file
		if (f.exists()) {
			f.delete();
		}
		OutputStream output =  new FileOutputStream(f);
		if(zipped){
			ZipOutputStream zipStream = new ZipOutputStream(output);
			zipStream.setLevel(5);
			zipStream.putNextEntry(new ZipEntry(name));
			output = zipStream;
		} 
		return output;
	}
	
	
	public DataIndexBuilder buildPOI() throws XMLStreamException, IOException{
		
		List<Amenity> list = region.getAmenityManager().getAllObjects();
		List<Long> interestedObjects = new ArrayList<Long>(list.size());
		for(Amenity a : list)	{
			interestedObjects.add(a.getEntity().getId());
		}
		OutputStream output = checkFile("POI/"+region.getName()+".osmand");
		try {
			OsmStorageWriter writer = new OsmStorageWriter();
			writer.savePOIIndex(output, region);
		} finally {
			output.close();
		}
		return this;
	}
	
	public DataIndexBuilder buildAddress() throws XMLStreamException, IOException{
		OutputStream output = checkFile("Address/"+region.getName()+".osmand");
		try {
			OsmStorageWriter writer = new OsmStorageWriter();
			writer.saveAddressIndex(output, region);
		} finally {
			output.close();
		}
		return this;
	}
}
