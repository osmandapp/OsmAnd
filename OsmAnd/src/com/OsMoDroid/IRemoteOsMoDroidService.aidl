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
	

}