package com.akar.tinyrenderer

import com.akar.tinyrenderer.math.Matrix
import com.akar.tinyrenderer.math.Vec3D
import com.akar.tinyrenderer.math.Vec3I
import com.akar.tinyrenderer.math.Vec4D
import com.akar.tinyrenderer.util.*
import ij.IJ
import ij.ImagePlus
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.stream.FileImageOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.doubleArrayOf as da

const val DEFAULT_IMAGE_WIDTH = 1024
const val DEFAULT_IMAGE_HEIGHT = 1024
const val CIRCLE_SECTIONS = 36
const val FOV = 90.0

val camPos = Vec3D(0.0, 1.0, 2.0)
val focus = Vec3D(0.0, 0.0, 0.0)
val up = Vec3D(0.0, 1.0, 0.0)
val lightDir = Vec3D(1.0,1.0,1.0).normalize()
var viewport = Matrix()

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
    val shader = MyShader(imageWidth.toDouble() / imageHeight)
    shader.view = lookat(camPos, focus, up)
    shader.lightDir = lightDir
    viewport = viewport(imageWidth.toDouble(), imageHeight.toDouble())
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
        shader.vertex(model.vertices)
        for (obj in model.objects.values) {
            shader.material = model.materials[obj.material]!!
            val faces = shader.clipFaces(obj.triangles)
            rasterize(faces, image, zbuffer, shader)
        }
        image.processor.flipVertical()
        println("<$i ${System.currentTimeMillis() - start}")
        writer.writeToSequence(image.bufferedImage)
    }
    writer.close()
    println(System.currentTimeMillis() - startTime)
}

fun rasterize(triangles: List<Face>,
              image: ImagePlus, zbuffer: DoubleArray, shader: MyShader) {
    triangles.forEach {
        val v0 = shader.ndc[it.vertex[0]]
        val v1 = shader.ndc[it.vertex[1]]
        val v2 = shader.ndc[it.vertex[2]]

        val side1 = v1 - v0
        val side2 = v2 - v0
        val intensity = side1.cross(side2).normalize() * Vec3D(0.0, 0.0, 1.0)
        if (intensity > 0) {
            image.processor.triangle(it, zbuffer, shader)
        }
    }
}

fun applyIntensity(rgb: Int, intensity: Double): Int {
    val color = Color(rgb)
    return Color((color.red * intensity).toInt(), (color.green * intensity).toInt(), (color.blue * intensity).toInt()).rgb
}

class MyShader(aspectRatio: Double) : Shader {
    var model = Matrix(3)
    val projection = perspective(FOV, aspectRatio, 0.1, 10.0)
    var view = Matrix(3)

    var vertices = listOf<Vec3D>()
    var tvertices = listOf<Vec3D>()
    var normals = listOf<Vec3D>()
    var material: Material = Material()


    var intensities = listOf<Double>()

    var clipCoords = listOf<Vec4D>()
    var ndc = listOf<Vec3D>()

    var modelNormals = listOf<Vec3D>()
    var clipCoordsNormals = listOf<Vec4D>()

    var lightDir = Vec3D(.0, .0, 1.0)

    fun load(vertices: List<Vec3D>, normals: List<Vec3D>, tvertices: List<Vec3D>) {
        this.vertices = vertices
        this.tvertices = tvertices
        this.normals = normals
    }

    override fun vertex(vertices: List<Vec3D>): List<Vec4D> {
        val clip = projection * view * model
        this.intensities = this.normals.mapIndexed { index, vector3 ->
            (model.inverse().transpose() * vector3).normalize() * lightDir.normalize()
        }
        clipCoords = vertices.map { clip.homohenTimes(it) }
        ndc = clipCoords.map { it.toVec3D() }

        return clipCoords
    }

    fun clipFaces(faces: List<Face>) = faces.filter {
        val vertexIndices = it.vertex
        for (i in 0..2) {
            val vertex = clipCoords[vertexIndices[i]].toVec3D()
            if (vertex.x in -1.0..1.0 && vertex.y in -1.0..1.0 && vertex.z in 0.0..1.0)
                return@filter true
        }
        false
    }

    override fun fragment(face: Face, baricentric: Vec3D): Int {
        val vt0 = tvertices[face.tvertvex[0]]
        val vt1 = tvertices[face.tvertvex[1]]
        val vt2 = tvertices[face.tvertvex[2]]

        var intensity = intensities[face.normal[0]] * baricentric[0] + intensities[face.normal[1]] * baricentric[1]+ intensities[face.normal[2]] * baricentric[2]
        intensity = max(intensity, 0.0)
        val pt = vt0 * baricentric.x + vt1 * baricentric.y + vt2 * baricentric.z
        return applyIntensity(material[pt.x, pt.y], intensity)
    }
}