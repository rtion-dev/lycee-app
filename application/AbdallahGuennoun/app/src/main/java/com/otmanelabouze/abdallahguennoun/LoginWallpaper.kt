package com.otmanelabouze.abdallahguennoun

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/** Three cached vector wave bands: no bitmap wallpaper, timer, video or continuous animation. */
@Composable
internal fun LoginWallpaper(dark: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.drawWithCache {
        val w = size.width
        val h = size.height
        val gradient = Brush.verticalGradient(if(dark) listOf(Color(0xFF0B203A),Color(0xFF23476D),Color(0xFF4977A2)) else listOf(Color(0xFFCDDEF0),Color(0xFFE7F2FD),Color(0xFFF5FAFF)))
        val upper = Path().apply {
            moveTo(0f,h*.12f)
            cubicTo(w*.28f,h*.18f,w*.48f,h*.39f,w,h*.19f)
            lineTo(w,h*.25f)
            cubicTo(w*.52f,h*.45f,w*.23f,h*.24f,0f,h*.18f)
            close()
        }
        val middle = Path().apply {
            moveTo(0f,h*.34f)
            cubicTo(w*.34f,h*.38f,w*.53f,h*.64f,w,h*.43f)
            lineTo(w,h*.49f)
            cubicTo(w*.54f,h*.7f,w*.31f,h*.44f,0f,h*.4f)
            close()
        }
        val lower = Path().apply {
            moveTo(0f,h*.6f)
            cubicTo(w*.48f,h*.59f,w*.6f,h*.85f,w,h*.63f)
            lineTo(w,h*.7f)
            cubicTo(w*.55f,h*.9f,w*.42f,h*.69f,0f,h*.67f)
            close()
        }
        val bottom = Path().apply {
            moveTo(0f,h*.85f)
            cubicTo(w*.38f,h*.97f,w*.63f,h*.98f,w,h*.88f)
            lineTo(w,h*.94f)
            cubicTo(w*.58f,h*1.02f,w*.26f,h*.99f,0f,h*.9f)
            close()
        }
        onDrawBehind {
            drawRect(gradient)
            drawPath(upper,Color.White.copy(alpha=if(dark).035f else .23f))
            drawPath(middle,Color.White.copy(alpha=if(dark).045f else .32f))
            drawPath(lower,Color.White.copy(alpha=if(dark).05f else .38f))
            drawPath(bottom,Color.White.copy(alpha=if(dark).04f else .3f))
        }
    })
}
