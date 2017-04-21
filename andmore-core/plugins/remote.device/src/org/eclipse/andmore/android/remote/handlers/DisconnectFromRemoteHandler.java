/*
 * Copyright (C) 2012 The Android Open Source Project
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
package org.eclipse.andmore.android.remote.handlers;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.common.log.AndmoreLogger;
import org.eclipse.andmore.android.remote.RemoteDeviceConstants;
import org.eclipse.andmore.android.remote.RemoteDevicePlugin;
import org.eclipse.andmore.android.remote.i18n.RemoteDeviceNLS;
import org.eclipse.andmore.android.remote.instance.RemoteDeviceInstance;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent.InstanceEventType;
import org.eclipse.sequoyah.device.framework.events.InstanceEventManager;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;

/**
 * Service handler responsible for disconnecting from a remote device.
 */
public class DisconnectFromRemoteHandler extends ServiceHandler {
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler#
	 * newInstance()
	 */
	@Override
	public IServiceHandler newInstance() {
		return new DisconnectFromRemoteHandler();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler#runService
	 * (org.eclipse.sequoyah.device.framework.model.IInstance, java.util.Map,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus runService(IInstance instance, Map<Object, Object> arguments, IProgressMonitor monitor) {
		AndmoreLogger.debug("TmL Disconnect from Remote Device Service: start disconnecting from remote device: "
				+ instance);

		if (arguments != null) {
			if (((Boolean) arguments.get(RemoteDeviceConstants.DUMMY_TRANSITION)).booleanValue()) {
				AndmoreLogger.debug("TmL Disconnect from Remote Device Service: dummy transition");
				setSuffix(instance);
				return Status.OK_STATUS;
			}
		}

		IStatus status = Status.OK_STATUS;

		/*
		 * Call ADB disconnect
		 */
		Properties prop = instance.getProperties();
		String host = prop.getProperty(RemoteDeviceInstance.PROPERTY_HOST);
		String port = prop.getProperty(RemoteDeviceInstance.PROPERTY_PORT);
		String timeout = prop.getProperty(RemoteDeviceInstance.PROPERTY_TIMEOUT);

		try {
			status = DDMSFacade.disconnectTcpIp((ISerialNumbered) instance, host, port, Integer.parseInt(timeout),
					monitor);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, RemoteDevicePlugin.PLUGIN_ID,
					RemoteDeviceNLS.ERR_DisconnectToRemote_AdbStart);
		}

		/* ------------------------------------------------------------ */

		if (status.getSeverity() == IStatus.OK) {
			setSuffix(instance);
		}

		AndmoreLogger
				.debug("TmL Disconnect from Remote Device Service: finish disconnecting from remote device. status: "
						+ status.getSeverity());

		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler#
	 * updatingService(org.eclipse.sequoyah.device.framework.model.IInstance,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus updatingService(IInstance instance, IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}

	/*
	 * Set the suffix to null - i.e. no suffix
	 * 
	 * @param instance the instance from which the suffix will be removed
	 */
	private void setSuffix(IInstance instance) {
		if (instance != null) {
			AndmoreLogger.debug("TmL Disconnect from Remote Device Service: removing suffix from instance "
					+ instance.getName());
			instance.setNameSuffix(null);
			InstanceEventManager.getInstance().notifyListeners(
					new InstanceEvent(InstanceEventType.INSTANCE_UPDATED, instance));
		}
	}

}
