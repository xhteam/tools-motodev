/**
 * 
 */
package org.eclipse.andmore.sdktool;

import org.eclipse.andmore.base.resources.IEditorIconFactory;
import org.eclipse.andmore.base.resources.ImageFactory;
import org.eclipse.andmore.base.resources.JFaceImageLoader;
import org.eclipse.andmore.base.resources.PluginResourceProvider;
import org.eclipse.swt.graphics.Image;

import com.android.repository.api.RepoManager;
import com.android.sdklib.repository.AndroidSdkHandler;
import com.android.utils.ILogger;

/**
 * @author andrew
 *
 */
public class SdkCallAgent {
	public static final int NO_TOOLS_MSG = 0;
	public static final int TOOLS_MSG_UPDATED_FROM_ADT = 1;
	public static final int TOOLS_MSG_UPDATED_FROM_SDKMAN = 2;

	private final SdkContext sdkContext;
	private final ILogger consoleLogger;
	private IEditorIconFactory iconEditorFactory;

	/**
	 * Construct SdkCallAgent object to mediate between application and UI layer
     * @param sdkHandler SDK handler
     * @param repoManager Repository manager
     * @param consoleLogger Console logger to persist all messages
	 */
	public SdkCallAgent(
            AndroidSdkHandler sdkHandler,
            RepoManager repoManager,
            ILogger consoleLogger)
	{
		this.sdkContext = new SdkContext(sdkHandler, repoManager);
		sdkContext.setSdkLogger(consoleLogger);
		this.consoleLogger = consoleLogger;
	}

	/**
	 * Construct SdkCallAgent object to mediate between application and UI layer requiring an icon factory
     * @param sdkHandler SDK handler
     * @param repoManager Repository manager
     * @param iconEditorFactory Icon factory to provide editor icons
     * @param consoleLogger Console logger to persist all messages
	 */
	public SdkCallAgent(
            AndroidSdkHandler sdkHandler,
            RepoManager repoManager,
            IEditorIconFactory iconEditorFactory,
            ILogger consoleLogger)
	{
		this(sdkHandler, repoManager, consoleLogger);
		this.iconEditorFactory = iconEditorFactory;
	}

	public SdkContext getSdkContext() {
		SdkHelper helper = sdkContext.getSdkHelper();
		if (helper.getImageFactory() == null)
			helper.setImageFactory(getImageLoader(new SdkResourceProvider()));
		return sdkContext;
	}

	public IEditorIconFactory getEditorIconFactory() {
		if (iconEditorFactory ==null)
			// Icon factory not set. Do not throw exception, but handle gracefully.
			return new IEditorIconFactory(){

				@Override
				public Image getColorIcon(String osName, int color) {
					// Return generic image to avoid NPE
					return sdkContext.getSdkHelper().getImageByName("nopkg_icon_16.png");
				}};//
		return iconEditorFactory;
	}

	public void dispose()
	{
		sdkContext.getSdkHelper().dispose();
	}

	/**
	 * Set image loader if not already set
	 */
	public ImageFactory getImageLoader(PluginResourceProvider provider)
	{
		JFaceImageLoader imageLoader = new JFaceImageLoader(provider);
		imageLoader.setLogger(consoleLogger);
		return imageLoader;
 	}
}
