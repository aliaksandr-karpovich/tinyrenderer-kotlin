package com.akar.tinyrenderer.math

import kotlin.math.sqrt

typealias Vec3I = Vector3<Int>
typealias Vec3D = Vector3<Double>
typealias Vec3F = Vector3<Float>
typealias Vec4D = Vector4<Double>

open class Vector3<T : Number>(var x: T, var y: T, var z: T) {
    open operator fun get(i: Int): T =
            when (i) {
                0 -> x
                1 -> y
                2 -> z
                else -> throw IllegalArgumentException("must be in 0..2")
            }

    open operator fun set(i: Int, value: T) {
        when (i) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            else -> throw IllegalArgumentException("must be in 0..2")
        }
    }

    fun toInt() = Vec3I(x.toInt(), y.toInt(), z.toInt())

    fun normalize(): Vec3D {
        val length = length()
        return Vec3D(x.toDouble() / length, y.toDouble() / length, z.toDouble() / length)
    }

    fun cross(other: Vector3<out Number>) =
            Vec3D(y.toDouble() * other.z.toDouble() - z.toDouble() * other.y.toDouble(),
                    z.toDouble() * other.x.toDouble() - x.toDouble() * other.z.toDouble(),
                    x.toDouble() * other.y.toDouble() - y.toDouble() * other.x.toDouble())

    fun dot(other: Vector3<out Number>) =
            x.toDouble() * other.x.toDouble() + y.toDouble() * other.y.toDouble() + z.toDouble() * other.z.toDouble()

    fun length(): Double {
        return sqrt(x.toDouble() * x.toDouble() + y.toDouble() * y.toDouble() + z.toDouble() * z.toDouble())
    }

    operator fun times(scalar: Double) = Vec3D(x.toDouble() * scalar, y.toDouble() * scalar, z.toDouble() * scalar)

    operator fun times(other: Vector3<out Number>) = dot(other)

    operator fun Double.times(vector: Vector3<out Number>): Vec3D = vector * this

    operator fun div(scalar: Double) = Vec3D(x.toDouble() / scalar, y.toDouble() / scalar, z.toDouble() / scalar)

    operator fun plus(other: Vector3<out Number>) =
            Vec3D(x.toDouble() + other.x.toDouble(),
                    y.toDouble() + other.y.toDouble(),
                    z.toDouble() + other.z.toDouble())

    operator fun minus(other: Vector3<out Number>) =
            Vec3D(x.toDouble() - other.x.toDouble(),
                    y.toDouble() - other.y.toDouble(),
                    z.toDouble() - other.z.toDouble())

    operator fun unaryMinus() = this * -1.0

    override fun toString(): String {
        return "[$x, $y, $z]"
    }

    fun toDoubleArray() = doubleArrayOf(x.toDouble(),y.toDouble(),z.toDouble())
}

class Vector4<T : Number>(x: T, y: T, z: T, var w: T) : Vector3<T>(x, y, z) {

    constructor(vec3: Vector3<T>, a : T): this(vec3.x, vec3.y, vec3.z, a)

    fun toVec3D(includeW: Boolean = true): Vec3D {
        if (includeW) return Vec3D(x.toDouble() / w.toDouble(), y.toDouble() / w.toDouble(), z.toDouble() / w.toDouble())
        return Vec3D(x.toDouble(), y.toDouble(), z.toDouble())
    }

    override operator fun get(i: Int): T =
            when (i) {
                in 0..2 -> super.get(i)
                3 -> w
                else -> throw IllegalArgumentException("must be in 0..3")
            }

    override operator fun set(i: Int, value: T) {
        when (i) {
            in 0..2 -> super.set(i, value)
            3 -> w = value
            else -> throw IllegalArgumentException("must be in 0..3")
        }
    }
}