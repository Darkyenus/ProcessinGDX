package com.darkyen.pv259

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.darkyen.processingdx.*
import com.darkyen.processingdx.functions.Settings

@Suppress("unused")
/**
 *
 */
object ChaoticPainter : Applet() {

    private lateinit var template:Pixmap

    override fun Settings.settings() {
        size(1080, 720)
        clampMouse()
    }

    private val points = mutableListOf<Point>()
    private lateinit var nextPoint:Point
    private var progressToNext = 0f
    private var distanceToNext = 0f

    private fun nextPoint(prevX:Float, prevY:Float): Point {
        val LineLength = 2//20

        var x = prevX.toInt() + MathUtils.random(-LineLength, LineLength)
        var y = prevY.toInt() + MathUtils.random(-LineLength, LineLength)

        if (Random.nextInt(100 / LineLength) == 0) {
            if (x < mouseX) {
                x += 1
            } else {
                x -= 1
            }
            if (y < mouseY) {
                y += 1
            } else {
                y -= 1
            }
        }

        x = MathUtils.clamp(x, 0, width)
        y = MathUtils.clamp(y, 0, height)

        val color = template.getPixel(
                MathUtils.clamp(((x / width.toFloat()) * template.width.toFloat()).toInt(), 0, template.width),
                MathUtils.clamp(((1f - y / height.toFloat()) * template.height.toFloat()).toInt(), 0, template.height))
        val a = ((color ushr 0) and 0xFF) / 255f
        val r = ((color ushr 24) and 0xFF) / 255f
        val g = ((color ushr 16) and 0xFF) / 255f
        val b = ((color ushr 8) and 0xFF) / 255f

        val width = 1f + Random.nextFloat() * 8f
        //val width = Math.pow(com.badlogic.gdx.math.Vector2.dst(x.toFloat(), y.toFloat(), width/2f, height/2f).toDouble(), 0.5).toFloat()/3f

        return Point(x.toFloat(), y.toFloat(), rgb(r,g,b,a), width)

    }

    val stroke by lazy { Stroke(5f, DebugFill, join = Stroke.Join.Round, cap = Stroke.Cap.Round) }


    override fun setup() {
        template = Pixmap(Gdx.files.local("drift.jpg"))
        nextPoint = nextPoint(width/2f, height/2f)

        val firstPoint = nextPoint(nextPoint.x, nextPoint.y)
        points.add(firstPoint)
        distanceToNext = firstPoint.distance(nextPoint)

        background(0.1f, 0.1f, 0.1f)
    }

    override fun DrawBatch.draw(delta:Float) {
        var remainingDelta = delta

        // Update
        while (true) {
            val speed = nextPoint.speed()
            val deltaNeeded = (distanceToNext - progressToNext) / speed

            if (deltaNeeded >= remainingDelta) {
                progressToNext += remainingDelta * speed
                break
            } else {
                progressToNext = 0f
                remainingDelta -= deltaNeeded
                val prevPoint = nextPoint
                points.add(prevPoint)
                nextPoint = nextPoint(prevPoint.x, prevPoint.y)
                distanceToNext = prevPoint.distance(nextPoint)
            }
        }

        draw {
            line(stroke) {
                for (point in points) {
                    vertexWC(point.x, point.y, point.width, point.color)
                }
                val lastPoint = points.last()
                val nextPoint = nextPoint
                val alpha = Interpolation.fade.apply(progressToNext / distanceToNext)

                vertexWC(MathUtils.lerp(lastPoint.x, nextPoint.x, alpha),
                        MathUtils.lerp(lastPoint.y, nextPoint.y, alpha),
                        MathUtils.lerp(lastPoint.width, nextPoint.width, alpha),
                        lerpRGB(lastPoint.color, nextPoint.color, alpha))

                points.clear()
                points.add(lastPoint)
            }
        }
    }

    class Point(val x:Float, val y:Float, val color:Float, val width:Float) {
        fun distance(to:Point):Float {
            val xD = x-to.x
            val yD = y-to.y
            return Math.sqrt((xD * xD + yD * yD).toDouble()).toFloat()
        }

        fun speed():Float = 300000f / width
    }
}