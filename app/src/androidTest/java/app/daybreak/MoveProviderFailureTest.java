package app.daybreak;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Exercises the actual MainActivity output path through a failing content provider. */
public final class MoveProviderFailureTest extends Instrumentation {
    private WebView web;
    private WebView findWeb(View view){if(view instanceof WebView)return (WebView)view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){WebView found=findWeb(((ViewGroup)view).getChildAt(i));if(found!=null)return found;}return null;}
    private String js(String code)throws Exception{CountDownLatch done=new CountDownLatch(1);String[] value={null};runOnMainSync(()->web.evaluateJavascript(code,result->{value[0]=result;done.countDown();}));if(!done.await(5,TimeUnit.SECONDS))throw new Exception("Web view did not respond");return value[0];}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle out=new Bundle();try{
        Context c=getTargetContext();if(!"ranchu".equals(Build.HARDWARE))throw new Exception("Disposable emulator only");
        Store.p(c).edit().clear().commit();
        Activity activity=startActivitySync(new android.content.Intent(c,MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
        runOnMainSync(()->web=findWeb(activity.findViewById(android.R.id.content)));
        long ready=System.currentTimeMillis()+15000;while(!"true".equals(js("typeof window.moveExportFailed==='function'"))&&System.currentTimeMillis()<ready)Thread.sleep(100);
        if(!"true".equals(js("typeof window.moveExportFailed==='function'")))throw new Exception("Move feedback UI did not load");
        Method export=MainActivity.class.getDeclaredMethod("exportMove",android.net.Uri.class,char[].class);export.setAccessible(true);
        StringBuilder calendar=new StringBuilder("BEGIN:VCALENDAR\r\nVERSION:2.0\r\n");
        for(int i=0;i<12000;i++)calendar.append("X-SYNTHETIC:").append(i).append("-ABCDEFGHIJKLMNOPQRSTUVWXYZ\r\n");
        calendar.append("END:VCALENDAR\r\n");Store.cache(c,calendar.toString());
        for(android.net.Uri uri:new android.net.Uri[]{FailingMoveDocumentProvider.FULL,FailingMoveDocumentProvider.PIPE}){
            Store.p(c).edit().putString("error","").commit();char[] password="synthetic-provider-password".toCharArray();
            runOnMainSync(()->{try{export.invoke(activity,uri,password);}catch(Exception error){throw new RuntimeException(error);}});
            long until=System.currentTimeMillis()+15000;String message="";
            while(message.isEmpty()&&System.currentTimeMillis()<until){Thread.sleep(100);message=Store.p(c).getString("error","");}
            if(message.isEmpty())throw new Exception("Provider failure was not shown for "+uri.getLastPathSegment());
            for(char value:password)if(value!=0)throw new Exception("Export password was retained");
            if(!message.contains("Move package was not saved")||!message.contains("incomplete"))throw new Exception("Error did not explain the incomplete move package: "+message);
            long shown=System.currentTimeMillis()+5000;while(!"true".equals(js("!document.getElementById('moveFeedback').hidden&&document.getElementById('moveFeedback').textContent.includes('incomplete')"))&&System.currentTimeMillis()<shown)Thread.sleep(100);
            if(!"true".equals(js("!document.getElementById('moveFeedback').hidden")))throw new Exception("Backup failure was not shown beside the move controls");
            if(!calendar.toString().equals(Store.cache(c)))throw new Exception("Failed export changed the source calendar");
            if(FailingMoveDocumentProvider.PIPE.equals(uri)&&c.getContentResolver().call(uri,"status",null,null).getInt("received")<=0)
                throw new Exception("Provider disconnected before receiving any backup bytes");
        }
        char[] successPassword="synthetic-provider-password".toCharArray();
        runOnMainSync(()->{try{export.invoke(activity,FailingMoveDocumentProvider.OK,successPassword);}catch(Exception error){throw new RuntimeException(error);}});
        long saved=System.currentTimeMillis()+15000;
        while(c.getContentResolver().call(FailingMoveDocumentProvider.OK,"status",null,null).getLong("savedBytes")<1024&&System.currentTimeMillis()<saved)Thread.sleep(100);
        if(c.getContentResolver().call(FailingMoveDocumentProvider.OK,"status",null,null).getLong("savedBytes")<1024)throw new Exception("Real provider did not receive a valid-size package");
        long cleared=System.currentTimeMillis()+5000;
        while(!"true".equals(js("document.getElementById('moveFeedback').hidden"))&&System.currentTimeMillis()<cleared)Thread.sleep(100);
        if(!"true".equals(js("document.getElementById('moveFeedback').hidden")))throw new Exception("Success did not clear the visible backup failure");
        long storeCleared=System.currentTimeMillis()+5000;
        while(!Store.p(c).getString("error","").isEmpty()&&System.currentTimeMillis()<storeCleared)Thread.sleep(100);
        if(!Store.p(c).getString("error","").isEmpty())throw new Exception("Success left a stale backup error in app state");
        for(char value:successPassword)if(value!=0)throw new Exception("Successful export retained password");
        out.putString("summary","Denied provider and mid-write disconnect showed persistent guidance; subsequent provider export cleared error; passwords cleared");finish(Activity.RESULT_OK,out);
    }catch(Throwable error){out.putString("error",error.toString());finish(Activity.RESULT_CANCELED,out);}}
}
