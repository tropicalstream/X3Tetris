package com.tetrallama

import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

object GlUtil {
    fun program(vs: String, fs: String): Int {
        fun sh(type: Int, src: String): Int {
            val s = GLES20.glCreateShader(type)
            GLES20.glShaderSource(s, src); GLES20.glCompileShader(s)
            val ok = IntArray(1)
            GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0)
            if (ok[0] == 0) Log.e("GlUtil", "shader: " + GLES20.glGetShaderInfoLog(s))
            return s
        }
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, sh(GLES20.GL_VERTEX_SHADER, vs))
        GLES20.glAttachShader(p, sh(GLES20.GL_FRAGMENT_SHADER, fs))
        GLES20.glLinkProgram(p)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0)
        if (ok[0] == 0) Log.e("GlUtil", "link: " + GLES20.glGetProgramInfoLog(p))
        return p
    }

    fun buffer(capacityFloats: Int): FloatBuffer =
        ByteBuffer.allocateDirect(capacityFloats * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

    fun buffer(data: FloatArray): FloatBuffer =
        buffer(data.size).apply { put(data); position(0) }

    /** hue 0..1 -> neon RGB */
    fun hue(h: Float, out: FloatArray, sat: Float = 1f, bright: Float = 1f) {
        val x = (h - kotlin.math.floor(h)) * 6f
        val i = x.toInt() % 6
        val f = x - i
        val q = 1f - f
        val (r, g, b) = when (i) {
            0 -> Triple(1f, f, 0f); 1 -> Triple(q, 1f, 0f); 2 -> Triple(0f, 1f, f)
            3 -> Triple(0f, q, 1f); 4 -> Triple(f, 0f, 1f); else -> Triple(1f, 0f, q)
        }
        out[0] = (r * sat + (1f - sat)) * bright
        out[1] = (g * sat + (1f - sat)) * bright
        out[2] = (b * sat + (1f - sat)) * bright
    }
}
