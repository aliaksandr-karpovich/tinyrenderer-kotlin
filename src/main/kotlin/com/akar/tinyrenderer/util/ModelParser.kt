package com.akar.tinyrenderer.util

import com.akar.tinyrenderer.math.Vec3D
import com.akar.tinyrenderer.math.Vec3I
import com.akar.tinyrenderer.math.Vector3
import ij.ImagePlus
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import kotlin.math.max

class Model {
    val vertices = mutableListOf<Vec3D>()
    var triangles = mutableListOf<Vec3I>()

    var tVertices = mutableListOf<Vec3D>()
    var tTriangles = mutableListOf<Vec3I>()

    var diffuseTexture: ImagePlus? = null

    fun normalizeVertices() {
        var max = Double.NEGATIVE_INFINITY
        vertices.forEach {
            for (i in 0..2) {
                max = max(it[i], max)
            }
        }
        for (i in vertices.indices) {
            vertices[i] = vertices[i] / max
        }
    }

}

fun parseObj(fileName: String): Model {
    val result = Model()
    val file = FileReader(File(fileName))
    file.readLines().asSequence().map { it.split(" ").filter { it.isNotEmpty() } }.filter { it.isNotEmpty() }.forEach {
        when (it[0]) {
            "v" -> result.vertices.add(Vector3(it[1].toDouble(), it[2].toDouble(), it[3].toDouble()))
            "f" -> {
                val vertexIds = Vec3I(0, 0, 0)
                val textureVertexIds = Vec3I(0, 0, 0)
                for (i in 1..3) {
                    val ids = it[i].split("/").map { it.toInt() }
                    vertexIds[i - 1] = ids[0] - 1
                    textureVertexIds[i - 1] = ids[1] - 1
                }
                result.triangles.add(vertexIds)
                result.tTriangles.add(textureVertexIds)
            }
        }
    }
    return result
}
