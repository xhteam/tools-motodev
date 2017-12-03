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
package org.eclipse.andmore.base.resources;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.utils.ILogger;

public class JFaceImageLoader implements ImageFactory {

    /** Image file location when using {@link #getImageByName(String)} */
    public static String ICONS_PATH = "icons/";
    
    protected Bundle bundle;
    protected PluginResourceProvider provider;
    protected final Map<String, ImageDescriptor> filterMap = new HashMap<>();
    protected ResourceManager resourceManager;
    protected ILogger logger;
	
	/**
	 * Loads bundle images 
	 * Images are loaded using a path relative to the bundle location.
	 *
	 * Instances are mangaged by a JFace resource manager, and thus should never be disposed by the image consumer.
	 *
	 */

	/**
     * Construct an ImageLoader object using given UI plugin instance.
     * This object provides imageDescriptorFromPlugin() method
	 * @param bundle
	 */
	public JFaceImageLoader(@NonNull PluginResourceProvider provider)
	{
		this.provider = provider;
		createResourceManager();
	}

	/**
     * Construct an ImageLoader object using given bundle instance 
	 * @param bundle
	 */
	public JFaceImageLoader(@NonNull Bundle bundle)
	{
		this.bundle = bundle;
		createResourceManager();
	}

    /**
     * Construct an ImageLoader object using given class of plugin associated with the bundle
     * @param bundleClass
     */
	public JFaceImageLoader(Class<?> bundleClass)
	{
		this(FrameworkUtil.getBundle(bundleClass));
	}

	public void setLogger(ILogger logger) {
		this.logger = logger;
	}
	
   /* (non-Javadoc)
    * @see org.eclipse.andmore.base.resources.ImageFactory#getImageByName(java.lang.String)
    */
    @Override
	@Nullable
    public Image getImageByName(String imageName) {
        return getImage(ICONS_PATH + imageName);
    }


    /* (non-Javadoc)
	 * @see org.eclipse.andmore.base.resources.ImageFactory#getImageByName(java.lang.String, java.lang.String, org.eclipse.andmore.base.resources.JFaceImageLoader.ImageEditor)
	 */
    @Override
	@Nullable
    public Image getImageByName(String imageName,
                                 String keyName,
                                 ImageEditor imageEditor) {
        if (imageEditor == null) // No imageEditor means just load image. The keyName is irrelevant.
            return getImageByName(imageName);                                    
    	String imagePath = ICONS_PATH + imageName;
    	Image image = null;
    	ImageDescriptor imageDescriptor = descriptorFromPath(imagePath);
     	if (imageDescriptor != null) {
     		ImageDescriptor imagefilterDescriptor = filterMap.get(keyName);
     		if (imagefilterDescriptor == null) {
     			// Assume filter input = output
 				imagefilterDescriptor = imageDescriptor;
         		image = resourceManager.createImage(imageDescriptor);
     			ImageData imageData = imageEditor.edit(image);
     			if (imageData !=  null) {
     				// Create new image from data
     				imagefilterDescriptor = ImageDescriptor.createFromImageData(imageData);
     	     		image = resourceManager.createImage(imagefilterDescriptor);
     			}
     			filterMap.put(keyName, imagefilterDescriptor);
     		}
     		else
         		image = resourceManager.createImage(imagefilterDescriptor);
     	}
    	return image;
    }

    /* (non-Javadoc)
	 * @see org.eclipse.andmore.base.resources.ImageFactory#getImage(java.lang.String)
	 */
    @Override
	public Image getImage(String imagePath) {
    	Image image = null;
    	ImageDescriptor imageDescriptor = descriptorFromPath(imagePath);
     	if (imageDescriptor != null)  {
    		image = resourceManager.createImage(imageDescriptor);
    		if ((image == null) && (logger != null))
    			logger.error(null, "Image creation failed for image path " + imagePath);
     	}
        return image;
    }

     /* (non-Javadoc)
	 * @see org.eclipse.andmore.base.resources.ImageFactory#dispose()
	 */
    @Override
	public void dispose() {
        // Garbage collect system resources
        if (resourceManager != null) 
        {
            resourceManager.dispose();
            resourceManager = null;
        }
    }
    
	@Override
	@Nullable
	public Image getImageByName(String imageName, ReplacementImager replacementImager) {
    	String imagePath = ICONS_PATH + imageName;
    	ImageDescriptor imageDescriptor = descriptorFromPath(imagePath);
     	if (imageDescriptor == null) {
			ImageDescriptor replacementImageDescriptor = filterMap.get(imagePath);
			if (replacementImageDescriptor == null) {
				replacementImageDescriptor = ImageDescriptor.createFromImageData(replacementImager.create());
 			    filterMap.put(imagePath, replacementImageDescriptor);
			}
			return resourceManager.createImage(replacementImageDescriptor);
		}
		return resourceManager.createImage(imageDescriptor);
	}

	@Override
	@Nullable
	public ImageDescriptor getDescriptorByName(String imageName) {
    	String imagePath = ICONS_PATH + imageName;
    	return descriptorFromPath(imagePath);
	}

	protected ImageDescriptor descriptorFromPath(String imagePath) {
		if (provider != null) {
			ImageDescriptor descriptor = provider.descriptorFromPath(imagePath);
			if ((logger != null) && (descriptor == null))
	    		logger.error(null, "Image descriptor null for image path: " +imagePath);
			return descriptor;
		}
    	ImageDescriptor imageDescriptor = null;
        // An image descriptor is an object that knows how to create an SWT image.
    	URL url = FileLocator.find(bundle, new Path(imagePath), null);
    	if (url != null) {
    		imageDescriptor = ImageDescriptor.createFromURL(url);
    		if (logger != null) {
    		    if (imageDescriptor != null)
    		    	logger.info("Image file found at " + url.toString());
    		    else
    	    		logger.error(null, "Image descriptor null for URL: " + url.toString());
    		    }
    	}
    	else if (logger != null)
    		logger.error(null, "Image path not found: " + imagePath);
        return imageDescriptor;
    }
    
    /**
     * Returns local Resource Manager
     * @return ResourceManager object
     */
    protected void createResourceManager() {
        if (resourceManager == null)
            Display.getDefault().syncExec(new Runnable() {
                
                @Override
                public void run() 
                {
                    // getResources() returns the ResourceManager for the current display. 
                    // May only be called from a UI thread.
                    resourceManager = new LocalResourceManager(JFaceResources.getResources());
                }
            });
     }



}
