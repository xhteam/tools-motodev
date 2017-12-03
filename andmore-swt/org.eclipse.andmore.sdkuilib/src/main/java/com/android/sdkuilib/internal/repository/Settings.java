package com.android.sdkuilib.internal.repository;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.android.annotations.Nullable;
import com.android.prefs.AndroidLocation;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.repository.api.Channel;
import com.android.repository.api.SettingsController;
import com.android.repository.io.FileOp;
import com.android.repository.io.FileOpUtils;
import com.android.repository.io.impl.FileSystemFileOp;
import com.android.utils.ILogger;

public class Settings implements SettingsController {
	
	private static final String SETTINGS_FILENAME = "androidtool.cfg";
	public static final String FORCE_HTTP_KEY = "sdkman.force.http";
	  
    private int mChannel = 0;
    private final FileOp mFileOp;
    private final Properties mProperties;

    public Settings()
    {
        mProperties = new Properties();
    	mFileOp = (FileSystemFileOp)FileOpUtils.create();
    }
    
	public boolean getEnablePreviews() {
		return true;
	}
	
	@Override
	public boolean getForceHttp() {
        return Boolean.parseBoolean(this.mProperties.getProperty(FORCE_HTTP_KEY));
	}

	@Override
	public void setForceHttp(boolean force) {
        mProperties.setProperty(FORCE_HTTP_KEY, Boolean.toString(force));
	}

    @Nullable
    @Override
    public Channel getChannel() {
        return Channel.create(mChannel);
    }

	public boolean initialize(ILogger logger) {
		try {
			loadSettings();
		} catch (AndroidLocationException | IOException e) {
			logger.error(e, "Error initializing SDK settings");
			return false;
		}
		return true;
	}


    private void loadSettings() throws AndroidLocationException, IOException
    {
        String folder = AndroidLocation.getFolder();
        File file = new File(folder, SETTINGS_FILENAME);
        
        mProperties.clear();
        mProperties.load(mFileOp.newFileInputStream(file));
        
        //setShowUpdateOnly(getShowUpdateOnly());
        //setSetting("sdkman.ask.adb.restart", getAskBeforeAdbRestart());
        //setSetting("sdkman.use.dl.cache", getUseDownloadCache());
        //setSetting("sdkman.enable.previews2", getEnablePreviews());
    }

	public Settings copy() {
		Settings copy = new Settings();
		// Load is not expected to fail. Copy will contain defaults if it does.
		try {
			copy.loadSettings();
		} catch (AndroidLocationException e) {
		} catch (IOException e) {
		}
		return copy;
	}

}
