package com.jiangdg.usbcamera.utils;

import org.json.JSONObject;

public interface HttpAsynTackCallback {
    void onSuccess(JSONObject result);
    void onFailure(Exception e);
}
