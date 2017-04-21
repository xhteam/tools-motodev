/*
 * Copyright (C) 2012 The Android Open Source Project
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
package org.eclipse.andmore.android.db.core.junit;

import static org.junit.Assert.assertTrue;

import org.eclipse.andmore.android.db.core.exception.AndmoreDbException;
import org.eclipse.andmore.android.db.core.model.DbModel;
import org.eclipse.andmore.android.db.core.ui.TableNode;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.datatools.modelbase.sql.tables.Table;
import org.junit.Test;

public class TableNodeTest {

	@Test
	public void test() {

		Path path = new Path("/Users/danielbfranco/temp/ranking.db");
		DbModel model = null;
		try {
			model = new DbModel(path);
		} catch (AndmoreDbException e) {
			e.printStackTrace();
		}
		IStatus s = model.connect();
		assertTrue(s.getCode() == IStatus.OK);

		Table table = model.getTable("mablinhos3");

		TableNode node = new TableNode(table, model, null);
		node.browseTableContents();

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
}
