package net.osmand.cli;

import java.io.IOException;
import java.io.File;

import net.osmand.IProgress;
import net.osmand.swing.DataExtractionSettings;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.data.preparation.IndexCreator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rtree.RTree;

public class OsmToObf {
 private static final Log log = LogFactory.getLog(IndexCreator.class);

 public static void main(String[] args) throws IOException {
  System.out.println("OsmToObf");

  if(args == null || args.length == 0){
   System.out.println("please specify input file");
    return;
  }


  File f = new File(args[0]);

  File dir = DataExtractionSettings.getSettings().getDefaultWorkingDir();
  IndexCreator creator = new IndexCreator(dir);
  try {
       
       creator.setIndexAddress(true);
       creator.setIndexPOI(true);
       creator.setNormalizeStreets(true);
       creator.setIndexTransport(true);
       creator.setIndexMap(true);
       creator.setIndexRouting(true);
       
       creator.setCityAdminLevel(DataExtractionSettings.getSettings().getCityAdminLevel());
       String fn = DataExtractionSettings.getSettings().getMapRenderingTypesFile();
       MapRenderingTypes types;
       if(fn == null || fn.length() == 0){
         types = MapRenderingTypes.getDefault();
       } else {
         types = new MapRenderingTypes(fn);
       }

       RTree.clearCache();

       int smoothness = 0;
       try {
           smoothness = Integer.parseInt(DataExtractionSettings.getSettings().getLineSmoothness());
       } catch (NumberFormatException e) {
       }

       creator.setZoomWaySmothness(smoothness);
       creator.generateIndexes(f, IProgress.EMPTY_PROGRESS, null, DataExtractionSettings.getSettings().getMapZooms(), types, log);
  } catch (Exception e)
  {
   System.out.println("something bad happend...");
  }

  return;
 }

}

