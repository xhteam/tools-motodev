package com.android.sdkuilib.internal.repository.content;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IInputProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.android.repository.api.RemotePackage;
import com.android.repository.api.UpdatablePackage;
import com.android.repository.impl.meta.Archive;
import com.android.sdklib.AndroidVersion;

public class PackageContentProvider implements ITreeContentProvider {

    private final IInputProvider mViewer;
    private final PackageFilter mPackageFilter;
    private boolean mDisplayArchives;

    public PackageContentProvider(IInputProvider viewer, PackageFilter packageFilter)
	{
        this.mViewer = viewer;
        this.mPackageFilter = packageFilter;
    }

    public void setDisplayArchives(boolean displayArchives) {
        mDisplayArchives = displayArchives;
    }


    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof ArrayList<?>) {
            return ((ArrayList<?>) parentElement).toArray();

        } else if (parentElement instanceof PkgCategory) {
        	@SuppressWarnings("unchecked")
			PkgCategory<AndroidVersion> pkgCategory = (PkgCategory<AndroidVersion>)parentElement;
        	if (mPackageFilter.isFilterOn())
        		return mPackageFilter.getFilteredItems(pkgCategory.getChildren()).toArray();
            return pkgCategory.getChildren().toArray();

        } else if (parentElement instanceof PkgItem) {
            if (mDisplayArchives) {

                UpdatablePackage pkg = ((PkgItem) parentElement).getUpdatePkg();

                // Display update packages as sub-items if the details mode is activated.
                if (pkg != null) {
                    return new Object[] { pkg };
                }

                return ((PkgItem) parentElement).getArchives();
            }

        } else if (parentElement instanceof RemotePackage) {
            if (mDisplayArchives) {
                return new Archive[]{((RemotePackage) parentElement).getArchive()};
            }

        }

        return new Object[0];
    }

    @Override
    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object getParent(Object element) {
        // This operation is expensive, so we do the minimum
        // and don't try to cover all cases.

        if (element instanceof PkgItem) {
            Object input = mViewer.getInput();
            if (input != null) {
                for (PkgCategory<?> cat : (List<PkgCategory<?>>) input) {
                    if (cat.getItems().contains(element)) {
                        return cat;
                    }
                }
            }
        }

        return null;
    }


    @Override
    public boolean hasChildren(Object parentElement) {
        if (parentElement instanceof ArrayList<?>) {
            return true;

        } else if (parentElement instanceof PkgCategory) {
            return true;

        } else if (parentElement instanceof PkgItem) {
            if (mDisplayArchives) {
                UpdatablePackage pkg = ((PkgItem) parentElement).getUpdatePkg();

                // Display update packages as sub-items if the details mode is activated.
                if (pkg != null) {
                    return true;
                }

                Archive[] archives = ((PkgItem) parentElement).getArchives();
                return archives.length > 0;
            }
        } else if (parentElement instanceof RemotePackage) {
            if (mDisplayArchives) {
                return ((RemotePackage) parentElement).getArchive() != null;
            }
        }

        return false;
    }

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
	}

}
