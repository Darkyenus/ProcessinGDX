package com.darkyen.processingdx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import org.lwjgl.opengl.GL11

/**
 * Load and compile shader program.
 * Returned shader is guaranteed to be compiled.
 *
 * @param vertex name of vertex shader file, ".vert" extension will be added
 * @param fragment name of fragment shader file, ".frag" extension will be added
 */
fun shader(vertex:String, fragment:String = vertex):ShaderProgram {
    val vertexShader = Gdx.files.internal("shaders/$vertex.vert").readString()
    val fragmentShader = Gdx.files.internal("shaders/$fragment.frag").readString()

    val shader = ShaderProgram(vertexShader, fragmentShader)
    if (!shader.isCompiled) throw IllegalArgumentException("Error compiling shader ($vertex, $fragment): " + shader.log)
    return shader
}

fun texture(name:String,
            minFilter:Texture.TextureFilter = Texture.TextureFilter.MipMapLinearLinear,
            magFilter:Texture.TextureFilter = Texture.TextureFilter.Linear,
            uWrap:Texture.TextureWrap = Texture.TextureWrap.Repeat,
            vWrap:Texture.TextureWrap = uWrap):Texture {
    val texture = Texture(Gdx.files.internal("textures/$name.png"), true)
    texture.setFilter(minFilter, magFilter)
    texture.setWrap(uWrap, vWrap)

    return texture
}

fun <T>DrawBatch.wireframe(func:DrawBatch.()->T):T {
    GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE)
    val result = func()
    flush()
    GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL)
    return result
}