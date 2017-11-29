package com.darkyen.pv259

import com.badlogic.gdx.math.MathUtils
import com.darkyen.processingdx.*
import com.badlogic.gdx.utils.FloatArray as Floats
import com.badlogic.gdx.utils.Array as Objects
import com.darkyen.processingdx.functions.Settings

@Suppress("unused")
/**
 *
 */
object Feeling : Applet() {

    override fun Settings.settings() {
        size(1000, 800)
    }

    val drapes = Objects<Drape>()

    override fun setup() {
        for (i in 0..10) {
            drapes.add(Drape().apply {
                x += direction * MathUtils.random(0f, 60f)
            })
        }
    }


    override fun DrawBatch.draw(delta: Float) {
        val iterator = drapes.iterator()
        var add = 0
        while (iterator.hasNext()) {
            val drape = iterator.next()

            if (drape.shouldRemove(delta)) {
                iterator.remove()
                add = 1
            }
        }
        for (i in 0 until add) {
            drapes.add(Drape())
        }

        background(hsb(HueBlue, 0.3f, 0.1f))

        draw {
            for (drape in drapes) {
                line(drape.drapeStroke) {
                    drape.points.forEachIndexed {index, value ->
                        if (index in (drape.invalid until drape.points.size - drape.invalid)) {
                            vertex(drape.x + drape.pointStep * index, drape.y + value)
                        }
                    }
                }

            }
        }

    }

    class Drape {
        val pointStep = MathUtils.random(5f, 20f)
        val points = FloatArray(maxOf(100f, width / pointStep + MathUtils.random(-100, 100)).toInt()) { MathUtils.random(-50f, 50f) }
        var invalid = 0

        var x = -points.size * pointStep - 10f
        var y = MathUtils.random(10f, height - 10f)
        var direction = MathUtils.randomSign() * MathUtils.random(10f, 50f)

        val drapeStroke = Stroke(5f, solidColorFill(randomColor()))

        init {
            if (direction < 0) {
                x = width + 10f
            }
        }

        fun smooth() {
            for (i in 1 .. (points.size - 2)) {
                points[i] = (points[i-1] + points[i] + points[i+1]) / 3f
            }
        }

        fun grit() {
            for (i in 1 .. (points.size - 2)) {
                points[i] = (-0.2f*points[i-1] + (1f + 0.4f)*points[i] - 0.5f*points[i+1])
            }
            invalid++
        }

        fun mul() {
            val factor = MathUtils.random(0.5f, 2f)
            for(i in 0 until (points.size)) {
                points[i] *= factor
            }
            invalid++
        }

        init {
            for(i in 0..MathUtils.random(10)) {
                change()
            }
        }

        fun change() {
            when (MathUtils.random(2)) {
                0 -> smooth()
                1 -> grit()
                2 -> mul()
            }
        }

        fun shouldRemove(delta:Float):Boolean {
            x += delta * direction
            if (direction < 0) {
                if (x + points.size * pointStep < -10f) {
                    return true
                }
            } else {
                if (x > points.size * pointStep + 10f) {
                    return true
                }
            }

            if (MathUtils.random() < 0.01f) {
                change()
            }

            return false
        }

    }
}