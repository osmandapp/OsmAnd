function add_point_to_map(points){
    console.log(points);
    for(var i=0;i<  points.length; i++)
      L.marker([points[i][0], points[i][1]]).addTo(map);
}
function bound_map_to_points(points){
	lat_long_bound = new L.latLngBounds(points);
	map.fitBounds(lat_long_bound);
}