package com.android.sdkuilib.internal.repository.content;

import java.util.ArrayList;
import java.util.List;

import com.android.repository.api.RepoPackage;

/**
 * An update operation, customized to either sort by API or sort by source.
 */
public abstract class UpdateOp<K> {
	static public class SdkSource{}
	
    private final List<PkgCategory<K>> mCategories = new ArrayList<>();

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

    /** Creates the category for the given key and returns it. */
    public abstract PkgCategory<K> createCategory(CategoryKeyType catKeyType, K catKeyValue);

    /** Sorts the category list (but not the items within the categories.) */
    public abstract void sortCategoryList();

}
