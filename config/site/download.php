<?php
include 'autoload.php';
use UnitedPrototype\GoogleAnalytics;

function downloadFile($filename) {
	if (!file_exists($filename)) {
		header('HTTP/1.0 404 Not Found');
		//die('File doesn\'t exist');
		die(1);
	}

	$from=0; 
	$cr=NULL;
	$to=filesize($filename) - 1;

	if (isset($_SERVER['HTTP_RANGE'])) {
	    list($a, $range) = explode("=",$_SERVER['HTTP_RANGE'],2);
	    list($range) = explode(",",$range,2);
	    list($from, $range_end) = explode("-", $range);
	    $from=intval($from);
	    if($range_end) {
	       $to=intval($range_end);
	    }
	    header('HTTP/1.1 206 Partial Content');
	    header('Content-Range: bytes ' . $from . '-' . $to.'/'.filesize($filename));
	} else {
            header('HTTP/1.1 200 Ok');
        }

	$size= $to - $from + 1;
	header('Accept-Ranges: bytes');
	header('Content-Length: ' . $size);

	header('Connection: close');
	header('Content-Type: application/octet-stream');
	header('Last-Modified: ' . gmdate('r', filemtime($filename)));
	$f=fopen($filename, 'r');
	header('Content-Disposition: attachment; filename="' . basename($filename) . '";');
	if ($from) fseek($f, $from, SEEK_SET);
	
	
	$downloaded=0;
	while(!feof($f) and !connection_status() and ($downloaded<$size)) {
	    $part = min(512000, $size-$downloaded);
	    echo fread($f, $part);
	    $downloaded+=$part;
	    flush();
	}
	fclose($f);
}

function url_exists($url) { 
    $hdrs = @get_headers($url); 
    return is_array($hdrs) ? preg_match('/^HTTP\\/\\d+\\.\\d+\\s+2\\d\\d\\s+.*$/',$hdrs[0]) : false; 
} 

function update_count_of_downloads($file) {
 try {
  $xml = simplexml_load_file("download_stat.xml");
  $res = $xml->xpath('//download[@name="'.$file.'"]');
  if (count($res) > 0) {
    $node = $res[0];
    $node["count"] = intval($node["count"]) + 1;
  } else {
    $obj = $xml-> addChild("download");
    $obj -> addAttribute("name", $file);
    $obj -> addAttribute("count", "1");
  }
  $xml->asXML("download_stat.xml");	
  //fclose($xml);
 } catch(Exception $e) {
 }
}

 if(!isset($_GET['file']) ) {
   header('HTTP/1.0 404 Not Found');
   die(1);
 }
 $file = $_GET['file'];
  // not used now
 if(!isset($_SERVER['HTTP_RANGE']) ) {
    // old version
    // update_count_of_downloads($file) ;

   if (!isset($_GET['event'])) {
     $eventno = 1;
   } else {
     $eventno = $_GET['event'];
   }
   if (isset($_GET['osmandver'])) {
     $app = $_GET['osmandver'];
   } else {
     $app = 'Download '.$_SERVER['HTTP_USER_AGENT'];
   }
      
    $tracker = new GoogleAnalytics\Tracker('UA-28342846-1', 'download.osmand.net');
    $visitor = new GoogleAnalytics\Visitor();
    $visitor->setIpAddress($_SERVER['REMOTE_ADDR']);
    $visitor->setUserAgent($_SERVER['HTTP_USER_AGENT']);
    $visitor->setScreenResolution('1024x768');
    // Assemble Session information
    // (could also get unserialized from PHP session)
    $session = new GoogleAnalytics\Session();

    // Assemble Page information
    $page = new GoogleAnalytics\Page('/download.php?'.$file);
    $page->setTitle('Download file '.$file);

    // Track page view
    $tracker->trackPageview($page, $session, $visitor);
    
    $event = new GoogleAnalytics\Event($app, 'App', $file, $eventno);
    $tracker->trackEvent($event, $session, $visitor);
 }
 set_time_limit(0);
 $xml = simplexml_load_file("indexes.xml");
 $res = $xml->xpath('//region[@name="'.$file.'"]');
 if(isset($_GET['srtm'])){
    downloadFile('srtm/'.$file);
 } else if(isset($_GET['road'])){
    downloadFile('road-indexes/'.$file);
 } else if (count($res) > 0) {
 	$node = $res[0];
        if($node["local"]) {
 		downloadFile('indexes/'.$file);
 	}  else {
 		header('HTTP/1.1 302 Found');
 		header('Location: http://osmand.googlecode.com/files/'.$file);
 	}
 } else {
    header('HTTP/1.1 302 Found');
    header('Location: http://osmand.googlecode.com/files/'.$file);
 }

?>
