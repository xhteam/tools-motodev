package com.android.sdkuilib.internal.repository.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.sdktool.SdkContext;

import com.android.SdkConstants;
import com.android.repository.api.LocalPackage;
import com.android.repository.api.RemotePackage;
import com.android.repository.api.RepoPackage;
import com.android.repository.api.UpdatablePackage;
import com.android.repository.impl.meta.TypeDetails;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.repository.meta.DetailsTypes;
import com.android.sdkuilib.internal.repository.PackageManager.UpdatablePackageHandler;
import com.android.sdkuilib.internal.repository.content.UpdateOp.SdkSource;

public class PackageAnalyser {
	public static final String GENERIC = "generic";
	
    private final SdkContext mSdkContext;
    private final UpdateOpApi mOpApi;
    private final Map<String, MetaPackage> mMetaPackageMap = new HashMap<>();
    private boolean mFirstLoadComplete = true;
    
    /**
     * {@link PkgState}s to check in {@link #processSource(UpdateOp, SdkSource, Package[])}.
     */
    public static enum PkgState { INSTALLED, NEW, DELETED };

    public PackageAnalyser(SdkContext sdkContext)
    {
    	this.mSdkContext = sdkContext;
    	mOpApi = new UpdateOpApi();
    	initMetaPackages();
    }

    public MetaPackage getMetaPackage(String name)
    {
    	return mMetaPackageMap.get(name);
    }

	/**
     * Removes all the internal state and resets the object.
     * Useful for testing.
     */
    public void clear() {
        mFirstLoadComplete = true;
        mOpApi.clear();
    }

    /** Return mFirstLoadComplete and resets it to false.
     * All following calls will returns false. */
    public boolean isFirstLoadComplete() {
        boolean b = mFirstLoadComplete;
        mFirstLoadComplete = false;
        return b;
    }


    /**
     * Mark all new and update PkgItems as checked.
     *
     * @param selectNew If true, select all new packages (except the rc/preview ones).
     * @param selectUpdates If true, select all update packages.
     * @param selectTop If true, select the top platform. All new packages are selected, excluding system images and 
     *    rc/preview. Packages to update are selected regardless.
     * @param currentPlatform The {@link SdkConstants#currentPlatform()} value.
     */
    public void checkNewUpdateItems(
            boolean selectUpdates,
            boolean selectTop) {
    	int apiLevel = 0;
    	if (selectTop) {
	    	for (PkgCategory<AndroidVersion> cat: mOpApi.getCategories())
	    	{
	    		// Find first API category to get top API
	    		if (cat.getKeyType() == CategoryKeyType.API) {
	    			PkgCategoryApi pkgCategoryApi = (PkgCategoryApi)cat;
	    			apiLevel = pkgCategoryApi.getKeyValue().getApiLevel();
	    			break;
	    		}
	    	}
    	}
		for (PkgCategory<AndroidVersion> cat: mOpApi.getCategories())
			checkNode(cat, selectUpdates, apiLevel, 0);
    }

	private void checkNode(INode node, boolean selectUpdates, int topApiLevel, int level) 
	{
		node.checkSelections(selectUpdates, topApiLevel);
		for (INode child: node.getChildren())
			checkNode(child, selectUpdates, topApiLevel, level+ 1);
	}
	
    /**
     * Mark all PkgItems as not checked.
     */
    public void uncheckAllItems() {
		for (PkgCategory<AndroidVersion> cat: mOpApi.getCategories())
			checkNode(cat, false, 0, 0);
    }
    	
    public List<PkgCategory<AndroidVersion>> getApiCategories() {
        return mOpApi.getCategories();
    }

    public List<PkgItem> getAllPkgItems() {
        List<PkgItem> items = new ArrayList<PkgItem>();

        List<PkgCategory<AndroidVersion>> cats = getApiCategories();
        synchronized (cats) {
            for (PkgCategory<AndroidVersion> cat : cats) {
                items.addAll(cat.getItems());
            }
        }
        return items;
    }

    public void updateStart() {
        mOpApi.updateStart();
    }

	public void removeDeletedNodes() {
		for (PkgCategory<AndroidVersion> cat: mOpApi.getCategories())
			remoteDeleted(cat, 0);
	}

    private void remoteDeleted(INode node, int level) {
    	List<INode> removeList = null;
    	for (INode childNode: node.getChildren()) {
    		if (childNode.isDeleted()) {
    			if (removeList == null)
    				removeList = new ArrayList<>();
    			removeList.add(childNode);
    		}
    		remoteDeleted(childNode, level + 1);
    	}
        if (removeList != null) {
        	for (INode removeNode: removeList) {
        		node.getChildren().remove(removeNode);
        	}
    	}
	}

	public boolean updateSourcePackages(SdkSource source, RepoPackage[] newPackages) {

        return mOpApi.updateSourcePackages(source, newPackages);
    }

    public boolean updateEnd() {
        return mOpApi.updateEnd();
    }

    public static String getNameFromPath(String path)
    {
    	int pos = path.indexOf(RepoPackage.PATH_SEPARATOR);
    	return pos == -1 ? path : path.substring(0, pos);
    }
    
    public static AndroidVersion getAndroidVersion(RepoPackage pkg) {
        TypeDetails details = pkg.getTypeDetails();
        if (details instanceof DetailsTypes.ApiDetailsType) {
        	return ((DetailsTypes.ApiDetailsType)details).getAndroidVersion();
        }
        return null;
    }

    private void initMetaPackages() {
    	MetaPackage metaPackage = new MetaPackage(PackageType.tools, "tool_pkg_16.png");
    	mMetaPackageMap.put(metaPackage.getName(), metaPackage);
    	
    	metaPackage = new MetaPackage(PackageType.platform_tools, "platformtool_pkg_16.png");
    	mMetaPackageMap.put(metaPackage.getName(), metaPackage);

    	metaPackage = new MetaPackage(PackageType.build_tools, "buildtool_pkg_16.png");
    	mMetaPackageMap.put(metaPackage.getName(), metaPackage);

    	metaPackage = new MetaPackage(PackageType.platforms, "platform_pkg_16.png");
    	mMetaPackageMap.put(metaPackage.getName(), metaPackage);

    	metaPackage = new MetaPackage(PackageType.add_ons, "addon_pkg_16.png");
    	mMetaPackageMap.put(metaPackage.getName(), metaPackage);

    	metaPackage = new MetaPackage(PackageType.system_images, "sysimg_pkg_16.png");
    	mMetaPackageMap.put(metaPackage.getName(), metaPackage);

    	metaPackage = new MetaPackage(PackageType.sources, "source_pkg_16.png");
    	mMetaPackageMap.put(metaPackage.getName(), metaPackage);

    	metaPackage = new MetaPackage(PackageType.docs, "doc_pkg_16.png");
    	mMetaPackageMap.put(metaPackage.getName(), metaPackage);

        metaPackage = new MetaPackage(PackageType.extras, "extra_pkg_16.png");
    	mMetaPackageMap.put(metaPackage.getName(), metaPackage);

        metaPackage = new MetaPackage(PackageType.emulator, "tool_pkg_16.png");
    	mMetaPackageMap.put(metaPackage.getName(), metaPackage);

        metaPackage = new MetaPackage(PackageType.cmake, "tool_pkg_16.png");
    	mMetaPackageMap.put(metaPackage.getName(), metaPackage);

        metaPackage = new MetaPackage(PackageType.lldb, "tag_default_16.png");
    	mMetaPackageMap.put(metaPackage.getName(), metaPackage);

        metaPackage = new MetaPackage(PackageType.ndk_bundle, "tag_default_16.png");
    	mMetaPackageMap.put(metaPackage.getName(), metaPackage);

        metaPackage = new MetaPackage(PackageType.patcher, "tool_pkg_16.png");
    	mMetaPackageMap.put(metaPackage.getName(), metaPackage);

        metaPackage = new MetaPackage(PackageType.generic, "tag_default_16.png");
    	mMetaPackageMap.put(metaPackage.getName(), metaPackage);	
    }

	public void loadPackages() {
		UpdatablePackageHandler updateHandler = new UpdatablePackageHandler(){

			@Override
			public void onPackageLoaded(UpdatablePackage updatePackage) {
				updateApiItem(updatePackage);
			}};
	    // All previous entries must be deleted or duplicates will occur		
		mOpApi.getCategories().clear();
		mSdkContext.getPackageManager().loadConsolidatedPackages(updateHandler);
		mOpApi.sortCategoryList();
	}
	
	private void updateApiItem(UpdatablePackage updatePackage) {
		LocalPackage local = updatePackage.getLocal();
		RemotePackage remote = updatePackage.getRemote();
		PkgCategory<AndroidVersion> cat = null;
		PkgItem item = null;
		if (local != null) {
			cat = getPkgCategory(local);
	        item = new PkgItem(local, metaPackageFromPackage(local), PkgState.INSTALLED);
	        if ((remote != null) && !local.getVersion().equals(remote.getVersion()))
	        	item.setUpdatePkg(updatePackage);
		}
		else {
			cat = getPkgCategory(remote);
	        item = new PkgItem(remote, metaPackageFromPackage(remote), PkgState.NEW);
		}
        cat.getItems().add(item);
	}

	private PkgCategory<AndroidVersion> getPkgCategory(RepoPackage pkg)
	{
        List<PkgCategory<AndroidVersion>> cats = mOpApi.getCategories();
        CategoryKeyType catKeyType = mOpApi.getCategoryKeyType(pkg);
        PkgCategory<AndroidVersion> cat = null;
        AndroidVersion catKeyValue = null;
        switch (catKeyType)
        {
        case API: 
        case REMOTE:
        	catKeyValue = mOpApi.getCategoryKeyValue(pkg);
        	cat = findCurrentCategory(cats, catKeyType, catKeyValue);
        	break;
        default:	
        	cat = findCurrentCategory(cats, catKeyType);
        }
        if (cat == null) {
            // This is a new category. Create it and add it to the list.
            cat = mOpApi.createCategory(catKeyType, catKeyValue);
            synchronized (cats) {
                cats.add(cat);
            }
        }
        return cat;
	}
	
    private PkgCategory<AndroidVersion> findCurrentCategory(
            List<PkgCategory<AndroidVersion>> currentCategories,
            CategoryKeyType catKeyType) {
        for (PkgCategory<AndroidVersion> cat : currentCategories) {
            if (cat.getKeyType() == catKeyType) {
                return cat;
            }
        }
        return null;
    }

    private PkgCategory<AndroidVersion> findCurrentCategory(
            List<PkgCategory<AndroidVersion>> currentCategories,
            CategoryKeyType catKeyType, Object categoryKeyValue) {
        for (PkgCategory<AndroidVersion> cat : currentCategories) {
            if ((cat.getKeyType() == catKeyType) && (cat.getKeyValue().equals(categoryKeyValue)))
                return cat;
            }
        return null;
    }
    
    private MetaPackage metaPackageFromPackage(RepoPackage repoPackage)
    {
    	String name = PackageAnalyser.getNameFromPath(repoPackage.getPath());
    	MetaPackage metaPackage = getMetaPackage(name);
    	return metaPackage != null ? metaPackage : getMetaPackage(PackageAnalyser.GENERIC);
    }

}
