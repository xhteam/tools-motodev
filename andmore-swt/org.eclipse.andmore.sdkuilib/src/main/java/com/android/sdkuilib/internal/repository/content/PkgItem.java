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
package com.android.sdkuilib.internal.repository.content;

import java.util.regex.Pattern;

import org.eclipse.andmore.sdktool.Utilities;

import com.android.SdkConstants;
import com.android.annotations.Nullable;
import com.android.repository.Revision;
import com.android.repository.api.RemotePackage;
import com.android.repository.api.RepoPackage;
import com.android.repository.api.UpdatablePackage;
import com.android.repository.impl.meta.Archive;
import com.android.repository.impl.meta.TypeDetails;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.repository.meta.DetailsTypes;
import com.android.sdkuilib.internal.repository.content.PackageAnalyser.PkgState;
import com.android.sdkuilib.internal.repository.ui.PackagesPage;
import com.android.sdkuilib.internal.repository.ui.PackagesPageIcons;

/**
 * A {@link PkgItem} represents one main {@link Package} combined with its state
 * and an optional update package.
 * <p/>
 * The main package is final and cannot change since it's what "defines" this PkgItem.
 * The state or update package can change later.
 */
public class PkgItem extends INode implements Comparable<PkgItem> {
    private final MetaPackage metaPackage;
    private PkgState state;
    private RepoPackage mainPackage;
    private UpdatablePackage updatePackage;

    /**
     * Create a new {@link PkgItem} for this main package.
     * The main package is final and cannot change since it's what "defines" this PkgItem.
     * The state or update package can change later.
     */
    public PkgItem(RepoPackage mainPkg, MetaPackage metaPackage, PkgState state) {
    	super();
    	this.mainPackage = mainPkg;
        this.metaPackage = metaPackage;
        this.state = state;
    }

    public boolean isObsolete() {
        return mainPackage.obsolete();
    }

    public UpdatablePackage getUpdatePkg() {
        return updatePackage;
    }

    public void setUpdatePkg(UpdatablePackage updatePkg) {
    	updatePackage = updatePkg;
    }
    
    public boolean hasUpdatePkg() {
        return updatePackage != null;
    }

    public String getName() {
        return mainPackage.getDisplayName();
    }

    public Revision getRevision() {
        return mainPackage.getVersion();
    }

    public MetaPackage getMetaPackage()
    {
    	return metaPackage;
    }
    
    public RepoPackage getMainPackage() {
        return mainPackage;
    }

    public PkgState getState() {
        return state;
    }

    @Nullable
    public AndroidVersion getAndroidVersion() {
        return getAndroidVersion(mainPackage);
    }

    public Archive[] getArchives() {
    	if (state == PkgState.NEW)
    		return new Archive[]{((RemotePackage)mainPackage).getArchive()};
        return new Archive[0];
    }

	/**
	 * Returns the image resource value for the label of the given element.  
	 * @param element Target
	 * @param columnIndex The index of the column being displayed
	 * @return the resource value of image used to label the element
	 */
    @Override
	public String getImage(Object element, int columnIndex) {
    	if (columnIndex == PackagesPage.NAME)
		    return metaPackage.getIconResource();
    	else if (columnIndex == PackagesPage.STATUS) {
            switch(state) {
            case INSTALLED:
                if (updatePackage != null) {
                    return PackagesPageIcons.ICON_PKG_UPDATE;
                } else {
                    return PackagesPageIcons.ICON_PKG_INSTALLED;
                }
            case NEW:
                return PackagesPageIcons.ICON_PKG_NEW;
            case DELETED:
            	return PackagesPageIcons.ICON_PKG_INCOMPAT;
            }
    	}
        return VOID;
	}


	/**
	 * Returns the text for the label of the given element.
	 * @param element Target
	 * @param columnIndex The index of the column being displayed
	 * @return the text string used to label the element, or VOID if there is no text label for the given object
	 */
    @Override
	public String getText(Object element, int columnIndex) {
    	switch (columnIndex)
    	{
    	case PackagesPage.NAME: 
    		return getPkgItemName();
    	case PackagesPage.API:  
    	{
    		AndroidVersion version = getAndroidVersion();
     		return version == null ? VOID : getAndroidVersion().getApiString();
    	}
    	case PackagesPage.REVISION: 
    		// Do not repeat version if included in name
    		if (Pattern.matches(".*\\d+\\.\\d+\\.\\d+.*", getPkgItemName()))
    			return VOID;
    		return mainPackage.getVersion().toString();
    	case PackagesPage.STATUS:   
    		return getStatusText();
    	default:
    	}
		return VOID;
	}

	/**
	 * Get the text displayed in the tool tip for given element 
	 * @param element Target
	 * @return the tooltop text, or VOID for no text to display
	 */
    @Override
	public String getToolTipText(Object element) {
        String s = getTooltipDescription(mainPackage);

        if ((updatePackage != null) && updatePackage.isUpdate()) {
            s += "\n-----------------" +        //$NON-NLS-1$
                 "\nUpdate Available:\n" +      //$NON-NLS-1$
                 getTooltipDescription(updatePackage.getRemote());
        }
        return s;
	}

    /**
     * Mark item as checked according to given criteria. Force uncheck if no criteria specified.
     * @param selectUpdates If true, select all update packages
     * @param topApiLevel If greater than 0, select platform packages of this api level
      */
    @Override
	public void checkSelections(
            boolean selectUpdates,
            int topApiLevel)
	{
		boolean hasUpdate = (state == PkgState.INSTALLED) && (updatePackage != null);
		if (selectUpdates  && hasUpdate) {
			    setChecked(true);
			    return;
		}
		if (topApiLevel > 0) {
			if (hasUpdate || // or new packages excluding system images and previews
					((state == PkgState.NEW) && (metaPackage.getPackageType() != PackageType.system_images))) {
    		    AndroidVersion version = getAndroidVersion();
    		    if ((version != null) && (version.getApiLevel() == topApiLevel) && !version.isPreview()) {
    			    setChecked(true);
    			    return;
    			}
			}
		} else {
	 	    setChecked(false);
		}
	}

    @Override
	public void markDeleted()
	{
		state = PkgState.DELETED;
		setChecked(false);
		updatePackage = null;
	}

    @Override
	public boolean isDeleted()
	{
		return state == PkgState.DELETED; 
	}

    /** Returns a string representation of this item, useful when debugging. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('<');

        if (isChecked) {
            sb.append(" * "); //$NON-NLS-1$
        }

        sb.append(state.toString());

        if (mainPackage != null) {
            sb.append(", pkg:"); //$NON-NLS-1$
            sb.append(mainPackage.toString());
        }

        if (updatePackage != null) {
            sb.append(", updated by:"); //$NON-NLS-1$
            sb.append(updatePackage.toString());
        }

        sb.append('>');
        return sb.toString();
    }

    @Override
    public int compareTo(PkgItem other) {
    	if (other == null)
    		return Integer.MIN_VALUE;
    	int comparison1 = state.ordinal() - other.getState().ordinal();
    	if (comparison1 != 0)
    		return comparison1;
    	if (hasUpdatePkg() && other.hasUpdatePkg())
    		return updatePackage.compareTo(other.getUpdatePkg());
        return mainPackage.compareTo(other.getMainPackage());
    }

    /**
     * Equality is defined as {@link #isSameItemAs(PkgItem)}: state, main package
     * and update package must be the similar.
     */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof PkgItem) && (compareTo((PkgItem) obj) == 0);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + state.hashCode();
        result = prime * result + mainPackage.hashCode();
        result = prime * result + ((updatePackage == null) ? 0 : updatePackage.hashCode());
        return result;
    }

    @Nullable
    public static AndroidVersion getAndroidVersion(RepoPackage repoPackage) {
        TypeDetails details = repoPackage.getTypeDetails();
        if (details instanceof DetailsTypes.ApiDetailsType) {
        	return ((DetailsTypes.ApiDetailsType)details).getAndroidVersion();
        }
        return null;
    }
    
    public static boolean isPreview(RepoPackage repoPackage) {
        TypeDetails details = repoPackage.getTypeDetails();
        if (details instanceof DetailsTypes.ApiDetailsType) {
        	return ((DetailsTypes.ApiDetailsType)details).getCodename() != null;
        }
        return false;
    }

    private String getStatusText() {
       switch(state) {
       case INSTALLED:
           if (updatePackage != null) {
               return String.format(
                       "Update available: rev. %1$s",
                       updatePackage.getRemote().getVersion().toString());
           }
           return "Installed";

       case NEW:
           if (((RemotePackage)mainPackage).getArchive().isCompatible()) {
               return "Not installed";
           } else {
               return String.format("Not compatible with %1$s",
                       SdkConstants.currentPlatformName());
           }
       case DELETED:
    	   return "Deleted";
       }
       return state.toString();
	}

    private String getTooltipDescription(RepoPackage repoPackage) {
    	String s = repoPackage.getDisplayName();
    	if (repoPackage instanceof RemotePackage) {
    		RemotePackage remote = (RemotePackage) repoPackage;
    		// For non-installed item get download size
    		long fileSize = remote.getArchive().getComplete().getSize();
    		s += '\n' + Utilities.formatFileSize(fileSize);
    		s += String.format("\nProvided by %1$s", remote.getSource().getUrl());
    	}
    	return s;
    }

    private String getPkgItemName() {
	    if (metaPackage.getPackageType() == PackageType.platforms)
	    	return "Platform SDK";
		return mainPackage.getDisplayName();
	}


}
