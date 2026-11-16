package io.freemytv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import org.json.*;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** Public sports tables, a sample YouTube channel, small image cache and published circuit geometry. */
final class DashboardData {
    static final class Standing {
        final String id,name,value;final int rank;
        Standing(String id,String name,String value,int rank){this.id=id;this.name=name;this.value=value;this.rank=rank;}
    }
    static final class Video {
        final String id,title,channel;final long published;
        Video(String id,String title){this(id,title,"Formula 1",0);}
        Video(String id,String title,String channel,long published){this.id=id;this.title=title;this.channel=channel;this.published=published;}
    }
    private final Context c;
    private final Runnable changed;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Map<String,Bitmap> images=Collections.synchronizedMap(new LinkedHashMap<String,Bitmap>(48,.75f,true){@Override protected boolean removeEldestEntry(Map.Entry<String,Bitmap> eldest){return size()>40;}});
    private final Map<String,String> logoUrls=new ConcurrentHashMap<>();
    private final Map<String,Bitmap> shieldRasters=Collections.synchronizedMap(new LinkedHashMap<String,Bitmap>(48,.75f,true){@Override protected boolean removeEldestEntry(Map.Entry<String,Bitmap> eldest){return size()>48;}});
    private final Set<String> pending=Collections.synchronizedSet(new HashSet<>());
    volatile List<Standing> pal=new ArrayList<>(),nba=new ArrayList<>();
    volatile List<Video> videos=new ArrayList<>();
    volatile String tableDate="16/11",videoDate="16/11";
    volatile float[] track;
    volatile String trackId="vegas",trackLabel="Las Vegas",trackLength="6,201 km";
    volatile boolean closed;
    private long attempted;
    private final FavoriteYouTube favorites;
    DashboardData(Context c,Runnable changed){
        this.c=c.getApplicationContext();this.changed=changed;
        try{
            JSONObject boot=new JSONObject(text(c.getAssets().open("racing/boot.json")));
            pal=bootRows(boot.getJSONArray("pal"));nba=bootRows(boot.getJSONArray("nba"));
            JSONArray vs=boot.getJSONArray("videos");for(int i=0;i<vs.length();i++){JSONObject v=vs.getJSONObject(i);videos.add(new Video(v.getString("id"),v.getString("title")));}
            track=parseTrack(text(c.getAssets().open("racing/vegas.geojson")));
        }catch(Exception e){android.util.Log.w("DashboardData","bundled data: "+e.getClass().getSimpleName());}
        for(String[] cache:new String[][]{{"table-pal.json","pal"},{"table-nba.json","nba"}})try{String body=text(new FileInputStream(file(cache[0])));if(cache[1].equals("pal"))pal=parseStandings(body,false);else nba=parseStandings(body,true);}catch(Exception ignored){}
        favorites=new FavoriteYouTube(c,list->{videos=list;notifyChanged();});videos=favorites.snapshot();
    }
    int channelCount(){return favorites.count();}
    void refresh(){
        favorites.refresh();
        if(closed||System.currentTimeMillis()-attempted<6*3600000L)return;attempted=System.currentTimeMillis();
        worker.execute(()->{
            for(int i=0;i<2;i++)try{
                String url=i==0?"https://site.api.espn.com/apis/v2/sports/soccer/bra.1/standings":i==1?"https://site.api.espn.com/apis/v2/sports/basketball/nba/standings":"https://www.youtube.com/feeds/videos.xml?channel_id=UCB_qr75-ydFVKSF9Dmo6izg";
                String body=new String(bytes(url),"UTF-8");
                if(i==0)pal=parseStandings(body,false);else if(i==1)nba=parseStandings(body,true);else videos=parseVideos(body);
                save(i==0?"table-pal.json":i==1?"table-nba.json":"videos.xml",body);
                String date=new java.text.SimpleDateFormat("dd/MM HH:mm",new Locale("pt","BR")).format(new Date());
                if(i<2)tableDate=date;else videoDate=date;
                notifyChanged();
            }catch(Exception e){android.util.Log.w("DashboardData","feed "+i+": "+e.getClass().getSimpleName());}
        });
    }
    void ensureTrack(String id){
        if(id==null||id.isEmpty()||id.equals(trackId)||closed)return;
        String code=trackCode(id);if(code==null)return;
        String key="track:"+id;if(!pending.add(key))return;
        worker.execute(()->{try{
            String body=new String(bytes("https://raw.githubusercontent.com/bacinger/f1-circuits/master/circuits/"+code+".geojson"),"UTF-8");
            float[] geometry=parseTrack(body);JSONObject feature=new JSONObject(body).getJSONArray("features").getJSONObject(0);
            track=geometry;trackId=id;trackLabel=feature.getJSONObject("properties").optString("Location",id);
            trackLength=String.format(new Locale("pt","BR"),"%.3f km",feature.getJSONObject("properties").optDouble("length")/1000.0);
            notifyChanged();
        }catch(Exception e){android.util.Log.w("DashboardData","track unavailable");}finally{pending.remove(key);}});
    }
    static String trackCode(String id){
        String[][] map={{"vegas","us-2023"},{"losail","qa-2004"},{"yas_marina","ae-2009"},{"albert_park","au-1953"},{"bahrain","bh-2002"},{"jeddah","sa-2021"},{"shanghai","cn-2004"},{"suzuka","jp-1962"},{"miami","us-2022"},{"imola","it-1953"},{"monaco","mc-1929"},{"catalunya","es-1991"},{"villeneuve","ca-1978"},{"red_bull_ring","at-1969"},{"silverstone","gb-1948"},{"spa","be-1925"},{"hungaroring","hu-1986"},{"zandvoort","nl-1948"},{"monza","it-1922"},{"baku","az-2016"},{"marina_bay","sg-2008"},{"americas","us-2012"},{"rodriguez","mx-1962"},{"interlagos","br-1940"}};
        for(String[] p:map)if(p[0].equals(id))return p[1];return null;
    }
    Bitmap logo(boolean basketball,String id){
        if(id==null||id.isEmpty())return null;
        String league=basketball?"nba":"soccer";
        String nbaCode=id.equals("2")?"bos":id.equals("19")?"orl":id.equals("20")?"phi":id.equals("8")?"det":id;
        return image("logo-"+league+"-"+id+".png",logoUrls.containsKey(league+id)?logoUrls.get(league+id):"https://a.espncdn.com/i/teamlogos/"+league+"/500/"+(basketball?nbaCode:id)+".png",640);
    }
    Bitmap shield(Bitmap source,int pixels){
        String key="shield:"+System.identityHashCode(source)+":"+pixels;Bitmap raster=shieldRasters.get(key);if(raster!=null||closed)return raster;
        if(pending.add(key))worker.execute(()->{try{Bitmap prepared=ShieldRasterizer.reduce(source,pixels);if(!closed)shieldRasters.put(key,prepared);notifyChanged();}catch(RuntimeException e){android.util.Log.w("DashboardData","shield resize unavailable");}finally{pending.remove(key);}});
        return null;
    }
    Bitmap logo(boolean basketball,String id,String url){if(url!=null&&url.startsWith("https://a.espncdn.com/"))logoUrls.put((basketball?"nba":"soccer")+id,url);return logo(basketball,id);}
    Bitmap thumbnail(Video v){return image("video-"+v.id+".jpg","https://i.ytimg.com/vi/"+v.id+"/mqdefault.jpg",320);}
    private Bitmap image(String key,String url,int max){
        Bitmap found=images.get(key);if(found!=null)return found;
        // Bundled shields are tiny and decoded once, then reused.
        if(pending.add(key))worker.execute(()->{try{
            byte[] raw;try{raw=all(c.getAssets().open("racing/"+key));}catch(IOException e){raw=bytes(url);}
            BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(raw,0,raw.length,o);
            o.inSampleSize=1;while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>max*2)o.inSampleSize*=2;o.inJustDecodeBounds=false;
            Bitmap bitmap=BitmapFactory.decodeByteArray(raw,0,raw.length,o);
            if(bitmap!=null&&key.startsWith("logo-"))bitmap=prepareShield(bitmap);
            if(bitmap!=null&&!closed)images.put(key,bitmap);notifyChanged();
        }catch(Exception ignored){}finally{pending.remove(key);}});
        return null;
    }
    private static Bitmap prepareShield(Bitmap original){
        int w=original.getWidth(),h=original.getHeight(),left=w,top=h,right=-1,bottom=-1;int[] pixels=new int[w*h];original.getPixels(pixels,0,w,0,0,w,h);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)if((pixels[y*w+x]>>>24)>16){left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);}
        if(right<left||bottom<top)return original;
        Bitmap cropped=Bitmap.createBitmap(original,left,top,right-left+1,bottom-top+1);
        return cropped;
    }
    private List<Standing> bootRows(JSONArray rows)throws Exception{
        List<Standing> out=new ArrayList<>();for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);out.add(new Standing(r.getString("id"),r.getString("name"),r.getString("value"),r.getInt("rank")));}return out;
    }
    private List<Standing> parseStandings(String body,boolean basketball)throws Exception{
        JSONArray children=new JSONObject(body).getJSONArray("children");JSONObject group=children.getJSONObject(0);
        if(basketball)for(int i=0;i<children.length();i++)if(children.getJSONObject(i).optString("name").contains("Eastern"))group=children.getJSONObject(i);
        JSONArray entries=group.getJSONObject("standings").getJSONArray("entries");List<Standing> all=new ArrayList<>();
        for(int i=0;i<entries.length();i++){
            JSONObject e=entries.getJSONObject(i),t=e.getJSONObject("team");JSONArray logos=t.optJSONArray("logos");if(logos!=null&&logos.length()>0){String url=logos.getJSONObject(0).optString("href");if(url.startsWith("https://a.espncdn.com/"))logoUrls.put((basketball?"nba":"soccer")+t.getString("id"),url);}JSONArray stats=e.getJSONArray("stats");Map<String,String> values=new HashMap<>();
            for(int j=0;j<stats.length();j++){JSONObject s=stats.getJSONObject(j);values.put(s.getString("name"),s.optString("displayValue"));}
            int rank=Integer.parseInt(values.get(basketball?"playoffSeed":"rank"));String value=basketball?values.get("wins")+"–"+values.get("losses"):values.get("points");
            all.add(new Standing(t.getString("id"),t.optString("shortDisplayName",t.getString("displayName")),value,rank));
        }
        all.sort(Comparator.comparingInt(s->s.rank));int favorite=0;for(int i=0;i<all.size();i++)if(all.get(i).id.equals(basketball?"2":"2029"))favorite=i;
        int from=Math.max(0,Math.min(favorite-1,all.size()-3));return new ArrayList<>(all.subList(from,Math.min(from+3,all.size())));
    }
    private List<Video> parseVideos(String body)throws Exception{
        DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);f.setExpandEntityReferences(false);
        Document doc=f.newDocumentBuilder().parse(new ByteArrayInputStream(body.getBytes("UTF-8")));NodeList entries=doc.getElementsByTagNameNS("http://www.w3.org/2005/Atom","entry");List<Video> out=new ArrayList<>();
        for(int i=0;i<Math.min(4,entries.getLength());i++){Element e=(Element)entries.item(i);String id=e.getElementsByTagNameNS("http://www.youtube.com/xml/schemas/2015","videoId").item(0).getTextContent();String title=e.getElementsByTagNameNS("http://www.w3.org/2005/Atom","title").item(0).getTextContent();out.add(new Video(id,title));}
        if(out.isEmpty())throw new IOException("empty feed");return out;
    }
    static float[] parseTrack(String body)throws Exception{
        JSONObject feature=new JSONObject(body).getJSONArray("features").getJSONObject(0);JSONObject g=feature.getJSONObject("geometry");JSONArray coords=g.getJSONArray("coordinates");if(g.getString("type").equals("MultiLineString"))coords=coords.getJSONArray(0);
        double lat=coords.getJSONArray(0).getDouble(1),cos=Math.cos(Math.toRadians(lat));double minX=Double.MAX_VALUE,minY=Double.MAX_VALUE,maxX=-Double.MAX_VALUE,maxY=-Double.MAX_VALUE;
        for(int i=0;i<coords.length();i++){JSONArray v=coords.getJSONArray(i);double x=v.getDouble(0)*cos,y=-v.getDouble(1);minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);}
        float[] points=new float[coords.length()*2];double size=Math.max(maxX-minX,maxY-minY);
        for(int i=0;i<coords.length();i++){JSONArray v=coords.getJSONArray(i);points[i*2]=(float)((v.getDouble(0)*cos-(minX+maxX)/2)/size);points[i*2+1]=(float)((-v.getDouble(1)-(minY+maxY)/2)/size);}return points;
    }
    private void notifyChanged(){main.post(()->{if(!closed)changed.run();});}
    private File file(String name){return new File(c.getFilesDir(),"dashboard-"+name);}
    private void save(String name,String body)throws Exception{try(FileOutputStream out=new FileOutputStream(file(name))){out.write(body.getBytes("UTF-8"));}}
    private static byte[] bytes(String url)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setRequestProperty("User-Agent","Mozilla/5.0 FreeMyTVPersonal/2");try{if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());return all(c.getInputStream());}finally{c.disconnect();}}
    private static String text(InputStream in)throws Exception{return new String(all(in),"UTF-8");}
    private static byte[] all(InputStream input)throws Exception{try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>4000000)throw new IOException("response too large");out.write(b,0,n);}return out.toByteArray();}}
    void close(){closed=true;favorites.close();worker.shutdownNow();images.clear();shieldRasters.clear();}
}
