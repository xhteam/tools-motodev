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
package org.eclipse.andmore.android.db.core.exception;

import org.eclipse.andmore.android.common.exception.AndroidException;

public class AndmoreDbException extends AndroidException {

	private static final long serialVersionUID = 1148147648131562077L;

	/**
	 * Creates a new AndmoreDbException object.
	 */
	public AndmoreDbException() {

	}

	/**
	 * Creates a new AndmoreDbException object.
	 * 
	 * @param message
	 *            the message used by the Exception.
	 */
	public AndmoreDbException(String message) {
		super(message);
	}

	/**
	 * Creates a new AndmoreDbException object.
	 * 
	 * @param cause
	 *            the associated cause.
	 */
	public AndmoreDbException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new AndmoreDbException object.
	 * 
	 * @param message
	 *            the message used by the Exception.
	 * @param cause
	 *            the associated cause.
	 */
	public AndmoreDbException(String message, Throwable cause) {
		super(message, cause);
	}

}
