package net.osmand.plus.download.ui;

import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.ui.DownloadMapDialogFragment;

public class DownloadMapDialogManager {

	private IndexItem currentIndexItem;
	
	public void showDialog(AppCompatActivity activity, IndexItem newIndexItem, String newRegionName, boolean usedOnMap) {
		if (newIndexItem != null) {
			if (!newIndexItem.equals(this.currentIndexItem)) {
				currentIndexItem = newIndexItem;
				FragmentManager fragmentManager = activity.getSupportFragmentManager();
				DownloadMapDialogFragment dialogFragment = 
						(DownloadMapDialogFragment) fragmentManager.findFragmentByTag(DownloadMapDialogFragment.TAG);
				if (dialogFragment != null) {
					//refresh dialog data
					dialogFragment.refreshData(newRegionName, newIndexItem);
				} else {
					//create a new dialog
					DownloadMapDialogFragment.showInstance(activity.getSupportFragmentManager(), newIndexItem, newRegionName, usedOnMap);
				}
			}
		}
	}
	
	public void hideDialog(AppCompatActivity activity) {
		currentIndexItem = null;
		DownloadMapDialogFragment.hideInstance(activity.getSupportFragmentManager());
	}
}
