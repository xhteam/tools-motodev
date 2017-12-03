/*
 * Copyright (C) 2009-2017 The Android Open Source Project
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
package com.android.sdkuilib.internal.repository;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.repository.api.LocalPackage;
import com.android.repository.api.RemotePackage;

/**
 * Represents package that we want to install.
 * <p/>
 * A new package is a remote package that needs to be downloaded and then
 * installed. It can replace an existing local one. It can also depend on another
 * (new or local) package, which means the dependent package needs to be successfully
 * installed first. Finally this package can also be a dependency for another one.
 * <p/>
 * The accepted and rejected flags are used by {@code SdkUpdaterChooserDialog} to follow
 * user choices. The installer should never install something that is not accepted.
 * <p/>
 * <em>Note</em>: There is currently no logic to support more than one level of
 * dependency, either here or in the {@code SdkUpdaterChooserDialog}, since we currently
 * have no need for it.
 */
public class PackageInfo implements Comparable<PackageInfo> {

    private final RemotePackage mNewPackage;
    private LocalPackage mReplaced;
    private boolean mAccepted;
    private boolean mRejected;

    /**
     * Creates a new replacement where the {@code newPackage} will replace the
     * currently installed {@code replaced} package.
     * When {@code newPackage} is not intended to replace anything (e.g. because
     * the user is installing a new package not present on her system yet), then
     * {@code replace} shall be null.
     *
     * @param newPackage A "new package" to be installed. This is always an package
     *          that comes from a remote site.
     */
    public PackageInfo(@NonNull RemotePackage newPackage) {
    	 this(newPackage, null);
    }

    public PackageInfo(@NonNull RemotePackage newPackage, @Nullable LocalPackage replaced) {
    	mNewPackage = newPackage;
   	 	mReplaced = replaced;
    }
    
    /**
     * Returns the "new archive" to be installed.
     * This <em>may</em> be null for missing archives.
     */
    public RemotePackage getNewPackage() {
        return mNewPackage;
    }

    /**
     * Returns an optional local archive that the new one will replace.
     * Can be null if this archive does not replace anything.
     */
    public LocalPackage getReplaced() {
        return mReplaced;
    }

    /**
     * Sets whether this package was accepted (either manually by the user or
     * automatically if it doesn't have a license) for installation.
     */
    public void setAccepted(boolean accepted) {
        mAccepted = accepted;
    }

    /**
     * Returns whether this package was accepted (either manually by the user or
     * automatically if it doesn't have a license) for installation.
     */
    public boolean isAccepted() {
        return mAccepted;
    }

    /**
     * Sets whether this package was rejected manually by the user.
     * An package can neither accepted nor rejected.
     */
    public void setRejected(boolean rejected) {
        mRejected = rejected;
    }

    /**
     * Returns whether this package was rejected manually by the user.
     * An package can neither accepted nor rejected.
     */
    public boolean isRejected() {
        return mRejected;
    }

    /**
     * PackageInfos are compared using ther "new package" ordering.
     *
     * @see Package#compareTo(Package)
     */
    @Override
    public int compareTo(PackageInfo rhs) {
        if (mNewPackage != null && rhs != null) {
            return mNewPackage.compareTo(rhs.mNewPackage);
        }
        return 0;
    }
}
