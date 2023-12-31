package com.example.gieok_moa

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.example.gieok_moa.MainFragment.Companion.imageUri
import com.example.gieok_moa.databinding.AddSnapPageBinding
import com.example.gieok_moa.databinding.AddtagDialogBinding
import com.takusemba.cropme.OnCropListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.util.Date


class AddSnapActivity : AppCompatActivity() {

    lateinit var Tagdatas: List<com.example.gieok_moa.Tag>
    lateinit var tagList:MutableList<com.example.gieok_moa.Tag>
    lateinit var snapdatas: List<Snap>
    lateinit var selectedTag:com.example.gieok_moa.Tag


    val zero:Int=0

// juseok

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding= AddSnapPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("park",MainFragment.imageUri.toString())

        val cropLayout = binding.addimage
//Uri
        cropLayout.setUri(MainFragment.imageUri)
//Bitmap
        /*cropLayout.setBitmap(image)
        Glide.with(binding.root)
            .load(MainFragment.imageUri)
            .into(binding.addimage)*/




        val currentContext: Context = this
        //db생성시 code
        val db = UserDatabase.getInstance(currentContext.applicationContext)
        val loading = CoroutineScope(Dispatchers.IO).launch {
            Tagdatas = db!!.tagDao().getAll()
        }
        runBlocking {
            loading.join()
        }

        tagList= mutableListOf()
        for (i in 0..Tagdatas.size-1){
            if(Tagdatas[i].ownedSnapID==zero.toLong())
                tagList.add(Tagdatas[i])
        }


        //Log.d("park",datas[0].staus)
        var spinnerAdapterTag=TagSpinnerAdapter(this, R.layout.item_spinner, tagList)

        binding.tagSpinner.adapter=spinnerAdapterTag
        Log.d("park","success3")

        binding.tagSpinner.onItemSelectedListener= object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // 선택된 항목의 값 가져오기
                val selectedItem: Any? = parent.getItemAtPosition(position)

                // Tag 클래스로 캐스팅
                if (selectedItem is com.example.gieok_moa.Tag) {
                    selectedTag= selectedItem
                    // 이제 selectedTag를 사용할 수 있습니다.
                    // 예: selectedTag.getName(), selectedTag.getId() 등
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 아무것도 선택되지 않았을 때의 동작
            }
        }
        binding.cancelButton.setOnClickListener {
            finish()
        }
        binding.completeButton.setOnClickListener {
            var imageUri: Uri
            val loading = CoroutineScope(Dispatchers.IO).launch {
                snapdatas = db!!.snapDao().getAll()
                Tagdatas = db!!.tagDao().getAll()
            }
            runBlocking {
                loading.join()
            }

            val date= Date()
            var usercomment=binding.edittext.text.toString()
            if(usercomment==null) usercomment=""

            var snapid: Long = 1
            if (snapdatas != null && snapdatas.isNotEmpty()) {
                snapid = snapdatas.last().snapId + 1
            }
            cropLayout.crop()
            cropLayout.addOnCropListener(object : OnCropListener {
                override fun onSuccess(bitmap: Bitmap) {
                    // 성공적으로 크롭된 Bitmap을 Uri로 전환하여 저장하는 코드
                    Log.d("park","success crop")

                    // Bitmap을 Uri로 변환
                    imageUri = bitmapToUriConverter(bitmap)
                    //database에 저장후 mainpage로 돌아감
                    Log.d("park","success image")


                    //고유 SANPID를 가진 TAG로 저장
                    var tagid: Long = 1
                    if (Tagdatas != null && Tagdatas.isNotEmpty()) {
                        tagid = Tagdatas.last().tagID + 1
                    }
                    Log.d("park","${tagid}")
                    val newSanpandTagpair = Tag(tagid.toLong(), selectedTag.staus, selectedTag.color, snapid.toLong())


                    val snap1 = Snap(snapid.toLong(), date, imageUri.toString(), usercomment)

                    //snap, tag를 db에 저장
                    CoroutineScope(Dispatchers.IO).launch{
                        db!!.tagDao().insertAll(newSanpandTagpair)
                        db!!.snapDao().insertAll(snap1)
                    }

                }

                override fun onFailure(e: Exception) {
                    // 에러 처리 로직
                }
            })

            finish()
        }

        binding.addtag.setOnClickListener {
            var tagid: Long = 1
            if (Tagdatas != null && Tagdatas.isNotEmpty()) {
                tagid = Tagdatas.last().tagID + 1
            }

            val dialogBinding = AddtagDialogBinding.inflate(layoutInflater)
            val dialog = AlertDialog.Builder(this).run {

                setView(dialogBinding.root)
                create()
            }
            dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            dialog.window?.setLayout(180, 170)

            dialogBinding.tagsaveBtn.setOnClickListener {
                // EditText 내용 가져오기

                val editTextContent = dialogBinding.edittext.text.toString()
                val radioGroup: RadioGroup = dialogBinding.TagradioGroup
                val checkedRadioButtonId = radioGroup.checkedRadioButtonId
                lateinit var saveTag:com.example.gieok_moa.Tag
                val radioButton: RadioButton = dialogBinding.root.findViewById(checkedRadioButtonId)

                lateinit var tagColor:Color
                when(radioButton){
                    dialogBinding.radioButtonRed->tagColor=Color.RED
                    dialogBinding.radioButtonGreen->tagColor=Color.GREEN
                    dialogBinding.radioButtonYellow->tagColor=Color.YELLOW
                    else->tagColor=Color.RED
                }
                Log.d("park", "Radio Group ID: $radioButton")
                saveTag=Tag(tagid,editTextContent,tagColor,zero.toLong())
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Log.d("park", "${zero.toLong()}")
                        db!!.tagDao().insertAll(saveTag)
                        tagList.add(saveTag)
                    } catch (e: Exception) {
                        Log.e("park", "Error inserting tag: ${e.message}")
                    }
                }

                dialog.dismiss()
            }

            dialog.show()
        }

    }

    private fun bitmapToUriConverter(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(this?.contentResolver, bitmap, "Title", null)
        return Uri.parse(path)
    }

}
