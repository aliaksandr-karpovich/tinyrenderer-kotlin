package com.akar.tinyrenderer.util

import com.akar.tinyrenderer.math.Vec3D
import com.akar.tinyrenderer.math.Vec3I
import ij.ImagePlus
import java.io.File
import java.io.FileReader
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.Double.Companion.NEGATIVE_INFINITY as NEG_INF
import kotlin.Double.Companion.POSITIVE_INFINITY as POS_INF

class Model {
    val vertices = mutableListOf<Vec3D>()
    var triangles = mutableListOf<Pair<Vec3I, Vec3I>>()

    var tVertices = mutableListOf<Vec3D>()

    var diffuseTexture: ImagePlus? = null

    /**
     * Function for model standartization. It converts vertices coords to `[-1;1]` range.
     */
    fun normalizeVertices() {
        var max = Vec3D(NEG_INF, NEG_INF, NEG_INF)
        var min = Vec3D(POS_INF, POS_INF, POS_INF)
        vertices.forEach {
            for (i in 0..2) {
                max[i] = max(it[i], max[i])
                min[i] = min(it[i], min[i])
            }
        }
        val middle = (max + min) / 2.0
        max -= middle
        min -= middle
        var maxScalar = NEG_INF
        for (i in 0..2) {
            maxScalar = max(abs(max[i]), maxScalar)
            maxScalar = max(abs(min[i]), maxScalar)
        }
        for (i in vertices.indices) {
            vertices[i] = (vertices[i] - middle) / maxScalar
        }
    }
}

fun parseObj(fileName: String): Model {
    val result = Model()
    val file = FileReader(File(fileName))
    file.readLines().asSequence().map { it.split(" ").filter { it.isNotEmpty() } }.filter { it.isNotEmpty() }.forEach {
        when (it[0]) {
            "v" -> result.vertices.add(Vec3D(it[1].toDouble(), it[2].toDouble(), it[3].toDouble()))
            "f" -> {
                val vertexIds = Vec3I(0, 0, 0)
                val textureVertexIds = Vec3I(0, 0, 0)
                for (i in 1..3) {
                    val ids = it[i].split("/").map { it.toInt() }
                    vertexIds[i - 1] = ids[0] - 1
                    textureVertexIds[i - 1] = ids[1] - 1
                }
                result.triangles.add(Pair(vertexIds, textureVertexIds))
            }
            "vt" -> {
                it.drop(1).map { it.toDouble() }.also {
                    result.tVertices.add(Vec3D(it[0], it[1], it[2]))
                }
            }
        }
    }
    return result
}
