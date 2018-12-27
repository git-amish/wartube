package com.amishgarg.wartube.Activity


import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.amishgarg.wartube.FirebaseUtil
import com.amishgarg.wartube.Model.Author
import com.amishgarg.wartube.Model.Post
import com.amishgarg.wartube.R
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import retrofit2.Retrofit
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.ref.WeakReference


class NewPostFragment : Fragment() {

    lateinit var databaseReference: DatabaseReference
    lateinit var postImageView: ImageView
    lateinit var postEditText: EditText
    lateinit var postButton: Button

    val GALLERY = 1;
    val CAMERA = 2;


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        databaseReference = FirebaseDatabase.getInstance().reference
        return inflater.inflate(R.layout.fragment_new_post, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postImageView = view.findViewById(R.id.new_post_img)
        postEditText = view.findViewById(R.id.new_post_text)
        postButton = view.findViewById(R.id.new_post_button)

        postImageView.setOnClickListener {
            showDialog()
        }




    }

    private fun showDialog()
    {
        val pictureDialog = AlertDialog.Builder(context!!)
        pictureDialog.setTitle("Select Action")
        val pictureDialogItems = arrayOf("Select photo from gallery", "Capture photo from camera")
        pictureDialog.setItems(pictureDialogItems
        ) { dialog, which ->
            when (which) {
                0 -> choosePhotoFromGallary()
                1 -> takePhotoFromCamera()
            }
        }
        pictureDialog.show()
    }


    private fun choosePhotoFromGallary()
    {
            val galleryIntent: Intent = Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        startActivityForResult(galleryIntent, GALLERY)
    }

    private fun takePhotoFromCamera()
    {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, CAMERA)

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GALLERY)
        {
            if (data != null)
            {
                val contentURI = data!!.data
                try
                {
                    val bitmap = MediaStore.Images.Media.getBitmap(activity?.contentResolver, contentURI)
                   //val path = saveImage(bitmap)
                    Toast.makeText(context, "Image Saved!", Toast.LENGTH_SHORT).show()
                    postImageView!!.setImageBitmap(bitmap)

                }
                catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(context, "Failed!", Toast.LENGTH_SHORT).show()
                }

            }

        }
        else if (requestCode == CAMERA)
        {
            val thumbnail = data!!.extras!!.get("data") as Bitmap
            postImageView!!.setImageBitmap(thumbnail)
            //saveImage(thumbnail)

        }
        val  bitmap : Bitmap = (postImageView.drawable as BitmapDrawable).bitmap
        postButton.setOnClickListener {
            uploadPost( postEditText.text.toString(), bitmap)
            findNavController().navigate(R.id.posts_dest)
        }
    }

    private fun uploadPost(postText : String, bitmap: Bitmap)
    {
        val postsKey = databaseReference.child("posts").push().key
        val storageReference : StorageReference = FirebaseStorage.getInstance().reference.child("images").child(postsKey!!)

        var baos : ByteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val byedata = baos.toByteArray()
        val uploadTask = storageReference.putBytes(byedata)
        var downloadurl : String? = null
        val author: Author = FirebaseUtil.getAuthor()
        var post = Post()
        uploadTask.addOnSuccessListener { taskSnapshot ->
            Log.d("Upload", "success")
            storageReference.downloadUrl.addOnSuccessListener{
                downloadurl = it.toString()
                Log.d("Upload", downloadurl)
                 post = Post(author, downloadurl, postText, storageReference.toString() ,System.currentTimeMillis())
                val updatedUserData = java.util.HashMap<String, Any>()
                Log.d("upload", FirebaseUtil.getPeoplePath() + author.uid + "/posts/"
                        + postsKey)
                updatedUserData[FirebaseUtil.getPeoplePath() + author.uid + "/posts/"
                        + postsKey] = true
                updatedUserData[FirebaseUtil.getPostsPath() + postsKey] = ObjectMapper().convertValue(post, Map::class.java)

                databaseReference.updateChildren(updatedUserData, object: DatabaseReference.CompletionListener
                {
                    override fun onComplete(p0: DatabaseError?, p1: DatabaseReference) {
                        if (p0 != null) {
                            Log.e("upload", "cannot update database")
                            Log.e("upload", p0.toString())
                        }}
                })
            }

        }.addOnFailureListener {
            Log.d("Upload", "Failed")
        }






    }
}




