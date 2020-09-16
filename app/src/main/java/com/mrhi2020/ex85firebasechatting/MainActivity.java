package com.mrhi2020.ex85firebasechatting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    CircleImageView ivProfile;
    EditText etName;

    //프로필 이미지 Uri 참조변수
    Uri imgUri;

    boolean isChanged= false; //데이터의 변경이 있었는가?

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivProfile= findViewById(R.id.iv_profile);
        etName= findViewById(R.id.et_name);

        //이미 저장되어 있는 정보들 읽어오기
        loadData();

        if(G.nickName != null){ //저장된 것이 있다는 것임
            etName.setText(G.nickName);
            Glide.with(this).load(G.profileUri).into(ivProfile);
        }

    }



    public void clickImage(View view) {
        Intent intent= new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if( requestCode==100 && resultCode==RESULT_OK){
            imgUri= data.getData();
            if(imgUri!=null){
                Glide.with(this).load(imgUri).into(ivProfile);

                //프로필 이미지를 변경했다고 표식
                isChanged= true;
            }
        }
    }

    //데이터를 저장하는 메소드
    void saveData(){
        //프로필이미지와 채팅명을 Firebase DB에 저장

        G.nickName= etName.getText().toString();


        //먼저 이미지파일 부터 Firebase Storage에 업로드
        //업로드할 파일명이 같으면 안되므로 날짜를 이용해서 파일명 지정
        SimpleDateFormat sdf= new SimpleDateFormat("yyyyMMddhhmmss");
        String fileName= sdf.format(new Date()) +".png";

        FirebaseStorage firebaseStorage= FirebaseStorage.getInstance();
        final StorageReference imgRef= firebaseStorage.getReference("profileImages/"+fileName);

        //이미지 업로드
        UploadTask task = imgRef.putFile(imgUri);
        task.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                //firebase실시간DB에 저장할 저장소에 업로드된 실제 인터넷경로 URL 알아내기
                imgRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {

                        //다운로드 URL을 G.profileUri에 저장
                        G.profileUri= uri.toString();

                        //firebase에 저장된 이미지 파일의 경로를 fire DB에 저장하고 내 디바이스에도 저장

                        //1. firebaseDB 저장 [ G.nickName, imgUri의 다운로드 URL ]
                        FirebaseDatabase firebaseDatabase= FirebaseDatabase.getInstance();
                        //"profiles"라는 이름의 자식노드 참조객체
                        DatabaseReference profileRef= firebaseDatabase.getReference("profiles");
                        //nickName을 Key 값을 지정한 노드에 이미지URL을 값으로 지정
                        profileRef.child(G.nickName).setValue(G.profileUri);


                        //2. SharedPreferences 이용하여 저장 [ G.nickName, G.profileUri ]
                        SharedPreferences pref= getSharedPreferences("account", MODE_PRIVATE);
                        SharedPreferences.Editor editor = pref.edit();

                        editor.putString("nickName", G.nickName);
                        editor.putString("profileUrl", G.profileUri);

                        editor.commit();

                        Toast.makeText(MainActivity.this, "저장완료", Toast.LENGTH_SHORT).show();

                        //모든 저장이 완료되었으므로..
                        //채팅화면으로 이동
                        Intent intent= new Intent(MainActivity.this, ChattingActivity.class);
                        startActivity(intent);

                        finish();
                    }
                });
            }
        });


    }

    public void clickBtn(View view) {

        //데이터의 변경이 있었는가?
        if( isChanged ) saveData();
        else {
            Intent intent= new Intent(this, ChattingActivity.class);
            startActivity(intent);
            finish();
        }
    }


    //디바이스에 저장된 정보 읽어오기
    void loadData(){
        SharedPreferences pref= getSharedPreferences("account", MODE_PRIVATE);
        G.nickName= pref.getString("nickName", null);
        G.profileUri= pref.getString("profileUrl", null);

    }
}
