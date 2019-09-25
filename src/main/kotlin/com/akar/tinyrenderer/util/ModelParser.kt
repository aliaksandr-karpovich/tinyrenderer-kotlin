package com.akar.tinyrenderer.util

import com.akar.tinyrenderer.math.Vec3D
import com.akar.tinyrenderer.math.Vec3I
import ij.IJ
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

    var objects = mutableMapOf<String, ModelObject>()

    var materials = mutableMapOf<String, Material>()

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

class ModelObject {
    var triangles = mutableListOf<Pair<Vec3I, Vec3I>>()


    var diffuseTexture: ImagePlus? = null
    var material: String = ""

    override fun toString(): String {
        return material
    }
}

class Material {
    var mapKd: ImagePlus? = null

}

fun parseObj(fileName: String): Model {
    val result = Model()
    val file = File(fileName)
    val reader = FileReader(file)
    var objName = ""
    reader.readLines().asSequence().map { it.split(" ").filter { it.isNotEmpty() } }.filter { it.isNotEmpty() }.forEach {
        when (it[0]) {
            "mtllib" -> result.materials.putAll(parseMtl(file.parent, it[1]))
            "o" -> {
                objName = it[1]
                result.objects[objName] = ModelObject()
            }
            "usemtl" -> result.objects[objName]?.material = it[1]
            "v" -> result.vertices.add(Vec3D(it[1].toDouble(), it[2].toDouble(), it[3].toDouble()))
            "f" -> {

                val vertexIds = Vec3I(0, 0, 0)
                val textureVertexIds = Vec3I(0, 0, 0)
                for (i in 1..3) {
                    val ids = it[i].split("/").map { it.toInt() }
                    vertexIds[i - 1] = ids[0] - 1
                    textureVertexIds[i - 1] = ids[1] - 1
                }
                result.triangles.add(vertexIds to textureVertexIds)
                result.objects[objName]?.triangles?.add(vertexIds to textureVertexIds)

                if (it.size == 5) {
                    val ids = it[4].split("/").map { it.toInt() }
                    val additionalFurnace = Vec3I(vertexIds[0], vertexIds[2], ids[0] - 1) to
                            Vec3I(textureVertexIds[0], textureVertexIds[2], ids[1] - 1)
                    result.triangles.add(additionalFurnace)
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
        }
    }
    return result
}
