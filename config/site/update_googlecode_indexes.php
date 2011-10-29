
<?php

function updateGoogleCodeIndexes($update=false) { 
  $localFileName='indexes.xml';
  // check each 30 minutes
  if(!$update && file_exists($localFileName) && time() - filemtime($localFileName) < 60 * 30) {
      return;
  }

  $dom = new DomDocument(); 


  $output = new DOMDocument();
  $output->formatOutput = true;
  
   $outputIndexes = $output->createElement( "osmand_regions" );
   $outputIndexes->setAttribute('mapversion','1');
   $output->appendChild( $outputIndexes );
   


  
   $st = 0;
   $num = 200;
   $count = 0;
   $mapNodes = array();
   /// 1. dlownload indexes  from googlecode
  while($st != -1){
    $dom->loadHTMLFile("http://code.google.com/p/osmand/downloads/list?num=".$num."&start=".$st."&colspec=Filename+Summary+Uploaded+Size"); 
	 $count ++;
	 $xpath = new DOMXpath($dom);
	 $xpathI = new DOMXpath($dom);
	 $res = $xpath->query('//td[contains(@class,"col_0")]');
	 if($res && $res->length > 0) { 	 
		foreach($res as $node) {
			$indexName = trim($node->nodeValue);
			
			$s = $xpathI->query('td[contains(@class,"col_1")]/a[1]', $node->parentNode);
			if(!$s || $s->length == 0) {
				continue;
			}
			$description = $s->item(0)->nodeValue;
			$i = strpos($description,"{");
			if(!$i) {
			   continue;
			}
			$i1 = strpos($description,":", $i);
			$i2 = stripos($description,"mb", $i1);
			if(!$i2) {
			    $i2 = strpos($description,"}", $i1);
			}
			$date = trim(substr($description, $i + 1, $i1 - $i -1));
			$size = trim(substr($description, $i1 + 1, $i2 - $i1 -1));
			$description = trim(substr($description, 0, $i));
						
			if(strpos($indexName,"voice.zip") || strpos($indexName,"_1.poi.zip") ||
			   strpos($indexName,"_1.poi.odb") || strpos($indexName,"_1.obf")) {
  			   $ipart = strpos($indexName,"zip-");
                           $part = 1;
			   $base = $indexName;
                           if($ipart) {
                              $part = (int)substr($indexName, $ipart+4);
                              $base = substr($indexName, 0, $ipart+3);
                              if($mapNodes[$base]) {
                                  $out = $mapNodes[$base];
                              } else {
                                 $out = $output->createElement( "multiregion" );
                                 $out -> setAttribute("parts", $part); 
                                 $mapNodes[$base] = $out;
			         $out -> setAttribute("date", $date);
			         $out -> setAttribute("size", $size);
			         $out -> setAttribute("name", $base);
			         $out -> setAttribute("description", $description);
                                 $outputIndexes->appendChild($out);
                              }
                              if( (int) $out -> getAttribute("parts") < $part){
                              		$out -> setAttribute("parts", $part);
                              }
                           } else {
			      $out = $output->createElement( "region" );
			      $out -> setAttribute("date", $date);
			      $out -> setAttribute("size", $size);
			      $out -> setAttribute("name", $indexName);
			      $out -> setAttribute("description", $description);
                              $outputIndexes->appendChild($out);
                              $mapNodes[$indexName] = $out;
			   }

			}
			
		
		} 
		$st += $num;
	 } else {
	   $st = -1;
	 }
     }
     /// 2. append local indexes
     $local = new DomDocument(); 
     $local->load('local_indexes.xml');
     $xpath = new DOMXpath($local);
     $res = $xpath->query("//*[name() = 'region' or name() = 'multiregion']");
     if($res && $res->length > 0) {
	    foreach($res as $node) {
		    // TODO fix multiregion is not the same as region for old clients
	      //if($mapNodes[$node->getAttribute("name")]) {
		   //	continue;
	      // }
	      $out = $output->createElement( $node -> nodeName);
	      $out -> setAttribute("date", $node -> getAttribute("date"));
	      $out -> setAttribute("size", $node -> getAttribute("size"));
	      $out -> setAttribute("name", $node -> getAttribute("name"));
	      $out -> setAttribute("description", $node -> getAttribute("description"));
            if($node -> getAttribute("parts")) {
		         $out -> setAttribute("parts", $node -> getAttribute("parts"));
	         }
	      $outputIndexes->appendChild($out);
      }
    }	
                                 
    $output->save($localFileName);	

}
?>
