<?php 
   
   if (!file_exists("indexes.xml")) {
	include 'update_googlecode_indexes.php';
	updateGoogleCodeIndexes();
   }
   header('Content-type: application/xml');
   header('Content-Disposition: attachment; filename="indexes.xml"');

   readfile('indexes.xml');
?>
