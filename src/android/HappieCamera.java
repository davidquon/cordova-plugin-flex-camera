package io.happie.cordovaCamera;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import main.java.com.mindscapehq.android.raygun4android.RaygunClient;

public class HappieCamera extends CordovaPlugin {

    private static CallbackContext staticCallbackContext;
    static File filesDir;
    private Context appContext;
    private static String currentAction = "";

    private static final String CAMERA = Manifest.permission.CAMERA;
    private static final int CAM_REQUEST_CODE = 0;

    public static Integer quality;
    public static String userId = "nouser";
    public static String jnId = "noid";

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        staticCallbackContext = callbackContext;
        currentAction = action;
        filesDir = this.cordova.getActivity().getApplicationContext().getFilesDir();
        appContext =this.cordova.getActivity().getApplicationContext();

        if(action.equals("getProcessingCount")){
            callbackContext.success("{\"count\":"+ HappieCameraJSON.GET_ACTIVE_PROCESSES() + ", \"total\":" + HappieCameraJSON.GET_TOTAL_IMAGES() +"}");
            return true;
        }
        else if(action.equals("writePhotoMeta")){
            String user = args.toString(0);
            String jnid = args.toString(1);
            JSONArray array = args.getJSONArray(2);

            for (int i=0; i<array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                String fileName = item.getString("id");
                String json = item.getString("data");

                FileOutputStream fos = null;
                try {
                    String filePath = appContext.getFilesDir() + "/media" + "/" + user + "/" + jnid;
                    File jsonFile = new File(filePath, fileName);
                    fos = new FileOutputStream(jsonFile);
                    fos.write(json.getBytes("UTF-8"));
                } catch (Exception e) {
                } finally {
                    try {
                        fos.close();
                    } catch (Exception ex) {
                    }
                }
            }
            callbackContext.success("finished writing json");
            return false;
        }
        else if(action.equals("readPhotoMeta")){
            String user = args.toString(0);
            String jnid = args.toString(1);

            String filePath = appContext.getFilesDir() + "/media" + "/" + user + "/" + jnid;
            File sessionDir = new File(filePath);

            StringBuilder output = new StringBuilder();
            output.append("[");

            if (sessionDir.exists()) {
                File[] files = sessionDir.listFiles();
                for (File file : files) {
                    FileInputStream fin = null;
                    try {
                        fin = new FileInputStream(file);
                        output.append(convertStreamToString(fin));
                    } catch (Exception e) {
                    }
                    finally {
                        try {
                            fin.close();
                        } catch (Exception ex) {
                        }
                    }
                }
            }
            output.deleteCharAt(output.lastIndexOf(","));
            output.append("]");
            callbackContext.success(output.toString());
            return false;
        }
        else if(action.equals("generateThumbnail")){
            try{
                generateThumbnail(args);
                callbackContext.success("called build thumbnail");
                return true;
            }catch (java.io.IOException e){
                return false;
            }

        }
        else if(cordova.hasPermission(CAMERA)) {
            quality = args.getInt(0);
            userId = args.getString(1);
            jnId = args.getString(2);
            return executeLogic(action);
        }
        else {
            quality = args.getInt(0);
            userId = args.getString(1);
            jnId = args.getString(2);
            getCamPermission(CAM_REQUEST_CODE);
            return false;
        }
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = null;
        try{
            reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
        catch(Exception e){
            return "";
        }
        finally {
            try{
                reader.close();
            }
            catch (Exception e){
            }
        }
    }

    public boolean executeLogic(String action) {
        if (action.equals("openCamera")) {
            try {
                if (action.equals("openCamera")) { //run thread safe camera
                    cordova.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            openCamera();
                        }
                    });
                }
            } catch (IllegalArgumentException e) {
                staticCallbackContext.error("Illegal Argument Exception");
                PluginResult r = new PluginResult(PluginResult.Status.ERROR);
                staticCallbackContext.sendPluginResult(r);
                return true;
            }

            PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
            r.setKeepCallback(false);
            staticCallbackContext.sendPluginResult(r);
            return true;
        }
        return false;
    }

    public void openCamera() {
        Intent pictureIntent = new Intent(appContext, io.happie.cordovaCamera.HappieCameraActivity.class);
        pictureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appContext.startActivity(pictureIntent);
    }

    public boolean generateThumbnail(final JSONArray args) throws JSONException, java.io.IOException {
        final Context context = this.cordova.getActivity().getApplicationContext();
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try{
                    String name = args.getString(0);
                    HappieCameraThumb thumbGen = new HappieCameraThumb();
                    try{
                        thumbGen.createThumbAtPathWithName(name, context);}
                    catch(java.io.IOException E){}
                }
                catch (JSONException e){}
            }
        });
        return true;
    }

    public void getCamPermission(int requestCode){
        cordova.requestPermission(this, requestCode, CAMERA);
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        for(int r:grantResults) {
            if(r == PackageManager.PERMISSION_DENIED) {
                staticCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Permission Denied"));
                return;
            }
        }
        switch(requestCode) {
            case CAM_REQUEST_CODE:
                executeLogic(currentAction);
                break;
        }
    }

    public static void sessionFinished(String JSON) {
        if (JSON != null && JSON.length() > 0) staticCallbackContext.success(JSON);
        else staticCallbackContext.error("no json");
    }
}
