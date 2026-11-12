package app.daybreak;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.graphics.Bitmap;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Synthetic emulator check of the actual Appearance picker and saved choice. */
public final class DogThemePickerTest extends Instrumentation {
    private WebView web;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private WebView findWeb(View view){if(view instanceof WebView)return (WebView)view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){WebView found=findWeb(((ViewGroup)view).getChildAt(i));if(found!=null)return found;}return null;}
    private String js(String script)throws Exception{CountDownLatch latch=new CountDownLatch(1);String[] result={null};runOnMainSync(()->web.evaluateJavascript(script,value->{result[0]=value;latch.countDown();}));if(!latch.await(5,TimeUnit.SECONDS))throw new Exception("WebView timeout");return result[0];}
    @Override public void onStart(){
        Bundle result=new Bundle();Context c=getTargetContext();Activity screen=null;
        var prefs=Store.p(c);String oldTheme=prefs.getString("appearance",null),oldDog=prefs.getString("dogCompanion",null),oldIcon=prefs.getString("iconDesign",null);
        try{
            FourWagsWakeIcon.skip(c);
            screen=startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Activity current=screen;runOnMainSync(()->web=findWeb(current.findViewById(android.R.id.content)));
            long end=System.currentTimeMillis()+15000;
            while(System.currentTimeMillis()<end&&!"true".equals(js("typeof st==='object'&&st!==null&&!!st.settings")))Thread.sleep(100);
            if(!"true".equals(js("typeof st==='object'&&st!==null&&!!st.settings")))throw new Exception("App did not load");
            js("page('appearance');$('dogBreed').value='Goldendoodle';$('dogBreed').dispatchEvent(new Event('change',{bubbles:true}))");
            if(!"\"3\"".equals(js("String(document.querySelectorAll('.dog-option[data-dog^=dog_goldendoodle]').length)")))throw new Exception("Goldendoodle did not replace Poodle with three coats");
            if(!"false".equals(js("[...$('dogBreed').options].some(option=>option.value==='Poodle')")))throw new Exception("Poodle is still selectable");
            js("page('appearance');$('dogBreed').value='Husky';$('dogBreed').dispatchEvent(new Event('change',{bubbles:true}));$('dogThemePanel').scrollIntoView({block:'start'})");
            String count=js("String(document.querySelectorAll('.dog-option[data-dog^=dog_husky]').length)");
            if(!"\"3\"".equals(count))throw new Exception("Husky coat choices missing: "+count);
            if(!"true".equals(js("$('dogSnoozePreview').getAttribute('src')==='dog-poses/dog_husky_0-curl.webp'&&$('dogAwakePreview').getAttribute('src')==='dog-poses/dog_husky_0-yawn.webp'")))throw new Exception("Large alarm-pose previews missing");
            if(!"true".equals(js("getComputedStyle(document.querySelector('[data-dog=dog_husky_0] .coat-swatch i:nth-child(2)')).backgroundColor==='rgb(232, 230, 222)'")))throw new Exception("Coat colors did not render under the app content policy");
            js("document.querySelector('[data-dog=dog_husky_0]').click()");
            end=System.currentTimeMillis()+5000;
            while(System.currentTimeMillis()<end&&!"dog_husky_0".equals(prefs.getString("dogCompanion","")))Thread.sleep(100);
            if(!"dog_husky_0".equals(Appearance.current(c).id)||!"dog_husky_0".equals(prefs.getString("dogCompanion","")))throw new Exception("Dog and palette did not save together");
            if(!"true".equals(js("document.querySelector('[data-dog=dog_husky_0]').getAttribute('aria-pressed')==='true'")))throw new Exception("Selected dog not visibly marked");
            js("$('dogPreview').scrollIntoView({block:'center'})");Thread.sleep(5200);
            Bitmap shot=getUiAutomation().takeScreenshot();if(shot==null)throw new Exception("Could not capture picker");
            File output=new File(c.getExternalFilesDir(null),"dog-picker-synthetic.png");
            try(FileOutputStream stream=new FileOutputStream(output)){shot.compress(Bitmap.CompressFormat.PNG,100,stream);}shot.recycle();
            js("$('dogNone').click()");
            end=System.currentTimeMillis()+5000;
            while(System.currentTimeMillis()<end&&!"none".equals(prefs.getString("dogCompanion","")))Thread.sleep(100);
            if(!"none".equals(prefs.getString("dogCompanion",""))||!"dog_husky_0".equals(Appearance.current(c).id))throw new Exception("Hiding art changed the color theme or failed to save");
            js("document.querySelector('[data-icon=wr_p1]').click()");
            end=System.currentTimeMillis()+5000;
            while(System.currentTimeMillis()<end&&!"wr_p1".equals(Appearance.icon(c)))Thread.sleep(100);
            if(!"wr_p1".equals(Appearance.icon(c))||!"none".equals(prefs.getString("dogCompanion","")))throw new Exception("Changing icon re-enabled hidden dog art");
            if(!"true".equals(js("document.querySelectorAll('.theme-option').length===19&&document.querySelector('#appearance .panel').id==='dogThemePanel'")))throw new Exception("Dog chooser is not first or classic colors are unavailable");
            result.putString("summary","Dog chooser is first; Husky dropdown shows three coats and two large poses; coat saves palette; classic colors stay available with basic icons; hide-art survives icon changes");finish(Activity.RESULT_OK,result);
        }catch(Throwable error){result.putString("error",error.toString());finish(Activity.RESULT_CANCELED,result);}
        finally{if(screen!=null&&!screen.isFinishing()){Activity finalScreen=screen;runOnMainSync(finalScreen::finish);}
            var edit=prefs.edit();if(oldTheme==null)edit.remove("appearance");else edit.putString("appearance",oldTheme);if(oldDog==null)edit.remove("dogCompanion");else edit.putString("dogCompanion",oldDog);if(oldIcon==null)edit.remove("iconDesign");else edit.putString("iconDesign",oldIcon);edit.commit();Appearance.syncLauncher(c);}
    }
}
