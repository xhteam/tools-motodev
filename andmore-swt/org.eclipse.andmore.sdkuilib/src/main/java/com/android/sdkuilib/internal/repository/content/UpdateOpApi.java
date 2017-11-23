package com.android.sdkuilib.internal.repository.content;

import java.util.Collections;
import java.util.Comparator;

import com.android.repository.api.RepoPackage;
import com.android.sdkuilib.internal.repository.ui.PackagesPageIcons;
import com.android.sdklib.AndroidVersion;

public class UpdateOpApi extends UpdateOp<AndroidVersion> {

	@Override
	public CategoryKeyType getCategoryKeyType(RepoPackage pkg) {
        // Sort by API
        AndroidVersion androidVersion = PkgItem.getAndroidVersion(pkg);
        if (androidVersion != null) {
            return CategoryKeyType.API;

        } else if (pkg.getPath().indexOf("tools") != -1) {
            if (PkgItem.isPreview(pkg)) {
                return CategoryKeyType.TOOLS_PREVIEW;
            } else {
                return CategoryKeyType.TOOLS;
            }
        } else {
            return CategoryKeyType.EXTRA;
        }
	}
	
	@Override
	public AndroidVersion getCategoryKeyValue(RepoPackage pkg) {
        // Sort by API
        return PkgItem.getAndroidVersion(pkg);
	}

	@Override
	public void addDefaultCategories() {
		// TODO Auto-generated method stub

	}

	@Override
	public PkgCategory<AndroidVersion> createCategory(CategoryKeyType catKeyType, AndroidVersion catKeyValue) {
        // Create API category.
        PkgCategory<AndroidVersion> cat = null;

        // We should not be trying to recreate the tools or extra categories.
        //assert (catKeyType != CategoryKeyType.TOOLS) && (catKeyType != CategoryKeyType.EXTRA);
        cat = new PkgCategoryApi(
        	catKeyType,
        	catKeyValue,
        	PackagesPageIcons.ICON_CAT_PLATFORM);

        return cat;
	}

	@Override
	public void sortCategoryList() {
        // Sort the categories list.
        // We always want categories in order tools..platforms..extras.
        // For platform, we compare in descending order (o2-o1).
        // This order is achieved by having the category keys ordered as
        // needed for the sort to just do what we expect.

        synchronized (getCategories()) {
            Collections.sort(getCategories(), new Comparator<PkgCategory<AndroidVersion>>() {
                @Override
                public int compare(PkgCategory<AndroidVersion> cat1, PkgCategory<AndroidVersion> cat2) {
                	int comparison1 = cat1.getKeyType().ordinal() - cat2.getKeyType().ordinal();
                	if ((cat1.getKeyType() == CategoryKeyType.API) && (cat2.getKeyType() == CategoryKeyType.API))
                		return cat2.getKeyValue().compareTo(cat1.getKeyValue());
                	else
                        return comparison1;
                }
            });
            for (PkgCategory<AndroidVersion> cat: getCategories())
            	sortPackages(cat);
        }
	}

	@Override
	public void postCategoryItemsChanged() {
		// TODO Auto-generated method stub

	}

	private void sortPackages(PkgCategory<AndroidVersion> cat)
	{
		synchronized (cat)
		{
			Collections.sort(cat.getItems(),  new Comparator<PkgItem>() {

				@Override
				public int compare(PkgItem item1, PkgItem item2) {
					int ordinal1 = item1.getMetaPackage().getPackageType().ordinal();
					int ordinal2 = item2.getMetaPackage().getPackageType().ordinal(); 
					int comparison1 =  ordinal1 - ordinal2;
					if (comparison1 != 0)
						return comparison1;
			    	String name1 = PackageAnalyser.getNameFromPath(item1.getMainPackage().getPath());
			    	String name2 = PackageAnalyser.getNameFromPath(item2.getMainPackage().getPath());
			    	int comparison2 = name1.compareTo(name2);
			    	// Use reverse lexical order of paths for same package types to get top down version ordering
					return comparison2 != 0 ? comparison2 : item2.getMainPackage().getPath().compareTo(item1.getMainPackage().getPath());
				}
				
			});
		}
	}
}
