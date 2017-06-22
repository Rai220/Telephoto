package com.rai220.securityalarmbot.photo;

import com.google.gson.Gson;
import com.rai220.securityalarmbot.utils.L;

import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Распознает объекты на изображениях
 * Created by rai220 on 10/25/16.
 */
public class ClarifyAiHelper {
    private static OkHttpClient client = new OkHttpClient();
    private static Gson gson = new Gson();

    public static final String[] CLASSES_HUMAN = {"HUMAN", "MAN", "PEOPLE", "BOY", "GIRL", "CROWD", "WOMAN", "MEN", "WEMAN", "PEOPLE"};

    public static boolean isThereMan(List<String> classes) {
        for (String name : CLASSES_HUMAN) {
            if (classes.contains(name)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> clarify(byte[] jpeg) {
        try {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("encoded_data", "file.jpg", MultipartBody.create(MediaType.parse("image/jpeg"), jpeg))
                    .build();

            Request request = new Request.Builder()
                    .addHeader("Authorization", "Bearer " + "nc8xqOaMYTJp7fMZiUbi396r7ZFkeB")
                    .url("https://api.clarifai.com/v1/tag/")
                    .post(requestBody)
                    .build();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String result = response.body().string();
                Map obj = gson.fromJson(result, Map.class);
                List resultsList = (List) obj.get("results");
                if (resultsList.size() > 0) {
                    Map res = (Map) ((Map) resultsList.get(0)).get("result");
                    Map tag = (Map) res.get("tag");
                    List classes = (List) tag.get("classes");
                    List<String> myClasses = (List) tag.get("classes");
                    for (int i = 0; i < classes.size(); i++) {
                        myClasses.add(classes.get(i).toString().toUpperCase());
                    }
                    return myClasses;
                }
                L.i(result);
            }
        } catch (Throwable ex) {
            L.e(ex);
        }
        return null;
    }
}
