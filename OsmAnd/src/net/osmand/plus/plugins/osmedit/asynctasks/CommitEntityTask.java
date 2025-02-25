package net.osmand.plus.plugins.osmedit.asynctasks;

import android.app.ProgressDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.CallbackWithObject;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.plugins.osmedit.data.OsmPoint.Action;
import net.osmand.plus.plugins.osmedit.helpers.OpenstreetmapUtil;
import net.osmand.plus.utils.AndroidUtils;

import java.util.Set;


public class CommitEntityTask extends BaseLoadAsyncTask<Void, Void, Entity> {

	private final OpenstreetmapUtil osmUtil;

	private final Entity entity;
	private final Action action;
	private final EntityInfo info;
	private final String comment;
	private final Set<String> changedTags;
	private final boolean closeChangeSet;
	private final CallbackWithObject<Entity> callback;

	public CommitEntityTask(@NonNull FragmentActivity activity, @NonNull OpenstreetmapUtil osmUtil,
			@NonNull Entity entity, @NonNull Action action, @Nullable EntityInfo info,
			@Nullable String comment, boolean closeChangeSet, @Nullable Set<String> changedTags,
			@Nullable CallbackWithObject<Entity> callback) {
		super(activity);
		this.action = action;
		this.entity = entity;
		this.info = info;
		this.comment = comment;
		this.closeChangeSet = closeChangeSet;
		this.callback = callback;
		this.osmUtil = osmUtil;
		this.changedTags = changedTags;
	}

	@Override
	protected Entity doInBackground(Void... params) {
		return osmUtil.commitEntityImpl(action, entity, info, comment, closeChangeSet, changedTags);
	}

	@Override
	protected void showProgress(boolean cancelableOnTouchOutside) {
		FragmentActivity activity = activityRef.get();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			progress = ProgressDialog.show(activity, app.getString(R.string.uploading), app.getString(R.string.uploading_data));
		}
	}

	@Override
	protected void onPostExecute(Entity result) {
		hideProgress();

		if (callback != null) {
			callback.processResult(result);
		}
	}
}
