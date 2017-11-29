@file:Suppress("unused")

package com.darkyen.pv259

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.MathUtils
import com.darkyen.Floats
import com.darkyen.Shorts
import com.darkyen.forEachIndexed
import com.darkyen.processingdx.*

/**
 *
 */
object DrawSound : Applet() {

    private val samplesPerPixel:Float = 1f
    private var maxSamples = 1
    private val samples:Floats = Floats()

    override fun resized(width: Int, height: Int, previousWidth: Int, previousHeight: Int) {
        val sampleCount = (width * samplesPerPixel).toInt()
        maxSamples = sampleCount
        if (samples.size > maxSamples) {
            samples.size = maxSamples
        } else {
            samples.ensureCapacity(sampleCount - samples.size)
        }
    }

    override fun mousePressed(mouseX: Float, mouseY: Float, button: Int) {
        mouseDragged(mouseX, mouseY, mouseX, mouseY)
    }

    private fun Float.xToSample():Int {
        return MathUtils.clamp(Math.round(this / samplesPerPixel), 0, maxSamples)
    }

    private fun Float.yToAmount():Float {
        return MathUtils.clamp((this / height) * 2f - 1f, -1f, 1f)
    }

    override fun mouseDragged(mouseX: Float, mouseY: Float, previousMouseX: Float, previousMouseY: Float) {
        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            // Erase mode
            val sample = mouseX.xToSample()
            if (sample < samples.size) {
                samples.size = sample
            }
        } else {
            // Draw mode
            var fromSample = previousMouseX.xToSample()
            var toSample = mouseX.xToSample()
            var fromAmount = previousMouseY.yToAmount()
            var toAmount = mouseY.yToAmount()

            if (fromSample > toSample) {
                val tmpSample = fromSample
                val tmpAmount = fromAmount
                fromSample = toSample
                fromAmount = toAmount
                toSample = tmpSample
                toAmount = tmpAmount
            }

            val fillSample = if (samples.size == 0) 0f else samples[samples.size - 1]
            while (toSample >= samples.size) {
                samples.add(fillSample)
            }


            val sampleItems = samples.items
            if (fromSample == toSample) {
                sampleItems[fromSample] = fromAmount
            } else {
                var alpha = 0f
                val alphaStep = 1f / (toSample - fromSample)
                for(s in fromSample..toSample) {
                    sampleItems[s] = MathUtils.lerp(fromAmount, toAmount, alpha)
                    alpha += alphaStep
                }
            }
        }
    }

    private val applyKernelTMP = Floats()
    private fun applyKernel(kernel:FloatArray, offset:Int = -kernel.size / 2) {
        val sampleCount = samples.size
        val sampleItems = samples.items
        val applied = applyKernelTMP.ensureCapacity(sampleCount)

        for (i in 0 until sampleCount) {
            var sum = 0f

            @Suppress("LoopToCallChain")
            for (k in 0 until kernel.size) {
                sum += sampleItems[Math.floorMod(i + k + offset, sampleCount)] * kernel[k]
            }

            applied[i] = MathUtils.clamp(sum, -1f, 1f)
        }

        System.arraycopy(applied, 0, sampleItems, 0, sampleCount)
    }

    override fun keyTyped(key: Char) {
        when (key) {
            'r' -> {
                val items = samples.items
                val size = samples.size
                for (i in 0 until size) {
                    items[i] = 0f
                }
            }
            's' -> {
                val items = samples.items
                val size = samples.size
                for (i in 0 until size) {
                    items[i] = Math.sin(i * Math.PI * 2.0 / size).toFloat()
                }
            }
            'a' -> {
                applyKernel(floatArrayOf(1f/5f, 1f/5f, 1f/5f, 1f/5f, 1f/5f))
            }
            'q' -> {
                applyKernel(floatArrayOf(-1f/5f, -1f/5f, 9f/5f, -1f/5f, -1f/5f))
            }
            'l' -> {
                applyKernel(floatArrayOf(0.8f))
            }
            'h' -> {
                applyKernel(floatArrayOf(1.1f))
            }
        }
    }

    val soundStroke = Stroke(2f, object : Fill {
        override fun color(sceneX: Float, sceneY: Float): Float {
            return hsb(sceneX/width, sceneY/height, sceneY/height)
        }
    })

    val crosshairStroke = Stroke(0.3f, solidColorFill(rgb(0.7f)))

    override fun DrawBatch.draw(delta: Float) {
        background(rgb(0.1f))

        draw {
            line(soundStroke) {
                samples.forEachIndexed { sample, index ->
                    val x = index / samplesPerPixel
                    val y = (sample*0.5f + 0.5f) * height

                    vertex(x, y)
                }
            }

            line(crosshairStroke) {
                vertex(mouseX, 0f)
                vertex(mouseX, height.toFloat())
            }

            line(crosshairStroke) {
                vertex(0f, mouseY)
                vertex(width.toFloat(), mouseY)
            }
        }
    }

    override fun setup() {
        val soundThread = Thread {
            try {
                val device = Gdx.audio.newAudioDevice(44000, true)

                val shortSamples = Shorts()
                while (true) {
                    val size = samples.size
                    val items = samples.items

                    shortSamples.size = 0
                    val shortSampleItems = shortSamples.ensureCapacity(size)
                    shortSamples.size = size

                    for (i in 0 until size) {
                        val sample = (items[i].toDouble() * Short.MAX_VALUE).toShort()
                        shortSampleItems[i] = sample
                    }

                    device.writeSamples(items, 0, size)

                    Thread.sleep(1)// Does not work without this for some reason
                }
            } catch (e:Throwable) {
                println("Thread failed:")
                e.printStackTrace()
            }
        }
        soundThread.name = "SoundThread"
        soundThread.isDaemon = true
        soundThread.start()
    }
}