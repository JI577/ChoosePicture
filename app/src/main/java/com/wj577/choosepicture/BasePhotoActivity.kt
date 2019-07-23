package com.wj577.selectpic

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import com.wj577.choosepicture.R
import com.wj577.choosepicture.SelectHeadPopupwindow
import java.io.*

import java.text.SimpleDateFormat
import java.util.*

open abstract class BasePhotoActivity : FragmentActivity() {

    private val REQUESTCODE_PICK = 0x121        // 相册选图标记
    val RESULT_COD = 102

    var headDialog: SelectHeadPopupwindow? = null
    private val mFile_Pic: File? = null// 存储图片的文件
    private val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private var dialog: AlertDialog? = null
    private var mBitmap: Bitmap? = null


    fun getPopView(): SelectHeadPopupwindow {
        if (null == headDialog) {
            headPic()
        }
            return headDialog!!
    }

    fun checkPermission(): Boolean {
        // 版本判断。当手机系统大于 23 时，才有必要去判断权限是否获取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                // 检查该权限是否已经获取
                val i = ContextCompat.checkSelfPermission(this, permission)
                // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
                if (i != PackageManager.PERMISSION_GRANTED) {
                    // 如果没有授予该权限，就去提示用户请求
                    showDialogTipUserRequestPermission()

                    return false
                }
            }
        }
        return true
    }

    fun showDialogTipUserRequestPermission() {
        AlertDialog.Builder(this)
                .setTitle("存储权限不可用")
                .setMessage("由于选取照片和拍照需要手机的部分权限，否则您将不能使用该应用的全部功能")
                .setPositiveButton("立即开启") { dialog, which -> startRequestPermission() }
                .setNegativeButton("取消") { dialog, which -> finish() }.setCancelable(false).show()
    }

    fun startRequestPermission() {
        ActivityCompat.requestPermissions(this, permissions, 321)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 321) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // 判断用户是否 点击了不再提醒。(检测该权限是否还可以申请)
                    val b = shouldShowRequestPermissionRationale(permissions[0])
                    if (!b) {
                        // 用户还是想用我的 APP 的
                        // 提示用户去应用设置界面手动开启权限
                        showDialogTipUserGoToAppSettting()
                    } else
                        finish()
                } else {

//                    headDialog!!.showAtLocation(llAll, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 0)
                    showDialog()
                }
            }
        }
    }

    fun showDialogTipUserGoToAppSettting() {
        dialog = AlertDialog.Builder(this)
                .setTitle("存储权限不可用")
                .setMessage("请在-应用设置-权限-中，允许本应用使用相机的相应权限来启用拍照等功能")
                .setPositiveButton("立即开启") { dialog, which ->
                    // 跳转到应用设置界面
                    goToAppSetting()
                }
                .setNegativeButton("取消") { dialog, which -> finish() }.setCancelable(false).show()
    }

    fun headPic() {
        headDialog = SelectHeadPopupwindow(this, View.OnClickListener { v ->
            when (v.id) {
                //拍照
                R.id.btn_take_photo -> getPicCamera()
                //相册
                R.id.btn_pick_photo -> selectPicture()
            }
            headDialog!!.dismiss()
        }, SelectHeadPopupwindow.PHOTO)

    }

    private var mPictureFile: String? = null
    private var filePath: String? = null
    private val OPEN_RESULT2 = 0x124 // 打开相机2

    fun getPicCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        // 取当前时间为照片名
        mPictureFile = DateFormat.format("yyyyMMdd_hhmmss",
                Calendar.getInstance(Locale.CHINA)).toString() + ".jpg"
        //        Log.d("onactivity", "mPictureFile：" + mPictureFile);
        filePath = getPhotoPath() + mPictureFile!!
        //        mFile_Pics[position] = getPhotoPath() + mPictureFile;
        //        // 通过文件创建一个uri中
        //        Uri imageUri = Uri.fromFile(new File(filePath));
        //        // 保存uri对应的照片于指定路径
        //        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        //        startActivityForResult(intent, OPEN_RESULT2);

        /*获取当前系统的android版本号*/
        val currentapiVersion = android.os.Build.VERSION.SDK_INT
        Log.e("jrq", "currentapiVersion====>$currentapiVersion")
        if (currentapiVersion < 24) { // 保存uri对应的照片于指定路径
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(File(filePath!!)))
            startActivityForResult(intent, OPEN_RESULT2)
        } else {
            val contentValues = ContentValues(1)
            contentValues.put(MediaStore.Images.Media.DATA, filePath)
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            startActivityForResult(intent, OPEN_RESULT2)
        }
    }

    /**
     * 获得照片路径
     *
     * @return
     */
    fun getPhotoPath(): String {
        return Environment.getExternalStorageDirectory().toString() + "/DCIM/"
    }

    fun goToAppSetting() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivityForResult(intent, 123)
    }

    /**
     * 选择图片
     */
    fun selectPicture() {
        val intent = Intent()
        intent.action = Intent.ACTION_PICK//Pick an item from the data
        intent.type = "image/*"//从所有图片中进行选择
        startActivityForResult(intent, REQUESTCODE_PICK)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RESULT_COD) {//裁剪
//            if (data != null) {
//                setPicParam(data)
//            }
            setPicParam()
        }

        /**修改相机 */
        if (requestCode == OPEN_RESULT2) {
            if (resultCode == Activity.RESULT_OK) {
                var uri: Uri? = null
                if (Build.VERSION.SDK_INT >= 24) {
                    uri = FileProvider.getUriForFile(this, packageName + ".FileProvider", File(filePath))
                } else {
                    uri = Uri.fromFile(File(filePath))
                }
                startPhotoZoom(uri)
            }
        }

        if (resultCode == Activity.RESULT_OK)
            when (requestCode) {
                REQUESTCODE_PICK//相册
                -> startPhotoZoom(data!!.data)
            }

    }
    var uritempFile :Uri?=null

    private fun startPhotoZoom(uri: Uri) {
        val intent = Intent("com.android.camera.action.CROP")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        intent.setDataAndType(uri, "image/*")
        //是否可裁剪
        intent.putExtra("corp", "true")
        //裁剪器高宽比
        intent.putExtra("aspectY", 1)
        intent.putExtra("aspectX", 1)
        //设置裁剪框高宽
        intent.putExtra("outputX", 300)
        intent.putExtra("outputY", 300)
        //返回数据
//        intent.putExtra("return-data", true)

         uritempFile = Uri.parse("file://" + "/" + Environment.getExternalStorageDirectory().getPath() + "/" + "small.jpg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uritempFile);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());


        startActivityForResult(intent, RESULT_COD)
    }

    private fun setPicParam(data: Intent) {
        val bundle = data.extras
        if (bundle != null) {
            mBitmap = bundle.getParcelable<Bitmap>("data")

            Log.e("jrq","---mBitmap-------"+mBitmap)
            val file = compressImage(mBitmap!!)
            if (file.exists()) {
                getHeadNet(file)
            }
        }

    }

    private fun setPicParam() {
              mBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uritempFile));
            Log.e("jrq","---mBitmap-------"+mBitmap)
            val file = compressImage(mBitmap!!)
            if (file.exists()) {
                getHeadNet(file)
            }

    }

    fun compressImage(bitmap: Bitmap): File {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        var options = 100
        while (baos.toByteArray().size / 1024 > 500) {  //循环判断如果压缩后图片是否大于500kb,大于继续压缩
            baos.reset()//重置baos即清空baos
            options -= 10//每次都减少10
            bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos)//这里压缩options%，把压缩后的数据存放到baos中
            val length = baos.toByteArray().size.toLong()
        }
        val format = SimpleDateFormat("yyyyMMddHHmmss")
        val date = Date(System.currentTimeMillis())
        val filename = format.format(date)
        val file = File(Environment.getExternalStorageDirectory(), "$filename.png")
        try {
            val fos = FileOutputStream(file)
            try {
                fos.write(baos.toByteArray())
                fos.flush()
                fos.close()
            } catch (e: IOException) {

                e.printStackTrace()
            }

        } catch (e: FileNotFoundException) {
            Log.e("jrq", e.message)
            e.printStackTrace()
        }

        recycleBitmap(bitmap)
        return file
    }

    fun recycleBitmap(vararg bitmaps: Bitmap) {
        if (bitmaps == null) {
            return
        }
        for (bm in bitmaps) {
            if (null != bm && !bm.isRecycled) {
                bm.recycle()
            }
        }
    }

    abstract fun showDialog()
    abstract fun getHeadNet(file: File)
}
