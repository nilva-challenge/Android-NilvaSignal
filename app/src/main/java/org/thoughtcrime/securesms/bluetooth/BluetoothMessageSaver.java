package org.thoughtcrime.securesms.bluetooth;

import android.content.Context;

import org.greenrobot.eventbus.EventBus;
import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.MessageDatabase;
import org.thoughtcrime.securesms.database.SmsDatabase;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.sms.IncomingTextMessage;
import org.thoughtcrime.securesms.sms.MessageSender;
import org.thoughtcrime.securesms.sms.OutgoingTextMessage;
import org.whispersystems.libsignal.util.guava.Optional;

import java.util.Date;

public class BluetoothMessageSaver {

    private static final String TAG = "BluetoothMessageSaver";

    public static long saveSentBluetoothMessage(final Context context,
                                                final OutgoingTextMessage message,
                                                final long threadId,
                                                final SmsDatabase.InsertListener insertListener) {


        Log.i(TAG, "Sending text message to " + message.getRecipient().getId() + ", thread: " + threadId);
        MessageDatabase database    = DatabaseFactory.getSmsDatabase(context);
        Recipient       recipient   = message.getRecipient();

        long allocatedThreadId = DatabaseFactory.getThreadDatabase(context).getOrCreateValidThreadId(recipient, threadId);
        long messageId         = database.insertMessageOutbox(allocatedThreadId, message, true, System.currentTimeMillis(), insertListener);

        DatabaseFactory.getSmsDatabase(context).markAsSent(messageId, true);
        onMessageSent();

        return allocatedThreadId;
    }

    public static void saveReceivedMessage(Context context, Recipient recipient, String readMessage) {

        IncomingTextMessage textMessage = new IncomingTextMessage(recipient.getId(), 1, new Date().getTime(), -1, readMessage, Optional.absent(), 0, false);
        new Thread() {
            @Override
            public void run() {
                super.run();
                DatabaseFactory.getSmsDatabase(context).insertMessageInbox(textMessage);
            }
        }.start();
    }

    private static void onMessageSent() {
        EventBus.getDefault().postSticky(MessageSender.MessageSentEvent.INSTANCE);
    }
}
