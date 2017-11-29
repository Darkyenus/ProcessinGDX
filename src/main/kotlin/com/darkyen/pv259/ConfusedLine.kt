package com.darkyen.pv259

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils.lerp
import com.darkyen.processingdx.*
import com.darkyen.processingdx.functions.Settings

/**
 *
 */
object ConfusedLine : Applet() {

    override fun Settings.settings() {
        size(1000, 800)
    }

    private val points = mutableListOf<Point>()
    private var nextPoint:Point = nextPoint()
    private var progressToNext = 0f
    private var distanceToNext = 0f

    private fun nextPoint() = Point(Random.nextInt(width).toFloat(), Random.nextInt(height).toFloat(), randomColor(), 2f + Random.nextFloat() * 30f)

    val stroke by lazy {Stroke(5f, DebugFill, join = Stroke.Join.Round, cap = Stroke.Cap.Round)}

    override fun setup() {
        val firstPoint = nextPoint()
        points.add(firstPoint)
        distanceToNext = firstPoint.distance(nextPoint)

        background(0.1f, 0.1f, 0.1f)
    }

    var speedModifier = 0f

    override fun DrawBatch.draw(delta:Float) {
        speedModifier += delta * 0.1f
        var remainingDelta = delta

        // Update
        while (true) {
            val speed = nextPoint.speed() * speedModifier
            val deltaNeeded = (distanceToNext - progressToNext) / speed

            if (deltaNeeded >= remainingDelta) {
                progressToNext += remainingDelta * speed
                break
            } else {
                progressToNext = 0f
                remainingDelta -= deltaNeeded
                points.add(nextPoint)
                nextPoint = nextPoint()
                distanceToNext = points.last().distance(nextPoint)
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

                vertexWC(lerp(lastPoint.x, nextPoint.x, alpha),
                        lerp(lastPoint.y, nextPoint.y, alpha),
                        lerp(lastPoint.width, nextPoint.width, alpha),
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

        fun speed():Float = 3000f / width
    }
}