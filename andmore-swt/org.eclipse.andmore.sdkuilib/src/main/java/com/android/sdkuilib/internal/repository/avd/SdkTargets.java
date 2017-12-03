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
package com.android.sdkuilib.internal.repository.avd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.andmore.sdktool.SdkContext;

import com.android.repository.api.ProgressIndicator;
import com.android.sdklib.AndroidTargetHash;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.repository.IdDisplay;
import com.android.sdklib.repository.meta.DetailsTypes;
import com.android.sdklib.repository.targets.AddonTarget;
import com.android.sdklib.repository.targets.PlatformTarget;
import com.android.sdklib.repository.targets.SystemImage;
import com.android.sdkuilib.internal.repository.content.PackageType;

/**
 * Container for Android targets and System images which relates them by Andriod version using
 * AndroidTargetHash .getPlatformHashString() following AVD Manager practice.
 * @author Andrew Bowley
 *
 * 16-11-2017
 */
public class SdkTargets {
    /** All available targets */
	private final List<IAndroidTarget> targets = new ArrayList<>();
	/** All available system images */
	private final Collection<SystemImage> sysImages;
	/** Maps system image to target by platform hash */
	private final Map<SystemImage,IAndroidTarget> targetMap = new HashMap<>();
	
	/**
	 * Construct SdkTargets object
	 * @param sdkContext The SDK context containing reference to AndroidSdkHandler object
	 */
	public SdkTargets(SdkContext sdkContext) {
		// Use AndroidTargetManager to get targets
		ProgressIndicator progress = sdkContext.getProgressIndicator();
        targets.addAll( 
        		sdkContext.getHandler().getAndroidTargetManager(progress)
                .getTargets(progress));
        Collections.sort(targets, new Comparator<IAndroidTarget>(){

			@Override
			public int compare(IAndroidTarget target1, IAndroidTarget target2) {
				return getApiLevel(target1) - getApiLevel(target2);
			}});
        // Use SystemImageManager to get system images
        sysImages =
        		sdkContext.getHandler().getSystemImageManager(progress).getImages();
        Iterator<SystemImage> iterator = sysImages.iterator();
        while(iterator.hasNext()) {
        	SystemImage systemImage = iterator.next();
        	IAndroidTarget target = mapTarget(systemImage);
        	if (target != null)
        		targetMap.put(systemImage, target);
        }
        	
	}

	/**
	 * Returns all available system images for given target
	 * @param target Android target
	 * @return sorted list of system images
	 */
	public List<SystemImage> getSystemImages(IAndroidTarget target) {
		List<SystemImage> systemImages = new ArrayList<>();
		for (SystemImage systemImage: sysImages) {
        	if (filterOnApi(systemImage, target))
        		systemImages.add(systemImage);
        }
        // Sort
        Collections.sort(systemImages, new Comparator<SystemImage>(){

			@Override
			public int compare(SystemImage sysImage1, SystemImage sysImage2) {
				return sysImage1.compareTo(sysImage2);
			}});
		return systemImages;
	}

	/**
	 * Returns map of system image to target by platform hash 
	 * @return map
	 */
	public Map<SystemImage,IAndroidTarget> getTargetMap()
	{
		return Collections.unmodifiableMap(targetMap);
	}

	/**
	 * Returns all available targets
	 * @return IAndroidTarget collection
	 * @ss {@link #getSystemImageTargets()}
	 */
	public Collection<IAndroidTarget> getTargets() {
		return targets;
	}

	/**
	 * Returns all targets with available system images
	 * @return IAndroidTarget collection
	 */
	public Collection<IAndroidTarget> getSystemImageTargets() {
		Set<IAndroidTarget> systemImageTargets = new TreeSet<>();
		for (SystemImage systemImage: sysImages) {
			for (IAndroidTarget target: targets) {
				if (filterOnApi(systemImage, target)) {
					systemImageTargets.add(target);
					break;
				}
			}
		}
		return systemImageTargets;
	}

	/**
	 * Returns all available system images
	 * @return SystemImage collection
	 */
	public Collection<SystemImage> getSysImages() {
		return sysImages;
	}

	/**
	 * Return target matching Android version of given system image
	 * @param systemImage SystemImage object
	 * @return IAndroidTarget object
	 */
	public IAndroidTarget getTargetForSysImage(SystemImage systemImage) {
		return targetMap.get(systemImage);
	}

	public IAndroidTarget getTargetForAndroidVersion(AndroidVersion androidVersion) {
		for (IAndroidTarget target: targetMap.values()) {
			if (filterOnApi(androidVersion, target))
				return target;
		}
		return null;
	}

	private IAndroidTarget mapTarget(SystemImage systemImage) {
		PackageType packageType = null;
		IdDisplay vendorId = IdDisplay.create("", "");
        DetailsTypes.ApiDetailsType details =
                (DetailsTypes.ApiDetailsType) systemImage.getPackage().getTypeDetails();
        if (details instanceof DetailsTypes.PlatformDetailsType) {
        	packageType = PackageType.platforms;
        } else if (details instanceof DetailsTypes.SysImgDetailsType) {
        	packageType = PackageType.system_images;
            vendorId = ((DetailsTypes.SysImgDetailsType) details).getVendor();
        } else if (details instanceof DetailsTypes.AddonDetailsType) {
        	packageType = PackageType.add_ons;
        	vendorId = ((DetailsTypes.AddonDetailsType) details).getVendor();
        }
		for (IAndroidTarget target: targets) 
		{
			if (filterOnApi(systemImage, target)) 
			{
		        if ((packageType == PackageType.add_ons) && 
		        	!target.isPlatform() &&
		        	target.getVendor().equals(vendorId.getId()))
		        	return target;
			    else if (target.isPlatform()) 
					return target;
			}
		}
		return null;
	}

	private boolean filterOnApi(SystemImage systemImage, IAndroidTarget target)
	{
		// AVD Manager, for each AVD, stores just platform version as hash, so this defines the system image/target association
		String imageApiHash = AndroidTargetHash.getPlatformHashString(systemImage.getAndroidVersion());
		String targetHash = AndroidTargetHash.getPlatformHashString(target.getVersion());
		return imageApiHash.equals(targetHash);
	}
	
	private boolean filterOnApi(AndroidVersion version, IAndroidTarget target)
	{
		// AVD Manager, for each AVD, stores just platform version as hash, so this defines the system image/target association
		String imageApiHash = AndroidTargetHash.getPlatformHashString(version);
		String targetHash = AndroidTargetHash.getPlatformHashString(target.getVersion());
		return imageApiHash.equals(targetHash);
	}
	
	private int getApiLevel(IAndroidTarget target) {
		if (target.isPlatform()) {
			PlatformTarget plaformTarget = (PlatformTarget)target;
			return plaformTarget.getVersion().getApiLevel();
		}
		AddonTarget addonTarget = (AddonTarget)target;
		return addonTarget.getParent().getVersion().getApiLevel();
	}

}
