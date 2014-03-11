package net.osmand.plus.activities.actions;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;

public class OsmAndDialogs {

	private static Map<Integer, OsmAndAction> dialogActions = new HashMap<Integer, OsmAndAction>(); 
	public static Dialog createDialog(int dialogID, Activity activity, Bundle args) {
		OsmAndAction action = dialogActions.get(dialogID);
		if(action != null) {
			return action.createDialog(activity, args);
		}
		return null;
	}
	
	public static void prepareDialog(int dialogID, Activity activity, Bundle args, Dialog dlg) {
		OsmAndAction action = dialogActions.get(dialogID);
		if(action != null) {
			action.prepareDialog(activity, args, dlg);
		}
	}
	
	
	public static void registerDialogAction(OsmAndAction action) {
		if(action.getDialogID() != 0) {
			dialogActions.put(action.getDialogID(), action);
		}
	}
			
	public static final int DIALOG_ADD_FAVORITE = 200;
	public static final int DIALOG_REPLACE_FAVORITE = 201;
	public static final int DIALOG_ADD_WAYPOINT = 202;
	public static final int DIALOG_RELOAD_TITLE = 203;
	public static final int DIALOG_SHARE_LOCATION = 204;
	public static final int DIALOG_SAVE_DIRECTIONS = 206;
	public static final int DIALOG_START_GPS = 207;
	
	
	public static int getPluginDialogId(int pluginId, int dialogId) {
		return (pluginId + 3) * 100 + dialogId;
	}
}
