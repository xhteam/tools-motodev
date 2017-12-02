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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.andmore.base.resources.ImageFactory;
import org.eclipse.andmore.sdktool.SdkContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import com.android.annotations.NonNull;
import com.android.repository.api.LocalPackage;
import com.android.repository.api.PackageOperation;
import com.android.repository.api.ProgressIndicator;
import com.android.repository.api.ProgressRunner;
import com.android.repository.api.RemotePackage;
import com.android.repository.api.RepoManager.RepoLoadedCallback;
import com.android.repository.api.Uninstaller;
import com.android.repository.impl.meta.Archive;
import com.android.repository.impl.meta.RepositoryPackages;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.repository.installer.SdkInstallerUtil;
import com.android.sdklib.repository.meta.DetailsTypes.PlatformDetailsType;
import com.android.sdkuilib.internal.repository.ITask;
import com.android.sdkuilib.internal.repository.ITaskFactory;
import com.android.sdkuilib.internal.repository.ITaskMonitor;
import com.android.sdkuilib.internal.repository.LoadPackagesRequest;
import com.android.sdkuilib.internal.repository.PackageInstallListener;
import com.android.sdkuilib.internal.repository.PackageManager;
import com.android.sdkuilib.internal.repository.content.CategoryKeyType;
import com.android.sdkuilib.internal.repository.content.PackageAnalyser;
import com.android.sdkuilib.internal.repository.content.PackageAnalyser.PkgState;
import com.android.sdkuilib.internal.repository.content.PackageContentProvider;
import com.android.sdkuilib.internal.repository.content.PackageFilter;
import com.android.sdkuilib.internal.repository.content.PackageInstaller;
import com.android.sdkuilib.internal.repository.content.PackageType;
import com.android.sdkuilib.internal.repository.content.PkgCategory;
import com.android.sdkuilib.internal.repository.content.PkgCellAgent;
import com.android.sdkuilib.internal.repository.content.PkgCellLabelProvider;
import com.android.sdkuilib.internal.repository.content.PkgItem;
import com.android.sdkuilib.internal.repository.content.PkgTreeColumnViewerLabelProvider;
import com.android.sdkuilib.internal.tasks.ILogUiProvider;
import com.android.sdkuilib.internal.widgets.PackageTypesSelector;
import com.android.sdkuilib.repository.SdkUpdaterWindow.SdkInvocationContext;
import com.android.sdkuilib.ui.GridDataBuilder;
import com.android.sdkuilib.ui.GridLayoutBuilder;

/**
 * Page that displays both locally installed packages as well as all known
 * remote available packages. This gives an overview of what is installed
 * vs what is available and allows the user to update or install packages.
 */
public final class PackagesPage extends Composite {

    enum MenuAction {
        RELOAD  (SWT.NONE,  "Reload"),
        //TOGGLE_SHOW_INSTALLED_PKG   (SWT.CHECK, "Show Installed Packages"),
        TOGGLE_SHOW_OBSOLETE_PKG  (SWT.CHECK, "Show Obsolete Packages"),
        TOGGLE_SHOW_NEW_PKG  (SWT.CHECK, "Show New Packages"),
        FILTER_PACKAGES  (SWT.NONE,  "Filter Packages");

        private final int mMenuStyle;
        private final String mMenuTitle;

        MenuAction(int menuStyle, String menuTitle) {
            mMenuStyle = menuStyle;
            mMenuTitle = menuTitle;
        }

        public int getMenuStyle() {
            return mMenuStyle;
        }

        public String getMenuTitle() {
            return mMenuTitle;
        }
    };

    // Column ids
	public static final int NAME = 1;
	public static final int API = 2;
	public static final int REVISION = 3;
	public static final int STATUS = 4;

	private final Map<MenuAction, MenuItem> mMenuActions = new HashMap<MenuAction, MenuItem>();

    private final SdkContext mSdkContext;
    //private final SdkInvocationContext mContext;
    private final PackageAnalyser mPackageAnalyser;

    private boolean mDisplayArchives = false;
    private boolean mOperationPending;
    private ProgressRunner mProgressRunner;
    private Composite mGroupPackages;
    private Text mTextSdkOsPath;
    private Button mCheckFilterObsolete;
    //private Button mCheckFilterInstalled;
    private Button mCheckFilterNew;
    private Button mCheckAll;
    private Composite mGroupOptions;
    private Composite mGroupSdk;
    private Button mButtonDelete;
    private Button mButtonInstall;
    private Button mButtonPkgTypes;
    private Button mButtonCancel;
    private Font mTreeFontItalic;
    private TreeColumn mTreeColumnName;
    private CheckboxTreeViewer mTreeViewer;
    private ILogUiProvider mSdkProgressControl;
    private ITaskFactory mTaskFactory;
    private PackageFilter mPackageFilter;
    private SdkProgressFactory factory;

    public PackagesPage(
            Composite parent,
            int swtStyle,
            SdkContext sdkContext,
            SdkInvocationContext context,
            Set<PackageType> packageTypeSet) 
    {
        super(parent, swtStyle);
        mSdkContext = sdkContext;
        mPackageFilter = new PackageFilter(packageTypeSet);
        mPackageAnalyser = new PackageAnalyser(sdkContext);
        //mContext = context;
        createContents(this);
        postCreate();
    }

    public void onReady(SdkProgressFactory factory) {
    	this.factory = factory;
    	mProgressRunner = factory;
    	mSdkProgressControl = factory.getProgressControl();
    	mTaskFactory = factory;
    	startLoadPackages();
    }

    public void onSdkReload() {
    	startLoadPackages();
    }

    private void createContents(Composite parent) 
    {
    	Color foreColor = parent.getForeground();
    	Color backColor = parent.getBackground();
    	Display display = parent.getDisplay();
    	Color hiForeColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
       	Color hiBackColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
    	GridLayoutBuilder.create(parent).noMargins().columns(2);

        mGroupSdk = new Composite(parent, SWT.NONE);
        GridDataBuilder.create(mGroupSdk).hFill().vCenter().hGrab().hSpan(2);
        GridLayoutBuilder.create(mGroupSdk).columns(2);

        Label label1 = new Label(mGroupSdk, SWT.NONE);
        label1.setText("SDK Path:");

        mTextSdkOsPath = new Text(mGroupSdk, SWT.NONE);
        GridDataBuilder.create(mTextSdkOsPath).hFill().vCenter().hGrab();
        mTextSdkOsPath.setEnabled(false);

        Group groupPackages = new Group(parent, SWT.SHADOW_NONE);
        mGroupPackages = groupPackages;
        GridDataBuilder.create(mGroupPackages).fill().grab().hSpan(2);
        groupPackages.setText("Packages");
        GridLayoutBuilder.create(groupPackages).columns(2);

        mTreeViewer = new CheckboxTreeViewer(groupPackages, SWT.BORDER);
        mTreeViewer.addFilter(new ViewerFilter() 
        {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                return filterViewerItem(element);
            }
        });

        mTreeViewer.addCheckStateListener(new ICheckStateListener() 
        {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                onTreeCheckStateChanged(event); //$hide$
            }
        });

        mTreeViewer.addDoubleClickListener(new IDoubleClickListener() 
        {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                onTreeDoubleClick(event); //$hide$
            }
        });

        Tree tree = mTreeViewer.getTree();
        tree.setLinesVisible(true);
        tree.setHeaderVisible(true);
        GridDataBuilder.create(tree).hSpan(2).fill().grab();

        // column name icon is set when loading depending on the current filter type
        // (e.g. API level or source)
        TreeViewerColumn columnName = new TreeViewerColumn(mTreeViewer, SWT.NONE);
        mTreeColumnName = columnName.getColumn();
        mTreeColumnName.setText("Name");
        mTreeColumnName.setWidth(400);

        TreeViewerColumn columnApi = new TreeViewerColumn(mTreeViewer, SWT.NONE);
        TreeColumn treeColumn2 = columnApi.getColumn();
        treeColumn2.setText("API");
        treeColumn2.setAlignment(SWT.LEFT);
        treeColumn2.setWidth(35);

        TreeViewerColumn columnRevision = new TreeViewerColumn(mTreeViewer, SWT.NONE);
        TreeColumn treeColumn3 = columnRevision.getColumn();
        treeColumn3.setText("Revision");
        treeColumn3.setToolTipText("Revision currently installed");
        treeColumn3.setAlignment(SWT.LEFT);
        treeColumn3.setWidth(80);


        TreeViewerColumn columnStatus = new TreeViewerColumn(mTreeViewer, SWT.NONE);
        TreeColumn treeColumn4 = columnStatus.getColumn();
        treeColumn4.setText("Status");
        treeColumn4.setAlignment(SWT.LEAD);
        treeColumn4.setWidth(205);

        mGroupOptions = new Group(groupPackages, SWT.SHADOW_OUT);
        GridDataBuilder.create(mGroupOptions).hFill().vCenter().hGrab();
        GridLayoutBuilder.create(mGroupOptions).columns(6).noMargins();
 
        // Options line 1, 6 columns

        Label label3 = new Label(mGroupOptions, SWT.NONE);
        label3.setText("Show:");
        GridDataBuilder.create(label3).vSpan(2).vTop();
        mCheckFilterNew = new Button(mGroupOptions, SWT.CHECK);
        GridDataBuilder.create(mCheckFilterNew).vTop();
        mCheckFilterNew.setText("New");
        mCheckFilterNew.setToolTipText("Show latest available new packages");
        mCheckFilterNew.addSelectionListener(new SelectionAdapter() 
        {
            @Override
            public void widgetSelected(SelectionEvent event) {
                // Only enable check all if "New" is checked
                mCheckAll.setEnabled(((Button)event.getSource()).getSelection());
                refreshViewerInput();
            }
        });
        mCheckFilterNew.setSelection(true);
        mCheckAll = new Button(mGroupOptions, SWT.CHECK);
        GridDataBuilder.create(mCheckAll).vSpan(2).vTop();
        mCheckAll.setText("All");
        mCheckAll.setToolTipText("Show all available new packages");
        mCheckAll.addSelectionListener(new SelectionAdapter() 
        {
            @Override
            public void widgetSelected(SelectionEvent e) {
                refreshViewerInput();
            }
        });
/*
        mCheckFilterInstalled = new Button(mGroupOptions, SWT.CHECK);
        mCheckFilterInstalled.setToolTipText("Show Installed");
        mCheckFilterInstalled.addSelectionListener(new SelectionAdapter() 
        {
            @Override
            public void widgetSelected(SelectionEvent e) {
                refreshViewerInput();
            }
        });
        mCheckFilterInstalled.setSelection(true);
        mCheckFilterInstalled.setText("Installed");
*/
        //new Label(mGroupOptions, SWT.NONE);

        Label label4 = new Label(mGroupOptions, SWT.NONE);
        label4.setText("Select:");
        GridDataBuilder.create(label4).vSpan(2).vTop();
        Link linkSelectUpdates = new Link(mGroupOptions, SWT.NONE);
        linkSelectUpdates.setText("<a>Select Updates</a>");
        linkSelectUpdates.setToolTipText("Selects all items that are updates.");
        //GridDataBuilder.create(linkSelectUpdates).hFill();
        linkSelectUpdates.addSelectionListener(new SelectionAdapter() 
        {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                onSelectPackages(true, false); // selectTop
            }
        });

        // placeholder between "select all" and "install"
        //Label placeholder = new Label(mGroupOptions, SWT.NONE);
        //GridDataBuilder.create(placeholder).hFill().hGrab();

        mButtonInstall = new Button(mGroupOptions, SWT.NONE);
        mButtonInstall.setText("");  //$NON-NLS-1$  placeholder, filled in updateButtonsState()
        mButtonInstall.setToolTipText("Install one or more packages");
        GridDataBuilder.create(mButtonInstall).vCenter().wHint(150).hFill().hGrab().hRight();
        mButtonInstall.addSelectionListener(new SelectionAdapter() 
        {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onButtonInstall();  //$hide$
            }
        });

        // Options line 2, 6 columns

        //Label placeholder2 = new Label(mGroupOptions, SWT.NONE);
        //GridDataBuilder.create(placeholder2).hFill().hGrab();

        mCheckFilterObsolete = new Button(mGroupOptions, SWT.CHECK);
        mCheckFilterObsolete.setText("Obsolete");
        mCheckFilterObsolete.setToolTipText("Also show obsolete packages");
        mCheckFilterObsolete.addSelectionListener(new SelectionAdapter() 
        {
            @Override
            public void widgetSelected(SelectionEvent event) {
            	if (((Button)event.getSource()).getSelection()) {
            		mCheckFilterObsolete.setForeground(hiForeColor);
            		mCheckFilterObsolete.setBackground(hiBackColor);
            	} else {
            		mCheckFilterObsolete.setForeground(foreColor);
            		mCheckFilterObsolete.setBackground(backColor);
            	}
                refreshViewerInput();
            }
        });
        mCheckFilterObsolete.setSelection(false);

        // placeholder before "deselect"
        //new Label(mGroupOptions, SWT.NONE);
        //new Label(mGroupOptions, SWT.NONE);

        Link linkDeselect = new Link(mGroupOptions, SWT.NONE);
        linkDeselect.setText("<a>Deselect All</a>");
        linkDeselect.setToolTipText("Deselects all the currently selected items");
        //GridDataBuilder.create(linkDeselect).hFill();
        linkDeselect.addSelectionListener(new SelectionAdapter() 
        {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                onDeselectAll();
            }
        });

        // placeholder between "deselect" and "delete"
        //placeholder = new Label(mGroupOptions, SWT.NONE);
        //GridDataBuilder.create(placeholder).hFill().hGrab();

        mButtonDelete = new Button(mGroupOptions, SWT.NONE);
        mButtonDelete.setText("");  //$NON-NLS-1$  placeholder, filled in updateButtonsState()
        mButtonDelete.setToolTipText("Delete one ore more installed packages");
        GridDataBuilder.create(mButtonDelete).vCenter().wHint(150).hFill().hGrab().hRight();
        mButtonDelete.addSelectionListener(new SelectionAdapter() 
        {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onButtonDelete();  //$hide$
            }
        });
        mGroupOptions.pack();
        Group controls = new Group(groupPackages, SWT.NONE);
        GridDataBuilder.create(controls).vCenter();
        GridLayoutBuilder.create(controls);
        mButtonPkgTypes = new Button(controls, SWT.NONE);
        mButtonPkgTypes.setText("Package Types...");  
        GridDataBuilder.create(mButtonPkgTypes).wHint(100);
        mButtonPkgTypes.addSelectionListener(new SelectionAdapter() 
        {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	selectPackages();
            }
        });
        mButtonCancel = new Button(controls, SWT.NONE);
        mButtonCancel.setText("Cancel");  
        GridDataBuilder.create(mButtonCancel).wHint(100);
        mButtonCancel.addSelectionListener(new SelectionAdapter() 
        {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mSdkContext.getProgressIndicator().cancel();  //$hide$
                getShell().close();
            }
        });
        controls.pack();
        FontData fontData = tree.getFont().getFontData()[0];
        fontData.setStyle(SWT.ITALIC);
        mTreeFontItalic = new Font(tree.getDisplay(), fontData);
        tree.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                mTreeFontItalic.dispose();
                mTreeFontItalic = null;
            }
        });
        mTreeViewer.setContentProvider(new PackageContentProvider(mTreeViewer, mPackageFilter));
        PkgCellAgent pkgCellAgent = new PkgCellAgent(mSdkContext, mPackageAnalyser, mTreeFontItalic);
        columnApi.setLabelProvider(
                new PkgTreeColumnViewerLabelProvider(new PkgCellLabelProvider(pkgCellAgent, PkgCellLabelProvider.API)));
        columnName.setLabelProvider(
                new PkgTreeColumnViewerLabelProvider(new PkgCellLabelProvider(pkgCellAgent, PkgCellLabelProvider.NAME)));
        columnStatus.setLabelProvider(
                new PkgTreeColumnViewerLabelProvider(new PkgCellLabelProvider(pkgCellAgent, PkgCellLabelProvider.STATUS)));
        columnRevision.setLabelProvider(
                new PkgTreeColumnViewerLabelProvider(new PkgCellLabelProvider(pkgCellAgent, PkgCellLabelProvider.REVISION)));
    }

    protected void selectPackages() {
    	PackageTypesSelector pkgTypesSelector = new PackageTypesSelector(getShell(), mPackageFilter.getPackageTypes());
    	if (pkgTypesSelector.open()) {
    		mPackageFilter.setPackageTypes(pkgTypesSelector.getPackageTypeSet());
    		refreshViewerInput();
    	}
	}

	private Image getImage(String filename) {
            ImageFactory imgFactory = mSdkContext.getSdkHelper().getImageFactory();
            if (imgFactory != null) {
                return imgFactory.getImageByName(filename);
            }
        return null;
    }


    // -- Start of internal part ----------
    // Hide everything down-below from SWT designer
    //$hide>>$


    // --- menu interactions ---

    protected void registerMenuAction(final MenuAction action, MenuItem item) {
    	item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                 Button button = null;

                switch (action) {
                case RELOAD:
                	startLoadPackages();
                    break;
//                case TOGGLE_SHOW_INSTALLED_PKG:
//                    button = mCheckFilterInstalled;
//                    break;
                case TOGGLE_SHOW_OBSOLETE_PKG:
                    button = mCheckFilterObsolete;
                    break;
                case TOGGLE_SHOW_NEW_PKG:
                    button = mCheckFilterNew;
                    break;
                case FILTER_PACKAGES:
                	selectPackages();
                	break;
                }

                if (button != null && !button.isDisposed()) {
                    // Toggle this button (radio or checkbox)

                    boolean value = button.getSelection();

                    // SWT doesn't automatically switch radio buttons when using the
                    // Widget#setSelection method, so we'll do it here manually.
                    if (!value && (button.getStyle() & SWT.RADIO) != 0) {
                        // we'll be selecting this radio button, so deselect all ther other ones
                        // in the parent group.
                        for (Control child : button.getParent().getChildren()) {
                            if (child instanceof Button &&
                                    child != button &&
                                    (child.getStyle() & SWT.RADIO) != 0) {
                                ((Button) child).setSelection(value);
                            }
                        }
                    }

                    button.setSelection(!value);

                    // SWT doesn't actually invoke the listeners when using Widget#setSelection
                    // so let's run the actual action.
                    button.notifyListeners(SWT.Selection, new Event());
                }

                updateMenuCheckmarks();
             }
        });

        mMenuActions.put(action, item);
    }

    // --- internal methods ---

    private void updateMenuCheckmarks() {
        for (Entry<MenuAction, MenuItem> entry : mMenuActions.entrySet()) {
            MenuAction action = entry.getKey();
            MenuItem item = entry.getValue();

            if (action.getMenuStyle() == SWT.NONE) {
                continue;
            }

            boolean value = false;
            Button button = null;

            switch (action) {
            //case TOGGLE_SHOW_INSTALLED_PKG:
            //    button = mCheckFilterInstalled;
            //    break;
            case TOGGLE_SHOW_OBSOLETE_PKG:
                button = mCheckFilterObsolete;
                break;
            case TOGGLE_SHOW_NEW_PKG:
                button = mCheckFilterNew;
                break;
            case RELOAD:
            case FILTER_PACKAGES:
                // No checkmark to update
                break;
            }

            if (button != null && !button.isDisposed()) {
                value = button.getSelection();
            }

            if (!item.isDisposed()) {
                item.setSelection(value);
            }
        }
    }

    private boolean postCreate() {
    	File sdkLocation = mSdkContext.getLocation();
    	// Only show SDK location if is valid
    	if (sdkLocation.exists() && sdkLocation.isDirectory())
            mTextSdkOsPath.setText(sdkLocation.toString());

        ((PackageContentProvider) mTreeViewer.getContentProvider()).setDisplayArchives(
                mDisplayArchives);

        ColumnViewerToolTipSupport.enableFor(mTreeViewer, ToolTip.NO_RECREATE);
        return true;

    }

	private void startLoadPackages() {
 
        if (mTreeColumnName.isDisposed()) {
            // If the UI got disposed, don't try to load anything since we won't be
            // able to display it anyway.
            return;
        }
        // Packages will be loaded when onReady() is called
        if (mProgressRunner == null)
        	return;
        mTreeColumnName.setImage(getImage(PackagesPageIcons.ICON_SORT_BY_API));

        PackageManager packageManager = mSdkContext.getPackageManager();
    	LoadPackagesRequest loadPackagesRequest = new LoadPackagesRequest(mProgressRunner);
    	RepoLoadedCallback onSuccess = new RepoLoadedCallback(){
			@Override
			public void doRun(RepositoryPackages packages) {
                if (!(mGroupPackages == null || mGroupPackages.isDisposed())) 
                {
                	packageManager.setPackages(packages);
                	mPackageAnalyser.loadPackages();
                    Collection<LocalPackage> localPackages = packageManager.getRepositoryPackages().getLocalPackagesForPrefix(PackageType.platforms.toString());
                    boolean hasPlatform = false;
                    if (!localPackages.isEmpty()) {
                    	Iterator<LocalPackage> iterator = localPackages.iterator();
                    	while (iterator.hasNext())
                    		if (iterator.next().getTypeDetails() instanceof PlatformDetailsType) {
                    			hasPlatform = true;
                    			break;
                    		}
                    }
                    if (!hasPlatform) {
                        mSdkProgressControl.setDescription("No Android Platform is installed. Please select one and then click on \"Install\" button.");
                        Set<PackageType> packageTypeSet = new TreeSet<>();
                        packageTypeSet.addAll(mPackageFilter.getPackageTypes());
                        packageTypeSet.add(PackageType.platforms);
                        mPackageFilter.setPackageTypes(packageTypeSet);
                        
                    } else {
                        mSdkProgressControl.setDescription("Done loading packages.");
                    }
                    mGroupPackages.getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
		                    // automatically select all new and update packages.
		                    Object[] checked = mTreeViewer.getCheckedElements();
		                    if (checked == null || checked.length == 0)
		                        onSelectPackages(
		                                true,  //selectUpdates,
		                                true); //selectTop
			                refreshViewerInput();
						}});
                }
			}};
		Runnable onError = new Runnable(){
			@Override
			public void run() {
				mSdkProgressControl.setDescription("Package operation did not complete due to error or cancellation");
			}};
		//loadPackagesRequest.setOnLocalComplete(Collections.singletonList(onLocalComplete));
    	loadPackagesRequest.setOnSuccess(Collections.singletonList(onSuccess));
    	loadPackagesRequest.setOnError(Collections.singletonList(onError));
    	packageManager.requestRepositoryPackages(loadPackagesRequest);
    }

    private void refreshViewerInput() {
        if (!mGroupPackages.isDisposed()) {
            try {
                setViewerInput();
            } catch (Exception ignore) {}

            // set the initial expanded state
            expandInitial(mTreeViewer.getInput());

            updateButtonsState();
            updateMenuCheckmarks();
        }
    }
    
    /**
     * Invoked from {@link #refreshViewerInput()} to actually either set the
     * input of the tree viewer or refresh it if it's the <em>same</em> input
     * object.
     */
    private void setViewerInput() {
        List<PkgCategory<AndroidVersion>> cats = mPackageAnalyser.getApiCategories();
        if ((mTreeViewer.getInput() != cats) || mPackageFilter.isFilterOn()) {
            // set initial input
        	if (mPackageFilter.isFilterOn())
                mTreeViewer.setInput(mPackageFilter.getFilteredApiCategories(cats));
        	else
                mTreeViewer.setInput(cats);
        } else {
            // refresh existing, which preserves the expanded state, the selection
            // and the checked state.
            mTreeViewer.refresh();
        }
    }

    /**
     * Decide whether to keep an item in the current tree based on user-chosen filter options.
     */
    private boolean filterViewerItem(Object treeElement) {
    	boolean selectNew = mCheckFilterNew.getSelection();
        if (treeElement instanceof PkgCategory) {
            PkgCategory<?> cat = (PkgCategory<?>) treeElement;
            cat.setSelectAllPackages(selectNew && mCheckAll.getSelection());
            if (!cat.getItems().isEmpty()) {
                // A category is hidden if all of its content is hidden.
                // However empty categories are always visible.
                for (PkgItem item : cat.getItems()) {
                    if (filterViewerItem(item)) {
                        // We found at least one element that is visible.
                        return true;
                    }
                }
                return false;
            }
        }

        if (treeElement instanceof PkgItem) {
            PkgItem item = (PkgItem) treeElement;

            if (!mCheckFilterObsolete.getSelection()) {
                if (item.isObsolete()) {
                    return false;
                }
            }
/*
            if (!mCheckFilterInstalled.getSelection()) {
                if (item.getState() == PkgState.INSTALLED) {
                    return false;
                }
            }
*/
            if (!selectNew) {
                if (item.getState() == PkgState.NEW ) { //|| item.hasUpdatePkg()
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Performs the initial expansion of the tree. This expands categories that contain
     * at least one installed item and collapses the ones with nothing installed.
     *
     * TODO: change this to only change the expanded state on categories that have not
     * been touched by the user yet. Once we do that, call this every time a new source
     * is added or the list is reloaded.
     */
    private void expandInitial(Object elem) {
        if (elem == null) {
            return;
        }
        if (mTreeViewer != null && !mTreeViewer.getTree().isDisposed()) {

            boolean enablePreviews =
                mSdkContext.getSettings().getEnablePreviews();

            mTreeViewer.setExpandedState(elem, true);
            nextCategory: for (Object pkg :
                    ((ITreeContentProvider) mTreeViewer.getContentProvider()).
                        getChildren(elem)) {
                if (pkg instanceof PkgCategory) {
                    PkgCategory<?> cat = (PkgCategory<?>) pkg;
                    // Always expand the Tools category (and the preview one, if enabled)
                    if ((cat.getKeyType() == CategoryKeyType.TOOLS) ||
                            (enablePreviews &&
                                    (cat.getKeyType() == CategoryKeyType.TOOLS_PREVIEW))) {
                        expandInitial(pkg);
                        continue nextCategory;
                    }
                    for (PkgItem item : cat.getItems()) {
                        if (item.getState() == PkgState.INSTALLED) {
                            expandInitial(pkg);
                            continue nextCategory;
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle checking and unchecking of the tree items.
     *
     * When unchecking, all sub-tree items checkboxes are cleared too.
     * When checking a source, all of its packages are checked too.
     * When checking a package, only its compatible archives are checked.
     */
    private void onTreeCheckStateChanged(CheckStateChangedEvent event) {
        boolean checked = event.getChecked();
        Object elem = event.getElement();

        assert event.getSource() == mTreeViewer;

        // When selecting, we want to only select compatible archives and expand the super nodes.
        checkAndExpandItem(elem, checked, true/*fixChildren*/, true/*fixParent*/);
        updateButtonsState();
    }

    private void onTreeDoubleClick(DoubleClickEvent event) {
        assert event.getSource() == mTreeViewer;
        ISelection sel = event.getSelection();
        if (sel.isEmpty() || !(sel instanceof ITreeSelection)) {
            return;
        }
        ITreeSelection tsel = (ITreeSelection) sel;
        Object elem = tsel.getFirstElement();
        if (elem == null) {
            return;
        }

        ITreeContentProvider provider =
            (ITreeContentProvider) mTreeViewer.getContentProvider();
        Object[] children = provider.getElements(elem);
        if (children == null) {
            return;
        }

        if (children.length > 0) {
            // If the element has children, expand/collapse it.
            if (mTreeViewer.getExpandedState(elem)) {
                mTreeViewer.collapseToLevel(elem, 1);
            } else {
                mTreeViewer.expandToLevel(elem, 1);
            }
        } else {
            // If the element is a terminal one, select/deselect it.
            checkAndExpandItem(
                    elem,
                    !mTreeViewer.getChecked(elem),
                    false /*fixChildren*/,
                    true /*fixParent*/);
            updateButtonsState();
        }
    }

    private void checkAndExpandItem(
            Object elem,
            boolean checked,
            boolean fixChildren,
            boolean fixParent) {
        ITreeContentProvider provider =
            (ITreeContentProvider) mTreeViewer.getContentProvider();

        // fix the item itself
        if (checked != mTreeViewer.getChecked(elem)) {
            mTreeViewer.setChecked(elem, checked);
        }
        if (elem instanceof PkgItem) {
            // update the PkgItem to reflect the selection
            ((PkgItem) elem).setChecked(checked);
        }

        if (!checked) {
            if (fixChildren) {
                // when de-selecting, we deselect all children too
                mTreeViewer.setSubtreeChecked(elem, checked);
                for (Object child : provider.getChildren(elem)) {
                    checkAndExpandItem(child, checked, fixChildren, false/*fixParent*/);
                }
            }

            // fix the parent when deselecting
            if (fixParent) {
                Object parent = provider.getParent(elem);
                if (parent != null && mTreeViewer.getChecked(parent)) {
                    mTreeViewer.setChecked(parent, false);
                }
            }
            return;
        }

        // When selecting, we also select sub-items (for a category)
        if (fixChildren) {
            if (elem instanceof PkgCategory || elem instanceof PkgItem) {
                Object[] children = provider.getChildren(elem);
                for (Object child : children) {
                    checkAndExpandItem(child, true, fixChildren, false/*fixParent*/);
                }
                // only fix the parent once the last sub-item is set
                if (elem instanceof PkgCategory) {
                    if (children.length > 0) {
                        checkAndExpandItem(
                                children[0], true, false/*fixChildren*/, true/*fixParent*/);
                    } else {
                        mTreeViewer.setChecked(elem, false);
                    }
                }
            } else if (elem instanceof Package) {
                // in details mode, we auto-select compatible packages
                selectCompatibleArchives(elem, provider);
            }
        }

        if (fixParent && checked && elem instanceof PkgItem) {
            Object parent = provider.getParent(elem);
            if (!mTreeViewer.getChecked(parent)) {
                Object[] children = provider.getChildren(parent);
                boolean allChecked = children.length > 0;
                for (Object e : children) {
                    if (!mTreeViewer.getChecked(e)) {
                        allChecked = false;
                        break;
                    }
                }
                if (allChecked) {
                    mTreeViewer.setChecked(parent, true);
                }
            }
        }
    }

    private void selectCompatibleArchives(Object pkg, ITreeContentProvider provider) {
        for (Object archive : provider.getChildren(pkg)) {
            if (archive instanceof Archive) {
                mTreeViewer.setChecked(archive, ((Archive) archive).isCompatible());
            }
        }
    }

    /**
     * Mark packages as checked according to selection criteria.
     */
    private void onSelectPackages(boolean selectUpdates, boolean selectTop) {
        // This will update the tree's "selected" state and then invoke syncViewerSelection()
        // which will in turn update tree.
        mPackageAnalyser.checkNewUpdateItems(
                selectUpdates,
                selectTop);
        mTreeViewer.setInput(mPackageAnalyser.getApiCategories());
        syncViewerSelection();
    }

    /**
     * Deselect all checked PkgItems.
     */
    private void onDeselectAll() {
        mPackageAnalyser.uncheckAllItems();
        syncViewerSelection();
    }

    /**
     * Synchronize the 'checked' state of PkgItems in the tree with their internal isChecked state.
     */
    private void syncViewerSelection() {
        ITreeContentProvider provider = (ITreeContentProvider) mTreeViewer.getContentProvider();

        Object input = mTreeViewer.getInput();
        if (input != null) {
            for (Object cat : provider.getElements(input)) {
                Object[] children = provider.getElements(cat);
                boolean allChecked = children.length > 0;
                for (Object child : children) {
                    if (child instanceof PkgItem) {
                        PkgItem item = (PkgItem) child;
                        boolean checked = item.isChecked();
                        allChecked &= checked;

                        if (checked != mTreeViewer.getChecked(item)) {
                            if (checked) {
                                if (!mTreeViewer.getExpandedState(cat)) {
                                    mTreeViewer.setExpandedState(cat, true);
                                }
                            }
                            checkAndExpandItem(
                                    item,
                                    checked,
                                    true/*fixChildren*/,
                                    false/*fixParent*/);
                        }
                    }
                }

                if (allChecked != mTreeViewer.getChecked(cat)) {
                    mTreeViewer.setChecked(cat, allChecked);
                }
            }
        }

        updateButtonsState();
    }

    /**
     * Indicate an install/delete operation is pending.
     * This disables the install/delete buttons.
     * Use {@link #endOperationPending()} to revert, typically in a {@code try..finally} block.
     */
    private void beginOperationPending() {
        mOperationPending = true;
        updateButtonsState();
    }

    private void endOperationPending() {
        mOperationPending = false;
        updateButtonsState();
    }

    /**
     * Updates the Install and Delete Package buttons.
     */
    private void updateButtonsState() {
        if (!mButtonInstall.isDisposed()) {
            int numPackages = getPackagesForInstall(null /*archives*/);

            mButtonInstall.setEnabled((numPackages > 0) && !mOperationPending);
            mButtonInstall.setText(
                    numPackages == 0 ? "Install packages..." :          // disabled button case
                        numPackages == 1 ? "Install 1 package..." :
                            String.format("Install %d packages...", numPackages));
        }

        if (!mButtonDelete.isDisposed()) {
            // We can only delete local archives
            int numPackages = getPackagesToDelete(null /*outMsg*/, null /*outArchives*/);

            mButtonDelete.setEnabled((numPackages > 0) && !mOperationPending);
            mButtonDelete.setText(
                    numPackages == 0 ? "Delete packages..." :           // disabled button case
                        numPackages == 1 ? "Delete 1 package..." :
                            String.format("Delete %d packages...", numPackages));
        }
    }

    /**
     * Called when the Install Package button is selected.
     * Collects the packages to be installed and shows the installation window.
     */
    private void onButtonInstall() {
        beginOperationPending();
    	List<PkgItem> requiredPackages = new ArrayList<>();
    	getPackagesForInstall(requiredPackages);
    	PackageInstaller packageInstaller = new PackageInstaller(requiredPackages, factory);
    	packageInstaller.installPackages(getShell(), mSdkContext, new PackageInstallListener(){

			@Override
			public void onPackagesInstalled(int count) {
				Display.getDefault().syncExec(new Runnable(){

					@Override
					public void run() {
			            endOperationPending();
			            // The local package list has changed, make sure to refresh it
			            startLoadPackages();
		                mButtonCancel.setText("OK");  
					}});
			}});
    }

    /**
     * Selects the packages that can be installed.
     * This can be used with a null {@code outPackageItems} just to count the number of
     * installable packages.
     *
     * @param outPackageItems A package item list to return remote packages.
     *   This can be null.
     * @return The number of archives that can be installed.
     */
    private int getPackagesForInstall(List<PkgItem> outPackageItems) {
        if (mTreeViewer == null ||
            mTreeViewer.getTree() == null ||
            mTreeViewer.getTree().isDisposed()) {
            return 0;
        }
        Object[] checked = mTreeViewer.getCheckedElements();
        if (checked == null) {
            return 0;
        }
        int count = 0;
        for (Object c : checked) {
            if (c instanceof PkgItem) {
                PkgItem packageItem = (PkgItem)c;
                RemotePackage remotePackage = null;
                if (packageItem.hasUpdatePkg()) {
            		remotePackage = packageItem.getUpdatePkg().getRemote();
                } else if (packageItem.getState() == PkgState.NEW) {
                	remotePackage = (RemotePackage) packageItem.getMainPackage();
                }
            if (remotePackage != null) {
                count++;
                if (outPackageItems != null) {
                	outPackageItems.add(packageItem);
                    }
                }
            }
        }
        return count;
    }

    /**
     * Called when the Delete Package button is selected.
     * Collects the packages to be deleted, prompt the user for confirmation
     * and actually performs the deletion.
     */
    private void onButtonDelete() {
        final String title = "Delete SDK Package";
        StringBuilder msg = new StringBuilder("Are you sure you want to delete:");

        // A list of package items to delete
        final ArrayList<PkgItem> outPackageItems = new ArrayList<PkgItem>();

        getPackagesToDelete(msg, outPackageItems);

        if (!outPackageItems.isEmpty()) {
            msg.append("\n").append("This cannot be undone.");  //$NON-NLS-1$
            if (MessageDialog.openQuestion(getShell(), title, msg.toString())) {
                beginOperationPending();
                Runnable onCompletion = new Runnable(){

					@Override
					public void run() {
	                    endOperationPending();

	                    // The local package list has changed, make sure to refresh it
	                	startLoadPackages();
	                    mButtonCancel.setText("OK");  
					}};
                mTaskFactory.start("Delete Package", new ITask() {
                    @Override
                    public void run(ITaskMonitor monitor) {
                        monitor.setProgressMax(outPackageItems.size() + 1);
                        for (PkgItem packageItem : outPackageItems) {
                        	LocalPackage localPackage = (LocalPackage)packageItem.getMainPackage();
                            monitor.setDescription("Deleting '%1$s' (%2$s)",
                            		localPackage.getDisplayName(),
                            		localPackage.getPath());

                            // Delete the actual package
                            Uninstaller uninstaller = SdkInstallerUtil.findBestInstallerFactory(localPackage, mSdkContext.getHandler())
                                    .createUninstaller(localPackage, mSdkContext.getRepoManager(), mSdkContext.getFileOp());
                            if (applyPackageOperation(uninstaller)) {
                            	packageItem.markDeleted();
                            } else {
                                // there was an error, abort.
                                monitor.error(null, "Uninstall of package failed due to an error");
                                monitor.setProgressMax(0);
                                break;
                            }
                            monitor.incProgress(1);
                            if (monitor.isCancelRequested()) {
                                break;
                            }
                        }

                        monitor.incProgress(1);
                        monitor.setDescription("Done");
                        mPackageAnalyser.removeDeletedNodes();
                    }
                }, onCompletion);
             }
        }
    }


	private boolean applyPackageOperation(
            @NonNull PackageOperation operation) {
    	ProgressIndicator progressIndicator = mSdkContext.getProgressIndicator();
        return operation.prepare(progressIndicator) && operation.complete(progressIndicator);
    }

    /**
     * Selects the archives that can be deleted and collect their names.
     * This can be used with a null {@code outArchives} and a null {@code outMsg}
     * just to count the number of archives to be deleted.
     *
     * @param outMsg A StringBuilder where the names of the packages to be deleted is
     *   accumulated. This is used to confirm deletion with the user.
     * @param outPackageItems A package item list to return local packages
     *   This can be null.
     * @return The number of packages that can be deleted.
     */
    private int getPackagesToDelete(StringBuilder outMsg, List<PkgItem> outPackageItems) {
        if (mTreeViewer == null ||
                mTreeViewer.getTree() == null ||
                mTreeViewer.getTree().isDisposed()) {
            return 0;
        }
        Object[] checked = mTreeViewer.getCheckedElements();
        if (checked == null) {
            // This should not happen since the button should be disabled
            return 0;
        }

        int count = 0;
        for (Object c : checked) {
            if (c instanceof PkgItem) {
                PkgItem packageItem = (PkgItem) c;
                PkgState state = packageItem.getState();
                if (state == PkgState.INSTALLED) {
                    LocalPackage localPackage = (LocalPackage)packageItem.getMainPackage();
                    count++;
                    if (outMsg != null) {
                        File dir = new File(localPackage.getPath());
                        if (dir.isDirectory()) {
                            outMsg.append("\n - ")    //$NON-NLS-1$
                                  .append(localPackage.getDisplayName());
                        }
                    }
                    if (outPackageItems != null) {
                    	outPackageItems.add(packageItem);
                    }
                }
            }
        }
        return count;
    }

    // --- End of hiding from SWT Designer ---
    //$hide<<$
}
