package com.akar.tinyrenderer

import com.akar.tinyrenderer.gui.Direction
import com.akar.tinyrenderer.gui.Rotation
import com.akar.tinyrenderer.gui.SimpleModelForSelect
import com.akar.tinyrenderer.math.Matrix
import com.akar.tinyrenderer.math.Vec3D
import com.akar.tinyrenderer.shader.LightShader
import com.akar.tinyrenderer.shader.PhongShader
import com.akar.tinyrenderer.util.DEFAULT_NAME
import com.akar.tinyrenderer.util.GifSequenceWriter
import com.akar.tinyrenderer.util.Model
import com.akar.tinyrenderer.util.parseObj
import ij.IJ
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
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

fun render(
    modelInfo: SimpleModelForSelect,
    imageview: ImageView,
    modelRotationSwitch: Rotation = Rotation.Y,
    modelRotationDirection: Direction = Direction.COUNTER_CLOCKWISE,
    lightRotationSwitch: Rotation = Rotation.Y,
    lightRotationDirection: Direction = Direction.COUNTER_CLOCKWISE
) {
    val imageWidth: Int = DEFAULT_IMAGE_WIDTH
    val imageHeight: Int = DEFAULT_IMAGE_HEIGHT
    val startTime = System.currentTimeMillis()
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

    val light = parseObj("obj/light/light.obj")
    light.normalizeVertices()
    light.vertices = light.vertices.map { it / 15.0 }.toMutableList()
    model.normalizeVertices()

    val viewMatrix = lookat(camPos, focus, up)
    val viewportMatrix = viewport(imageWidth.toDouble(), imageHeight.toDouble())
    val projectionMatrix = perspective(FOV, imageWidth.toDouble() / imageHeight, 0.1, 10.0)

    val frames = runBlocking(Dispatchers.Default) {
        (0 until CIRCLE_SECTIONS).map { i ->
            async {
                renderFrame(
                    i, imageWidth, imageHeight, model, light,
                    viewMatrix, viewportMatrix, projectionMatrix,
                    modelRotationSwitch, modelRotationDirection,
                    lightRotationSwitch, lightRotationDirection
                )
            }
        }.awaitAll()
    }

    val outputStream = FileImageOutputStream(File("result.gif"))
    val writer = GifSequenceWriter(outputStream, BufferedImage.TYPE_INT_RGB, 100, true)
    for (frame in frames) {
        writer.writeToSequence(frame)
    }
    writer.close()
    outputStream.close()

    imageview.image = Image(FileInputStream(File("result.gif")))
    println("Total: ${System.currentTimeMillis() - startTime}ms")
}

private fun renderFrame(
    frameIndex: Int,
    imageWidth: Int,
    imageHeight: Int,
    model: Model,
    light: Model,
    viewMatrix: Matrix,
    viewportMatrix: Matrix,
    projectionMatrix: Matrix,
    modelRotationSwitch: Rotation,
    modelRotationDirection: Direction,
    lightRotationSwitch: Rotation,
    lightRotationDirection: Direction
): BufferedImage {
    val start = System.currentTimeMillis()
    val image = IJ.createImage("frame$frameIndex", "RGB", imageWidth, imageHeight, 1)
    image.processor.setColor(Color.BLACK)
    image.processor.fill()
    val zbuffer = DoubleArray(imageHeight * imageWidth) { Double.POSITIVE_INFINITY }

    println(">$frameIndex")
    val alfa = 2 * PI / CIRCLE_SECTIONS * frameIndex

    val modelRotation = pivotRotation(modelRotationSwitch, alfaFromDirection(modelRotationDirection, alfa))
    val lightRotation = pivotRotation(lightRotationSwitch, alfaFromDirection(lightRotationDirection, alfa))

    val shader = PhongShader()
    shader.view = viewMatrix
    shader.lightDir = lightDir
    shader.viewport = viewportMatrix
    shader.projection = projectionMatrix
    shader.campos = camPos
    shader.model = modelRotation
    shader.load(model.vertices, model.vertexNormals, model.tVertices)
    shader.vertex()
    shader.lightPos = lightRotation * Vec3D(0.0, 0.0, 1.25)

    for (obj in model.objects.values) {
        shader.material = model.materials[obj.material]!!
        val faces = shader.clipFaces(obj.triangles)
        faces.forEach {
            if (backfaceCulling(it, shader)) {
                image.processor.triangle(it, zbuffer, shader)
            }
        }
    }

    val transfer = Matrix(4)
    transfer[3][0] = 0.0
    transfer[3][1] = 0.0
    transfer[3][2] = 1.25

    val lightShader = LightShader()
    lightShader.view = viewMatrix
    lightShader.viewport = viewportMatrix
    lightShader.projection = projectionMatrix
    lightShader.model = lightRotation * transfer.transpose()
    lightShader.load(light.vertices)
    lightShader.vertex()

    for (obj in light.objects.values) {
        obj.triangles.forEach {
            image.processor.triangle(it, zbuffer, lightShader)
        }
    }

    image.processor.flipVertical()
    println("<$frameIndex ${System.currentTimeMillis() - start}ms")
    return image.bufferedImage
}

private fun alfaFromDirection(
    modelRotationDirection: Direction,
    alfa: Double
) = when (modelRotationDirection) {
    Direction.COUNTER_CLOCKWISE -> alfa
    Direction.CLOCKWISE -> -alfa
    Direction.NONE -> 0.0
}

private fun pivotRotation(
    modelRotationSwitch: Rotation,
    alfa: Double
) = when (modelRotationSwitch) {
    Rotation.X -> xrotation(alfa)
    Rotation.X_AND_Y -> xrotation(alfa) * yrotation(alfa)
    Rotation.Y -> yrotation(alfa)
}

private fun yrotation(alfa: Double) = Matrix(
    arrayOf(
        da(cos(alfa), 0.0, sin(alfa), 0.0),
        da(0.0, 1.0, 0.0, 0.0),
        da(-sin(alfa), 0.0, cos(alfa), 0.0),
        da(0.0, 0.0, 0.0, 1.0)
    )
)

private fun xrotation(alfa: Double) = Matrix(
    arrayOf(
        da(1.0, 0.0, 0.0, 0.0),
        da(0.0, cos(-alfa), -sin(-alfa), 0.0),
        da(0.0, sin(-alfa), cos(-alfa), 0.0),
        da(0.0, 0.0, 0.0, 1.0)
    )
)