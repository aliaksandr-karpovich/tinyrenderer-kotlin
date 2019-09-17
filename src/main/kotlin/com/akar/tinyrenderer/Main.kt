package com.akar.tinyrenderer

import ij.IJ
import ij.ImagePlus
import java.awt.Color
import kotlin.math.abs

fun main() {
    val image = IJ.createImage("result", "RGB", 1024, 1024, 1)

    val model = parseObj("/obj/african_head/african_head.obj")

    model.vertices.forEach {
        for (i in 0..2) {
            it[i] *= -500.0
            it[i] += 500.0
        }
    }

    model.triangles.forEach {
        for (i in 0..2) {
            image.line(model.vertices[it[i]][0].toInt(), model.vertices[it[i]][1].toInt(),
                    model.vertices[it[(i + 1) % 3]][0].toInt(), model.vertices[it[(i + 1) % 3]][1].toInt(),
                    Color.RED.rgb)
        }
    }


    image.line(0, 0, 550, 135, Color.RED.rgb)
    IJ.saveAs(image, "png", "result")
}


fun ImagePlus.line(x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
    var steep = false
    var _x0 = x0
    var _y0 = y0
    var _x1 = x1
    var _y1 = y1

    if (abs(_x0 - _x1) < abs(_y0 - _y1)) {
        _x0 = _y0.also { _y0 = _x0 }
        _x1 = _y1.also { _y1 = _x1 }
        steep = true
    }
    if (_x0 > _x1) {
        _x0 = _x1.also { _x1 = _x0 }
        _y0 = _y1.also { _y1 = _y0 }
    }
    val dx = _x1 - _x0
    val dy = _y1 - _y0
    val derror2 = abs(dy) * 2
    var error2 = 0
    var y = _y0
    for (x in _x0.._x1) {
        if (steep) {
            this.processor[y, x] = color
        } else {
            this.processor[x, y] = color
        }
        error2 += derror2
        if (error2 > dx) {
            y += if (_y1 > _y0) 1 else -1
            error2 -= dx * 2
        }
    }
}
