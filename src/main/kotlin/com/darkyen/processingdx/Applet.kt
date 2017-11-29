package com.darkyen.processingdx

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.darkyen.processingdx.functions.General
import com.darkyen.processingdx.functions.Settings
import java.util.*

/**
 *
 */
typealias KeyCode = Int

open class Applet : General {

    private val windowViewport = ScreenViewport(OrthographicCamera())
    private val canvasViewport = ScreenViewport(OrthographicCamera())
    private lateinit var batch:DrawBatch

    private val applicationListener = object : ApplicationListener, InputProcessor {

        var lastX:Float = 0f
        var lastY:Float = 0f

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val mousePosition = mousePosition(screenX, screenY)
            eventProcessed = true
            this@Applet.mousePressed(mousePosition.x, mousePosition.y, button)
            lastX = mousePosition.x
            lastY = mousePosition.y
            return eventProcessed
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            val mousePosition = mousePosition(screenX, screenY)
            eventProcessed = true
            this@Applet.mouseDragged(mousePosition.x, mousePosition.y, lastX, lastY)
            lastX = mousePosition.x
            lastY = mousePosition.y
            return eventProcessed
        }

        override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
            val mousePosition = mousePosition(screenX, screenY)
            eventProcessed = true
            this@Applet.mouseMoved(mousePosition.x, mousePosition.y, lastX, lastY)
            lastX = mousePosition.x
            lastY = mousePosition.y
            return eventProcessed
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val mousePosition = mousePosition(screenX, screenY)
            eventProcessed = true
            this@Applet.mouseReleased(mousePosition.x, mousePosition.y, button)
            lastX = mousePosition.x
            lastY = mousePosition.y
            return eventProcessed
        }

        override fun keyDown(keycode: Int): Boolean {
            if (keycode == Input.Keys.F3) {
                println("FPS: ${Gdx.graphics.framesPerSecond}")
            } else if (keycode == Input.Keys.F4) {
                screenshot("${this@Applet.javaClass.simpleName}-${System.currentTimeMillis()}.png")
            }

            eventProcessed = true
            this@Applet.keyPressed(keycode)
            return eventProcessed
        }

        override fun keyUp(keycode: Int): Boolean {
            eventProcessed = true
            this@Applet.keyReleased(keycode)
            return eventProcessed
        }

        override fun keyTyped(character: Char): Boolean {
            eventProcessed = true
            this@Applet.keyTyped(character)
            return eventProcessed
        }

        override fun scrolled(amount: Int): Boolean {
            eventProcessed = true
            this@Applet.mouseWheel(amount)

            if (!eventProcessed && separateCanvas) {
                separateCanvasScale = MathUtils.clamp(separateCanvasScale + amount * 0.05f, 0.05f, 50f)
                return true
            }

            return eventProcessed
        }

        private var screenFramebuffer:FrameBuffer? = null
        private var resizeScreenFramebuffer = true

        override fun create() {
            Gdx.input.inputProcessor = this

            batch = if (separateCanvas) {
                canvasViewport.update(width, height, true)
                DrawBatch(canvasViewport)
            } else {
                DrawBatch(windowViewport)
            }
        }

        override fun resize(width: Int, height: Int) {
            val lastW = this@Applet.windowWidth
            val lastH = this@Applet.windowHeight

            this@Applet.windowWidth = width
            this@Applet.windowHeight = height
            windowViewport.update(width, height, true)
            if (!separateCanvas) {
                resizeScreenFramebuffer = true
            }

            this@Applet.resized(width, height, lastW, lastH)
        }

        override fun resume() {
            //TODO
        }

        private val mousePosition_TMP = Vector2()
        fun mousePosition(screenX:Int, screenY:Int):Vector2 {
            val p = windowViewport.unproject(mousePosition_TMP.set(screenX.toFloat(), screenY.toFloat()))
            if (settings.clampMouse) {
                p.x = MathUtils.clamp(p.x, 0f, width.toFloat())
                p.y = MathUtils.clamp(p.y, 0f, height.toFloat())
            }
            return p
        }

        private var shouldSetup = true

        override fun render() {
            Gdx.gl20.glClearColor(0.1f, 0.1f, 0.1f, 1f)
            Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT or GL20.GL_STENCIL_BUFFER_BIT)

            pMouseX = mouseX
            pMouseY = mouseY
            val mousePosition = mousePosition(Gdx.input.x, Gdx.input.y)
            mouseX = mousePosition.x
            mouseY = mousePosition.y

            val oldFramebuffer = screenFramebuffer
            val framebuffer:FrameBuffer
            if (oldFramebuffer == null || resizeScreenFramebuffer) {
                val fbWidth:Int
                val fbHeight:Int
                if (separateCanvas) {
                    fbWidth = width
                    fbHeight = height
                } else {
                    fbWidth = Gdx.graphics.backBufferWidth
                    fbHeight = Gdx.graphics.backBufferHeight
                }

                framebuffer = FrameBuffer(Pixmap.Format.RGBA8888, fbWidth, fbHeight, false, false)
                resizeScreenFramebuffer = false
                canvasViewport.update(fbWidth, fbHeight)
            } else {
                framebuffer = oldFramebuffer
            }

            framebuffer.begin()
            if (separateCanvas) {
                Gdx.gl.glViewport(0, 0, width, height)
            } else {
                windowViewport.apply()
            }
            if (oldFramebuffer == null) {
                Gdx.gl20.glClearColor(0f, 0f, 0f, 1f)
                Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT or GL20.GL_STENCIL_BUFFER_BIT)
            } else if (oldFramebuffer !== framebuffer) {
                Gdx.gl20.glClearColor(0f, 0f, 0f, 1f)
                Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT or GL20.GL_STENCIL_BUFFER_BIT)
                batch.drawFramebuffer(oldFramebuffer)
                oldFramebuffer.dispose()
            }

            if (shouldSetup) {
                setup()
                shouldSetup = false
            }

            val delta = Gdx.graphics.deltaTime
            time += delta
            this@Applet.batch.draw(delta)

            if (scheduledScreenshot != null) {
                val pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, framebuffer.width, framebuffer.height)
                val file = Gdx.files.local(scheduledScreenshot)
                PixmapIO.writePNG(file, pixmap)
                pixmap.dispose()
                println("Screnshot taken ${file.file().canonicalPath}")

                scheduledScreenshot = null
            }

            framebuffer.end()
            this.screenFramebuffer = framebuffer

            HdpiUtils.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
            if (separateCanvas) {
                windowViewport.apply()

                val scaledWidth = width * separateCanvasScale
                val scaledHeight = height * separateCanvasScale
                val x = if (scaledWidth < windowWidth) {
                    (windowWidth - scaledWidth) / 2f
                } else {
                    (windowWidth - scaledWidth) * (mouseX / windowWidth)
                }
                val y = if (scaledHeight < windowHeight) {
                    (windowHeight - scaledHeight) / 2f
                } else {
                    (windowHeight - scaledHeight) * (mouseY / windowHeight)
                }

                batch.drawFramebuffer(framebuffer,
                        x / windowWidth, y / windowHeight,
                        scaledWidth / windowWidth, scaledHeight / windowHeight)
            } else {
                batch.drawFramebuffer(framebuffer)
            }
        }

        override fun pause() {
            //TODO
        }

        override fun dispose() {
            this@Applet.batch.dispose()
        }
    }

    private val settings = object : Settings {
        var initialAllowHiDpi = true
        var initialSmoothLevel = 2
        var initialFullScreen = false

        var clampMouse = false

        override fun size(width: Int, height: Int) {
            this@Applet.windowWidth = width
            this@Applet.windowHeight = height
        }

        override fun canvasSize(width: Int, height: Int) {
            this@Applet.separateCanvas = true
            this@Applet.width = width
            this@Applet.height = height
        }

        override fun sizeMode(allowHiDpi: Boolean) {
            this.initialAllowHiDpi = allowHiDpi
        }

        override fun smooth(level: Int) {
            initialSmoothLevel = level
        }

        override fun fullScreen() {
            initialFullScreen = true
        }

        override fun clampMouse() {
            clampMouse = true
        }
    }

    val Random = Random()

    var windowWidth:Int = 1000
        private set

    var windowHeight:Int = 800
        private set

    private var separateCanvas = false
    private var separateCanvasScale = 1f

    var width:Int = -1
        get() = if (separateCanvas) field else windowWidth
        private set

    var height:Int = -1
        get() = if (separateCanvas) field else windowHeight
        private set


    var mouseX:Float = 0f
        private set
    var mouseY:Float = 0f
        private set
    var pMouseX:Float = 0f
        private set
    var pMouseY:Float = 0f
        private set

    var time:Double = 0.0

    private var scheduledScreenshot:String? = null

    fun screenshot(name:String) {
        scheduledScreenshot = name
    }

    open fun Settings.settings() {}

    /**
     * The setup() function is run once, when the program starts. It's used to define initial environment properties such as screen size and to load media such as images and fonts as the program starts.
     */
    open fun setup() {}

    /**
     * Called directly after setup(), the draw() function continuously executes the lines of code contained inside its block until the program is stopped or noLoop() is called. draw() is called automatically and should never be called explicitly. All Processing programs update the screen at the end of draw(), never earlier.
     *
     * To stop the code inside of draw() from running continuously, use noLoop(), redraw() and loop(). If noLoop() is used to stop the code in draw() from running, then redraw() will cause the code inside draw() to run a single time, and loop() will cause the code inside draw() to resume running continuously.
     *
     * The number of times draw() executes in each second may be controlled with the frameRate() function.
     *
     * It is common to call background() near the beginning of the draw() loop to clear the contents of the window, as shown in the first example above. Since pixels drawn to the window are cumulative, omitting background() may result in unintended results.
     */
    open fun DrawBatch.draw(delta:Float) {
        exit()
    }

    private var eventProcessed = false

    open fun mousePressed(mouseX:Float, mouseY:Float, button:Int) {
        eventProcessed = false
    }
    open fun mouseReleased(mouseX:Float, mouseY:Float, button:Int) {
        eventProcessed = false
    }
    open fun mouseMoved(mouseX:Float, mouseY:Float, previousMouseX:Float, previousMouseY:Float) {
        eventProcessed = false
    }
    open fun mouseDragged(mouseX:Float, mouseY:Float, previousMouseX:Float, previousMouseY:Float) {
        eventProcessed = false
    }
    open fun mouseWheel(steps:Int) {
        eventProcessed = false
    }

    open fun keyPressed(key:KeyCode) {
        eventProcessed = false
    }
    open fun keyReleased(key:KeyCode) {
        eventProcessed = false
    }
    open fun keyTyped(key:Char) {
        eventProcessed = false
    }

    open fun resized(width:Int, height:Int, previousWidth:Int, previousHeight:Int) {}

    override final fun exit() {
        Gdx.app.exit()
    }

    override final fun loop() {
        Gdx.graphics.isContinuousRendering = true
    }

    override final fun noLoop() {
        Gdx.graphics.isContinuousRendering = false
    }

    override final fun redraw() {
        Gdx.graphics.requestRendering()
    }

    fun start() {
        settings.settings()
        val config = Lwjgl3ApplicationConfiguration()
        config.setTitle("PV256")
        config.useOpenGL3(true, 3, 2)
        config.setBackBufferConfig(8, 8, 8, 0, 0, 0, 0)
        config.useVsync(true)
        config.setHdpiMode(Lwjgl3ApplicationConfiguration.HdpiMode.Logical)
        if (settings.initialFullScreen) {
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode())
        } else {
            config.setWindowedMode(windowWidth, windowHeight)
        }

        Lwjgl3Application(this@Applet.applicationListener, config)
    }
}