package net.osmand.plus.plugins.audionotes;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;

import org.apache.commons.logging.Log;

import java.util.Timer;
import java.util.TimerTask;

public class RecordingPlayer {

	private static final Log log = PlatformUtil.getLog(RecordingPlayer.class);

	private final OsmandApplication app;
	@Nullable
	private final Runnable onPlaybackUpdate;

	private Timer timer;
	private MediaPlayer player;
	private Recording recording;

	public RecordingPlayer(@NonNull OsmandApplication app, @Nullable Runnable onPlaybackUpdate) {
		this.app = app;
		this.onPlaybackUpdate = onPlaybackUpdate;
	}

	public boolean isPlaying() {
		try {
			return player != null && player.isPlaying();
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isPlaying(Recording r) {
		return isPlaying() && recording == r;
	}

	public int getPlayingPosition() {
		if (isPlaying()) {
			return player.getCurrentPosition();
		} else if (player != null) {
			return player.getDuration();
		} else {
			return -1;
		}
	}

	public void stopPlaying() {
		if (isPlaying()) {
			try {
				player.stop();
			} finally {
				player.release();
				player = null;
				notifyPlaybackUpdate();
			}
		}
	}

	public void playRecording(@NonNull Context ctx, @NonNull Recording recording) {
		if (recording.isVideo() || recording.isPhoto()) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			String type = recording.isVideo() ? "video/*" : "image/*";
			intent.setDataAndType(AndroidUtils.getUriForFile(ctx, recording.getFile()), type);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			AndroidUtils.startActivityIfSafe(ctx, intent);
			return;
		}

		if (isPlaying()) {
			stopPlaying();
		}
		this.recording = recording;
		player = new MediaPlayer();
		try {
			player.setDataSource(recording.getFile().getAbsolutePath());
			player.setOnPreparedListener(mp -> {
				try {
					player.start();
					if (timer != null) {
						timer.cancel();
					}
					timer = new Timer();
					timer.schedule(new TimerTask() {

						@Override
						public void run() {
							notifyPlaybackUpdate();
							if (!isPlaying()) {
								cancel();
								timer = null;
							}
						}

					}, 10, 1000);
				} catch (Exception e) {
					logErr(e);
				}
			});
			player.setOnCompletionListener(mp -> this.recording = null);
			player.prepareAsync();
		} catch (Exception e) {
			logErr(e);
		}
	}

	private void notifyPlaybackUpdate() {
		if (onPlaybackUpdate != null) {
			onPlaybackUpdate.run();
		}
	}

	private void logErr(@NonNull Exception e) {
		log.error("Error starting recorder ", e);
		app.showToastMessage(app.getString(R.string.recording_error) + " : " + e.getMessage());
	}
}