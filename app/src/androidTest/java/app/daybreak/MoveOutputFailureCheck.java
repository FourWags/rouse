package app.daybreak;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import org.json.JSONObject;
import java.io.*;
import java.util.*;

/** Disposable emulator only: failed document writes must not change source data or restore. */
public final class MoveOutputFailureCheck extends Instrumentation {
    private static final class FailingOutput extends OutputStream {
        final ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        final int limit;final String reason;
        FailingOutput(int limit,String reason){this.limit=limit;this.reason=reason;}
        @Override public void write(int b)throws IOException{if(bytes.size()>=limit)throw new IOException(reason);bytes.write(b);}
        @Override public void write(byte[] b,int off,int len)throws IOException{
            int allowed=Math.max(0,limit-bytes.size());
            if(allowed>0)bytes.write(b,off,Math.min(len,allowed));
            if(len>allowed)throw new IOException(reason);
        }
    }
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle out=new Bundle();try{
        Context c=getTargetContext();if(!"ranchu".equals(Build.HARDWARE))throw new Exception("Disposable emulator only");
        Store.p(c).edit().clear().commit();Store.cache(c,"BEGIN:VCALENDAR\r\nVERSION:2.0\r\nEND:VCALENDAR\r\n");
        Store.p(c).edit().putString("source","Imported calendar file").commit();
        String id="manual:qa-output-failure";Store.put(c,"manual",new JSONObject().put(id,new JSONObject().put("id",id).put("kind","manual").put("title","Synthetic alarm").put("at",System.currentTimeMillis()+86400000L)));
        String cache=Store.cache(c);
        char[] password="synthetic-output-failure-password".toCharArray();
        ArrayList<byte[]> partials=new ArrayList<>();
        for(FailingOutput target:new FailingOutput[]{new FailingOutput(64,"Document provider disconnected"),new FailingOutput(256,"No space left on device")}){
            boolean failed=false;
            try{MovePackage.exportTo(c,target,password);}catch(IOException expected){failed=true;}
            if(!failed||target.bytes.size()!=target.limit)throw new Exception("Failed output was not exercised");
            if(!"Imported calendar file".equals(Store.p(c).getString("source",""))||!cache.equals(Store.cache(c))||!Store.object(c,"manual").has(id))throw new Exception("Export failure changed calendar or personal alarm data");
            partials.add(target.bytes.toByteArray());
        }
        Arrays.fill(password,'\0');
        Store.p(c).edit().clear().commit();Store.cache(c,"");Store.put(c,"manual",new JSONObject());
        for(byte[] partial:partials){
            boolean rejected=false;try{MovePackage.importFrom(c,new ByteArrayInputStream(partial),"synthetic-output-failure-password".toCharArray());}catch(Exception expected){rejected=true;}
            if(!rejected||Store.object(c,"manual").length()!=0||!Store.p(c).getString("source","").isEmpty())throw new Exception("Incomplete package was accepted");
        }
        File[] files=c.getCacheDir().listFiles();if(files!=null)for(File f:files)if(f.getName().startsWith("rouse-move-"))throw new Exception("Private staging file remained");
        out.putString("summary","Interrupted provider and no-space writes failed; source stayed intact; partial packages rejected; staging cleaned");finish(Activity.RESULT_OK,out);
    }catch(Throwable e){out.putString("error",e.toString());finish(Activity.RESULT_CANCELED,out);}}
}
