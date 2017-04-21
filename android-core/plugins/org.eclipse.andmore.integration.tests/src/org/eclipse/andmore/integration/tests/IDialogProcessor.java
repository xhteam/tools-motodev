/*******************************************************************************
 * Copyright (c) 2006-2010 eBay Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *******************************************************************************/

// copied from Ebay Turmeric Plugin project and used for monitoring dialogs to
// be dismissed.
package org.eclipse.andmore.integration.tests;

public interface IDialogProcessor {

	// This method is called to process the found dialog.
	// The dialog is passed in as an Object because eclipse does not have
	// one root dialog.
	public void processDialog(Object dialog);
}