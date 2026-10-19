package ae.prayertimes.abudhabi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.widget.Switch;

/** Native Switch behavior and accessibility with the approved Material-shaped visual. */
final class DesignSwitch extends Switch {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int selectedColor, unselectedColor, outlineColor;
    DesignSwitch(Context context,int selected,int unselected,int outline){
        super(context);selectedColor=selected;unselectedColor=unselected;outlineColor=outline;
        setShowText(false);setSwitchMinWidth(dp(52));setMinHeight(dp(48));setMinimumWidth(dp(52));setPadding(0,0,0,0);
        setThumbDrawable(null);setTrackDrawable(null);
    }
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override public void onMeasure(int widthSpec,int heightSpec){
        setMeasuredDimension(resolveSize(dp(52),widthSpec),resolveSize(dp(48),heightSpec));
    }
    @Override protected void onDraw(Canvas canvas){
        float width=dp(52),height=dp(32),left=(getWidth()-width)/2f,top=(getHeight()-height)/2f;
        paint.setStyle(Paint.Style.FILL);paint.setColor(isChecked()?selectedColor:unselectedColor);paint.setAlpha(isEnabled()?255:100);
        RectF bounds=new RectF(left,top,left+width,top+height);canvas.drawRoundRect(bounds,dp(16),dp(16),paint);
        if(!isChecked()){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));paint.setColor(outlineColor);RectF edge=new RectF(bounds);edge.inset(dp(1),dp(1));canvas.drawRoundRect(edge,dp(15),dp(15),paint);}
        boolean right=isChecked()!=(getLayoutDirection()==View.LAYOUT_DIRECTION_RTL);
        float center=left+dp(right?36:16);paint.setStyle(Paint.Style.FILL);paint.setColor(isChecked()?Color.WHITE:outlineColor);canvas.drawCircle(center,top+height/2f,dp(isChecked()?12:8),paint);
    }
}
