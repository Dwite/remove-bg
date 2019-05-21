package com.theapache64.removebg.sample

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.esafirm.imagepicker.features.ImagePicker
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener
import com.theapache64.removebg.ErrorResponse
import com.theapache64.removebg.RemoveBg
import com.theapache64.twinkill.logger.info
import java.io.File
import java.lang.StringBuilder


class MainActivity : AppCompatActivity() {

    private val projectDir by lazy {
        val rootPath = Environment.getExternalStorageDirectory().absolutePath
        File("$rootPath/remove-bg")
    }
    private var image: File? = null

    @BindView(R.id.iv_input)
    lateinit var ivInput: ImageView

    @BindView(R.id.iv_output)
    lateinit var ivOutput: ImageView

    @BindView(R.id.tv_input_details)
    lateinit var tvInputDetails: TextView

    @BindView(R.id.b_process)
    lateinit var bProcess: View

    @BindView(R.id.tv_progress)
    lateinit var tvProgress: TextView

    @BindView(R.id.pb_progress)
    lateinit var pbProgress: ProgressBar

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

    private fun appendInputDetails(details: String) {
        tvInputDetails.text = "${tvInputDetails.text}\n$details"
    }

    private fun clearInputDetails() {
        tvInputDetails.text = ""
    }

    @OnClick(R.id.b_process)
    fun onProcessClicked() {
        if (image != null) {

            info("Image is ${image!!.path}")

            // Check permission
            checkPermission {

                info("Permission granted")

                // permission granted, compress the image now
                compressImage(image!!) { bitmap ->

                    info("Image compressed")

                    saveImage(bitmap) { compressedImage ->

                        info("Compressed image saved to ${compressedImage.absolutePath}, and removing bg...")
                        val compressedImageSize = compressedImage.length() / 1024
                        val originalImageSize = image!!.length() / 1024

                        pbProgress.visibility = View.VISIBLE
                        tvProgress.visibility = View.VISIBLE

                        tvProgress.setText(R.string.status_uploading)
                        pbProgress.progress = 0

                        val finalImage = if (compressedImageSize < originalImageSize) compressedImage else image!!
                        appendInputDetails("Compressed : ${finalImage.length() / 1024}KB")

                        // image saved, now upload
                        RemoveBg.from(finalImage, object : RemoveBg.RemoveBgCallback {

                            override fun onProcessing() {
                                tvProgress.setText(R.string.status_processing)
                            }

                            override fun onUploadProgress(progress: Float) {
                                tvProgress.text = "Uploading ${progress.toInt()}%"
                                pbProgress.progress = progress.toInt()
                            }

                            override fun onError(errors: List<ErrorResponse.Error>) {
                                runOnUiThread {
                                    val errorBuilder = StringBuilder()
                                    errors.forEach {
                                        errorBuilder.append("${it.title} : ${it.detail} : ${it.code}\n")
                                    }

                                    showErrorAlert(errorBuilder.toString())
                                    tvProgress.text = errorBuilder.toString()
                                    pbProgress.visibility = View.INVISIBLE
                                }
                            }

                            override fun onSuccess(bitmap: Bitmap) {
                                info("background removed from bg , and output is $bitmap")
                                runOnUiThread {
                                    ivOutput.setImageBitmap(bitmap)
                                    ivOutput.visibility = View.VISIBLE
                                    tvProgress.visibility = View.INVISIBLE
                                    pbProgress.visibility = View.INVISIBLE
                                }
                            }

                        })
                    }
                }
            }

        } else {
            toast(R.string.error_no_image_selected)
        }
    }

    /**
     * To show an alert message with title 'Error'
     */
    private fun showErrorAlert(message: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.title_error)
            .setMessage(message)
            .create()
            .show()
    }


    /**
     * To save given bitmap into a file
     */
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

    /**
     * To compress given image file with Glide
     */
    private fun compressImage(image: File, onLoaded: (bitmap: Bitmap) -> Unit) {

        Glide.with(this)
            .asBitmap()
            .load(image)
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadCleared(placeholder: Drawable?) {

                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    onLoaded(resource)
                }
            })
    }

    /**
     * To check WRITE_EXTERNAL_STORAGE permission
     */
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

            // IMAGE PICKED!!
            val imagePicked = ImagePicker.getFirstImageOrNull(data)

            if (imagePicked != null) {

                this.image = File(imagePicked.path)

                Glide.with(this)
                    .load(this.image)
                    .into(ivInput)

                // Showing process button
                bProcess.visibility = View.VISIBLE

                clearInputDetails()
                appendInputDetails("Image : ${image!!.name}")
                appendInputDetails("Original Size : ${image!!.length() / 1024}KB")
                ivOutput.visibility = View.INVISIBLE

            } else {
                toast(R.string.error_no_image_selected)
            }

            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun toast(@StringRes message: Int) {
        toast(getString(message))
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
