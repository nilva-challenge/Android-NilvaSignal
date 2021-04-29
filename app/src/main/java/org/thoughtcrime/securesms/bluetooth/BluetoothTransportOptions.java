package org.thoughtcrime.securesms.bluetooth;

import android.content.Context;

import androidx.annotation.NonNull;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.TransportOption;
import org.thoughtcrime.securesms.util.PushCharacterCalculator;

public class BluetoothTransportOptions {

    public static @NonNull TransportOption getBluetoothTransportOption(@NonNull Context context) {
        TransportOption bluetoothTrans = new TransportOption(TransportOption.Type.TEXTSECURE,
                R.drawable.ic_send_bluetooth_lock_24,
                context.getResources().getColor(R.color.core_ultramarine_dark),
                context.getString(R.string.ConversationActivity_transport_bluetooth),
                context.getString(R.string.conversation_activity__type_message_bluetooth),
                new PushCharacterCalculator());

        bluetoothTrans.setIsBluetooth(true);
        return bluetoothTrans;
    }
}
