package com.zjh.btim.Fragment;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.zjh.btim.Adapter.ItemBtListAdapter;
import com.zjh.btim.Bean.BlueToothBean;
import com.zjh.btim.CallBack.BlueToothInterface;
import com.zjh.btim.R;
import com.zjh.btim.Receiver.BluetoothStateBroadcastReceive;
import com.zjh.btim.Service.BluetoothChatService;
import com.zjh.btim.Util.BluetoothUtil;
import com.zjh.btim.model.ConnectionModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;
import static com.zjh.btim.Activity.BluetoothConnectionActivity.BLUE_TOOTH_DIALOG;
import static com.zjh.btim.Activity.BluetoothConnectionActivity.BLUE_TOOTH_READ;
import static com.zjh.btim.Activity.BluetoothConnectionActivity.BLUE_TOOTH_SUCCESS;
import static com.zjh.btim.Activity.BluetoothConnectionActivity.BLUE_TOOTH_TOAST;
import static com.zjh.btim.Activity.BluetoothConnectionActivity.DEVICE_CONNECTION_KEY;
import static com.zjh.btim.Activity.BluetoothConnectionActivity.DEVICE_CONNECTION_MODEL;


public class HomeFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, AdapterView.OnItemClickListener {

    private BluetoothUtil bluetoothUtil;
    private BluetoothStateBroadcastReceive broadcastReceive;
    private SwipeRefreshLayout layoutSwipeRefresh;
    private ListView lvBtList;
    private List<BlueToothBean> list;
    private ItemBtListAdapter adapter;
    private LinearLayout layoutHide;
    private ProgressDialog progressDialog;
    private BluetoothChatService mBluetoothChatService;

    private final String localNumber;
    private final String mobileNumber;
    private ConnectionModel connectionModel;


    public HomeFragment(String localNumber, String mobileNumber) {
        this.localNumber = localNumber;
        this.mobileNumber = mobileNumber;
    }

    private BlueToothInterface blueToothInterface = new BlueToothInterface() {
        @Override
        public void getBlueToothDevices(BluetoothDevice device) {
            BlueToothBean blueToothBean = new BlueToothBean(device.getName(), device.getAddress());
            if (device.getName() != null && device.getAddress() != null) {
                int k = 0;
                for (BlueToothBean i : list)
                    if (i.getMac().equals(blueToothBean.getMac()))
                        k++;
                if (k == 0) {
                    list.add(blueToothBean);
                    adapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void getConnectedBlueToothDevices(BluetoothDevice device) {
       }

        @Override
        public void getDisConnectedBlueToothDevices(BluetoothDevice device) { ;
        }

        @Override
        public void searchFinish() {
            layoutSwipeRefresh.setRefreshing(false);
        }

        @Override
        public void open() {
        }

        @Override
        public void disable() {
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case BLUE_TOOTH_DIALOG:
                    showProgressDialog((String) msg.obj);
                    break;
                case BLUE_TOOTH_TOAST:
                    dismissProgressDialog();
                    Snackbar.make(getView(), (String) msg.obj, Snackbar.LENGTH_LONG).show();
                    break;
                case BLUE_TOOTH_READ:
                    dismissProgressDialog();
                    final String mobileStr = (String) msg.obj;

                    if(mobileStr == null)
                        throw new IllegalStateException("mobileStr should not be null");

                    if(mobileStr.contains(DEVICE_CONNECTION_KEY) && !mobileStr.substring(DEVICE_CONNECTION_KEY.length()).equals(mobileNumber)){
                        Snackbar.make(getView(), "This device is not the user's device, try another one.", Snackbar.LENGTH_LONG).show();
                        break;
                    }

                    Intent intent = new Intent();
                    intent.putExtra(DEVICE_CONNECTION_MODEL, connectionModel);
                    Objects.requireNonNull(getActivity()).setResult(RESULT_OK, intent);
                    getActivity().finish();
                    break;
                case BLUE_TOOTH_SUCCESS:
                    BluetoothDevice remoteDevice = (BluetoothDevice) msg.obj;
                    connectionModel = new ConnectionModel(mobileNumber,
                            remoteDevice.getAddress(), remoteDevice.getName());
                    //we send a connection key with phone number to check and avoid printing
                    //it in the chat
                    mBluetoothChatService.sendData(DEVICE_CONNECTION_KEY + localNumber);
                    break;
            }
        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bluetoothUtil = new BluetoothUtil(getContext());
        layoutSwipeRefresh = view.findViewById(R.id.layout_swipe_refresh);
        layoutSwipeRefresh.setOnRefreshListener(this);
        lvBtList = view.findViewById(R.id.lv_bt_list);
        list = new ArrayList<>();
        adapter = new ItemBtListAdapter(list, getContext());
        lvBtList.setAdapter(adapter);
        lvBtList.setOnItemClickListener(this);
        layoutHide = view.findViewById(R.id.layout_hide);
        update();
        registerBluetoothReceiver();
    }

    private void registerBluetoothReceiver() {
        Log.i("zjh", "蓝牙广播监听启动");
        if (broadcastReceive == null) {
            broadcastReceive = new BluetoothStateBroadcastReceive(blueToothInterface);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_OFF");
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_ON");
        getContext().registerReceiver(broadcastReceive, intentFilter);
    }


    @Override
    public void onRefresh() {
        update();
    }

    private void update() {
        layoutSwipeRefresh.setRefreshing(true);
        list.clear();
        if (bluetoothUtil.isBluetoothEnable()) {
            layoutHide.setVisibility(View.INVISIBLE);
            list.addAll(bluetoothUtil.getDevicesList());
            bluetoothUtil.startDiscovery();
        } else {
            layoutHide.setVisibility(View.VISIBLE);
            layoutSwipeRefresh.setRefreshing(false);
        }
        adapter.notifyDataSetChanged();
        if (bluetoothUtil.isBluetoothEnable()) {
            mBluetoothChatService = BluetoothChatService.getInstance(handler);
            mBluetoothChatService.start();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ShowDialog(bluetoothUtil.getBluetoothDevice(list.get(position).getMac()));
    }

    private void ShowDialog(final BluetoothDevice device) {
        AlertDialog.Builder ad = new AlertDialog.Builder(this.getActivity());
        ad.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mBluetoothChatService.connectDevice(device);
            }
        });
        ad.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        ad.setMessage("Are you sure to establish connection with " + device.getName() + "?");
        ad.setTitle("Connection");
        ad.setCancelable(false);
        ad.show();
    }

    public void showProgressDialog(String msg) {
        if (progressDialog == null)
            progressDialog = new ProgressDialog(getActivity());
        progressDialog.setTitle("Open Signal");
        progressDialog.setMessage("Connection needs to open Signal App in the opposite device.\n" + msg);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mBluetoothChatService.stop();
            }
        });
        progressDialog.show();
    }

    public void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        update();
    }
}
