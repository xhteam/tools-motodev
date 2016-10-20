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
package org.eclipse.andmore.internal.editors.layout.configuration;

import com.android.ide.common.resources.configuration.LocaleQualifier;

import junit.framework.TestCase;

@SuppressWarnings("javadoc")
public class LocaleTest extends TestCase {
    public void test() {
    	
        String language1 = "nb";
        String language2 = "no";
        String region1 = "NO";
        String region2 = "SE";

        assertEquals(Locale.ANY, Locale.ANY);
        assertFalse(Locale.ANY.hasLanguage());
        assertFalse(Locale.ANY.hasRegion());
        assertFalse(Locale.create(new LocaleQualifier(LocaleQualifier.FAKE_VALUE)).hasLanguage());
        assertFalse(Locale.create(new LocaleQualifier(LocaleQualifier.FAKE_VALUE)).hasRegion());

        assertEquals(Locale.create(new LocaleQualifier(language1)), Locale.create(new LocaleQualifier(language1)));
        assertTrue(Locale.create(new LocaleQualifier(language1)).hasLanguage());
        assertFalse(Locale.create(new LocaleQualifier(language1)).hasRegion());
        assertTrue(Locale.create(new LocaleQualifier(null, language1, region1, null)).hasLanguage());
        assertTrue(Locale.create(new LocaleQualifier(null, language1, region1, null)).hasRegion());

        assertEquals(Locale.create(new LocaleQualifier(null, language1, region1, null)), Locale.create(new LocaleQualifier(null, language1, region1, null)));
        assertEquals(Locale.create(new LocaleQualifier(language1)), Locale.create(new LocaleQualifier(language1)));
        assertTrue(Locale.create(new LocaleQualifier(language1)).equals(Locale.create(new LocaleQualifier(language1))));
        assertTrue(Locale.create(new LocaleQualifier(null, language1, region1, null)).equals(Locale.create(new LocaleQualifier(null, language1, region1, null))));
        assertFalse(Locale.create(new LocaleQualifier(null, language1, region1, null)).equals(Locale.create(new LocaleQualifier(null, language1, region2, null))));
        assertFalse(Locale.create(new LocaleQualifier(language1)).equals(Locale.create(new LocaleQualifier(null, language1, region1, null))));
        assertFalse(Locale.create(new LocaleQualifier(language1)).equals(Locale.create(new LocaleQualifier(language2))));
        assertFalse(Locale.create(new LocaleQualifier(null, language1, region1, null)).equals(Locale.create(new LocaleQualifier(null, language2, region1, null))));
        assertEquals("nb", Locale.create(new LocaleQualifier(language1)).toString());
        assertEquals("nb-rNO", Locale.create(new LocaleQualifier(null, language1, region1, null)).toString());
    }
}
