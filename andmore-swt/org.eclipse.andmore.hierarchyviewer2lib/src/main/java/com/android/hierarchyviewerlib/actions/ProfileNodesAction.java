/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.hierarchyviewerlib.actions;

import com.android.hierarchyviewerlib.HierarchyViewerDirector;

import org.eclipse.andmore.base.resources.ImageFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

public class ProfileNodesAction extends SelectedNodeEnabledAction implements ImageAction {
    private static ProfileNodesAction sAction;

    private Image mImage;

    public ProfileNodesAction() {
        super("Profile Node");
        ImageFactory imageFactory = HierarchyViewerDirector.getDirector().getImageFactory();
        mImage = imageFactory.getImageByName("profile.png"); //$NON-NLS-1$
        setImageDescriptor(ImageDescriptor.createFromImage(mImage));
        setToolTipText("Obtain layout times for tree rooted at selected node");
    }

    public static ProfileNodesAction getAction() {
        if (sAction == null) {
            sAction = new ProfileNodesAction();
        }
        return sAction;
    }

    @Override
    public void run() {
        HierarchyViewerDirector.getDirector().profileCurrentNode();
    }

    @Override
    public Image getImage() {
        return mImage;
    }
}
