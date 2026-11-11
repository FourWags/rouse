package app.daybreak;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.IOException;

/** Synthetic external document provider. Only installed on an isolated emulator. */
public final class FailingMoveDocumentProvider extends ContentProvider {
    public static final Uri FULL=Uri.parse("content://app.daybreak.test.failing-move/full");
    public static final Uri PIPE=Uri.parse("content://app.daybreak.test.failing-move/pipe");
    public static final Uri OK=Uri.parse("content://app.daybreak.test.failing-move/ok");
    private volatile int received;
    @Override public boolean onCreate(){return true;}
    @Override public Bundle call(String method,String arg,Bundle extras){Bundle result=new Bundle();result.putInt("received",received);result.putLong("savedBytes",new File(getContext().getCacheDir(),"synthetic-move-success.rouse").length());return result;}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
        if(!mode.startsWith("w"))throw new FileNotFoundException("Synthetic document is write-only");
        if(FULL.equals(uri))return ParcelFileDescriptor.open(new File("/dev/full"),ParcelFileDescriptor.MODE_WRITE_ONLY);
        if(OK.equals(uri))return ParcelFileDescriptor.open(new File(getContext().getCacheDir(),"synthetic-move-success.rouse"),ParcelFileDescriptor.MODE_CREATE|ParcelFileDescriptor.MODE_TRUNCATE|ParcelFileDescriptor.MODE_WRITE_ONLY);
        if(!PIPE.equals(uri))throw new FileNotFoundException("Unknown synthetic document");
        try{
            ParcelFileDescriptor[] ends=ParcelFileDescriptor.createReliablePipe();
            Thread reader=new Thread(()->{
                try(FileInputStream in=new FileInputStream(ends[0].getFileDescriptor())){
                    byte[] buffer=new byte[4096];int count,total=0;
                    while(total<16384&&(count=in.read(buffer))!=-1){total+=count;received=total;}
                    ends[0].closeWithError("Document provider disconnected");
                }catch(IOException ignored){try{ends[0].close();}catch(IOException alsoIgnored){}}
            },"synthetic-move-provider");reader.setDaemon(true);reader.start();
            return ends[1];
        }catch(IOException failure){throw new FileNotFoundException("Could not create synthetic document pipe");}
    }
    @Override public String getType(Uri uri){return "application/octet-stream";}
    @Override public Cursor query(Uri uri,String[] p,String s,String[] a,String o){return null;}
    @Override public Uri insert(Uri uri,ContentValues v){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String s,String[] a){return 0;}
    @Override public int update(Uri uri,ContentValues v,String s,String[] a){return 0;}
}
