package com.akar.tinyrenderer.util

import com.akar.tinyrenderer.math.Vec3D
import com.akar.tinyrenderer.math.Vec3I
import ij.IJ
import ij.ImagePlus
import java.awt.Color
import java.io.File
import java.io.FileReader
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.Double.Companion.NEGATIVE_INFINITY as NEG_INF
import kotlin.Double.Companion.POSITIVE_INFINITY as POS_INF

const val DEFAULT_NAME = "default"

class Model {
    val vertices = mutableListOf<Vec3D>()
    val vertexNormals = mutableListOf<Vec3D>()
    var triangles = mutableListOf<Face>()

    var tVertices = mutableListOf<Vec3D>()

    var objects = mutableMapOf(DEFAULT_NAME to ModelObject())

    var materials = mutableMapOf(DEFAULT_NAME to Material())

    /**
     * Function for model standardization. It converts vertices coords to `[-1;1]` range.
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

data class Face(val vertex: Vec3I, val tvertvex: Vec3I, val normal: Vec3I) {

}

class ModelObject {
    var triangles = mutableListOf<Face>()

    var material: String = DEFAULT_NAME

    override fun toString(): String {
        return material
    }
}

class Material {
    var mapKd: ImagePlus? = null
    var kd: Color? = null

    operator fun get(x: Double, y: Double): Int {
        if (mapKd != null) {
            return mapKd!!.processor[(mapKd!!.processor.width * x).toInt(), (mapKd!!.processor.height * (1.0 - y)).toInt()]
        }
        return kd!!.rgb
    }
}

fun parseObj(fileName: String): Model {
    val result = Model()
    val file = File(fileName)
    val reader = FileReader(file)
    var objName = DEFAULT_NAME
    reader.readLines().asSequence().map { it.split(" ").filter { it.isNotEmpty() } }.filter { it.isNotEmpty() }.forEach {
        when (it[0]) {
            "mtllib" -> result.materials.putAll(parseMtl(file.parent, it[1]))
            "o" -> {
                objName = it[1]
                result.objects[objName] = ModelObject()
            }
            "usemtl" -> result.objects[objName]?.material = it[1]
            "v" -> result.vertices.add(Vec3D(it[1].toDouble(), it[2].toDouble(), it[3].toDouble()))
            "vn" -> result.vertexNormals.add(Vec3D(it[1].toDouble(), it[2].toDouble(), it[3].toDouble()))
            "f" -> {
                val vertexIds = Vec3I(0, 0, 0)
                val textureVertexIds = Vec3I(0, 0, 0)
                val normalIds = Vec3I(0, 0, 0)
                for (i in 1..3) {
                    val ids = it[i].split("/").map {
                        if (it.isEmpty()) return@map Int.MIN_VALUE + 1
                        it.toInt()
                    }
                    vertexIds[i - 1] = ids[0] - 1
                    textureVertexIds[i - 1] = ids[1] - 1
                    normalIds[i - 1] = ids[2] - 1
                }

                result.objects[objName]?.triangles?.add(Face(vertexIds, textureVertexIds, normalIds))

                if (it.size == 5) {
                    val ids = it[4].split("/").map {
                        if (it.isEmpty()) return@map Int.MIN_VALUE + 1
                        it.toInt()
                    }
                    val additionalFurnace = Face(
                            Vec3I(vertexIds[0], vertexIds[2], ids[0] - 1),
                            Vec3I(textureVertexIds[0], textureVertexIds[2], ids[1] - 1),
                            Vec3I(normalIds[0], normalIds[2], ids[2] - 1))
                    result.objects[objName]?.triangles?.add(additionalFurnace)
                }
            }
            "vt" -> {
                it.drop(1).map { it.toDouble() }.also {
                    result.tVertices.add(Vec3D(it[0], it[1], if (it.size > 2) it[2] else 0.0))
                }
            }
        }
    }
    return result
}

fun parseMtl(parent: String, fileName: String): MutableMap<String, Material> {
    val result = mutableMapOf<String, Material>()
    val file = File("$parent/$fileName")
    val fileReader = FileReader(file)
    var matName = ""
    fileReader.readLines().asSequence().map { it.split(" ").filter { it.isNotEmpty() } }.filter { it.isNotEmpty() }.forEach {
        when (it[0]) {
            "newmtl" -> {
                matName = it[1]
                result[matName] = Material()
            }
            "map_Kd" -> {
                result[matName]?.mapKd = IJ.openImage("${file.parent}/${it[1]}")
            }
            "Kd" -> {
                val rgbf = it.drop(1).map { it.toFloat() }
                result[matName]?.kd = Color(rgbf[0], rgbf[1], rgbf[2])
            }
        }
    }
    return result
}
