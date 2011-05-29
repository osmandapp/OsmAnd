<?php 
   header('Content-type: application/xml');
   header('Content-Disposition: attachment; filename="tile_sources.xml"');

   readfile('tile_sources.xml');
?>
