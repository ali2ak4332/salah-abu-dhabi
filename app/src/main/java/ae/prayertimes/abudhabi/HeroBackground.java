package ae.prayertimes.abudhabi;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Quiet geometric detail confined to the next-prayer card. */
final class HeroBackground extends Drawable {
    private final int color;
    private final float radius;
    private final boolean rtl;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    HeroBackground(int color,float radius,boolean rtl){this.color=color;this.radius=radius;this.rtl=rtl;}
    @Override public void draw(Canvas canvas){
        RectF r=new RectF(getBounds());Path clip=new Path();clip.addRoundRect(r,radius,radius,Path.Direction.CW);
        int save=canvas.save();canvas.clipPath(clip);
        int lifted=Color.rgb(Math.min(255,Color.red(color)+8),Math.min(255,Color.green(color)+8),Math.min(255,Color.blue(color)+8));
        paint.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,color,lifted,Shader.TileMode.CLAMP));paint.setStyle(Paint.Style.FILL);canvas.drawRect(r,paint);paint.setShader(null);
        // Fine overlapping squares form a restrained corner motif; text stays dominant.
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1,r.height()/110));paint.setColor(0x0cffffff);
        float cx=rtl?r.left+r.height()*.3f:r.right-r.height()*.3f,cy=r.top+r.height()*.5f;
        canvas.translate(cx,cy);canvas.rotate(45);
        for(float scale:new float[]{.42f,.68f,.94f}){float h=r.height()*scale;canvas.drawRect(-h,-h,h,h,paint);}
        canvas.restoreToCount(save);
    }
    @Override public void setAlpha(int alpha){}
    @Override public void setColorFilter(ColorFilter filter){}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
