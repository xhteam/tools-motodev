/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*******************************************************************************
* Copyright (c) 2015 David Carver and others
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* David Carver - removing usage tracker implementation.
*******************************************************************************/

package org.eclipse.andmore.internal.welcome;

import com.android.sdkstats.DdmsPreferenceStore;
import com.android.sdkuilib.internal.repository.ui.AdtUpdateDialog;

import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.sdk.AdtConsoleSdkLog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Wizard shown on first start for new users: configure SDK location, accept or
 * reject usage data collection, etc
 */
public class WelcomeWizard extends Wizard {
    private final DdmsPreferenceStore mStore;

    private WelcomeWizardPage mWelcomePage;

    private final boolean mShowWelcomePage;

    /**
     * Creates a new {@link WelcomeWizard}
     *
     * @param store preferences for usage statistics collection etc
     * @param showInstallSdkPage show page to install SDK's
     * @param showUsageOptinPage show page to get user consent for usage data collection
     */
    public WelcomeWizard(DdmsPreferenceStore store, boolean showInstallSdkPage) {
        mStore = store;
        mShowWelcomePage = showInstallSdkPage;

        setWindowTitle("Welcome to Android Development");
        ImageDescriptor image = AndmoreAndroidPlugin.getImageDescriptor("icons/android-64.png"); //$NON-NLS-1$
        setDefaultPageImageDescriptor(image);
    }

    @Override
    public void addPages() {
        if (mShowWelcomePage) {
            mWelcomePage = new WelcomeWizardPage();
            addPage(mWelcomePage);
        }
    }

    @Override
    public boolean performFinish() {

        if (mWelcomePage != null) {
            // Read out wizard settings immediately; we will perform the actual work
            // after the wizard window has been taken down and it's too late to read the
            // settings then
            final File path = mWelcomePage.getPath();
            final boolean installCommon = mWelcomePage.isInstallCommon();
            final boolean installLatest = mWelcomePage.isInstallLatest();
            final boolean createNew = mWelcomePage.isCreateNew();

            // Perform installation asynchronously since it takes a while.
            getShell().getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    if (createNew) {
                        try {
                            Set<Integer> apiLevels = new HashSet<Integer>();
                            if (installCommon) {
                                apiLevels.add(8);
                            }
                            if (installLatest) {
                                apiLevels.add(AdtUpdateDialog.USE_MAX_REMOTE_API_LEVEL);
                            }
                            installSdk(path, apiLevels);
                        } catch (Exception e) {
                            AndmoreAndroidPlugin.logAndPrintError(e, "Andmore Welcome Wizard",
                                    "Installation failed");
                        }
                    }

                    // Set SDK path after installation since this will trigger a SDK refresh.
                    AdtPrefs.getPrefs().setSdkLocation(path);
                }
            });
        }

        // The wizard always succeeds, even if installation fails or is aborted
        return true;
    }

    /**
     * Trigger the install window. It will connect to the repository, display
     * a confirmation window showing which packages are selected for install
     * and display a progress dialog during installation.
     */
    private boolean installSdk(File path, Set<Integer> apiLevels) {
        if (!path.isDirectory()) {
            if (!path.mkdirs()) {
                AndmoreAndroidPlugin.logAndPrintError(null, "Andmore Welcome Wizard",
                        "Failed to create directory %1$s",
                        path.getAbsolutePath());
                return false;
            }
        }

        // Get a shell to use for the SDK installation. There are cases where getActiveShell
        // returns null so attempt to obtain it through other means.
        Display display = AndmoreAndroidPlugin.getDisplay();
        Shell shell = display.getActiveShell();
        if (shell == null) {
            IWorkbench workbench = PlatformUI.getWorkbench();
            IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
            if (window != null) {
                shell = window.getShell();
            }
        }
        boolean disposeShell = false;
        if (shell == null) {
            shell = new Shell(display);
            AndmoreAndroidPlugin.log(IStatus.WARNING, "No parent shell for SDK installation dialog");
            disposeShell = true;
        }

        AdtUpdateDialog updater = new AdtUpdateDialog(
                shell,
                new AdtConsoleSdkLog(),
                path.getAbsolutePath());
        // Note: we don't have to specify tools & platform-tools since they
        // are required dependencies of any platform.
        boolean result = updater.installNewSdk(apiLevels);

        // TODO: Install extra package here as well since it is now core to most of
        // the templates
        // if (result) {
        //     updater.installExtraPackage(vendor, path);
        // }

        if (disposeShell) {
            shell.dispose();
        }

        if (!result) {
            AndmoreAndroidPlugin.printErrorToConsole("Failed to install Android SDK.");
            return false;
        }

        return true;
    }
}
