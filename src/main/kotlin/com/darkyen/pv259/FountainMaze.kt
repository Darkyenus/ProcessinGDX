package com.darkyen.pv259

import com.badlogic.gdx.math.MathUtils
import com.darkyen.Objects
import com.darkyen.processingdx.*
import com.darkyen.processingdx.DrawBatch.Companion.White
import com.darkyen.processingdx.functions.Settings
import com.darkyen.pv259.FountainMaze.Direction.*

/**
 *
 */
object FountainMaze : Applet() {

    override fun Settings.settings() {
        canvasSize(1024, 4800)
    }

    var drawAgain = true

    override fun keyTyped(key: Char) {
        if (key == ' ') {
            drawAgain = true
        }
    }

    /*
    HBlock:

    +-+-+-+-+-+  H Row
    | | | | | |  V Row
    +-+-+-+-+-+
    5 6 7 8 9 10 width
    | | | | | |
    +-+-+-+-+-+
     0 1 2 3 4  width-1

    Width = 6
    Height = 3
     */

    class Maze(val width: Int, val height: Int) {

        private val Clear:Byte = 0
        private val Set:Byte = 1
        private val Blocked:Byte = 2

        private val data = ByteArray((width-1) * height + width * (height - 1))
        private val hBlockStride = width + width - 1

        var lineCount = 0

        private fun index(x:Int, y:Int, direction: Direction):Int {
            val rightOffset = y * hBlockStride + x

            return when (direction) {
                UP -> if (y >= height - 1) {
                    -1
                } else {
                    rightOffset + width - 1
                }
                DOWN -> if (y <= 0) {
                    -1
                } else {
                    rightOffset - width
                }
                LEFT -> if (x <= 0) {
                    -1
                } else {
                    rightOffset - 1
                }
                RIGHT -> if (x >= width - 1) {
                    -1
                } else {
                    rightOffset
                }
            }
        }

        fun hasLine(x: Int, y: Int, direction: Direction):Boolean {
            val index = index(x, y, direction)
            return if (index == -1) {
                false
            } else {
                data[index] == Set
            }
        }

        fun noLineButPossibleAndClear(x: Int, y: Int, direction: Direction):Boolean {
            val index = index(x, y, direction)
            return if (index == -1) {
                false
            } else {
                data[index] == Clear && isUntouchedButViable(x + direction.dX, y + direction.dY)
            }
        }

        fun makeLineIfPossibleAndClear(x: Int, y: Int, direction: Direction):Boolean {
            val index = index(x, y, direction)
            return if (index == -1) {
                false
            } else {
                if (data[index] == Clear && isUntouchedButViable(x + direction.dX, y + direction.dY)) {
                    data[index] = Set
                    lineCount++
                    true
                } else false
            }
        }

        fun makeLine(x: Int, y: Int, direction: Direction):Boolean {
            val index = index(x, y, direction)
            return if (index == -1) {
                false
            } else {
                data[index] = Set
                lineCount++
                true
            }
        }

        fun blockLine(x: Int, y: Int, direction: Direction):Boolean {
            val index = index(x, y, direction)
            return if (index == -1) {
                false
            } else {
                data[index] = Blocked
                true
            }
        }

        fun isUntouchedButViable(x: Int, y: Int):Boolean {
            var viable = false
            for (d in Direction.VALUES) {
                val index = index(x, y, d)
                if (index == -1) {
                    continue
                } else if (data[index] == Set) {
                    return false
                } else {
                    viable = true
                }
            }
            return viable
        }

    }

    fun generateMaze(width: Int, height: Int):Maze {
        val maze = Maze(width, height)

        var pointX = MathUtils.random(0, width - 1)
        var pointY = MathUtils.random(0, 5)
        val directions = Objects<Direction>()

        // Block random segments
        val random = MathUtils.random
        val randomWeight = 5f
        for (y in 0 until height) {
            val blockProbability = Math.sqrt(Math.max(randomWeight / (y+randomWeight), randomWeight / (height-y-1 + randomWeight)).toDouble()).toFloat()

            // H Line
            for (x in 0 until (maze.width-1)) {
                if (random.nextFloat() < blockProbability) {
                    maze.blockLine(x, y, RIGHT)
                }
            }
            // V Line
            if (y == maze.height - 1) {
                continue
            }
            for (x in 0 until maze.width) {
                if (random.nextFloat() < blockProbability) {
                    maze.blockLine(x, y, UP)
                }
            }

        }

        master@while (true) {
            // Pick random possible direction to go
            for (d in Direction.randomDirections()) {
                if (maze.makeLineIfPossibleAndClear(pointX, pointY, d)) {
                    pointX += d.dX
                    pointY += d.dY
                    directions.add(d)
                    continue@master
                }
            }

            // No direction to go, backtrack
            while (directions.size > 0) {
                val backtracked = directions.pop()
                pointX -= backtracked.dX
                pointY -= backtracked.dY

                // Try viable points around here
                for (d in Direction.randomDirections()) {
                    if (maze.makeLineIfPossibleAndClear(pointX, pointY, d)) {
                        pointX += d.dX
                        pointY += d.dY
                        directions.add(d)
                        continue@master
                    }
                }
            }

            // Backtracked fully and nothing, done
            break@master
        }

        return maze
    }

    enum class Direction(val dX:Int, val dY:Int) {
        UP(0, 1),
        DOWN(0, -1),
        LEFT(-1, 0),
        RIGHT(1, 0);

        infix fun opposite(d:Direction) = this.dX == -d.dX && this.dY == -d.dY

        companion object {
            val VALUES = values()

            private val RANDOM_CACHE = values()

            fun randomDirections():Array<Direction> {
                val field = RANDOM_CACHE

                val random = MathUtils.random
                val size = field.size

                //Shuffle
                for (i in size downTo 2) {
                    val one = i - 1
                    val with = random.nextInt(i)

                    val tmp = field[one]
                    field[one] = field[with]
                    field[with] = tmp
                }

                return field
            }
        }
    }

    private val MazeStroke = Stroke(15f, solidColorFill(rgb(0f)), Stroke.Cap.Round, Stroke.Join.Round)

    val cell = 64

    override fun DrawBatch.draw(delta: Float) {
        if (!drawAgain) {
            return
        }
        drawAgain = false

        background(White)
        var maze = generateMaze(width / cell, height / cell)
        while (maze.lineCount < 50) {
            maze = generateMaze(width/cell, height/cell)
        }

        val xOffset = (width - (maze.width - 1) * cell) / 2f
        val yOffset = (height - (maze.height - 1) * cell) / 2f

        draw {
            for (y in 0 until maze.height) {
                // H Line
                for (x in 0 until (maze.width-1)) {
                    if (maze.hasLine(x, y, RIGHT)) {
                        line(MazeStroke) {
                            vertex(xOffset + x* cell, yOffset + y* cell)
                            vertex(xOffset + x* cell + cell, yOffset + y* cell)
                        }
                    }
                }
                // V Line
                if (y == maze.height - 1) {
                    continue
                }
                for (x in 0 until maze.width) {
                    if (maze.hasLine(x, y, UP)) {
                        line(MazeStroke) {
                            vertex(xOffset + x* cell, yOffset + y* cell)
                            vertex(xOffset + x* cell, yOffset + y* cell+ cell)
                        }
                    }
                }
            }
        }
    }
}