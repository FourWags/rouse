package app.daybreak;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.ViewGroup;
import android.widget.Button;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.json.JSONArray;
import org.json.JSONObject;

/** Synthetic, silent emulator check of artwork, button identity and move-package settings. */
public final class DogAlarmArtworkTest extends Instrumentation {
    private String requested="dog_shiba_0";
    @Override public void onCreate(Bundle args){super.onCreate(args);if(args!=null)requested=args.getString("dog",requested);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();Context c=getTargetContext();RingActivity screen=null;
        var prefs=Store.p(c);String previousTheme=prefs.getString("appearance",null),previousDog=prefs.getString("dogCompanion",null),previousCurrent=prefs.getString("current",null);
        try{
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(InputStream source=c.getAssets().open("dog-themes.json")){byte[] chunk=new byte[4096];int count;while((count=source.read(chunk))!=-1)bytes.write(chunk,0,count);}
            JSONArray rows=new JSONArray(new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8));
            if(rows.length()!=240)throw new Exception("Expected 240 dog coats");
            if(!DogThemes.has(c,requested))throw new Exception("Unknown requested dog");
            for(int i=0;i<rows.length();i++){
                String id=rows.getJSONObject(i).getString("id");
                Appearance palette=DogThemes.appearance(c,id);
                if(palette==null)throw new Exception("Missing palette "+id);
                ThemeColors colors=palette.customColors;
                if(ThemeColors.contrast(colors.text,colors.surface)<4.5||ThemeColors.contrast(colors.actionText,colors.primary)<4.5)throw new Exception("Unreadable dog palette "+id);
                for(String kind:new String[]{"curl","yawn"}){
                    Bitmap image=DogThemes.pose(c,id,kind);
                    if(image==null||image.getWidth()<100||image.getHeight()<90)throw new Exception("Missing pose "+id+" "+kind);
                    image.recycle();
                }
            }
            Appearance.current(c); // Complete the existing first-run palette migration before simulating a saved choice.
            if(!prefs.edit().putString("appearance","dog_poodle_0").putString("dogCompanion","dog_poodle_0").commit())throw new Exception("Could not set prior Poodle choice");
            if(!"dog_goldendoodle_0".equals(Appearance.current(c).id)||!"dog_goldendoodle_0".equals(DogThemes.selected(c)))throw new Exception("Prior Poodle choice did not migrate to Goldendoodle");
            if(!prefs.edit().putString("appearance",requested).putString("dogCompanion",requested).putString("current",new JSONObject().put("title","Synthetic dog preview").toString()).commit())throw new Exception("Could not set synthetic state");
            String exported=SettingsTransfer.exportJson(c);SettingsTransfer.validateJson(c,exported);
            if(!new JSONObject(exported).getJSONObject("settings").getString("dogCompanion").equals(requested))throw new Exception("Move package omitted dog choice");
            screen=(RingActivity)startActivitySync(new Intent(c,RingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Thread.sleep(1000);
            ViewGroup layout=(ViewGroup)((ViewGroup)screen.findViewById(android.R.id.content)).getChildAt(0);
            if(layout.getChildCount()!=2||!(layout.getChildAt(0) instanceof Button)||!(layout.getChildAt(1) instanceof Button))throw new Exception("Alarm buttons changed");
            Button snooze=(Button)layout.getChildAt(0),awake=(Button)layout.getChildAt(1);
            boolean textPriority=c.getResources().getConfiguration().fontScale>=1.8f&&c.getResources().getDisplayMetrics().heightPixels/2<Math.round(390*c.getResources().getDisplayMetrics().density);
            for(Button button:new Button[]{snooze,awake}){
                if(button.getHeight()<200||button.getContentDescription()==null||button.getContentDescription().length()==0)throw new Exception("Alarm action or accessibility label missing");
                Spanned text=(Spanned)button.getText();ImageSpan[] images=text.getSpans(0,text.length(),ImageSpan.class);
                if(textPriority?images.length!=0:images.length!=1||images[0].getDrawable().getBounds().height()<70)throw new Exception("Pose layout does not match text size");
            }
            Bitmap shot=getUiAutomation().takeScreenshot();
            if(shot==null)throw new Exception("Could not capture synthetic alarm");
            File output=new File(c.getExternalFilesDir(null),"dog-alarm-"+requested+".png");
            try(FileOutputStream stream=new FileOutputStream(output)){shot.compress(Bitmap.CompressFormat.PNG,100,stream);}shot.recycle();
            result.putString("summary",textPriority?"240 palettes and 480 poses decode; 200% text uses uncluttered two-button fallback; move settings valid; synthetic screenshot saved":"240 palettes and 480 poses decode; two large accessible buttons retain art; move settings valid; synthetic screenshot saved");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable error){result.putString("error",error.toString());finish(Activity.RESULT_CANCELED,result);}
        finally{
            if(screen!=null&&!screen.isFinishing()){RingActivity last=screen;runOnMainSync(last::finish);}
            var edit=prefs.edit();if(previousTheme==null)edit.remove("appearance");else edit.putString("appearance",previousTheme);
            if(previousDog==null)edit.remove("dogCompanion");else edit.putString("dogCompanion",previousDog);
            if(previousCurrent==null)edit.remove("current");else edit.putString("current",previousCurrent);edit.commit();
        }
    }
}
