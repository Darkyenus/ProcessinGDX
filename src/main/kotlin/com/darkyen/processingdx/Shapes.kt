package com.darkyen.processingdx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ByteArray as Bytes
import com.badlogic.gdx.utils.FloatArray as Floats
import com.badlogic.gdx.utils.IntArray as Ints

/**
 *
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun DrawBatch.vertex(x:Float, y:Float, fill:Fill, objX:Float, objY:Float, objWidth:Float, objHeight:Float):Short {
    return vertex(x, y, fill.color(x, y, objX, objY, objWidth, objHeight), fill.textureU(x, y, objX, objY, objWidth, objHeight), fill.textureV(x, y, objX, objY, objWidth, objHeight))
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun DrawBatch.expect(vCalls:Int, iCalls:Int, fill: Fill) {
    expect(vCalls, iCalls)
    val tex = fill.texture()
    if (tex != null) {
        this.texture = tex
    }
}

fun background(color:Float) {
    background(color.red, color.green, color.blue, color.alpha)
}

fun background(r:Float, g:Float, b:Float, alpha:Float = 1f) {
    Gdx.gl20.glClearColor(r, g, b, alpha)
    Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT)
}

fun DrawBatch.rectangleCentered(x:Float, y:Float, width:Float, height:Float = width, fill:Fill = WhiteFill, stroke:Stroke? = null){
    rectangle(x - width/2f, y - height/2f, width, height, fill, stroke)
}

fun DrawBatch.rectangle(x:Float, y:Float, width:Float, height:Float = width, fill:Fill = WhiteFill, stroke:Stroke? = null){
    expect(4, 2, fill)

    val bl = vertex(x, y, fill, x, y, width, height)
    val tl = vertex(x, y + height, fill, x, y, width, height)
    val tr = vertex(x+width, y + height, fill, x, y, width, height)
    val br = vertex(x + width, y, fill, x, y, width, height)

    indicesRect(bl, tl, tr, br)

    if (stroke != null) {
        lineLoop(stroke) {
            vertex(x, y)
            vertex(x, y + height)
            vertex(x+width, y+height)
            vertex(x+width, y)
        }
    }
}

fun DrawBatch.rectangle(x:Float, y:Float, width:Float, height:Float = width, texture: Texture, color: Float = DrawBatch.White){
    expect(4, 2)
    this.texture = texture

    val bl = vertex(x, y, color, 0f, 0f)
    val tl = vertex(x, y + height, color, 0f, 1f)
    val tr = vertex(x+width, y + height, color, 1f, 1f)
    val br = vertex(x + width, y, color, 1f, 0f)

    indicesRect(bl, tl, tr, br)
}

fun DrawBatch.rectangle(x:Float, y:Float, width:Float, height:Float = width, texture:TextureRegion, color: Float = DrawBatch.White){
    expect(4, 2)
    this.texture = texture.texture

    val bl = vertex(x, y, color, texture.u, texture.v)
    val tl = vertex(x, y + height, color, texture.u, texture.v2)
    val tr = vertex(x+width, y + height, color, texture.u2, texture.v2)
    val br = vertex(x + width, y, color, texture.u2, texture.v)

    indicesRect(bl, tl, tr, br)
}

fun DrawBatch.ellipseCentered(x:Float, y:Float, width:Float, height:Float = width, fill:Fill = WhiteFill, stroke:Stroke? = null) {
    ellipse(x - width/2f, y - height/2f, width, height, fill, stroke)
}

fun DrawBatch.ellipse(x:Float, y:Float, width:Float, height:Float = width, fill:Fill = WhiteFill, stroke:Stroke? = null) {
    val radiusX = width/2
    val radiusY = height/2
    val centerX = x + radiusX
    val centerY = y + radiusY

    val quarterCount = Math.max(1, Math.round(Math.max(Math.abs(width), Math.abs(height)) / 16f))
    val step = Math.PI / 2.0 / (quarterCount + 1)
    val points = FloatArray(quarterCount * 2)
    expect(4*(quarterCount+1) + 1, 4*(quarterCount+1), fill)//TODO Check

    val center = vertex(centerX, centerY, fill, x, y, width, height)

    var angle = step
    for (i in 0 until quarterCount) {
        points[2*i] = Math.sin(angle).toFloat()
        points[2*i+1] = Math.cos(angle).toFloat()
        angle += step
    }

    val top = vertex(centerX, centerY + radiusY, fill, x, y, width, height)
    val left = vertex(centerX - radiusX, centerY, fill, x, y, width, height)
    val right = vertex(centerX + radiusX, centerY, fill, x, y, width, height)
    val down = vertex(centerX, centerY - radiusY, fill, x, y, width, height)

    var tr = top
    var tl = left
    var bl = down
    var br = right

    for (i in 0..quarterCount) {
        var ntr:Short
        var ntl:Short
        var nbl:Short
        var nbr:Short
        if (i == quarterCount) {
            ntr = right
            ntl = top
            nbl = left
            nbr = down
        } else {
            ntr = vertex(centerX + points[2*i] * radiusX, centerY + points[2*i+1] * radiusY, fill, x, y, width, height)
            ntl = vertex(centerX - points[2*i+1] * radiusX, centerY + points[2*i] * radiusY, fill, x, y, width, height)
            nbl = vertex(centerX - points[2*i] * radiusX, centerY - points[2*i+1] * radiusY, fill, x, y, width, height)
            nbr = vertex(centerX + points[2*i+1] * radiusX, centerY - points[2*i] * radiusY, fill, x, y, width, height)
        }

        indices(center, tr, ntr)
        indices(center, tl, ntl)
        indices(center, bl, nbl)
        indices(center, br, nbr)

        tr = ntr
        tl = ntl
        bl = nbl
        br = nbr
    }

    if (stroke != null) {
        lineLoop(stroke) {
            // Top right
            for (i in 0 until quarterCount) {
                vertex(centerX + points[2*i] * radiusX, centerY + points[2*i+1] * radiusY)
            }
            vertex(centerX + radiusX, centerY)

            // Bottom right
            for (i in 0 until quarterCount) {
                vertex(centerX + points[2*i+1] * radiusX, centerY - points[2*i] * radiusY)
            }
            vertex(centerX, centerY - radiusY)

            // Bottom left
            for (i in 0 until quarterCount) {
                vertex(centerX - points[2*i] * radiusX, centerY - points[2*i+1] * radiusY)
            }
            vertex(centerX - radiusX, centerY)

            // Top left
            for (i in 0 until quarterCount) {
                vertex(centerX - points[2*i+1] * radiusX, centerY + points[2*i] * radiusY)
            }
            vertex(centerX, centerY + radiusY)
        }
    }
}

val SharedLineDrawer = LineDrawer()

fun <T> DrawBatch.lineLoop(stroke: Stroke, func:LineDrawing.() -> T):T {
    val lineDrawer = SharedLineDrawer
    lineDrawer.init(stroke)
    val result = lineDrawer.func()
    lineDrawer.doneLoop(this)
    return result
}

fun <T> DrawBatch.line(stroke: Stroke, func:LineDrawing.() -> T):T {
    val lineDrawer = SharedLineDrawer
    lineDrawer.init(stroke)
    val result = lineDrawer.func()
    lineDrawer.doneLine(this)
    return result
}
