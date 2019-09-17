package com.akar.tinyrenderer

import ij.IJ
import ij.ImagePlus
import java.awt.Color
import kotlin.math.abs

fun main() {
    val image = IJ.createImage("result", "RGB", 1024, 1024, 1)

    image.processor.putPixel(1, 1, Color.red.rgb)

    line(0,0, 550, 135,image,  Color.RED.rgb)
    IJ.saveAs(image, "png", "result")
}


fun line(x0: Int, y0: Int, x1: Int, y1: Int, image: ImagePlus, color: Int) {
    var steep = false
    var _x0 = x0
    var _y0 = y0
    var _x1 = x1
    var _y1 = y1

    if (abs(x0 - x1) < abs(y0 - y1)) {
        _x0 = _y0.also { _y0 = _x0 }
        _x1 = _y1.also { _y1 = _x1 }
        steep = true
    }
    if (x0 > x1) {
        _x0 = _x1.also { _x1 = _x0 }

        _y0 = _y1.also { _y1 = _y0 }
    }
    val dx = x1 - x0
    val dy = y1 - y0
    val derror2 = abs(dy) * 2
    var error2 = 0
    var y = y0
    for (x in x0..x1) {
        if (steep) {
            image.processor[y, x] = color
        } else {
            image.processor[x, y] = color
        }
        error2 += derror2
        if (error2 > dx) {
            y += if (_y1 > _y0) 1 else -1
            error2 -= dx * 2
        }
    }
}
