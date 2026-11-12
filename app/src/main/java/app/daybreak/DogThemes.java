package app.daybreak;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import org.json.JSONArray;
import org.json.JSONObject;

/** Bounded, offline catalog of the reviewed breed and coat artwork. */
final class DogThemes {
    private static Map<String,JSONObject> catalog;
    private DogThemes(){}
    private static synchronized Map<String,JSONObject> catalog(Context context){
        if(catalog!=null)return catalog;
        try(InputStream stream=context.getAssets().open("dog-themes.json")){
            ByteArrayOutputStream buffer=new ByteArrayOutputStream();byte[] chunk=new byte[4096];int count;
            while((count=stream.read(chunk))!=-1)buffer.write(chunk,0,count);
            byte[] bytes=buffer.toByteArray();
            JSONArray items=new JSONArray(new String(bytes,StandardCharsets.UTF_8));
            if(items.length()!=84)throw new IllegalStateException("Dog theme catalog incomplete");
            Map<String,JSONObject> found=new HashMap<>();
            for(int i=0;i<items.length();i++){
                JSONObject entry=items.getJSONObject(i);String id=entry.getString("id");
                if(!id.matches("dog_[a-z0-9_]+")||found.put(id,entry)!=null)throw new IllegalStateException("Invalid dog theme catalog");
            }
            catalog=found;return found;
        }catch(Exception error){catalog=Collections.emptyMap();return catalog;}
    }
    static boolean has(Context context,String id){return id!=null&&id.startsWith("dog_")&&catalog(context).containsKey(id);}
    static String launcherIcon(Context context,String id){
        if(!has(context,id))return null;
        int suffix=id.lastIndexOf('_');
        return suffix>4?"dog_howl_"+id.substring(4,suffix):null;
    }
    static Appearance appearance(Context context,String id){
        if(id==null||!id.startsWith("dog_"))return null;
        JSONObject entry=catalog(context).get(id);if(entry==null)return null;
        try{return Appearance.dogPalette(id,entry.optString("breed")+" · "+entry.optString("coat"),
            entry.optString("bg"),entry.optString("action"),entry.optString("accent"),entry.optString("ink"));}
        catch(RuntimeException invalid){return null;}
    }
    static String selected(Context context){
        String saved=Store.p(context).getString("dogCompanion",null);
        if("none".equals(saved))return null;
        if(saved!=null&&saved.startsWith("dog_poodle_")){
            String updated=saved.replace("dog_poodle_","dog_goldendoodle_");
            if(has(context,updated)){
                Store.p(context).edit().putString("dogCompanion",updated).apply();
                saved=updated;
            }
        }
        if(has(context,saved))return saved;
        String theme=Appearance.current(context).id;
        if(has(context,theme))return theme;
        if("shiba_apricot".equals(theme))return "dog_shiba_0";
        if("shiba_black_tan".equals(theme))return "dog_shiba_1";
        if("shiba_sesame".equals(theme))return "dog_shiba_2";
        return null;
    }
    static Bitmap pose(Context context,String id,String kind){
        if(!has(context,id)||!("curl".equals(kind)||"yawn".equals(kind)))return null;
        try(InputStream stream=context.getAssets().open("dog-poses/"+id+"-"+kind+".webp")){
            Bitmap image=BitmapFactory.decodeStream(stream);
            if(image==null||image.getWidth()>800||image.getHeight()>800){if(image!=null)image.recycle();return null;}
            return image;
        }catch(Exception error){return null;}
    }
}
