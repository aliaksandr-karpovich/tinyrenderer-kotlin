package com.akar.tinyrenderer

import com.akar.tinyrenderer.math.Matrix
import com.akar.tinyrenderer.math.Vec3D
import com.akar.tinyrenderer.shader.GouraudShader
import com.akar.tinyrenderer.util.GifSequenceWriter
import com.akar.tinyrenderer.util.parseObj
import ij.IJ
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.stream.FileImageOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.doubleArrayOf as da

const val DEFAULT_IMAGE_WIDTH = 1024
const val DEFAULT_IMAGE_HEIGHT = 1024
const val CIRCLE_SECTIONS = 36
const val FOV = 90.0

val camPos = Vec3D(0.0, 1.0, 2.0)
val focus = Vec3D(0.0, 0.0, 0.0)
val up = Vec3D(0.0, 1.0, 0.0)
val lightDir = Vec3D(1.0, 1.0, 1.0).normalize()

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
    val shader = GouraudShader()
    shader.view = lookat(camPos, focus, up)
    shader.lightDir = lightDir
    shader.viewport = viewport(imageWidth.toDouble(), imageHeight.toDouble())
    shader.projection = perspective(FOV, imageWidth.toDouble() / imageHeight, 0.1, 10.0)
    for (i in 0 until CIRCLE_SECTIONS) {
        val start = System.currentTimeMillis()
        val zbuffer = DoubleArray(imageHeight * imageWidth) { Double.POSITIVE_INFINITY }
        image.processor.fill()
        println(">$i")
        val alfa = 2 * PI / CIRCLE_SECTIONS * i
        val rotation = Matrix(arrayOf(
                da(cos(alfa), 0.0, sin(alfa)),
                da(0.0, 1.0, 0.0),
                da(-sin(alfa), 0.0, cos(alfa))))
        shader.model = rotation
        shader.load(model.vertices, model.vertexNormals, model.tVertices)
        shader.vertex()
        for (obj in model.objects.values) {
            shader.material = model.materials[obj.material]!!
            val faces = shader.clipFaces(obj.triangles)
            faces.forEach {
                if (backfaceCulling(it, shader)) {
                    image.processor.triangle(it, zbuffer, shader)
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