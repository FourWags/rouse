package app.daybreak;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.service.notification.StatusBarNotification;
import android.app.Instrumentation;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import org.json.JSONObject;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** A pending snooze can end without removing any other scheduled alarm. */
public final class EndSnoozeTest extends Instrumentation {
    private static final String FIRST="snooze:qa-end-one";
    private static final String SECOND="snooze:qa-end-two";
    private static final String PERSONAL="manual:qa-keep";
    private MainActivity screen;
    private WebView web;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Context c=getTargetContext();Bundle result=new Bundle();
        try{
            if(!Scheduler.allowed(c))throw new Exception("Exact alarm permission is unavailable on this test emulator");
            long at=System.currentTimeMillis()+600_000L;
            JSONObject snoozes=Store.object(c,"snoozes");
            snoozes.put(FIRST,alarm(FIRST,"snooze",at));
            snoozes.put(SECOND,alarm(SECOND,"snooze",at+60_000L));
            Store.put(c,"snoozes",snoozes);
            JSONObject manual=Store.object(c,"manual");manual.put(PERSONAL,alarm(PERSONAL,"manual",at+120_000L));Store.put(c,"manual",manual);
            Sync.run(c,false);
            if(!Store.object(c,"scheduled").has(FIRST)||!Store.object(c,"scheduled").has(SECOND)||!Store.object(c,"scheduled").has(PERSONAL))
                throw new Exception("Synthetic alarms were not all scheduled");
            screen=(MainActivity)startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            long deadline=System.currentTimeMillis()+10_000L;
            while(web==null&&System.currentTimeMillis()<deadline){runOnMainSync(()->web=find(screen.findViewById(android.R.id.content)));Thread.sleep(50);}
            if(web==null)throw new Exception("Rouse home screen did not load");
            while(!"true".equals(js("!!st&&!!st.snoozes['"+FIRST+"']&&!$('snoozeBanner').hidden&&!$('endSnooze').disabled"))&&System.currentTimeMillis()<deadline)Thread.sleep(50);
            if(!"true".equals(js("$('endSnooze').textContent.includes('End snooze')&&!!document.querySelector('#manualList [data-alarm-action=endSnooze]')")))
                throw new Exception("Snooze ending controls are missing from the home screen");
            if(!"true".equals(js("$('readinessCard').textContent.includes('Next alarm check')&&$('readinessChecks').textContent.includes('Scheduled with Android')&&$('readinessChecks').textContent.includes('Phone alarm volume')")))
                throw new Exception("Unified readiness card is missing scheduled alarm and phone volume");
            Notification notice=activeSnooze(c);
            if(notice==null||notice.visibility!=Notification.VISIBILITY_PRIVATE||notice.publicVersion==null
                    ||notice.actions==null||notice.actions.length!=1||!"I’m up".contentEquals(notice.actions[0].title)
                    ||String.valueOf(notice.extras.getCharSequence(Notification.EXTRA_TEXT)).contains("Synthetic"))
                throw new Exception("Snooze notice is missing, public, or exposes alarm details");
            notice.actions[0].actionIntent.send();
            deadline=System.currentTimeMillis()+5000L;
            while(!"true".equals(js("$('confirmDialog').open&&$('cancelConfirm').textContent==='Keep snoozing'"))&&System.currentTimeMillis()<deadline)Thread.sleep(50);
            if(!"true".equals(js("$('confirmDialog').open&&$('yesConfirm').textContent==='End snooze'")))
                throw new Exception("Notification action did not open End snooze confirmation");
            if(!Store.object(c,"snoozes").has(FIRST))throw new Exception("Notification action canceled snooze without confirmation");
            js("$('cancelConfirm').click()");
            js("$('endSnooze').click()");
            if(!"true".equals(js("$('confirmDialog').open&&$('cancelConfirm').textContent==='Keep snoozing'&&$('yesConfirm').textContent==='End snooze'")))
                throw new Exception("End snooze confirmation is missing");
            js("$('cancelConfirm').click()");
            if(!Store.object(c,"snoozes").has(FIRST))throw new Exception("Keep snoozing canceled the alarm");
            js("$('endSnooze').click();$('yesConfirm').click()");
            deadline=System.currentTimeMillis()+8_000L;
            while((Store.object(c,"snoozes").has(FIRST)||!"true".equals(js("!busy")))&&System.currentTimeMillis()<deadline)Thread.sleep(50);
            if(Store.object(c,"snoozes").has(FIRST)||Store.object(c,"scheduled").has(FIRST)||operationExists(c,FIRST))
                throw new Exception("Ended snooze could still ring");
            if(!Store.object(c,"snoozes").has(SECOND)||!Store.object(c,"scheduled").has(SECOND)||!operationExists(c,SECOND)
                    ||!Store.object(c,"manual").has(PERSONAL)||!Store.object(c,"scheduled").has(PERSONAL)||!operationExists(c,PERSONAL))
                throw new Exception("Ending snooze changed another alarm");
            if(Scheduler.endSnooze(c,FIRST)||Scheduler.endSnooze(c,PERSONAL))throw new Exception("Stale or non-snooze ID was accepted");
            Notification remaining=activeSnooze(c);
            if(remaining==null||!String.valueOf(remaining.extras.getCharSequence(Notification.EXTRA_TEXT)).contains("Snoozed until"))
                throw new Exception("Snooze notice did not move to the remaining alarm");
            if(!Scheduler.endSnooze(c,SECOND)||activeSnooze(c)!=null)throw new Exception("Snooze notice remained after final snooze ended");
            if(!Store.object(c,"scheduled").has(PERSONAL))throw new Exception("Ending final snooze removed personal alarm");
            result.putString("summary","Home screen confirmation ended one snooze; cancel preserved it; second snooze and personal alarm stayed scheduled; stale IDs rejected");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable error){result.putString("error",error.toString());finish(Activity.RESULT_CANCELED,result);}
        finally{
            if(screen!=null&&!screen.isFinishing()){MainActivity last=screen;runOnMainSync(last::finish);}
            try{
                JSONObject snoozes=Store.object(c,"snoozes");snoozes.remove(FIRST);snoozes.remove(SECOND);Store.put(c,"snoozes",snoozes);
                JSONObject manual=Store.object(c,"manual");manual.remove(PERSONAL);Store.put(c,"manual",manual);
                for(String id:new String[]{FIRST,SECOND,PERSONAL})Scheduler.cancel(c,id);
                Sync.run(c,false);
            }catch(Exception ignored){}
        }
    }
    private Notification activeSnooze(Context c){
        NotificationManager manager=c.getSystemService(NotificationManager.class);
        for(StatusBarNotification posted:manager.getActiveNotifications())if(posted.getId()==205)return posted.getNotification();
        return null;
    }
    private JSONObject alarm(String id,String kind,long at)throws Exception{
        return new JSONObject().put("id",id).put("kind",kind).put("title","Synthetic test alarm").put("at",at);
    }
    private boolean operationExists(Context c,String id)throws Exception{
        Method operation=Scheduler.class.getDeclaredMethod("operation",Context.class,String.class,int.class);
        operation.setAccessible(true);
        return operation.invoke(null,c,id,PendingIntent.FLAG_NO_CREATE)!=null;
    }
    private WebView find(View view){
        if(view instanceof WebView)return (WebView)view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int n=0;n<group.getChildCount();n++){WebView found=find(group.getChildAt(n));if(found!=null)return found;}}
        return null;
    }
    private String js(String script)throws Exception{
        CountDownLatch latch=new CountDownLatch(1);String[] result={null};
        runOnMainSync(()->web.evaluateJavascript(script,value->{result[0]=value;latch.countDown();}));
        if(!latch.await(5,TimeUnit.SECONDS))throw new Exception("Rouse home screen did not respond");
        return result[0];
    }
}
