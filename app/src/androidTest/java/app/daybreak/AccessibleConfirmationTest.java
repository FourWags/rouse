package app.daybreak;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.view.ViewGroup;
import android.widget.Button;
import org.json.JSONObject;

/** Switch and screen-reader selection uses the two full-screen buttons, with a second deliberate activation. */
public final class AccessibleConfirmationTest extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();Context c=getTargetContext();RingActivity activity=null;
        String id="qa-accessible-confirm";
        try{
            if(!RingActivity.usesAssistiveSelection(c))throw new Exception("Requires TalkBack or Switch Access selection");
            JSONObject alarm=new JSONObject().put("id",id).put("title","Synthetic accessibility alarm")
                .put("kind","manual").put("at",System.currentTimeMillis());
            c.startForegroundService(new Intent(c,AlarmService.class).setAction("RING").putExtra("alarm",alarm.toString()));
            long deadline=System.currentTimeMillis()+8000;
            while(!Store.object(c,"ringing").has(id)&&System.currentTimeMillis()<deadline)Thread.sleep(50);
            if(!Store.object(c,"ringing").has(id))throw new Exception("Synthetic alarm did not ring");
            Notification notice=null;
            deadline=System.currentTimeMillis()+3000;
            while(notice==null&&System.currentTimeMillis()<deadline){
                for(StatusBarNotification active:c.getSystemService(NotificationManager.class).getActiveNotifications())
                    if(active.getId()==1)notice=active.getNotification();
                if(notice==null)Thread.sleep(50);
            }
            if(notice==null||notice.contentIntent==null||notice.actions!=null&&notice.actions.length>0)
                throw new Exception("Ringing notice check failed: present="+(notice!=null)+
                    ", open="+(notice!=null&&notice.contentIntent!=null)+
                    ", actions="+(notice==null||notice.actions==null?0:notice.actions.length));
            notice.contentIntent.send();
            if(!Store.object(c,"ringing").has(id))throw new Exception("Opening the notice stopped the alarm");
            int snoozesBefore=Store.object(c,"snoozes").length();
            activity=(RingActivity)startActivitySync(new Intent(c,RingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            ViewGroup layout=(ViewGroup)((ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0);
            if(layout.getChildCount()!=2||!(layout.getChildAt(0) instanceof Button)||!(layout.getChildAt(1) instanceof Button))
                throw new Exception("Ringing screen must have only the two large action buttons");
            Button awake=(Button)layout.getChildAt(1);
            if(!awake.getText().toString().contains("twice"))throw new Exception("Assistive screen does not explain two selections");
            runOnMainSync(awake::performClick);
            if(!Store.object(c,"ringing").has(id)||Store.object(c,"snoozes").length()!=snoozesBefore)
                throw new Exception("First selection stopped or snoozed the alarm");
            if(!awake.getText().toString().contains("Select again to stop"))
                throw new Exception("The same large button did not show the confirmation step");
            runOnMainSync(awake::performClick);
            deadline=System.currentTimeMillis()+8000;
            while(Store.object(c,"ringing").has(id)&&System.currentTimeMillis()<deadline)Thread.sleep(50);
            if(Store.object(c,"ringing").has(id)||Store.object(c,"snoozes").length()!=snoozesBefore)
                throw new Exception("Second selection did not dismiss without snoozing");
            result.putString("summary","Two large buttons; first I’m up selection preserved ring; second dismissed without snooze");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable error){result.putString("error",error.toString());finish(Activity.RESULT_CANCELED,result);}
        finally{c.startService(new Intent(c,AlarmService.class).setAction("STOP"));if(activity!=null&&!activity.isFinishing()){RingActivity screen=activity;runOnMainSync(screen::finish);}}
    }
}
