package io.freemytv;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small, cached schedule; no live scores or polling while another app is in front. */
final class RacingAgenda {
    static final String[] NAMES = {"FERRARI / F1", "PALMEIRAS", "BOSTON CELTICS", "FERRARI / WEC"};
    static final String[] SOURCES = {"Jolpica F1", "ESPN · Brasileirão", "ESPN · NBA", "FIA WEC · calendário 2027"};
    static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    static final class Event {
        final String title, detail; final long at; final boolean dateOnly;
        String homeId="", awayId="",homeLogo="",awayLogo="";
        Event(String title, String detail, long at, boolean dateOnly) { this.title=title; this.detail=detail; this.at=at; this.dateOnly=dateOnly; }
    }
    private final Context context;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<List<Event>> events = new ArrayList<>();
    private final long[] fetched = new long[4];
    private final boolean[] failed = new boolean[4];
    private boolean busy, closed;
    private long attempted;
    volatile String raceName="Próximo GP", circuitName="Consultando circuito", circuitId="";
    RacingAgenda(Context c) {
        context=c.getApplicationContext();
        for(int i=0;i<4;i++) events.add(new ArrayList<>());
        loadWec();
        for(int i=0;i<3;i++) {
            try { JSONObject cache=new JSONObject(read(new FileInputStream(cache(i))));
                fetched[i]=cache.optLong("at");
                parse(i,cache.getString("body"));
            } catch(Exception ignored) {}
        }
    }
    synchronized List<Event> upcoming(int i) {
        List<Event> out=new ArrayList<>(); long now=System.currentTimeMillis();
        for(Event e:events.get(i)) if(e.at+(e.dateOnly?86400000L:0)>now) out.add(e);
        out.sort(Comparator.comparingLong(e->e.at));
        return out;
    }
    synchronized String status(int i) {
        if(i==3) return "Agenda oficial · revisão 16/11/2026";
        if(fetched[i]==0) return failed[i]?"Fonte indisponível · tentar mais tarde":"Consultando agenda…";
        return (failed[i]?"Salvo · ":"Atualizado · ")+new java.text.SimpleDateFormat("dd/MM HH:mm",new Locale("pt","BR")).format(new Date(fetched[i]));
    }
    void refresh(Runnable changed) {
        if(closed||busy||System.currentTimeMillis()-attempted<30*60000L) return;
        boolean due=false; for(int i=0;i<3;i++) if(System.currentTimeMillis()-fetched[i]>6*3600000L) due=true;
        if(!due) return;
        attempted=System.currentTimeMillis(); busy=true;
        worker.execute(()->{
            for(int i=0;i<3;i++) {
                try {
                    String url=i==0?"https://api.jolpi.ca/ergast/f1/current.json":i==1?
                        "https://site.api.espn.com/apis/site/v2/sports/soccer/bra.1/teams/2029/schedule?fixture=true":
                        "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/teams/2/schedule?fixture=true";
                    String body=get(url); parse(i,body);
                    if(i==0&&upcoming(0).isEmpty()) {
                        int year=java.time.ZonedDateTime.now(ZONE).getYear()+1;
                        body=get("https://api.jolpi.ca/ergast/f1/"+year+".json"); parse(0,body);
                    }
                    long now=System.currentTimeMillis();
                    synchronized(this){fetched[i]=now;failed[i]=false;}
                    JSONObject cache=new JSONObject().put("at",now).put("body",body);
                    try(FileOutputStream out=new FileOutputStream(cache(i))){out.write(cache.toString().getBytes(StandardCharsets.UTF_8));}
                } catch(Exception e){synchronized(this){failed[i]=true;} android.util.Log.w("RacingAgenda","source "+i+": "+e.getClass().getSimpleName());}
            }
            main.post(()->{busy=false;if(!closed)changed.run();});
        });
    }
    private synchronized void parse(int i,String body) throws Exception {
        List<Event> list=new ArrayList<>(); JSONObject root=new JSONObject(body);
        if(i==0){
            JSONArray races=root.getJSONObject("MRData").getJSONObject("RaceTable").getJSONArray("Races");
            boolean nextFound=false;
            for(int j=0;j<races.length();j++){
                JSONObject r=races.getJSONObject(j); String time=r.optString("time");
                String name=r.getString("raceName").replace("Grand Prix","GP");
                name=name.replace("Brazilian GP","GP de São Paulo").replace("Las Vegas GP","GP de Las Vegas").replace("Qatar GP","GP do Catar").replace("Abu Dhabi GP","GP de Abu Dhabi");
                long raceAt=timestamp(r.getString("date"),time);
                if(!nextFound && raceAt>System.currentTimeMillis()){
                    nextFound=true;raceName=name;
                    JSONObject circuit=r.getJSONObject("Circuit");circuitName=circuit.getString("circuitName");circuitId=circuit.getString("circuitId");
                }
                String[][] sessions={{"FirstPractice","Treino livre 1"},{"SecondPractice","Treino livre 2"},{"ThirdPractice","Treino livre 3"},{"SprintQualifying","Classificação Sprint"},{"Sprint","Sprint"},{"Qualifying","Classificação"}};
                for(String[] session:sessions){JSONObject s=r.optJSONObject(session[0]);if(s==null||!s.has("date"))continue;
                    String t=s.optString("time");list.add(new Event(session[1],name,timestamp(s.getString("date"),t),t.isEmpty()));}
                list.add(new Event("Corrida",name,raceAt,time.isEmpty()));
            }
        }else{
            JSONArray es=root.getJSONArray("events");
            for(int j=0;j<es.length();j++){
                JSONObject e=es.getJSONObject(j); JSONArray cs=e.getJSONArray("competitions"); if(cs.length()==0)continue;
                JSONObject competition=cs.getJSONObject(0);
                JSONObject type=competition.optJSONObject("status"); if(type!=null){type=type.optJSONObject("type");if(type!=null&&type.optBoolean("completed"))continue;}
                JSONArray teams=competition.getJSONArray("competitors"); String home="",away="",homeId="",awayId="",homeLogo="",awayLogo="";
                for(int k=0;k<teams.length();k++){
                    JSONObject t=teams.getJSONObject(k);JSONObject team=t.getJSONObject("team");
                    String name=team.optString("shortDisplayName",team.optString("displayName"));
                    JSONArray logos=team.optJSONArray("logos");String logo=logos!=null&&logos.length()>0?logos.getJSONObject(0).optString("href"):team.optString("logo");
                    if("home".equals(t.optString("homeAway"))){home=name;homeId=team.optString("id");homeLogo=logo;}else{away=name;awayId=team.optString("id");awayLogo=logo;}
                }
                String raw=e.getString("date");long at=Instant.parse(raw.replaceAll("T(\\d\\d):(\\d\\d)Z$","T$1:$2:00Z")).toEpochMilli();
                boolean tbd=competition.optBoolean("timeValid",true)==false;
                Event event=new Event(home+" × "+away,i==1?"Brasileirão · futebol":"NBA · temporada regular",at,tbd);event.homeId=homeId;event.awayId=awayId;event.homeLogo=homeLogo;event.awayLogo=awayLogo;list.add(event);
            }
        }
        list.sort(Comparator.comparingLong(e->e.at));events.set(i,list);
    }
    private void loadWec(){
        // Race dates, not session start times. Official calendar checked on 2026-11-16.
        String[][] rounds={{"2027-03-27","Qatar 1812 km"},{"2027-04-11","6 Horas de Imola"},{"2027-04-25","6 Horas de Silverstone"},{"2027-05-15","6 Horas de Spa"},{"2027-06-12","24 Horas de Le Mans"},{"2027-07-11","6 Horas de São Paulo"},{"2027-09-12","Lone Star Le Mans"},{"2027-09-26","6 Horas de Fuji"},{"2027-11-06","8 Horas do Bahrein"}};
        for(String[] r:rounds)events.get(3).add(new Event(r[1],"Ferrari Hypercar · horário a confirmar",timestamp(r[0],""),true));
    }
    private static long timestamp(String date,String time){return time.isEmpty()?LocalDate.parse(date).atStartOfDay(ZONE).toInstant().toEpochMilli():Instant.parse(date+"T"+time).toEpochMilli();}
    private File cache(int i){return new File(context.getFilesDir(),"racing-agenda-"+i+".json");}
    private static String read(InputStream stream)throws Exception{try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>3000000)throw new IOException("response too large");out.write(b,0,n);}return out.toString("UTF-8");}}
    private static String get(String address)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(12000);c.setRequestProperty("User-Agent","FreeMyTV-Racing/1.0 personal dashboard");
        try{if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());return read(c.getInputStream());}finally{c.disconnect();}
    }
    void close(){closed=true;worker.shutdownNow();}
}
