package com.akar.tinyrenderer.shader

import com.akar.tinyrenderer.math.Matrix
import com.akar.tinyrenderer.math.Vec3D
import com.akar.tinyrenderer.math.Vec4D
import com.akar.tinyrenderer.util.Face
import java.awt.Color

class LightShader: Shader {
    override var model = Matrix(3)
    override var view = Matrix(4)
    override var projection = Matrix(4)
    override var viewport = Matrix(4)
    override var clipCoords= listOf<Vec4D>()
    override var screenCoords = listOf<Vec3D>()
    override var ndc = listOf<Vec3D>()


    var vertices = listOf<Vec3D>()
    fun load(vertices: List<Vec3D>) {
        this.vertices = vertices
    }

    override fun vertex() {
        val clip = projection * view * model
        clipCoords = vertices.map { clip.homohenTimes(it) }
        ndc = clipCoords.map { it.toVec3D() }
        screenCoords = ndc.map { viewport * it }
    }

    override fun fragment(face: Face, bary: Vec3D): Int {
        return Color.white.rgb
    }
}