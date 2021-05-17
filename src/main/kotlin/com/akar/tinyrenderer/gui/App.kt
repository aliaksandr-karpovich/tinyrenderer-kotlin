package com.akar.tinyrenderer.gui

import com.akar.tinyrenderer.render
import javafx.event.EventHandler
import javafx.scene.control.SelectionMode
import tornadofx.*

const val bumpmappath1 =  "obj/diablo3_pose/diablo3_pose_nm_tangent.png"
const val bumpmappath2 =  "obj/african_head/african_head_nm_tangent.png"
val modelInfoList = listOf(
    SimpleModelForSelect("mech", "obj/mech/mech.obj", null, null, null),
    SimpleModelForSelect("bot", "obj/bot/OrangeBOT.obj", null, null, null),
    SimpleModelForSelect("trooper", "obj/trooper/0.obj", null, null, null),
    SimpleModelForSelect("afro", "obj/african_head/african_head.obj", "obj/african_head/african_head_diffuse.png",
        bumpmappath2,"obj/african_head/african_head_spec.png"),
    SimpleModelForSelect("diablo", "obj/diablo3_pose/diablo3_pose.obj", "obj/diablo3_pose/diablo3_pose_diffuse.png",
        bumpmappath1,"obj/diablo3_pose/diablo3_pose_spec.png"),
    SimpleModelForSelect("lego", "obj/lego/lego.obj", null, null, null)
)
fun main(args: Array<String>) {


    launch<MyApp>(args)
}


class MyApp : App(MyView::class)

class MyView : View() {
    override val root = vbox {
        minHeight = 700.0
        minWidth = 1000.0
        val models = listview<SimpleModelForSelect> {
            items.addAll(modelInfoList)
            selectionModel.selectFirst()
            selectionModel.selectionMode = SelectionMode.SINGLE
        }
        models.maxHeight = 100.0
        val myImageView = imageview()
        myImageView.prefHeight(300.0)
        button {
            label("Run render")
            onAction = EventHandler  {
                println(models.selectedItem)
                render(models.selectedItem!!, myImageView)
            }
        }
    }
}

class Select : View() {
    override val root = listview<String> {

    }
}

data class SimpleModelForSelect(val name: String, val objpath: String, val diffmappath: String?, val bumpmappath: String?, val specmappath: String?) {
    override fun toString(): String {
        return name
    }
}