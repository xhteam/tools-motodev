/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdkuilib.internal.widgets;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.android.sdklib.internal.avd.AvdInfo.AvdStatus;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdkuilib.internal.repository.avd.AvdAgent;
import com.android.sdkuilib.ui.GridDataBuilder;
import com.android.sdkuilib.ui.GridLayoutBuilder;
import com.android.sdkuilib.ui.SwtBaseDialog;

/**
 * Dialog displaying the details of an AVD.
 */
final class AvdDetailsDialog extends SwtBaseDialog {

    private final AvdAgent mAvdAgent;
    private volatile int row = 0;

    public AvdDetailsDialog(Shell shell, AvdAgent avdAgent) {
        super(shell, SWT.APPLICATION_MODAL, "AVD details");
        mAvdAgent = avdAgent;
    }

    /**
     * Create contents of the dialog.
     */
    @Override
    protected void createContents() {
        Shell shell = getShell();
        GridLayoutBuilder.create(shell).columns(2);
        GridDataBuilder.create(shell).fill();

        GridLayout gl;

        Composite c = new Composite(shell, SWT.NONE);
        c.setLayout(gl = new GridLayout(2, false));
        gl.marginHeight = gl.marginWidth = 0;
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        c.setLayoutData(gridData);
        //Display display = c.getDisplay();
        //c.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        //c.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));

        if (mAvdAgent != null) {
            displayValue(c, "Name:", mAvdAgent.getAvd().getName());
            displayValue(c, "CPU/ABI:", mAvdAgent.getPrettyAbiType());
            displayValue(c, "Path:", mAvdAgent.getPath());
            if (mAvdAgent.getAvd().getStatus() != AvdStatus.OK) {
                displayValue(c, "Error:", mAvdAgent.getAvd().getErrorMessage());
            } else {
                displayValue(c, "Target:", mAvdAgent.getTargetDisplayName());
                displayValue(c, "Skin:", mAvdAgent.getSkin());
                String sdcard = mAvdAgent.getSdcard();
                if (!sdcard.isEmpty()) {
                    displayValue(c, "SD Card:", sdcard);
                }
                String snapshot = mAvdAgent.getSnapshot();
                if (!snapshot.isEmpty()) {
                    displayValue(c, "Snapshot:", snapshot);
                }
                // display other hardware
                HashMap<String, String> copy = new HashMap<String, String>(mAvdAgent.getAvd().getProperties());
                // remove stuff we already displayed (or that we don't want to display)
                copy.remove(AvdManager.AVD_INI_ABI_TYPE);
                copy.remove(AvdManager.AVD_INI_CPU_ARCH);
                copy.remove(AvdManager.AVD_INI_SKIN_NAME);
                copy.remove(AvdManager.AVD_INI_SKIN_PATH);
                copy.remove(AvdManager.AVD_INI_SDCARD_SIZE);
                copy.remove(AvdManager.AVD_INI_SDCARD_PATH);
                copy.remove(AvdManager.AVD_INI_IMAGES_1);
                copy.remove(AvdManager.AVD_INI_IMAGES_2);

                if (copy.size() > 0) {
                    Label l = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
                    l.setLayoutData(new GridData(
                            GridData.FILL, GridData.HORIZONTAL_ALIGN_BEGINNING, false, false, 2, 1));
                    //display = l.getDisplay();
                    //l.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
                    c = new Composite(shell, SWT.NONE);
                    c.setLayout(gl = new GridLayout(2, false));
                    //display = c.getDisplay();
                    gl.marginHeight = gl.marginWidth = 0;
                    //c.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
                    c.setLayoutData(new GridData(GridData.FILL_BOTH));
                    //c.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
                    //c.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
                    for (Map.Entry<String, String> entry : copy.entrySet()) {
                        displayValue(c, entry.getKey() + ":", entry.getValue());
                    }
                }
            }
        }
    }

    // -- Start of internal part ----------
    // Hide everything down-below from SWT designer
    //$hide>>$


    @Override
    protected void postCreate() {
        // pass
    }

    /**
     * Displays a value with a label.
     *
     * @param parent the parent Composite in which to display the value. This Composite must use a
     * {@link GridLayout} with 2 columns.
     * @param label the label of the value to display.
     * @param value the string value to display.
     */
    private void displayValue(Composite parent, String key, String value) {
        Label label = new Label(parent, SWT.LEFT);
        label.setLayoutData(new GridData(GridData.FILL, GridData.VERTICAL_ALIGN_CENTER, false, false));
        Display display = label.getDisplay();
        if ((row & 1) == 0) {
	        label.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
	        label.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
        }
        label.setText(key);

        label = new Label(parent, SWT.LEFT);
        label.setLayoutData(new GridData(GridData.FILL, GridData.VERTICAL_ALIGN_CENTER, true, false));
        display = label.getDisplay();
        if ((row & 1) == 0) {
	        label.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
	        label.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
        }
        label.setText(value);
        ++row;
    }

    // End of hiding from SWT Designer
    //$hide<<$
}
