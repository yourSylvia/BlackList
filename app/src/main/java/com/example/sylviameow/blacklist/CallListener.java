package com.example.sylviameow.blacklist;

import android.app.AlertDialog;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.IntDef;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

public class CallListener extends Service {
    TelephonyManager tm;
    exPhoneCallListener myPhoneCallListener;

    public CallListener() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.

        return new MyBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        myPhoneCallListener = new exPhoneCallListener();

        tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        tm.listen(myPhoneCallListener, PhoneStateListener.LISTEN_CALL_STATE);


        return super.onStartCommand(intent, flags, startId);
    }


    private class MyBinder extends Binder {
        /**
         * Get Service
         * @return return PlayerService
         */
        public  CallListener getService(){
            return CallListener.this;
        }
    }


    public class exPhoneCallListener extends PhoneStateListener {

        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:

                    try {
                        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        if (audioManager != null) {

                            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                            audioManager.getStreamVolume(AudioManager.STREAM_RING);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                case TelephonyManager.CALL_STATE_OFFHOOK:

                    try {
                        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        if (audioManager != null) {

                            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                            audioManager.getStreamVolume(AudioManager.STREAM_RING);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                // Ringing
                case TelephonyManager.CALL_STATE_RINGING:
                    // Check whether the phone number is in the black list
                    try {
                        boolean isInBlackList= AddToBlackList.isInBlacklist(getApplicationContext(), incomingNumber);
                        if (isInBlackList) {
                            silenceIncome();
                        }
                        else {
                            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                            intent.putExtra("isStartedFromService",true);
                            intent.putExtra("msgFromService","NotBlackList");
                            intent.putExtra("incomingNumber",incomingNumber);
                            startActivity(intent);
                        }
                    }catch (Exception e){
                        e.printStackTrace();

                    }

                    break;
                default:
                    break;
            }
//            super.onCallStateChanged(state, incomingNumber);
        }

    }


    public void silenceIncome() {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {

                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);

                audioManager.getStreamVolume(AudioManager.STREAM_RING);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
