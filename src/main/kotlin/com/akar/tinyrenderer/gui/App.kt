package com.akar.tinyrenderer.gui

import com.akar.tinyrenderer.render
import javafx.event.EventHandler
import javafx.scene.control.ListView
import javafx.scene.control.SelectionMode
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import tornadofx.*

const val bumpmappath1 = "obj/diablo3_pose/diablo3_pose_nm_tangent.png"
const val bumpmappath2 = "obj/african_head/african_head_nm_tangent.png"
val modelInfoList = listOf(
        SimpleModelForSelect("mech", "obj/mech/mech.obj", null, null, null),
        SimpleModelForSelect("bot", "obj/bot/OrangeBOT.obj", null, null, null),
        SimpleModelForSelect("trooper", "obj/trooper/0.obj", null, null, null),
        SimpleModelForSelect(
                "afro", "obj/african_head/african_head.obj", "obj/african_head/african_head_diffuse.png",
                bumpmappath2, "obj/african_head/african_head_spec.png"
        ),
        SimpleModelForSelect(
                "diablo", "obj/diablo3_pose/diablo3_pose.obj", "obj/diablo3_pose/diablo3_pose_diffuse.png",
                bumpmappath1, "obj/diablo3_pose/diablo3_pose_spec.png"
        ),
        SimpleModelForSelect("lego", "obj/lego/lego.obj", null, null, null)
)

fun main(args: Array<String>) {


    launch<MyApp>(args)
}


class MyApp : App(MyView::class)

class MyView : View() {
    private lateinit var modelRotateListView: ListView<Rotation>
    private lateinit var lightRotateListView: ListView<Rotation>
    private lateinit var myImageView: ImageView

    private lateinit var lightDirectionListView: ListView<Direction>
    private lateinit var modelDirectionListView: ListView<Direction>
    override val root = vbox {
        label("Renderer") {
            this.font = Font.font(25.0)
        }
        minHeight = 900.0
        minWidth = 1000.0
        val models = listview<SimpleModelForSelect> {
            items.addAll(modelInfoList)
            selectionModel.selectFirst()
            selectionModel.selectionMode = SelectionMode.SINGLE
        }
        models.maxHeight = 100.0

        hbox {

            vbox {
                minWidth = 400.0
                label("Вращение модели")
                modelRotateListView = rotateListView()
                modelDirectionListView = directionListView()
            }
            vbox {
                minWidth = 400.0
                label("Вращение света")
                lightRotateListView = rotateListView()
                lightDirectionListView = directionListView()


            }
        }
        button {
            label("Run render")
            onAction = EventHandler {
                println(models.selectedItem)
                render(
                        models.selectedItem!!,
                        myImageView,
                        modelRotateListView.selectedItem ?: Rotation.Y,
                        modelDirectionListView.selectedItem ?: Direction.COUNTER_CLOCKWISE,
                        lightRotateListView.selectedItem ?: Rotation.Y,
                        lightDirectionListView.selectedItem ?: Direction.NONE
                )
            }
        }
        myImageView = imageview()
        myImageView.prefHeight(300.0)

    }

    private fun VBox.directionListView(): ListView<Direction> =
            listview {
                items.addAll(Direction.entries.toTypedArray())
                selectionModel.selectFirst()
                selectionModel.selectionMode = SelectionMode.SINGLE
                prefHeight = 75.0
            }

    private fun VBox.rotateListView(): ListView<Rotation> =
            listview {
                items.addAll(Rotation.entries.toTypedArray())
                selectionModel.selectFirst()
                selectionModel.selectionMode = SelectionMode.SINGLE
                prefHeight = 75.0
            }
}

class Select : View() {
    override val root = listview<String> {

    }
}

data class SimpleModelForSelect(
        val name: String,
        val objpath: String,
        val diffmappath: String?,
        val bumpmappath: String?,
        val specmappath: String?
) {
    override fun toString(): String {
        return name
    }
}

enum class Rotation(private val label: String) {
    Y("Вокруг оси Y"), X("Вокруг оси X"), X_AND_Y("Вокруг осей X и Y");

    override fun toString() = label

}

enum class Direction(private val label: String) {
    COUNTER_CLOCKWISE("Против часовой стрелки"), CLOCKWISE("По часовой стрелке"), NONE("Не вращать");

    override fun toString() = label
}