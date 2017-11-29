package com.darkyen.processingdx.functions

/**
 *
 */
interface Settings {

    fun size(width:Int, height:Int)

    fun canvasSize(width:Int, height:Int)

    fun sizeMode(allowHiDpi: Boolean)

    fun smooth(level:Int)

    fun fullScreen()

    fun clampMouse()
}