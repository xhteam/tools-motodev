package com.android.sdkuilib.internal.repository.content;

public enum PackageType {
	tools,
	platform_tools,
	build_tools,
	platforms,
	add_ons,
	system_images,
	sources,
	samples,
	docs,
    extras,
    emulator,
    cmake,
    lldb,
    ndk_bundle,
    patcher,
    generic;
	
	public String label;
	
	static {
		tools.label = "Tools";
		platform_tools.label = "Platform tools";
		build_tools.label = "Build tools";
		platforms.label = "Platforms";
		add_ons.label = "Add ons";
		system_images.label = "System images";
		sources.label = "Sources";
		samples.label = "Samples";
		docs.label = "Documents";
	    extras.label = "Extras";
	    emulator.label = "Emulators";
	    cmake.label = "cmake";
	    lldb.label = "Layout Libraries";
	    ndk_bundle.label = "NDK bundle";
	    patcher.label = "Patcher";
	    generic.label = "Generic";
	}
}
