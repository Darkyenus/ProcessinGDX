package com.darkyen.processingdx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.PixmapTextureData
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.viewport.Viewport

@Suppress("MemberVisibilityCanPrivate", "unused")
/**
 *
 */
class DrawBatch(val viewport:Viewport, size:Int = Short.MAX_VALUE.toInt(), defaultShader: ShaderProgram? = null, defaultTexture: Texture? = null) {

    internal val mesh: Mesh = Mesh(
            Mesh.VertexDataType.VertexBufferObjectWithVAO, false, Short.MAX_VALUE.toInt(), Short.MAX_VALUE.toInt(),
            VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
            VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
            VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"))
    internal val vertexFloatFields = mesh.vertexAttributes.vertexSize / java.lang.Float.BYTES

    internal val vertices = FloatArray(size * vertexFloatFields)
    internal var vIndex = 0
    internal val indices = ShortArray(size)
    internal var iIndex = 0

    val defaultTexture:Texture = defaultTexture ?: createDefaultTexture()
    private val ownsDefaultTexture = defaultTexture == null

    var texture:Texture = this.defaultTexture
        set(value) {
            if (field != value) {
                flush()

                field = value
                invTexWidth = 1.0f / value.width
                invTexHeight = 1.0f / value.height
            }
        }
    private var invTexWidth = 0f
    private var invTexHeight = 0f

    private var drawing = false

    var blendingEnabled = true
        set(value) {
            if (field != value) {
                flush()
                field = value
            }
        }
    var blendSrcFunc = GL20.GL_SRC_ALPHA
        private set
    var blendDstFunc = GL20.GL_ONE_MINUS_SRC_ALPHA
        private set

    fun setBlendFunction(srcFunc: Int = GL20.GL_SRC_ALPHA, dstFunc: Int = GL20.GL_ONE_MINUS_SRC_ALPHA) {
        if (blendSrcFunc == srcFunc && blendDstFunc == dstFunc) return
        flush()
        blendSrcFunc = srcFunc
        blendDstFunc = dstFunc
    }

    val defaultShader: ShaderProgram = defaultShader ?: Batch2dShader

    var shader: ShaderProgram = this.defaultShader
        set(value) {
            if (drawing) {
                flush()
                field.end()
            }
            field = value
            if (drawing) {
                field.begin()
                setupMatrices()
            }
        }


    /** Number of render calls since the last [.begin].  */
    var renderCalls = 0

    /** Number of rendering calls, ever. Will not be reset unless set manually.  */
    var totalRenderCalls = 0

    /** The maximum number of sprites rendered in one batch so far.  */
    var maxSpritesInBatch = 0

    fun begin(blend: Blend = Blend.None) {
        if (drawing) throw IllegalStateException("SpriteBatch.end must be called before begin.")
        renderCalls = 0

        Gdx.gl.glDepthMask(false)
        shader.begin()
        setupMatrices()

        drawing = true

        blend.setup()
    }

    fun end() {
        if (!drawing) throw IllegalStateException("SpriteBatch.begin must be called before end.")
        if (this.vIndex > 0) flush()
        drawing = false

        val gl = Gdx.gl
        gl.glDepthMask(true)
        if (blendingEnabled) gl.glDisable(GL20.GL_BLEND)

        shader.end()
    }

    inline fun <T>draw(blend: Blend = Blend.None, func:DrawBatch.()->T):T {
        begin(blend)
        val result = this.func()
        end()
        return result
    }

    fun canExpectWithoutFlush(vCalls: Int, iCalls: Int):Boolean {
        val indices = iCalls * 3
        return vIndex + vCalls <= this.vertices.size && iIndex + indices <= this.indices.size
    }

    /** @return true if flushed */
    fun expect(vCalls:Int, iCalls:Int):Boolean {
        if (!canExpectWithoutFlush(vCalls, iCalls)) {
            flush()
            assert(canExpectWithoutFlush(vCalls, iCalls))
                {"$vCalls and $iCalls will never fit"}
            return true
        }
        return false
    }

    fun vertex(x:Float, y:Float, color:Float = White, u:Float = 0.5f, v:Float = 0.5f):Short {
        assert(drawing) {"Not drawing"}

        val vertices = vertices

        val idx = this.vIndex
        val vertexIndex = (idx / 5).toShort()

        vertices[idx] = x
        vertices[idx + 1] = y
        vertices[idx + 2] = color
        vertices[idx + 3] = u
        vertices[idx + 4] = v
        this.vIndex = idx + 5

        return vertexIndex
    }

    fun indices(i1:Short, i2:Short, i3:Short) {
        val indices = indices

        val idx = this.iIndex
        indices[idx] = i1
        indices[idx+1] = i2
        indices[idx+2] = i3
        this.iIndex = idx + 3
    }

    fun indicesRect(bl:Short, tl:Short, tr:Short, br:Short) {
        val indices = indices

        val idx = this.iIndex
        indices[idx] = bl
        indices[idx+1] = tl
        indices[idx+2] = tr

        indices[idx+3] = tr
        indices[idx+4] = br
        indices[idx+5] = bl
        this.iIndex = idx + 6
    }

    internal fun drawFramebuffer(frameBuffer: FrameBuffer, x:Float, y:Float, w:Float, h:Float) {
        assert(!drawing)
        val gl = Gdx.gl
        val shader = FramebufferShader

        gl.glDepthMask(false)
        shader.begin()
        frameBuffer.colorBufferTexture.bind(0)
        shader.setUniformi("u_texture", 0)

        val glX = x * 2f - 1f
        val glY = y * 2f - 1f
        val glX2 = glX + w * 2f
        val glY2 = glY + h * 2f

        vertices[0] = glX
        vertices[1] = glY
        vertices[2] = 0f
        vertices[3] = 0f
        vertices[4] = 0f

        vertices[5+0] = glX
        vertices[5+1] = glY2
        vertices[5+2] = 0f
        vertices[5+3] = 0f
        vertices[5+4] = 1f

        vertices[10+0] = glX2
        vertices[10+1] = glY2
        vertices[10+2] = 0f
        vertices[10+3] = 1f
        vertices[10+4] = 1f

        vertices[15+0] = glX2
        vertices[15+1] = glY
        vertices[15+2] = 0f
        vertices[15+3] = 1f
        vertices[15+4] = 0f

        indices[0] = 0
        indices[1] = 1
        indices[2] = 2
        indices[3] = 2
        indices[4] = 3
        indices[5] = 0

        val mesh = this.mesh
        mesh.setVertices(vertices, 0, 20)
        mesh.setIndices(indices, 0, 6)
        mesh.render(shader, GL20.GL_TRIANGLES, 0, 6)

        shader.end()
        gl.glDepthMask(true)
    }

    internal fun drawFramebuffer(frameBuffer: FrameBuffer) {
        assert(!drawing)
        val gl = Gdx.gl
        val shader = FramebufferShader

        gl.glDepthMask(false)
        shader.begin()
        frameBuffer.colorBufferTexture.bind(0)
        shader.setUniformi("u_texture", 0)

        vertices[0] = -1f
        vertices[1] = -1f
        vertices[2] = 0f
        vertices[3] = 0f
        vertices[4] = 0f

        vertices[5+0] = -1f
        vertices[5+1] = 1f
        vertices[5+2] = 0f
        vertices[5+3] = 0f
        vertices[5+4] = 1f

        vertices[10+0] = 1f
        vertices[10+1] = 1f
        vertices[10+2] = 0f
        vertices[10+3] = 1f
        vertices[10+4] = 1f

        vertices[15+0] = 1f
        vertices[15+1] = -1f
        vertices[15+2] = 0f
        vertices[15+3] = 1f
        vertices[15+4] = 0f

        indices[0] = 0
        indices[1] = 1
        indices[2] = 2
        indices[3] = 2
        indices[4] = 3
        indices[5] = 0

        val mesh = this.mesh
        mesh.setVertices(vertices, 0, 20)
        mesh.setIndices(indices, 0, 6)
        mesh.render(shader, GL20.GL_TRIANGLES, 0, 6)

        shader.end()
        gl.glDepthMask(true)
    }

    fun flush() {
        if (this.vIndex == 0) return

        renderCalls++
        totalRenderCalls++

        texture.bind()
        val mesh = this.mesh
        mesh.setVertices(vertices, 0, this.vIndex)
        mesh.setIndices(indices, 0, this.iIndex)

        if (!blendingEnabled) {
            Gdx.gl.glDisable(GL20.GL_BLEND)
        } else {
            Gdx.gl.glEnable(GL20.GL_BLEND)
            if (blendSrcFunc != -1) Gdx.gl.glBlendFunc(blendSrcFunc, blendDstFunc)
        }

        mesh.render(shader, GL20.GL_TRIANGLES, 0, iIndex)

        this.vIndex = 0
        this.iIndex = 0
    }

    private fun setupMatrices() {
        shader.setUniformMatrix("u_projTrans", viewport.camera.combined)
        shader.setUniformi("u_texture", 0)
    }

    fun dispose() {
        mesh.dispose()
        if (ownsDefaultTexture) defaultTexture.dispose()
    }

    companion object {

        val White = Color.WHITE.toFloatBits()
        val Black = Color.BLACK.toFloatBits()

        private fun createDefaultTexture(): Texture {
            val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
            pixmap.setColor(Color.WHITE)
            pixmap.drawPixel(0, 0)
            return Texture(PixmapTextureData(pixmap, null, false, true))
        }

        /** Lazy instance of the default shader used by SpriteBatch for GL2 when no shader is specified.  */
        private val Batch2dShader: ShaderProgram by lazy { shader("batch2d") }

        private val FramebufferShader:ShaderProgram by lazy { shader("framebuffer") }
    }

    enum class Blend {
        None {
            override fun setup() {
                Gdx.gl.glDisable(GL20.GL_BLEND)
            }
        },
        Blend {
            override fun setup() {
                Gdx.gl.glEnable(GL20.GL_BLEND)
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
            }
        },
        Additive {
            override fun setup() {
                Gdx.gl.glEnable(GL20.GL_BLEND)
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
            }
        },
        ;

        abstract fun setup()
    }
}