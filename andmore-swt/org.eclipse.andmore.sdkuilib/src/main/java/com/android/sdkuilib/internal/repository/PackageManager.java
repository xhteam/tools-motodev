package com.android.sdkuilib.internal.repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.sdktool.SdkContext;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.annotations.VisibleForTesting.Visibility;
import com.android.repository.api.Downloader;
import com.android.repository.api.Installer;
import com.android.repository.api.LocalPackage;
import com.android.repository.api.PackageOperation;
import com.android.repository.api.ProgressIndicator;
import com.android.repository.api.ProgressRunner;
import com.android.repository.api.RemotePackage;
import com.android.repository.api.RepoManager;
import com.android.repository.api.RepoManager.RepoLoadedCallback;
import com.android.repository.api.UpdatablePackage;
import com.android.repository.impl.meta.RepositoryPackages;
import com.android.repository.io.FileOpUtils;
import com.android.repository.io.impl.FileSystemFileOp;
import com.android.sdklib.repository.installer.SdkInstallerUtil;
import com.android.sdklib.repository.legacy.LegacyDownloader;
import com.google.common.collect.Maps;

public class PackageManager {

	public interface LocalPackageHandler
	{
		void onPackageLoaded(LocalPackage localPackage);
	}
	
	public interface RemotePackageHandler
	{
		void onPackageLoaded(RemotePackage remotePackage);
	}
	
	public interface UpdatablePackageHandler
	{
		void onPackageLoaded(UpdatablePackage updatablePackage);
	}
	
    /**
     * Map from {@code path} (the unique ID of a package) to {@link UpdatablePackage}, including all
     * packages installed or available.
     */
    private final Map<String, UpdatablePackage> consolidatedPkgs = Maps.newTreeMap();
    private final SdkContext sdkContext;
    private final Downloader downloader;
    private RepositoryPackages packages;

    public PackageManager(SdkContext sdkContext)
    {
    	this.sdkContext = sdkContext;
    	downloader = downloaderInstance();
    }

    public Downloader getDownloader()
    {
    	return downloader;
    }
    
    public UpdatablePackage getPackageById(String id) 
    {
    	return consolidatedPkgs.get(id);
    }
    
    public boolean loadLocalPackages(LocalPackageHandler handler)
    {
    	sdkContext.getRepoManager().loadSynchronously(0, sdkContext.getProgressIndicator(), downloader, sdkContext.getSettings());
        if (sdkContext.hasError())
        	return false;

        for (LocalPackage local : getRepositoryPackages().getLocalPackages().values()) 
            handler.onPackageLoaded(local);
        return true;
    }
    
    public boolean loadRemotePackages(RemotePackageHandler handler)
    {
    	sdkContext.getRepoManager().loadSynchronously(0, sdkContext.getProgressIndicator(), downloader, sdkContext.getSettings());
        if (sdkContext.hasError())
        	return false;

        for (RemotePackage remote : getRepositoryPackages().getRemotePackages().values()) 
            handler.onPackageLoaded(remote);
        return true;
    }
    
    public boolean loadUpdatablePackages(UpdatablePackageHandler handler)
    {
        if (sdkContext.hasError())
        	return false;
        for (UpdatablePackage updatable : getRepositoryPackages().getUpdatedPkgs()) 
            handler.onPackageLoaded(updatable);
        return true;
    }

    public boolean loadConsolidatedPackages(UpdatablePackageHandler handler)
    {
        if (sdkContext.hasError())
        	return false;
        consolidatedPkgs.clear();
        consolidatedPkgs.putAll(getRepositoryPackages().getConsolidatedPkgs());
        for (UpdatablePackage updatable : consolidatedPkgs.values()) 
            handler.onPackageLoaded(updatable);
        return true;
    }

    public RepositoryPackages getRepositoryPackages()
    {
    	if (packages == null)
    	{
    		RepoManager repoManager = sdkContext.getRepoManager();
    		repoManager.loadSynchronously(0, sdkContext.getProgressIndicator(), downloader, sdkContext.getSettings());
   		    packages =  repoManager.getPackages();
    	}
    	return packages;
    }
/*
    public abstract void load(long cacheExpirationMs,
            @Nullable List<RepoLoadedCallback> onLocalComplete,
            @Nullable List<RepoLoadedCallback> onSuccess,
            @Nullable List<Runnable> onError,
            @NonNull ProgressRunner runner,
            @Nullable Downloader downloader,
            @Nullable SettingsController settings,
            boolean sync);
 */
 
    public void requestRepositoryPackages(LoadPackagesRequest loadPackagesRequest)
	{
		RepoManager repoManager = sdkContext.getRepoManager();
		repoManager.load(0, 
				loadPackagesRequest.getOnLocalComplete(), 
				loadPackagesRequest.getOnSuccess(), 
				loadPackagesRequest.getOnError(), 
				loadPackagesRequest.getRunner(), 
				downloader, 
				sdkContext.getSettings(), 
				false);
	}
    
    public RepositoryPackages getRepositoryPackages(
	            @Nullable List<RepoLoadedCallback> onLocalComplete,
	            @Nullable List<RepoLoadedCallback> onSuccess,
	            @Nullable List<Runnable> onError,
	            @NonNull ProgressRunner runner)
    {
    	if (packages == null)
    	{
    		RepoManager repoManager = sdkContext.getRepoManager();
    		repoManager.load(0, onLocalComplete, onSuccess, onError, runner, downloader, sdkContext.getSettings(), true);
   		    packages =  repoManager.getPackages();
    	}
    	return packages;
    }

	public void setPackages(RepositoryPackages packages) {
		this.packages = packages;
	}

    /**
     * Install the list of given {@link Package}s. This is invoked by the user selecting some
     * packages in the remote page and then clicking "install selected".
     *
     * @param packages The packages to install.
     * @param taskFactory Task factory to show progress during installation
     * @param flags Optional flags for the installer, such as {@link #NO_TOOLS_MSG}.
     * @return A list of packages that have been installed. Can be empty but not null.
     */
    @VisibleForTesting(visibility=Visibility.PRIVATE)
    public int installPackages(final List<RemotePackage> packages, List<RemotePackage> acceptedRemotes, ITaskFactory taskFactory, final int flags) {
        // List to accumulate all the packages installed.
        int[] numInstalled = new int[]{0};
        taskFactory.start("Installing Packages", new ITask() {
            @Override
            public void run(ITaskMonitor monitor) {
                List<RemotePackage> rejectedRemotes = new ArrayList<>();
                Iterator<RemotePackage> iterator = packages.iterator();
                while (iterator.hasNext()) {
                	RemotePackage remote = iterator.next();
                	if (!acceptedRemotes.contains(remote))
                		rejectedRemotes.add(remote);
                }
                List<RemotePackage> remotes = new ArrayList<>();
                remotes.addAll(packages);
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
                    Installer installer = SdkInstallerUtil.findBestInstallerFactory(remotePackage, sdkContext.getHandler())
                            .createInstaller(remotePackage, sdkContext.getRepoManager(), downloader, sdkContext.getFileOp());
	                if (applyPackageOperation(installer)) {
	                	++numInstalled[0];
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
/* TODO - Complete post package install operations
                if (installedAddon) {
                    // Update the USB vendor ids for adb
                    try {
                        mSdkManager.updateAdb();
                        monitor.log("Updated ADB to support the USB devices declared in the SDK add-ons.");
                    } catch (Exception e) {
                        mSdkLog.error(e, "Update ADB failed");
                        monitor.logError("failed to update adb to support the USB devices declared in the SDK add-ons.");
                    }
                }

                if (preInstallHookInvoked) {
                    broadcastPostInstallHook();
                }

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
        });

        return numInstalled[0];
    }

    private Downloader downloaderInstance()
    {
        FileSystemFileOp fop = (FileSystemFileOp)FileOpUtils.create();
        return new LegacyDownloader(fop, sdkContext.getSettings());
    }

	private boolean applyPackageOperation(
            @NonNull PackageOperation operation) {
    	ProgressIndicator progressIndicator = sdkContext.getProgressIndicator();
        return operation.prepare(progressIndicator) && operation.complete(progressIndicator);
    }


}
