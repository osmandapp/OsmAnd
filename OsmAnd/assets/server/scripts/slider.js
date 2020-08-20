var images_android=[
"promo-1s.png",
"promo-2s.png",
"promo-3s.png",
"promo-4s.png",
"promo-5s.png",
"promo-6s.png",
"promo-7s.png",
"promo-8s.png",
"promo-9s.png",
"promo-10s.png",
"promo-11s.png",
"promo-12s.png"
];

var images_ios=[
"ios-1s.png",
"ios-2s.png",
"ios-3s.png",
"ios-4s.png",
"ios-5s.png"
];

function slider(container){
var $cnt = $(container);
var $img1 = $cnt.find("#screenshot1");
var $img2 =$cnt.find("#screenshot2");
var $img3 = $cnt.find("#screenshot3");
var $img4 = $cnt.find("#screenshot4");
var $leftarrow =  $cnt.find(".arrow.left");
var $rightarrow =  $cnt.find(".arrow.right");
var $btnleft = $cnt.find(".button.left");
var $btnright = $cnt.find(".button.right");
var currentPosition =0;
var count =4;

var images = images_android;

var init = function(){
	updatePictures();
	updateArrows();	
	$leftarrow.on('click', function(){
		if (currentPosition > 0){
			currentPosition-=count;
			updatePictures();
			updateArrows();	
		}
	});
	$rightarrow.on('click', function(){
		if (currentPosition + count < images.length){
			currentPosition+=count;
			updatePictures();
			updateArrows();	
		}
	});
	$btnleft.on('click', function(){
		if (!$btnleft.hasClass("active")){
			$btnleft.addClass("active");
			$btnright.removeClass("active");
			$btnright.addClass
			images = images_android;
			currentPosition = 0;
			updatePictures();
			updateArrows();
		}
		
		
	});
	$btnright.on('click', function(){
		if (!$btnright.hasClass("active")){
			$btnright.addClass("active");
			$btnleft.removeClass("active");
			images = images_ios;
			currentPosition = 0;
			updatePictures();
			updateArrows();
		}
		
	});
}

var changePicture = function(img, index){
	if (index < images.length){
		img.attr("src", "images/" + images[index]);
	}else{
		img.attr("src", "images/empty.png");
	}
}
var updatePictures = function(){
	changePicture( $img1, currentPosition);
	changePicture( $img2, currentPosition+1);
	changePicture( $img3, currentPosition+2);
	changePicture( $img4, currentPosition+3);
}
var updateArrows = function(){
	if (currentPosition + count < images.length){
		enableRightArrow();	
	}else{
		disableRightArrow();
	}
	if (currentPosition== 0 ){
		disableLeftArrow();	
	}else{
		enableLeftArrow();
	}
}
var enableLeftArrow = function(){
	$leftarrow.attr("src", "images/left_arrow_orange.png");
	while ($leftarrow.hasClass("disabled")){	
		$leftarrow.removeClass("disabled");
	}
}
var disableLeftArrow = function(){
	$leftarrow.attr("src", "images/left_arrow_grey.png");
	$leftarrow.addClass("disabled");
}
var enableRightArrow = function(){
	$rightarrow.attr("src", "images/right_arrow_orange.png");
	while ($rightarrow.hasClass("disabled")){	
		$rightarrow.removeClass("disabled");
	}
}
var disableRightArrow = function(){
	$rightarrow.attr("src", "images/right_arrow_grey.png");
	$rightarrow.addClass("disabled");
}
init();

}
