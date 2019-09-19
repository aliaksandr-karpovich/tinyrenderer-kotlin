package com.akar.tinyrenderer.util

import com.akar.tinyrenderer.math.Vec3D
import com.akar.tinyrenderer.math.Vec3I
import com.akar.tinyrenderer.math.Vector3
import java.io.InputStreamReader
import kotlin.math.max

class Model {
    val vertices = mutableListOf<Vec3D>()
    var triangles = mutableListOf<Vec3I>()

    var tVertices = mutableListOf<Vec3D>()
    var tTriangles = mutableListOf<Vec3I>()

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
    val file = InputStreamReader(Model::class.java.getResource(fileName).openStream())
    file.readLines().asSequence().map { it.split(" ") }.forEach {
        when (it[0]) {
            "v" -> result.vertices.add(Vector3(it[1].toDouble(), it[2].toDouble(), it[3].toDouble()))
            "f" -> result.triangles.add(Vector3(it[1].split("/")[0].toInt() - 1, it[2].split("/")[0].toInt() - 1, it[3].split("/")[0].toInt() - 1))
        }
    }
    return result
}
