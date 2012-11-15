<html>
<head><title>OsmAnd Indexes</title></head>
<?php 
   $update = $_GET['update'];
   include 'update_indexes.php';
   updateGoogleCodeIndexes($update);
   $dom = new DomDocument(); 
   $dom->load('indexes.xml'); 
   $xpath = new DOMXpath($dom);
?>
<body>
<h1><?php echo "Table of multiindexes hosted on googlecode"; ?></h1>
<table border="1">
<?php

   $res = $xpath->query('//multiregion');
   if($res && $res->length > 0) { 	 
	   
		foreach($res as $node) {
		  echo "<tr><td>".$node->getAttribute('name')."</td><td>".$node->getAttribute('date').
		   "</td><td>".$node->getAttribute('size')."</td><td>".$node->getAttribute('parts')."</td><td>".
			$node->getAttribute('description')."</td></tr>";
      }
    }		
?>
</table>
<h1><?php echo "Table of indexes hosted on osmand.net"; ?></h1>
<table border="1">
<?php
   $res = $xpath->query('//region[@local]');
   if($res && $res->length > 0) {
		foreach($res as $node) {
		  if (file_exists('indexes/'.$node->getAttribute('name'))) {
		      echo "<tr><td>".$node->getAttribute('name')."</td><td>".$node->getAttribute('date')
         		   ."</td><td>".$node->getAttribute('size')
               ."</td><td>".$node->getAttribute('description')
               ;
				
	      }
        }
    }			
?>
</table>
<h1><?php echo "Table of road indexes hosted on osmand.net"; ?></h1>
<table border="1">
<?php
   $res = $xpath->query('//road_region[@local]');
   if($res && $res->length > 0) {
    foreach($res as $node) {
      if (file_exists('road-indexes/'.$node->getAttribute('name'))) {
          echo "<tr><td>".$node->getAttribute('name')."</td><td>".$node->getAttribute('date')
               ."</td><td>".$node->getAttribute('size')
               ."</td><td>".$node->getAttribute('description')
               ;
        
        }
        }
    }     
?>
</table>
<h1><?php echo "Table of  indexes on googlecode"; ?></h1>
<table border="1">
<?php
   $res = $xpath->query('//region');
   if($res && $res->length > 0) {

   	foreach($res as $node) {
   		if (!file_exists('indexes/'.$node->getAttribute('name')) || !$node->getAttribute('local')) {
   			echo "<tr><td>".$node->getAttribute('name')."</td><td>".$node->getAttribute('date')
               ."</td><td>".$node->getAttribute('size')."</td><td>".$node->getAttribute('description')
               ;
   		}
   	}
   }
?>
</table>

</body>
</html>
