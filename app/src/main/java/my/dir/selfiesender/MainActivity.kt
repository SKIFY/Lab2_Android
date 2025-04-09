package my.dir.selfiesender

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import my.dir.selfiesender.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var currentPhotoPath: String
    private val TAG = "SelfieSender"

    // Контракт для зйомки фото
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            try {
                binding.imageView.setImageURI(Uri.fromFile(File(currentPhotoPath)))
                binding.btnSendSelfie.isEnabled = true
            } catch (e: Exception) {
                Log.e(TAG, "Error setting image: ${e.message}")
                showToast("Помилка при відображенні фото")
            }
        }
    }

    // Контракт для запиту дозволів (оновлено для Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            openCamera()
        } else {
            showToast("Дозволи не надано")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnTakeSelfie.setOnClickListener {
            checkPermissions()
        }

        binding.btnSendSelfie.setOnClickListener {
            sendEmailWithSelfie()
        }
    }

    private fun checkPermissions() {
        // Оновлений перелік дозволів для Android 13+
        val requiredPermissions = mutableListOf(Manifest.permission.CAMERA)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            requiredPermissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            openCamera()
        } else {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            val photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(photoUri)
        } catch (e: IOException) {
            Log.e(TAG, "Error creating file: ${e.message}")
            showToast("Помилка при створенні файлу")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera: ${e.message}")
            showToast("Помилка при відкритті камери")
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Використовуємо внутрішнє сховище додатка - не потребує дозволів
        val storageDir = getExternalFilesDir(null) ?: filesDir
        return File.createTempFile(
            "JPEG_${System.currentTimeMillis()}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun sendEmailWithSelfie() {
        try {
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("hodovychenko@op.edu.ua"))
                putExtra(Intent.EXTRA_SUBJECT, "ANDROID [Ваше Прізвище та Ім'я]")
                putExtra(Intent.EXTRA_TEXT, "Посилання на репозиторій: [ваше посилання]")

                val file = File(currentPhotoPath)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        this@MainActivity,
                        "${packageName}.fileprovider",
                        file
                    )
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    showToast("Фото не знайдено")
                    return
                }
            }

            startActivity(Intent.createChooser(emailIntent, "Відправити email"))
        } catch (e: Exception) {
            Log.e(TAG, "Error sending email: ${e.message}")
            showToast("Помилка при відправці email")
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}