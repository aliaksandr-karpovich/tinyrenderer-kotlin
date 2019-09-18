package com.akar.tinyrenderer

import ij.IJ
import ij.ImagePlus
import java.awt.Color
import kotlin.math.abs

const val IMAGE_WIDTH = 2048
const val IMAGE_HEIGHT = 2048

fun main() {

    val image = IJ.createImage("result", "RGB", IMAGE_WIDTH, IMAGE_HEIGHT, 1)
    val zbuffer = Array(IMAGE_HEIGHT) { DoubleArray(IMAGE_WIDTH) { Double.NEGATIVE_INFINITY } }
    image.processor.setColor(Color.BLACK.rgb)
    image.processor.fill()

    val model = parseObj("/obj/skull.obj")
    model.normalizeVertices()

    val vertices = model.vertices.map { it * (IMAGE_WIDTH / 2 - 1 ).toDouble() + Vec3I(IMAGE_WIDTH / 2 , IMAGE_WIDTH / 2, IMAGE_WIDTH / 2) }


    model.triangles.forEach {
        val v0 = vertices[it[0]]
        val v1 = vertices[it[1]]
        val v2 = vertices[it[2]]

        val side1 = v1 - v0
        val side2 = v2 - v0
        val intensity = side1.cross(side2).normalize().scalar(Vec3D(0.0, 0.0, 1.0)).toFloat()
        if (intensity > 0) {
            val color = Color(intensity, intensity, intensity).rgb
            image.triangle(v0.toInt(), v1.toInt(), v2.toInt(), color, zbuffer)
        }
    }

    image.processor.flipVertical()
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

fun ImagePlus.triangle(v0: Vec3I, v1: Vec3I, v2: Vec3I, color: Int, zbuffer: Array<DoubleArray>) {
    val xes = intArrayOf(v0.x, v1.x, v2.x)
    val xmin = xes.min()!!
    val xmax = xes.max()!!

    val ys = intArrayOf(v0.y, v1.y, v2.y)
    val ymin = ys.min()!!
    val ymax = ys.max()!!

    for (x in xmin..xmax) {
        for (y in ymin..ymax) {
            val bary = barycentric(Vec3I(x, y, 0), v0, v1, v2)
            if (bary.x < 0 || bary.y < 0 || bary.z < 0) continue
            val z = v0.z * bary.x + v1.z * bary.y + v2.z * bary.z
            if (zbuffer[x][y] < z) {
                zbuffer[x][y] = z
                this.processor[x, y] = color
            }
        }
    }

}

fun barycentric(v0: Vec3I, v1: Vec3I, v2: Vec3I, v3: Vec3I): Vec3D {
    val denominator = ((v2.y - v3.y) * (v1.x - v3.x) + (v3.x - v2.x) * (v1.y - v3.y))
    val l0 = ((v2.y - v3.y) * (v0.x - v3.x) + (v3.x - v2.x) * (v0.y - v3.y)).toDouble() / denominator
    val l1 = ((v3.y - v1.y) * (v0.x - v3.x) + (v1.x - v3.x) * (v0.y - v3.y)).toDouble() / denominator
    val l2 = 1 - l0 - l1
    return Vec3D(l0, l1, l2)
}
