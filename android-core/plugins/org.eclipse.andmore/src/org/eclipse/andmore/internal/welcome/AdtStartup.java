/*
 * Copyright (C) 2011 - 2015 The Android Open Source Project
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
 * 
 * Contributors:
 * David Carver - bug 462184 - remove usage tracker
 * 
 */

package org.eclipse.andmore.internal.welcome;

import com.android.SdkConstants;
import com.android.annotations.Nullable;
import com.android.utils.GrabProcessOutput;
import com.android.utils.GrabProcessOutput.IProcessOutput;
import com.android.utils.GrabProcessOutput.Wait;
import com.android.sdkstats.DdmsPreferenceStore;
import com.android.sdkstats.SdkStatsService;

import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.AndmoreAndroidPlugin.CheckSdkErrorHandler;
import org.eclipse.andmore.base.InstallDetails;
import org.eclipse.andmore.internal.editors.layout.gle2.LayoutWindowCoordinator;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Andmore startup tasks (other than those performed in {@link AndmoreAndroidPlugin#start(org.osgi.framework.BundleContext)}
 * when the plugin is initializing.
 * <p>
 * The main tasks currently performed are:
 * <ul>
 *   <li> See if the user has ever run the welcome wizard, and if not, run it
 * </ul>
 */
public class AdtStartup implements IStartup, IWindowListener {

    private DdmsPreferenceStore mStore = new DdmsPreferenceStore();

    @Override
    public void earlyStartup() {
        if (!isSdkSpecified()) {
            File bundledSdk = getBundledSdk();
            if (bundledSdk != null) {
                AdtPrefs.getPrefs().setSdkLocation(bundledSdk);
            }
        }

        boolean showSdkInstallationPage = !isSdkSpecified() && isFirstTime();

        if (showSdkInstallationPage) {
            showWelcomeWizard(showSdkInstallationPage);
        }

        initializeWindowCoordinator();

        AndmoreAndroidPlugin.getDefault().workbenchStarted();
    }

    private boolean isSdkSpecified() {
        String osSdkFolder = AdtPrefs.getPrefs().getOsSdkFolder();
        return (osSdkFolder != null && !osSdkFolder.isEmpty());
    }

    /**
     * Returns the path to the bundled SDK if this is part of the ADT package.
     * The ADT package has the following structure:
     *   root
     *      |--eclipse
     *      |--sdk
     * @return path to bundled SDK, null if no valid bundled SDK detected.
     */
    private File getBundledSdk() {
        Location install = Platform.getInstallLocation();
        if (install != null && install.getURL() != null) {
            File toolsFolder = new File(install.getURL().getFile()).getParentFile();
            if (toolsFolder != null) {
                File sdkFolder = new File(toolsFolder, "sdk");
                if (sdkFolder.exists() && AndmoreAndroidPlugin.getDefault().checkSdkLocationAndId(
                        sdkFolder.getAbsolutePath(),
                        new SdkValidator())) {
                    return sdkFolder;
                }
            }
        }

        return null;
    }

    private boolean isFirstTime() {
        for (int i = 0; i < 2; i++) {
            String osSdkPath = null;

            if (i == 0) {
                // If we've recorded an SDK location in the .android settings, then the user
                // has run ADT before but possibly in a different workspace. We don't want to pop up
                // the welcome wizard each time if we can simply use the existing SDK install.
                osSdkPath = mStore.getLastSdkPath();
            } else if (i == 1) {
                osSdkPath = getSdkPathFromWindowsRegistry();
            }

            if (osSdkPath != null && osSdkPath.length() > 0) {
                boolean ok = new File(osSdkPath).isDirectory();

                if (!ok) {
                    osSdkPath = osSdkPath.trim();
                    ok = new File(osSdkPath).isDirectory();
                }

                if (ok) {
                    // Verify that the SDK is valid
                    ok = AndmoreAndroidPlugin.getDefault().checkSdkLocationAndId(
                            osSdkPath, new SdkValidator());
                    if (ok) {
                        // Yes, we've seen an SDK location before and we can use it again,
                        // no need to pester the user with the welcome wizard.
                        // This also implies that the user has responded to the usage statistics
                        // question.
                        AdtPrefs.getPrefs().setSdkLocation(new File(osSdkPath));
                        return false;
                    }
                }
            }
        }

        // Check whether we've run this wizard before.
        return !mStore.isAdtUsed();
    }

    private static class SdkValidator extends AndmoreAndroidPlugin.CheckSdkErrorHandler {
        @Override
        public boolean handleError(
                CheckSdkErrorHandler.Solution solution,
                String message) {
            return false;
        }

        @Override
        public boolean handleWarning(
                CheckSdkErrorHandler.Solution  solution,
                String message) {
            return true;
        }
    }

    private String getSdkPathFromWindowsRegistry() {
        if (SdkConstants.CURRENT_PLATFORM != SdkConstants.PLATFORM_WINDOWS) {
            return null;
        }

        final String valueName = "Path";                                               //$NON-NLS-1$
        final AtomicReference<String> result = new AtomicReference<String>();
        final Pattern regexp =
            Pattern.compile("^\\s+" + valueName + "\\s+REG_SZ\\s+(.*)$");//$NON-NLS-1$ //$NON-NLS-2$

        for (String key : new String[] {
                "HKLM\\Software\\Android SDK Tools",                                   //$NON-NLS-1$
                "HKLM\\Software\\Wow6432Node\\Android SDK Tools" }) {                  //$NON-NLS-1$

            String[] command = new String[] {
                "reg", "query", key, "/v", valueName       //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            };

            Process process;
            try {
                process = Runtime.getRuntime().exec(command);

                GrabProcessOutput.grabProcessOutput(
                        process,
                        Wait.WAIT_FOR_READERS,
                        new IProcessOutput() {
                            @Override
                            public void out(@Nullable String line) {
                                if (line != null) {
                                    Matcher m = regexp.matcher(line);
                                    if (m.matches()) {
                                        result.set(m.group(1));
                                    }
                                }
                            }

                            @Override
                            public void err(@Nullable String line) {
                                // ignore stderr
                            }
                        });
            } catch (IOException ignore) {
            } catch (InterruptedException ignore) {
            }

            String str = result.get();
            if (str != null) {
                if (new File(str).isDirectory()) {
                    return str;
                }
                str = str.trim();
                if (new File(str).isDirectory()) {
                    return str;
                }
            }
        }

        return null;
    }

    private void initializeWindowCoordinator() {
        final IWorkbench workbench = PlatformUI.getWorkbench();
        workbench.addWindowListener(this);
        workbench.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
                    LayoutWindowCoordinator.get(window, true /*create*/);
                }
            }
        });
    }

    private void showWelcomeWizard(final boolean showSdkInstallPage) {
        final IWorkbench workbench = PlatformUI.getWorkbench();
        workbench.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
                if (window != null) {
                    WelcomeWizard wizard = new WelcomeWizard(mStore, showSdkInstallPage);
                    WizardDialog dialog = new WizardDialog(window.getShell(), wizard);
                    dialog.open();
                }
            }
        });
    }

    // ---- Implements IWindowListener ----

    @Override
    public void windowActivated(IWorkbenchWindow window) {
    }

    @Override
    public void windowDeactivated(IWorkbenchWindow window) {
    }

    @Override
    public void windowClosed(IWorkbenchWindow window) {
        LayoutWindowCoordinator listener = LayoutWindowCoordinator.get(window, false /*create*/);
        if (listener != null) {
            listener.dispose();
        }
    }

    @Override
    public void windowOpened(IWorkbenchWindow window) {
        LayoutWindowCoordinator.get(window, true /*create*/);
    }
}
