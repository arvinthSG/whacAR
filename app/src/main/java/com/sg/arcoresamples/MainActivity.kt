package com.sg.arcoresamples

import android.graphics.ColorSpace
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Light
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.sg.arcoresamples.Configuration.Companion.ROW_NUM
import com.sg.arcoresamples.Configuration.Companion.COL_NUM
import com.sg.arcoresamples.Configuration.Companion.START_LIVES

class MainActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment

    private lateinit var scoreboard: ScoreboardView

    private var gameHandler = Handler()

    var cubeRenderable: ColorSpace.Model? = null

    private var droidRenderable: ModelRenderable? = null

    private var scoreboardRenderable: ViewRenderable? = null

    private var failLight: Light? = null

    private var grid = Array(ROW_NUM) { arrayOfNulls<TranslatableNode>(COL_NUM)}
    private var initialized = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment
        arFragment.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane,_: MotionEvent ->
            if(initialized){
                failHit()
                return@setOnTapArPlaneListener
            }
            if (plane.type != Plane.Type.HORIZONTAL_UPWARD_FACING){
                "Find an HORIZONTAL and UPWARD FACING plane!".toast(this)
                return@setOnTapArPlaneListener
            }

            if(droidRenderable == null || scoreboardRenderable == null || failLight == null){
                return@setOnTapArPlaneListener
            }

            val spacing = 0.3F

            val anchorNode = AnchorNode(hitResult.createAnchor())
            anchorNode.setParent(arFragment.arSceneView.scene)

            grid.matrixIndices { col, row ->
                val renderableModel = droidRenderable?.makeCopy() ?: return@matrixIndices
                TranslatableNode().apply {
                    setParent(anchorNode)
                    renderable = renderableModel
                    addOffset(x = row * spacing, z = col * spacing)
                    grid[col][row] = this
                    this.setOnTapListener{_,_ ->

                    }
                }
            }

            val renderableView = scoreboardRenderable?: return@setOnTapArPlaneListener
            TranslatableNode()
                    .also {
                        it.setParent(anchorNode)
                        it.renderable = renderableView
                        it.addOffset(x = spacing, y = .6F)
                    }
            Node().apply {
                setParent(anchorNode)
                light = failLight
                localPosition = Vector3(.3F, .3F, .3F)
            }

            initialized = true
        }

        initResource()
    }

    private fun initResource() {
        ModelRenderable.builder()
                .setSource(this, R.raw.andy)
                .build()
                .thenAccept { droidRenderable = it }
                .exceptionally { it.toast(this) }
        scoreboard = ScoreboardView(this)

        scoreboard.onStartTapped = {
            scoreboard.life = START_LIVES
            scoreboard.score = 0
        }

        ViewRenderable.builder()
                .setView(this, scoreboard)
                .build()
                .thenAccept {
                    it.isShadowReceiver = true
                    scoreboardRenderable = it
                }
                .exceptionally { it.toast(this) }

        failLight = Light.builder(Light.Type.POINT)
                .setColor(Color(android.graphics.Color.RED))
                .setShadowCastingEnabled(true)
                .setIntensity(0F)
                .build()
    }

    private fun failHit() {
        scoreboard.score -= 50
        scoreboard.life -= 1
        if (scoreboard.life <= 0) {
            // Game over
            gameHandler.removeCallbacksAndMessages(null)
        }
    }
}