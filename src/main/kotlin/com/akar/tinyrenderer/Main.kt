package com.akar.tinyrenderer

import com.akar.tinyrenderer.math.Matrix
import com.akar.tinyrenderer.math.Vec3D
import com.akar.tinyrenderer.math.Vec3I
import com.akar.tinyrenderer.util.GifSequenceWriter
import com.akar.tinyrenderer.util.Material
import com.akar.tinyrenderer.util.Model
import com.akar.tinyrenderer.util.parseObj
import ij.IJ
import ij.ImagePlus
import ij.process.ImageProcessor
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.stream.FileImageOutputStream
import kotlin.math.*
import kotlin.doubleArrayOf as da

const val DEFAULT_IMAGE_WIDTH = 1024
const val DEFAULT_IMAGE_HEIGHT = 1024
const val CIRCLE_SECTIONS = 36
const val FOV = 90.0

val xrotationAlpha = -PI / 2
val xrotation = Matrix(arrayOf(
        da(1.0, .0, .0),
        da(.0, cos(xrotationAlpha), -sin(xrotationAlpha)),
        da(.0, sin(xrotationAlpha), cos(xrotationAlpha))))
val camPos = Vec3D(0.0, 0.0, 2.0)
val focus = Vec3D(0.0, 0.0, 0.0)
val up = Vec3D(0.0, 1.0, 0.0)

fun main(args: Array<String>) {
    val imageWidth: Int = DEFAULT_IMAGE_WIDTH
    val imageHeight: Int = DEFAULT_IMAGE_HEIGHT
    val startTime = System.currentTimeMillis()
    val image = IJ.createImage("result", "RGB", imageWidth, imageHeight, 1)
    image.processor.setColor(Color.BLACK)
    val outputStream = FileImageOutputStream(File("result.gif"))
    val writer = GifSequenceWriter(outputStream, BufferedImage.TYPE_INT_RGB, 100, true)
    val model = parseObj("obj/mech/mech.obj")
    model.normalizeVertices()
    for (i in 0 until CIRCLE_SECTIONS) {
        val start = System.currentTimeMillis()
        val zbuffer = DoubleArray(imageHeight * imageWidth) { Double.POSITIVE_INFINITY }
        image.processor.fill()
        println(">$i")
        val alfa = 2 * PI / CIRCLE_SECTIONS * i
        val look = lookat(camPos, focus, up)
        val rotation = Matrix(arrayOf(
                da(cos(alfa), 0.0, sin(alfa)),
                da(0.0, 1.0, 0.0),
                da(-sin(alfa), 0.0, cos(alfa))))
        val clip = perspective(FOV, imageWidth.toDouble() / imageHeight , 0.1, 10.0) * look * rotation
        val viewport = viewport(imageWidth.toDouble(), imageHeight.toDouble())
        val modelView = clip * viewport
        val clipCoords = model.vertices.map {
            (clip * it)
        }
        val viewportCoords = clipCoords.map { viewport * it }
        for(obj in model.objects.values) {
            val surfaces = obj.triangles.filter {
                val vertexIndices = it.first
                for (i in 0..2) {
                    val vertex = clipCoords[vertexIndices[i]]
                    if (vertex.x in -1.0..1.0 && vertex.y in -1.0..1.0 && vertex.z in 0.0..1.0)
                        return@filter true
                }
                false
            }
            rasterize(viewportCoords, surfaces, obj.material, model, image, zbuffer)
        }
        image.processor.flipVertical()
        println("<$i ${System.currentTimeMillis() - start}")
        writer.writeToSequence(image.bufferedImage)
    }
    writer.close()
    println(System.currentTimeMillis() - startTime)
}

fun rasterize(vertices: List<Vec3D>, triangles: List<Pair<Vec3I,Vec3I>>, material: String, model: Model, image: ImagePlus, zbuffer: DoubleArray) {
    triangles.forEach {
        val v0 = vertices[it.first[0]]
        val v1 = vertices[it.first[1]]
        val v2 = vertices[it.first[2]]

        val vt0: Vec3D
        val vt1: Vec3D
        val vt2: Vec3D

        if (it.second[0] != Int.MIN_VALUE) {
            vt0 = model.tVertices[it.second[0]]
            vt1 = model.tVertices[it.second[1]]
            vt2 = model.tVertices[it.second[2]]
        } else {
            vt0 = Vec3D(0.0, 0.0, 0.0)
            vt1 = Vec3D(0.0, 0.0, 0.0)
            vt2 = Vec3D(0.0, 0.0, 0.0)
        }

        val side1 = v1 - v0
        val side2 = v2 - v0
        val intensity = side1.cross(side2).normalize().scalar(Vec3D(0.0, 0.0, 1.0))
        if (intensity > 0) {
            image.processor.triangle(v0, v1, v2, vt0, vt1, vt2, model.materials[material]!!, zbuffer, intensity)
        }
    }
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

fun lookat(cameraPosition: Vec3D, focus: Vec3D, up: Vec3D): Matrix {
    val z = (cameraPosition - focus).normalize()
    val x = up.cross(z).normalize()
    val y = z.cross(x).normalize()
    val transition = Matrix(4)
    val transformation = Matrix(4)
    for (i in 0 until 3) {
        transformation[0][i] = x[i]
        transformation[1][i] = y[i]
        transformation[2][i] = z[i]
        transition[i][3] = -cameraPosition[i]
    }
    return transformation * transition
}

fun perspective(fov: Double, aspectRatio: Double, near: Double, far: Double): Matrix {
    val r = tan(fov / 2 * PI / 180) * near
    val l = -r
    val t = r / aspectRatio
    val b = -t
    return Matrix(arrayOf(
            da(2 * near / (r - l), 0.0,                (r + l) / (r - l),            0.0),
            da(0.0,                2 * near / (t - b), (t + b) / (t - b),            0.0),
            da(0.0,                0.0,                -(far + near) / (far - near), -2 * far * near / (far - near)),
            da(0.0,                0.0,                -1.0,                         0.0)))
}

fun viewport(width: Double, height: Double): Matrix {
    return Matrix(arrayOf(
            da(width / 2, 0.0,        0.0, width / 2),
            da(0.0,       height / 2, 0.0, height / 2),
            da(0.0,       0.0,        0.5, 0.5),
            da(0.0,       0.0,        0.0, 1.0)
    ))
}

fun ImageProcessor.triangle(v0: Vec3D, v1: Vec3D, v2: Vec3D,
                            vt0: Vec3D, vt1: Vec3D, vt2: Vec3D,
                            material: Material, zbuffer: DoubleArray,
                            intensity: Double) {
    val xes = da(v0.x, v1.x, v2.x)
    val xmin = xes.min()!!
    val xmax = xes.max()!!

    val ys = da(v0.y, v1.y, v2.y)
    val ymin = ys.min()!!
    val ymax = ys.max()!!

    operator fun DoubleArray.get(x: Int, y: Int) = get(y * width + x)
    operator fun DoubleArray.set(x: Int, y: Int, value: Double) = set(y * width + x, value)

    for (x in ceil(xmin).toInt()..xmax.toInt()) {
        if (x !in 0 until this.width) continue
        for (y: Int in ceil(ymin).toInt()..ymax.toInt()) {
            if (y !in 0 until this.height) continue
            val bary = barycentric(Vec3D(x.toDouble(), y.toDouble(), 0.0), v0, v1, v2)
            if (bary.x < 0 || bary.y < 0 || bary.z < 0) continue
            val z = v0.z * bary.x + v1.z * bary.y + v2.z * bary.z
            if (zbuffer[x, y] > z) {
                zbuffer[x, y] = z
                val pt = vt0 * bary.x + vt1 * bary.y + vt2 * bary.z
                this[x, y] = applyIntensity(material[pt.x, pt.y], intensity)
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

fun ImagePlus.antiAlias(level: Int): ImagePlus {
    val result = IJ.createImage("result", "RGB", this.width / level, this.height / level, 1)
    for (x in 0 until result.width) {
        for (y in 0 until result.height) {
            val colors = mutableListOf<Int>()
            for (i in 0 until level) {
                for (j in 0 until level) {
                    colors.add(this.processor[level * x + i, level * y + j])
                }
            }
            result.processor[x, y] = averageColor(*colors.toIntArray())
        }
    }
    return result
}

fun averageColor(vararg colors: Int): Int {
    val objColors = colors.map { Color(it) }
    return Color(objColors.sumBy { it.red } / objColors.size,
            objColors.sumBy { it.green } / objColors.size,
            objColors.sumBy { it.blue } / objColors.size).rgb
}