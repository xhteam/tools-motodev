package org.eclipse.andmore.sdktool;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.prefs.AndroidLocation;
import com.android.repository.api.LocalPackage;
import com.android.repository.api.ProgressIndicator;
import com.android.repository.api.ProgressIndicatorAdapter;
import com.android.repository.api.RemotePackage;
import com.android.repository.api.RepoManager;
import com.android.repository.impl.meta.RepositoryPackages;
import com.android.repository.io.FileOp;
import com.android.sdklib.devices.DeviceManager;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdklib.repository.AndroidSdkHandler;
import com.android.sdkuilib.internal.repository.PackageManager;
import com.android.sdkuilib.internal.repository.Settings;
import com.android.utils.ILogger;

public class SdkContext {

    private final AndroidSdkHandler handler;
    private final RepoManager repoManager;
    private final DeviceManager deviceManager;
    private final PackageManager packageManager;
    private final SdkHelper sdkHelper;
    private final AtomicBoolean hasWarning = new AtomicBoolean();
    private final AtomicBoolean hasError = new AtomicBoolean();
    private final ArrayList<String> logMessages = new ArrayList<String>();
    private Settings settings;
    private ProgressIndicator sdkProgressIndicator;
    private ILogger sdkLogger;
    private boolean sdkLocationChanged;
    
	public SdkContext(AndroidSdkHandler handler, RepoManager repoManager) {
		super();
		this.handler = handler;
		this.repoManager = repoManager;
		this.sdkHelper = new SdkHelper();
        deviceManager = DeviceManager.createInstance(handler.getLocation(), loggerInstance());
        packageManager = new PackageManager(this);
	}

	public void setSettings(Settings settings)
	{
		this.settings = settings;
	}
	
	public Settings getSettings()
	{
		if (settings == null)
			settings = new Settings();
		return  settings;
	}
	
	public boolean isSdkLocationChanged() {
		return sdkLocationChanged;
	}

	public PackageManager getPackageManager()
	{
		return packageManager;
	}
	public SdkHelper getSdkHelper()
	{
		return sdkHelper;
	}
	
	public AndroidSdkHandler getHandler() {
		return handler;
	}

	public RepoManager getRepoManager() {
		return repoManager;
	}

	public AvdManager getAvdManager()
	{
		String avdFolder = null;
		AvdManager avdManager = null;
		ILogger logger = loggerInstance();
        try {
            avdFolder = AndroidLocation.getAvdFolder();
            avdManager = AvdManager.getInstance(handler, new File(avdFolder), logger);
        } catch (AndroidLocation.AndroidLocationException e) {
            logger.error(e, "Error obtaining AVD Manager");
        }
        return avdManager;
	}
	
	public RepositoryPackages getPackages() {
		return repoManager.getPackages();
	}

	public FileOp getFileOp() {
		return handler.getFileOp();
	}

	public File getLocalPath() {
		return repoManager.getLocalPath();
	}

	public File getLocation() {
		File location = handler.getLocation();
		if (location == null)
			return new File("");
		return location;
	}

	public Map<String, RemotePackage> getRemotePackages() {
		return getPackages().getRemotePackages();
	}

	public Map<String, LocalPackage> getLocalPackages() {
		return getPackages().getLocalPackages();
	}
	
	public DeviceManager getDeviceManager() {
		return deviceManager;
	}

	public boolean hasWarning() {
		return hasWarning.get();
	}

	public boolean hasError() {
		return hasError.get();
	}

	public ArrayList<String> getLogMessages() {
		return logMessages;
	}

	public ILogger loggerInstance() {
		hasWarning.set(false);
		hasError.set(false);
		logMessages.clear();
		return new ILogger() {
            @Override
            public void error(@Nullable Throwable throwable, @Nullable String errorFormat,
                    Object... arg) {
            	hasError.set(true);
                if (errorFormat != null) {
                    logMessages.add(String.format("Error: " + errorFormat, arg));
                }

                if (throwable != null) {
                    logMessages.add(throwable.getMessage());
                }
            }

            @Override
            public void warning(@NonNull String warningFormat, Object... arg) {
            	hasWarning.set(true);
                logMessages.add(String.format("Warning: " + warningFormat, arg));
            }

            @Override
            public void info(@NonNull String msgFormat, Object... arg) {
                logMessages.add(String.format(msgFormat, arg));
            }

            @Override
            public void verbose(@NonNull String msgFormat, Object... arg) {
                info(msgFormat, arg);
            }
        };

	}

	public void setSdkProgressIndicator(ProgressIndicator sdkProgressIndicator) {
		this.sdkProgressIndicator = sdkProgressIndicator;
	}

	public void setSdkLogger(ILogger sdkLogger) {
		this.sdkLogger = sdkLogger;
	}

	public ProgressIndicator getProgressIndicator() {
		if (sdkProgressIndicator != null)
			return sdkProgressIndicator;
		return new ProgressIndicatorAdapter()
		{
			ILogger logger = getSdkLog();
		    @Override
		    public void logWarning(@NonNull String s, @Nullable Throwable e) {
		    	if (s != null)
		    		logger.warning(s);
		    	if (e != null)
		    		logger.warning(e.getMessage());
		    }

		    @Override
		    public void logError(@NonNull String s, @Nullable Throwable e) {
		    	logger.error(e, s);
		    }

		    @Override
		    public void logInfo(@NonNull String s) {
		    	logger.info(s);
		    }
		};
	}
	
	public ILogger getSdkLog() {
		if (sdkLogger != null)
			return sdkLogger;
		return loggerInstance();
	}

	public void setLocation(File sdkLocation) {
		// This change needs to be monitored so external AndroidSdkHandler consumers can re-sync
		sdkLocationChanged = true;
		AndroidSdkHandler.resetInstance(sdkLocation);
	}

}
