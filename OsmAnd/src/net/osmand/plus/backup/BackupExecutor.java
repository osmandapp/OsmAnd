package net.osmand.plus.backup;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BackupExecutor extends ThreadPoolExecutor {

	private final OsmandApplication app;
	private final AtomicInteger aState = new AtomicInteger(State.IDLE.ordinal());
	private List<BackupCommand> activeCommands = new ArrayList<>();
	private List<BackupExecutorListener> listeners = new ArrayList<>();

	public enum State {
		IDLE,
		BUSY;

		@Nullable
		public static State getByOrdinal(int ordinal) {
			for (State s : values()) {
				if (s.ordinal() == ordinal) {
					return s;
				}
			}
			return null;
		}
	}

	public interface BackupExecutorListener {
		void onBackupExecutorStateChanged(@NonNull State state);
	}

	public BackupExecutor(@NonNull OsmandApplication app) {
		super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
		this.app = app;
	}

	public State getState() {
		return State.getByOrdinal(aState.get());
	}

	public void addListener(@NonNull BackupExecutorListener listener) {
		listeners = CollectionUtils.addToList(listeners, listener);
	}

	public void removeListener(@NonNull BackupExecutorListener listener) {
		listeners = CollectionUtils.removeFromList(listeners, listener);
	}

	public void runCommand(@NonNull BackupCommand command) {
		updateActiveCommands();
		if (command.getStatus() == AsyncTask.Status.PENDING) {
			activeCommands = CollectionUtils.addToList(activeCommands, command);
			OsmAndTaskManager.executeTask(command, this, (Object[]) null);
		}
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public <T extends BackupCommand> T getActiveCommand(@NonNull Class<T> commandClass) {
		updateActiveCommands();
		for (BackupCommand command : activeCommands) {
			if (commandClass.isInstance(command)) {
				return (T) command;
			}
		}
		return null;
	}

	@Nullable
	private State updateState(boolean beforeExecute) {
		updateActiveCommands();
		State newState = beforeExecute || !getQueue().isEmpty() || !activeCommands.isEmpty() ? State.BUSY : State.IDLE;
		State oldState = State.getByOrdinal(aState.getAndSet(newState.ordinal()));
		return newState != oldState ? newState : null;
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		State newState = updateState(true);
		if (newState != null) {
			notifyStateChanged(newState);
		}
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		State newState = updateState(false);
		if (newState != null) {
			notifyStateChanged(newState);
		}
	}

	private void notifyStateChanged(State state) {
		app.runInUIThread(() -> {
			for (BackupExecutorListener listener : listeners) {
				listener.onBackupExecutorStateChanged(state);
			}
		});
	}

	private void updateActiveCommands() {
		Set<BackupCommand> commandsToRemove = new HashSet<>();
		for (BackupCommand command : activeCommands) {
			if (command.getStatus() == AsyncTask.Status.FINISHED) {
				commandsToRemove.add(command);
			}
		}
		activeCommands = CollectionUtils.removeAllFromList(activeCommands, commandsToRemove);
	}
}
