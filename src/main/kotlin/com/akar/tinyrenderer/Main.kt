package com.akar.tinyrenderer

import com.akar.tinyrenderer.gui.SimpleModelForSelect
import com.akar.tinyrenderer.math.Matrix
import com.akar.tinyrenderer.math.Vec3D
import com.akar.tinyrenderer.shader.LightShader
import com.akar.tinyrenderer.shader.PhongShader
import com.akar.tinyrenderer.util.DEFAULT_NAME
import com.akar.tinyrenderer.util.GifSequenceWriter
import com.akar.tinyrenderer.util.parseObj
import ij.IJ
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import javax.imageio.stream.FileImageOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.doubleArrayOf as da

const val DEFAULT_IMAGE_WIDTH = 512
const val DEFAULT_IMAGE_HEIGHT = 512
const val CIRCLE_SECTIONS = 36
const val FOV = 90.0

val camPos = Vec3D(0.0, 1.0, 2.0)
val focus = Vec3D(0.0, 0.0, 0.0)
val up = Vec3D(0.0, 1.0, 0.0)
val lightDir = Vec3D(1.0, 1.0, 1.0).normalize()

fun render(modelInfo: SimpleModelForSelect, imageview: ImageView) {
    val imageWidth: Int = DEFAULT_IMAGE_WIDTH
    val imageHeight: Int = DEFAULT_IMAGE_HEIGHT
    val startTime = System.currentTimeMillis()
    val image = IJ.createImage("result", "RGB", imageWidth, imageHeight, 1)
    image.processor.setColor(Color.BLACK)
    val outputStream = FileImageOutputStream(File("result.gif"))
    val writer = GifSequenceWriter(outputStream, BufferedImage.TYPE_INT_RGB, 100, true)
    val model = parseObj(modelInfo.objpath)
    if (modelInfo.diffmappath != null) {
        model.materials[DEFAULT_NAME]!!.mapKd = IJ.openImage(modelInfo.diffmappath)
    }
    if (modelInfo.bumpmappath != null) {
        model.materials[DEFAULT_NAME]!!.mapBump = IJ.openImage(modelInfo.bumpmappath)
    }
    if (modelInfo.specmappath != null) {
        model.materials[DEFAULT_NAME]!!.mapKs = IJ.openImage(modelInfo.specmappath)
    }

//    model.objects["4_LowerBody_M"]?.triangles?.forEach{it.reverse()}

    val light = parseObj("obj/light/light.obj")
    light.normalizeVertices()
    light.vertices = light.vertices.map { it / 15.0 }.toMutableList()
    model.normalizeVertices()
    val shader = PhongShader()
    val lightShader = LightShader()
    lightShader.view = lookat(camPos, focus, up)
    lightShader.viewport = viewport(imageWidth.toDouble(), imageHeight.toDouble())
    lightShader.projection = perspective(FOV, imageWidth.toDouble() / imageHeight, 0.1, 10.0)

    shader.view = lookat(camPos, focus, up)
    shader.lightDir = lightDir
    shader.viewport = viewport(imageWidth.toDouble(), imageHeight.toDouble())
    shader.projection = perspective(FOV, imageWidth.toDouble() / imageHeight, 0.1, 10.0)
    shader.campos = camPos
    for (i in 0 until CIRCLE_SECTIONS) {
        val start = System.currentTimeMillis()
        val zbuffer = DoubleArray(imageHeight * imageWidth) { Double.POSITIVE_INFINITY }
        image.processor.fill()
        println(">$i")
        val alfa = 2 * PI / CIRCLE_SECTIONS * i
        val rotation = Matrix(
            arrayOf(
                da(cos(alfa), 0.0, sin(alfa), 0.0),
                da(0.0, 1.0, 0.0, 0.0),
                da(-sin(alfa), 0.0, cos(alfa), 0.0),
                da(0.0, 0.0, 0.0, 1.0)
            )
        )
        val alfaModel = alfa * -1 + PI / 2;
        val rotationModel = Matrix(
            arrayOf(
                da(cos(alfaModel), 0.0, sin(alfaModel), 0.0),
                da(0.0, 1.0, 0.0, 0.0),
                da(-sin(alfaModel), 0.0, cos(alfaModel), 0.0),
                da(0.0, 0.0, 0.0, 1.0)
            )
        )
        val xrotation = Matrix(
            arrayOf(
                da(1.0, 0.0, 0.0, 0.0),
                da(0.0, cos(-alfaModel), -sin(-alfaModel), 0.0),
                da(0.0, sin(-alfaModel), cos(-alfaModel), 0.0),
                da(0.0, 0.0, 0.0, 1.0)
            )
        )
        var transfer = Matrix(4)
        transfer[3][0] = 0.3
        transfer[3][1] = 0.0
        transfer[3][2] = 1.25
        shader.model = rotation
        shader.load(model.vertices, model.vertexNormals, model.tVertices)
        shader.vertex()
        shader.lightPos = Vec3D(0.0, 0.0, 4.0) //Vec3D(sin(alfa) * 1.25 , 0.0, cos(alfa)*1.25 )
        for (obj in model.objects.values) {
            shader.material = model.materials[obj.material]!!
            val faces = shader.clipFaces(obj.triangles)
            faces.forEach {
                if (backfaceCulling(it, shader)) {
                    image.processor.triangle(it, zbuffer, shader)
                }
            }
        }

//        lightShader.model = rotation * transfer.transpose()
//        lightShader.load(light.vertices)
//        lightShader.vertex()
//        for (obj in light.objects.values) {
//            val faces = obj.triangles
//            faces.forEach {
//                image.processor.triangle(it, zbuffer, lightShader)
//            }
//        }
        image.processor.flipVertical()
        println("<$i ${System.currentTimeMillis() - start}")
        writer.writeToSequence(image.bufferedImage)
        imageview.image = Image(FileInputStream(File("result.gif")))
    }
    writer.close()
    println(System.currentTimeMillis() - startTime)
}