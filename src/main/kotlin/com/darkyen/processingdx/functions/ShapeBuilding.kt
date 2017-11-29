package com.darkyen.processingdx.functions

/**
 *
 */
interface ShapeBuilding {

    /** */
    fun arc()
    /** */
    fun ellipse()
    /** */
    fun line()
    /** */
    fun point()
    /** */
    fun quad()
    /** */
    fun rect()
    /** */
    fun triangle()

    /** */
    fun bezier()
    /** */
    fun bezierDetail()
    /** */
    fun bezierPoint()
    /** */
    fun bezierTangent()
    /** */
    fun curve()
    /** */
    fun curveDetail()
    /** */
    fun curvePoint()
    /** */
    fun curveTangent()
    /** */
    fun curveTightness()

    /** */
    fun ellipseMode()
    /** */
    fun rectMode()
    /** */
    fun strokeCap()
    /** */
    fun strokeJoin()
    /** */
    fun strokeWeight()

    /** */
    fun beginContour()
    /** */
    fun beginShape()
    /** */
    fun bezierVertex()
    /** */
    fun curveVertex()
    /** */
    fun endContour()
    /** */
    fun endShape()
    /** */
    fun quadraticVertex()
    /** */
    fun vertex()

    /** */
    fun shape()
    /** */
    fun shapeMode()
}