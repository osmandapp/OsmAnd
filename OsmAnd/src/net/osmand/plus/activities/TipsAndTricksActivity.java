package net.osmand.plus.activities;

import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import android.app.Dialog;
import android.content.Context;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

public class TipsAndTricksActivity {
	private int[][] tipNamesAndDescriptions = new int[][] {
			{R.string.tip_recent_changes,R.string.tip_recent_changes_0_8_4_t},
			{R.string.tip_rotation_switching,R.string.tip_rotation_switching_t_v2},
			{R.string.tip_update_index,R.string.tip_update_index_t},
			{R.string.tip_day_night_mode,R.string.tip_day_night_mode_t_v2},
			{R.string.tip_location_sharing,R.string.tip_location_sharing_t},
			{R.string.tip_favorites,R.string.tip_favorites_t},
			{R.string.tip_osm_improve,R.string.tip_osm_improve_t},
			{R.string.tip_map_context_menu,R.string.tip_map_context_menu_t},
			{R.string.tip_search,R.string.tip_search_t},
			{R.string.tip_select_destination_point,R.string.tip_select_destination_point_t},
			{R.string.tip_navigation,R.string.tip_navigation_t},
			{R.string.tip_app_mode,R.string.tip_app_mode_t_v2},
			{R.string.tip_recent_changes,R.string.tip_recent_changes_0_8_3_t},
			{R.string.tip_recent_changes,R.string.tip_recent_changes_0_8_2_t},
			{R.string.tip_map_switch,R.string.tip_map_switch_t_v2},
			};

	private final Context ctx;
	private int numberOfShownTips = 0;
	private boolean[] shownTips = new boolean[tipNamesAndDescriptions.length];
	
	public TipsAndTricksActivity(Context ctx){
		this.ctx = ctx;
	}

	public boolean areAllTipsShown(){
		return numberOfShownTips == tipNamesAndDescriptions.length;
	}
	
	public int getNumberOfTips(){
		return tipNamesAndDescriptions.length;
	}
	
	public void markTipAsShown(int index) {
		if (!shownTips[index]) {
			shownTips[index] = true;
			numberOfShownTips++;
		}
	}
	
	public int getNextRandomTipToShow() {
		int l = getNumberOfTips();
		if (l != 0) {
			int mod = (int) (System.currentTimeMillis() % l);
			return getNextTipToShow(mod);
		}
		return -1;
	}
	
	public void clearShownInfo(){
		Arrays.fill(shownTips, false);
		numberOfShownTips = 0;
	}

	public int getNextTipToShow(int suggest) {
		int l = getNumberOfTips();
		if(suggest >= l || suggest < 0){
			suggest = 0;
		}
		if (l > 0) {
			int it = suggest;
			do {
				if (!shownTips[it]) {
					markTipAsShown(it);
					return it;
				}
				it++;
				if (it == l) {
					it = 0;
				}
			} while (it != suggest);
			
			clearShownInfo();
			markTipAsShown(suggest);
			return suggest;
		}
		return -1;
	}
	
	public String getTipName(int ind){
		return ctx.getString(tipNamesAndDescriptions[ind][0]);
	}
	
	public String getTipDescription(int ind){
		return ctx.getString(tipNamesAndDescriptions[ind][1]);
	}
	
	public Dialog getDialogToShowTips(boolean showFirst, boolean random){
		
		final Dialog dlg = new Dialog(ctx);
		dlg.setContentView(R.layout.tips_and_tricks);
		dlg.getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		final TextView tipDescription = (TextView) dlg.findViewById(R.id.TipDescription);
		if (!((OsmandApplication)ctx.getApplicationContext()).getSettings().ACCESSIBILITY_EXTENSIONS.get())
			tipDescription.setMovementMethod(ScrollingMovementMethod.getInstance());
		int nextInd = 0;
		final TIntArrayList toShow = new TIntArrayList();
		final int[] historyInd = new int[1];
		if(showFirst){
			dlg.setTitle(R.string.tip_initial);
			tipDescription.setText(R.string.tip_initial_t);
			historyInd[0] = -1;
		} else if(!random){
			nextInd = 0;
			dlg.setTitle(getTipName(nextInd));
			tipDescription.setText(getTipDescription(nextInd));
			toShow.add(nextInd);
			historyInd[0] = 0;
		} else {
			nextInd = getNextRandomTipToShow();
			dlg.setTitle(getTipName(nextInd));
			tipDescription.setText(getTipDescription(nextInd));
			toShow.add(nextInd);
			historyInd[0] = 0;
		}
		
		final Button nextButton = ((Button)dlg.findViewById(R.id.NextButton));
		final Button prevButton = (Button)dlg.findViewById(R.id.PreviousButton);
		
		prevButton.setEnabled(historyInd[0] > 0);
		nextButton.setEnabled(historyInd[0] < getNumberOfTips() - 1);
		
		nextButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View view) {
				if(historyInd[0] == toShow.size() - 1){
					int suggest = 0;
					if(historyInd[0] >= 0) {
						suggest = toShow.get(historyInd[0]) + 1;
					}
					toShow.add(getNextTipToShow(suggest));
				}
				historyInd[0] ++;
				dlg.setTitle(getTipName(toShow.get(historyInd[0])));
				tipDescription.setText(getTipDescription(toShow.get(historyInd[0])));
				tipDescription.scrollTo(0, 0);
				
				prevButton.setEnabled(historyInd[0] >  0);
				nextButton.setEnabled(historyInd[0] < getNumberOfTips() - 1);
			}
		});
		
		prevButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View view) {
				if(historyInd[0] > 0){
					historyInd[0] --;
					dlg.setTitle(getTipName(toShow.get(historyInd[0])));
					tipDescription.setText(getTipDescription(toShow.get(historyInd[0])));
					tipDescription.scrollTo(0, 0);
				}
				prevButton.setEnabled(historyInd[0] > 0);
				nextButton.setEnabled(historyInd[0] < getNumberOfTips() - 1);
			}
		});
		
		((Button)dlg.findViewById(R.id.CloseButton)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dlg.dismiss();
			}
		});
		return dlg;
	}


}
