package dev.namedkenzo.namedp

import android.graphics.*
import android.graphics.drawable.Drawable

/** Small original vector marks, shared by navigation and profile cards. */
class GlyphDrawable(private val glyph:String,private val tint:Int,private val size:Int):Drawable(){
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=tint;strokeWidth=1.7f;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
    private val path=Path()
    override fun getIntrinsicWidth()=size
    override fun getIntrinsicHeight()=size
    override fun setAlpha(alpha:Int){paint.alpha=alpha;invalidateSelf()}
    override fun setColorFilter(colorFilter:ColorFilter?){paint.colorFilter=colorFilter;invalidateSelf()}
    @Deprecated("Deprecated in Java") override fun getOpacity()=PixelFormat.TRANSLUCENT
    override fun draw(canvas:Canvas){
        canvas.save();canvas.translate(bounds.left.toFloat(),bounds.top.toFloat());canvas.scale(bounds.width()/24f,bounds.height()/24f)
        paint.style=Paint.Style.STROKE;path.reset()
        when(glyph){
            "home"->{path.moveTo(4f,11f);path.lineTo(12f,4f);path.lineTo(20f,11f);path.moveTo(6f,10f);path.lineTo(6f,20f);path.lineTo(10f,20f);path.lineTo(10f,14f);path.lineTo(14f,14f);path.lineTo(14f,20f);path.lineTo(18f,20f);path.lineTo(18f,10f);canvas.drawPath(path,paint)}
            "profiles"->{canvas.drawRoundRect(4f,4f,20f,11f,2f,2f,paint);canvas.drawRoundRect(4f,15f,20f,22f,2f,2f,paint);canvas.drawPoint(8f,7.5f,paint);canvas.drawPoint(8f,18.5f,paint);canvas.drawLine(12f,7.5f,16f,7.5f,paint);canvas.drawLine(12f,18.5f,16f,18.5f,paint)}
            "settings"->{for((y,x)in listOf(6f to 15f,12f to 8f,18f to 15f)){canvas.drawLine(4f,y,x-2.5f,y,paint);canvas.drawLine(x+2.5f,y,20f,y,paint);canvas.drawCircle(x,y,2.5f,paint)}}
            "paste"->{canvas.drawRoundRect(5f,5f,19f,22f,2f,2f,paint);canvas.drawRoundRect(9f,2f,15f,7f,1.5f,1.5f,paint);canvas.drawLine(9f,12f,15f,12f,paint);canvas.drawLine(9f,16f,13f,16f,paint)}
            else->{paint.style=Paint.Style.FILL;canvas.drawOval(5f,13f,19f,23f,paint);canvas.drawOval(1f,6f,7f,14f,paint);canvas.drawOval(8.5f,1f,14.5f,10f,paint);canvas.drawOval(17f,6f,23f,14f,paint)}
        };canvas.restore()
    }
}
