<?php
function downloadFile($filename) {
	if (!file_exists($filename)) die('File doesn\'t exist');

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


 $file = $_GET['file'];
 $direct = $_GET['direct'];
 if(!isset($_SERVER['HTTP_RANGE']) ) {
  update_count_of_downloads($file) ;
 }
 set_time_limit(0);
 if($direct == 'yes' or !url_exists('http://osmand.googlecode.com/files/'.$file)) {
    downloadFile('indexes/'.$file);
 } else {
    header('HTTP/1.1 302 Found');
    header('Location: http://osmand.googlecode.com/files/'.$file);
 }

?>
