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
package com.android.sdkuilib.internal.repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.andmore.sdktool.SdkContext;

import com.android.repository.api.Installer;
import com.android.repository.api.RemotePackage;

/**
 * @author Andrew Bowley
 *
 * 29-11-2017
 */
public abstract class PackageInstallTask implements ITask, Runnable {

    private final SdkContext sdkContext;
	private final PackageManager packageManager;
	private final List<RemotePackage> packageList; 
	private final List<RemotePackage> acceptedRemotes;
	private int numInstalled;
	
	/**
	 * 
	 */
	public PackageInstallTask(SdkContext sdkContext, List<RemotePackage> packageList, List<RemotePackage> acceptedRemotes) {
		this.sdkContext = sdkContext;
		this.packageManager = sdkContext.getPackageManager();
		this.packageList = packageList;
		this.acceptedRemotes = acceptedRemotes;
	}

	public int getNumInstalled() {
		return numInstalled;
	}

	/* (non-Javadoc)
	 * @see com.android.sdkuilib.internal.repository.ITask#run(com.android.sdkuilib.internal.repository.ITaskMonitor)
	 */
	@Override
	public void run(ITaskMonitor monitor) {
    	// Assume installation will proceed instead of complicating the install task
    	sdkContext.getSdkHelper().broadcastPreInstallHook(sdkContext.getSdkLog());
        List<RemotePackage> rejectedRemotes = new ArrayList<>();
        Iterator<RemotePackage> iterator = packageList.iterator();
        while (iterator.hasNext()) {
        	RemotePackage remote = iterator.next();
        	if (!acceptedRemotes.contains(remote))
        		rejectedRemotes.add(remote);
        }
        List<RemotePackage> remotes = new ArrayList<>();
        remotes.addAll(packageList);
    	if (!rejectedRemotes.isEmpty()) {
    		String title = "Package licences not accepted";
         	StringBuilder builder = new StringBuilder();
        	builder.append("The following packages can not be installed since their " +
                          "licenses or those of the packages they depend on were not accepted:");
        	Iterator<RemotePackage> iterator2 = rejectedRemotes.iterator();
        	while(iterator2.hasNext())
        		builder.append('\n').append(iterator2.next().getPath());
            if (!acceptedRemotes.isEmpty()) {
            	builder.append("\n\nContinue installing the remaining packages?");
            	if (!monitor.displayPrompt(title, 
                                           builder.toString()))
            		return;
                else {
                	monitor.displayInfo(title, builder.toString());
            		return;
                }
            }
            remotes = acceptedRemotes;
    	}
        final int progressPerPackage = 2 * 100;
        monitor.setProgressMax(1 + remotes.size() * progressPerPackage);
        monitor.setDescription("Preparing to install packages");
        for (RemotePackage remotePackage : remotes) {
            int nextProgress = monitor.getProgress() + progressPerPackage;
            Installer installer = packageManager.createInstaller(remotePackage);
            if (packageManager.applyPackageOperation(installer)) {
            	++numInstalled;
            } else {
                // there was an error, abort.
                monitor.error(null, "Install of package failed due to an error");
                monitor.setProgressMax(0);
                break;
            }
            if (monitor.isCancelRequested()) {
                break;
            }
            monitor.incProgress(nextProgress - monitor.getProgress());
        }
        if (numInstalled > 0)
    	    sdkContext.getSdkHelper().broadcastPostInstallHook(sdkContext.getSdkLog());
        /* Post package install operations used to give the user an opportuning to restart ADB
         * and advise check for ADT Updates 
        if (installedAddon || installedPlatformTools) {
            // We need to restart ADB. Actually since we don't know if it's even
            // running, maybe we should just kill it and not start it.
            // Note: it turns out even under Windows we don't need to kill adb
            // before updating the tools folder, as adb.exe is (surprisingly) not
            // locked.

            askForAdbRestart(monitor);
        }

        if (installedTools) {
            notifyToolsNeedsToBeRestarted(flags);
        }
        */
    }

}
