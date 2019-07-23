package com.wj577.choosepicture

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import com.wj577.selectpic.BasePhotoActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : BasePhotoActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvPic.setOnClickListener {
            if (checkPermission()) {
                showDialog()
            }
        }

    }
    override fun showDialog() {
        getPopView().showAtLocation(llAll, Gravity.BOTTOM, 0, 0)
    }

    override fun getHeadNet(file: File) {
        tvContent.text = file!!.absolutePath

    }
}
