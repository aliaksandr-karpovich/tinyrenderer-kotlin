package com.akar.tinyrenderer

import java.io.InputStreamReader

class Model {
    val vertices = mutableListOf<Vec3D>()
    var triangles = mutableListOf<Vec3I>()

    var tVertices = mutableListOf<Vec3D>()
    var tTriangles = mutableListOf<Vec3I>()

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
