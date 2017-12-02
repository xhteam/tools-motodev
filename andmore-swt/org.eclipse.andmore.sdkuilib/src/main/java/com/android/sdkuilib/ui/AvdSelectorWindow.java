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
package com.android.sdkuilib.ui;

import org.eclipse.andmore.sdktool.SdkCallAgent;
import org.eclipse.andmore.sdktool.SdkContext;
import org.eclipse.andmore.sdktool.Utilities;
import org.eclipse.andmore.sdktool.Utilities.Compatibility;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;

import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdkuilib.internal.repository.avd.AvdAgent;
import com.android.sdkuilib.internal.repository.avd.SdkTargets;
import com.android.sdkuilib.internal.repository.avd.SystemImageInfo;
import com.android.sdkuilib.internal.widgets.AvdSelector;
import com.android.sdkuilib.internal.widgets.AvdSelector.IAvdFilter;

/**
 * A control to select an Android Virtual Device (AVD)
 * @author Andrew Bowley
 *
 */
public class AvdSelectorWindow {

	private final AvdSelector avdSelector;
	private final SdkContext sdkContext;
    private final SdkTargets sdkTargets;
	
	public AvdSelectorWindow(Composite parent, SdkCallAgent sdkCallAgent) {
		this.sdkContext = sdkCallAgent.getSdkContext();
    	sdkTargets = new SdkTargets(sdkContext);
		avdSelector = new AvdSelector(parent, sdkContext, (IAvdFilter)null, AvdDisplayMode.SIMPLE_CHECK);
	}

	/**
     * Sets the current target selection.
     * <p/>
     * If the selection is actually changed, this will invoke the selection listener
     * (if any) with a null event.
     *
     * @param target the target to be selected. Use null to deselect everything.
     * @return true if the target could be selected, false otherwise.
     */
	public void setSelection(AvdInfo avd) {
		avdSelector.setSelection(avd);
	}

    /**
     * Returns the currently selected item. In {@link DisplayMode#SIMPLE_CHECK} mode this will
     * return the {@link AvdInfo} that is checked instead of the list selection.
     *
     * @return The currently selected item or null.
     */
    public AvdInfo getSelected() {
    	AvdAgent avdAgent = avdSelector.getSelected();
    	return avdAgent != null ? avdAgent.getAvd() : null;
    }
    
    /**
     * Sets the table grid layout data.
     *
     * @param heightHint If > 0, the height hint is set to the requested value.
     */
    public void setTableHeightHint(int heightHint) {
    	avdSelector.setTableHeightHint(heightHint);
    }
    
    /**
     * Sets a selection listener. Set it to null to remove it.
     * The listener will be called <em>after</em> this table processed its selection
     * events so that the caller can see the updated state.
     * <p/>
     * The event's item contains a {@link TableItem}.
     * The {@link TableItem#getData()} contains an {@link IAndroidTarget}.
     * <p/>
     * It is recommended that the caller uses the {@link #getSelected()} method instead.
     * <p/>
     * The default behavior for double click (when not in {@link DisplayMode#SIMPLE_CHECK}) is to
     * display the details of the selected AVD.<br>
     * To disable it (when you provide your own double click action), set
     * {@link SelectionEvent#doit} to false in
     * {@link SelectionListener#widgetDefaultSelected(SelectionEvent)}
     *
     * @param selectionListener The new listener or null to remove it.
     */
    public void setSelectionListener(SelectionListener selectionListener) {
    	avdSelector.setSelectionListener(selectionListener);
    }

    /**
     * Enables the receiver if the argument is true, and disables it otherwise.
     * A disabled control is typically not selectable from the user interface
     * and draws with an inactive or "grayed" look.
     *
     * @param enabled the new enabled state.
     */
    public void setEnabled(boolean enabled) {
    	avdSelector.setEnabled(enabled);
    }
    
    /**
     * Sets a new AVD manager and updates AVD filter parameters
     * This also refreshes the display 
     * @param manager the AVD manager.
     */
    public void setManager(AvdManager manager, IAndroidTarget target, AndroidVersion minApiVersion) {
    	avdSelector.setManager(manager);
    	avdSelector.refresh(false);
    	avdSelector.setFilter(getCompatibilityFilter(target, minApiVersion));
    }

    private IAvdFilter getCompatibilityFilter(IAndroidTarget target, AndroidVersion minApiVersion) {
    	return new IAvdFilter() {
 
	        @Override
	        public void prepare() {
	        }
	
	        @Override
	        public void cleanup() {
	        }
	
	        @Override
	        public boolean accept(AvdAgent avdAgent) {
	        	AvdInfo info = avdAgent.getAvd();
	            Compatibility c =
	            		Utilities.canRun(info, getAndroidTargetFor(info), target, minApiVersion);
	            return (c == Compatibility.NO) ? false : true;
	        }
	    };
    }
    
    private IAndroidTarget getAndroidTargetFor(AvdInfo info) {
        SystemImageInfo systemImageInfo = new SystemImageInfo(info);
        if (systemImageInfo.hasSystemImage())
        	return sdkTargets.getTargetForSysImage(systemImageInfo.getSystemImage());
        return avdSelector.getSdkTargets().getTargetForAndroidVersion(info.getAndroidVersion());
    }
}
