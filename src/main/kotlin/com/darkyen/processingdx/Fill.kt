package com.darkyen.processingdx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.darkyen.processingdx.DrawBatch.Companion.Black
import com.darkyen.processingdx.DrawBatch.Companion.White

/**
 *
 */
interface Fill {
    fun color(sceneX:Float, sceneY:Float, objectX:Float, objectY:Float, objectWidth:Float, objectHeight:Float):Float = color(sceneX, sceneY)
    fun color(sceneX:Float, sceneY:Float, lineVertex:Int, lineVertexCount:Int):Float = color(sceneX, sceneY)
    fun color(sceneX:Float, sceneY:Float):Float = White
    fun texture(): Texture? = null
    fun textureU(sceneX:Float, sceneY:Float, objectX:Float, objectY:Float, objectWidth:Float, objectHeight:Float):Float = textureU(sceneX, sceneY)
    fun textureV(sceneX:Float, sceneY:Float, objectX:Float, objectY:Float, objectWidth:Float, objectHeight:Float):Float = textureV(sceneX, sceneY)
    fun textureU(sceneX:Float, sceneY:Float):Float = 0f
    fun textureV(sceneX:Float, sceneY:Float):Float = 0f
}

val WhiteFill = solidColorFill(White)
val BlackFill = solidColorFill(Black)

fun solidColorFill(color:Float):Fill = object : Fill {
    override fun color(sceneX: Float, sceneY: Float): Float = color
}

class MutableSolidColorFill(var color: Float = rgb(1f)) : Fill {
    override fun color(sceneX: Float, sceneY: Float): Float {
        return color
    }
}

val DebugFill = object : Fill {
    override fun color(sceneX: Float, sceneY: Float, objectX: Float, objectY: Float, objectWidth: Float, objectHeight: Float): Float {
        val u = (sceneX - objectX) / objectWidth
        val v = (sceneY - objectY) / objectHeight

        return Color.toFloatBits(u, v, 1f, 1f)
    }

    override fun color(sceneX: Float, sceneY: Float, lineVertex: Int, lineVertexCount: Int): Float {
        var x = (sceneX / Gdx.graphics.width) % 1f
        var y = (sceneY / Gdx.graphics.height) % 1f
        if (x < 0f) x += 1f
        if (y < 0f) y += 1f
        return Color.toFloatBits(lineVertex.toFloat() / lineVertexCount, x, y, 1f)
    }

    override fun color(sceneX: Float, sceneY: Float): Float {
        var r = (sceneX / Gdx.graphics.width) % 1f
        var g = (sceneY / Gdx.graphics.height) % 1f
        if (r < 0f) r += 1f
        if (g < 0f) g += 1f

        return Color.toFloatBits(r, g, 1f, 1f)
    }
}

fun tiledTextureFill(texture: String, scale:Float = 0.5f):Fill {
    val t = texture(texture)
    return tiledTextureFill(t, t.width.toFloat() * scale, t.height.toFloat() * scale)
}

fun tiledTextureFill(texture: Texture, width:Float = texture.width.toFloat(), height:Float = texture.height.toFloat()):Fill = object : Fill {
    override fun texture(): Texture? = texture

    override fun textureU(sceneX: Float, sceneY: Float): Float = sceneX / width

    override fun textureV(sceneX: Float, sceneY: Float): Float = sceneY / height
}