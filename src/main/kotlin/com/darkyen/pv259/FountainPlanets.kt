package com.darkyen.pv259

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.MathUtils.*
import com.darkyen.Objects
import com.darkyen.processingdx.*
import com.darkyen.processingdx.DrawBatch.Companion.White
import com.darkyen.processingdx.functions.Settings

@Suppress("unused")
/**
 *
 */
object FountainPlanets : Applet() {

    override fun Settings.settings() {
        canvasSize(1024, 4096*2)
    }

    var drawAgain = true

    override fun keyTyped(key: Char) {
        if (key == ' ') {
            drawAgain = true
        }
    }

    class Planet(val x:Float, val velocity:Float)

    private val PlanetFill = solidColorFill(rgb(0.0f))

    override fun DrawBatch.draw(delta: Float) {
        if (drawAgain) {
            drawAgain = false
            background(White)

            val stepPx = 1
            val stepTime = 10f

            val planetCount = random(4, 40)
            println("PlanetCount: $planetCount")
            val planetMass = FloatArray(planetCount) { random(1f, 10f) }

            val planets = Objects<Array<Planet>>(height/stepPx + 8)
            var previous = Array(planetCount) { Planet(random(-200f, width.toFloat()+200f), 0f) }
            planets.add(previous)

            var genY = stepPx
            while (genY <= height) {
                val next = Array(planetCount) {
                    resolveI ->

                    val resolve = previous[resolveI]
                    var force = 0f
                    for (otherI in 0 until planetCount) {
                        if (resolveI == otherI) {
                            continue
                        }

                        val other = previous[otherI]

                        val mass = planetMass[resolveI] * planetMass[otherI]
                        val distance = other.x - resolve.x

                        var rawForce = mass / (distance * distance)
                        rawForce = MathUtils.clamp(rawForce, 0.0001f, 0.3f)

                        force += Math.copySign(rawForce, distance) * 0.01f
                    }

                    val acceleration = force / planetMass[resolveI]

                    Planet(resolve.x + resolve.velocity * stepTime + 0.5f * acceleration * stepTime,
                            resolve.velocity + acceleration * stepTime)
                }

                planets.add(next)
                previous = next

                genY += stepPx
            }

            // Done simulating, lets draw

            draw {
                for (planetI in 0 until planetCount) {
                    line(Stroke(planetMass[planetI], PlanetFill)) {
                        var y = 0f
                        for (planet in planets) {

                            vertex(planet[planetI].x, y)

                            y += stepPx
                        }
                    }
                }
            }

        }
    }
}