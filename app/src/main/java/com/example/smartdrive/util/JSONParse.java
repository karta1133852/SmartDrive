package com.example.smartdrive.util;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.Iterator;


public class JSONParse {

    public static JSONObject loadJSONFromAsset(Context context, String fileName) {
        String json = null;
        JSONObject jsonObject = null;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
            jsonObject = new JSONObject(json);
            /*Iterator iterator = jsonObject.keys();
            while (iterator.hasNext()) {
                Log.e("Json", iterator.next().toString());
            }*/

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return jsonObject;
    }
}
