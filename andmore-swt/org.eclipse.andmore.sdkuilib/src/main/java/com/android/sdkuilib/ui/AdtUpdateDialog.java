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

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.andmore.sdktool.SdkCallAgent;
import org.eclipse.andmore.sdktool.SdkContext;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

import com.android.repository.api.ProgressRunner;
import com.android.repository.api.RemotePackage;
import com.android.repository.api.RepoManager.RepoLoadedCallback;
import com.android.repository.api.RepoPackage;
import com.android.repository.impl.meta.RepositoryPackages;
import com.android.repository.impl.meta.TypeDetails;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.repository.meta.DetailsTypes.ExtraDetailsType;
import com.android.sdklib.repository.meta.DetailsTypes.PlatformDetailsType;
import com.android.sdkuilib.internal.repository.LoadPackagesRequest;
import com.android.sdkuilib.internal.repository.PackageInstallListener;
import com.android.sdkuilib.internal.repository.PackageManager;
import com.android.sdkuilib.internal.repository.Settings;
import com.android.sdkuilib.internal.repository.content.PackageAnalyser;
import com.android.sdkuilib.internal.repository.content.PackageInstaller;
import com.android.sdkuilib.internal.repository.content.PackageType;
import com.android.sdkuilib.internal.repository.content.PackageVisitor;
import com.android.sdkuilib.internal.repository.content.PkgItem;
import com.android.sdkuilib.internal.repository.ui.SdkProgressFactory;
import com.android.sdkuilib.internal.repository.ui.SdkProgressFactory.ISdkLogWindow;
import com.android.utils.ILogger;
import com.android.utils.Pair;

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
public class AdtUpdateDialog extends SwtBaseDialog implements ISdkLogWindow {

    private enum TextStyle {
        DEFAULT,
        TITLE,
        ERROR
    }
    
    public static final int USE_MAX_REMOTE_API_LEVEL = 0;

    private static final String APP_NAME = "Android SDK Manager";
    private final SdkContext mSdkContext;
    private final PackageAnalyser mPackageAnalyser;
    PackageInstaller packageInstaller;

    private PackageVisitor mPackageFilter;

    private ProgressBar mProgressBar;
    private Label mStatusText;
    private StyledText mStyledText;
    private Button mCloseButton;

    private PackageInstallListener onIdle = new PackageInstallListener(){

		@Override
		public void onPackagesInstalled(int count) {
			enableClose();
		}
    };

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
        mPackageAnalyser = new PackageAnalyser(mSdkContext);

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
        boolean success = packageInstaller != null;
        if (success) {
        	success = packageInstaller.getNumInstalled() > 0;
        }
        File installPath = null;
        if (success) {
            for (PkgItem entry : packageInstaller.getRequiredPackageItems()) {
                if (entry.getMetaPackage().getPackageType() == PackageType.platforms) {
                    installPath = new File(mSdkContext.getLocalPath(), entry.getMainPackage().getPath().replaceAll(";", "/"));
                    break;
                }
            }
        }
        return Pair.of(success, installPath);
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
        boolean success = packageInstaller != null;
        if (success) {
        	success = packageInstaller.getNumInstalled() > 0;
        }
        File installPath = null;
        if (success) {
            for (PkgItem entry : packageInstaller.getRequiredPackageItems()) {
                if (entry.getMetaPackage().getPackageType() == PackageType.extras) {
                    installPath = new File(mSdkContext.getLocalPath(), entry.getMainPackage().getPath().replaceAll(";", "/"));
                    break;
                }
            }
        }
        return Pair.of(success, installPath);
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
        boolean success = packageInstaller != null;
        if (success) {
        	success = packageInstaller.getNumInstalled() > 0;
        }
        File installPath = null;
        if (success) {
            for (PkgItem entry : packageInstaller.getRequiredPackageItems()) {
                if (entry.getMetaPackage().getPackageType() == PackageType.platform_tools) {
                    installPath = new File(mSdkContext.getLocalPath(), entry.getMainPackage().getPath().replaceAll(";", "/"));
                    break;
                }
            }
        }

        return Pair.of(success, installPath);
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
        boolean success = packageInstaller != null;
        if (success) {
        	success = packageInstaller.getNumInstalled() > 0;
        }
        return success;
    }

    @Override
    protected void createContents() {
        Shell shell = getShell();
        GridLayout glShell = new GridLayout(2, false);
        glShell.verticalSpacing = 0;
        glShell.horizontalSpacing = 0;
        glShell.marginWidth = 0;
        glShell.marginHeight = 0;
        shell.setLayout(glShell);

        shell.setMinimumSize(new Point(600, 300));
        shell.setSize(700, 500);
        GridLayoutBuilder.create(shell).columns(1);
        Composite composite1 = new Composite(shell, SWT.NONE);
        composite1.setLayout(new GridLayout(1, false));
        GridDataBuilder.create(composite1).fill().grab();

    	Composite composite = new Composite(composite1, SWT.NONE);
        GridLayoutBuilder.create(composite).columns(2);
        GridDataBuilder.create(composite).fill().grab();

        mStyledText = new StyledText(composite,
                SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
        GridDataBuilder.create(mStyledText).hSpan(2).fill().grab();

        mStatusText = new Label(composite, SWT.NONE);
        GridDataBuilder.create(mStatusText).hFill().hGrab();

        mCloseButton = new Button(composite, SWT.NONE);
        mCloseButton.setText("Stop");
        mCloseButton.setToolTipText("Aborts the installation");
        mCloseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mSdkContext.getProgressIndicator().cancel();
                close();
            }
        });
        mProgressBar = new ProgressBar(composite1, SWT.NONE);
        GridDataBuilder.create(mProgressBar).hFill().hGrab();
    }

    @Override
    protected void postCreate() {
        SdkProgressFactory factory = new SdkProgressFactory(mStatusText, mProgressBar, mCloseButton, this);
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
		Runnable onError = new Runnable(){
			@Override
			public void run() {
				enableClose();
				factory.getProgressControl().setDescription("Package operation did not complete due to error or cancellation");
			}};
    	RepoLoadedCallback onSuccess = new RepoLoadedCallback(){
			@Override
			public void doRun(RepositoryPackages packages) {
                if (!getShell().isDisposed()) 
                {
                	mSdkContext.getPackageManager().setPackages(packages);
                	mPackageAnalyser.loadPackages();
            		packageInstaller = new PackageInstaller(mPackageAnalyser, mPackageFilter, factory);
            		if (packageInstaller.getRequiredPackageItems().isEmpty()) {
            			// No packages were selected
            			onError.run();
            			return;
            		}
                 	factory.getProgressControl().setDescription("Done loading packages.");
                	packageInstaller.installPackages(getShell(), mSdkContext, onIdle);
                }
			}};
        loadPackages(factory, onSuccess, onError);
    }

    /**
     * Sets the description in the current task dialog.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void setDescription(final String description) {
        syncExec(mStatusText, new Runnable() {
            @Override
            public void run() {
                mStatusText.setText(description);
                appendLine(TextStyle.TITLE, description);
            }
        });
    }

    /**
     * Logs a "normal" information line.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void log(final String log) {
        syncExec(mStatusText, new Runnable() {
            @Override
            public void run() {
                appendLine(TextStyle.DEFAULT, log);
            }
        });
    }

    /**
     * Logs an "error" information line.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void logError(final String log) {
        syncExec(mStatusText, new Runnable() {
            @Override
            public void run() {
                appendLine(TextStyle.ERROR, log);
            }
        });
    }

    /**
     * Logs a "verbose" information line, that is extra details which are typically
     * not that useful for the end-user and might be hidden until explicitly shown.
     * This method can be invoked from a non-UI thread.
     */
    @Override
    public void logVerbose(final String log) {
        syncExec(mStatusText, new Runnable() {
            @Override
            public void run() {
                appendLine(TextStyle.DEFAULT, "  " + log);      //$NON-NLS-1$
            }
        });
    }

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

    public static PackageVisitor createExtraFilter(
            final String vendor,
            final String path) {
        return new PackageVisitor() {
            String mVendor = vendor;
            String mPath = path;

            @Override
            public boolean accept(PkgItem pkg) {
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
        };
    }

    private PackageVisitor createPlatformToolsFilter() {
        return new PackageVisitor() {
            @Override
            public boolean accept(PkgItem pkg) {
                return pkg.getMetaPackage().getPackageType() == PackageType.platform_tools;
            }
         };
    }

    public static PackageVisitor createPlatformFilter(final int apiLevel) {
        return new PackageVisitor() {
            int mApiLevel = apiLevel;
            boolean mFindMaxApi = apiLevel == USE_MAX_REMOTE_API_LEVEL;

            @Override
            public boolean accept(PkgItem pkg) {
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
            public boolean visit(PkgItem pkg) {
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
                return true;
            }
        };
    }

    public static PackageVisitor createNewSdkFilter(final Set<Integer> apiLevels) {
        return new PackageVisitor() {
            int mMaxApiLevel;
            boolean mFindMaxApi = apiLevels.contains(USE_MAX_REMOTE_API_LEVEL);
            boolean mNeedTools = true;
            boolean mNeedPlatformTools = true;

            @Override
            public boolean accept(PkgItem pkg) {
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
            public boolean visit(PkgItem pkg) {
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
                return true;
            }
        };
    }

    private void loadPackages(ProgressRunner progressRunner, RepoLoadedCallback onSuccess, Runnable onError) {
        final PackageManager packageManager = mSdkContext.getPackageManager();
    	LoadPackagesRequest loadPackagesRequest = new LoadPackagesRequest(progressRunner);
		//loadPackagesRequest.setOnLocalComplete(Collections.singletonList(onLocalComplete));
    	loadPackagesRequest.setOnSuccess(Collections.singletonList(onSuccess));
    	loadPackagesRequest.setOnError(Collections.singletonList(onError));
    	packageManager.requestRepositoryPackages(loadPackagesRequest);
    }

    
    private void syncExec(final Widget widget, final Runnable runnable) {
        if (widget != null && !widget.isDisposed()) {
            widget.getDisplay().syncExec(runnable);
        }
    }

    private void appendLine(TextStyle style, String text) {
        if (!text.endsWith("\n")) {                                 //$NON-NLS-1$
            text += '\n';
        }

        int start = mStyledText.getCharCount();

        if (style == TextStyle.DEFAULT) {
            mStyledText.append(text);

        } else {
            mStyledText.append(text);

            StyleRange sr = new StyleRange();
            sr.start = start;
            sr.length = text.length();
            sr.fontStyle = SWT.BOLD;
            if (style == TextStyle.ERROR) {
                sr.foreground = mStyledText.getDisplay().getSystemColor(SWT.COLOR_DARK_RED);
            }
            sr.underline = false;
            mStyledText.setStyleRange(sr);
        }

        // Scroll caret if it was already at the end before we added new text.
        // Ideally we would scroll if the scrollbar is at the bottom but we don't
        // have direct access to the scrollbar without overriding the SWT impl.
        if (mStyledText.getCaretOffset() >= start) {
            mStyledText.setSelection(mStyledText.getCharCount());
        }
    }

	@Override
	public void show() {
	}

	private void enableClose() {
		syncExec(mCloseButton, new Runnable() {
            @Override
            public void run() {
            	mCloseButton.setEnabled(true);
            	mCloseButton.setText("OK");
            }
        });
	}
}
