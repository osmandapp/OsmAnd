<?php
$zoom = 15;
$lat = $_GET['lat'];
$lon = $_GET['lon'];
$zoom = $_GET['z'];
header('HTTP/1.1 302 Found');
header('Location: http://maps.google.com/maps?q=loc:'.$lat.','.$lon.'&z='.$zoom);
?>