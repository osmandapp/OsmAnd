

function mapselector(container){
var $cnt = $(container);
var $selectbox = $cnt.find(".selectbox");

$cnt.find("label").on('click', function(){
		refreshMap();
	});

	var refreshMap = function(){
	var selectedStyle = getSelectedStyle().val();
		var selectedMap = getSelectedMap().val();
		if (selectedStyle && selectedMap){
			var imageName = selectedMap + "-" +  selectedStyle + ".png";
			$cnt.css('background-image', "url('/images/" + imageName + "')");
		}
	}

var getSelectedStyle = function(){
	return $selectbox.find("input[name='style']:checked");
}
var getSelectedMap = function(){
	return $selectbox.find("input[name='show']:checked");
}
refreshMap();


}
