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
/**
 * 
 */
package com.android.sdkuilib.internal.repository.content;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.andmore.sdktool.SdkContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.android.repository.api.LocalPackage;
import com.android.repository.api.ProgressIndicator;
import com.android.repository.api.RemotePackage;
import com.android.repository.api.UpdatablePackage;
import com.android.repository.util.InstallerUtil;
import com.android.sdkuilib.internal.repository.ITask;
import com.android.sdkuilib.internal.repository.ITaskFactory;
import com.android.sdkuilib.internal.repository.ITaskMonitor;
import com.android.sdkuilib.internal.repository.PackageInstallListener;
import com.android.sdkuilib.internal.repository.PackageInstallTask;
import com.android.sdkuilib.internal.repository.ui.SdkProgressFactory;
import com.android.sdkuilib.internal.repository.ui.SdkUpdaterChooserDialog;
import com.android.sdkuilib.internal.tasks.ILogUiProvider;

/**
 * Installs specified packages and their dependencies. 
 * The package details must first be loaded by PackageAnalyser. 
 * At the start of the installation, a dialog informs the user of the packages to
 * be installed and prompts for acceptance of license terms.
 * @author Andrew Bowley
 *
 * 29-11-2017
 */
public class PackageInstaller {

	private static final String NONE_INSTALLED = "Done. Nothing was installed.";
	
    private final List<PkgItem> requiredPackageItems = new ArrayList<PkgItem>();
    private final List<RemotePackage> remotes = new ArrayList<>();
    private final Map<RemotePackage,UpdatablePackage> updateMap = new HashMap<>();
    private final SdkProgressFactory factory;
	private volatile int numInstalled;

    /**
	 * 
	 */
	public PackageInstaller(PackageAnalyser packageAnalyser, PackageVisitor packageVisitor, SdkProgressFactory factory) {
		this.factory = factory;
   	    for (PkgItem packageItem: packageAnalyser.getAllPkgItems()) {
   	    	if (!packageVisitor.visit(packageItem))
   	    		break;
   	    }
   	    for (PkgItem packageItem: packageAnalyser.getAllPkgItems()) {
            // Is this the package we want to install?
            if (packageVisitor.accept(packageItem))
            	requiredPackageItems.add(packageItem);
   	    }
   	    assemblePackages();
	}

    /**
	 * 
	 */
	public PackageInstaller(List<PkgItem> requiredPackageItems, SdkProgressFactory factory) {
		this.factory = factory;
		this.requiredPackageItems.addAll(requiredPackageItems);
   	    assemblePackages();
	}

	public Collection<? extends PkgItem> getRequiredPackageItems() {
		return requiredPackageItems;
	}
	
	public int getNumInstalled() {
		return numInstalled;
	}

	public void installPackages(Shell shell, SdkContext sdkContext, PackageInstallListener installListener) {
		ITaskFactory taskFactory = factory;
        List<RemotePackage> acceptedRemotes = new ArrayList<>();
		ITask prepareTask = new ITask(){

			@Override
			public void run(ITaskMonitor monitor) {
		        if (computeDependencies(sdkContext) > 0) {
			        SdkUpdaterChooserDialog dialog =
			                new SdkUpdaterChooserDialog(shell, sdkContext, updateMap.values(), remotes);
			        Display.getDefault().syncExec(new Runnable(){
	
						@Override
						public void run() {
				            dialog.open();
				            acceptedRemotes.addAll(dialog.getResult());
						}});
		        } else {
			 		ILogUiProvider sdkProgressControl = factory.getProgressControl();
					sdkProgressControl.setDescription(NONE_INSTALLED);
		        	if (installListener != null) {
	                    installListener.onPackagesInstalled(0);
			        }
		        }
			}};
			
        taskFactory.start("Preparing Packages", prepareTask, new Runnable(){

			@Override
			public void run() {
			    if (acceptedRemotes.size() > 0) {
			 		ILogUiProvider sdkProgressControl = factory.getProgressControl();
			        PackageInstallTask packageInstallTask = new PackageInstallTask(sdkContext, remotes, acceptedRemotes) {
			    		@Override
			    		public void run() {
			                int count = getNumInstalled();
			                numInstalled = count;
			                if (count == 0) {
								sdkProgressControl.setDescription(NONE_INSTALLED);
			                }
			                else {
			                	sdkProgressControl.setDescription(String.format("Done. %1$d %2$s installed.",
			                			count,
			                			count == 1 ? "package" : "packages"));
			                }
			                if (installListener != null)
			                	installListener.onPackagesInstalled(count);
			    		}
			    	};
			    	factory.start("Installing Packages", packageInstallTask, packageInstallTask);
		        } else {
			 		ILogUiProvider sdkProgressControl = factory.getProgressControl();
					sdkProgressControl.setDescription(NONE_INSTALLED);
		        	if (installListener != null) {
	                    installListener.onPackagesInstalled(0);
			        }
		        }
			}});
	}  
	
    private boolean assemblePackages() {
    	List<PkgItem> installedList = null;
        for (PkgItem item: requiredPackageItems) {
        	RemotePackage remotePackage = null;
        	if (item.hasUpdatePkg()) { // Update installed package
        		remotePackage = item.getUpdatePkg().getRemote();
        		updateMap.put(remotePackage, item.getUpdatePkg());
        	} else if (item.getMainPackage() instanceof LocalPackage) {
        		factory.getProgressControl().setDescription("Package " + item.getMainPackage().getDisplayName() + " is already installed on the local SDK");
        		if (installedList == null)
        			installedList = new ArrayList<>();
        		installedList.add(item);
        		continue;
        	} else {
        		remotePackage = (RemotePackage)item.getMainPackage();
        	}
        	remotes.add(remotePackage);
        }
		if (installedList != null)
			requiredPackageItems.removeAll(installedList);
        return !requiredPackageItems.isEmpty();
	}

    private int computeDependencies(SdkContext sdkContext) {
        // computeRequiredPackages() may produce a list containing duplicates!
    	ProgressIndicator progress = sdkContext.getProgressIndicator();
        List<RemotePackage> requiredPackages = InstallerUtil.computeRequiredPackages(
                remotes, sdkContext.getPackages(), progress);
        if (requiredPackages == null) {
            progress.logWarning("Unable to compute a complete list of dependencies.");
            return 0;
        }
        Iterator<RemotePackage> iterator = requiredPackages.iterator();
        Set<RemotePackage> existenceSet = new HashSet<>();
        existenceSet.addAll(remotes);
        while (iterator.hasNext()) {
        	RemotePackage requiredPackage = iterator.next();
        	if (!existenceSet.contains(requiredPackage)) {
        		{
        			existenceSet.add(requiredPackage);
        			remotes.add(requiredPackage);
        		}
        	}
        }
        // Remove references now existenceSet no longer needed
        existenceSet.clear();
        return remotes.size();
    }

}
