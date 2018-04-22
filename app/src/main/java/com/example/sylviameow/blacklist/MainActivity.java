package com.example.sylviameow.blacklist;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.telecom.Call;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import static java.lang.String.valueOf;


public class MainActivity extends Activity {

    private TextView displayNum;
    private String new_name;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        displayNum = findViewById(R.id.BlockNumber);

        boolean isStartedFromService = getIntent().getBooleanExtra("isStartedFromService", false);
        String msgFromService = getIntent().getStringExtra("msgFromService");
        if (isStartedFromService) {
            switch (msgFromService) {
                case "NotBlackList":
                    String incomingNumber = getIntent().getStringExtra("incomingNumber");
                    getContactPepole(getApplicationContext(), incomingNumber);

                    break;
            }

        } else {
            Intent intent = new Intent(this, CallListener.class);
            startService(intent); // Start service
        }


        // Ask for modify notification
        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && !notificationManager.isNotificationPolicyAccessGranted()) {

            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);

            startActivity(intent);
        }


        // Request to write the contact
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_CONTACTS}, 0);


        // Get the latest renew info from shared preference
        SharedPreferences sharedPref = MainActivity.this.getSharedPreferences("refresh_time", MODE_PRIVATE);
        long time = sharedPref.getLong("refreshTime", 0);
        long current_time = System.currentTimeMillis();


        // check the latest download time
        if (current_time > time + 604800000) {
            // Open a sub thread. Download the image from internet
            new Task().execute("http://icons.iconarchive.com/icons/oxygen-icons.org/oxygen/128/Status-dialog-warning-icon.png");
            long refresh_time = System.currentTimeMillis();
            SharedPreferences sharedPreferences = MainActivity.this.getSharedPreferences("refresh_time", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong("refreshTime", refresh_time);
        }


        final Button silence = (Button) findViewById(R.id.Silence);
        silence.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SilenceIncome.class);
                startActivity(intent);
            }
        });

    }


    // Read from the contact and check
    private void getContactPepole(Context context, String incomingNumber) {

        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(incomingNumber));
        Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

        if (cursor.moveToFirst()) {
            String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            displayNum.setText(contactName + ": " + incomingNumber);
        }

        // NO corresponding contact
        else if (!(cursor.moveToFirst()) || cursor.getCount() == 0) {
            showAlertDialogue(incomingNumber);
            try {
                displayPic();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    /* To avoid the android.view.WindowManager$BadTokenException:
    Unable to add window -- token android.os.BinderProxy@XXX is not valid; is your activity running? */
    public static Activity getActivity() throws Exception {
        Class activityThreadClass = Class.forName("android.app.ActivityThread");
        Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
        Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
        activitiesField.setAccessible(true);

        Map<Object, Object> activities = (Map<Object, Object>) activitiesField.get(activityThread);
        if (activities == null)
            return null;

        for (Object activityRecord : activities.values()) {
            Class activityRecordClass = activityRecord.getClass();
            Field pausedField = activityRecordClass.getDeclaredField("paused");
            pausedField.setAccessible(true);
            if (!pausedField.getBoolean(activityRecord)) {
                Field activityField = activityRecordClass.getDeclaredField("activity");
                activityField.setAccessible(true);
                Activity activity = (Activity) activityField.get(activityRecord);
                return activity;
            }
        }

        return null;
    }


    private void showAlertDialogue(final String incomingNumber) {
        try {
            final AlertDialog.Builder normalDialog = new AlertDialog.Builder(MainActivity.this);
            //normalDialog.setIcon(R.drawable.icon_dialog);
            normalDialog.setMessage("This call is unknown");

            // Set incoming call silence
            normalDialog.setNegativeButton("Silence",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            silenceIncome();
                        }
                    });

            // Add current call to contact
            normalDialog.setPositiveButton("+contacts",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showCustomizeDialog(incomingNumber);
                        }
                    });

            // Add current call to blacklist
            normalDialog.setNeutralButton("+Blacklist",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AddToBlackList.addBlacklist(getApplicationContext(), incomingNumber);
                            Toast.makeText(MainActivity.this, "Added successfully", Toast.LENGTH_SHORT).show();
                        }
                    });

            normalDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void addContact(String name, String phoneNumber) {
        // Create a new ContentValues
        ContentValues values = new ContentValues();

        // insert to RawContacts.CONTENT_URI
        // get the rawContactId from the android system
        Uri rawContactUri = getContentResolver().insert(ContactsContract.RawContacts.CONTENT_URI, values);
        long rawContactId = ContentUris.parseId(rawContactUri);
        values.clear();

        // contact name
        values.put(ContactsContract.Contacts.Data.RAW_CONTACT_ID, rawContactId);
        values.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        values.put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name);
        getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);
        values.clear();

        // phone number
        values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        values.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber);
        values.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);
        values.clear();

        Toast.makeText(this, "Add new contact successfully", Toast.LENGTH_SHORT).show();
    }


    private void showCustomizeDialog(final String incomingNumber) {
        AlertDialog.Builder customizeDialog = new AlertDialog.Builder(MainActivity.this);

        final View dialogView = LayoutInflater.from(MainActivity.this)
                .inflate(R.layout.dialogue_window, null);

        customizeDialog.setTitle("Please enter contact name");
        customizeDialog.setView(dialogView);
        customizeDialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText edit_text = (EditText) dialogView.findViewById(R.id.name);
                        new_name = edit_text.getText().toString();
                        Toast.makeText(MainActivity.this, edit_text.getText().toString(), Toast.LENGTH_SHORT).show();

                        addContact(new_name, incomingNumber);
                    }
                });
        customizeDialog.show();
    }


    public void silenceIncome() {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {

                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);

                //audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                audioManager.getStreamVolume(AudioManager.STREAM_RING);
                Toast.makeText(MainActivity.this, "Silence income", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            displayNum.setText(e.toString());
            e.printStackTrace();
        }
    }


    class Task extends AsyncTask<String, Integer, Void> {

        protected Void doInBackground(String... params) {
            GetImageInputStream(params[0]);
            return null;
        }

        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
           /*Message message=new Message();
            message.what=0x123;
            handler.sendMessage(message);*/
        }

    }


    // Download the image from the internet
    public void GetImageInputStream(String imageurl) {

        HttpURLConnection connection = null;
        Bitmap bitmap = null;

        try {
            URL url = new URL(imageurl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(6000); //time exceeded
            connection.setDoInput(true);
            connection.setUseCaches(false); // without using cache
            InputStream inputStream = connection.getInputStream();
            bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SavaImage(bitmap, Environment.getExternalStorageDirectory().getPath());
    }


    // Save image
    public void SavaImage(Bitmap bitmap, String path) {
        File file = new File(path);
        FileOutputStream fileOutputStream = null;

        // Folder doesn't exist, create one
        if (!file.exists()) {
            file.mkdir();
        }
        try {
            fileOutputStream = new FileOutputStream(path + "/warning.png");
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);         // ?
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void displayPic() throws Exception {
        Dialog dia = new Dialog(MainActivity.this, R.style.pic_display_style);
        dia.setContentView(R.layout.warning_display);

        String path = Environment.getExternalStorageDirectory().getPath() + "/warning.png";
        File mFile = new File(path);

        //if the file exist
        if (mFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(path);

            ImageView imageView = dia.findViewById(R.id.start_img);
            imageView.setImageBitmap(bitmap);
            dia.show();

            dia.setCanceledOnTouchOutside(true); // Sets whether this dialog is
            Window w = dia.getWindow();
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.x = 0;
            lp.y = 40;
            dia.onWindowAttributesChanged(lp);

        } else {
            Toast.makeText(MainActivity.this, "File doesn't exist", Toast.LENGTH_SHORT).show();
        }
    }
}


// com.example.sylviameow.blacklist
