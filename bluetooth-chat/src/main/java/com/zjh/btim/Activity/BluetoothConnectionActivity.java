package com.zjh.btim.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.zjh.btim.Fragment.HomeFragment;
import com.zjh.btim.R;
import com.zjh.btim.Util.XPermissionUtil;


public class BluetoothConnectionActivity extends AppCompatActivity {

    public static final int BLUE_TOOTH_DIALOG = 0x111;
    public static final int BLUE_TOOTH_TOAST = 0x123;
    public static final int BLUE_TOOTH_WRAITE = 0X222;
    public static final int BLUE_TOOTH_READ = 0X333;
    public static final int BLUE_TOOTH_WRAITE_FILE_NOW = 0X511;
    public static final int BLUE_TOOTH_READ_FILE_NOW = 0X996;
    public static final int BLUE_TOOTH_WRAITE_FILE = 0X555;
    public static final int BLUE_TOOTH_READ_FILE = 0X888;
    public static final int BLUE_TOOTH_SUCCESS = 0x444;
    public static final String MOBILE_NUMBER = "mobile_number";
    public static final String LOCAL_NUMBER = "local_number";
    public static final String DEVICE_CONNECTION_MODEL = "device_connection_model";
    public static final String DEVICE_CONNECTION_KEY = "SVSGE5VV=)(=MCNO3FDS";


    private String mobileNumber;
    private String localNumber;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mobileNumber = getIntent().getStringExtra(MOBILE_NUMBER);
        localNumber = getIntent().getStringExtra(LOCAL_NUMBER);
        if (mobileNumber == null)
            throw new IllegalArgumentException("mobile number must not be null.");
        if (localNumber == null)
            throw new IllegalArgumentException("local number must not be null.");

        initView();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView() {

        XPermissionUtil.requestPermissions(this, 1,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN},
                new XPermissionUtil.OnPermissionListener() {
                    @Override
                    public void onPermissionGranted() { }

                    @Override
                    public void onPermissionDenied() {

                    }
                });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment, new HomeFragment(localNumber, mobileNumber))
                .commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        XPermissionUtil.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
