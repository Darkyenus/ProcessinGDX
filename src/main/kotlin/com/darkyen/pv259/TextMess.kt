package com.darkyen.pv259

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Align
import com.darkyen.processingdx.*
import com.darkyen.processingdx.font.Font
import com.darkyen.processingdx.font.GlyphLayout

@Suppress("unused")
/**
 *
 */
object TextMess : Applet() {

    val font by lazy(LazyThreadSafetyMode.NONE) {
        Font(Gdx.files.internal("fonts/Runa Sans.stbfont"))
    }

    val layout by lazy(LazyThreadSafetyMode.NONE) {
        GlyphLayout(font, true)
    }

    val padding = 20f

    val text = StringBuilder()

    val textStride = 1 /* letter */ + 3 /* {#...} */ + 3*2 /* color */

    fun StringBuilder.appendHex(num:Float){
        val hex = MathUtils.clamp(Math.round(num * 255f), 0, 255).toString(16)
        if (hex.length == 1) {
            append('0')
        }
        append(hex)
    }

    override fun keyTyped(key: Char) {
        if (key == 8.toChar()) {
            text.setLength(maxOf(0, text.length - textStride))
        } else {
            text.append("{#")
            val color = hsb((text.length/ textStride) / 200f, 0.9f, 0.9f)
            text.appendHex(color.red)
            text.appendHex(color.green)
            text.appendHex(color.blue)
            text.append("}")
            text.append(key)
        }
    }

    override fun keyReleased(key: KeyCode) {
        if (key == Input.Keys.DEL && (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT))) {
            text.setLength(0)
        }
    }

    override fun DrawBatch.draw(delta: Float) {
        background(rgb(0.2f))
        draw {
            val x = padding
            val y = height - padding

            val oldLength = text.length
            text.append(System.currentTimeMillis())
            layout.setText(text, rgb(1f), width - padding - padding, Align.left)
            text.setLength(oldLength)
            layout.draw(this, x, y)
        }
    }
}