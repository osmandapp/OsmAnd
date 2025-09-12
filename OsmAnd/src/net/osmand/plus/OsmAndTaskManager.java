package net.osmand.plus;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.plugins.PluginsHelper;

import org.apache.commons.logging.Log;

import java.util.concurrent.Executor;

public class OsmAndTaskManager {

	private static final Log LOG = PlatformUtil.getLog(OsmAndTaskManager.class);

	private final OsmandApplication app;

	public OsmAndTaskManager(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public <Params, Progress, Result> OsmAndTask<Params, Progress, Result> runInBackground(
			OsmAndTaskRunnable<Params, Progress, Result> r, Params... params) {
		InternalTaskExecutor<Params, Progress, Result> exec = new InternalTaskExecutor<Params, Progress, Result>(r);
		r.exec = exec;
		executeTask(exec, params);
		return exec;
	}

	
	private class InternalTaskExecutor<Params, Progress, Result> 
			extends AsyncTask<Params, Progress, Result>
			implements OsmAndTask<Params, Progress, Result> {
		private final OsmAndTaskRunnable<Params, Progress, Result> run;
		
		private InternalTaskExecutor(OsmAndTaskRunnable<Params, Progress, Result> r){
			this.run = r;
		}
		
		@Override
		protected Result doInBackground(Params... params) {
			return run.doInBackground(params);
		}
		
		@Override
		protected void onPreExecute() {
			run.onPreExecute();
		}
		
		@Override
		protected void onPostExecute(Result result) {
			run.onPostExecute(result);
		}
		
		@Override
		protected void onProgressUpdate(Progress... values) {
			run.onProgressUpdate(values);
		}

		@Override
		public void executeWithParams(Params... params) {
			executeTask(this, params);
		}
	}
	
	
	public interface OsmAndTask<Params, Progress, Result> {
		
		boolean isCancelled();
		
		boolean cancel(boolean mayInterruptDuringRun);
		
		void executeWithParams(Params... params);
		
	}
	
	public abstract static class OsmAndTaskRunnable<Params, Progress, Result> {
		
		public OsmAndTask<Params, Progress, Result> exec;
		
		protected void onPreExecute() {}
		
		protected String getName() { return "Runnable";}
		
		protected abstract Result doInBackground(Params... params);
		
		protected void onPostExecute(Result result) {}
		
		protected void onProgressUpdate(Progress... values) {}
		
	}

	public static <P, T extends AsyncTask<P, ?, ?>> T executeTask(@NonNull T task, @Nullable P... params) {
		return executeTask(task, THREAD_POOL_EXECUTOR, params);
	}

	public static <P, T extends AsyncTask<P, ?, ?>> T executeTask(@NonNull T task, @NonNull Executor executor, @Nullable P... params) {
		if (PluginsHelper.isDevelopment()) {
			LOG.info("Submitting task: " + task.getClass().getName());
		}
		return (T) task.executeOnExecutor(executor, params);
	}
}
