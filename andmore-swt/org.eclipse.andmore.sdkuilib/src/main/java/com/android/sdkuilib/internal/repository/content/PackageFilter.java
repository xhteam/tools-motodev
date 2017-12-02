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
package com.android.sdkuilib.internal.repository.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.android.sdklib.AndroidVersion;

/**
 * Provides functions to select categories and and package items 
 * based on user provided set of package types
 * @author Andrew Bowley
 *
 * 24-11-2017
 */
public class PackageFilter {

	public static Set<PackageType> EMPTY_PACKAGE_TYPE_SET;
	public static final PackageType[] GENERIC_PACKAGE_TYPES; 
	
	static 
	{
		EMPTY_PACKAGE_TYPE_SET = Collections.emptySet();
		GENERIC_PACKAGE_TYPES = new PackageType[] {
				PackageType.emulator,
				PackageType.cmake,
				PackageType.docs,
				PackageType.lldb,
				PackageType.ndk_bundle,
				PackageType.patcher,
				PackageType.generic,
				PackageType.samples
		};
	}
	
	/** Set of package types on which to filter. An empty set indicates no filtering */
	private Set<PackageType> packageTypeSet;
	private boolean selectTools;
	private boolean selectApi;
	private boolean selectExtra;
	private boolean selectGeneric;
	
	/**
	 * Default constructor
	 */
	public PackageFilter() {
		packageTypeSet = EMPTY_PACKAGE_TYPE_SET; 
	}

	/**
	 * Construct PackageFilter object with given selection set
	 */
	public PackageFilter(Set<PackageType> packageTypes) {
		if ((packageTypes == null) || (packageTypes.isEmpty()))
			packageTypeSet = EMPTY_PACKAGE_TYPE_SET; 
		else {
			packageTypeSet = new TreeSet<>();
			packageTypeSet.addAll(packageTypes);
			initialize();
		}
	}

	public void setPackageTypes(Set<PackageType> packageTypeSet) {
		if (this.packageTypeSet.isEmpty())
			this.packageTypeSet = new TreeSet<>();
		this.packageTypeSet.clear();
		this.packageTypeSet.addAll(packageTypeSet);
		initialize();
	}

	public Set<PackageType> getPackageTypes() {
		return Collections.unmodifiableSet(packageTypeSet);
	}

	public boolean isFilterOn() {
		return !packageTypeSet.isEmpty();
	}

	public List<PkgCategory<AndroidVersion>> getFilteredApiCategories(List<PkgCategory<AndroidVersion>> cats) {
		if (!isFilterOn())
			return cats;
		List<PkgCategory<AndroidVersion>> selectCategories = new ArrayList<>();
		for (PkgCategory<AndroidVersion> cat: cats) {
			CategoryKeyType catKeyType = cat.getKeyType();
			switch(catKeyType) {
			case TOOLS:
			case TOOLS_PREVIEW: 
				if (selectTools)
					selectCategories.add(cat);
				break;
			case API: 
				if (selectApi)
					selectCategories.add(cat);
				break;
			case EXTRA: 
				if (selectExtra)
					selectCategories.add(cat);
				break;
			case GENERIC: 
				if (selectGeneric)
				    selectCategories.add(cat);
				break;
			default:
				break;
			}
		}
		return selectCategories;
	}

	public List<? extends INode> getFilteredItems(List<? extends INode> items)
	{
		if (!isFilterOn() || ((items == null) || items.isEmpty() || !(items.get(0) instanceof PkgItem)))
			return items;
		List<PkgItem> selectItems = new ArrayList<>();
		for (INode node: items) {
			PkgItem packageItem = (PkgItem)node;
			PackageType packageType = packageItem.getMetaPackage().getPackageType();
			if (packageTypeSet.contains(packageType))
				selectItems.add(packageItem);
			if (selectGeneric) {
				for (int i = 0; i < GENERIC_PACKAGE_TYPES.length; ++i)
					if (GENERIC_PACKAGE_TYPES[i] == packageType) {
						selectItems.add(packageItem);
						break;
					}
			}
		}
		return selectItems;
		
	}
	
	private void initialize()
	{
		if (!isFilterOn())
			return;
		selectTools = selectApi = selectExtra = selectGeneric = false;
		selectTools = 
				packageTypeSet.contains(PackageType.build_tools) ||
				packageTypeSet.contains(PackageType.platform_tools) ||
				packageTypeSet.contains(PackageType.tools);
		selectApi =
				packageTypeSet.contains(PackageType.platforms) ||
				packageTypeSet.contains(PackageType.add_ons) ||
				packageTypeSet.contains(PackageType.system_images) ||
		        packageTypeSet.contains(PackageType.sources);
		selectExtra = packageTypeSet.contains(PackageType.extras);
		
		for (int i = 0; i < GENERIC_PACKAGE_TYPES.length; ++i)
			if (packageTypeSet.contains(GENERIC_PACKAGE_TYPES[i])) {
				selectGeneric = true;
				break;
			}
	}

}
