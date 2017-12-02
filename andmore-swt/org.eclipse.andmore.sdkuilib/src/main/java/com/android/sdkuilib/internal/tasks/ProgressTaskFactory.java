/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdkuilib.internal.tasks;

import com.android.sdkuilib.internal.repository.ITask;
import com.android.sdkuilib.internal.repository.ITaskFactory;

import org.eclipse.swt.widgets.Shell;

/**
 * An {@link ITaskFactory} that creates a new {@link ProgressTask} dialog
 * for each new task.
 */
public final class ProgressTaskFactory implements ITaskFactory {

    private final Shell mShell;

    public ProgressTaskFactory(Shell shell) {
        mShell = shell;
    }

    @Override
    public void start(String title, ITask task, Runnable onTerminateTask) {
        ProgressTask p = new ProgressTask(mShell, title);
        p.start(task);
    }
}
