package net.osmand.plus.osmodroid;

import java.util.ArrayList;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.parkingpoint.ParkingPositionLayer;
import net.osmand.plus.views.OsmandMapTileView;

import org.apache.commons.logging.Log;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.OsMoDroid.IRemoteOsMoDroidListener;
import com.OsMoDroid.IRemoteOsMoDroidService;

public class OsMoDroidPlugin extends OsmandPlugin {
IRemoteOsMoDroidListener.Stub inter = new IRemoteOsMoDroidListener.Stub() {

@Override
public void channelUpdated() throws RemoteException {
if(activity!=null){
activity.refreshMap();
//test
}

}

@Override
public void channelsListUpdated() throws RemoteException {
if (activity!=null&&connected){

for (OsMoDroidLayer myOsMoDroidLayer : osmoDroidLayerList){
activity.getMapView().removeLayer(myOsMoDroidLayer);
}
osmoDroidLayerList.clear();
requestLayersFromOsMoDroid(activity);
for (OsMoDroidLayer myOsMoDroidLayer :osmoDroidLayerList){
activity.getMapView().addLayer(myOsMoDroidLayer, 4.5f);

}
}

}

};


@Override
public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
registerLayers(activity);
super.updateLayers(mapView, activity);
}
MapActivity activity;
public static final String ID = "osmand.osmodroid";
private static final Log log = PlatformUtil.getLog(OsMoDroidPlugin.class);
private OsmandApplication app;
IRemoteOsMoDroidService mIRemoteService;
private ServiceConnection mConnection;
private int OSMODROID_SUPPORTED_VERSION_MIN = 5;
private OsMoDroidLayer osmoDroidLayer;
protected boolean connected=false;
ArrayList<OsMoDroidLayer> osmoDroidLayerList = new ArrayList<OsMoDroidLayer>();

public ArrayList<OsMoDroidPoint> getOsMoDroidPointArrayList(int id) {
ArrayList<OsMoDroidPoint> result =new ArrayList<OsMoDroidPoint>();
try {
for (int i = 0; i < mIRemoteService.getNumberOfObjects(id); i++) {
result.add(new OsMoDroidPoint(mIRemoteService.getObjectLat(id, mIRemoteService.getObjectId(id, i)) , mIRemoteService.getObjectLon(id, mIRemoteService.getObjectId(id, i)), mIRemoteService.getObjectName(id, mIRemoteService.getObjectId(id, i)), mIRemoteService.getObjectDescription(id, mIRemoteService.getObjectId(id, i)), mIRemoteService.getObjectId(id, i),id,mIRemoteService.getObjectSpeed(id, mIRemoteService.getObjectId(id, i)),mIRemoteService.getObjectColor(id, mIRemoteService.getObjectId(id, i))));
}
} catch (RemoteException e) {

log.error(e.getMessage(), e);
}

return result;

}

@Override
public String getId() {
return ID;
}

public OsMoDroidPlugin(OsmandApplication app) {
this.app = app;

}

@Override
public String getDescription() {
return app.getString(R.string.osmodroid_plugin_description);
}

@Override
public String getName() {
return app.getString(R.string.osmodroid_plugin_name);
}
//test
@Override
public boolean init(final OsmandApplication app) {
mConnection = new ServiceConnection() {
@Override
public void onServiceConnected(ComponentName name, IBinder service) {
mIRemoteService = IRemoteOsMoDroidService.Stub.asInterface(service);
try {
System.out.println(mIRemoteService.getVersion());
if(mIRemoteService.getVersion() < OSMODROID_SUPPORTED_VERSION_MIN) {
app.showToastMessage(R.string.osmodroid_plugin_old_ver_not_supported);
shutdown(app);
}else {
mIRemoteService.registerListener(inter);
connected=true;
}

} catch (RemoteException e) {
log.error(e.getMessage(), e);
}

}

@Override
public void onServiceDisconnected(ComponentName name) {
connected=false;
mIRemoteService = null;
}
};
Intent serviceIntent = (new Intent("OsMoDroid.remote"));
app.bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
return true;
}

void requestLayersFromOsMoDroid(MapActivity activity){
try {
for (int i = 0; i < mIRemoteService.getNumberOfLayers(); i++)
{
osmoDroidLayerList.add(new OsMoDroidLayer(activity, mIRemoteService.getLayerId(i),this,mIRemoteService.getLayerName( mIRemoteService.getLayerId(i)), mIRemoteService.getLayerDescription( mIRemoteService.getLayerId(i))));
}
} catch (RemoteException e) {

log.error(e.getMessage(), e);
}
}

@Override
public void registerLayers(MapActivity activity) {
this.activity=activity;
if (connected){

for (OsMoDroidLayer myOsMoDroidLayer : osmoDroidLayerList){
activity.getMapView().removeLayer(myOsMoDroidLayer);
}
osmoDroidLayerList.clear();
requestLayersFromOsMoDroid(activity);
for (OsMoDroidLayer myOsMoDroidLayer :osmoDroidLayerList){
activity.getMapView().addLayer(myOsMoDroidLayer, 4.5f);

}
}

}


@Override
public void registerLayerContextMenuActions(OsmandMapTileView mapView,
ContextMenuAdapter adapter, MapActivity mapActivity) {
for (OsMoDroidLayer myOsMoDroidLayer : osmoDroidLayerList){


adapter.registerItem(myOsMoDroidLayer.layerName);
}

super.registerLayerContextMenuActions(mapView, adapter, mapActivity);
}

@Override
public void disable(OsmandApplication app) {
shutdown(app);
}

private void shutdown(OsmandApplication app) {
if (mIRemoteService != null) {
if(connected){
try {
mIRemoteService.unregisterListener(inter);
} catch (RemoteException e) {
log.error(e.getMessage(), e);
}
}
app.unbindService(mConnection);
mIRemoteService = null;
}
}
}

