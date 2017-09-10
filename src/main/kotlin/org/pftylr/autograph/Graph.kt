/*
 * MIT License
 *
 * Copyright (c) 2017 ptaylor
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package org.pftylr.autograph

import org.pftylr.autograph.Sampler
import org.pftylr.autograph.History
import org.pftylr.autograph.ResizableCanvas

import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.Group
import javafx.scene.text.TextAlignment
import javafx.scene.text.Font
import javafx.scene.control.Button
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import javafx.geometry.VPos
import javafx.scene.canvas.GraphicsContext 
import javafx.concurrent.Task
import javafx.application.Platform
import kotlin.concurrent.thread

class Graph(val group: Group, val dataSource: InputStreamDataSource, var width: Double, var height: Double, val size: Int) : Sampler() {

    var count =  0
    var history : History
    var names = listOf<String>()
    var numValues = -1

    var minValue: Double = Double.MAX_VALUE
    var maxValue: Double = Double.MIN_VALUE
    var graphMaxValue: Double = Double.MIN_VALUE
    var graphMinValue: Double = Double.MAX_VALUE

    val canvas: Canvas
    val gc: GraphicsContext 


    // https://docs.oracle.com/javase/8/javafx/api/javafx/scene/paint/Color.html
    val LINE_COLOURS = listOf(
    		       Color.ORANGE, 
		       Color.BLUE, 
                       Color.PINK, 
                       Color.GREEN,
                       Color.RED, 
                       Color.BROWN,
    		       Color.CYAN, 
                       Color.GRAY, 
                       Color.DARKRED,
		       Color.VIOLET, 
                       Color.GOLD, 
		       Color.IVORY
    )
    val BORDER_TOP = 40.0
    val BORDER_BOTTOM = 40.0
    val BORDER_LEFT = 80.0
    val BORDER_RIGHT = 140.0
    
    val BORDER_COLOUR = Color.CHARTREUSE
    val BORDER_WIDTH = 0.5

    val LINE_WIDTH = 1.0
    val TEXT_WIDTH = 1.0
    val TEXT_COLOUR = BORDER_COLOUR
    val XAXIS_FONT = Font.font("Monospaced", 12.0)
    val YAXIS_FONT = Font.font("Monospaced", 8.0)
    val LEGEND_FONT = Font.font("Monospaced", 12.0)

    val LEGEND_LINE_WIDTH = 2.0
    val LEGEND_TEXT_WIDTH = 1.0
    val LEGEND_LINE_LENGTH = 10.0
    val LEGEND_BORDER_LEFT = 20.0
    val MIN_MAX_COLOUR = Color.GREEN
    val MIN_MAX_WIDTH = 0.5
    val MIN_MAX_DASH_SIZE = 5.0
    val TICK_LENGTH = 4.0

    init {
        history = History(size + 1)

        canvas = ResizableCanvas(width, height)
        gc = canvas.getGraphicsContext2D()

	group.getChildren().add(canvas)

	resize(width, height)
    }

    fun resize(width: Double, height: Double) {
       //println("GRAPH RESIZE ${width} ${height}")
       //canvas.resize(width, height)
       canvas.setWidth(width)
       canvas.setHeight(height)

       calculateSizes()
       Platform.runLater {
           draw()
       }
    }

    fun run() {
    	
	val sampler = this
	thread(name = "sampler") {
	   dataSource.process(sampler)
	   println("*** PROCESS THREAD FINISHED")
	}
    }

    override fun newNames(strs: List<String>) {
        checkSize(strs.size)
	names = strs
    }

    override fun newValues(nums: List<Double>) {

        checkSize(nums.size)

	calculateMinMax(nums)
	calculateSizes()
        history.put(count++, nums)
	if (count > 1) {
  	    Platform.runLater {
	        draw()
            }
	}
    }

    private fun checkSize(s: Int) {
        if (numValues < 0) {
            numValues = s
        }

        if (s != numValues) {
	    fatal("number of data values (${s}) is not as expected (${numValues})")
	}
	
	if (s > LINE_COLOURS.size) {
	    fatal("too many data values (${s}) for number of available colours (${LINE_COLOURS.size})")
	}

    }

    private fun fatal(s: String) {
        System.err.println("ERROR: ${s}")
	Platform.exit()
    }


    private fun calculateMinMax(nums: List<Double>) {
        nums.forEach { n -> 
	   if (n < minValue) {
	       minValue = n
	   }
	   if (n > maxValue) {
	      maxValue = n
	   }
        }

	val diff = Math.abs(maxValue - minValue) * 0.10
	// TODO: make thibs more intelligent
	graphMaxValue = maxValue + diff
	graphMinValue = minValue - diff

	//println("MIN/MAX ${graphMinValue} ${minValue} ${maxValue} ${graphMaxValue}")
    }

    private fun draw() {
        clearGraph()
	drawBackground()
	drawGraph()
    }

    private fun drawGraph() {

        gc.setLineWidth(LINE_WIDTH);

	var x = history.size - 1
    	var previous : List<Double>? = null
	for (nums in history.values) {
	    if (previous != null) {
	        for (j in 0 .. nums.size - 1) {
	      	    val color = LINE_COLOURS[j]
                    gc.setStroke(color);

	            val v2 = previous[j]
		    val v1 = nums[j]
		    //println("x ${x}, v1 ${v1}, ${v2} = (${scaley(v1)}, ${scaley(v2)})")
		    plot(scalex(x.toDouble()), scaley(v1), scalex((x + 1).toDouble()), scaley(v2))
	        } 
	    }

            x = x - 1;
	    previous = nums
        }
      
    }

    private fun drawBackground() {

    	drawBorders()
	drawXAxis()
	drawYAxis()
	drawMinMaxLines()	
	drawLegend()

    }

    private fun drawBorders() {
        gc.setStroke(BORDER_COLOUR)
	gc.setLineWidth(BORDER_WIDTH)

	line(BORDER_LEFT, BORDER_TOP, width - BORDER_RIGHT, BORDER_TOP)
	line(BORDER_LEFT, height - BORDER_BOTTOM, width - BORDER_RIGHT, height - BORDER_BOTTOM)
	line(BORDER_LEFT, BORDER_TOP, BORDER_LEFT, height - BORDER_BOTTOM)
	line(width - BORDER_RIGHT, BORDER_TOP, width - BORDER_RIGHT, height - BORDER_BOTTOM)
    }

    private fun drawYAxis() {

	// Verticle ticks (left side)
        gc.setTextBaseline(VPos.CENTER)
	gc.setTextAlign(TextAlignment.RIGHT)
	gc.setFont(YAXIS_FONT)
	val s : Double = (graphMaxValue - graphMinValue) / 10.0
	var i : Double = graphMinValue
	while (i <= graphMaxValue) {
            val y = scaley(i.toDouble())

	    gc.setLineWidth(BORDER_WIDTH);
            gc.setStroke(BORDER_COLOUR)
	    line(BORDER_LEFT, y, BORDER_LEFT - TICK_LENGTH, y)

	    gc.setLineWidth(TEXT_WIDTH);
	    gc.setStroke(TEXT_COLOUR)
	    text("%.2f".format(i), BORDER_LEFT - (TICK_LENGTH + 10), y)

	    i = i + s
	}

    }

    private fun drawXAxis() {

	for (i in 0 .. size step size / 10) {
	    val x = scalex(i.toDouble())

	    gc.setLineWidth(BORDER_WIDTH);
	    line(x, height - BORDER_BOTTOM, x, height - BORDER_BOTTOM + TICK_LENGTH)
        }

        gc.setTextBaseline(VPos.CENTER)
	gc.setTextAlign(TextAlignment.CENTER)
	gc.setFont(XAXIS_FONT)
        gc.setStroke(TEXT_COLOUR)
	text("", scalex(size / 2.0), height - BORDER_BOTTOM / 2)

    }

    private fun drawMinMaxLines() {
	gc.setStroke(MIN_MAX_COLOUR)
	gc.setLineWidth(MIN_MAX_WIDTH);
	dashedLine(BORDER_LEFT, scaley(minValue), width - BORDER_RIGHT, scaley(minValue), MIN_MAX_DASH_SIZE)
	dashedLine(BORDER_LEFT, scaley(maxValue), width - BORDER_RIGHT, scaley(maxValue), MIN_MAX_DASH_SIZE)
    }

    private fun drawLegend() {

        gc.setTextBaseline(VPos.CENTER)
	var y = scaley(maxValue)
	var i = 0
        for (name in names) {
 	    val colour = LINE_COLOURS[i++]
	    gc.setStroke(colour)
	    val x = width - BORDER_RIGHT + LEGEND_BORDER_LEFT

            gc.setLineWidth(LEGEND_LINE_WIDTH)
	    gc.setTextAlign(TextAlignment.LEFT)
	    gc.setFont(LEGEND_FONT)
	    line(x, y, x + LEGEND_LINE_LENGTH, y)

            gc.setLineWidth(LEGEND_TEXT_WIDTH)
	    gc.setFont(LEGEND_FONT)
	    gc.setStroke(TEXT_COLOUR)
	    text(name, x + LEGEND_LINE_LENGTH * 2, y)
	    y += 20
        }
    
    }

    private fun plot(x1: Double, v1: Double, x2: Double, v2: Double) {
        //println("Plot ${x1},${v1} ${x2},${v2}")
        line(x1 , v1 , x2, v2)
    }

    private fun dashedLine(x1: Double, y1: Double, x2: Double, y2: Double, dashSize: Double) {
    	var x = x1
	val max = x2 - dashSize * 2
	while (x <= max) {
            line(x, y1, x + dashSize, y2)
	    x += dashSize * 2.0
        }
    }

    private fun line(x1: Double, y1: Double, x2: Double, y2: Double) {
        //println("LINE (${x1}, $y1}) - (${x2}, ${y2})")
	gc.strokeLine(x1, y1, x2, y2)
    }

    private fun text(s: String, x: Double, y: Double) {
        gc.strokeText(s, x, y)
    }

    fun clearGraph() {
        gc.setFill(Color.WHITE)
	gc.clearRect(0.0, 0.0, canvas.getWidth(), canvas.getHeight())
    }
 


    fun calculateSizes() {
	width = canvas.width
	height = canvas.height
    }

    private fun scalex(v: Double) : Double {

        val cw = width - (BORDER_LEFT + BORDER_RIGHT)
	val x = cw - (v / size) * cw + BORDER_LEFT
	return x
    }

    private fun scaley(v: Double) : Double {

        val h = graphMaxValue - graphMinValue
    	val ch = height - (BORDER_TOP + BORDER_BOTTOM)
	val y = height - ((v - graphMinValue) / h) * ch - BORDER_BOTTOM

   	return y
    }

}