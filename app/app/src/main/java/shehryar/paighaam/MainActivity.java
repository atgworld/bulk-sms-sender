/* Copyright (C) 2016 CodingInfinite Technologies - All Rights Reserved
 * NOTICE:  All information contained herein is, and remains
 * the property of CodingInfinite Technologies and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to CodingInfinite Technologies
 * and its suppliers and may be covered by Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from CodingInfinite Technologies.
 */
package shehryar.paighaam;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, SMSCallbackInterface {

    private static final int FILE_REQUEST_CODE = 123;
    ImageView sendSMSBtn;
    EditText smsMessageET;
    ListView nmbrsList;
    TextView filename, numberLeft,length;
    Button dirChooserButton1;

    public static int limit, pause, deleteNum;

    ArrayList<String> nmbers = new ArrayList<>();
    ArrayAdapter arrayAdapter;

    String filePath = "";
    private ProgressBar progressBar;
    private SMSWorker worker;
    private ScheduledExecutorService ses;
    private PowerManager.WakeLock wl;

    private String[] permissions = new String[]{
            Manifest.permission.SEND_SMS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //add permissions to android 8.0 or latest
        Permission.permissionsValidate(1, MainActivity.this, permissions);

        Toolbar toolbar = findViewById(R.id.toolbar);
        filename = findViewById(R.id.filename);
        length = findViewById(R.id.length);
        numberLeft = findViewById(R.id.numberLeft);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        nmbrsList = findViewById(R.id.listView1);
        dirChooserButton1 = findViewById(R.id.button1);
        progressBar = findViewById(R.id.progressBar);
        NavigationView navigationView = findViewById(R.id.nav_view);
        sendSMSBtn = findViewById(R.id.btnSendSMS);
        smsMessageET = findViewById(R.id.editText1);
        numberLeft.setNestedScrollingEnabled(true);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "SMSAPP:PowerTag");

        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        ses = Executors.newScheduledThreadPool(1);
        navigationView.setNavigationItemSelectedListener(this);
        arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, nmbers);

        smsMessageET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length()>=160){
                    length.setTextColor(Color.parseColor("#ff0000"));
                    length.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                }else{
                    length.setTextColor(Color.parseColor("#000000"));
                    length.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
                }
                length.setText(charSequence.length() + "/160");
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        dirChooserButton1.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("InvalidWakeLockTag")
            @Override
            public void onClick(View v) {
                Intent intent = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                            .setType("text/*")
                            .addCategory(Intent.CATEGORY_OPENABLE);
                    String[] types = {"text/*","text/csv"};
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, types);
                    startActivityForResult(Intent.createChooser(intent, "Select a file"), FILE_REQUEST_CODE);
                }
            }
        });

        sendSMSBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (smsMessageET.getText().toString().isEmpty())
                    Toast.makeText(getBaseContext(), "Message can not be empty!", Toast.LENGTH_SHORT).show();
                else if (filePath.isEmpty()) {
                    Toast.makeText(getBaseContext(), "Please choose a file!", Toast.LENGTH_SHORT).show();
                } else {
                    sendSMS();
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.close) {
            if (nmbers.size() != 0) {
                if (worker != null) {
                    worker.stopSending();
                    arrayAdapter.clear();
                    filename.setText("");
                    worker.removeReciever();
                    filePath = "";
                    numberLeft.setText("");
                    progressBar.setVisibility(View.GONE);
                    smsMessageET.setText("");
                    worker.removeNotification();
                    wl.release();
                    btnEnabled(true);
                } else {
                    Toast.makeText(this, "Start a service first", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Select a file with appropiate phone numbers", Toast.LENGTH_SHORT).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    void btnEnabled(boolean bool) {
        dirChooserButton1.setEnabled(bool);
        smsMessageET.setEnabled(bool);
        sendSMSBtn.setEnabled(bool);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        worker.removeReciever();
        worker.removeNotification();
        ses.shutdown();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            moveTaskToBack(true);
//            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri selectedfile = data.getData();
                getPhoneNumber(selectedfile);
            }
        }

    }

    public void getPhoneNumber(Uri uri) {
        if (uri.getPath() != null) {
            Log.e("TAG", "getPhoneNumber: " + uri.getEncodedPath());
            String path = uri.getPath().substring(uri.getPath().indexOf(":") + 1);
            filePath = path;
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), path);
            try {
                InputStreamReader is = new InputStreamReader(getContentResolver().openInputStream(uri));
                BufferedReader reader = new BufferedReader(is);
                String line = reader.readLine();
                nmbers.clear();
                filename.setText(path);
                while (line != null) {
                    if (line.length() <=12)
                        nmbers.add(line);
                    line = reader.readLine();
                }
                nmbrsList.setAdapter(arrayAdapter);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error Reading File", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error Reading File", Toast.LENGTH_SHORT).show();
            }
        } else
            Toast.makeText(this, "Uri is null", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.send_sms) {
        } else if (id == R.id.settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.about) {
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
        } else if (id == R.id.exit_app) {
            finish();
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    protected void sendSMS() {
        btnEnabled(false);
        worker = new SMSWorker(getApplicationContext(),
                smsMessageET.getText().toString(),
                ses, this, nmbers);
        wl.acquire(15*60*1000L /*15 minutes*/);
        progressBar.setMax(nmbers.size());
        progressBar.setVisibility(View.VISIBLE);
        numberLeft.setText("Number's Left:".concat(String.valueOf(nmbers.size())));
    }

    @Override
    public void SingleSmsSent(int progress) {
        progressBar.setProgress(progress);
        if (wl.isHeld())
            wl.release();
        wl.acquire(15*60*1000L /*15 minutes*/);
        numberLeft.setText("Number's Left:".concat(String.valueOf(nmbers.size() - progress)));
        arrayAdapter.notifyDataSetChanged();
    }

    @Override
    public void AllSmsSent() {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(this, "All Sms sent", Toast.LENGTH_SHORT).show();
        worker.stopSending();
        arrayAdapter.clear();
        filename.setText("");
        filePath = "";
        worker.removeReciever();
        numberLeft.setText("");
        smsMessageET.setText("");
        worker.removeNotification();
        wl.release();
        btnEnabled(true);
    }
}
