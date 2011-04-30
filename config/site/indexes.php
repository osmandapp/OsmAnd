<html>
<head><title>OsmAnd Indexes</title></head>
<?php 
   $update = $_GET['update'];
   include 'update_googlecode_indexes.php';
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
<h1><?php echo "Table of indexes hosted on googlecode"; ?></h1>
<table border="1">
<?php
   $res = $xpath->query('//region');
   if($res && $res->length > 0) { 	 
	   
		foreach($res as $node) {
		  echo "<tr><td>".$node->getAttribute('name')."</td><td>".$node->getAttribute('date').
		   "</td><td>".$node->getAttribute('size')."</td><td>".
				$node->getAttribute('description')."</td></tr>";
      }
    }			
?>
</table>
</body>
</html>
