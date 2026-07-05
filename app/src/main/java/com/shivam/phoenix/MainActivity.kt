package com.shivam.phoenix

import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.media.MediaPlayer
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.core.view.children
import com.google.ar.core.*
import io.github.sceneview.ar.ARSceneView
import dev.romainguy.kotlin.math.Float4
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.ModelNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.LightNode
import com.google.android.filament.EntityManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.sceneview.math.Scale
import androidx.core.view.isVisible

class MainActivity : AppCompatActivity() {
    private lateinit var arSceneView: ARSceneView
    private lateinit var modelLoader: ModelLoader
    private lateinit var modelNode: ModelNode
    private lateinit var anchorNode: AnchorNode
    private var mediaPlayer: MediaPlayer? = null
    private var angleStep = 0.01f
    private var radius = 6.2f
    private var camOffsetX = 6f
    private var camOffsetZ = 0f
    private var height =0.5f
    private var model =true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar)
        setupScene()
        updateToolbar()
        configureARSession()
        setupOnUpdate()
        setUpButton()
    }
    private fun setUpButton() {
        val btnSpeedPlus = findViewById<Button>(
            R.id.btnSpeedPlus)
        btnSpeedPlus.setOnClickListener {
            angleStep += 0.01f
        }

        val btnSpeedMinus = findViewById<Button>(
            R.id.btnSpeedMinus)
        btnSpeedMinus.setOnClickListener {
            angleStep = (angleStep - 0.01f).coerceAtLeast(0.001f)
        }
        val btnHeightPlus = findViewById<Button>(
            R.id.btnHeightPlus)
        btnHeightPlus.setOnClickListener {
            height += 0.1f
        }

        val btnHeightMinus = findViewById<Button>(
            R.id.btnHeightMinus)
        btnHeightMinus.setOnClickListener {
            height = (height - 0.1f).coerceAtLeast(0.01f)
        }
        val btnRadiusPlus = findViewById<Button>(
            R.id.btnRadiusPlus)
        btnRadiusPlus.setOnClickListener {
            radius += 0.1f
        }

        val btnRadiusMinus = findViewById<Button>(
            R.id.btnRadiusMinus)
        btnRadiusMinus.setOnClickListener {
            radius = (radius - 0.1f).coerceAtLeast(0.5f)
        }
        val btnZPlus = findViewById<Button>(
            R.id.btnZPlus)
        btnZPlus.setOnClickListener {
            camOffsetZ += 1f
        }

        val btnZMinus = findViewById<Button>(
            R.id.btnZMinus)
        btnZMinus.setOnClickListener {
            camOffsetZ -= 1f
        }
        val btnXPlus = findViewById<Button>(
            R.id.btnXPlus)
        btnXPlus.setOnClickListener {
            camOffsetX += 1f
        }

        val btnXMinus = findViewById<Button>(
            R.id.btnXMinus)
        btnXMinus.setOnClickListener {
            camOffsetX -= 1f
        }
        val captureBtn = findViewById<FloatingActionButton>(
            R.id.cameraIcon)
        captureBtn.setOnClickListener {
            captureSceneView { bitmap ->
                bitmap?.let { saveImageToGallery(it) }
            }
        }
        val settingBtn = findViewById<FloatingActionButton>(
            R.id.settings)
        settingBtn.setOnClickListener {
            updateToolbar()
        }
    }

    private fun updateToolbar() {
        val toolbar = findViewById<LinearLayout>(
            R.id.toolbar
        )
        val captureBtn = findViewById<FloatingActionButton>(
            R.id.cameraIcon)
        if (toolbar.isVisible) {
            toolbar.visibility = LinearLayout.GONE
            captureBtn.visibility = ImageButton.VISIBLE
            toolbar.children.apply { forEach { it.visibility = LinearLayout.GONE } }
        } else {
            toolbar.visibility = LinearLayout.VISIBLE
            captureBtn.visibility = ImageButton.GONE
            toolbar.children.apply { forEach { it.visibility = LinearLayout.VISIBLE } }
        }
    }

    private fun configureARSession() {
        arSceneView.configureSession { _ , config ->

            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        }
    }

    private fun setupOnUpdate() {
        model = true;
        var angle = 0f
        val cam = arSceneView.cameraNode.position
        val center = Position(cam.x , cam.y, cam.z - 1.5f)

        arSceneView.onFrame = { frameTime ->
            angle += angleStep

            val x = center.x + radius * kotlin.math.cos(angle) + camOffsetX
            val z = center.z + radius * kotlin.math.sin(angle) + camOffsetZ
            val y = center.y + 0.2f * kotlin.math.sin(angle * 2f) + height

            modelNode.position = Position(x, y, z)

// tangent direction
            val dirX = -kotlin.math.sin(angle)
            val dirZ =  kotlin.math.cos(angle)

            val yawRad = kotlin.math.atan2(dirX, dirZ)
            val yawDeg = Math.toDegrees(yawRad.toDouble()).toFloat()

// combine rotations
            modelNode.rotation = Rotation(
                0f,
                yawDeg,              // 🟢 face flight direction
                10f * kotlin.math.sin(angle)  // 🔵 banking tilt
            )

            //  modelNode.lookAt(lookTarget)
        }
        arSceneView.onSessionUpdated = { session, frame ->
            // 1️⃣ Get updated planes
            val planes = frame.getUpdatedTrackables(Plane::class.java)

            // 2️⃣ Find a tracking plane
            val plane = planes.firstOrNull {
                it.trackingState == TrackingState.TRACKING &&
                        it.type == Plane.Type.HORIZONTAL_UPWARD_FACING
            }
            if(model && plane != null) {  // 3️⃣ Create anchor at plane center
                val anchor = plane.createAnchor(plane.centerPose)
                anchorNode = AnchorNode(
                    engine = arSceneView.engine,
                    anchor = anchor
                )
                // 4️⃣ Attach your node

                modelNode.scale= Scale(2f,2f,2f)
                modelNode.playAnimation(0, loop = true)
                anchorNode.addChildNode(modelNode)
                // anchorNode.addChildNode(modelNode2)
                arSceneView.addChildNode(anchorNode)
                playMusicFor();
                model=false;
            }
        }
    }

    private fun setupScene() {
        arSceneView = findViewById<ARSceneView>(R.id.arSceneView)
        val entity: Int = EntityManager.get().create()
        val mainLight = object : LightNode(arSceneView.engine, entity) {
            init {
                intensity = 1500000f
                color = Float4(1.0f, 1.0f, 1.0f, 1.0f)
                position = io.github.sceneview.math.Position(0f, 1f, 3f)
            }
        }
        arSceneView.addChildNode(mainLight)
        arSceneView.planeRenderer.isVisible = true
        modelLoader = ModelLoader(arSceneView.engine, this)
        val modelInstance =
            modelLoader.createModelInstance("phoenix.glb")

        modelNode = ModelNode(
            modelInstance = modelInstance,
            scaleToUnits = 0.1f,
        )
        mediaPlayer = MediaPlayer()
    }
    private fun captureSceneView(callback: (Bitmap?) -> Unit) {
        val view = arSceneView

        val bitmap = createBitmap(view.width, view.height)

        val handler = android.os.Handler(mainLooper)

        android.view.PixelCopy.request(
            view,
            bitmap,
            { copyResult ->
                if (copyResult == android.view.PixelCopy.SUCCESS) {
                    callback(bitmap)
                } else {
                    callback(null)
                }
            },
            handler
        )
    }
    override fun onDestroy() {
        stopMusic();
        Log.e("ARActivity", "DESTROYED!")
        super.onDestroy()
        mediaPlayer?.release()
        arSceneView.destroy()
    }

    private fun playMusicFor() {
        stopMusic()

        try {
            val afd = assets.openFd("phoenix.wav")
            mediaPlayer?.apply {
                reset()
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                isLooping = true
                prepare()
                start()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Music play error: ${e.message}")
        }
    }


    private fun stopMusic() {
        mediaPlayer?.apply {
            stop()
            reset()
        }
    }
    private fun saveImageToGallery(bitmap: Bitmap) {
        val filename = "Phoenix_${System.currentTimeMillis()}.jpg"

        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Phoenix")
        }

        val uri = contentResolver.insert(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )

        uri?.let {
            contentResolver.openOutputStream(it)?.use { outStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
            }
        }

        Toast.makeText(this, "Photo saved to Gallery!", Toast.LENGTH_SHORT).show()
    }

}
