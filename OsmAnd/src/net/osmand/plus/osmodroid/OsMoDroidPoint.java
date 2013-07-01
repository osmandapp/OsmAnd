package net.osmand.plus.osmodroid;


import net.osmand.data.LatLon;

public class OsMoDroidPoint {
@Override
public boolean equals(Object o) {

if((o instanceof OsMoDroidPoint) && this.id == ((OsMoDroidPoint)o).id )
{
return true;
}
else
{
return false;
}
}

LatLon latlon;
LatLon prevlatlon;
String name;
String description;
int id;
int layerId;
String speed="";
String color="AAAAAA";

public OsMoDroidPoint(float objectLat, float objectLon, String objectName, String objectDescription, int objectId, int layerId ,String speed, String color) {
this.latlon=new LatLon(objectLat, objectLon);
this.name=objectName;
this.description=objectDescription;
this.id=objectId;
this.layerId=layerId;
if(speed!=null){ this.speed=speed;}
if(color!=null){ this.color=color;}
}

}



