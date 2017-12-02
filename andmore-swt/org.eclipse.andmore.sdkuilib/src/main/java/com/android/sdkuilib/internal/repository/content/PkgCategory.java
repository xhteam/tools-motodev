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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.android.sdkuilib.internal.repository.content.PackageAnalyser.PkgState;
import com.android.sdkuilib.internal.repository.ui.PackagesPage;

/**
 * PkgCategory represents the first level of the package tree
 * @author Andrew Bowley
 *
 * 10-11-2017
 */
public abstract class PkgCategory<K> extends INode {
	protected final CategoryKeyType keyType;
	protected final K keyValue;
	protected final String imageReference;
	protected final List<PkgItem> packageList = new ArrayList<PkgItem>();
	protected final Map<String, PkgItem> productMap = new TreeMap<>();
	protected String label;
	protected boolean selectAllPackages = false;

    public PkgCategory(CategoryKeyType keyType, String label, String imageReference) {
    	this(keyType, null, label, imageReference);
    }
    
    public PkgCategory(CategoryKeyType keyType, K keyValue, String label, String imageReference) {
    	super();
    	this.keyType = keyType;
    	this.keyValue = keyValue;
    	this.label = label;
    	this.imageReference = imageReference;
    }

    public CategoryKeyType getKeyType()
    {
    	return keyType;
    }
    
    public K getKeyValue() {
        return keyValue;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setSelectAllPackages(boolean selectAllPackages) {
		this.selectAllPackages = selectAllPackages;
	}

	public List<PkgItem> getItems() {
        return packageList;
    }

    public void clearProducts() {
    	productMap.clear();
    }
    
    public void putProduct(String product, PkgItem item) {
    	productMap.put(product, item);
    }

    public PkgItem getProduct(String product) {
    	return productMap.get(product);
    }
    
	/**
	 * Returns the text for the label of the given element.
	 * @param element Target
	 * @param columnIndex The index of the column being displayed
	 * @return the text string used to label the element
	 */
    @Override
	public String getText(Object element, int columnIndex) {
		return columnIndex == PackagesPage.NAME ? getLabel() : VOID;
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
		    return imageReference;
        return VOID;
	}

	/**
	 * Returns list of descendents
	 * @return INode list
	 */
    @Override
	public List<? extends INode> getChildren() {
    	if (!selectAllPackages) {
    		String product = null;
    		List<PkgItem> filteredPackageList = new ArrayList<PkgItem>();
    		Iterator<PkgItem> iterator = packageList.iterator();
    		while (iterator.hasNext()) {
    			PkgItem packageItem = iterator.next();
    			if ((packageItem.getState() != PkgState.NEW) || 
    				 !packageItem.getProduct().equals(product)) {
    				product = packageItem.getProduct();
    				filteredPackageList.add(packageItem);
    			}
    		}
			return filteredPackageList;
    	}
        return packageList;
	}

    @Override
    public String toString() {
        return String.format("%s <key=%s, label=%s, #items=%d>",
                this.getClass().getSimpleName(),
                keyValue == null ? keyType.toString() : keyValue.toString(),
                label,
                packageList.size());
    }

    /** {@link PkgCategory}s are equal if their internal keys are equal. */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((keyValue == null) ? keyType.hashCode() : keyValue.hashCode());
        return result;
    }

    /** {@link PkgCategory}s are equal if their internal keys are equal. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        PkgCategory<?> other = (PkgCategory<?>) obj;
        if (keyType != other.keyType)
        	return false;
        if (keyValue == null) {
            if (other.keyValue != null) return false;
        } else if (!keyValue.equals(other.keyValue)) return false;
        return true;
    }
}
