package com.triaged.badge.net.api.requests;

import com.triaged.badge.models.Device;

/**
 * Created by Sadegh Kazemy on 9/18/14.
 */
public class DeviceRequest {

    Device device;

    public DeviceRequest(Device device) {
        this.device = device;
    }
}
