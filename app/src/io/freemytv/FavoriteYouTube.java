package io.freemytv;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.*;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Public uploads from the owner's chosen channels. Per-channel offline cache, newest first. */
final class FavoriteYouTube {
    private final Context context;
    private final Consumer<List<DashboardData.Video>> changed;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService network=Executors.newFixedThreadPool(3);
    private final Map<String,List<DashboardData.Video>> feeds=new HashMap<>();
    private final List<String> ids=new ArrayList<>();
    private boolean closed;private long attempted;
    FavoriteYouTube(Context c,Consumer<List<DashboardData.Video>> changed){
        context=c.getApplicationContext();this.changed=changed;
        try{
            JSONArray channels=new JSONArray(read(context.getAssets().open("racing/youtube-channels.json")));
            for(int i=0;i<channels.length();i++){
                JSONObject channel=channels.getJSONObject(i);String id=channel.getString("id");if(!id.matches("UC[\\w-]{22}"))continue;ids.add(id);
                List<DashboardData.Video> videos=new ArrayList<>();JSONArray boot=channel.getJSONArray("videos");
                for(int j=0;j<boot.length();j++){JSONObject v=boot.getJSONObject(j);videos.add(new DashboardData.Video(v.getString("id"),v.getString("title"),v.optString("channel",channel.getString("name")),v.getLong("published")));}
                try{videos=parse(read(new FileInputStream(file(id))));}catch(Exception ignored){}
                feeds.put(id,videos);
            }
        }catch(Exception e){android.util.Log.w("FavoriteYouTube","channel configuration unavailable");}
    }
    int count(){return ids.size();}
    synchronized List<DashboardData.Video> snapshot(){
        List<DashboardData.Video> merged=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(String id:ids)for(DashboardData.Video v:feeds.get(id))if(seen.add(v.id))merged.add(v);
        merged.sort((a,b)->Long.compare(b.published,a.published));return merged;
    }
    void refresh(){
        if(closed||System.currentTimeMillis()-attempted<30*60000L)return;attempted=System.currentTimeMillis();
        for(String id:ids)network.execute(()->{
            try{
                HttpURLConnection connection=(HttpURLConnection)new URL("https://www.youtube.com/feeds/videos.xml?channel_id="+id).openConnection();
                connection.setConnectTimeout(10000);connection.setReadTimeout(10000);connection.setRequestProperty("User-Agent","Mozilla/5.0 FreeMyTVPersonal/3");String body;
                try{if(connection.getResponseCode()!=200)throw new IOException("HTTP "+connection.getResponseCode());body=read(connection.getInputStream());}finally{connection.disconnect();}
                List<DashboardData.Video> parsed=parse(body);
                synchronized(this){feeds.put(id,parsed);}
                try(FileOutputStream out=new FileOutputStream(file(id))){out.write(body.getBytes("UTF-8"));}
                main.post(()->{if(!closed)changed.accept(snapshot());});
            }catch(Exception e){android.util.Log.w("FavoriteYouTube","channel "+id+": "+e.getClass().getSimpleName());}
        });
    }
    private List<DashboardData.Video> parse(String body)throws Exception{
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);factory.setExpandEntityReferences(false);
        Document doc=factory.newDocumentBuilder().parse(new ByteArrayInputStream(body.getBytes("UTF-8")));
        NodeList entries=doc.getElementsByTagNameNS("http://www.w3.org/2005/Atom","entry");List<DashboardData.Video> out=new ArrayList<>();
        for(int i=0;i<Math.min(6,entries.getLength());i++){
            Element e=(Element)entries.item(i);String id=value(e,"http://www.youtube.com/xml/schemas/2015","videoId"),title=value(e,"http://www.w3.org/2005/Atom","title"),published=value(e,"http://www.w3.org/2005/Atom","published");
            Element author=(Element)e.getElementsByTagNameNS("http://www.w3.org/2005/Atom","author").item(0);String channel=author==null?"YouTube":value(author,"http://www.w3.org/2005/Atom","name");
            if(!id.matches("[\\w-]{11}"))continue;out.add(new DashboardData.Video(id,title,channel,Instant.parse(published).toEpochMilli()));
        }
        if(out.isEmpty())throw new IOException("empty feed");return out;
    }
    private String value(Element e,String ns,String name){Node node=e.getElementsByTagNameNS(ns,name).item(0);return node==null?"":node.getTextContent();}
    private File file(String id){return new File(context.getFilesDir(),"favorite-youtube-"+id+".xml");}
    private static String read(InputStream input)throws Exception{try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>1500000)throw new IOException("response too large");out.write(b,0,n);}return out.toString("UTF-8");}}
    void close(){closed=true;network.shutdownNow();}
}
