/*
 * Copyright (C) 2011 The Android Open Source Project
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

import com.android.repository.api.RemotePackage;
import com.android.repository.api.RepoPackage;
import com.android.repository.api.UpdatablePackage;
import com.android.repository.impl.meta.TypeDetails;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.repository.meta.DetailsTypes.ExtraDetailsType;
import com.android.sdklib.repository.meta.DetailsTypes.PlatformDetailsType;
import com.android.sdkuilib.internal.repository.PackageManager.RemotePackageHandler;
import com.android.sdkuilib.internal.repository.PackageManager.UpdatablePackageHandler;
import com.android.sdkuilib.internal.repository.Settings;
import com.android.sdkuilib.internal.repository.content.PackageType;
import com.android.sdkuilib.internal.repository.content.PkgItem;
import com.android.sdkuilib.internal.repository.ui.LogWindow;
import com.android.sdkuilib.internal.repository.ui.SdkProgressFactory;
import com.android.sdkuilib.internal.tasks.ProgressView;
import com.android.sdkuilib.repository.SdkUpdaterWindow.SdkInvocationContext;
import com.android.sdkuilib.ui.GridDataBuilder;
import com.android.sdkuilib.ui.GridLayoutBuilder;
import com.android.sdkuilib.ui.SwtBaseDialog;
import com.android.utils.ILogger;
import com.android.utils.Pair;

import org.eclipse.andmore.sdktool.SdkCallAgent;
import org.eclipse.andmore.sdktool.SdkContext;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * TODO - Integrate this class with SdkUpdaterWindow
 * This is a private implementation of UpdateWindow for ADT,
 * designed to install a very specific package.
 * <p/>
 * Example of usage:
 * <pre>
 * AdtUpdateDialog dialog = new AdtUpdateDialog(
 *     AdtPlugin.getDisplay().getActiveShell(),
 *     new AdtConsoleSdkLog(),
 *     sdk.getSdkLocation());
 *
 * Pair<Boolean, File> result = dialog.installExtraPackage(
 *     "android", "compatibility");  //$NON-NLS-1$ //$NON-NLS-2$
 * or
 * Pair<Boolean, File> result = dialog.installPlatformPackage(11);
 * </pre>
 */
public class AdtUpdateDialog extends SwtBaseDialog {

    public static final int USE_MAX_REMOTE_API_LEVEL = 0;

    private static final String APP_NAME = "Android SDK Manager";
    private final SdkContext mSdkContext;

    private Boolean mResultCode = Boolean.FALSE;
    private Map<PkgItem, File> mResultPaths = null;
    private PackageFilter mPackageFilter;

    private ProgressBar mProgressBar;
    private Label mStatusText;
    private LogWindow mLogWindow;

    /**
     * Creates a new {@link AdtUpdateDialog}.
     * Callers will want to call {@link #installExtraPackage} or
     * {@link #installPlatformPackage} after this.
     *
     * @param parentShell The existing parent shell. Must not be null.
     * @param sdkCallAgent Mediator between application and UI layer
      */
    public AdtUpdateDialog(
            Shell parentShell,
            SdkCallAgent sdkCallAgent) {
        super(parentShell, SWT.NONE, APP_NAME);
        mSdkContext = sdkCallAgent.getSdkContext();
    }

    /**
     * Displays the update dialog and triggers installation of the requested platform
     * package with the specified API  level.
     * <p/>
     * Callers must not try to reuse this dialog after this call.
     *
     * @param apiLevel The platform API level to match.
     *  The special value {@link #USE_MAX_REMOTE_API_LEVEL} means to use
     *  the highest API level available on the remote repository.
     * @return A boolean indicating whether the installation was successful (meaning the package
     *   was either already present, or got installed or updated properly) and a {@link File}
     *   with the path to the root folder of the package. The file is null when the boolean
     *   is false, otherwise it should point to an existing valid folder.
     */
    public Pair<Boolean, File> installPlatformPackage(int apiLevel) {
        mPackageFilter = createPlatformFilter(apiLevel);
        open();

        File installPath = null;
        if (mResultPaths != null) {
            for (Entry<PkgItem, File> entry : mResultPaths.entrySet()) {
                if (entry.getKey().getMetaPackage().getPackageType() == PackageType.platforms) {
                    installPath = entry.getValue();
                    break;
                }
            }
        }

        return Pair.of(mResultCode, installPath);
    }

    /**
     * Displays the update dialog and triggers installation of the requested {@code extra}
     * package with the specified vendor and path attributes.
     * <p/>
     * Callers must not try to reuse this dialog after this call.
     *
     * @param vendor The extra package vendor string to match.
     * @param path   The extra package path   string to match.
     * @return A boolean indicating whether the installation was successful (meaning the package
     *   was either already present, or got installed or updated properly) and a {@link File}
     *   with the path to the root folder of the package. The file is null when the boolean
     *   is false, otherwise it should point to an existing valid folder.
     * @wbp.parser.entryPoint
     */
    public Pair<Boolean, File> installExtraPackage(String vendor, String path) {
        mPackageFilter = createExtraFilter(vendor, path);
        open();

        File installPath = null;
        if (mResultPaths != null) {
            for (Entry<PkgItem, File> entry : mResultPaths.entrySet()) {
                if (entry.getKey().getMetaPackage().getPackageType() == PackageType.extras) {
                    installPath = entry.getValue();
                    break;
                }
            }
        }
        return Pair.of(mResultCode, installPath);
    }

    /**
     * Displays the update dialog and triggers installation of platform-tools package.
     * <p/>
     * Callers must not try to reuse this dialog after this call.
     *
     * @return A boolean indicating whether the installation was successful (meaning the package
     *   was either already present, or got installed or updated properly) and a {@link File}
     *   with the path to the root folder of the package. The file is null when the boolean
     *   is false, otherwise it should point to an existing valid folder.
     * @wbp.parser.entryPoint
     */
    public Pair<Boolean, File> installPlatformTools() {
        mPackageFilter = createPlatformToolsFilter();
        open();

        File installPath = null;
        if (mResultPaths != null) {
            for (Entry<PkgItem, File> entry : mResultPaths.entrySet()) {
                if (entry.getKey().getMetaPackage().getPackageType() == PackageType.platform_tools) {
                    installPath = entry.getValue();
                    break;
                }
            }
        }

        return Pair.of(mResultCode, installPath);
    }

    /**
     * Displays the update dialog and triggers installation of a new SDK. This works by
     * requesting a remote platform package with the specified API levels as well as
     * the first tools or platform-tools packages available.
     * <p/>
     * Callers must not try to reuse this dialog after this call.
     *
     * @param apiLevels A set of platform API levels to match.
     *  The special value {@link #USE_MAX_REMOTE_API_LEVEL} means to use
     *  the highest API level available in the repository.
     * @return A boolean indicating whether the installation was successful (meaning the packages
     *   were either already present, or got installed or updated properly).
     */
    public boolean installNewSdk(Set<Integer> apiLevels) {
        mPackageFilter = createNewSdkFilter(apiLevels);
        open();
        return mResultCode.booleanValue();
    }

    @Override
    protected void createContents() {
        Shell shell = getShell();
        shell.setMinimumSize(new Point(450, 100));
        shell.setSize(450, 100);
        GridLayoutBuilder.create(shell).columns(1);
        Composite composite1 = new Composite(shell, SWT.NONE);
        composite1.setLayout(new GridLayout(1, false));
        GridDataBuilder.create(composite1).fill().grab();

        mProgressBar = new ProgressBar(composite1, SWT.NONE);
        GridDataBuilder.create(mProgressBar).hFill().hGrab();

        mStatusText = new Label(composite1, SWT.NONE);
        mStatusText.setText("Status Placeholder");  //$NON-NLS-1$ placeholder
        GridDataBuilder.create(mStatusText).hFill().hGrab();
        createLogWindow();
    }

    /**
     * Creates the log window.
     * <p/>
     * If this is invoked from an IDE, we also define a secondary logger so that all
     * messages flow to the IDE log. This may or may not be what we want in the end
     * (e.g. a middle ground would be to repeat error, and ignore normal/verbose)
     */
    private void createLogWindow() {
        mLogWindow = new LogWindow(getShell(), mSdkContext.getSdkLog());
        mLogWindow.open();
    }


    @Override
    protected void postCreate() {
        // This class delegates all logging to the mLogWindow window
        // and filters errors to make sure the window is visible when
        // an error is logged.
        SdkProgressFactory.ISdkLogWindow logAdapter = new SdkProgressFactory.ISdkLogWindow() {
            @Override
            public void setDescription(String description) {
                mLogWindow.setDescription(description);
            }

            @Override
            public void log(String log) {
                mLogWindow.log(log);
            }

            @Override
            public void logVerbose(String log) {
                mLogWindow.logVerbose(log);
            }

            @Override
            public void logError(String log) {
                mLogWindow.logError(log);
            }
            @Override
            public void show()
            {
                // Run the window visibility check/toggle on the UI thread.
                // Note: at least on Windows, it seems ok to check for the window visibility
                // on a sub-thread but that doesn't seem cross-platform safe. We shouldn't
                // have a lot of error logging, so this should be acceptable. If not, we could
                // cache the visibility state.
                if (getShell() != null && !getShell().isDisposed()) {
                	getShell().getDisplay().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (!mLogWindow.isVisible()) {
                            	mLogWindow.setVisible(true);
                            }
                        }
                    });
                }
            }
        };
        SdkProgressFactory factory = new SdkProgressFactory(mStatusText, mProgressBar, null, logAdapter);
        initializeSettings();
        if (mSdkContext.hasError())
        {
        	ILogger logger = (ILogger)factory;
        	Iterator<String> iterator = mSdkContext.getLogMessages().iterator();
        	while(iterator.hasNext())
        		logger.error(null, iterator.next());
            close();
            return;
        }
        mSdkContext.setSdkLogger(factory);
        mSdkContext.setSdkProgressIndicator(factory);
    }

    @Override
    protected void eventLoop() {
 /*       		
            public boolean acceptPackage(PkgItem pkg) {
                // Is this the package we want to install?
                return mPackageFilter.accept(pkg);
            }

            public void setResult(boolean success, Map<PkgItem, File> installPaths) {
                // Capture the result from the installation.
                mResultCode = Boolean.valueOf(success);
                mResultPaths = installPaths;
            }

             public void taskCompleted() {
                // We can close that window now.
                close();
            }
*/
        super.eventLoop();
    }

    // -- Start of internal part ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

    // --- Public API -----------


    // --- Internals & UI Callbacks -----------

    /**
     * Initializes settings.
     * This must be called after addExtraPages(), which created a settings page.
     * Iterate through all the pages to find the first (and supposedly unique) setting page,
     * and use it to load and apply these settings.
     */
    private boolean initializeSettings() {
    	Settings settings = new Settings();
        if (settings.initialize(mSdkContext.getSdkLog()))
        {
        	mSdkContext.setSettings(settings);
        	return true;
        }
        return false;
    }


    // ----

    private static abstract class PackageFilter {
        /** Returns the installer flags for the corresponding mode. */
        abstract int installFlags();

        /** Visit a new package definition, in case we need to adjust the filter dynamically. */
        abstract void visit(PkgItem pkg);

        /** Checks whether this is the package we've been looking for. */
        abstract boolean accept(PkgItem pkg);
    }

    public static PackageFilter createExtraFilter(
            final String vendor,
            final String path) {
        return new PackageFilter() {
            String mVendor = vendor;
            String mPath = path;

            @Override
            boolean accept(PkgItem pkg) {
                if (pkg.getMetaPackage().getPackageType() == PackageType.extras) {
                    TypeDetails details = pkg.getMainPackage().getTypeDetails();
                    if (details instanceof ExtraDetailsType) {
                    	ExtraDetailsType extraDetailsType = (ExtraDetailsType)details;
	                    if (extraDetailsType.getVendor().getId().equals(mVendor)) {
	                        // Check actual extra <path> field
	                        if (pkg.getMainPackage().getPath().equals(mPath)) {
	                            return true;
	                        }
	                    }
                    }
               }
                return false;
            }

            @Override
            void visit(PkgItem pkg) {
                // nop
            }

            @Override
            int installFlags() {
                return SdkCallAgent.TOOLS_MSG_UPDATED_FROM_ADT;
            }
        };
    }

    private PackageFilter createPlatformToolsFilter() {
        return new PackageFilter() {
            @Override
            boolean accept(PkgItem pkg) {
                return pkg.getMetaPackage().getPackageType() == PackageType.platform_tools;
            }

            @Override
            void visit(PkgItem pkg) {
                // nop
            }

            @Override
            int installFlags() {
                return SdkCallAgent.TOOLS_MSG_UPDATED_FROM_ADT;
            }
        };
    }

    public static PackageFilter createPlatformFilter(final int apiLevel) {
        return new PackageFilter() {
            int mApiLevel = apiLevel;
            boolean mFindMaxApi = apiLevel == USE_MAX_REMOTE_API_LEVEL;

            @Override
            boolean accept(PkgItem pkg) {
                if (pkg.getMetaPackage().getPackageType() == PackageType.platforms) {
                	RepoPackage remotePackage = pkg.getMainPackage();
                    TypeDetails details = remotePackage.getTypeDetails();
                    if (details instanceof PlatformDetailsType) {
                    	PlatformDetailsType platformDetailsType = (PlatformDetailsType)details;
	                    AndroidVersion androidVersion = platformDetailsType.getAndroidVersion();
	                    return !androidVersion.isPreview() && androidVersion.getApiLevel() == mApiLevel;
                    }
                }
                return false;
            }

            @Override
            void visit(PkgItem pkg) {
                // Try to find the max API in all remote packages
                if (mFindMaxApi && (pkg.getMetaPackage().getPackageType() == PackageType.platforms)) {
                	RepoPackage remotePackage = pkg.getMainPackage();
                    TypeDetails details = remotePackage.getTypeDetails();
                    if ((details instanceof PlatformDetailsType) && (remotePackage instanceof RemotePackage)) {
                    	PlatformDetailsType platformDetailsType = (PlatformDetailsType)details;
	                    AndroidVersion androidVersion = platformDetailsType.getAndroidVersion();
	                    if (!androidVersion.isPreview()) {
	                        int api = androidVersion.getApiLevel();
	                        if (api > mApiLevel) {
	                            mApiLevel = api;
	                        }
	                    }
                    }
                }
            }

            @Override
            int installFlags() {
                return SdkCallAgent.TOOLS_MSG_UPDATED_FROM_ADT;
            }
        };
    }

    public static PackageFilter createNewSdkFilter(final Set<Integer> apiLevels) {
        return new PackageFilter() {
            int mMaxApiLevel;
            boolean mFindMaxApi = apiLevels.contains(USE_MAX_REMOTE_API_LEVEL);
            boolean mNeedTools = true;
            boolean mNeedPlatformTools = true;

            @Override
            boolean accept(PkgItem pkg) {
            	RepoPackage repoPackage = pkg.getMainPackage();
            	if (repoPackage instanceof RemotePackage) {
	                TypeDetails details = repoPackage.getTypeDetails();
	                if ((details instanceof PlatformDetailsType)) {
	                	PlatformDetailsType platformDetailsType = (PlatformDetailsType)details;
	                    AndroidVersion androidVersion = platformDetailsType.getAndroidVersion();
	                    if (!androidVersion.isPreview()) {
	                        int level = androidVersion.getApiLevel();
	                        if ((mFindMaxApi && level == mMaxApiLevel) ||
	                                (level > 0 && apiLevels.contains(level))) {
	                            return true;
	                        }
	                    }
                    } else if (mNeedTools && (pkg.getMetaPackage().getPackageType() == PackageType.tools)) {
                        // We want a tool package. There should be only one,
                        // but in case of error just take the first one.
                        mNeedTools = false;
                        return true;
                    } else if (mNeedPlatformTools && (pkg.getMetaPackage().getPackageType() == PackageType.platform_tools)) {
                        // We want a platform-tool package. There should be only one,
                        // but in case of error just take the first one.
                        mNeedPlatformTools = false;
                        return true;
                    }
                }
                return false;
            }

            @Override
            void visit(PkgItem pkg) {
                // Try to find the max API in all remote packages
                if (mFindMaxApi && (pkg.getMetaPackage().getPackageType() == PackageType.platforms)) {
                	RepoPackage remotePackage = pkg.getMainPackage();
                    TypeDetails details = remotePackage.getTypeDetails();
                    if ((details instanceof PlatformDetailsType) && (remotePackage instanceof RemotePackage)) {
                    	PlatformDetailsType platformDetailsType = (PlatformDetailsType)details;
	                    AndroidVersion androidVersion = platformDetailsType.getAndroidVersion();
	                    if (!androidVersion.isPreview()) {
	                        int api = androidVersion.getApiLevel();
	                        if (api > mMaxApiLevel) {
	                            mMaxApiLevel = api;
	                        }
	                    }
                    }
                }
            }

            @Override
            int installFlags() {
                return SdkCallAgent.NO_TOOLS_MSG;
            }
        };
    }

    // End of hiding from SWT Designer
    //$hide<<$

    // -----

}
