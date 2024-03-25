package com.akar.tinyrenderer

import com.akar.tinyrenderer.math.Matrix
import com.akar.tinyrenderer.math.Vec3D
import com.akar.tinyrenderer.math.Vec4D
import com.akar.tinyrenderer.shader.LightShader
import com.akar.tinyrenderer.util.Face
import ij.IJ
import ij.ImagePlus
import ij.process.ImageProcessor
import java.awt.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.tan

interface Shader {
    var model: Matrix
    var view: Matrix
    var projection: Matrix
    var viewport: Matrix

    var clipCoords: List<Vec4D>
    var screenCoords: List<Vec3D>
    var ndc: List<Vec3D>

    fun vertex()
    fun clipFaces(faces: List<Face>) = faces.filter {
        val vertexIndices = it.vertex
        for (i in 0..2) {
            val vertex = clipCoords[vertexIndices[i]].toVec3D()
            if (vertex.x in -1.0..1.0 && vertex.y in -1.0..1.0 && vertex.z in 0.0..1.0)
                return@filter true
        }
        false
    }
    fun fragment(face: Face, bary: Vec3D): Int
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
            doubleArrayOf(2 * near / (r - l), 0.0,                (r + l) / (r - l),            0.0),
            doubleArrayOf(0.0,                2 * near / (t - b), (t + b) / (t - b),            0.0),
            doubleArrayOf(0.0,                0.0,                -(far + near) / (far - near), -2 * far * near / (far - near)),
            doubleArrayOf(0.0,                0.0,                -1.0,                         0.0)))
}

fun viewport(width: Double, height: Double): Matrix {
    return Matrix(arrayOf(
            doubleArrayOf(width / 2, 0.0,        0.0, width / 2),
            doubleArrayOf(0.0,       height / 2, 0.0, height / 2),
            doubleArrayOf(0.0,       0.0,        0.5, 0.5),
            doubleArrayOf(0.0,       0.0,        0.0, 1.0)
    ))
}


fun ImageProcessor.triangle(face: Face,
                             zbuffer: DoubleArray,
                            shader: Shader) {
    val v0 = shader.clipCoords[face.vertex[0]]
    val v1 = shader.clipCoords[face.vertex[1]]
    val v2 = shader.clipCoords[face.vertex[2]]
    val v0s = shader.screenCoords[face.vertex[0]]
    val v1s = shader.screenCoords[face.vertex[1]]
    val v2s = shader.screenCoords[face.vertex[2]]
    val xes = doubleArrayOf(v0s.x, v1s.x, v2s.x)
    val xmin = xes.min()
    val xmax = xes.max()

    val ys = doubleArrayOf(v0s.y, v1s.y, v2s.y)
    val ymin = ys.minOrNull()!!
    val ymax = ys.maxOrNull()!!

    operator fun DoubleArray.get(x: Int, y: Int) = get(y * width + x)
    operator fun DoubleArray.set(x: Int, y: Int, value: Double) = set(y * width + x, value)

    for (x in ceil(xmin).toInt()..xmax.toInt()) {
        if (x !in 0 until this.width) continue
        for (y: Int in ceil(ymin).toInt()..ymax.toInt()) {
            if (y !in 0 until this.height) continue
            val baryScreen = barycentric(Vec3D(x.toDouble(), y.toDouble(), 0.0), v0s, v1s, v2s)
            var baryClip = Vec3D(baryScreen.x / v0.w, baryScreen.y / v1.w, baryScreen.z / v2.w)
            baryClip /= baryClip.x + baryClip.y + baryClip.z
            if (baryScreen.x < 0 || baryScreen.y < 0 || baryScreen.z < 0) continue
            val z = v0.z * baryClip.x + v1.z * baryClip.y + v2.z * baryClip.z
//            if (shader is LightShader) {
//                this[x,y] = Color.white.rgb
//            }
            if (zbuffer[x, y] > z) {
                zbuffer[x, y] = z
                this[x, y] = shader.fragment(face, baryClip)

            }
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

fun barycentric(v0: Vec3D, v1: Vec3D, v2: Vec3D, v3: Vec3D): Vec3D {
    val denominator = (v2.y - v3.y) * (v1.x - v3.x) + (v3.x - v2.x) * (v1.y - v3.y)
    val l0 = ((v2.y - v3.y) * (v0.x - v3.x) + (v3.x - v2.x) * (v0.y - v3.y)) / denominator
    val l1 = ((v3.y - v1.y) * (v0.x - v3.x) + (v1.x - v3.x) * (v0.y - v3.y)) / denominator
    val l2 = 1 - l0 - l1
    return Vec3D(l0, l1, l2)
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
    return Color(objColors.sumOf { it.red } / objColors.size,
            objColors.sumOf { it.green } / objColors.size,
            objColors.sumOf { it.blue } / objColors.size).rgb
}

fun applyIntensity(rgb: Int, intensity: Double): Int {
    val color = Color(rgb)
    return Color((color.red * intensity).toInt(), (color.green * intensity).toInt(), (color.blue * intensity).toInt()).rgb
}

fun backfaceCulling(face: Face, shader: Shader): Boolean {
    val v0 = shader.ndc[face.vertex[0]]
    val v1 = shader.ndc[face.vertex[1]]
    val v2 = shader.ndc[face.vertex[2]]

    val side1 = v1 - v0
    val side2 = v2 - v0
    val intensity = side1.cross(side2).normalize() * Vec3D(0.0, 0.0, 1.0)
    return intensity > 0
}