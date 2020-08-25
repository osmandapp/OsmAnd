function applyPolStyles(){

 if ($('.pds-box').length > 0){
	$('.pds-box').css('border', 'none');
	$('.pds-vote-button').css('float', 'left');
	$('.pds-vote-button').css('background', 'none');
	$('.pds-vote-button').css('background-color', '#FF8F00');
	$('.pds-vote-button').css('color', '#fff');
	$('.pds-vote-button').css('border', 'none');
	$('.pds-vote-button').css('border-radius', '5px');
	$('.pds-box-outer').css('padding', '0');
	$('.pds-view-results').on('click', function(){
		subscribeToReturnToPoll();		
	});
	}else{
	   setTimeout(applyPolStyles, timeout);
	}
 }
 
 function subscribeToReturnToPoll(){
 if ($('.pds-return-poll').length > 0){
	$('.pds-return-poll').on('click', function(){applyStyleOnBackToPoll();});
	applyPolStyles();
 }else{
	setTimeout(subscribeToReturnToPoll, timeout);
 }
 }
 
 function applyStyleOnBackToPoll(){
  if ($('.pds-view-results').length >0){	
	applyPolStyles();
 }else{
	setTimeout(applyStyleOnBackToPoll, timeout);
 }
 }