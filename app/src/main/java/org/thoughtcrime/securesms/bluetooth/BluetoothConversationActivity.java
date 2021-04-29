package org.thoughtcrime.securesms.bluetooth;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import com.google.android.material.snackbar.Snackbar;
import com.zjh.btim.Activity.BluetoothConnectionActivity;
import com.zjh.btim.CallBack.BlueToothInterface;
import com.zjh.btim.Receiver.BluetoothStateBroadcastReceive;
import com.zjh.btim.Service.BluetoothChatService;
import com.zjh.btim.Util.BluetoothUtil;
import com.zjh.btim.model.ConnectionModel;
import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.TransportOption;
import org.thoughtcrime.securesms.conversation.ConversationActivity;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.sms.OutgoingTextMessage;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.concurrent.SimpleTask;

import static com.zjh.btim.Activity.BluetoothConnectionActivity.BLUE_TOOTH_DIALOG;
import static com.zjh.btim.Activity.BluetoothConnectionActivity.BLUE_TOOTH_READ;
import static com.zjh.btim.Activity.BluetoothConnectionActivity.BLUE_TOOTH_READ_FILE;
import static com.zjh.btim.Activity.BluetoothConnectionActivity.BLUE_TOOTH_READ_FILE_NOW;
import static com.zjh.btim.Activity.BluetoothConnectionActivity.BLUE_TOOTH_SUCCESS;
import static com.zjh.btim.Activity.BluetoothConnectionActivity.BLUE_TOOTH_TOAST;
import static com.zjh.btim.Activity.BluetoothConnectionActivity.BLUE_TOOTH_WRAITE;
import static com.zjh.btim.Activity.BluetoothConnectionActivity.BLUE_TOOTH_WRAITE_FILE;
import static com.zjh.btim.Activity.BluetoothConnectionActivity.BLUE_TOOTH_WRAITE_FILE_NOW;
import static com.zjh.btim.Activity.BluetoothConnectionActivity.DEVICE_CONNECTION_KEY;

public class BluetoothConversationActivity extends ConversationActivity {

    private static final int UPDATE_DATA = 0x666;
    private String message;
    private String mobileNumber;
    private String localNumber;
    private boolean isPaired;
    private ConnectionModel connectionModel;
    private BluetoothChatService bluetoothChatService;
    private ProgressDialog progressDialog;
    View parentLayout;
    private BluetoothUtil bluetoothUtil;
    private BluetoothStateBroadcastReceive broadcastReceive;

    private boolean selectedTransportIsBluetooth;

    private final BlueToothInterface blueToothInterface = new BlueToothInterface() {
        @Override
        public void getBlueToothDevices(BluetoothDevice device) {

        }

        @Override
        public void getConnectedBlueToothDevices(BluetoothDevice device) {

        }

        @Override
        public void getDisConnectedBlueToothDevices(BluetoothDevice device) {

        }

        @Override
        public void searchFinish() {

        }

        @Override
        public void open() {
            bluetoothChatService = BluetoothChatService.getInstance(handler);
            bluetoothChatService.start();
        }

        @Override
        public void disable() {
            bluetoothChatService.stop();
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
                    Snackbar.make(parentLayout, (String) msg.obj, Snackbar.LENGTH_LONG).show();
                    break;
                case BLUE_TOOTH_SUCCESS:
                    BluetoothDevice remoteDevice = (BluetoothDevice) msg.obj;
                    connectionModel = new ConnectionModel(mobileNumber,
                            remoteDevice.getAddress(), remoteDevice.getName());

                    //we send a connection key with phone number to check and avoid printing
                    //it in the chat
                    bluetoothChatService.sendData(DEVICE_CONNECTION_KEY + localNumber);
                    isPaired = true;
                    break;
                case BLUE_TOOTH_READ:
                    dismissProgressDialog();
                    String readMessage = (String) msg.obj;
                    if(readMessage.contains(DEVICE_CONNECTION_KEY)) break;
                    saveReceivedMessage(recipient.get(), readMessage);
                    break;
                case BLUE_TOOTH_WRAITE:
                    String writeMessage = (String) msg.obj;
                    if(writeMessage.contains(DEVICE_CONNECTION_KEY)) break;
                    saveSentMessage(writeMessage);
                    break;
                case BLUE_TOOTH_READ_FILE_NOW:

                    break;
                case BLUE_TOOTH_WRAITE_FILE_NOW:

                    if (msg.obj.toString().equals("File sending failed")) {

                    } else {

                    }
                    break;
                case BLUE_TOOTH_READ_FILE:

                    break;
                case BLUE_TOOTH_WRAITE_FILE:

                    break;
                case UPDATE_DATA:

                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle state, boolean ready) {
        super.onCreate(state, ready);

        parentLayout = findViewById(android.R.id.content);
        bluetoothUtil = new BluetoothUtil(getBaseContext());
        localNumber = TextSecurePreferences.getLocalNumber(getBaseContext());
        mobileNumber = getRecipient().getE164().get();

        selectedTransportIsBluetooth = sendButton.getSelectedTransport().isBluetooth();

        if (selectedTransportIsBluetooth) {
            start();
            registerBluetoothReceiver();
        }
    }

    @Override
    public void sendTextViaBluetooth(String message) {
        this.message = message;

        if (isPaired) {
            bluetoothChatService.sendData(message);
        } else {
            Intent intent = new Intent(this, BluetoothConnectionActivity.class);
            intent.putExtra(BluetoothConnectionActivity.MOBILE_NUMBER, mobileNumber);
            intent.putExtra(BluetoothConnectionActivity.LOCAL_NUMBER, localNumber);
            startActivityForResult(intent, REQUEST_ID);
        }
    }
    @Override
    public void onTransportChanged(TransportOption newTransport, boolean manuallySelected) {
        if(!manuallySelected) return;
        Log.d(TAG, "onTransportChanged: type: " + newTransport.getType() + " isBluetooth: " + newTransport.isBluetooth() + " manuallySelected: " + manuallySelected);
        bluetoothUtil = new BluetoothUtil(getBaseContext());
        if (newTransport.isType(TransportOption.Type.TEXTSECURE) && newTransport.isBluetooth()) {
            start();
            registerBluetoothReceiver();
        } else {
            unregisterBluetoothReceiver();
            if(bluetoothUtil != null)
                bluetoothUtil.disableBluetooth();
        }
    }

    private void registerBluetoothReceiver() {
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
        getBaseContext().registerReceiver(broadcastReceive, intentFilter);
    }

    private void unregisterBluetoothReceiver() {
        if (broadcastReceive != null) {
            getBaseContext().unregisterReceiver(broadcastReceive);
            broadcastReceive = null;
        }

        if(bluetoothUtil != null)
            bluetoothUtil.close();

        if (bluetoothChatService != null)
            bluetoothChatService.stop();

        if (isPaired)
            isPaired = false;
    }

    private void start() {
        if (!bluetoothUtil.isBluetoothEnable()) {
            bluetoothUtil.openBluetooth();
        }
        bluetoothUtil.startDiscovery();
        bluetoothChatService = BluetoothChatService.getInstance(handler);
        bluetoothChatService.start();
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        if (reqCode == REQUEST_ID && resultCode == RESULT_OK) {

            bluetoothChatService = BluetoothChatService.getInstance(handler);
            connectionModel = data.getParcelableExtra(BluetoothConnectionActivity.DEVICE_CONNECTION_MODEL);
            bluetoothChatService.sendData(message);
            isPaired = true;
        }
    }

    private void showProgressDialog(String msg) {
        if (progressDialog == null)
            progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Open Signal");
        progressDialog.setMessage("Connection needs to open Signal App in the opposite device.\n" + msg);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                bluetoothChatService.stop();
            }
        });
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(selectedTransportIsBluetooth) {
            unregisterBluetoothReceiver();
        }
    }

    private void saveSentMessage(String writeMessage) {

        final long    thread      = this.threadId;
        final Context context     = getApplicationContext();

        OutgoingTextMessage message = new OutgoingTextMessage(recipient.get(), writeMessage, 0, -1);
        silentlySetComposeText("");
        final long id = fragment.stageOutgoingMessage(message);

        SimpleTask.run(() -> {
            return BluetoothMessageSaver.saveSentBluetoothMessage(context, message, thread, () -> fragment.releaseOutgoingMessage(id));
        }, this::sendComplete);
    }

    private void saveReceivedMessage(Recipient recipient, String readMessage) {
        BluetoothMessageSaver.saveReceivedMessage(this, recipient, readMessage);
    }

    public final int REQUEST_ID = 104;
    private final String TAG = this.getClass().getSimpleName();
}
