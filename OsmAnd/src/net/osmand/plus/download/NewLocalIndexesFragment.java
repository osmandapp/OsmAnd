package net.osmand.plus.download;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StatFs;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.PopupMenu;
import android.text.method.LinkMovementMethod;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.LocalIndexHelper;
import net.osmand.plus.activities.LocalIndexHelper.LocalIndexType;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.activities.OsmAndListFragment;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.activities.OsmandExpandableListFragment;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.resources.IncrementalChangesManager;
import net.osmand.plus.resources.IncrementalChangesManager.IncrementalUpdate;
import net.osmand.plus.resources.IncrementalChangesManager.IncrementalUpdateList;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public class NewLocalIndexesFragment extends OsmAndListFragment {
	private static final MessageFormat formatGb = new MessageFormat("{0, number,#.##} GB", Locale.US);

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.local_index_fragment, container, false);

		getDownloadActivity().setSupportProgressBarIndeterminateVisibility(false);

		ListView listView = (ListView)view.findViewById(android.R.id.list);
//		listAdapter = new LocalIndexesAdapter(getActivity());
//		listView.setAdapter(listAdapter);
//		setListView(listView);
		TextView descriptionText = (TextView) view.findViewById(R.id.memory_size);
		ProgressBar sizeProgress = (ProgressBar) view.findViewById(R.id.memory_progress);
		File dir = getMyApplication().getAppPath("").getParentFile();
		String size = formatGb.format(new Object[]{0});
		int percent = 0;
		if(dir.canRead()){
			StatFs statFs = new StatFs(dir.getAbsolutePath());
			//noinspection deprecation
			size = formatGb.format(new Object[]{(float) (statFs.getAvailableBlocks()) * statFs.getBlockSize() / (1 << 30) });
			//noinspection deprecation
			percent = (int) (statFs.getAvailableBlocks() * 100 / statFs.getBlockCount());
		}
		sizeProgress.setProgress(percent);
		String text = getString(R.string.free, size);
		descriptionText.setText(text);
		descriptionText.setMovementMethod(LinkMovementMethod.getInstance());
		return view;
	}

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

}
