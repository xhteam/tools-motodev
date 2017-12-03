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
package com.android.sdkuilib.internal.repository.content;

import java.util.List;

import org.eclipse.andmore.sdktool.SdkContext;
import org.eclipse.swt.graphics.Font;

import com.android.sdklib.AndroidVersion;
import org.eclipse.andmore.base.resources.ImageFactory;

public class PkgCellAgent {

	/** Column identities */
	public static final int NAME = 1;
	public static final int API = 2;
	public static final int REVISION = 3;
	public static final int STATUS = 4;

	private final Font mTreeFontItalic;
	private final ImageFactory mImgFactory;
	private final List<PkgCategory<AndroidVersion>> mCategoryList;
	
	public PkgCellAgent(SdkContext sdkContext, PackageAnalyser packageAnalyser, Font treeFontItalic) {
		this.mTreeFontItalic = treeFontItalic;
		this.mImgFactory = sdkContext.getSdkHelper().getImageFactory();
		this.mCategoryList = packageAnalyser.getApiCategories();
	}

	public Font getTreeFontItalic()
	{
		return mTreeFontItalic;
	}

	public ImageFactory getImgFactory() {
		return mImgFactory;
	}

	public List<PkgCategory<AndroidVersion>> getCategoryList() {
		return mCategoryList;
	}

}
