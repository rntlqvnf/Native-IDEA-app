package com.jiangdg.usbcamera.utils;
import android.util.Log;

import org.json.JSONObject;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketThread extends Thread {
    private JSONObject result;

    public SocketThread(JSONObject _result)
    {
        result = _result;
    }

    @Override
    public void run() {
        try {
            Socket socket = new Socket(ServerSetting.socketHost, ServerSetting.socketPort);
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(result);
            outputStream.flush();

            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            Object input = inputStream.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
