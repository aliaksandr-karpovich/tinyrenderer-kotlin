package com.akar.tinyrenderer.math

import kotlin.math.sqrt

typealias Vec3I = Vector3<Int>
typealias Vec3D = Vector3<Double>

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

    fun toInt() = Vector3(x.toInt(), y.toInt(), z.toInt())

    fun normalize(): Vec3D {
        val length = length()
        return Vec3D(x.toDouble() / length, y.toDouble() / length, z.toDouble() / length)
    }

    fun cross(other: Vector3<out Number>) =
            Vec3D(y.toDouble() * other.z.toDouble() - z.toDouble() * other.y.toDouble(),
                    z.toDouble() * other.x.toDouble() - x.toDouble() * other.z.toDouble(),
                    x.toDouble() * other.y.toDouble() - y.toDouble() * other.x.toDouble())

    fun scalar(other: Vector3<out Number>) =
            x.toDouble() * other.x.toDouble() + y.toDouble() * other.y.toDouble() + z.toDouble() * other.z.toDouble()


    fun length(): Double {
        return sqrt(x.toDouble() * x.toDouble() + y.toDouble() * y.toDouble() + z.toDouble() * z.toDouble())
    }

    operator fun times(scalar: Double) = Vec3D(x.toDouble() * scalar, y.toDouble() * scalar, z.toDouble() * scalar)

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

    override fun toString(): String {
        return "[$x, $y, $z]"
    }
}