

package onion.chat;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;

public class Notifier {

    private static Notifier instance;
    private Context context;
    private int activities = 0;
    TextToSpeech t1;

    private Notifier(Context context) {
        context = context.getApplicationContext();
        this.context = context;
        t1 = new TextToSpeech(context.getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        });
    }

    public static Notifier getInstance(Context context) {
        context = context.getApplicationContext();
        if (instance == null) {
            instance = new Notifier(context);
        }
        return instance;
    }

    private void log(String s) {
        Log.i("Notifier", s);
    }

    public synchronized void onMessage() {
        t1 = new TextToSpeech(context.getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        });
        log("onMessage");
        if (activities <= 0) {
            Database.getInstance(context).addNotification();
            update();
            inform();
        } else {
            if (Settings.getPrefs(context).getBoolean("sound", true)) {
                try {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(context, notification);
                    inform();
                    r.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized void onResumeActivity() {
        Database.getInstance(context).clearNotifications();
        activities++;
        update();
    }

    public synchronized void onPauseActivity() {
        activities--;
    }

    private void update() {
        log("update");
        //inform();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        int id = 5;
        int messages = Database.getInstance(context).getNotifications();
        if (messages <= 0 || !Settings.getPrefs(context).getBoolean("notify", true)) {
            log("cancel");
            notificationManager.cancel(id);
        } else {
            log("notify");
            inform();
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            NotificationCompat.Builder b = new NotificationCompat.Builder(context)
                    //.setSound()
                    //.setSound(soundUri)
                    //.setLights(0xffffffff, 1000, 1000)


                    //.setAutoCancel()

                    //.setPriority(NotificationCompat.PRIORITY_HIGH)
                    //.setDefaults(NotificationCompat.DEFAULT_SOUND)


                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)

                    .setColor(context.getResources().getColor(R.color.colorNotification))

                    .setDefaults(NotificationCompat.DEFAULT_LIGHTS)

                            //.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                    .setContentTitle(context.getResources().getString(R.string.app_name))
                            //.setContentText(messages == 1 ? "1 new message" : messages + " new messages")
                    .setContentText(context.getResources().getQuantityString(R.plurals.notification_new_messages, messages, messages))
                    .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                            //.setSmallIcon(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP ? R.drawable.ic_chat_bubble_white_24dp : R.mipmap.ic_launcher)



                    .setSmallIcon(R.drawable.ic_chat_bubble_white_24dp);

            if (Settings.getPrefs(context).getBoolean("sound", true)) {
                inform();
                b.setDefaults(NotificationCompat.DEFAULT_SOUND);


            }
            inform();
            notificationManager.notify(id, b.build());
        }
    }
    private void inform(){
        String toSpeak="You have received a new message";
        Toast.makeText(context.getApplicationContext(),
                toSpeak, Toast.LENGTH_SHORT).show();
        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
    }

}
