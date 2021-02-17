package com.jiangdg.usbcamera.utils;
import android.os.Handler;
import android.util.Log;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.net.Socket;

public class SocketThread extends Thread {
    private JSONObject result;
    private Handler mHandler;

    public SocketThread(JSONObject _result, Handler _handler)
    {
        result = _result;
        mHandler = _handler;
    }

    @Override
    public void run() {
        try {
            Socket socket = new Socket(ServerSetting.socketHost, ServerSetting.socketPort);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF(result.toString());
            outputStream.flush();

            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            String input = inputStream.readUTF();
            System.out.println(input);
            /***
             * Uncomment this if need error detection
             */
            //boolean result = Boolean.valueOf(input.get("result").toString());
            boolean result = true;
            if(result)
                mHandler.sendEmptyMessage(1);
            else
                mHandler.sendEmptyMessage(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
