/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.andmore.internal.settings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.eclipse.andmore.android.multidex.MultiDexManager;
import org.eclipse.andmore.internal.editors.layout.refactoring.AdtProjectTest;
import org.eclipse.core.resources.IProject;
import org.junit.Test;

/**
 * Test case to see if enabling multi-dex is still working.
 */

public class MultiDexTest extends AdtProjectTest {
	
	@Test
	public void testEnableMultiDex() {
		IProject project = getProject();
		
		MultiDexManager.enableMultiDex(project, null);
		assertTrue(MultiDexManager.isMultiDexEnabled(project));
		
		// find out if the --main-dex-list file has been created. The content can be modified manually by the user and does not require validation.
		File mainDexListFile = MultiDexManager.getMainDexListFile(project);
		assertNotNull(mainDexListFile);
		assertTrue(mainDexListFile.exists());
	}
	
	@Test
	public void testDisableMultiDex() {
		IProject project = getProject();
		
		MultiDexManager.disableMultiDex(project);
		assertFalse(MultiDexManager.isMultiDexEnabled(project));
	}
}
