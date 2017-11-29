package com.darkyen.processingdx

import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.FloatArray as Floats
import com.badlogic.gdx.utils.IntArray as Ints
import com.darkyen.*

class LineDrawer : LineDrawing {

    private val MAX_JOIN_VERTICES = 305
    private val MAX_JOIN_INDICES = 301
    private val MAX_CAP_VERTICES = 303
    private val MAX_CAP_INDICES = 301

    private var stroke: Stroke = WhiteStroke

    /** X,Y,W,C */
    private val data = Floats(true, 32 * 4)
    /** Since color can sometimes be calculated only after length is known, this contains indices into [data] at which
     * incomplete vertices are. */
    private val missingColorIndices = Ints(true, 32)

    fun init(stroke: Stroke) {
        this.stroke = stroke
        this.data.clear()
        this.missingColorIndices.clear()
    }

    override fun vertex(x:Float, y:Float) {
        val data = data.ensureCapacity(4)
        val i = this.data.size
        data[i] = x
        data[i+1] = y
        data[i+2] = stroke.width
        //data[i+3] = missing color
        this.data.size = i + 4
        missingColorIndices.add(i)
    }

    override fun vertexW(x:Float, y:Float, width: Float) {
        val data = data.ensureCapacity(4)
        val i = this.data.size
        data[i] = x
        data[i+1] = y
        data[i+2] = width
        //data[i+3] = missing color
        this.data.size = i + 4
        missingColorIndices.add(i)
    }

    override fun vertexC(x:Float, y:Float, color:Float) {
        val data = data.ensureCapacity(4)
        val i = this.data.size
        data[i] = x
        data[i+1] = y
        data[i+2] = stroke.width
        data[i+3] = color
        this.data.size = i + 4
    }

    override fun vertexWC(x:Float, y:Float, width: Float, color:Float) {
        val data = data.ensureCapacity(4)
        val i = this.data.size
        data[i] = x
        data[i+1] = y
        data[i+2] = width
        data[i+3] = color
        this.data.size = i + 4
    }

    private fun segmentBegin(segment:Int):Int {
        var b = segment * 4
        val size = data.size
        if (b >= size) b -= size
        return b
    }

    private fun segmentDirection(segment:Int, v: Vector2 = Vector2()): Vector2 {
        val data = this.data.items

        val sIdx = segmentBegin(segment)
        val sX = data[sIdx]
        val sY = data[sIdx+1]

        val eIdx = segmentBegin(segment + 1)
        val eX = data[eIdx]
        val eY = data[eIdx+1]

        return v.set(eX, eY).sub(sX, sY).nor()
    }

    private fun DrawBatch.prepareToDraw():Int {
        val count = this@LineDrawer.data.size / 4
        // Obtain missing colors
        missingColorIndices.forEach { i ->
            val x = data[i]
            val y = data[i+1]
            data[i+3] = stroke.fill.color(x, y, i/4, count)
        }

        val tex = stroke.fill.texture()
        if (tex != null) {
            this.texture = tex
        }

        return count
    }

    /**
     * Create join at the end of this [segment] with the next segment.
     *
     * @returns vertex number of (-> segment.end.left, segment.end.right, nSegment.start.left, nSegment.start.right)
     */
    private fun createJoinBevel(batch: DrawBatch, fill: Fill, segment: Int, segmentDirection: Vector2, nextSegmentDirection: Vector2):Short {
        val data = this.data.items
        val bIdx = segmentBegin(segment + 1)
        val x = data[bIdx]
        val y = data[bIdx+1]
        val w = data[bIdx+2]
        val c = data[bIdx+3]

        // Previous end
        val lx = x - segmentDirection.y * w
        val ly = y + segmentDirection.x * w
        val rx = x + segmentDirection.y * w
        val ry = y - segmentDirection.x * w

        // Next start
        val nLx = x - nextSegmentDirection.y * w
        val nLy = y + nextSegmentDirection.x * w
        val nRx = x + nextSegmentDirection.y * w
        val nRy = y - nextSegmentDirection.x * w

        val pL = batch.vertex(lx, ly, c, fill)
        val pR = batch.vertex(rx, ry, c, fill)
        val nL = batch.vertex(nLx, nLy, c, fill)
        val nR = batch.vertex(nRx, nRy, c, fill)

        val turnsRight = Vector2.dot(segmentDirection.y, -segmentDirection.x, nextSegmentDirection.x, nextSegmentDirection.y) >= 0
        val center = batch.vertex(x, y, c, fill)
        if (turnsRight) {
            batch.indices(pL, nL, center)
        } else {
            batch.indices(pR, nR, center)
        }

        return pL
    }

    private val createJoinMiter_TMP = Vector2()
    private fun createJoinMiter(batch: DrawBatch, fill: Fill, segment: Int, segmentDirection: Vector2, nextSegmentDirection: Vector2):Short {

        val data = this.data.items
        val bIdx = segmentBegin(segment + 1)
        val x = data[bIdx]
        val y = data[bIdx+1]
        val w = data[bIdx+2]
        val c = data[bIdx+3]


        // Previous end
        var lx = x - segmentDirection.y * w
        var ly = y + segmentDirection.x * w
        var rx = x + segmentDirection.y * w
        var ry = y - segmentDirection.x * w

        // Next start
        var nLx = x - nextSegmentDirection.y * w
        var nLy = y + nextSegmentDirection.x * w
        var nRx = x + nextSegmentDirection.y * w
        var nRy = y - nextSegmentDirection.x * w

        val turnsRight = Vector2.dot(segmentDirection.y, -segmentDirection.x, nextSegmentDirection.x, nextSegmentDirection.y) >= 0
        if (turnsRight) {
            // Extend and intersect left
            val intersection = createJoinMiter_TMP
            if (!Intersector.intersectLines(lx, ly, lx + segmentDirection.x, ly + segmentDirection.y,
                    nLx, nLy, nLx + nextSegmentDirection.x, nLy + nextSegmentDirection.y, intersection)) {
                return createJoinBevel(batch, fill, segment, segmentDirection, nextSegmentDirection)
            }

            lx = intersection.x
            ly = intersection.y
            nLx = intersection.x
            nLy = intersection.y
        } else {
            // Extend and intersect right
            val intersection = createJoinMiter_TMP
            if (!Intersector.intersectLines(rx, ry, rx + segmentDirection.x, ry + segmentDirection.y,
                    nRx, nRy, nRx + nextSegmentDirection.x, nRy + nextSegmentDirection.y, intersection)) {
                return createJoinBevel(batch, fill, segment, segmentDirection, nextSegmentDirection)
            }

            rx = intersection.x
            ry = intersection.y
            nRx = intersection.x
            nRy = intersection.y
        }


        val pL = batch.vertex(lx, ly, c, fill)
        batch.vertex(rx, ry, c, fill)
        batch.vertex(nLx, nLy, c, fill)
        batch.vertex(nRx, nRy, c, fill)

        /*val baseSegmentAngle = segmentDirection.angle()
        val nextBaseSegmentAngle = nextSegmentDirection.angle()

        var segmentAngle = normalizeAngleDeg(baseSegmentAngle + 180)
        val nextSegmentAngle = normalizeAngleDeg(nextBaseSegmentAngle)
        if (segmentAngle < nextSegmentAngle) {
            segmentAngle += 360
        }
        val angleDistance = segmentAngle - nextSegmentAngle

        val anglePath = normalizeAngleOffsetDeg(baseSegmentAngle - nextBaseSegmentAngle) / 2f
        val adjustedW = (1f / Math.abs(MathUtils.cosDeg(anglePath))) * w

        val halfAngle = nextSegmentAngle + angleDistance/2f
        val miterDirectionX = MathUtils.cosDeg(halfAngle)
        val miterDirectionY = MathUtils.sinDeg(halfAngle)

        // Previous end
        val lx: Float = x + miterDirectionX * adjustedW
        val ly: Float = y + miterDirectionY * adjustedW
        val rx: Float = x - miterDirectionX * adjustedW
        val ry: Float = y - miterDirectionY * adjustedW

        val pL = batch.vertex(lx, ly, c, fill)
        batch.vertex(rx, ry, c, fill)
        batch.vertex(lx, ly, c, fill)
        batch.vertex(rx, ry, c, fill)*/

        return pL
    }

    private val createJoinRound_TMP = Vector2()
    private val createJoinRound_TMP2 = Vector2()
    private fun createJoinRound(batch: DrawBatch, fill: Fill, segment: Int, segmentDirection: Vector2, nextSegmentDirection: Vector2):Short {
        val data = this.data.items
        val bIdx = segmentBegin(segment + 1)
        val x = data[bIdx]
        val y = data[bIdx+1]
        val w = data[bIdx+2]
        val c = data[bIdx+3]

        // Previous end
        val lx = x - segmentDirection.y * w
        val ly = y + segmentDirection.x * w
        val rx = x + segmentDirection.y * w
        val ry = y - segmentDirection.x * w

        // Next start
        val nLx = x - nextSegmentDirection.y * w
        val nLy = y + nextSegmentDirection.x * w
        val nRx = x + nextSegmentDirection.y * w
        val nRy = y - nextSegmentDirection.x * w

        val pL = batch.vertex(lx, ly, c, fill)
        val pR = batch.vertex(rx, ry, c, fill)
        val nL = batch.vertex(nLx, nLy, c, fill)
        val nR = batch.vertex(nRx, nRy, c, fill)

        val turnsRight = Vector2.dot(segmentDirection.y, -segmentDirection.x, nextSegmentDirection.x, nextSegmentDirection.y) > 0
        val center = batch.vertex(x, y, c, fill)

        val fromX:Float
        val fromY:Float
        val toX:Float
        val toY:Float
        var lastVx:Short
        val finalVx:Short
        if (turnsRight) {
            fromX = lx - x
            fromY = ly - y
            toX = nLx - x
            toY = nLy - y
            lastVx = pL
            finalVx = nL
        } else {
            fromX = rx - x
            fromY = ry - y
            toX = nRx - x
            toY = nRy - y
            lastVx = pR
            finalVx = nR
        }

        val pos = createJoinRound_TMP.set(fromX, fromY)
        val to = createJoinRound_TMP2.set(toX, toY)
        var angle = normalizeAngleDeg(pos.angle(to))
        if (turnsRight) {
            angle -= 360
        }
        val steps = Math.min(300, (w * Math.abs(angle) / 360f).toInt() + 2) // MIN needed to honor MAX_JOIN_VERTICES/INDICES
        val stepRad = (angle/steps) * MathUtils.degreesToRadians
        pos.nor().scl(w)

        val stepCos = Math.cos(stepRad.toDouble()).toFloat()
        val stepSin = Math.sin(stepRad.toDouble()).toFloat()

        for (s in 1..steps) {
            val newX = pos.x * stepCos - pos.y * stepSin
            val newY = pos.x * stepSin + pos.y * stepCos
            pos.x = newX
            pos.y = newY

            val vx = batch.vertex(x + newX, y + newY, c, fill)
            batch.indices(center, lastVx, vx)
            lastVx = vx
        }

        batch.indices(center, lastVx, finalVx)

        return pL
    }

    private fun createJoin(batch: DrawBatch, segment: Int, segmentDirection: Vector2, nextSegmentDirection: Vector2):Short {
        return when (stroke.join) {
            Stroke.Join.Miter -> {
                if (Vector2.dot(segmentDirection.x, segmentDirection.y, nextSegmentDirection.x, nextSegmentDirection.y) < stroke.miterLimit) {
                    createJoinBevel(batch, stroke.fill, segment, segmentDirection, nextSegmentDirection)
                } else {
                    createJoinMiter(batch, stroke.fill, segment, segmentDirection, nextSegmentDirection)
                }
            }
            Stroke.Join.Round -> createJoinRound(batch, stroke.fill, segment, segmentDirection, nextSegmentDirection)
            Stroke.Join.Bevel -> createJoinBevel(batch, stroke.fill, segment, segmentDirection, nextSegmentDirection)
        }
    }

    private fun createCapSquare(batch: DrawBatch, fill: Fill, segment:Int, segmentDirection: Vector2, begin:Boolean, size: Float):Short {
        val data = this.data.items
        val bIdx = segmentBegin(if (begin) segment else segment + 1)
        val x = data[bIdx] + (if (begin) -segmentDirection.x else segmentDirection.x) * size
        val y = data[bIdx+1] + (if (begin) -segmentDirection.y else segmentDirection.y) * size
        val w = data[bIdx+2]
        val c = data[bIdx+3]

        val lx = x - segmentDirection.y * w
        val ly = y + segmentDirection.x * w
        val rx = x + segmentDirection.y * w
        val ry = y - segmentDirection.x * w

        val pL = batch.vertex(lx, ly, c, fill)
        batch.vertex(rx, ry, c, fill)

        return pL
    }

    private fun createCapRound(batch: DrawBatch, fill: Fill, segment:Int, segmentDirection: Vector2, begin:Boolean):Short {
        val data = this.data.items
        val bIdx = segmentBegin(if (begin) segment else segment + 1)
        val x = data[bIdx]
        val y = data[bIdx+1]
        val w = data[bIdx+2]
        val c = data[bIdx+3]

        val lx = x - segmentDirection.y * w
        val ly = y + segmentDirection.x * w
        val rx = x + segmentDirection.y * w
        val ry = y - segmentDirection.x * w

        val vLeft = batch.vertex(lx, ly, c, fill)
        val vRight = batch.vertex(rx, ry, c, fill)
        val vCenter = batch.vertex(x, y, c, fill)

        val steps = Math.min(300, (w * 0.5f).toInt() + 2) //Same formula like used for round join, even if it does not look like it
        val stepRad = MathUtils.PI / steps

        var rotX = if (begin) lx - x else rx - x
        var rotY = if (begin) ly - y else ry - y

        val stepCos = Math.cos(stepRad.toDouble()).toFloat()
        val stepSin = Math.sin(stepRad.toDouble()).toFloat()

        var lastVx = if (begin) vLeft else vRight

        for (s in 1..steps) {
            val newX = rotX * stepCos - rotY * stepSin
            val newY = rotX * stepSin + rotY * stepCos
            rotX = newX
            rotY = newY

            val vx = batch.vertex(x + rotX, y + rotY, c, fill)
            batch.indices(vCenter, lastVx, vx)
            lastVx = vx
        }

        batch.indices(vCenter, lastVx, if (begin) vRight else vLeft)

        return vLeft
    }

    private fun createCap(batch: DrawBatch, segment:Int, segmentDirection: Vector2, begin:Boolean):Short {
        return when(stroke.cap) {
            Stroke.Cap.Round -> createCapRound(batch, stroke.fill, segment, segmentDirection, begin)
            Stroke.Cap.Square -> {
                val width = this.data.items[segmentBegin(if (begin) segment else segment + 1) + 2]
                createCapSquare(batch, stroke.fill, segment, segmentDirection, begin, width)
            }
            Stroke.Cap.Butt -> createCapSquare(batch, stroke.fill, segment, segmentDirection, begin, 0f)
        }
    }

    private fun DrawBatch.expectAndTranslate(vCalls:Int, iCalls:Int, index:Short, amount:Int):Short {
        if (expect(vCalls, iCalls)) {
            // Did flush, save the data!
            val savedDataLength = amount * vertexFloatFields
            System.arraycopy(vertices, index * vertexFloatFields, vertices, 0, savedDataLength)
            vIndex = savedDataLength
            return 0
        }
        return index
    }

    /** complete non-looped */
    fun doneLine(batch: DrawBatch) {
        val count = batch.prepareToDraw()
        if (count <= 1) return

        val thisDirection = segmentDirection(0)
        batch.expect(MAX_CAP_VERTICES, MAX_CAP_INDICES)
        var thisBeginLeft:Short = createCap(batch, 0, thisDirection, true)
        val nextDirection = Vector2()

        for (i in 0..(count-3)) {
            // Create join in the end of segment that starts at i and complete the segment
            segmentDirection(i+1, nextDirection)
            thisBeginLeft = batch.expectAndTranslate(MAX_JOIN_VERTICES, MAX_CAP_INDICES + 2, thisBeginLeft, 2)
            val thisEndLeft:Short = createJoin(batch, i, thisDirection, nextDirection)

            batch.indicesRect(thisBeginLeft, thisEndLeft, (thisEndLeft + 1).toShort(), (thisBeginLeft + 1).toShort())

            thisBeginLeft = (thisEndLeft + 2).toShort()
            thisDirection.set(nextDirection)
        }

        // Now create last segment
        thisBeginLeft = batch.expectAndTranslate(MAX_JOIN_VERTICES, MAX_CAP_INDICES + 2, thisBeginLeft, 2)
        val endCap = createCap(batch, count-2, thisDirection, false)
        batch.indicesRect(thisBeginLeft, endCap, (endCap + 1).toShort(), (thisBeginLeft + 1).toShort())
    }

    /** complete non-looped */
    fun doneLoop(batch: DrawBatch) {
        val count = batch.prepareToDraw()
        if (count <= 1) return

        val thisDirection = segmentDirection(count - 1)
        val nextDirection = segmentDirection(0)
        /** = total last end left */
        batch.expect(MAX_JOIN_VERTICES, MAX_JOIN_INDICES)
        var loopJoin = createJoin(batch, count-1, thisDirection, nextDirection)
        var loopJoinData:FloatArray? = null

        var thisBeginLeft:Short = (loopJoin + 2).toShort()
        thisDirection.set(nextDirection)

        for (i in 0..(count-2)) {
            // Create join in the end of segment that starts at i and complete the segment
            segmentDirection(i+1, nextDirection)

            if (batch.expect(MAX_JOIN_VERTICES, MAX_CAP_INDICES + 2 + 1 /* prepare for final segment for simplicity */)) {
                // Did flush, save the data!
                val savedDataLength = 2 * batch.vertexFloatFields
                if (loopJoinData == null) {
                    loopJoinData = FloatArray(savedDataLength)
                    System.arraycopy(batch.vertices, loopJoin * batch.vertexFloatFields, loopJoinData, 0, savedDataLength)
                }


                System.arraycopy(batch.vertices, thisBeginLeft * batch.vertexFloatFields, batch.vertices, batch.vIndex, savedDataLength)
                batch.vIndex += savedDataLength
                thisBeginLeft = 0

                System.arraycopy(loopJoinData, 0, batch.vertices, batch.vIndex, savedDataLength)
                batch.vIndex += savedDataLength
                loopJoin = 2
            }

            val thisEndLeft:Short = createJoin(batch, i, thisDirection, nextDirection)

            batch.indicesRect(thisBeginLeft, thisEndLeft, (thisEndLeft + 1).toShort(), (thisBeginLeft + 1).toShort())

            thisBeginLeft = (thisEndLeft + 2).toShort()
            thisDirection.set(nextDirection)
        }

        // Now create last segment
        batch.indicesRect(thisBeginLeft, loopJoin, (loopJoin + 1).toShort(), (thisBeginLeft + 1).toShort())
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun DrawBatch.vertex(x:Float, y:Float, color:Float, fill:Fill):Short {
    return vertex(x, y, color, fill.textureU(x, y), fill.textureV(x, y))
}

interface LineDrawing {
    fun vertex(x:Float, y:Float)

    fun vertexW(x:Float, y:Float, width: Float)

    fun vertexC(x:Float, y:Float, color:Float)

    fun vertexWC(x:Float, y:Float, width: Float, color:Float)
}