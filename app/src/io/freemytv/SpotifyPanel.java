package io.freemytv;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import java.util.*;

/** Reads only Spotify's local Android media session, never account credentials or notifications. */
final class SpotifyPanel implements MediaSessionManager.OnActiveSessionsChangedListener {
    private final Context c;private final Runnable changed;private final Handler main=new Handler(Looper.getMainLooper());
    private MediaController controller;private boolean started;
    String title="Nada tocando agora",artist="Abra o Spotify para ouvir",state="SPOTIFY";Bitmap artwork;
    private final MediaController.Callback callback=new MediaController.Callback(){
        @Override public void onMetadataChanged(MediaMetadata m){update();}
        @Override public void onPlaybackStateChanged(PlaybackState p){update();}
        @Override public void onSessionDestroyed(){bind(null);}
    };
    SpotifyPanel(Context c,Runnable changed){this.c=c;this.changed=changed;}
    void start(){if(started)return;if(!Prefs.nowPlaying()){state="SPOTIFY · WIDGET DESATIVADO";changed.run();return;}try{MediaSessionManager m=c.getSystemService(MediaSessionManager.class);m.addOnActiveSessionsChangedListener(this,NowPlaying.component(c),main);started=true;onActiveSessionsChanged(m.getActiveSessions(NowPlaying.component(c)));}catch(SecurityException e){state="SPOTIFY · ACESSO PENDENTE";changed.run();}}
    void stop(){if(!started)return;started=false;c.getSystemService(MediaSessionManager.class).removeOnActiveSessionsChangedListener(this);if(controller!=null)controller.unregisterCallback(callback);controller=null;}
    public void onActiveSessionsChanged(List<MediaController> sessions){MediaController match=null;if(sessions!=null)for(MediaController m:sessions)if(m.getPackageName().startsWith("com.spotify.")){match=m;break;}bind(match);}
    private void bind(MediaController m){if(controller!=null)controller.unregisterCallback(callback);controller=m;if(m!=null)m.registerCallback(callback,main);update();}
    private void update(){
        title="Nada tocando agora";artist="Abra o Spotify para ouvir";artwork=null;state="SPOTIFY";
        if(controller!=null){MediaMetadata metadata=controller.getMetadata();if(metadata!=null){CharSequence t=metadata.getDescription().getTitle(),a=metadata.getDescription().getSubtitle();if(t!=null)title=t.toString();if(a!=null)artist=a.toString();Bitmap b=metadata.getDescription().getIconBitmap();if(b==null)b=metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);if(b==null)b=metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);if(b!=null)artwork=b.getWidth()>180||b.getHeight()>180?Bitmap.createScaledBitmap(b,160,160,true):b;}
            PlaybackState p=controller.getPlaybackState();state=p!=null&&p.getState()==PlaybackState.STATE_PLAYING?"SPOTIFY · TOCANDO":"SPOTIFY · PAUSADO";}
        changed.run();
    }
    boolean available(){return controller!=null;}
    void toggle(){if(controller==null)return;PlaybackState p=controller.getPlaybackState();if(p!=null&&p.getState()==PlaybackState.STATE_PLAYING)controller.getTransportControls().pause();else controller.getTransportControls().play();}
    void next(){if(controller!=null)controller.getTransportControls().skipToNext();}
}
