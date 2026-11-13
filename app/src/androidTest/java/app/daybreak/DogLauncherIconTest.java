package app.daybreak;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import org.json.JSONArray;
import org.json.JSONObject;

/** Disposable-emulator check of all breed icons as actual launcher aliases. */
public final class DogLauncherIconTest extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();Context c=getTargetContext();var prefs=Store.p(c);
        String oldTheme=prefs.getString("appearance",null),oldDog=prefs.getString("dogCompanion",null),oldIcon=prefs.getString("iconDesign",null);
        try{
            byte[] source;try(InputStream in=c.getAssets().open("dog-themes.json")){source=in.readAllBytes();}
            JSONArray catalog=new JSONArray(new String(source,StandardCharsets.UTF_8));
            LinkedHashMap<String,String> breeds=new LinkedHashMap<>();
            for(int i=0;i<catalog.length();i++){
                JSONObject row=catalog.getJSONObject(i);String id=row.getString("id");
                String icon=DogThemes.launcherIcon(c,id);
                if(icon==null||!Appearance.validIcon(c,icon))throw new AssertionError("Invalid breed icon mapping: "+id);
                breeds.putIfAbsent(icon,id);
            }
            if(catalog.length()!=240||breeds.size()!=80)throw new AssertionError("Expected 240 coats and 80 dog icons.");
            PackageManager pm=c.getPackageManager();int checked=0;
            for(var pair:breeds.entrySet()){
                String icon=pair.getKey(),id=pair.getValue();
                Appearance.choose(c,id);
                if(!icon.equals(Appearance.icon(c))||!id.equals(Appearance.current(c).id))throw new AssertionError("Breed icon and coat did not save together: "+id);
                var launchers=pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(c.getPackageName()),0);
                if(launchers.size()!=1)throw new AssertionError("Expected exactly one home-screen icon after selecting "+id);
                ResolveInfo selected=launchers.get(0);
                String component=c.getPackageName()+"."+Appearance.current(c).launcher(icon);
                if(!component.equals(selected.activityInfo.name)||!"WagRise".contentEquals(selected.loadLabel(pm)))throw new AssertionError("Wrong launcher alias or label for "+id);
                if(selected.activityInfo.getIconResource()!=Appearance.current(c).iconResource(c,icon)||selected.loadIcon(pm)==null)throw new AssertionError("Missing icon art for "+id);
                checked++;
            }
            boolean poodlePackaged=true;
            try{pm.getActivityInfo(new ComponentName(c.getPackageName(),"app.daybreak.DogHowl_poodle"),PackageManager.MATCH_DISABLED_COMPONENTS);}
            catch(PackageManager.NameNotFoundException expected){poodlePackaged=false;}
            if(poodlePackaged)throw new AssertionError("Retired Poodle icon is still packaged.");
            result.putString("summary",checked+" distinct dog launcher icons; 240 coats map to the correct dog; one WagRise launcher remains enabled; Poodle absent");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable error){result.putString("error",error.toString());finish(Activity.RESULT_CANCELED,result);}
        finally{
            var editor=prefs.edit();if(oldTheme==null)editor.remove("appearance");else editor.putString("appearance",oldTheme);
            if(oldDog==null)editor.remove("dogCompanion");else editor.putString("dogCompanion",oldDog);
            if(oldIcon==null)editor.remove("iconDesign");else editor.putString("iconDesign",oldIcon);
            editor.commit();Appearance.syncLauncher(c);
        }
    }
}
