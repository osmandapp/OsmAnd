package com.OsMoDroid;

import com.OsMoDroid.IRemoteOsMoDroidListener;

interface IRemoteOsMoDroidService {

void registerListener(IRemoteOsMoDroidListener listener);

void unregisterListener(IRemoteOsMoDroidListener listener);

    int getVersion();

    int getBackwardCompatibleVersion();
	
	void Deactivate();
	
	void Activate();

	boolean isActive();
	
	 int getNumberOfLayers();

int getLayerId(int pos);

String getLayerName(int layerId);

String getLayerDescription(int layerId);

int getNumberOfObjects(int layerId);

int getObjectId(int layerId, int pos);

float getObjectLat(int layerId, int objectId);

float getObjectLon(int layerId, int objectId);

String getObjectSpeed(int layerId, int objectId);

String getObjectName(int layerId, int objectId);

String getObjectDescription(int layerId, int objectId);

String getObjectColor(int layerId, int objectId);

int getNumberOfGpx(int layerId);

String getGpxFile (int layerId, int pos);

int getGpxColor (int layerId, int pos);

int getNumberOfPoints(int layerId);

int getPointId(int layerId, int pos);

float getPointLat(int  layerId, int pointId);

float getPointLon(int  layerId, int pointId);

String getPointName(int  layerId, int pointId);

String getPointDescription(int  layerId, int pointId);

String getPointColor(int  layerId, int pointId);
	
void refreshChannels();
}