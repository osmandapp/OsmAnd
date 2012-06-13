<html>
<head><title>OsmAnd Local Indexes List</title></head>
<?php 
   $update = $_GET['update'];
?>
<body>
<h1>Osmand Local Indexes List</h1>
<table border="1">
<?php

class Download {
    var $file;
    var $date;
    var $mtime;
    var $size;
    var $description;

    function Download($file,$mtime,$date,$size,$description)
    {
        $this->file = $file;
        $this->mtime = $mtime;
        $this->date = $date;
        $this->size = $size;
        $this->description = $description;
    }

    static function cmp_string($a, $b)
    {
        $al = strtolower($a);
        $bl = strtolower($b);
        if ($al == $bl) {
            return 0;
        }
        return ($al > $bl) ? +1 : -1;
    }

    /* This is the static comparing function: */
    static function cmp_file($a, $b)
    {
      return Download::cmp_string($a->file,$b->file);
    }


    static function cmp_description($a, $b)
    {
        return Download::cmp_string($a->description,$b->description);
    }

    static function cmp_date($a, $b)
    {
        return Download::cmp_int(strtotime($a->date),strtotime($b->date));
    }


    static function cmp_size($a, $b)
    {
        return Download::cmp_int($a->size,$b->size);
    }


    static function cmp_int($a, $b)
    {
	if ($a > $b) {
          return 1;
        } else {
	  return -1;
        }
    }

}


	//echo "Searching dir";
	// Open a known directory, and proceed to read its contents
	$dir='indexes/';
    $localFileName='local_list.xml';
	if (is_dir($dir)) {
		if (filemtime($dir) > filemtime($localFileName) || $update) {
           //echo "Recreating local_list.xml";
				if ($dh = opendir($dir)) {
					$output = new DOMDocument();
					$output->formatOutput = true;
		  
					$outputIndexes = $output->createElement( "local_list" );
					//$outputIndexes->setAttribute('mapversion','1');
					$output->appendChild( $outputIndexes );
		 
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
						$a[] = new Download($file,$stat['mtime'],$date,$size,$description);

						$out = $output->createElement("file");
						$out -> setAttribute("file", $file);
						$out -> setAttribute("mtime", $stat['mtime']);
						$out -> setAttribute("date", $date);
						$out -> setAttribute("size", $size);
						$out -> setAttribute("description", $description);
						$outputIndexes->appendChild($out);
					}
					closedir($dh);
					//echo "Writing to file";
					$output->save($localFileName);	
				}
        } else {
           //read the file
           //echo "Reading local_list.xml";
     	   $local = new DomDocument(); 
      	   $local->load($localFileName);
           $xpath = new DOMXpath($local);
           $res = $xpath->query("//*[name() = 'file']");
		   if($res && $res->length > 0) {
				foreach($res as $node) {
                  $a[] = new Download(
						$node->getAttribute("file"),
						$node->getAttribute("mtime"),
						$node->getAttribute("date"),
						$node->getAttribute("size"),
						$node->getAttribute("description"));
			  }
			}	
		 
        }
	} else {
		print($dir . " not a directory!\n");
	}

$sortby = $_GET['sortby'];
$d = $_GET['d'];
if (!isset($sortby)) {
  $sortby = "cmp_file";
}
if (!isset($d)) {
  $d = 1;
}

usort($a, array("Download",$sortby) );
$d=$d*1;
if ($d == -1) {
 $a = array_reverse($a);
}
$d = $d*-1;

	echo "<tr><th><A HREF=\"list.php?d=$d\">File</A></th>".
             "<th><A HREF=\"list.php?sortby=cmp_date&d=$d\">Date</A></th>".
             "<th><A HREF=\"list.php?sortby=cmp_size&d=$d\">Size</A></th>".
             "<th><A HREF=\"list.php?sortby=cmp_description&d=$d\">Description</A></th></tr>";
foreach ($a as $item) {
      echo "<tr><td><A HREF=\"/download.php?file=$item->file\">".$item->file."</A>".
           "</td><td>".$item->date.
           "</td><td>".$item->size.
           "</td><td>".$item->description.
           "</td></tr>";
}

?>
</table>
</body>
</html>
