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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.andmore.sdktool.SdkContext;

import com.android.repository.api.ProgressIndicator;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.repository.IdDisplay;
import com.android.sdklib.repository.meta.DetailsTypes;
import com.android.sdklib.repository.targets.AddonTarget;
import com.android.sdklib.repository.targets.PlatformTarget;
import com.android.sdklib.repository.targets.SystemImage;
import com.android.sdkuilib.internal.repository.content.PackageType;

/**
 * @author Andrew Bowley
 *
 * 16-11-2017
 */
public class SdkTargets {

	private final List<IAndroidTarget> targets = new ArrayList<>();
	private final Collection<SystemImage> sysImages;
	private final Map<SystemImage,IAndroidTarget> targetMap = new HashMap<>();
	
	/**
	 * 
	 */
	public SdkTargets(SdkContext sdkContext) {
		ProgressIndicator progress = sdkContext.getProgressIndicator();
        targets.addAll( 
        		sdkContext.getHandler().getAndroidTargetManager(progress)
                .getTargets(progress));
        Collections.sort(targets, new Comparator<IAndroidTarget>(){

			@Override
			public int compare(IAndroidTarget target1, IAndroidTarget target2) {
				return getApiLevel(target1) - getApiLevel(target2);
			}});
        
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

	public List<SystemImage> getSystemImages(IAndroidTarget target) {
		List<SystemImage> systemImages = new ArrayList<>();
    	Set<Map.Entry<SystemImage,IAndroidTarget>> targetEntries = targetMap.entrySet();
        Iterator<Map.Entry<SystemImage,IAndroidTarget>> iterator = targetEntries.iterator();
        while(iterator.hasNext()) {
        	Entry<SystemImage, IAndroidTarget> entry = iterator.next();
        	if (entry.getValue().canRunOn(target))
        		systemImages.add(entry.getKey());
        }
        // Sort
        Collections.sort(systemImages, new Comparator<SystemImage>(){

			@Override
			public int compare(SystemImage sysImage1, SystemImage sysImage2) {
				return sysImage1.compareTo(sysImage2);
			}});
        // Remove duplicates which contain same abitype details
        // Prior sort ensures predictable results
        Set<String> abiTypeSet = new HashSet<>();
        Iterator<SystemImage> imagesIterator = systemImages.iterator();
        while (imagesIterator.hasNext()) {
        	SystemImage systemImage = imagesIterator.next();
        	String key = AvdInfo.getPrettyAbiType(systemImage);
            if (abiTypeSet.contains(key))
            	imagesIterator.remove();
            else
            	abiTypeSet.add(key);
        }
		return systemImages;
	}
	
	public Map<SystemImage,IAndroidTarget> getTargetMap()
	{
		return Collections.unmodifiableMap(targetMap);
	}
	
	public Collection<IAndroidTarget> getTargets() {
		return targets;
	}

	public Collection<SystemImage> getSysImages() {
		return sysImages;
	}
	
	public IAndroidTarget getTargetForSysImage(SystemImage systemImage) {
		return targetMap.get(systemImage);
	}

	IAndroidTarget mapTarget(SystemImage systemImage) {
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
		int imageApi = systemImage.getAndroidVersion().getApiLevel();
		int targetApi = getApiLevel(target);
		return imageApi <=  targetApi;
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
