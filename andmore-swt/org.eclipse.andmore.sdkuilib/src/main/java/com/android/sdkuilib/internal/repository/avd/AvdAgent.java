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
/**
 * 
 */
package com.android.sdkuilib.internal.repository.avd;

import java.util.Map;

import com.android.sdklib.AndroidVersion;
import com.android.sdklib.SdkVersionInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdkuilib.internal.repository.content.PackageType;

/**
 * Contains and AvdInfo object and details extracted from it suitable for application use.
 * Also contains the target assigned to the AvdInfo object to match it's Android version 
 * @author Andrew Bowley
 *
 * 15-11-2017
 */
public class AvdAgent {

	private static final String BLANK = "";
	private final AvdInfo avd;
	private final IAndroidTarget target;
	private String deviceName;
	private String deviceMfctr;
	private String path;
	private SystemImageInfo systemImageInfo;
	private String platformVersion;
	private String versionWithCodename;
	private AndroidVersion androidVersion;
	private PackageType packageType;
	private String vendor;
	private String targetDisplayName;
	private String skin;
	private String sdcard;
	private String snapshot;
	
	/**
	 * 
	 */
	public AvdAgent(IAndroidTarget target, AvdInfo avd) {
		this.target = target;
		this.avd = avd;
		path = avd.getDataFolderPath();
		systemImageInfo = new SystemImageInfo(avd);
		vendor = BLANK;
		targetDisplayName = BLANK;
		init();
	}

	public AvdInfo getAvd() {
		return avd;
	}

	public IAndroidTarget getTarget()
	{
		return target;
	}
	
	public String getDeviceName() {
		return deviceName;
	}

	public String getDeviceMfctr() {
		return deviceMfctr;
	}

	public String getPath() {
		return path;
	}

	public SystemImageInfo getSystemImageInfo() {
		return systemImageInfo;
	}

	public String getPlatformVersion() {
		return platformVersion;
	}

	public String getVersionWithCodename() {
		return versionWithCodename;
	}

	public AndroidVersion getAndroidVersion() {
		return androidVersion;
	}

	public PackageType getPackageType() {
		return packageType;
	}

	public String getVendor() {
		return vendor;
	}

	public String getTargetFullName() {
		return target.getFullName();
	}

	public String getTargetVersionName() {
		String platform = target.getVersionName();
		return platform != null ? platform : BLANK;
	}

	public String getTargetDisplayName() {
		return targetDisplayName;
	}

	public String getSkin() {
		return skin;
	}

	public String getSdcard() {
		return sdcard;
	}

	public String getSnapshot() {
		return snapshot;
	}

	public String getPrettyAbiType() {
		return AvdInfo.getPrettyAbiType(avd);
	}
	
	private void init()
	{
        deviceName = avd.getProperties().get(AvdManager.AVD_INI_DEVICE_NAME);
        deviceMfctr = avd.getProperties().get(AvdManager.AVD_INI_DEVICE_MANUFACTURER);
        if (deviceName == null) {
        	deviceName = BLANK;
        	deviceMfctr = BLANK;
        }
        else if (deviceMfctr == null)
        	deviceMfctr = BLANK;
        androidVersion = systemImageInfo.getAndroidVersion();
        versionWithCodename = SdkVersionInfo
                .getVersionWithCodename(androidVersion);
        platformVersion = SdkVersionInfo.getVersionString(androidVersion.getApiLevel());
        PackageType packageType = systemImageInfo.getPackageType();
        if ((packageType == PackageType.system_images) || (packageType == PackageType.add_ons)) {
            vendor = systemImageInfo.getVendor();
        }
        if (target.isPlatform()) {
        	targetDisplayName = String.format("  API: %s", versionWithCodename);
        } else {
            targetDisplayName = 
            	String.format("Target: %s\n" +
                              "        Based on %s)", 
                              target.getFullName(), 
                              target.getParent().getFullName());
        }
        // Some extra values.
        Map<String, String> properties = avd.getProperties();
        skin = properties.get(AvdManager.AVD_INI_SKIN_NAME);
        sdcard = properties.get(AvdManager.AVD_INI_SDCARD_SIZE);
        if (sdcard == null) 
            sdcard = properties.get(AvdManager.AVD_INI_SDCARD_PATH);
        if (sdcard == null)
            sdcard = BLANK;
        snapshot = properties.get(AvdManager.AVD_INI_SNAPSHOT_PRESENT);
        if (snapshot == null) 
        	snapshot = BLANK;
	}
}
