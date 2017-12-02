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

package com.android.sdkuilib.internal.repository.ui;


import com.android.SdkConstants;
import com.android.sdklib.internal.avd.AvdInfo;

import com.android.sdkuilib.repository.ISdkChangeListener;
import com.android.sdkuilib.internal.repository.AboutDialog;
import com.android.sdkuilib.internal.repository.MenuBarWrapper;
import com.android.sdkuilib.internal.repository.SettingsDialog;
import com.android.sdkuilib.internal.repository.avd.SdkTargets;
import com.android.sdkuilib.internal.repository.ui.DeviceManagerPage.IAvdCreatedListener;
import com.android.sdkuilib.repository.SdkUpdaterWindow;

import com.android.sdkuilib.repository.AvdManagerWindow.AvdInvocationContext;
import com.android.sdkuilib.ui.GridDataBuilder;
import com.android.sdkuilib.ui.GridLayoutBuilder;
import com.android.utils.ILogger;

import org.eclipse.andmore.base.resources.ImageFactory;
import org.eclipse.andmore.sdktool.SdkContext;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

/**
 * This is an intermediate version of the {@link AvdManagerPage}
 * wrapped in its own standalone window for use from the SDK Manager 2.
 */
public class AvdManagerWindowImpl1 {

    private static final String APP_NAME = "Android Virtual Device (AVD) Manager";
    private static final String APP_NAME_MAC_MENU = "AVD Manager";
    private static final String SIZE_POS_PREFIX = "avdman1"; //$NON-NLS-1$

    private final Shell mParentShell;
    private final AvdInvocationContext mContext;
    private final SdkContext mSdkContext;

    // --- UI members ---

    protected Shell mShell;
    private AvdManagerPage mAvdPage;
    private TabFolder mTabFolder;

    /**
     * Creates a new window. Caller must call open(), which will block.
     *
     * @param parentShell Parent shell.
     * @param sdkLog Logger. Cannot be null.
     * @param sdkContext SDK handler and repo manager
     * @param context The {@link AvdInvocationContext} to change the behavior depending on who's
     *  opening the SDK Manager.
     */
    public AvdManagerWindowImpl1(
            Shell parentShell,
            ILogger sdkLog,
            SdkContext sdkContext,
            AvdInvocationContext context) {
        mParentShell = parentShell;
        mSdkContext = sdkContext;
        mContext = context;
    }

    /**
     * Creates a new window. Caller must call open(), which will block.
     * <p/>
     * This is to be used when the window is opened from {@link SdkUpdaterWindowImpl2}
     * to share the same {@link SwtUpdaterData} structure.
     *
     * @param parentShell Parent shell.
     * @param sdkContext SDK handler and repo manager
     * @param context The {@link AvdInvocationContext} to change the behavior depending on who's
     *  opening the SDK Manager.
     */
    public AvdManagerWindowImpl1(
            Shell parentShell,
            SdkContext sdkContext,
            AvdInvocationContext context) {
        mParentShell = parentShell;
        mContext = context;
        mSdkContext = sdkContext;
    }

    /**
     * Opens the window.
     * @wbp.parser.entryPoint
     */
    public void open() {
        if (mParentShell == null) {
            Display.setAppName(APP_NAME); //$hide$ (hide from SWT designer)
        }

        createShell();
        preCreateContent();
        createContents();
        createMenuBar();
        mShell.open();
        mShell.layout();

        boolean ok = postCreateContent();

        if (ok && mContext == AvdInvocationContext.STANDALONE) {
            Display display = Display.getDefault();
            while (!mShell.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
            dispose();  //$hide$
        }
    }

    private void createShell() {
        // The AVD Manager must use a shell trim when standalone
        // or a dialog trim when invoked from somewhere else.
        int style = SWT.SHELL_TRIM;
        if (mContext != AvdInvocationContext.STANDALONE) {
            style |= SWT.APPLICATION_MODAL;
        }

        mShell = new Shell(mParentShell, style);
        mShell.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                ShellSizeAndPos.saveSizeAndPos(mShell, SIZE_POS_PREFIX);    //$hide$
                mAvdPage.dispose();                                         //$hide$
            }
        });

        GridLayout glShell = new GridLayout(2, false);
        glShell.verticalSpacing = 0;
        glShell.horizontalSpacing = 0;
        glShell.marginWidth = 0;
        glShell.marginHeight = 0;
        mShell.setLayout(glShell);

        mShell.setMinimumSize(new Point(600, 300));
        mShell.setSize(700, 500);
        mShell.setText(APP_NAME);

        ShellSizeAndPos.loadSizeAndPos(mShell, SIZE_POS_PREFIX);
    }

    private void createContents() {
        mTabFolder = new TabFolder(mShell, SWT.NONE);
        GridDataBuilder.create(mTabFolder).fill().grab().hSpan(2);

        // avd tab
        TabItem avdTabItem = new TabItem(mTabFolder, SWT.NONE);
        avdTabItem.setText("Android Virtual Devices");
        avdTabItem.setToolTipText(avdTabItem.getText());
        createAvdTab(mTabFolder, avdTabItem);

        // device tab
        TabItem devTabItem = new TabItem(mTabFolder, SWT.NONE);
        devTabItem.setText("Device Definitions");
        devTabItem.setToolTipText(devTabItem.getText());
        createDeviceTab(mTabFolder, devTabItem);
    }

    private void createAvdTab(TabFolder tabFolder, TabItem avdTabItem) {
        Composite root = new Composite(tabFolder, SWT.NONE);
        avdTabItem.setControl(root);
        GridLayoutBuilder.create(root).columns(1);

        mAvdPage = new AvdManagerPage(root, SWT.NONE, mSdkContext);
        GridDataBuilder.create(mAvdPage).fill().grab();
    }

    private void createDeviceTab(TabFolder tabFolder, TabItem devTabItem) {
        Composite root = new Composite(tabFolder, SWT.NONE);
        devTabItem.setControl(root);
        GridLayoutBuilder.create(root).columns(1);

        DeviceManagerPage devicePage =
            new DeviceManagerPage(root, SWT.NONE, mSdkContext, new SdkTargets(mSdkContext));
        GridDataBuilder.create(devicePage).fill().grab();

        devicePage.setAvdCreatedListener(new IAvdCreatedListener() {
            @Override
            public void onAvdCreated(AvdInfo avdInfo) {
                if (avdInfo != null) {
                    mTabFolder.setSelection(0);      // display mAvdPage
                    mAvdPage.selectAvd(avdInfo, true /*reloadAvdList*/);
                }
            }
        });
    }

    // MenuBarWrapper works using side effects
    private void createMenuBar() {
        Menu menuBar = new Menu(mShell, SWT.BAR);
        mShell.setMenuBar(menuBar);

        // Only create the tools menu when running as standalone.
        // We don't need the tools menu when invoked from the IDE, or the SDK Manager
        // or from the AVD Chooser dialog. The only point of the tools menu is to
        // get the about box, and invoke Tools > SDK Manager, which we don't
        // need to do in these cases.
        if (mContext == AvdInvocationContext.STANDALONE) {

            MenuItem menuBarTools = new MenuItem(menuBar, SWT.CASCADE);
            menuBarTools.setText("Tools");

            Menu menuTools = new Menu(menuBarTools);
            menuBarTools.setMenu(menuTools);

            MenuItem manageSdk = new MenuItem(menuTools, SWT.NONE);
            manageSdk.setText("Manage SDK...");
            manageSdk.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    onSdkManager();
                }
            });

            try {
                new MenuBarWrapper(APP_NAME_MAC_MENU, menuTools) {
                    @Override
                    public void onPreferencesMenuSelected() {
                        SettingsDialog sd = new SettingsDialog(mShell, mSdkContext);
                        sd.open();
                    }

                    @Override
                    public void onAboutMenuSelected() {
                        AboutDialog ad = new AboutDialog(mShell, mSdkContext);
                        ad.open();
                    }

                    @Override
                    public void printError(String format, Object... args) {
                        if (mSdkContext != null) {
                            mSdkContext.getSdkLog().error(null, format, args);
                        }
                    }
                };
            } catch (Throwable e) {
                mSdkContext.getSdkLog().error(e, "Failed to setup menu bar");
                e.printStackTrace();
            }
        }
    }


    // -- Start of internal part ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

    // --- Public API -----------

    /**
     * Adds a new listener to be notified when a change is made to the content of the SDK.
     */
    public void addListener(ISdkChangeListener listener) {
        mSdkContext.getSdkHelper().addListeners(listener);
    }

    /**
     * Removes a new listener to be notified anymore when a change is made to the content of
     * the SDK.
     */
    public void removeListener(ISdkChangeListener listener) {
        mSdkContext.getSdkHelper().removeListener(listener);
    }

    // --- Internals & UI Callbacks -----------

    /**
     * Called before the UI is created.
     */
    private void preCreateContent() {
        mSdkContext.getSdkHelper().setWindowShell(mShell);
        // Note: we can't create the TaskFactory yet because we need the UI
        // to be created first, so this is done in postCreateContent().
    }

    /**
     * Once the UI has been created, initializes the content.
     * This creates the pages, selects the first one, setup sources and scan for local folders.
     *
     * Returns true if we should show the window.
     */
    private boolean postCreateContent() {
        setWindowImage(mShell);

        if (!initializeSettings()) {
            return false;
        }
        // TODO - Consider how to signal SDK loaded
        //mSdkContext.getSdkHelper().broadcastOnSdkLoaded(mSdkContext.getSdkLog());
        return true;
    }

    /**
     * Creates the icon of the window shell.
     *
     * @param shell The shell on which to put the icon
     */
    private void setWindowImage(Shell shell) {
        String imageName = "android_icon_16.png"; //$NON-NLS-1$
        if (SdkConstants.currentPlatform() == SdkConstants.PLATFORM_DARWIN) {
            imageName = "android_icon_128.png";
        }

        if (mSdkContext != null) {
            ImageFactory imgFactory = mSdkContext.getSdkHelper().getImageFactory();
            if (imgFactory != null) {
                shell.setImage(imgFactory.getImageByName(imageName));
            }
        }
    }

    /**
     * Called by the main loop when the window has been disposed.
     */
    private void dispose() {
        //mSdkContext.getSources().saveUserAddons(mSdkContext.getSdkLog());
    }

    /**
     * Initializes settings.
     * This must be called after addExtraPages(), which created a settings page.
     * Iterate through all the pages to find the first (and supposedly unique) setting page,
     * and use it to load and apply these settings.
     */
    private boolean initializeSettings() {
        return mSdkContext.getSettings().initialize(mSdkContext.getSdkLog());
    }

    private void onSdkManager() {
        try {
            SdkUpdaterWindowImpl2 win = new SdkUpdaterWindowImpl2(
                    mShell,
                    mSdkContext,
                    SdkUpdaterWindow.SdkInvocationContext.AVD_MANAGER);

            win.open();
        } catch (Exception e) {
            mSdkContext.getSdkLog().error(e, "SDK Manager window error");
        }
    }
}
