package com.android.sdkuilib.internal.repository.content;

/**
 * Information about a package type
 */
public class MetaPackage {

	private final PackageType mPackageType;
	private final String mIconResource;
	
	public MetaPackage(PackageType packageType, String iconResource) {
		this.mPackageType = packageType;
		this.mIconResource = iconResource;
	}

	public PackageType getPackageType() {
		return mPackageType;
	}

	public String getIconResource() {
		return mIconResource;
	}

	public String getName()
	{
		return mPackageType.toString().replaceAll("_", "-");
	}
}
