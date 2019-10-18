package com.akar.tinyrenderer.shader

import com.akar.tinyrenderer.Shader
import com.akar.tinyrenderer.applyIntensity
import com.akar.tinyrenderer.math.Matrix
import com.akar.tinyrenderer.math.Vec3D
import com.akar.tinyrenderer.math.Vec4D
import com.akar.tinyrenderer.util.Face
import com.akar.tinyrenderer.util.Material
import kotlin.math.max

class GouraudShader: Shader {
    override var model = Matrix(3)
    override var projection = Matrix(4)
    override var view = Matrix(3)
    override var viewport = Matrix(4)

    var vertices = listOf<Vec3D>()
    var tvertices = listOf<Vec3D>()
    var normals = listOf<Vec3D>()
    var material: Material = Material()

    var intensities = listOf<Double>()

    override var clipCoords = listOf<Vec4D>()
    override var screenCoords = listOf<Vec3D>()
    override var ndc = listOf<Vec3D>()

    var modelNormals = listOf<Vec3D>()
    var clipCoordsNormals = listOf<Vec4D>()

    var lightDir = Vec3D(.0, .0, 1.0)

    fun load(vertices: List<Vec3D>, normals: List<Vec3D>, tvertices: List<Vec3D>) {
        this.vertices = vertices
        this.tvertices = tvertices
        this.normals = normals
    }

    override fun vertex() {
        val clip = projection * view * model
        this.intensities = this.normals.mapIndexed { index, vector3 ->
            (model.inverse().transpose() * vector3).normalize() * lightDir.normalize()
        }
        clipCoords = vertices.map { clip.homohenTimes(it) }
        ndc = clipCoords.map { it.toVec3D() }
        screenCoords = ndc.map { viewport * it }
    }

    override fun fragment(face: Face, baricentric: Vec3D): Int {
        val vt0 = tvertices[face.tvertvex[0]]
        val vt1 = tvertices[face.tvertvex[1]]
        val vt2 = tvertices[face.tvertvex[2]]
        var intensity = intensities[face.normal[0]] * baricentric[0] + intensities[face.normal[1]] * baricentric[1] + intensities[face.normal[2]] * baricentric[2]
        intensity = max(intensity, 0.0)
        val pt = vt0 * baricentric.x + vt1 * baricentric.y + vt2 * baricentric.z
        return applyIntensity(material[pt.x, pt.y], intensity)
    }
}