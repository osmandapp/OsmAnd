<?php 
   include 'preg_find.php';   

   header('Content-type: application/xml');
   header('Content-Disposition: attachment; filename="builds.xml"');
	
	function addBuilds($files, $output, $outputIndexes, $tag_suffix="") {
		foreach($files as $file => $stats) {	
			$fname = basename($file); 
			if(stristr($fname, "osmand-")) {
				$type = "OsmAnd";
			} else if(stristr($fname, "osmandmapcreator-")) {
				$type = "OsmAndMapCreator";
			} else {
				continue;
			}
			if (stripos($fname, "-nb-") !== false) {
				$tag = preg_replace("/[^-]*-(.*)-nb.*/i", "$1", $fname);
			} else {
				$tag = preg_replace("/[^-]*-([^\.-]*).*/i", "$1", $fname);
			}	
			$size = round(filesize($file)/1048576, 1);
			$date = date("d.m.Y", filemtime($file));
			$build = $output->createElement( "build" );
			$outputIndexes->appendChild( $build );
			$build -> setAttribute("size", $size);
			$build -> setAttribute("date", $date);
			$build -> setAttribute("tag", $tag.$tag_suffix);
			$build -> setAttribute("type", $type);
			$build -> setAttribute("path", $file);	
	  }
	}

   $output = new DOMDocument();
   $output->formatOutput = true;
  
   $outputIndexes = $output->createElement( "builds" );
   $output->appendChild( $outputIndexes );
	
   $files = preg_find('/./', 'latest-night-build', PREG_FIND_RETURNASSOC | PREG_FIND_SORTDESC| PREG_FIND_SORTMODIFIED);
   addBuilds($files, $output, $outputIndexes);
	
   $files = preg_find('/./', 'night-builds', PREG_FIND_RETURNASSOC | PREG_FIND_SORTDESC | PREG_FIND_SORTMODIFIED);
   addBuilds($files, $output, $outputIndexes, "-night-build");   
	
   echo $output->saveXML();   
?>
