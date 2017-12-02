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

    protected Installer createInstaller(RemotePackage remotePackage) {
    	return SdkInstallerUtil.findBestInstallerFactory(remotePackage, sdkContext.getHandler())
                .createInstaller(remotePackage, sdkContext.getRepoManager(), downloader, sdkContext.getFileOp());

    }
    
	private Downloader downloaderInstance()
    {
        FileSystemFileOp fop = (FileSystemFileOp)FileOpUtils.create();
        return new LegacyDownloader(fop, sdkContext.getSettings());
    }

	boolean applyPackageOperation(
            @NonNull PackageOperation operation) {
    	ProgressIndicator progressIndicator = sdkContext.getProgressIndicator();
        return operation.prepare(progressIndicator) && operation.complete(progressIndicator);
    }


}
