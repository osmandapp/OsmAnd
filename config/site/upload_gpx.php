<?php 

 if(!isset($_GET['file']) || !isset($_GET['author']) || !isset($_GET['wd']) ) {
   header('HTTP/1.0 404 Not Found');
   print 'Specify file, author name and password';
   die(1);
 }
 $target_path = '/var/www/gpx/';
 
 if (is_uploaded_file($_FILES['filename']['tmp_name'])) {
   $target_file = tempnam($target_path, $_GET['author'] . '.' . basename($_FILES['filename']['name']). '.')  ;

   if(move_uploaded_file($_FILES['filename']['tmp_name'], $target_file)) {
     header('HTTP/1.1 200 Ok');
     echo "OK. Upload successfull.";
   } else {
     echo "There was an error uploading the file, please try again!";
   }
 } else {
   echo "Error : file is not uploaded";
   
 }
?>
