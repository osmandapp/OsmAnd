<?php
function loadIndexesFromDir($output, $outputIndexes, $dir, $elementName, $mapNodes){
	$local_file = basename($_SERVER['PHP_SELF']) == basename(__FILE__);
	if (is_dir($dir)) {
		if ($dh = opendir($dir)) {
			$zip = new ZipArchive();
			while (($file = readdir($dh)) !== false) {
				$filename = $dir . $file ; //"./test112.zip";
				//print("processing file:" . $filename . "\n");
				if ($zip->open($filename,ZIPARCHIVE::CHECKCONS)!==TRUE) {
					// echo exit("cannot open <$filename>\n");
					// print($filename . " cannot open as zip\n");
					continue;
				}
				$indexName=$file;

				$description = $zip->getCommentIndex(0);
				$stat = $zip->statIndex( 0 );
				$date= date('d.m.Y',$stat['mtime']);
				$size=  number_format((filesize($filename) / (1024.0*1024.0)), 1, '.', '');
				$zip->close();
                if($local_file) {
					echo 'Local : '.$indexName.' '.$date.' '.$size.'<br>';
                }
				if (isset($mapNodes[$indexName])) {
					$exdate = DateTime::createFromFormat('d.m.Y', $mapNodes[$indexName]->getAttribute("date"));
                    $localdate = DateTime::createFromFormat('d.m.Y', $date);
                                        
                    if($localdate->getTimestamp() <= $exdate->getTimestamp()) {
						continue;
					}	
					$out = $mapNodes[$indexName];				
					//if($out -> getAttribute("parts")) {
						$outputIndexes->removeChild($out);
						$out = $output->createElement( $elementName);
						$outputIndexes->appendChild($out);
					//}
				} else {
					$out = $output->createElement( $elementName);
					$outputIndexes->appendChild($out);
				}
				
				
				$out -> setAttribute("date", $date);
				$out -> setAttribute("local", "true");
				$out -> setAttribute("size", $size);
				$out -> setAttribute("name", $indexName);
				$out -> setAttribute("description", $description);
				//$mapNodes[$indexName] = $out;
			}
			closedir($dh);
		}
	} else {
		print($dir . " not a directory!\n");
	}
}

function updateGoogleCodeIndexes($update=false) {
    $local_file = basename($_SERVER['PHP_SELF']) == basename(__FILE__);
	if( $local_file) 	{
    	$update = true;
	}

	$localFileName='indexes.xml';
	// check each 30 minutes
	if(!$update && file_exists($localFileName) && time() - filemtime($localFileName) < 60 * 30) {
		return;
	}
	if($local_file) {
		echo '<h1>File update : </h1> <br>';
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
            if($local_file) {
                        	echo $indexName.'   '.$date.'  '.$size.' <br>';
			}

	 		if(strpos($indexName,"voice.zip") || strpos($indexName,".obf")) {
	 			$ipart = strpos($indexName,"zip-");
	 			$part = 1;
	 			$base = $indexName;
	 			if($ipart) {
	 				$part = (int)substr($indexName, $ipart+4);
	 				$base = substr($indexName, 0, $ipart+3);
	 				if(isset($mapNodes[$base])) {
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
		// Open a known directory, and proceed to read its contents
	
    loadIndexesFromDir($output, $outputIndexes, 'indexes/', 'region', $mapNodes);
    loadIndexesFromDir($output, $outputIndexes, 'road-indexes/', 'road_region');
	$output->save($localFileName);
}

updateGoogleCodeIndexes(false);
?>