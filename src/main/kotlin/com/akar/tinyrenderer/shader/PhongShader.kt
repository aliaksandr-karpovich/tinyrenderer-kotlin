package com.akar.tinyrenderer.shader

import com.akar.tinyrenderer.Shader
import com.akar.tinyrenderer.applyIntensity
import com.akar.tinyrenderer.math.Matrix
import com.akar.tinyrenderer.math.Vec3D
import com.akar.tinyrenderer.math.Vec4D
import com.akar.tinyrenderer.util.Face
import com.akar.tinyrenderer.util.Material
import kotlin.math.max

class PhongShader : Shader {
    override var model = Matrix(3)
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

    var clip = Matrix()


    var lightDir = Vec3D(.0, .0, 1.0)
    var MIT= Matrix()

    fun load(vertices: List<Vec3D>, normals: List<Vec3D>, tvertices: List<Vec3D>) {
        this.vertices = vertices
        this.tvertices = tvertices
        this.normals = normals
    }

    override fun vertex() {
        clip = projection * view * model
        MIT = model.inverse().transpose()
        clipNormals = normals.map { MIT * it }
        clipCoords = vertices.map { clip.homohenTimes(it) }
        ndc = clipCoords.map { it.toVec3D() }
        screenCoords = ndc.map { viewport * it }
    }

    override fun fragment(face: Face, baricentric: Vec3D): Int {
        val vt0 = tvertices[face.tvertvex[0]]
        val vt1 = tvertices[face.tvertvex[1]]
        val vt2 = tvertices[face.tvertvex[2]]
        val l = lightDir.normalize()
        val n =(clipNormals[face.normal[0]]*baricentric.x + clipNormals[face.normal[1]] * baricentric.y + clipNormals[face.normal[2]] * baricentric.z).normalize()
        var intensity =  n * l
        intensity = max(intensity, 0.1)
        val pt = vt0 * baricentric.x + vt1 * baricentric.y + vt2 * baricentric.z
        return applyIntensity(material[pt.x, pt.y], intensity)
    }
}