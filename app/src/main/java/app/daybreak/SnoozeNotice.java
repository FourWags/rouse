package app.daybreak;

import dev.fourwags.wagrise.R;
import android.app.*;
import android.content.*;
import android.net.Uri;
import org.json.JSONObject;
import java.text.DateFormat;
import java.util.Iterator;

/** A silent private reminder that opens the confirmation for the next pending snooze. */
public final class SnoozeNotice {
    static final String OPEN_ACTION="app.daybreak.OPEN_SNOOZE";
    private static final String CHANNEL="snooze_status";
    private static final int NOTICE=205;
    public static void createChannel(Context c){
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Snoozed alarms",NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Private status while an alarm is snoozed");
        channel.setSound(null,null);channel.enableVibration(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        c.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
    public static void update(Context c){
        try {
        NotificationManager manager=c.getSystemService(NotificationManager.class);
        if(manager==null)return;
        JSONObject snoozes=Store.object(c,"snoozes");
        JSONObject next=null;int count=0;long now=System.currentTimeMillis();
        for(Iterator<String> ids=snoozes.keys();ids.hasNext();){
            JSONObject alarm=snoozes.optJSONObject(ids.next());
            if(alarm==null||!"snooze".equals(alarm.optString("kind"))||alarm.optLong("at")<=now)continue;
            count++;if(next==null||alarm.optLong("at")<next.optLong("at"))next=alarm;
        }
        if(next==null){manager.cancel(NOTICE);return;}
        createChannel(c);
        String id=next.optString("id");
        Intent open=new Intent(c,MainActivity.class).setAction(OPEN_ACTION)
            .setData(Uri.parse("daybreak://snooze/"+Uri.encode(id))).putExtra("snoozeId",id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent review=PendingIntent.getActivity(c,NOTICE,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        String time=DateFormat.getTimeInstance(DateFormat.SHORT).format(new java.util.Date(next.optLong("at")));
        String text=count==1?"Snoozed until "+time:"Next of "+count+" snoozes at "+time;
        Notification publicVersion=new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_alarm_notification)
            .setContentTitle("WagRise snooze active").setContentText("Open WagRise to review").build();
        Notification notice=new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_alarm_notification)
            .setColor(Appearance.current(c).accent).setContentTitle("WagRise snooze active")
            .setContentText(text).setCategory(Notification.CATEGORY_STATUS).setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion).setOnlyAlertOnce(true).setOngoing(true)
            .setContentIntent(review).addAction(new Notification.Action.Builder(null,"I’m up",review).build()).build();
        manager.notify(NOTICE,notice);
        } catch(RuntimeException ignored) { /* In-app snooze controls remain available. */ }
    }
    private SnoozeNotice(){}
}
