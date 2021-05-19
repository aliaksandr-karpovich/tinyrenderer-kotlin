package com.akar.tinyrenderer.gui

import javafx.scene.layout.HBox
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.input.MouseEvent

class ToggleSwitch : HBox() {
    private val label = Label()
    private val button = Button()
    private val switchedOn = SimpleBooleanProperty(false)
    fun switchOnProperty(): SimpleBooleanProperty {
        return switchedOn
    }

    private fun init() {
        label.text = Direction.CLOCKWISE.toString()
        children.addAll(label, button)
        button.onAction = EventHandler { e: ActionEvent? -> switchedOn.set(!switchedOn.get()) }
        label.onMouseClicked = EventHandler { e: MouseEvent? -> switchedOn.set(!switchedOn.get()) }
        setStyle()
        bindProperties()
    }

    private fun setStyle() {
        //Default Width
        width = 80.0
        label.alignment = Pos.CENTER
        style = "-fx-background-color: grey; -fx-text-fill:black; -fx-background-radius: 4;"
        alignment = Pos.CENTER_LEFT
    }

    private fun bindProperties() {
        label.prefWidthProperty().bind(widthProperty().divide(2))
        label.prefHeightProperty().bind(heightProperty())
        button.prefWidthProperty().bind(widthProperty().divide(2))
        button.prefHeightProperty().bind(heightProperty())
    }

    init {
        init()
        switchedOn.addListener { _: ObservableValue<out Boolean>?, _: Boolean?, c: Boolean ->
            if (c) {
                label.text = Direction.CLOCKWISE.toString()
//                style = "-fx-background-color: green;"
                label.toFront()
            } else {
                label.text = Direction.COUNTER_CLOCKWISE.toString()
//                style = "-fx-background-color: grey;"
                button.toFront()
            }
        }
    }
}