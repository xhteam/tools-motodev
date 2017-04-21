/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.eclipse.andmore.common.resources.platform;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.andmore.integration.tests.AdtTestData;
import org.eclipse.andmore.mock.TestLogger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


public class AttrsXmlParserManifestTest {

    private AttrsXmlParser mParser;
    private String mFilePath;

    private static final String MOCK_DATA_PATH =
        "org/eclipse/andmore/testdata/mock_manifest_attrs.xml"; //$NON-NLS-1$

    @Before
    public void setUp() throws Exception {
    	
        mFilePath = AdtTestData.getInstance().getTestFilePath(MOCK_DATA_PATH);
        mParser = new AttrsXmlParser(mFilePath, new TestLogger(), 100);
    }

    @Test
    public void testGetOsAttrsXmlPath() throws Exception {
        assertEquals(mFilePath, mParser.getOsAttrsXmlPath());
    }

    private Map<String, DeclareStyleableInfo> preloadAndGetStyleables() {
        assertSame(mParser, mParser.preload());

        Map<String, DeclareStyleableInfo> styleableList = mParser.getDeclareStyleableList();
        // For testing purposes, we want the strings sorted
        if (!(styleableList instanceof TreeMap<?, ?>)) {
            styleableList = new TreeMap<String, DeclareStyleableInfo>(styleableList);
        }
        return styleableList;
    }

    @Test
    public void testPreload() throws Exception {
        Map<String, DeclareStyleableInfo> styleableList = preloadAndGetStyleables();

        assertEquals(
                "[AndroidManifest, " +
                "AndroidManifestActivityAlias, " +
                "AndroidManifestApplication, " +
                "AndroidManifestNewElement, " +
                "AndroidManifestNewParent, " +
                "AndroidManifestPermission" +
                "]",
                Arrays.toString(styleableList.keySet().toArray()));
    }

    /**
     * Tests that AndroidManifestNewParentNewElement got renamed to AndroidManifestNewElement
     * and a parent named AndroidManifestNewParent was automatically created.
     */
    @Test
    public void testNewParent() throws Exception {
        Map<String, DeclareStyleableInfo> styleableList = preloadAndGetStyleables();

        DeclareStyleableInfo newElement = styleableList.get("AndroidManifestNewElement");
        assertNotNull(newElement);
        assertEquals("AndroidManifestNewElement", newElement.getStyleName());
        assertEquals("[AndroidManifestNewParent]",
                     Arrays.toString(newElement.getParents()));

        DeclareStyleableInfo newParent = styleableList.get("AndroidManifestNewParent");
        assertNotNull(newParent);
        assertEquals("[AndroidManifest]",
                     Arrays.toString(newParent.getParents()));

    }
}
