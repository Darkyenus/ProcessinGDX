package com.darkyen.processingdx.functions

/**
 *
 */
interface General {

    /**
     * Quits/stops/exits the program. Programs without a draw() function stop automatically after the last line has run, but programs with draw() run continuously until the program is manually stopped or exit() is run.
     *
     * Rather than terminating immediately, exit() will cause the sketch to exit after draw() has completed (or after setup() completes if called during the setup() function).
     */
    fun exit()

    /**
     * By default, Processing loops through draw() continuously, executing the code within it. However, the draw() loop may be stopped by calling noLoop(). In that case, the draw() loop can be resumed with loop().
     */
    fun loop()

    /**
     * Stops Processing from continuously executing the code within draw(). If loop() is called, the code in draw() begins to run continuously again. If using noLoop() in setup(), it should be the last line inside the block.

    When noLoop() is used, it's not possible to manipulate or access the screen inside event handling functions such as mousePressed() or keyPressed(). Instead, use those functions to call redraw() or loop(), which will run draw(), which can update the screen properly. This means that when noLoop() has been called, no drawing can happen, and functions like saveFrame() or loadPixels() may not be used.

    Note that if the sketch is resized, redraw() will be called to update the sketch, even after noLoop() has been specified. Otherwise, the sketch would enter an odd state until loop() was called.
     */
    fun noLoop()

    /**
     * Executes the code within draw() one time. This functions allows the program to update the display window only when necessary, for example when an event registered by mousePressed() or keyPressed() occurs.

    In structuring a program, it only makes sense to call redraw() within events such as mousePressed(). This is because redraw() does not run draw() immediately (it only sets a flag that indicates an update is needed).

    The redraw() function does not work properly when called inside draw(). To enable/disable animations, use loop() and noLoop().
     */
    fun redraw()

}