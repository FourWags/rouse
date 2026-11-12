package app.daybreak;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Verifies the in-app guide can be found and opens Android's accessibility setup. */
public final class AccessibilityGuideTest extends Instrumentation {
    private WebView web;
    private WebView findWeb(View v) {
        if (v instanceof WebView) return (WebView) v;
        if (v instanceof ViewGroup) for (int i=0;i<((ViewGroup)v).getChildCount();i++) {
            WebView found=findWeb(((ViewGroup)v).getChildAt(i));
            if (found!=null) return found;
        }
        return null;
    }
    private String js(String script) throws Exception {
        CountDownLatch done=new CountDownLatch(1);
        String[] result={null};
        runOnMainSync(()->web.evaluateJavascript(script,value->{result[0]=value;done.countDown();}));
        if (!done.await(5,TimeUnit.SECONDS)) throw new Exception("Guide JavaScript timed out");
        return result[0];
    }
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result=new Bundle();
        try {
            Activity activity=startActivitySync(new android.content.Intent(getTargetContext(),MainActivity.class)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
            runOnMainSync(()->web=findWeb(activity.findViewById(android.R.id.content)));
            long deadline=System.currentTimeMillis()+15000;
            while (!"true".equals(js("typeof st==='object'&&st!==null&&!!st.settings"))) {
                if (System.currentTimeMillis()>deadline) throw new Exception("WagRise did not load");
                Thread.sleep(100);
            }
            String open="page('settings');document.querySelector('[data-settings-group=accessibility]').click();"
                +"!document.querySelector('[data-group=accessibility]').hidden"
                +"&&document.querySelector('[data-settings-group=accessibility]').getAttribute('aria-pressed')==='true'"
                +"&&document.querySelector('[data-group=wake]').hidden";
            if (!"true".equals(js(open))) throw new Exception("Accessibility section was not selected");
            String guide="(()=>{const e=document.querySelector('[data-group=accessibility]');"
                +"const t=e.textContent;return t.includes('TalkBack')&&t.includes('Switch Access')"
                +"&&t.includes('Volume Down')&&t.includes('Volume Up')"
                +"&&t.includes('Select again to stop')&&t.includes('Stop test alarm')"
                +"&&document.getElementById('openAccessibilitySettings').getBoundingClientRect().height>=44})()";
            if (!"true".equals(js(guide))) throw new Exception("Guide content or setup button is missing");
            ActivityMonitor monitor=addMonitor(new IntentFilter(Settings.ACTION_ACCESSIBILITY_SETTINGS),null,true);
            js("document.getElementById('openAccessibilitySettings').click()");
            deadline=System.currentTimeMillis()+5000;
            while (monitor.getHits()==0&&System.currentTimeMillis()<deadline) Thread.sleep(50);
            removeMonitor(monitor);
            if (monitor.getHits()==0) throw new Exception("Android accessibility settings did not open");
            result.putString("summary","Guide navigation, instructions and Android settings action passed");
            finish(Activity.RESULT_OK,result);
        } catch (Throwable error) {
            result.putString("error",error.toString());
            finish(Activity.RESULT_CANCELED,result);
        }
    }
}
