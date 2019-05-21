package com.theapache64.removebg.sample

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.esafirm.imagepicker.features.ImagePicker
import com.esafirm.imagepicker.model.Image
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener
import com.theapache64.twinkill.logger.info
import java.io.File


class MainActivity : AppCompatActivity() {

    private val projectDir by lazy {
        val rootPath = Environment.getExternalStorageDirectory().absolutePath
        File("$rootPath/remove-bg")
    }
    private var image: Image? = null

    @BindView(R.id.iv_input)
    lateinit var ivInput: ImageView

    @BindView(R.id.iv_output)
    lateinit var ivOutput: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

    }

    @OnClick(R.id.b_choose_image, R.id.i_choose_image)
    fun onChooseImageClicked() {

        info("Choose image clicked")

        ImagePicker.create(this)
            .single()
            .start()
    }

    @OnClick(R.id.b_process)
    fun onProcessClicked() {
        if (image != null) {

            info("Image is ${image!!.path}")

            // Check permission
            checkPermission {

                info("Permission granted")

                // permission granted, compress the image now
                compressImage { bitmap ->

                    info("Image compressed")

                    saveImage(bitmap) { file ->

                        info("Compressed image saved to ${file.absolutePath}, and removing bg...")

                        // image saved, now upload
                        removeBgFromImage(file) { output ->

                            info("background removed from bg , and output is $output")
                            runOnUiThread {
                                ivOutput.setImageBitmap(output)
                            }
                        }
                    }
                }
            }

        } else {
            toast(R.string.error_no_image_selected)
        }
    }

    private fun removeBgFromImage(file: File, onRemoved: (output: Bitmap) -> Unit) {
        // Do Api Call here
        RemoveBg.from(file, onRemoved)
    }

    private fun saveImage(bitmap: Bitmap, onSaved: (file: File) -> Unit) {

        // Create project dir
        if (!projectDir.exists()) {
            projectDir.mkdir()
        }

        // Create image file
        val imageFile = File("$projectDir/${System.currentTimeMillis()}.jpg")
        imageFile.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
        }

        onSaved(imageFile)
    }

    private fun compressImage(onLoaded: (bitmap: Bitmap) -> Unit) {

        Glide.with(this)
            .asBitmap()
            .load(File(image!!.path))
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadCleared(placeholder: Drawable?) {

                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    onLoaded(resource)
                }
            })
    }

    private fun checkPermission(onPermissionChecked: () -> Unit) {

        val deniedListener = DialogOnDeniedPermissionListener.Builder.withContext(this)
            .withTitle(R.string.title_permission)
            .withMessage(R.string.message_permission)
            .withButtonText(R.string.action_ok)
            .build()

        val permissionListener = object : BasePermissionListener() {
            override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                onPermissionChecked()
            }

            override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                toast(R.string.error_permission)
            }
        }

        val listener = CompositePermissionListener(permissionListener, deniedListener)

        Dexter.withActivity(this)
            .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(listener)
            .check()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
            val image = ImagePicker.getFirstImageOrNull(data)

            if (image != null) {

                this.image = image

                Glide.with(this)
                    .load(File(image.path))
                    .into(ivInput)

            } else {
                toast(R.string.error_no_image_selected)
            }

            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun toast(@StringRes message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
