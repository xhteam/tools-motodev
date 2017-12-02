/*
 * Copyright (C) 2007 The Android Open Source Project
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

package org.eclipse.andmore.ddms.views;

import com.android.ddmuilib.AllocationPanel;

import org.eclipse.andmore.base.resources.ImageFactory;
import org.eclipse.andmore.ddms.DdmsPlugin;
import org.eclipse.swt.widgets.Composite;

public class AllocTrackerView extends TableView {

	public static final String ID = "org.eclipse.andmore.ddms.views.AllocTrackerView"; //$NON-NLS-1$
	private AllocationPanel mPanel;
	private ImageFactory mImageFactory;

	public AllocTrackerView() {
		super();
		mImageFactory = DdmsPlugin.getDefault().getImageFactory();
	}

	@Override
	public void createPartControl(Composite parent) {
		mPanel = new AllocationPanel(mImageFactory);
		mPanel.createPanel(parent);

		setSelectionDependentPanel(mPanel);

		// listen to focus changes for table(s) of the panel.
		setupTableFocusListener(mPanel, parent);
	}

	@Override
	public void setFocus() {
		mPanel.setFocus();
	}
}
