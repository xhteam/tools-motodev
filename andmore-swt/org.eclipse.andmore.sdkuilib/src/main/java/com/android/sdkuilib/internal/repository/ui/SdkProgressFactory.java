/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 
 */
package com.android.sdkuilib.internal.repository.ui;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.repository.api.ProgressIndicator;
import com.android.repository.api.ProgressIndicatorAdapter;
import com.android.repository.api.ProgressRunner;
import com.android.sdkuilib.internal.repository.ITask;
import com.android.sdkuilib.internal.repository.ITaskFactory;
import com.android.sdkuilib.internal.repository.ITaskMonitor;
import com.android.sdkuilib.internal.tasks.ILogUiProvider;
import com.android.sdkuilib.internal.tasks.ProgressTask;
import com.android.sdkuilib.internal.tasks.ProgressView;
import com.android.sdkuilib.internal.tasks.SdkProgressIndicator;
import com.android.utils.ILogger;

/**
 * An {@link ITaskFactory} that creates a new {@link ProgressTask} dialog
 * for each new task.
 */
public class SdkProgressFactory extends ProgressIndicatorAdapter implements ITaskFactory, ILogger, ProgressRunner {

	public interface ISdkLogWindow
	{
		void log(String log);
		void logVerbose(String log);
		void logError(String log);
		void setDescription(String description);
		void show();
	}
	
    private final ProgressView progressView;
    private final ISdkLogWindow logWindow;
    private int referenceCount;

    /**
     * Creates a new {@link ProgressView} object, a simple "holder" for the various
     * widgets used to display and update a progress + status bar. This object is
     * provided to the factory.
     *
     * @param statusText The label to display titles of status updates (e.g. task titles and
     *      calls to {@link #setDescription(String)}.) Must not be null.
     * @param progressBar The progress bar to update during a task. Must not be null.
     * @param stopButton The stop button. It will be disabled when there's no task that can
     *      be interrupted. A selection listener will be attached to it. Optional. Can be null.
     * @param logWindow Log adapter which can be requested to become visible when errors are logged
     */
    public SdkProgressFactory(
            Label statusText,
            ProgressBar progressBar,
            Control stopButton,
	        ISdkLogWindow logWindow) {
    	this.logWindow = logWindow;
		this.progressView = new ProgressView(statusText, progressBar, stopButton, getLogUiProvider(logWindow));
	}

    public ILogUiProvider getProgressControl()
    {
    	return progressView;
    }

    /**
     * Starts a new task with a new {@link ITaskMonitor}.
     * The task will execute asynchronously in a job.
     * @param title The title of the task, displayed in the monitor if any.
     * @param task The task to run.
     * @param onTerminateTask Callback when task done
     */
    @Override
    public void start(String title, ITask task, Runnable onTerminateTask) {
        progressView.startAsyncTask(title, task, onTerminateTask);
    }
    
    // Returns object which delegates all logging to the logWindow window
    // and filters errors to make sure the window is visible when
    // an error is logged.
    private ILogUiProvider getLogUiProvider(ISdkLogWindow logWindow)
    {
        return new ILogUiProvider() {
	        @Override
	        public void setDescription(String description) {
	            logWindow.setDescription(description);
	        }
	
	        @Override
	        public void log(String log) {
	            logWindow.log(log);
	        }
	
	        @Override
	        public void logVerbose(String log) {
	            logWindow.logVerbose(log);
	        }
	
	        @Override
	        public void logError(String log) {
	            logWindow.logError(log);
	            logWindow.show();
	    }};
    }

    // --- ILogger interface ---- //
	@Override
	public void error(Throwable throwable, String errorFormat, Object... arg) {
		StringWriter builder = new StringWriter();
        if (errorFormat != null) 
            builder.append(String.format("Error: " + errorFormat, arg));

        if (throwable != null) {
        	if (errorFormat == null) 
                builder.append("Error: ").append(throwable.getMessage());
            builder.append("\n");
            PrintWriter writer = new PrintWriter(builder);
            throwable.printStackTrace(writer);
        }
        logWindow.logError(builder.toString());
        logWindow.show();
	}


	@Override
	public void info(String errorFormat, Object... arg) {
		logWindow.log(String.format(errorFormat, arg));
	}


	@Override
	public void verbose(String errorFormat, Object... arg) {
		logWindow.logVerbose(String.format(errorFormat, arg));
	}


	@Override
	public void warning(String errorFormat, Object... arg) {
		// TODO - Add warning level
		logWindow.log(String.format(errorFormat, arg));
	}

    // --- Logger component of IProgressIndicator interface ---- //
    /**
     * Logs a warning.
     */
	@Override
    public void logWarning(@NonNull String s) {
		logWindow.log(s);
	}

    /**
     * Logs a warning, including a stacktrace.
     */
	@Override
	public void logWarning(@NonNull String s, @Nullable Throwable throwable) {
		StringWriter builder = new StringWriter();
		builder.append(s);
		if (throwable != null) {
	        builder.append("\n");
            PrintWriter writer = new PrintWriter(builder);
            throwable.printStackTrace(writer);
		}
		logWindow.log(builder.toString());
	}

    /**
     * Logs an error.
     */
	@Override
	public void logError(@NonNull String s) {
		error(null, s);
	}

    /**
     * Logs an error, including a stacktrace.
     */
	@Override
	public void logError(@NonNull String s, @Nullable Throwable throwable) {
		error(throwable, s);
    }

    /**
     * Logs an info message.
     */
	@Override
	public void logInfo(@NonNull String s) {
		logWindow.log(s);
	}

	@Override
	public void logVerbose(@NonNull String s) {
		logWindow.logVerbose(s);
	}


	@Override
	public void runAsyncWithProgress(final ProgressRunnable progressRunnable) {
		String title = this.getClass().getSimpleName() + referenceCount++;
		ProgressRunnable monitor = new ProgressRunnable(){

			@Override
			public void run(ProgressIndicator indicator, ProgressRunner runner) {
				try
				{
				    progressRunnable.run(indicator, runner);
				}
				finally
				{
					progressView.endTask();
				}
			}};
		progressView.startAsyncTask(title, taskInstance(monitor), null);
	}


	@Override
	public void runSyncWithProgress(ProgressRunnable progressRunnable) {
		String title = this.getClass().getSimpleName() + referenceCount++;
        progressView.startSyncTask(title, taskInstance(progressRunnable));
    }


	@Override
	public void runSyncWithoutProgress(Runnable runnable) {
		runnable.run();
	}
	
	private ITask taskInstance(ProgressRunnable progressRunnable)
	{
		return new ITask(){
            
			@Override
			public void run(ITaskMonitor monitor) {
				ProgressIndicator progressIndicator = new SdkProgressIndicator(monitor);
				ProgressRunner progressRunner = new ProgressRunner(){

					@Override
					public void runAsyncWithProgress(ProgressRunnable r) {
						if (!progressIndicator.isCanceled())
							SdkProgressFactory.this.runAsyncWithProgress(r);
					}

					@Override
					public void runSyncWithProgress(ProgressRunnable r) {
						if (!progressIndicator.isCanceled())
							SdkProgressFactory.this.runSyncWithProgress(r);
					}

					@Override
					public void runSyncWithoutProgress(Runnable r) {
						if (!progressIndicator.isCanceled())
							SdkProgressFactory.this.runSyncWithoutProgress(r);
					}};
				progressRunnable.run(progressIndicator, progressRunner);
			}
		};

	}
}
