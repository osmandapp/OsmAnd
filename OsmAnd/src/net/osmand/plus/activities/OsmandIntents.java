package net.osmand.plus.activities;

import net.osmand.plus.activities.search.SearchActivity;
import android.app.Activity;

public class OsmandIntents {
	
	public static Class<SettingsActivity> getSettingsActivity(){
		return SettingsActivity.class;
	}
	
	public static Class<MapActivity> getMapActivity(){
		return MapActivity.class;
	}
	
	public static Class<SearchActivity> getSearchActivity(){
		return SearchActivity.class;
	}
	
	public static Class<FavouritesActivity> getFavoritesActivity(){
		return FavouritesActivity.class;
	}

	public static Class<MainMenuActivity> getMainMenuActivity() {
		return MainMenuActivity.class;
	}
	
	public static Class<? extends Activity> getDownloadIndexActivity() {
		return DownloadIndexActivity.class;
	}
	
	public static Class<? extends Activity> getLocalIndexActivity() {
		return LocalIndexesActivity.class;
	}

}
