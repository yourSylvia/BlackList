package com.example.sylviameow.blacklist;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.MODE_APPEND;

/**
 * Created by sylviameow on 29/11/17.
 */

public class AddToBlackList{
    public static void addBlacklist(Context context, String phoneNumber){

        SharedPreferences sharedPreferences = context.getSharedPreferences("black_list", MODE_APPEND);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("phoneNum",phoneNumber);

        int maxIndex = sharedPreferences.getInt("maxIndex",0);

        editor.putString("phoneNum"+maxIndex ,phoneNumber);
        editor.putInt("maxIndex",maxIndex+1);

        editor.commit();
    }

    public static boolean isInBlacklist (Context context, String phoneNumber)   {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences("black_list", MODE_APPEND);

            Map<String, Integer> allContent = new HashMap<String,Integer>();

            int maxIndex = sharedPreferences.getInt("maxIndex",0);
            for(int i=0;i<maxIndex;i++){
                allContent.put(sharedPreferences.getString("phoneNum"+i,"-"),1);
            }
            int result = allContent.get(phoneNumber);

            if(result !=0){
                return true;
            }else{
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
