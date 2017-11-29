package com.darkyen.processingdx

/**
 * Describes a line stroke.
 *
 * @param miterLimit what is the max dot product <-1; 1> that still has miter join (values under this fall back to bevel)
 *                      -1 = no limit (but glitches), 1 = always fallback
 */
data class Stroke(val width:Float, val fill:Fill, val cap: Cap = Cap.Butt, val join: Join = Join.Bevel, val miterLimit:Float = -0.8f) {

    enum class Cap {
        /** Circular cap */
        Round,
        /** Rectangular cap, extending by stroke width. */
        Square,
        /** Like [Square], but does not extend. */
        Butt
    }

    enum class Join {
        /** Edges are extended to meet. Creates sharp edges, falls back to [Bevel] if the extension would be over [miterLimit]. */
        Miter,
        /** Looks as if circle was placed on the join center. */
        Round,
        /** Gap that appears is filled with a single triangle. */
        Bevel
    }
}

val WhiteStroke = Stroke(5f, WhiteFill)
val BlackStroke = Stroke(5f, BlackFill)