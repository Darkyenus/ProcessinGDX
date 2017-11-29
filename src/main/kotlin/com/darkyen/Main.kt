package com.darkyen

import com.darkyen.processingdx.Applet

/**
 *
 */
fun main(args: Array<String>) {
    val appletClass = Class.forName("com.darkyen.pv259.${args[0]}").getField("INSTANCE").get(null) as Applet
    appletClass.start()
}