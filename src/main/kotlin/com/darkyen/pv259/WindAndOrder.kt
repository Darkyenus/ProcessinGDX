package com.darkyen.pv259

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Vector2
import com.darkyen.processingdx.*
import com.darkyen.processingdx.functions.Settings

/**
 *
 */
object WindAndOrder:Applet() {

    val grids = arrayOf(Grid(200, 10f, 5f) { solidColorFill(rgb(0.9f)) },
            Grid(200, 10f, 5f) { solidColorFill(rgb(0.5f))},
            Grid(200, 10f, 5f) { solidColorFill(rgb(0.3f))}
    )

    override fun Settings.settings() {
        size(1000, 800)
        clampMouse()
    }

    val line = mutableListOf<Triple<Int, Int, Int>>()
    init {
        line.add(Triple(0, grids[0].pointAmount/2, grids[0].pointAmount/2))
    }

    fun addLinePoint() {
        val (_, lastX, lastY) = line.last()

        val gridIndex = Random.nextInt(grids.size)
        val grid = grids[gridIndex]

        if (MathUtils.randomBoolean()) {
            val x = MathUtils.clamp(lastX + MathUtils.random(-2, 2), 0, grid.pointAmount)
            line.add(Triple(gridIndex, x, lastY))
        } else {
            val y = MathUtils.clamp(lastY + MathUtils.random(-2, 2), 0, grid.pointAmount)
            line.add(Triple(gridIndex, lastX, y))
        }

    }

    var sinceLastPoint = 0f

    val alternatingStroke = Stroke(1f, object :Fill {

        var phase = true

        val Light = rgb(0.9f)
        val Dark = rgb(0.7f)

        override fun color(sceneX: Float, sceneY: Float): Float {
            phase = !phase
            return if (phase) {
                Light
            } else {
                Dark
            }
        }

        override fun color(sceneX: Float, sceneY: Float, lineVertex: Int, lineVertexCount: Int): Float {
            return rgb(lineVertex.toFloat() / lineVertexCount)
        }
    })

    override fun DrawBatch.draw(delta: Float) {
        background(rgb(0.1f))

        sinceLastPoint -= delta
        while (sinceLastPoint <= 0) {
            sinceLastPoint += Random.nextFloat() * 0.1f + 0.01f

            addLinePoint()
        }

        draw(DrawBatch.Blend.None) {
            grids[0].transform.idt()
                    .translate(mouseX + MathUtils.sinDeg(mouseY) * 10f, mouseY + MathUtils.sinDeg(mouseX) * 10f)
                    .rotate(Math.sin(time).toFloat() * 1.3f)
                    .translate(- grids[0].size/2f, - grids[0].size/2f)

            grids[1].transform.idt()
                    .translate(mouseX, mouseY)
                    .rotate(mouseX)
                    .translate(- grids[1].size/2f, - grids[1].size/2f)

            grids[2].transform.idt()
                    .translate(mouseX, mouseY)
                    .rotate(time.toFloat())
                    .translate(- grids[2].size/2f, - grids[2].size/2f)

            for (grid in grids) {
                grid.draw(this)
            }

            line(alternatingStroke) {
                val v = Vector2()

                for ((gridI, x, y) in line) {
                    val grid = grids[gridI]
                    v.set(grid.points[x][y]).mul(grid.transform)

                    vertex(v.x, v.y)
                }
            }
        }

    }

    class Grid (val pointAmount:Int, spacing:Float, val pointSide:Float, fillCreator:Grid.() -> Fill) {

        val size = pointAmount * spacing

        val transform = Matrix3()

        val points = Array(pointAmount) { x -> Array(pointAmount) { y ->
            Vector2(x*spacing, y*spacing)
        } }

        val PointFill = fillCreator()

        fun draw(batch: DrawBatch) {
            val v = Vector2()

            points.forEachIndexed { x, row ->
                row.forEachIndexed { y, point ->

                    v.set(point).mul(transform)

                    batch.rectangleCentered(v.x, v.y, pointSide, fill = PointFill)
                }
            }
        }

    }
}