package com.akar.tinyrenderer.math

import java.util.*
import kotlin.math.abs
import kotlin.math.pow

class Matrix {
    private val eps = 0.0000001
    private var elements: Array<DoubleArray>
    private var q: Double
    private var n: Int
    private var m: Int

    constructor() {
        q = 1.0
        m = 1
        n = m
        elements = Array(n) { DoubleArray(m) }
        elements[0][0] = 0.0
    }

    constructor(n: Int, m: Int) {
        q = 1.0
        this.n = n
        this.m = m
        elements = Array(n) { DoubleArray(m) }
    }

    constructor(arr: Array<DoubleArray>) {
        q = 1.0
        n = arr.size
        m = arr[0].size
        elements = Array(n) { DoubleArray(m) }
        for (i in 0 until n) {
            System.arraycopy(arr[i], 0, elements[i], 0, m)
        }
    }


    constructor(b: Matrix) {
        q = b.q
        n = b.n
        m = b.m
        elements = Array(n) { DoubleArray(m) }
        for (i in 0 until n) {
            System.arraycopy(b.elements[i], 0, elements[i], 0, m)
        }
    }

    constructor(vector: Vec3D) {
        q = 1.0
        n = 3
        m = 1
        elements = Array(n) { row -> DoubleArray(m) { vector[row] } }
    }

    constructor(n: Int, vector: Vec3D) {
        q = 1.0
        this.n = n
        m = 1
        elements = Array(n) { row ->
            DoubleArray(m) {
                if (row > 2) {
                    return@DoubleArray 1.0
                }
                vector[row]
            }
        }
    }


    val isSquare: Boolean
        get() = n == m

    operator fun get(i: Int) = elements[i]

    override fun toString(): String {
        val f = Formatter()
        for (i in 0 until n) {
            for (j in 0 until m) {
                f.format("%6.2f", elements[i][j])
            }
            f.format("\n")
        }
        return f.toString()
    }

    fun range(): Int {
        val res = stairStep()
        var k = 0
        for (i in 0 until n) {
            if (res.checkZeroString(i)) k++
        }
        return n - k
    }

    fun stairStep(): Matrix {
        var shift = 0
        val result = Matrix(this)
        var flag: Boolean
        var i = 0
        while (i + shift < result.m) {
            flag = abs(result.elements[i][i + shift]) < eps
            while (flag) {
                for (j in i + 1 until result.n) {
                    if (abs(result.elements[j][i + shift]) > eps) {
                        result.swapString(i, j)
                        flag = false
                        break
                    }
                }
                if (flag) ++shift
                if (i + shift == result.m) return result
            }
            for (j in i + 1 until result.n) {
                val qs = result.elements[j][i + shift] / result.elements[i][i + shift]
                for (k in i + shift until result.m) {
                    result.elements[j][k] -= result.elements[i][k] * qs
                }
            }
            ++i
        }
        return result
    }

    fun determinant(): Double {
        if (!isSquare) throw Exception("Matrix is not square.")
        val a = stairStep()
        var result = 1.0
        for (i in 0 until a.n) {
            result *= a.elements[i][i]
        }
        result *= a.q
        return result
    }

    fun transpose(): Matrix {
        val result = Matrix(m, n)
        for (i in 0 until n) {
            for (j in 0 until m) {
                result.elements[j][i] = elements[i][j]
            }
        }
        return result
    }


    fun inverse(): Matrix {
        val det = determinant()
        val result = Matrix(n, m)
        val transposed = transpose()
        for (i in 0 until result.n) {
            for (j in 0 until result.m) {
                result.elements[i][j] = transposed.adj(i, j) * (-1.0).pow(i + j.toDouble()) / det
            }
        }
        return result
    }


    private fun adj(a: Int, b: Int): Double {
        val result = Matrix(n - 1, m - 1)
        var i = 0
        var i1 = 0
        while (i < result.n) {
            if (i1 == a) ++i1
            var j = 0
            var j1 = 0
            while (j < result.m) {
                if (j1 == b) ++j1
                result.elements[i][j] = elements[i1][j1]
                ++j
                ++j1
            }
            ++i
            ++i1
        }
        return result.determinant()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other::class.java != this.javaClass) return false
        val a = other as Matrix
        if (n != a.n || m != a.m) return false
        for (i in 0 until n) {
            for (j in 0 until m) {
                if (elements[i][j] != a.elements[i][j]) return false
            }
        }
        return true
    }


    operator fun times(b: Matrix): Matrix {
        if (m != b.n) throw Exception("Matrices size don't match for multiply")
        val result = Matrix(n, b.m)
        var s: Double
        for (i in 0 until result.n) {
            for (j in 0 until result.m) {
                s = 0.0
                for (k in 0 until m) {
                    s += elements[i][k] * b.elements[k][j]
                }
                result.elements[i][j] = s
            }
        }
        return result
    }

    operator fun times(n: Double): Matrix {
        val result = Matrix(this)
        for (i in 0 until result.n) {
            for (j in 0 until result.m) {
                result.elements[i][j] *= n
            }
        }
        return result
    }

    operator fun minus(b: Matrix): Matrix {
        if (n != b.n || m != b.m) throw Exception("Matrices sizes don't match.")
        val result = Matrix(n, m)
        for (i in 0 until result.n) {
            for (j in 0 until result.m) {
                result.elements[i][j] = elements[i][j] - b.elements[i][j]
            }
        }
        return result
    }

    operator fun plus(b: Matrix): Matrix {
        if (n != b.n || m != b.m) throw Exception("Matrices sizes don't match.")
        val result = Matrix(n, m)
        for (i in 0 until result.n) {
            for (j in 0 until result.m) {
                result.elements[i][j] = elements[i][j] + b.elements[i][j]
            }
        }
        return result
    }

    operator fun times(vector: Vec3D): Vec3D {
        if (n == 4) {
            val result = times(Matrix(4, vector))
            return Vec3D(result[0][0], result[1][0], result[2][0]) / result[3][0]
        }
        val result = times(Matrix(vector))
        return Vec3D(result[0][0], result[1][0], result[2][0])
    }

    private fun checkZeroString(a: Int): Boolean {
        for (i in 0 until m) {
            if (abs(elements[a][i]) > eps) return false
        }
        return true
    }

    private fun swapString(a: Int, b: Int) {
        if (a == b) return
        for (i in 0 until m) {
            val buf = elements[a][i]
            elements[a][i] = elements[b][i]
            elements[b][i] = buf
        }
        q *= -1.0
    }

    override fun hashCode(): Int {
        return elements.contentDeepHashCode()
    }
}