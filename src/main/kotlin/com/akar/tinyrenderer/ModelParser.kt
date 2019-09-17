package com.akar.tinyrenderer

import java.io.InputStreamReader


class Model {
    val vertices = mutableListOf<Vector3<Double>>()
    var triangles = mutableListOf<Vector3<Int>>()

    var tVertices = mutableListOf<Vector3<Double>>()
    var tTriangles = mutableListOf<Vector3<Int>>()

}

class Vector3<T : Number>(var x: T, var y: T, var z: T) {
    operator fun get(i: Int): T =
            when (i) {
                0 -> x
                1 -> y
                2 -> z
                else -> throw IllegalArgumentException("must be in 0..2")
            }

    operator fun set(i: Int, value: T) {
        when (i) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            else -> throw IllegalArgumentException("must be in 0..2")
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
