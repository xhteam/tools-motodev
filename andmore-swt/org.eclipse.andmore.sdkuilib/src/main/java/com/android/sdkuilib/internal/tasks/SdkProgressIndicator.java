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
package com.android.sdkuilib.internal.tasks;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.android.repository.api.ProgressIndicator;
import com.android.sdkuilib.internal.repository.ITaskMonitor;

/**
 * @author Andrew Bowley
 *
 * 12-11-2017
 */
public class SdkProgressIndicator implements ProgressIndicator {
	// Use Monitor max count value convert fractions to integer values
    private static final int MAX_COUNT = 10000;

	private final ITaskMonitor monitor;
    private volatile boolean isCancelled = false;
    private volatile boolean cancellable = true;
    // TODO - implement indeterminate cursor
    private volatile boolean indeterminate = false;
	
	/**
	 * 
	 */
	public SdkProgressIndicator(ITaskMonitor monitor) {
		this.monitor = monitor;
		monitor.setProgressMax(MAX_COUNT);
	}

	/* (non-Javadoc)
	 * @see com.android.repository.api.ProgressIndicator#cancel()
	 */
	@Override
	public void cancel() {
		if (cancellable)
			// TODO - Consider informing user request denied or disable stop button when not cancellable
			isCancelled = true;
	}

	/* (non-Javadoc)
	 * @see com.android.repository.api.ProgressIndicator#getFraction()
	 */
	@Override
	public double getFraction() {
		return MAX_COUNT / monitor.getProgress();
	}

	/* (non-Javadoc)
	 * @see com.android.repository.api.ProgressIndicator#isCanceled()
	 */
	@Override
	public boolean isCanceled() {
		return isCancelled || monitor.isCancelRequested();
	}

	/* (non-Javadoc)
	 * @see com.android.repository.api.ProgressIndicator#isCancellable()
	 */
	@Override
	public boolean isCancellable() {
		return cancellable;
	}

	/* (non-Javadoc)
	 * @see com.android.repository.api.ProgressIndicator#isIndeterminate()
	 */
	@Override
	public boolean isIndeterminate() {
		return indeterminate;
	}

	/* (non-Javadoc)
	 * @see com.android.repository.api.ProgressIndicator#logError(java.lang.String)
	 */
	@Override
	public void logError(String message) {
		monitor.logError(message);
	}

	/* (non-Javadoc)
	 * @see com.android.repository.api.ProgressIndicator#logError(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void logError(String message, Throwable throwable) {
		monitor.error(throwable, message);
	}

	/* (non-Javadoc)
	 * @see com.android.repository.api.ProgressIndicator#logInfo(java.lang.String)
	 */
	@Override
	public void logInfo(String message) {
		monitor.info(message);
	}

	/* (non-Javadoc)
	 * @see com.android.repository.api.ProgressIndicator#logWarning(java.lang.String)
	 */
	@Override
	public void logWarning(String message) {
		monitor.warning(message);
	}

	/* (non-Javadoc)
	 * @see com.android.repository.api.ProgressIndicator#logWarning(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void logWarning(String message, Throwable throwable) {
		StringWriter builder = new StringWriter();
		builder.append(message);
		if (throwable != null) {
	        builder.append("\n");
            PrintWriter writer = new PrintWriter(builder);
            throwable.printStackTrace(writer);
		}
		monitor.warning(builder.toString());
	}

	/* (non-Javadoc)
	 * @see com.android.repository.api.ProgressIndicator#setCancellable(boolean)
	 */
	@Override
	public void setCancellable(boolean cancellable) {
		this.cancellable = cancellable;
	}

	/* (non-Javadoc)
	 * @see com.android.repository.api.ProgressIndicator#setFraction(double)
	 */
	@Override
	public void setFraction(double fraction) {
		int progress = fraction == 1.0 ? MAX_COUNT : (int)((double)MAX_COUNT * fraction);
		monitor.incProgress(progress - monitor.getProgress());
	}

	/* (non-Javadoc)
	 * @see com.android.repository.api.ProgressIndicator#setIndeterminate(boolean)
	 */
	@Override
	public void setIndeterminate(boolean indeterminate) {
		this.indeterminate = indeterminate;
	}

	/* (non-Javadoc)
	 * @see com.android.repository.api.ProgressIndicator#setSecondaryText(java.lang.String)
	 */
	@Override
	public void setSecondaryText(String text) {
		// TODO - implement secondary text
		monitor.logVerbose(text);
	}

	/* (non-Javadoc)
	 * @see com.android.repository.api.ProgressIndicator#setText(java.lang.String)
	 */
	@Override
	public void setText(String text) {
		monitor.setDescription(text);
	}

}
