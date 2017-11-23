package org.eclipse.andmore.sdktool;

import java.util.ArrayList;

import org.eclipse.andmore.base.resources.ImageFactory;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdkuilib.repository.ISdkChangeListener;
import com.android.utils.ILogger;

public class SdkHelper {
    private Shell mWindowShell;
    
    /** The current {@link ImageFactory}. */
    private ImageFactory mImageFactory;

    private final ArrayList<ISdkChangeListener> mListeners = new ArrayList<ISdkChangeListener>();

    /** Adds a listener ({@link ISdkChangeListener}) that is notified when the SDK is reloaded. */
    public void addListeners(ISdkChangeListener listener) {
        if (mListeners.contains(listener) == false) {
            mListeners.add(listener);
        }
    }

    /** Removes a listener ({@link ISdkChangeListener}) that is notified when the SDK is reloaded. */
    public void removeListener(ISdkChangeListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Safely invoke all the registered {@link ISdkChangeListener#onSdkLoaded()}.
     * This can be called from any thread.
     */
    public void broadcastOnSdkLoaded(ILogger logger) {
        if (!mListeners.isEmpty()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (ISdkChangeListener listener : mListeners) {
                        try {
                            listener.onSdkLoaded();
                        } catch (Throwable t) {
                        	logger.error(t, null);
                        }
                    }
                }
            });
        }
    }

    public void setWindowShell(Shell windowShell) {
        mWindowShell = windowShell;
    }

    public Shell getWindowShell() {
        return mWindowShell;
    }

    public void setImageFactory(ImageFactory imageFactory) {
    	mImageFactory = imageFactory;
    }
    
    /**
     * Returns image factory.
     * @return ImageFactory object
     */
    public ImageFactory getImageFactory() {
        return mImageFactory;
    }

	public void dispose() {
		if (mImageFactory != null)
			mImageFactory.dispose();
	}
    /**
     * Loads an image given its filename (with its extension).
     * Might return null if the image cannot be loaded.  <br/>
     * The image is cached. Successive calls will return the <em>same</em> object. <br/>
     * The image is automatically disposed when {@link ImageFactory} is disposed.
     *
     * @param imageName The filename (with extension) of the image to load.
     * @return A new or existing {@link Image}. The caller must NOT dispose the image (the
     *  image will disposed by {@link #dispose()}). The returned image can be null if the
     *  expected file is missing.
     */
    @Nullable
    public Image getImageByName(@NonNull String imageName) {
        return mImageFactory != null ? mImageFactory.getImageByName(imageName, imageName, null) : null;
    }

    /**
     * Runs the runnable on the UI thread using {@link Display#syncExec(Runnable)}.
     *
     * @param r Non-null runnable.
     */
    protected void runOnUiThread(@NonNull Runnable r) {
        if (mWindowShell != null && !mWindowShell.isDisposed()) {
            mWindowShell.getDisplay().syncExec(r);
        }
    }


}
