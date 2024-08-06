package com.akar.tinyrenderer.shader

import com.akar.tinyrenderer.math.Matrix
import com.akar.tinyrenderer.math.Vec3D
import com.akar.tinyrenderer.math.Vec4D
import com.akar.tinyrenderer.util.Face

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