package io.freemytv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.*;

/** Personal 720p-friendly home. Static Canvas art, no video backgrounds or per-frame network work. */
class RacingHomeView extends View implements HomeData.Listener {
    private static final int PINK=0xFFFF3FA4, WHITE=0xFFF5EFF7, MUTED=0xFFAAA0B3, LILAC=0xFFAC83FF;
    private static final String[] APPS={"YouTube","Prime Video","Spotify"};
    private static final String[] PACKAGES={"com.google.android.youtube.tv","com.amazon.amazonvideo.livingroom","com.spotify.tv.android"};
    private static final String[] TOOLS={"CLIMA","APLICATIVOS","TV / HDMI","AJUSTES"};
    protected final Activity activity;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r=new RectF();
    private final Typeface regular=Typeface.create("sans-serif",Typeface.NORMAL), bold=Typeface.create("sans-serif",Typeface.BOLD);
    protected final HomeData data;
    protected final RacingAgenda agenda;
    private final Bitmap[] icons=new Bitmap[3];
    private final RectF[] hit=new RectF[11];
    private boolean running;
    private int row=0,index=0;
    private float scale=1,offsetX,offsetY;
    private LinearGradient background;
    private final SimpleDateFormat clock=new SimpleDateFormat("HH:mm",new Locale("pt","BR"));
    private final SimpleDateFormat day=new SimpleDateFormat("EEEE, dd 'de' MMMM",new Locale("pt","BR"));
    private final Runnable tick=new Runnable(){public void run(){if(!running)return;data.refreshIfStale();agenda.refresh(()->invalidate());invalidate();postDelayed(this,60000);}};

    RacingHomeView(Activity a){
        super(a);activity=a;data=HomeData.get(a);agenda=new RacingAgenda(a);
        setFocusable(true);setFocusableInTouchMode(true);requestFocus();
        clock.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));day.setTimeZone(clock.getTimeZone());
        for(int i=0;i<hit.length;i++)hit[i]=new RectF();
        for(int i=0;i<3;i++)try{Drawable d=a.getPackageManager().getApplicationIcon(PACKAGES[i]);Bitmap b=Bitmap.createBitmap(72,72,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);d.setBounds(0,0,72,72);d.draw(c);icons[i]=b;}catch(Exception ignored){}
        setContentDescription("Race After Dark. YouTube, Prime Video, Spotify. Agenda Ferrari, Palmeiras, Celtics e WEC. Use as setas e OK.");
    }
    void resume(){if(running)return;running=true;data.addListener(this);removeCallbacks(tick);post(tick);}
    void pause(){running=false;removeCallbacks(tick);data.removeListener(this);}
    void destroy(){pause();agenda.close();for(Bitmap b:icons)if(b!=null)b.recycle();}
    void reset(boolean animate){row=0;index=0;invalidate();}
    public void onData(){invalidate();}
    void showRecents(){showAllApps();}
    void showToast(String message){Toast.makeText(activity,message,Toast.LENGTH_LONG).show();}

    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        scale=Math.min(w/1920f,h/1080f);offsetX=(w-1920*scale)/2;offsetY=(h-1080*scale)/2;
        background=new LinearGradient(0,0,1920,1080,new int[]{0xFF09090F,0xFF17101E,0xFF09090F},null,Shader.TileMode.CLAMP);
    }
    @Override protected void onDraw(Canvas canvas){
        canvas.drawColor(0xFF09090F);canvas.save();canvas.translate(offsetX,offsetY);canvas.scale(scale,scale);
        p.setShader(background);canvas.drawRect(0,0,1920,1080,p);p.setShader(null);
        // Quiet grid and a circuit silhouette. These are vector strokes, not a large wallpaper bitmap.
        p.setColor(0x0CFF3FA4);p.setStrokeWidth(1);
        for(int x=1040;x<1870;x+=70)canvas.drawLine(x,40,x,330,p);
        for(int y=55;y<320;y+=55)canvas.drawLine(1040,y,1870,y,p);
        circuit(canvas);
        text(canvas,"RACE / AFTER DARK",72,80,23,PINK,true);
        text(canvas,"SEU PADDOCK. SUA TELA.",72,119,16,MUTED,false);
        text(canvas,clock.format(new Date()),1510,93,59,WHITE,true);
        text(canvas,day.format(new Date()),1512,129,18,MUTED,false);
        line(canvas,72,155,1848,155,0x33FF3FA4,1);
        text(canvas,"NIGHT SESSION",72,213,17,LILAC,true);
        text(canvas,"Luzes apagadas.",69,280,58,WHITE,true);
        text(canvas,"Seu mundo em pole position.",72,322,25,MUTED,false);
        weatherSummary(canvas,1510,185);
        text(canvas,"SEUS FAVORITOS",72,390,19,WHITE,true);
        text(canvas,"01 / STREAMING",1654,390,16,MUTED,false);
        for(int i=0;i<3;i++)drawApp(canvas,i);
        text(canvas,"NA SUA AGENDA",72,628,19,WHITE,true);
        text(canvas,"Horários de Brasília · sem placares ao vivo",1290,628,18,MUTED,false);
        for(int i=0;i<4;i++)drawAgenda(canvas,i);
        for(int i=0;i<4;i++)drawTool(canvas,i);
        text(canvas,"FERRARI  /  PALMEIRAS  /  CELTICS",72,1027,16,0xFF817489,false);
        text(canvas,"SETAS para navegar     OK para abrir",1328,1027,17,MUTED,false);
        canvas.restore();
    }
    private void circuit(Canvas c){
        Path path=new Path();path.moveTo(1080,298);path.lineTo(1140,225);path.lineTo(1210,225);path.cubicTo(1250,225,1225,170,1270,165);path.lineTo(1370,165);path.lineTo(1410,208);path.lineTo(1465,215);path.lineTo(1410,270);path.lineTo(1330,280);path.lineTo(1270,325);path.lineTo(1165,325);path.lineTo(1135,297);path.close();
        p.setStyle(Paint.Style.STROKE);p.setStrokeJoin(Paint.Join.ROUND);p.setColor(0x223F1631);p.setStrokeWidth(21);c.drawPath(path,p);p.setColor(0xAAFF3FA4);p.setStrokeWidth(3);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
        p.setColor(LILAC);c.drawCircle(1080,298,5,p);text(c,"APEX / 01",1080,358,14,MUTED,false);
    }
    private void drawApp(Canvas c,int i){
        float x=72+i*600;RectF b=hit[i];b.set(x,420,x+576,568);boolean selected=row==0&&index==i;
        card(c,b,selected,selected?0xFF2D162A:0xFF1A1520);
        if(icons[i]!=null){p.setColor(WHITE);p.setFilterBitmap(true);r.set(x+29,454,x+101,526);c.drawBitmap(icons[i],null,r,p);}else drawSpotify(c,x+65,490);
        text(c,APPS[i],x+128,484,32,WHITE,true);
        text(c,i==0?"Vídeos e suas inscrições":i==1?"Filmes e séries":"Sua trilha sonora",x+128,521,19,MUTED,false);
        text(c,selected?"ABRIR  ›":"0"+(i+1),x+450,548,14,selected?PINK:MUTED,true);
    }
    private void drawSpotify(Canvas c,float x,float y){
        p.setColor(0xFF1ED760);c.drawCircle(x,y,34,p);p.setColor(0xFF07140B);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setStrokeCap(Paint.Cap.ROUND);
        for(int i=0;i<3;i++){r.set(x-21+i*2,y-14+i*10,x+21-i*2,y+8+i*8);c.drawArc(r,210,120,false,p);}p.setStrokeCap(Paint.Cap.BUTT);p.setStyle(Paint.Style.FILL);
    }
    private void drawAgenda(Canvas c,int i){
        float x=72+i*450;RectF b=hit[3+i];b.set(x,657,x+426,875);boolean selected=row==1&&index==i;
        card(c,b,selected,0xFF17131D);int accent=i==1?0xFF79CFA3:i==2?0xFF8FD4AA:i==3?LILAC:PINK;
        line(c,x+24,683,x+57,683,accent,3);text(c,RacingAgenda.NAMES[i],x+69,691,17,accent,true);
        List<RacingAgenda.Event> list=agenda.upcoming(i);
        if(list.isEmpty()){
            text(c,"Sem próxima data",x+24,741,24,WHITE,true);text(c,"Aguardando a programação",x+24,777,18,MUTED,false);
        }else{
            RacingAgenda.Event e=list.get(0);text(c,fit(e.title,26,375),x+24,740,26,WHITE,true);
            text(c,eventDate(e,false),x+24,780,21,0xFFE4CADF,false);
        }
        text(c,i==3?"Calendário 2027 · revisão manual":agenda.status(i),x+24,845,14,MUTED,false);
    }
    private void drawTool(Canvas c,int i){
        float x=72+i*450;RectF b=hit[7+i];b.set(x,916,x+426,975);boolean selected=row==2&&index==i;
        card(c,b,selected,0xFF141019);text(c,TOOLS[i],x+24,954,18,selected?WHITE:MUTED,true);text(c,"›",x+391,956,25,selected?PINK:MUTED,false);
    }
    private void weatherSummary(Canvas c,float x,float y){
        HomeData.Weather w=data.weather;text(c,Prefs.city().isEmpty()?"CLIMA":Prefs.city().toUpperCase(new Locale("pt","BR")),x,y,17,LILAC,true);
        text(c,Double.isNaN(w.temp)?"--°":Math.round(w.temp)+"°C",x,y+62,48,WHITE,true);
        text(c,condition(w.code),x,y+95,18,MUTED,false);
        if(!Double.isNaN(w.min))text(c,"Mín "+Math.round(w.min)+"°  /  Máx "+Math.round(w.max)+"°",x,y+125,18,MUTED,false);
    }
    private void card(Canvas c,RectF b,boolean selected,int color){
        p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(b,18,18,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(selected?3:1);p.setColor(selected?PINK:0xFF302335);c.drawRoundRect(b,18,18,p);p.setStyle(Paint.Style.FILL);
    }
    private void text(Canvas c,String s,float x,float y,float size,int color,boolean strong){p.setTypeface(strong?bold:regular);p.setTextSize(size);p.setColor(color);p.setShader(null);p.setStyle(Paint.Style.FILL);c.drawText(s,x,y,p);}
    private String fit(String s,float size,float width){p.setTypeface(bold);p.setTextSize(size);if(p.measureText(s)<=width)return s;int n=p.breakText(s,true,width-p.measureText("…"),null);return s.substring(0,n)+"…";}
    private void line(Canvas c,float x,float y,float xx,float yy,int color,float width){p.setColor(color);p.setStrokeWidth(width);c.drawLine(x,y,xx,yy,p);}
    private static String eventDate(RacingAgenda.Event e,boolean full){
        SimpleDateFormat f=new SimpleDateFormat(e.dateOnly?"dd/MM/yyyy '· horário a confirmar'":"EEE, dd/MM · HH:mm",new Locale("pt","BR"));f.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));return f.format(new Date(e.at));
    }
    private static String condition(int code){if(code<0)return "Consultando previsão";if(code==0)return "Céu limpo";if(code<=3)return "Parcialmente nublado";if(code<=48)return "Neblina";if(code<=67)return "Chuva";if(code<=77)return "Neve";if(code<=86)return "Pancadas de chuva";return "Trovoadas";}

    boolean onKey(KeyEvent e){
        int key=e.getKeyCode();boolean ours=key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT||key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER||key==KeyEvent.KEYCODE_BACK;
        if(!ours)return false;if(e.getAction()!=KeyEvent.ACTION_DOWN)return true;
        if(key==KeyEvent.KEYCODE_DPAD_LEFT)index=Math.max(0,index-1);
        else if(key==KeyEvent.KEYCODE_DPAD_RIGHT)index=Math.min(row==0?2:3,index+1);
        else if(key==KeyEvent.KEYCODE_DPAD_UP){row=Math.max(0,row-1);index=Math.min(index,row==0?2:3);}
        else if(key==KeyEvent.KEYCODE_DPAD_DOWN)row=Math.min(2,row+1);
        else if(key==KeyEvent.KEYCODE_BACK)reset(false);
        else activate();invalidate();return true;
    }
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=(e.getX()-offsetX)/scale,y=(e.getY()-offsetY)/scale;for(int i=0;i<hit.length;i++)if(hit[i].contains(x,y)){row=i<3?0:i<7?1:2;index=i<3?i:i<7?i-3:i-7;activate();invalidate();break;}performClick();return true;}
    @Override public boolean performClick(){super.performClick();return true;}
    private void activate(){if(row==0)openApp(index);else if(row==1)showAgenda(index);else if(index==0)showWeather();else if(index==1)showAllApps();else if(index==2)showInputs();else settings();}
    protected void openApp(int i){
        try{Intent intent=activity.getPackageManager().getLeanbackLaunchIntentForPackage(PACKAGES[i]);if(intent==null)intent=activity.getPackageManager().getLaunchIntentForPackage(PACKAGES[i]);
            if(intent==null)intent=new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id="+PACKAGES[i])).setPackage("com.android.vending");activity.startActivity(intent);
        }catch(Exception e){showToast("Não foi possível abrir "+APPS[i]);}
    }
    protected void settings(){
        new AlertDialog.Builder(activity).setTitle("Ajustes / "+Prefs.dashboardName()).setItems(new String[]{"Selecionar painel","Free My TV · configurações e modo Original","Configurações da TV","Sobre a versão Racing"},(d,i)->{
            if(i==0)activity.startActivity(new Intent(activity,SettingsActivity.class).putExtra("dashboard_profiles",true));
            else if(i==1)activity.startActivity(new Intent(activity,SettingsActivity.class));
            else if(i==2)HomeApps.open(activity,HomeApps.action(HomeApps.SETTINGS,HomeApps.TOP_SETTINGS,"Configurações","settings"),null);
            else new AlertDialog.Builder(activity).setTitle("Painel Racing 2.0").setMessage("Versão pessoal baseada no Free My TV 1.5.\nClima: Open-Meteo. Agenda: Jolpica F1 e ESPN. Classificações: ESPN. Vídeos: canal oficial Formula 1, como exemplo. Spotify: sessão de mídia local do Android.\n\nSem resultados ao vivo. Horários de Brasília, sujeitos a alterações. As agendas da internet são consultadas no máximo a cada 6 horas e guardadas para uso offline.\n\nPara voltar à tela de fábrica: Free My TV → Home Screen → Original.").setPositiveButton("OK",null).show();
        }).show();
    }
    protected void showAgenda(int i){
        List<RacingAgenda.Event> list=agenda.upcoming(i);StringBuilder s=new StringBuilder();
        for(int j=0;j<Math.min(5,list.size());j++){RacingAgenda.Event e=list.get(j);s.append(e.title).append("\n").append(eventDate(e,true)).append("\n").append(e.detail).append("\n\n");}
        if(list.isEmpty())s.append("Nenhuma próxima data disponível nesta fonte.\n\n");
        s.append("Fonte: ").append(RacingAgenda.SOURCES[i]).append("\n").append(agenda.status(i)).append("\nHorários de Brasília · sujeitos a alterações");
        new AlertDialog.Builder(activity).setTitle(RacingAgenda.NAMES[i]).setMessage(s.toString()).setPositiveButton("FECHAR",null).show();
    }
    protected void showWeather(){
        HomeData.Weather w=data.weather;StringBuilder s=new StringBuilder(condition(w.code));
        if(!Double.isNaN(w.temp))s.append(" · ").append(Math.round(w.temp)).append("°C");s.append("\n\nPRÓXIMAS HORAS\n");
        SimpleDateFormat f=new SimpleDateFormat("HH:mm",new Locale("pt","BR"));f.setTimeZone(clock.getTimeZone());
        for(int i=0;i<w.hours.size();i++)s.append(f.format(new Date(w.hours.get(i)))).append("   ").append(Math.round(w.hourTemp.get(i))).append("°C   ").append(condition(w.hourCode.get(i))).append("\n");
        s.append("\nPRÓXIMOS DIAS\n");f=new SimpleDateFormat("EEE dd/MM",new Locale("pt","BR"));f.setTimeZone(clock.getTimeZone());
        for(int i=0;i<w.days.size();i++)s.append(f.format(new Date(w.days.get(i)))).append("   ").append(Math.round(w.dayMin.get(i))).append("° / ").append(Math.round(w.dayMax.get(i))).append("°\n");
        s.append("\nFonte: Open-Meteo");new AlertDialog.Builder(activity).setTitle("Clima / "+Prefs.city()).setMessage(s.toString()).setPositiveButton("FECHAR",null).show();
    }
    void showAllApps(){
        Intent q=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> found=activity.getPackageManager().queryIntentActivities(q,0);
        found.sort((a,b)->a.loadLabel(activity.getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(activity.getPackageManager()).toString()));
        String[] names=new String[found.size()];for(int i=0;i<names.length;i++)names[i]=found.get(i).loadLabel(activity.getPackageManager()).toString();
        new AlertDialog.Builder(activity).setTitle("Aplicativos").setItems(names,(d,i)->{try{ResolveInfo a=found.get(i);activity.startActivity(new Intent(Intent.ACTION_MAIN).setClassName(a.activityInfo.packageName,a.activityInfo.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception e){showToast("Não foi possível abrir o aplicativo");}}).show();
    }
    void showInputs(){HomeApps.open(activity,HomeApps.action(HomeApps.INPUTS,HomeApps.TOP_INPUTS,"TV / HDMI","hdmi"),null);}
}
