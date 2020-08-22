package shehryar.paighaam;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SMSWorker {

    Context context;
    private int count,i;
    final static int INTERVAL = 1;
    boolean isPreviousSent = true;
    ArrayList<String> numberList;
    ArrayList<PendingIntent> sentPi;
    private String smsToBeSent;
    private ScheduledFuture<?> scheduledFuture;
    final static String SENT = "SENT_SMS_ACTION";
    private NotificationManager manager;
    private BroadcastReceiver receiver;
    private int countOfIntents = 0;

    public SMSWorker(final Context context, String smsToBeSent, final ScheduledExecutorService ses, final SMSCallbackInterface smsCallbackInterface, final ArrayList<String> numberList) {
        this.context = context;
        this.smsToBeSent = smsToBeSent;
        this.numberList = numberList;
        count = numberList.size();
        sentPi = new ArrayList<>();
        i = 0;
        manager = new NotificationManager(context, String.valueOf(count), (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
        manager.createNewNotification();
        Runnable task = new Runnable() {
            @Override
            public void run() {
                if (i < count) {
                    if (isPreviousSent) {
                        sendSms();
                    }
                } else {
                    scheduledFuture.cancel(true);
                    manager.updateText("All SMS Sent");
                    smsCallbackInterface.AllSmsSent();
                }
            }
        };
        scheduledFuture = ses.scheduleAtFixedRate(task, 0, INTERVAL, TimeUnit.MINUTES);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                gotMessageConfirmation(smsCallbackInterface);
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(context, "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(context, "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(context, "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(context, "Radio off",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        context.registerReceiver(receiver, new IntentFilter(SENT));
    }

    public void removeReciever() {
        context.unregisterReceiver(receiver);
    }

    public void stopSending() {
        scheduledFuture.cancel(true);
    }

    private void sendSms() {
        PendingIntent sentPI = PendingIntent.getBroadcast(context, 0, new Intent(SENT), 0);
        try {
            sentPi.clear();
            countOfIntents=0;
            if (smsToBeSent.length()>160){
                sendLargeMessage(sentPI);
            }else{
                sendNormalMessage(sentPI);
            }
        } catch (Exception e) {
            isPreviousSent = true;
            Toast.makeText(context, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            scheduledFuture.cancel(true);
            removeNotification();
        }
    }

    private void sendNormalMessage(PendingIntent sentPI){
        Log.e("sending to", numberList.get(i));
        isPreviousSent = false;
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(numberList.get(i), null, smsToBeSent, sentPI, null);
    }
    private void sendLargeMessage(PendingIntent sentPI){
        Log.e("sending to", numberList.get(i));
        isPreviousSent = false;
        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> parts = sms.divideMessage(smsToBeSent);
        for (int j=0; j<parts.size(); j++){
            sentPi.add(sentPI);
        }
        sms.sendMultipartTextMessage(numberList.get(i), null, parts, sentPi, null);
    }

    private void gotMessageConfirmation(SMSCallbackInterface smsCallbackInterface){
        if (smsToBeSent.length()>160){
            countOfIntents++;
            if (countOfIntents==sentPi.size())
                messageConfirmTotally(smsCallbackInterface);
        }else{
            messageConfirmTotally(smsCallbackInterface);
        }
    }

    private void messageConfirmTotally(SMSCallbackInterface smsCallbackInterface){
        isPreviousSent = true;
        smsCallbackInterface.SingleSmsSent(i + 1);
        manager.updateText(String.valueOf(i + 1).concat("/").concat(String.valueOf(count)));
        i++;
        if (i == count) {
            scheduledFuture.cancel(true);
            manager.updateText("All SMS Sent");
            smsCallbackInterface.AllSmsSent();
        }
    }

    public void removeNotification() {
        manager.removeNotification();
    }
}
