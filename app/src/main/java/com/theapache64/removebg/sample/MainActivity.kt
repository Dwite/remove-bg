package com.theapache64.removebg.sample

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.bumptech.glide.Glide
import com.esafirm.imagepicker.features.ImagePicker
import com.esafirm.imagepicker.model.Image
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener
import com.theapache64.twinkill.logger.info
import java.io.File


class MainActivity : AppCompatActivity() {

    private var image: Image? = null

    @BindView(R.id.iv_input)
    lateinit var ivInput: ImageView


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
        // Check permission
        checkPermission()
    }

    fun checkPermission() {

        val dialogListener = DialogOnAnyDeniedMultiplePermissionsListener.Builder.withContext(this)
            .withTitle(R.string.title_permission)

        Dexter.withActivity(this)
            .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)


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
