package com.jiangdg.usbcamera.utils;

import android.os.AsyncTask;
import android.util.Base64;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpAsynTask extends AsyncTask<String, Void, Void> {
    private HttpAsynTackCallback mCallBack;
    private JSONObject response;
    private Exception exception;

    public HttpAsynTask(HttpAsynTackCallback callback){
        mCallBack = callback;
    }

    @Override
    public Void doInBackground(String... params) {
        String uri = "http://" + ServerSetting.httpHost + ":" + ServerSetting.httpPort + "/recommend";

        try {
            URL url = new URL(uri);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept-Charset","UTF-8");
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setDoOutput(true);
            conn.setDoInput(true);

            String encodedImage = encodeImageToBase64(params[0]);
            JSONObject json = new JSONObject();
            json.put("image", encodedImage);
            json.put("path", params[1]);
            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(json.toString().getBytes("UTF-8"));
            outputStream.flush();
            outputStream.close();

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

            String line;
            String page = "";

            while((line = reader.readLine()) != null){
                page += line;
            }

            response = new JSONObject(page);
            System.out.println(response);
            conn.disconnect();
        } catch (Exception ex)
        {
            ex.printStackTrace();
            exception = ex;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (mCallBack != null){
            mCallBack.onSuccess(response);
        } else {
            mCallBack.onFailure(exception);
        }
    }

    private String encodeImageToBase64(String picPath) {
        try {
            InputStream inputStream = new FileInputStream(picPath);
            byte[] bytes;
            byte[] buffer = new byte[8192];
            int bytesRead;
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            try {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            bytes = output.toByteArray();
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (Exception ex)
        {
            ex.printStackTrace();
            return "";
        }
    }
}