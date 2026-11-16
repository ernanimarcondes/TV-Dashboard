package io.freemytv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.MotionEvent;
import java.text.SimpleDateFormat;
import java.util.*;

/** Sports-first personal home, drawn at a logical 1920×1080 and scaled to the TV's actual UI. */
final class DashboardHomeView extends RacingHomeView {
    private static final int BG=0xFF000000,PANEL=0xFF211A2A,WHITE=0xFFF9F3FF,MUTED=0xFFC5B6D1,PINK=0xFFFF2EAE,PURPLE=0xFFBE51FF,LINE=0xFF4B2B61;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF temp=new RectF();private final Path path=new Path();
    private final Typeface normal,strong;
    private final RectF[] regions=new RectF[13];
    private final DashboardData feeds;
    private final SpotifyPanel spotify;
    private int focused=2,videoPage=0;private float zoom=1,ox,oy;
    private boolean dashboardRunning;private float videoSlide;private int videoTarget,videoDirection=1;
    private android.animation.ValueAnimator videoAnimator;
    private static final long VIDEO_INTERVAL=12000L;
    private final Runnable autoVideo=new Runnable(){@Override public void run(){if(!dashboardRunning)return;if(focused!=4&&hasWindowFocus()&&isShown()&&feeds.videos.size()>1)changeVideo(1);else scheduleVideo();}};
    private final SimpleDateFormat time=format("HH:mm"),date=format("dd 'de' MMMM"),weekday=format("EEEE"),sessionDate=format("EEE dd/MM"),hour=format("HH:mm");
    private final Runnable refreshFeeds=new Runnable(){@Override public void run(){feeds.refresh();postDelayed(this,10*60000L);}};
    DashboardHomeView(Activity a){
        super(a);normal=Ui.medium;Typeface game;try{game=Typeface.createFromAsset(a.getAssets(),"fonts/rajdhani/Rajdhani-Bold.ttf");}catch(RuntimeException e){game=Ui.displaySemibold;}strong=game;
        paint.setSubpixelText(true);paint.setHinting(Paint.HINTING_ON);paint.setDither(true);
        feeds=new DashboardData(a,()->invalidate());spotify=new SpotifyPanel(a,()->invalidate());
        for(int i=0;i<regions.length;i++)regions[i]=new RectF();
        regions[0].set(52,118,492,500);regions[1].set(52,522,492,904);regions[2].set(514,118,1286,904);
        regions[3].set(1308,118,1868,330);regions[4].set(1308,352,1868,632);regions[5].setEmpty();regions[6].set(1308,654,1868,904);
        for(int i=0;i<6;i++)regions[7+i].set(52+i*306,942,52+i*306+286,1010);
        setContentDescription("Painel pessoal. Palmeiras e Celtics à esquerda. Fórmula 1 ao centro. Spotify, YouTube e clima à direita. Atalhos na parte inferior. Navegue com as setas e OK.");
    }
    private static SimpleDateFormat format(String pattern){SimpleDateFormat f=new SimpleDateFormat(pattern,new Locale("pt","BR"));f.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));return f;}
    @Override void resume(){super.resume();dashboardRunning=true;feeds.refresh();removeCallbacks(refreshFeeds);postDelayed(refreshFeeds,10*60000L);spotify.start();scheduleVideo();}
    @Override void pause(){dashboardRunning=false;removeCallbacks(autoVideo);if(videoAnimator!=null)videoAnimator.end();removeCallbacks(refreshFeeds);spotify.stop();super.pause();}
    @Override void destroy(){dashboardRunning=false;removeCallbacks(autoVideo);if(videoAnimator!=null)videoAnimator.end();removeCallbacks(refreshFeeds);spotify.stop();feeds.close();super.destroy();}
    @Override void reset(boolean animate){if(videoAnimator!=null)videoAnimator.end();focused=7;videoPage=0;scheduleVideo();invalidate();}
    @Override public void onData(){invalidate();}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){zoom=Math.min(w/1920f,h/1080f);ox=(w-1920*zoom)/2;oy=(h-1080*zoom)/2;}
    @Override protected void onDraw(Canvas c){
        c.drawColor(BG);c.save();c.translate(ox,oy);c.scale(zoom,zoom);paint.setShader(null);
        Date now=new Date();
        String clockText=time.format(now);paint.setTypeface(Ui.light);paint.setTextSize(72);float clockWidth=paint.measureText(clockText),dateRight=1868-clockWidth-26;
        paint.setColor(WHITE);paint.setTextAlign(Paint.Align.RIGHT);c.drawText(clockText,1868,85,paint);
        paint.setTypeface(Ui.medium);paint.setTextSize(17);paint.setColor(PINK);c.drawText(weekday.format(now).toUpperCase(new Locale("pt","BR")),dateRight,49,paint);
        paint.setTypeface(Ui.regular);paint.setTextSize(27);paint.setColor(WHITE);c.drawText(date.format(now),dateRight,84,paint);paint.setTextAlign(Paint.Align.LEFT);
        sports(c,0,false);sports(c,1,true);formula(c);music(c);youtube(c);weather(c);
        String[] dock={"YouTube","Prime Video","Spotify","Aplicativos","TV / HDMI","Ajustes"};
        for(int i=0;i<dock.length;i++){panel(c,regions[7+i],focused==7+i);text(c,dock[i],regions[7+i].left+26,985,23,focused==7+i?WHITE:MUTED,true);text(c,"›",regions[7+i].right-32,986,27,PINK,false);}
        text(c,"Horários de Brasília",54,1052,18,MUTED,false);text(c,"SETAS para navegar   ·   OK para abrir",1410,1052,18,MUTED,false);
        c.restore();
    }
    private void sports(Canvas c,int id,boolean nba){
        RectF b=regions[id];float x=b.left,y=b.top;panel(c,b,focused==id);
        shield(c,feeds.logo(nba,nba?"2":"2029"),x+24,y+20,64);
        text(c,nba?"Boston Celtics":"Palmeiras",x+105,y+49,29,WHITE,true);text(c,nba?"NBA · Conferência Leste":"Brasileirão · Série A",x+105,y+80,18,MUTED,false);
        List<RacingAgenda.Event> upcoming=agenda.upcoming(nba?2:1);RacingAgenda.Event next=upcoming.isEmpty()?null:upcoming.get(0);
        text(c,"PRÓXIMO JOGO",x+25,y+126,16,PINK,true);
        if(next==null){text(c,"Aguardando programação",x+25,y+167,23,WHITE,false);}else{
            shield(c,feeds.logo(nba,next.homeId,next.homeLogo),x+25,y+145,35);shield(c,feeds.logo(nba,next.awayId,next.awayLogo),x+72,y+145,35);
            text(c,fit(next.title,23,291),x+122,y+172,23,WHITE,true);
            text(c,eventTime(next),x+25,y+211,22,WHITE,false);
        }
        line(c,x+25,y+235,x+415,y+235,LINE,1);
        text(c,nba?"LESTE":"CLASSIFICAÇÃO",x+25,y+264,16,PURPLE,true);text(c,nba?"V–D":"PTS",x+358,y+264,16,MUTED,true);
        List<DashboardData.Standing> table=nba?feeds.nba:feeds.pal;
        for(int i=0;i<Math.min(3,table.size());i++){
            DashboardData.Standing s=table.get(i);float baseline=y+296+i*35;boolean favorite=s.id.equals(nba?"2":"2029");
            if(favorite){paint.setColor(0xFF422050);c.drawRoundRect(x+16,baseline-24,x+424,baseline+7,5,5,paint);}
            text(c,String.valueOf(s.rank),x+26,baseline,20,favorite?PINK:MUTED,true);shield(c,feeds.logo(nba,s.id),x+61,baseline-23,27);
            text(c,fit(s.name,21,235),x+99,baseline,21,favorite?WHITE:MUTED,favorite);text(c,s.value,x+357,baseline,20,WHITE,true);
        }
    }
    private void formula(Canvas c){
        RectF b=regions[2];float x=b.left,y=b.top;panel(c,b,focused==2);
        text(c,"FÓRMULA 1",x+30,y+43,18,PINK,true);
        text(c,fit(agenda.raceName,43,710),x+30,y+103,43,WHITE,true);
        text(c,fit(agenda.circuitName,21,710),x+30,y+139,21,MUTED,false);
        feeds.ensureTrack(agenda.circuitId);
        boolean matched=agenda.circuitId.equals(feeds.trackId);
        if(matched&&feeds.track!=null){
            float[] coords=feeds.track;path.reset();float centerX=x+386,centerY=y+289;
            float minX=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,minY=Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
            for(int i=0;i<coords.length;i+=2){float rx=-coords[i+1],ry=coords[i];minX=Math.min(minX,rx);maxX=Math.max(maxX,rx);minY=Math.min(minY,ry);maxY=Math.max(maxY,ry);}
            float size=Math.min(674f/Math.max(.01f,maxX-minX),238f/Math.max(.01f,maxY-minY));
            for(int i=0;i<coords.length;i+=2){float px=centerX+(-coords[i+1]-(minX+maxX)/2)*size,py=centerY+(coords[i]-(minY+maxY)/2)*size;if(i==0)path.moveTo(px,py);else path.lineTo(px,py);}
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeWidth(16);paint.setColor(0xFF5C2379);c.drawPath(path,paint);paint.setStrokeWidth(4);paint.setColor(PINK);c.drawPath(path,paint);paint.setStrokeCap(Paint.Cap.BUTT);paint.setStyle(Paint.Style.FILL);
            float sx=centerX+(-coords[1]-(minX+maxX)/2)*size,sy=centerY+(coords[0]-(minY+maxY)/2)*size;paint.setColor(WHITE);c.drawCircle(sx,sy,5,paint);
            text(c,feeds.trackLength,x+30,y+418,20,MUTED,false);
        }else text(c,"Traçado em atualização",x+245,y+290,23,MUTED,false);
        List<RacingAgenda.Event> all=agenda.upcoming(0);List<RacingAgenda.Event> weekend=new ArrayList<>();for(RacingAgenda.Event e:all)if(e.detail.equals(agenda.raceName))weekend.add(e);
        temp.set(x+25,y+438,x+747,y+514);paint.setColor(0xFF422052);c.drawRoundRect(temp,12,12,paint);
        if(!weekend.isEmpty()){RacingAgenda.Event e=weekend.get(0);text(c,"PRÓXIMA SESSÃO",x+43,y+466,15,PINK,true);text(c,e.title,x+43,y+496,25,WHITE,true);text(c,sessionDate.format(new Date(e.at))+" · "+hour.format(new Date(e.at)),x+438,y+487,23,WHITE,true);}
        text(c,"PROGRAMAÇÃO DO GP",x+30,y+554,17,PURPLE,true);
        float step=weekend.size()>5?29:36;
        for(int i=0;i<Math.min(7,weekend.size());i++){
            RacingAgenda.Event e=weekend.get(i);float baseline=y+592+i*step;
            text(c,e.title,x+30,baseline,22,i==0?WHITE:MUTED,i==0);text(c,sessionDate.format(new Date(e.at)),x+430,baseline,20,MUTED,false);text(c,e.dateOnly?"A confirmar":hour.format(new Date(e.at)),x+630,baseline,22,i==0?PINK:WHITE,true);
        }
    }
    private void music(Canvas c){
        RectF b=regions[3];float x=b.left,y=b.top;panel(c,b,focused==3);
        text(c,spotify.state,x+24,y+35,17,PINK,true);
        temp.set(x+24,y+57,x+152,y+185);paint.setColor(0xFF49205F);c.drawRoundRect(temp,10,10,paint);
        if(spotify.artwork!=null)bitmap(c,spotify.artwork,x+24,y+57,128,128);else{text(c,"♪",x+62,y+142,67,PINK,false);}
        text(c,fit(spotify.title,27,354),x+174,y+95,27,WHITE,true);text(c,fit(spotify.artist,21,354),x+174,y+132,21,MUTED,false);
        text(c,spotify.available()?"OK · controles de reprodução":"OK · abrir Spotify",x+174,y+175,17,MUTED,false);
    }
    private void youtube(Canvas c){
        temp.set(1308,352,1868,632);panel(c,temp,focused==4);
        text(c,"NOVIDADES DOS MEUS CANAIS",1332,384,19,PINK,true);
        List<DashboardData.Video> videos=feeds.videos;
        videoPage=Math.min(videoPage,Math.max(0,videos.size()-1));
        if(!videos.isEmpty()){
            int save=c.save();c.clipRect(1330,400,1848,604);
            drawVideo(c,videos.get(videoPage),-videoDirection*videoSlide*530);
            if(videoSlide>0)drawVideo(c,videos.get(Math.min(videoTarget,videos.size()-1)),videoDirection*(1-videoSlide)*530);
            c.restoreToCount(save);
            feeds.thumbnail(videos.get((videoPage+1)%videos.size()));
        }
        text(c,videos.isEmpty()?"Aguardando vídeos dos seus canais":(videoPage+1)+" de "+videos.size()+"  ·  "+(focused==4?"← → para escolher":"troca automática"),1332,622,16,MUTED,false);
    }
    private void drawVideo(Canvas c,DashboardData.Video video,float offset){
        int save=c.save();c.translate(offset,0);Bitmap thumb=feeds.thumbnail(video);
        temp.set(1332,405,1604,558);paint.setColor(LINE);c.drawRoundRect(temp,9,9,paint);if(thumb!=null)c.drawBitmap(thumb,null,temp,paint);
        drawWrapped(c,video.title,1624,431,22,220,4);
        text(c,fit(video.channel,19,510),1332,590,19,MUTED,false);c.restoreToCount(save);
    }
    private void scheduleVideo(){removeCallbacks(autoVideo);if(dashboardRunning)postDelayed(autoVideo,VIDEO_INTERVAL);}
    private void changeVideo(int direction){
        int count=feeds.videos.size();if(count<2){scheduleVideo();return;}
        if(videoAnimator!=null)videoAnimator.end();removeCallbacks(autoVideo);
        videoDirection=direction;videoTarget=(videoPage+direction+count)%count;
        videoAnimator=android.animation.ValueAnimator.ofFloat(0,1);videoAnimator.setDuration(420);
        videoAnimator.addUpdateListener(animation->{videoSlide=(float)animation.getAnimatedValue();invalidate();});
        videoAnimator.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator animation){videoPage=videoTarget;videoSlide=0;videoAnimator=null;invalidate();scheduleVideo();}});
        videoAnimator.start();
    }
    private void weather(Canvas c){
        RectF b=regions[6];float x=b.left,y=b.top;panel(c,b,focused==6);HomeData.Weather w=data.weather;
        text(c,fit(Prefs.city().isEmpty()?"CLIMA":Prefs.city().toUpperCase(new Locale("pt","BR")),18,510),x+24,y+36,18,PINK,true);weatherIcon(c,w.code,x+68,y+91,28);
        text(c,Double.isNaN(w.temp)?"--°":Math.round(w.temp)+"°",x+112,y+116,53,WHITE,true);
        text(c,condition(w.code),x+239,y+85,20,WHITE,false);
        if(!Double.isNaN(w.min))text(c,"Mín "+Math.round(w.min)+"° / Máx "+Math.round(w.max)+"°",x+239,y+117,19,MUTED,false);
        line(c,x+24,y+142,x+536,y+142,LINE,1);SimpleDateFormat df=format("EEE");
        for(int i=0;i<Math.min(4,w.days.size());i++){float xx=x+39+i*133; text(c,df.format(new Date(w.days.get(i))),xx,y+172,18,MUTED,false);weatherIcon(c,w.dayCode.get(i),xx+59,y+167,13);text(c,Math.round(w.dayMin.get(i))+"° / "+Math.round(w.dayMax.get(i))+"°",xx,y+216,21,WHITE,true);}
    }
    private void weatherIcon(Canvas c,int code,float x,float y,float size){
        if(code<0){paint.setColor(MUTED);c.drawCircle(x,y,size/2,paint);return;}
        if(code<=2){paint.setColor(0xFFF2BFC4);c.drawCircle(x-size*.25f,y-size*.2f,size*.65f,paint);}
        if(code!=0){paint.setColor(0xFFE2C7FA);c.drawCircle(x-size*.4f,y+size*.1f,size*.43f,paint);c.drawCircle(x,y-size*.15f,size*.55f,paint);c.drawCircle(x+size*.5f,y+size*.1f,size*.4f,paint);c.drawRoundRect(x-size*.6f,y,x+size*.7f,y+size*.5f,size*.2f,size*.2f,paint);}
        if(code>=51){paint.setColor(PINK);paint.setStrokeWidth(2);for(int i=0;i<3;i++)c.drawLine(x-size*.4f+i*size*.4f,y+size*.7f,x-size*.5f+i*size*.4f,y+size,paint);}
    }
    private String condition(int code){if(code<0)return "Consultando clima";if(code==0)return "Céu limpo";if(code<=3)return "Parcialmente nublado";if(code<=48)return "Neblina";if(code<=67)return "Chuva";if(code<=77)return "Neve";if(code<=86)return "Pancadas de chuva";return "Trovoadas";}
    private String eventTime(RacingAgenda.Event e){return format(e.dateOnly?"EEE dd/MM '· a confirmar'":"EEE dd/MM · HH:mm").format(new Date(e.at));}
    private void panel(Canvas c,RectF b,boolean selected){paint.setColor(PANEL);paint.setStyle(Paint.Style.FILL);c.drawRoundRect(b,17,17,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(6);paint.setColor(selected?Color.WHITE:PINK);c.drawRoundRect(b,17,17,paint);paint.setStyle(Paint.Style.FILL);}
    private void text(Canvas c,String text,float x,float y,float size,int color,boolean bold){paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setTypeface(bold?strong:normal);paint.setTextSize(Math.max(18,size)*(bold?1.10f:1f));paint.setColor(color);c.drawText(text,x,y,paint);}
    private String fit(String s,float size,float width){paint.setTypeface(strong);paint.setTextSize(Math.max(18,size)*1.10f);if(paint.measureText(s)<=width)return s;int n=paint.breakText(s,true,width-paint.measureText("…"),null);return s.substring(0,n)+"…";}
    private void bitmap(Canvas c,Bitmap b,float x,float y,float w,float h){if(b==null)return;float s=Math.min(w/b.getWidth(),h/b.getHeight()),bw=b.getWidth()*s,bh=b.getHeight()*s;temp.set(x+(w-bw)/2,y+(h-bh)/2,x+(w+bw)/2,y+(h+bh)/2);paint.setColor(WHITE);paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setFilterBitmap(true);paint.setDither(true);c.drawBitmap(b,null,temp,paint);}
    private void shield(Canvas c,Bitmap source,float x,float y,float size){
        if(source==null)return;int pixels=Math.max(1,Math.round(size*zoom));Bitmap small=feeds.shield(source,pixels);
        if(small==null){bitmap(c,source,x,y,size,size);return;}
        float width=small.getWidth()/zoom,height=small.getHeight()/zoom;
        float left=(Math.round(ox+(x+(size-width)/2)*zoom)-ox)/zoom,top=(Math.round(oy+(y+(size-height)/2)*zoom)-oy)/zoom;
        temp.set(left,top,left+width,top+height);paint.setColor(WHITE);paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setFilterBitmap(false);c.drawBitmap(small,null,temp,paint);paint.setFilterBitmap(true);
    }
    private void line(Canvas c,float x,float y,float xx,float yy,int color,float width){paint.setColor(color);paint.setStrokeWidth(width);c.drawLine(x,y,xx,yy,paint);}
    private void drawTwoLines(Canvas c,String s,float x,float y,float size,float width){paint.setTextSize(size);paint.setTypeface(normal);int n=paint.breakText(s,true,width,null);if(n>=s.length()){text(c,s,x,y,size,WHITE,false);return;}int word=s.lastIndexOf(' ',n);if(word>n/2)n=word;text(c,s.substring(0,n),x,y,size,WHITE,false);text(c,fit(s.substring(n).trim(),size,width),x,y+27,size,WHITE,false);}
    private void drawWrapped(Canvas c,String value,float x,float y,float size,float width,int lines){String rest=value;for(int i=0;i<lines&&!rest.isEmpty();i++){paint.setTextSize(size);paint.setTypeface(normal);int n=paint.breakText(rest,true,width,null);if(i==lines-1){text(c,fit(rest,size,width),x,y+i*29,size,WHITE,false);break;}if(n<rest.length()){int word=rest.lastIndexOf(' ',n);if(word>n/2)n=word;}if(n==0)break;text(c,rest.substring(0,n),x,y+i*29,size,WHITE,false);rest=rest.substring(n).trim();}}
    @Override boolean onKey(KeyEvent e){
        if(e.getAction()==KeyEvent.ACTION_DOWN&&videoAnimator!=null)videoAnimator.end();
        int key=e.getKeyCode();if(key==KeyEvent.KEYCODE_BACK){if(e.getAction()==KeyEvent.ACTION_DOWN)reset(false);return true;}
        if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER){if(e.getAction()==KeyEvent.ACTION_DOWN)activate();return true;}
        if(key!=19&&key!=20&&key!=21&&key!=22)return false;if(e.getAction()!=KeyEvent.ACTION_DOWN)return true;
        if(key==22&&focused==4){changeVideo(1);return true;}
        if(key==21&&focused==4&&videoPage>0){changeVideo(-1);return true;}
        RectF here=regions[focused];float best=Float.MAX_VALUE;int target=focused;
        for(int i=0;i<regions.length;i++)if(i!=focused&&!regions[i].isEmpty()){RectF next=regions[i];float dx=next.centerX()-here.centerX(),dy=next.centerY()-here.centerY();boolean horizontal=key==21||key==22;
            float primary=horizontal?dx:dy,perpendicular=horizontal?dy:dx;if((key==19||key==21)?primary>=-1:primary<=1)continue;
            float score=Math.abs(primary)+Math.abs(perpendicular)*2.4f;if(score<best){best=score;target=i;}}
        focused=target;invalidate();return true;
    }
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=(e.getX()-ox)/zoom,y=(e.getY()-oy)/zoom;for(int i=0;i<regions.length;i++)if(regions[i].contains(x,y)){focused=i;activate();invalidate();break;}performClick();return true;}
    private void activate(){
        if(focused==0)showAgenda(1);else if(focused==1)showAgenda(2);else if(focused==2)showAgenda(0);else if(focused==3)musicActions();else if(focused==4)openVideo(videoPage);else if(focused==6)showWeather();else if(focused<10)openApp(focused-7);else if(focused==10)showAllApps();else if(focused==11)showInputs();else settings();
    }
    private void openVideo(int index){List<DashboardData.Video> vs=feeds.videos;if(index>=vs.size())return;String id=vs.get(index).id;try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.youtube.com/watch?v="+id)).setPackage("com.google.android.youtube.tv"));}catch(Exception e){openApp(0);}}
    private void musicActions(){if(!spotify.available()){openApp(2);return;}new AlertDialog.Builder(activity).setTitle(spotify.title).setItems(new String[]{"Abrir Spotify","Reproduzir / Pausar","Próxima faixa"},(d,i)->{if(i==0)openApp(2);else if(i==1)spotify.toggle();else spotify.next();}).show();}
    @Override protected void showAgenda(int i){
        if(i!=0){super.showAgenda(i);return;}
        StringBuilder message=new StringBuilder();for(RacingAgenda.Event e:agenda.upcoming(0))if(e.detail.equals(agenda.raceName))message.append(e.title).append("\n").append(eventTime(e)).append("\n\n");
        message.append("Horários de Brasília · sujeitos a alterações\n").append(agenda.status(0));
        new AlertDialog.Builder(activity).setTitle(agenda.raceName).setMessage(message.toString()).setPositiveButton("FECHAR",null).show();
    }
}
