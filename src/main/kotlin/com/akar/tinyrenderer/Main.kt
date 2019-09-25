package com.akar.tinyrenderer

import com.akar.tinyrenderer.math.Matrix
import com.akar.tinyrenderer.math.Vec3D
import com.akar.tinyrenderer.math.Vec3I
import com.akar.tinyrenderer.util.GifSequenceWriter
import com.akar.tinyrenderer.util.parseObj
import ij.IJ
import ij.ImagePlus
import ij.process.ImageProcessor
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.stream.FileImageOutputStream
import kotlin.math.*

const val IMAGE_WIDTH = 1024
const val IMAGE_HEIGHT = 1024
const val CIRCLE_SECTIONS = 36
const val C = 1.0

val PERSPECTIVE_PROJECTION = Matrix(arrayOf(
        doubleArrayOf(1.0, .0, .0, .0),
        doubleArrayOf(.0, 1.0, .0, .0),
        doubleArrayOf(.0, .0, 1.0, .0),
        doubleArrayOf(.0, .0, -1 / C, 1.0)))
val xrotation = Matrix(arrayOf(
        doubleArrayOf(1.0, .0, .0),
        doubleArrayOf(.0, .0, -1.0),
        doubleArrayOf(.0, 1.0, .0)
))

fun main() {
    val startTime = System.currentTimeMillis()
    val image = IJ.createImage("result", "RGB", IMAGE_WIDTH, IMAGE_HEIGHT, 1)
    image.processor.setColor(Color.BLACK)
    val outputStream = FileImageOutputStream(File("result.gif"))
    val writer = GifSequenceWriter(outputStream, BufferedImage.TYPE_INT_RGB, 100, true)
    val model = parseObj("obj/mech/mech.obj")
    model.normalizeVertices()
    for (i in 0 until CIRCLE_SECTIONS) {
        val start = System.currentTimeMillis()
        val zbuffer = DoubleArray(IMAGE_HEIGHT * IMAGE_WIDTH) { Double.NEGATIVE_INFINITY }
        image.processor.fill()
        println(">$i")
        val alfa = 2 * PI / CIRCLE_SECTIONS * i
        val rotation = Matrix(arrayOf(doubleArrayOf(cos(alfa), 0.0, sin(alfa)),
                doubleArrayOf(0.0, 1.0, 0.0),
                doubleArrayOf(-sin(alfa), 0.0, cos(alfa))))
        val vertices = model.vertices.map {
            (PERSPECTIVE_PROJECTION * (rotation * it + Vec3I(0, 0, -1))) * (IMAGE_WIDTH / 2 - 1).toDouble() + Vec3I(IMAGE_WIDTH / 2, IMAGE_HEIGHT / 2, IMAGE_HEIGHT / 2)
        }

        model.objects.values.forEach { obj ->
            obj.triangles.forEach {
                val v0 = vertices[it.first[0]]
                val v1 = vertices[it.first[1]]
                val v2 = vertices[it.first[2]]

                val vt0 = model.tVertices[it.second[0]]
                val vt1 = model.tVertices[it.second[1]]
                val vt2 = model.tVertices[it.second[2]]

                val side1 = v1 - v0
                val side2 = v2 - v0
                val intensity = side1.cross(side2).normalize().scalar(Vec3D(0.0, 0.0, 1.0))
                if (intensity > 0) {
                    image.processor.triangle(v0, v1, v2, vt0, vt1, vt2, model.materials[obj.material]?.mapKd!!, zbuffer, intensity)
                }
            }
        }
        image.processor.flipVertical()
        println("<$i ${System.currentTimeMillis() - start}")
        writer.writeToSequence(image.bufferedImage)
    }
    writer.close()
    println(System.currentTimeMillis() - startTime)
}

fun intensityRange(value: Double) = when (value) {
    in 0.0..0.4 -> 0.4f
    in 0.4..0.8 -> 0.8f
    else -> 1.0f
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

fun ImageProcessor.triangle(v0: Vec3D, v1: Vec3D, v2: Vec3D,
                            vt0: Vec3D, vt1: Vec3D, vt2: Vec3D,
                            diffuse: ImagePlus, zbuffer: DoubleArray,
                            intensity: Double) {
    val xes = doubleArrayOf(v0.x, v1.x, v2.x)
    val xmin = xes.min()!!
    val xmax = xes.max()!!

    val ys = doubleArrayOf(v0.y, v1.y, v2.y)
    val ymin = ys.min()!!
    val ymax = ys.max()!!

    operator fun DoubleArray.get(x: Int, y: Int) = get(x * width + y)
    operator fun DoubleArray.set(x: Int, y: Int, value: Double) = set(x * width + y, value)

    for (x in ceil(xmin).toInt()..xmax.toInt()) {
        if (x !in 0 until IMAGE_HEIGHT) continue
        for (y: Int in ceil(ymin).toInt()..ymax.toInt()) {
            if (y !in 0 until IMAGE_WIDTH) continue
            val bary = barycentric(Vec3D(x.toDouble(), y.toDouble(), 0.0), v0, v1, v2)
            if (bary.x < 0 || bary.y < 0 || bary.z < 0) continue
            val z = v0.z * bary.x + v1.z * bary.y + v2.z * bary.z
            if (zbuffer[x, y] < z) {
                zbuffer[x, y] = z
                val pt = vt0 * bary.x + vt1 * bary.y + vt2 * bary.z
                pt.x *= diffuse.width
                pt.y = (1 - pt.y) * diffuse.height
                this[x, y] = applyIntensity(diffuse.processor[pt.x.toInt(), pt.y.toInt()], intensity)
            }
        }
    }
}

fun barycentric(v0: Vec3D, v1: Vec3D, v2: Vec3D, v3: Vec3D): Vec3D {
    val denominator = (v2.y - v3.y) * (v1.x - v3.x) + (v3.x - v2.x) * (v1.y - v3.y)
    val l0 = ((v2.y - v3.y) * (v0.x - v3.x) + (v3.x - v2.x) * (v0.y - v3.y)) / denominator
    val l1 = ((v3.y - v1.y) * (v0.x - v3.x) + (v1.x - v3.x) * (v0.y - v3.y)) / denominator
    val l2 = 1 - l0 - l1
    return Vec3D(l0, l1, l2)
}

fun applyIntensity(rgb: Int, intensity: Double): Int {
    val color = Color(rgb)
    return Color((color.red * intensity).toInt(), (color.green * intensity).toInt(), (color.blue * intensity).toInt()).rgb
}