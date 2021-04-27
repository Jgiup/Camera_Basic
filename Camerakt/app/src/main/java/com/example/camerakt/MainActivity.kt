package com.example.camerakt

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.Image
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.camerakt.databinding.ActivityMainBinding
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    val REQUEST_IMAGE_CAPTURE = 1 // 카메라 사진 촬영 요청코드
    lateinit var curPhotoPath: String // 문자열 형태의 사진경로 값 ( 초기값을 null로 지정 )
    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setPermission() // 권한을 체크하는 메소드 수행.

        binding.btnCamera.setOnClickListener {
            takeCapture() // 기본 카메라 앱을 사용하여 사진촬영.
        }


    }

    private fun takeCapture() {
        // 기본카메라앱 실행
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?. also {
                val photoFile: File? = try {
                    createImageFile()
                }catch ( ex: IOException ){
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                            this,
                            "com.example.camerakt.fileprovider",
                            it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    private fun createImageFile(): File? {
        val timestamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPRG_${timestamp}_",".jpg" ,storageDir)
                .apply { curPhotoPath = absolutePath}
    }

    private fun setPermission() {
        val permission = object : PermissionListener {
            override fun onPermissionGranted() { //위험 권한들이 허용 되었을 경우 수행
                Toast.makeText(this@MainActivity,"권한이 허용되었어용.", Toast.LENGTH_SHORT).show()
            }

            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) { // 위험 권한들 중 거부를 한 경우 수행
                Toast.makeText(this@MainActivity,"권한이 거부되었어용.", Toast.LENGTH_SHORT).show()
            }

        }

        TedPermission.with(this)
                .setPermissionListener(permission)
                .setRationaleMessage("카메라 앱을 사용하시려면 권한허용해주세요.")
                .setDeniedMessage("권한을 거부하셨습니다, [앱 설정] -> [권한] 항목에서 허용해주세요.")
                .setPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.CAMERA)
                .check()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { // startActivityForResult를 튱해서 기본 카메라 앱으로부터 받아온 사진 결과 값.
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) { // 이미지를 성공적으로 가져왔다면
            val bitmap: Bitmap
            val file = File(curPhotoPath)
            if (Build.VERSION.SDK_INT < 28){ // 안드로이드 9.0 ( Pie ) 버전보다 낮은 경우
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.fromFile(file))
                binding.ivProfile.setImageBitmap(bitmap)
            }else{ // 안드로이드 9.0 ( Pie ) 버전 이상인 경우
                val decode = ImageDecoder.createSource(
                        this.contentResolver,
                        Uri.fromFile(file)
                )
                bitmap = ImageDecoder.decodeBitmap(decode)
                binding.ivProfile.setImageBitmap(bitmap)
            }

            savePhoto(bitmap)
        }
    }
    //갤러리에 저장한다는 savePhoto메소드에 관한 코드.
    private fun savePhoto(bitmap: Bitmap) {
        val folderPath = Environment.getExternalStorageDirectory().absolutePath + "/Pictures/" // 사진폴더로 저정하기 위한 경로 선언.
        val timestamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val fileName = "${timestamp}.jpeg"
        val folder = File(folderPath)
        if(!folder.isDirectory){ // 현재 해당 경로에 폴더가 존재하지 않는다면
            folder.mkdirs() // make directory줄임말로 해당 경로에 폴더 자동으로 새로 만들기.
        }
        // 실제적인 저장처리
        val out = FileOutputStream(folderPath + fileName)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        Toast.makeText(this, "사진이 앨범에 저장되었습니다.", Toast.LENGTH_SHORT).show()
    }
}

