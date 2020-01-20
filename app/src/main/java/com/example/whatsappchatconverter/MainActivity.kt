package com.example.whatsappchatconverter

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider.getUriForFile
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import org.w3c.dom.Text
import java.io.*
import java.lang.Exception
import java.lang.StringBuilder
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    val TAG = "fileerror"
    val filename : TextView by lazy {findViewById<TextView>(R.id.filename)}
    val findFile : Button by lazy { findViewById<Button>(R.id.selectfile) }
    val convert : Button by lazy { findViewById<Button>(R.id.convert) }
    val share : Button by lazy { findViewById<Button>(R.id.share) }
    var root : String? = null
    var lines : ArrayList<HashMap<String,String>> = ArrayList()
    val chatee  = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var content : Uri? = null
        when {
            intent?.action == Intent.ACTION_SEND  || intent?.action == Intent.ACTION_SEND_MULTIPLE ->{

                val selectedfile_uri : Uri? = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)[0];
                Log.d(TAG,selectedfile_uri.toString())
                var fname : String? = selectedfile_uri?.lastPathSegment

                if(fname!!.contains("primary:")){
                    try{
                        fname = fname.substring(fname.lastIndexOf('/') + 1)
                    }catch (e : Exception){
                        fname = fname.substring(fname.indexOf(':') + 1)
                    }
                }

                filename.setText(fname)
                readTextFile(selectedfile_uri)
            }
        }

//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
//            if( checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
//                var permission : Array<String> = Array(1,{android.Manifest.permission.WRITE_EXTERNAL_STORAGE})
//                requestPermissions(permission,100);
//            }
//        }

        findFile.setOnClickListener( View.OnClickListener {

            val intent : Intent = Intent().setType("text/plain").setAction(Intent.ACTION_GET_CONTENT)
            startActivityForResult(Intent.createChooser(intent,"Select Whatsapp Chat TXT file"), 123)

        })

        convert.setOnClickListener( View.OnClickListener {
           content =  convertTextObjectModel(lines)
            if(content != null){
                share.isEnabled = true
            }else{
                share.isEnabled = false
            }
        })

        share.setOnClickListener( View.OnClickListener {
            if(content != null) {
                shareFile(content!!);
            }
        })
    }

//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//
//        when(requestCode){
//            100 -> {
//                if(grantResults.size > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED){
//                    Toast.makeText(this,"Storage permission is required to store data",Toast.LENGTH_LONG).show()
//                    finishAffinity()
//                }
//            }
//        }
//    }

    override fun onActivityResult(requestCode:Int, resultCode:Int, data:Intent?){
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 123 && resultCode == Activity.RESULT_OK){
            val selectedfile_uri : Uri? = data?.getData()
            var fname : String? = selectedfile_uri?.lastPathSegment

            if(fname!!.contains("primary:")){
                try{
                    fname = fname.substring(fname.lastIndexOf('/') + 1)
                }catch (e : Exception){
                    fname = fname.substring(fname.indexOf(':') + 1)
                }
            }
            filename.setText(fname)
            lines = ArrayList()
            readTextFile(selectedfile_uri)
        }
    }

    fun convertTextObjectModel(lines : ArrayList<HashMap<String,String>>) : Uri?{
        val gson : Gson = GsonBuilder().setPrettyPrinting().create()
        var content : Uri? = null
        var jsonStr : String = gson.toJson(lines)
        var file : String = filename.text.toString()
        root = (this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!.absolutePath)
        try {
            file = file.substring(0, file.indexOf(".")) + ".json"
        }catch (e : Exception){
            file = file + ".json"
        }

        try {
            val dir : File = File(this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!.absolutePath)
            if(!dir.exists()){
                if(!dir.mkdirs()){
                    Log.d(TAG,"Error while creating file")
                }
            }
            var f : File = File((this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!.absolutePath) + "/" + file)
            if(f.exists()){
                f.delete()
            }
            val out =  BufferedWriter( FileWriter(f))
            out.write(jsonStr)
            out.close()
            Toast.makeText(this,"file saved at " + this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!.absolutePath, Toast.LENGTH_LONG).show()
            content  = getUriForFile(this,"com.example.fileprovider",f)

        }catch (e:Exception){
            Log.d(TAG,e.message)
        }

        return  content
    }

    fun shareFile(content : Uri){

        val intent = ShareCompat.IntentBuilder.from(this)
                .setStream(content)
                .setType("*/*")
                .intent
                .setAction(Intent.ACTION_SEND)
                .setDataAndType(content,"*/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    fun readTextFile(uri : Uri?) {

        var prev_time : String = ""
        var prev_sender : String = ""
        var date : String = ""
        var sender: String = ""
        var time : String = ""
        var msg : String = ""

        val pattern = Regex("[\\d]{1,2}/[\\d]{1,2}/[\\d]{2}, [\\d]{1,2}:[\\d]{1,2} ")

        try {
            val inputStream : InputStreamReader = InputStreamReader(uri?.let { getContentResolver().openInputStream(it) },"UTF-8")
            val reader : BufferedReader =  BufferedReader(inputStream)
        var line: String? = null;
        while ({ line = reader.readLine(); line }() != null) {

                try {
                    if (line?.let { pattern.containsMatchIn(it) }!!) {
                        date = line!!.substring(0, line!!.indexOf(","))
                        time = line!!.substring(line!!.indexOf(",") + 1, line!!.indexOf("-") - 1)
                        sender = line!!.substring(line!!.indexOf("-") + 1, line!!.indexOf(":", line!!.indexOf("-")))
                        msg = line!!.substring(line!!.indexOf(":", line!!.indexOf("-")) + 2)
                        chatee.add(sender)
                        if (prev_sender == sender && prev_time == time) {
                            val str: String? = lines.get(lines.lastIndex).get("message")
                            lines.get(lines.lastIndex).put("message", str + "\n" + msg)
                        } else {
                            prev_sender = sender
                            prev_time = time
                            val hashmap: HashMap<String, String> = HashMap(4)
                            hashmap.put("date", date)
                            hashmap.put("time", time)
                            hashmap.put("sender", sender)
                            hashmap.put("message", msg)
                            lines.add(hashmap)
                        }
                    }else{
                        msg = line!!.substring(line!!.indexOf(":", line!!.indexOf("-")) + 2)
                        val str: String? = lines.get(lines.lastIndex).get("message")
                        lines.get(lines.lastIndex).put("message", str + "\n" + msg)
                    }
                }catch (e : Exception){
                    continue
                }
            }
            reader.close()
            convert.isEnabled = true

        } catch (e : Exception){
            Log.d(TAG,e.message )
            Toast.makeText(this,e.message,Toast.LENGTH_LONG).show()
        }
    }
}
