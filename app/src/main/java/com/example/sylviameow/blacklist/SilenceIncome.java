package com.example.sylviameow.blacklist;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static android.R.attr.id;

public class SilenceIncome extends MainActivity {
    String temp = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_silence_income);
        // The keyboard will not pop up automatically
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        final Button silence = findViewById(R.id.Silence);
        silence.setOnClickListener(new Button.OnClickListener() {

            // Add blacklist manually
            @Override
            public void onClick(View v) {
                String number="";
                EditText input = findViewById(R.id.Number);
                number = input.getText().toString();

                addNumber(number);
            }
        });

        final Button calls = findViewById(R.id.Calls);
        calls.setOnClickListener(new Button.OnClickListener() {

            // Add blacklist from call log
            @Override
            public void onClick(View v) {
                showListDialog();
            }
        });
    }


    private void showListDialog() {
        final String[] items = getNumberFromCallog(getApplicationContext());

        if(items == null){
            Toast.makeText(SilenceIncome.this, "Call log is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder listDialog = new AlertDialog.Builder(SilenceIncome.this);
        listDialog.setTitle("Recent call log");
        listDialog.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(SilenceIncome.this, "Added" + items[which], Toast.LENGTH_SHORT).show();
                temp = items[which];

                EditText input = findViewById(R.id.Number);
                input.setText(temp);
            }
        });
        listDialog.show();
    }

    private String[] getNumberFromCallog(Context context){
        int type;
        int j=0;
        String sNumber;
        Cursor cursor=null;
        String[] phone ={"","",""};

        // search the call log
        try{
            cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, new String[] {
                            CallLog.Calls.NUMBER, CallLog.Calls.TYPE}, null, null,
                    CallLog.Calls.DATE + " DESC");

            if(cursor!=null){
                for (int i = 0; i < cursor.getCount(); i++) {
                    cursor.moveToPosition(i);
                    sNumber = cursor.getString(0);
                    type = cursor.getInt(1);

                    // in:1, out:2, miss:3
                    if (type == 3 && j <3){
                        phone[j] = sNumber;
                        j++;
                    }
                }
                cursor.close();
            }

        }catch (SecurityException e){
            Toast.makeText(getApplicationContext(),"Permission deny",Toast.LENGTH_LONG).show();
        }

        return phone;
    }


    private void addNumber(String number){
        AddToBlackList.addBlacklist(getApplicationContext(), number);
        Toast.makeText(SilenceIncome.this, "Added successfully", Toast.LENGTH_SHORT).show();
    }

}
