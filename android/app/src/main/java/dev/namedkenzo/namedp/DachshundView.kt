package dev.namedkenzo.namedp

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.View
import kotlin.math.sin
import kotlin.math.min

class DachshundView(context: Context):View(context) {
    var state="idle"
    var night=false
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private val started=SystemClock.elapsedRealtime()
    private val animator=ValueAnimator.ofFloat(0f,1f).apply {duration=1600;repeatCount=ValueAnimator.INFINITE;addUpdateListener { if(isShown)invalidate() }}
    override fun onAttachedToWindow(){super.onAttachedToWindow();if(ValueAnimator.areAnimatorsEnabled())animator.start()}
    override fun onDetachedFromWindow(){animator.cancel();super.onDetachedFromWindow()}
    override fun onWindowVisibilityChanged(visibility:Int){super.onWindowVisibilityChanged(visibility);if(visibility!=VISIBLE)animator.pause()else if(animator.isPaused)animator.resume()}
    private fun fill(c:Canvas,color:String,block:(Paint)->Unit){p.color=Color.parseColor(color);p.style=Paint.Style.FILL;block(p)}
    private fun path(c:Canvas,color:String,block:Path.()->Unit){val q=Path().apply(block);fill(c,color){c.drawPath(q,it)}}
    override fun onDraw(c:Canvas){
        super.onDraw(c)
        val s=min(width/420f,height/250f)
        val t=if(ValueAnimator.areAnimatorsEnabled())(SystemClock.elapsedRealtime()-started)/1000.0 else 0.0
        c.save();c.translate((width-420*s)/2,(height-250*s)/2);c.scale(s,s)
        fill(c,if(night)"#263E3A" else "#DFE8DD"){c.drawOval(78f,198f,344f,216f,it)}
        val pace=if(state=="connecting")10.0 else 2.0
        val bounce=if(state=="connecting")3.0 else 1.3
        c.translate(if(state=="connecting")(sin(t*3)*9).toFloat() else 0f,(sin(t*pace)*bounce).toFloat())
        val tailPace=if(state=="connected")13.0 else 2.0
        val tailAngle=if(state=="connected")26.0 else 4.0
        c.save();c.rotate((sin(t*tailPace)*tailAngle).toFloat(),104f,137f)
        path(c,"#AD6338"){moveTo(105f,141f);cubicTo(75f,137f,55f,112f,59f,94f);cubicTo(64f,100f,72f,119f,108f,123f);close()};c.restore()
        val step=if(state=="connecting")(sin(t*10)*13).toFloat()else 0f
        fun leg(x:Float,delta:Float,color:String){c.save();c.rotate(delta,x,158f);fill(c,color){c.drawRoundRect(x-7,151f,x+11,201f,8f,8f,it)};c.restore()}
        leg(134f,step,"#713D2C");leg(289f,-step,"#713D2C")
        path(c,"#AD6338"){moveTo(101f,133f);cubicTo(108f,112f,135f,111f,160f,114f);lineTo(270f,114f);cubicTo(301f,104f,322f,124f,317f,148f);cubicTo(315f,173f,294f,178f,270f,177f);lineTo(143f,177f);cubicTo(112f,177f,94f,159f,101f,133f);close()}
        fill(c,"#D59A61"){c.drawRoundRect(125f,145f,297f,171f,17f,17f,it)}
        leg(153f,-step,"#AD6338");leg(305f,step,"#AD6338")
        c.save();c.rotate(if(state=="connected")-5f else (sin(t*1.7)*2).toFloat(),308f,126f)
        path(c,"#AD6338"){moveTo(282f,121f);cubicTo(267f,83f,284f,58f,311f,59f);cubicTo(337f,59f,346f,81f,344f,99f);lineTo(365f,104f);cubicTo(381f,105f,384f,122f,372f,130f);lineTo(330f,137f);cubicTo(306f,145f,290f,136f,282f,121f);close()}
        path(c,"#713D2C"){moveTo(299f,74f);cubicTo(277f,67f,271f,92f,279f,119f);cubicTo(282f,133f,291f,147f,303f,137f);cubicTo(314f,122f,316f,86f,299f,74f);close()}
        fill(c,"#D59A61"){c.drawRoundRect(332f,112f,376f,130f,7f,7f,it)}
        fill(c,"#241F21"){c.drawOval(361f,104f,381f,118f,it);val blink=t%5.4>5.2;c.drawOval(323f,if(blink)88f else 83f,333f,if(blink)90f else 95f,it)}
        if(t%5.4<=5.2)fill(c,"#FFFFFF"){c.drawCircle(329.5f,87f,1.8f,it)}
        if(state=="connected"){p.color=Color.parseColor("#713D2C");p.style=Paint.Style.STROKE;p.strokeWidth=2f;c.drawArc(343f,121f,364f,137f,0f,160f,false,p)}
        c.restore()
        path(c,"#63C7B0"){moveTo(287f,134f);quadTo(298f,142f,316f,136f);lineTo(318f,144f);quadTo(300f,151f,285f,142f);close()}
        fill(c,"#EBC57F"){c.drawOval(303f,144f,311f,154f,it)}
        c.restore()
        contentDescription=if(state=="connected")"Happy dachshund, connected" else if(state=="connecting")"Running dachshund, connecting" else "Resting dachshund"
    }
}
