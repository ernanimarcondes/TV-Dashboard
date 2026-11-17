package io.freemytv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;

/** A separate sports profile, ready for its own layout without replacing the personal dashboard. */
final class SportsHomeView extends RacingHomeView {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF[] buttons = {new RectF(100, 660, 640, 760),
            new RectF(690, 660, 1230, 760), new RectF(1280, 660, 1820, 760)};
    private final String[] labels = {"Selecionar painel", "Aplicativos", "Ajustes"};
    private int focus;
    private float scale = 1, left, top;

    SportsHomeView(Activity activity) {
        super(activity);
        setContentDescription("Esportes Dash. Perfil em preparação. Selecionar painel, Aplicativos e Ajustes. Use as setas e OK.");
    }

    // The placeholder does not fetch the personal dashboard's feeds or subscribe to Spotify.
    @Override void resume() { invalidate(); }
    @Override void pause() {}
    @Override void reset(boolean animate) { focus = 0; invalidate(); }
    @Override public void onData() { invalidate(); }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        scale = Math.min(w / 1920f, h / 1080f);
        left = (w - 1920 * scale) / 2;
        top = (h - 1080 * scale) / 2;
    }

    @Override protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        canvas.save();
        canvas.translate(left, top);
        canvas.scale(scale, scale);
        text(canvas, "ESPORTES DASH", 100, 160, 28, 0xFFFF2EAE, true);
        text(canvas, "Seu novo painel de esportes", 100, 360, 64, Color.WHITE, true);
        text(canvas, "Perfil criado. O conteúdo e o visual serão definidos na próxima etapa.",
                100, 430, 29, 0xFFC5B6D1, false);
        text(canvas, "O Dash Pessoal continua disponível no seletor de painéis.",
                100, 482, 29, 0xFFC5B6D1, false);
        for (int i = 0; i < buttons.length; i++) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF211A2A);
            canvas.drawRoundRect(buttons[i], 17, 17, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(6);
            paint.setColor(i == focus ? Color.WHITE : 0xFFFF2EAE);
            canvas.drawRoundRect(buttons[i], 17, 17, paint);
            paint.setStyle(Paint.Style.FILL);
            text(canvas, labels[i], buttons[i].left + 28, buttons[i].top + 62, 30, Color.WHITE, true);
        }
        text(canvas, "SETAS para navegar  ·  OK para abrir", 100, 985, 22, 0xFFC5B6D1, false);
        canvas.restore();
    }

    private void text(Canvas canvas, String value, float x, float y, float size, int color, boolean bold) {
        paint.setTypeface(bold ? Ui.displaySemibold : Ui.regular);
        paint.setTextSize(size);
        paint.setColor(color);
        canvas.drawText(value, x, y, paint);
    }

    @Override boolean onKey(KeyEvent event) {
        int key = event.getKeyCode();
        boolean handled = key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT
                || key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN
                || key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_BACK;
        if (!handled) return false;
        if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
        if (key == KeyEvent.KEYCODE_DPAD_LEFT) focus = Math.max(0, focus - 1);
        else if (key == KeyEvent.KEYCODE_DPAD_RIGHT) focus = Math.min(buttons.length - 1, focus + 1);
        else if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) activate();
        else if (key == KeyEvent.KEYCODE_BACK) reset(false);
        invalidate();
        return true;
    }

    private void activate() {
        if (focus == 0) activity.startActivity(new Intent(activity, SettingsActivity.class)
                .putExtra("dashboard_profiles", true));
        else if (focus == 1) showAllApps();
        else settings();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        float x = (event.getX() - left) / scale, y = (event.getY() - top) / scale;
        for (int i = 0; i < buttons.length; i++) if (buttons[i].contains(x, y)) {
            focus = i;
            activate();
            invalidate();
            performClick();
            break;
        }
        return true;
    }
}
