/*******************************************************************************
 * Copyright (c) 2006-2010 eBay Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *******************************************************************************/

// copied from Ebay Turmeric Plugin project and used for monitoring dialogs to
// be dismissed.
package org.eclipse.andmore.integration.tests;


import org.eclipse.swt.widgets.Display;
import org.junit.Assert;

/**
 * 
 * @author ddodd
 * 
 *         The DialogMonitor is used to detect dialogs which appear during a UI
 *         plugin unit tests. The IDialogProcessor can be customize different
 *         types of dialogs. Once the test is over, the passed in IDialogProcess
 *         can be used to query any information during the test.
 * 
 *         See DefaultDialogProcessor
 * 
 */
public class DialogMonitor {

	DialogMonitorJob m_dialogMonitorJob;

	IDialogProcessor m_dialogProcessor;

	boolean syncMode = true;// is in sync mode,default to true

	public boolean isSyncMode() {
		return syncMode;
	}

	public void setSyncMode(boolean syncMode) {
		this.syncMode = syncMode;
	}

	/**
	 * Create the default processor
	 * 
	 */
	public DialogMonitor() {
		m_dialogProcessor = new DefaultDialogProcessor();
	}

	public DialogMonitor(IDialogProcessor dialogProcessor) {
		m_dialogProcessor = dialogProcessor;
	}

	/**
	 * Fire up the thread and start looking for the dialog
	 * 
	 */
	public void startMonitoring() {

		final Display m_display = Display.getCurrent();

		Assert.assertNotNull(m_display);

		m_dialogMonitorJob = new DialogMonitorJob(m_display, m_dialogProcessor,
				isSyncMode());
		m_dialogMonitorJob.start();
	}

	/**
	 * Tell the thread to stop.
	 * 
	 * @return
	 */
	public void stopMonitoring() {

		m_dialogMonitorJob.setAllDone(true);

	}
}