package com.akar.tinyrenderer.shader

import com.akar.tinyrenderer.Shader
import com.akar.tinyrenderer.applyIntensity
import com.akar.tinyrenderer.math.Matrix
import com.akar.tinyrenderer.math.Vec3D
import com.akar.tinyrenderer.math.Vec4D
import com.akar.tinyrenderer.util.Face
import com.akar.tinyrenderer.util.Material
import java.awt.Color
import kotlin.math.max

class MyShader : Shader {

    override var model = Matrix(4)
    override var projection = Matrix(4)
    override var view = Matrix(3)
    override var viewport = Matrix(4)

    var vertices = listOf<Vec3D>()
    var tvertices = listOf<Vec3D>()
    var normals = listOf<Vec3D>()
    var material: Material = Material()

    override var clipCoords = listOf<Vec4D>()
    override var screenCoords = listOf<Vec3D>()
    override var ndc = listOf<Vec3D>()

    var clipNormals = listOf<Vec3D>()

    var clip = Matrix(4)


    var lightDir = Vec3D(.0, .0, 1.0)

    var lightPos = Vec3D(0.0, 0.0, 3.0)

    var worldCoords = listOf<Vec3D>()
    var MIT = Matrix()

    fun load(vertices: List<Vec3D>, normals: List<Vec3D>, tvertices: List<Vec3D>) {
        this.vertices = vertices
        this.tvertices = tvertices
        this.normals = normals
    }

    override fun vertex() {
        clip = projection * view * model
        MIT = model.inverse().transpose()
        worldCoords = vertices.map { model * it }
        clipNormals = normals.map { MIT * it }
        clipCoords = vertices.map { clip.homohenTimes(it) }
        ndc = clipCoords.map { it.toVec3D() }
        screenCoords = ndc.map { viewport * it }
        lightDir = lightDir.normalize()
    }

    override fun fragment(face: Face, bary: Vec3D): Int {
        val uv0 = tvertices[face.tvertvex[0]]
        val uv1 = tvertices[face.tvertvex[1]]
        val uv2 = tvertices[face.tvertvex[2]]

        val pt = uv0 * bary.x + uv1 * bary.y + uv2 * bary.z
        val v0 = worldCoords[face.vertex[0]]
        val v1 = worldCoords[face.vertex[1]]
        val v2 = worldCoords[face.vertex[2]]
        val l = (lightPos - (worldCoords[face.vertex[0]] * bary.x + worldCoords[face.vertex[1]] * bary.y + worldCoords[face.vertex[2]] * bary.z)).normalize()

        val edge1 = v1 - v0
        val edge2 = v2 - v0
        val duv1 = uv1 - uv0
        val duv2 = uv2 - uv0

        val f = 1.0 / (duv1.x * duv2.y - duv2.x * duv1.y);

//        val tangent = Vec3D(
//                (duv2.y * edge1.x - duv1.y * edge2.x) * f,
//                (duv2.y * edge1.y - duv1.y * edge2.y) * f,
//                (duv2.y * edge1.z - duv1.y * edge2.z) * f).normalize()
//        val bitangent = Vec3D(
//                f * (-duv2.x * edge1.x + duv1.x * edge2.x),
//                f * (-duv2.x * edge1.y + duv1.x * edge2.y),
//                f * (-duv2.x * edge1.z + duv1.x * edge2.z)).normalize()
        val  tangent = ((edge1 * duv2.y - edge2 * duv1.y) * f)
        val bitangent = ((edge2 * duv1.x - edge1 * duv2.x) * f)

        val binormal = tangent.cross(bitangent)

        val T = (model * Vec4D(tangent, 0.0)).toVec3D(false).normalize().toDoubleArray()
        val B = (model * Vec4D(bitangent, 0.0)).toVec3D(false).normalize().toDoubleArray()
        val N = (clipNormals[face.normal[0]] * bary.x + clipNormals[face.normal[1]] * bary.y + clipNormals[face.normal[2]] * bary.z).normalize().toDoubleArray()


        val TBN = Matrix(arrayOf(T,B,N)).transpose()

        val arr = Color(material.normal(pt.x, pt.y)).getRGBComponents(null).map { it.toDouble() }
        val tnormal = Vec3D(arr[0] * 2 - 1, arr[1] * 2 - 1, arr[2] * 2 -1).normalize()

        val n = (TBN * tnormal).normalize()

//        val n = (clipNormals[face.normal[0]] * bary.x + clipNormals[face.normal[1]] * bary.y + clipNormals[face.normal[2]] * bary.z).normalize()
        val intensity = max(n * l, 0.25)

        return   applyIntensity(material[pt.x, pt.y], intensity)
    }
}