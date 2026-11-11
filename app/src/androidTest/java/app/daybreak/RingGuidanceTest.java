package app.daybreak;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.app.NotificationManager;
import android.app.Notification;
import android.service.notification.StatusBarNotification;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import org.json.JSONObject;

/** Synthetic alarm: a short tap has an explicit exit; a swipe cannot stop or snooze it. */
public final class RingGuidanceTest extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();Context c=getTargetContext();RingActivity screen=null;
        String id="qa-ring-guidance";
        try{
            AccessibilityManager access=(AccessibilityManager)c.getSystemService(Context.ACCESSIBILITY_SERVICE);
            if(access!=null&&access.isEnabled())throw new Exception("Use a test emulator with accessibility services off");
            JSONObject alarm=new JSONObject().put("id",id).put("title","Synthetic guidance alarm")
                .put("kind","manual").put("at",System.currentTimeMillis());
            c.startForegroundService(new Intent(c,AlarmService.class).setAction("RING").putExtra("alarm",alarm.toString()));
            long deadline=SystemClock.elapsedRealtime()+8_000L;
            while(!Store.object(c,"ringing").has(id)&&SystemClock.elapsedRealtime()<deadline)Thread.sleep(50);
            if(!Store.object(c,"ringing").has(id))throw new Exception("Synthetic alarm did not ring");
            int snoozesBefore=Store.object(c,"snoozes").length();
            screen=(RingActivity)startActivitySync(new Intent(c,RingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            ViewGroup layout=(ViewGroup)((ViewGroup)screen.findViewById(android.R.id.content)).getChildAt(0);
            Button awake=(Button)layout.getChildAt(1);RingActivity ring=screen;
            runOnMainSync(awake::performClick);
            AlertDialog dialog=screen.holdInstructionsDialog();
            if(!screen.holdInstructionsShowing()||dialog.getButton(AlertDialog.BUTTON_POSITIVE)==null||
                !"OK, keep ringing".contentEquals(dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText()))
                throw new Exception("Short-tap guidance lacks an OK button");
            runOnMainSync(()->dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick());
            deadline=SystemClock.elapsedRealtime()+1_000L;
            while(screen.holdInstructionsShowing()&&SystemClock.elapsedRealtime()<deadline)Thread.sleep(20);
            if(screen.holdInstructionsShowing())throw new Exception("OK did not close guidance");
            runOnMainSync(()->{
                long now=SystemClock.uptimeMillis();float y=awake.getHeight()/2f;
                for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP}){
                    float x=action==MotionEvent.ACTION_DOWN?awake.getWidth()/2f:awake.getWidth()+20f;
                    MotionEvent event=MotionEvent.obtain(now,now+40,action,x,y,0);
                    awake.dispatchTouchEvent(event);event.recycle();
                }
                ring.onBackPressed();
            });
            if(screen.isFinishing()||!Store.object(c,"ringing").has(id)||Store.object(c,"snoozes").length()!=snoozesBefore)
                throw new Exception("Swipe or Back stopped or snoozed the alarm");
            runOnMainSync(()->{ring.moveTaskToBack(true);});
            Thread.sleep(500);
            if(!Store.object(c,"ringing").has(id)||Store.object(c,"snoozes").length()!=snoozesBefore)
                throw new Exception("Leaving the screen stopped or snoozed the alarm");
            boolean ongoing=false;
            for(StatusBarNotification notification:c.getSystemService(NotificationManager.class).getActiveNotifications())
                if(notification.getId()==1&&(notification.getNotification().flags&Notification.FLAG_ONGOING_EVENT)!=0)ongoing=true;
            if(!ongoing)throw new Exception("Ringing notification disappeared after leaving the screen");
            result.putString("summary","Short-tap OK, swipe, Back, background ring, and ongoing notification passed");finish(Activity.RESULT_OK,result);
        }catch(Throwable error){result.putString("error",error.toString());finish(Activity.RESULT_CANCELED,result);}
        finally{c.startService(new Intent(c,AlarmService.class).setAction("STOP"));if(screen!=null&&!screen.isFinishing()){RingActivity last=screen;runOnMainSync(last::finish);}}
    }
}
