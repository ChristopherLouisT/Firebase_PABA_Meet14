package paba.meet14.firebase

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.util.HashMap
import com.cloudinary.android.MediaManager
import java.io.File

class MainActivity : AppCompatActivity() {
//    var dataProvinsi = ArrayList<daftarProvinsi>()
    var data: MutableList<Map<String, Any>> = ArrayList()
//    lateinit var  lvAdapter : ArrayAdapter<daftarProvinsi>
    lateinit var lvAdapter: SimpleAdapter

    lateinit var _etProvinsi: EditText
    lateinit var _etIbukota: EditText
    lateinit var _btnSimpan: Button
    lateinit var _lvData: ListView
    lateinit var _ivUpload: ImageView

    private val CLOUDINARY_CLOUD_NAME = "dtwkqcaiy"
    private val UNSIGNED_UPLOAD_PRESET = "preset1"
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val db = Firebase.firestore
        _etProvinsi = findViewById<EditText>(R.id.etProvinsi)
        _etIbukota = findViewById<EditText>(R.id.etIbukota)
        _btnSimpan = findViewById<Button>(R.id.btnSimpan)
        _lvData = findViewById<ListView>(R.id.lvData)
        _ivUpload = findViewById<ImageView>(R.id.ivUpload)

//        lvAdapter = ArrayAdapter<daftarProvinsi>(
//            this,
//            android.R.layout.simple_list_item_1,
//            dataProvinsi
//        )

        lvAdapter = SimpleAdapter(
            this,
            data,
            R.layout.list_item_with_image,
            arrayOf("Image", "Provinsi", "IbuKota"),
            intArrayOf(R.id.imageLogo, R.id.tvProvinsi, R.id.tvIbukota)
        )

        lvAdapter.setViewBinder { view, data, _ ->
            if (view.id == R.id.imageLogo) {
                val imgView = view as ImageView
                val defaultImage =R.drawable.chisa

                if (data is String && data.isNotEmpty()) {
                } else {
                    imgView.setImageResource(defaultImage)
                }
                return@setViewBinder true
            }
            false
        }

        _lvData.adapter = lvAdapter

        _lvData.setOnItemClickListener { adapterView, view, i, l ->
            _etProvinsi.setText(data[i].get("Provinsi").toString())
            _etIbukota.setText(data[i].get("IbuKota").toString())
        }

        _lvData.setOnItemLongClickListener { adapterView, view, i, l ->
            val  namaProvinsi: String = data[i]["Provinsi"].toString()
            val  namaIbukota: String = data[i]["IbuKota"].toString()
            db.collection("tbProvinsi").document(namaProvinsi).delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "$namaProvinsi dan $namaIbukota Berhasil Dihapus", Toast.LENGTH_SHORT).show()
                    readData(db)
                }
                .addOnFailureListener {
                    Log.d("Firebase - DELETE DATA", it.message.toString())
                }
                true
        }

        _btnSimpan.setOnClickListener {
            tambahData(db, _etProvinsi.text.toString(), _etIbukota.text.toString())
        }

        readData(db)

        val config = mapOf("cloud_name" to CLOUDINARY_CLOUD_NAME, "upload_preset" to UNSIGNED_UPLOAD_PRESET)
        MediaManager.init(this,config)

        _ivUpload.setOnClickListener {
            showImagePickDialog()
        }
    }
    
    private val pickImageFromGallery = registerForActivityResult(
        ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { 
            selectedImageUri = it
            _ivUpload.setImageURI(it)
        }
    }
    
    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()) {
        if (it) {
            selectedImageUri = cameraImageUri
            _ivUpload.setImageURI(cameraImageUri)
        }
    }

    private fun createImageUri(): Uri {
        val imageFile = File(
            cacheDir,
            "temp_image_${System.currentTimeMillis()}.jpg"
        )
        return FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            imageFile
        )
    }

    private fun showImagePickDialog() {
        val options = arrayOf("Pilih dari Galeri", "Ambil Foto")
        AlertDialog.Builder(this).setTitle("Pilih Gambar").setItems(options) { dialog, which ->
            when (which) {
                0 -> pickImageFromGallery.launch("image/*")
                1 -> {
                    createImageUri()?.let { uri ->
                        cameraImageUri = uri
                        takePicture.launch(uri)
                    }
                }
            }
        }.show()
    }

    fun tambahData(db: FirebaseFirestore, provinsi: String, ibuKota: String) {
        val dataBaru = daftarProvinsi(provinsi, ibuKota)

        db.collection("tbProvinsi").document(_etProvinsi.text.toString()).set(dataBaru)
            // Kalau pake document sifatnya akan jadi update maka menggunakan set, jika tidak pakai document maka gunakan add
            // Gunanya document agar nama documentnya jelas sehingga kalau ada document yang sama akan diupdate dan tidak tambah baru
            .addOnSuccessListener {
                _etProvinsi.setText("")
                _etIbukota.setText("")
                readData(db)
                Log.d("Firebase", dataBaru.provinsi + " Berhasil Ditambahkan")
                Toast.makeText(this, dataBaru.provinsi + " dan " + dataBaru.ibuKota + " Berhasil Ditambahkan", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Log.d("Firebase - ADD DATA", it.message.toString())
            }
    }

    fun readData(db: FirebaseFirestore) {
        db.collection("tbProvinsi").get()
            .addOnSuccessListener { result ->
//                dataProvinsi.clear()
                data.clear()
                for (item in result) {
//                    val itemData = daftarProvinsi(item.data.get("provinsi").toString(), item.data.get("ibuKota").toString())
//                    dataProvinsi.add(itemData)

                    val itemData: MutableMap<String, Any> = HashMap(2)
                    itemData["Provinsi"] = item.data.get("provinsi").toString()
                    itemData["IbuKota"] = item.data.get("ibuKota").toString()
                    data.add(itemData)
                }
                lvAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Log.d("Firebase - GET DATA", it.message.toString())
            }
    }
}