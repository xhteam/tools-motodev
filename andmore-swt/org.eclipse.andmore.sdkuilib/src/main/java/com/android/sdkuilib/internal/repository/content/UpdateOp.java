package com.android.sdkuilib.internal.repository.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.android.repository.api.RepoPackage;

/**
 * An update operation, customized to either sort by API or sort by source.
 */
public abstract class UpdateOp<K> {
	static public class SdkSource{}
	
    private final List<PkgCategory<K>> mCategories = new ArrayList<>();
    private final Set<PkgCategory<K>> mCatsToRemove = new HashSet<>();
    private final Set<PkgItem> mItemsToRemove = new HashSet<>();
    private final Map<RepoPackage, PkgItem> mUpdatesToRemove = new HashMap<>();

    /** Removes all internal state. */
    public void clear() {
    }

    /** Retrieve the sorted category list. */
    public List<PkgCategory<K>> getCategories() {
    	return mCategories;
    }

    /** Retrieve the category key type for the given package */
    public abstract CategoryKeyType getCategoryKeyType(RepoPackage pkg);

    /** Retrieve the category key value for the given package. May be null. */
    public abstract K getCategoryKeyValue(RepoPackage pkg);

    /** Modified {@code currentCategories} to add default categories. */
    public abstract void addDefaultCategories();

    /** Creates the category for the given key and returns it. */
    public abstract PkgCategory<K> createCategory(CategoryKeyType catKeyType, K catKeyValue);

    /** Sorts the category list (but not the items within the categories.) */
    public abstract void sortCategoryList();

    /** Called after items of a given category have changed. Used to sort the
     * items and/or adjust the category name. */
    public abstract void postCategoryItemsChanged();

    public void updateStart() {
    }
    
    public boolean updateSourcePackages(SdkSource source, RepoPackage[] newPackages) {
    	return false;
    }
    
    public boolean updateEnd() {
    	return false;
    }
    public boolean isKeep(PkgItem item) {
        return !mItemsToRemove.contains(item);
    }

    public void keep(Package pkg) {
        mUpdatesToRemove.remove(pkg);
    }

    public void keep(PkgItem item) {
        mItemsToRemove.remove(item);
    }

    public void keep(PkgCategory<K> cat) {
        mCatsToRemove.remove(cat);
    }

    public void dontKeep(PkgItem item) {
        mItemsToRemove.add(item);
    }

    public void dontKeep(PkgCategory<K> cat) {
        mCatsToRemove.add(cat);
    }
}
