package com.tomatotimer.app.timer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader

/** Transparent brand artwork; the notification card itself belongs to System UI. */
internal object NotificationArt {
    fun timerFace(resting: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(450, 495, Bitmap.Config.ARGB_8888)
        val c = Canvas(bitmap)
        c.scale(1.5f, 1.5f)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val body = Path().apply {
            moveTo(150f,68f)
            cubicTo(205f,42f,290f,81f,289f,174f)
            cubicTo(294f,269f,220f,322f,150f,307f)
            cubicTo(80f,322f,6f,269f,11f,174f)
            cubicTo(10f,81f,95f,42f,150f,68f);close()
        }
        p.shader = LinearGradient(30f,60f,260f,300f,
            Color.rgb(255,139,107), Color.rgb(227,59,67), Shader.TileMode.CLAMP)
        c.drawPath(body,p);p.shader=null
        p.style=Paint.Style.STROKE;p.strokeWidth=7f;p.strokeCap=Paint.Cap.ROUND
        p.color=if(resting) Color.argb(204,189,242,213) else Color.argb(204,255,252,247)
        c.drawArc(37f,67f,263f,293f,-70f,320f,false,p)
        p.style=Paint.Style.FILL;p.color=Color.rgb(187,241,185)
        val crown=Path().apply {
            moveTo(150f,63f)
            cubicTo(132f,44f,115f,40f,99f,45f)
            cubicTo(109f,60f,131f,63f,148f,65f)
            cubicTo(132f,68f,121f,78f,120f,90f)
            cubicTo(138f,89f,148f,78f,151f,69f)
            cubicTo(160f,84f,173f,89f,184f,87f)
            cubicTo(179f,72f,165f,65f,155f,65f)
            cubicTo(175f,63f,195f,57f,203f,43f)
            cubicTo(180f,38f,162f,47f,150f,63f);close()
        }
        c.drawPath(crown,p)
        p.style=Paint.Style.STROKE;p.strokeWidth=7f
        c.drawPath(Path().apply{moveTo(150f,62f);quadTo(148f,37f,165f,24f)},p)
        return bitmap
    }
}
