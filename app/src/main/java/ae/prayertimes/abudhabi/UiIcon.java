package ae.prayertimes.abudhabi;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Small original outline icons, drawn as vectors at every screen density. */
final class UiIcon extends Drawable {
    static final int CALENDAR=0, BELL=1, SETTINGS=2, PREVIOUS=3, NEXT=4, TODAY=5;
    private final int kind, size;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    UiIcon(int kind,int color,int size){this.kind=kind;this.size=size;paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.7f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);}
    @Override public void draw(Canvas canvas){
        int save=canvas.save();canvas.translate(getBounds().left,getBounds().top);canvas.scale(getBounds().width()/24f,getBounds().height()/24f);
        if(kind==CALENDAR||kind==TODAY){
            canvas.drawRoundRect(new RectF(4,5,20,21),3,3,paint);line(canvas,8,3,8,7);line(canvas,16,3,16,7);line(canvas,4,10,20,10);
            if(kind==TODAY)canvas.drawCircle(12,15.5f,2.5f,paint);
            else{line(canvas,8,14,9,14);line(canvas,14,14,15,14);line(canvas,8,17,9,17);}
        }else if(kind==BELL){
            Path p=new Path();p.moveTo(5,17);p.lineTo(7,14);p.lineTo(7,10);p.cubicTo(7,3,17,3,17,10);p.lineTo(17,14);p.lineTo(19,17);p.close();canvas.drawPath(p,paint);
            canvas.drawArc(new RectF(9,17,15,22),20,140,false,paint);line(canvas,12,3,12,4);
        }else if(kind==SETTINGS){
            Path p=new Path();
            for(int i=0;i<32;i++){double a=-Math.PI/2+i*Math.PI/16;float r=(i%4==1||i%4==2)?9.5f:7.8f;float x=12+(float)Math.cos(a)*r,y=12+(float)Math.sin(a)*r;if(i==0)p.moveTo(x,y);else p.lineTo(x,y);}
            p.close();canvas.drawPath(p,paint);canvas.drawCircle(12,12,3,paint);
        }else{
            Path p=new Path();float x=kind==PREVIOUS?14.5f:9.5f,end=kind==PREVIOUS?9.5f:14.5f;p.moveTo(x,6.5f);p.lineTo(end,12);p.lineTo(x,17.5f);canvas.drawPath(p,paint);
        }
        canvas.restoreToCount(save);
    }
    private void line(Canvas c,float x,float y,float a,float b){c.drawLine(x,y,a,b,paint);}
    @Override public int getIntrinsicWidth(){return size;}
    @Override public int getIntrinsicHeight(){return size;}
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
