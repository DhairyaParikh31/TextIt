package com.dhairyaparikh.textit.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.ActivityGroup;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.dhairyaparikh.textit.Adapters.GroupMessagesAdapter;
import com.dhairyaparikh.textit.Adapters.MessagesAdapter;
import com.dhairyaparikh.textit.Models.Message;
import com.dhairyaparikh.textit.R;
import com.dhairyaparikh.textit.databinding.ActivityChatBinding;
import com.dhairyaparikh.textit.databinding.ActivityGroupChatBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class GroupChatActivity extends AppCompatActivity {
    ActivityGroupChatBinding binding;
    GroupMessagesAdapter adapter;
    ArrayList<Message> messages;
    FirebaseDatabase database;
    FirebaseStorage storage;
    String senderUid;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityGroupChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        senderUid= FirebaseAuth.getInstance().getUid();


        getSupportActionBar().setTitle("Group Chat");
        messages = new ArrayList<>();
        dialog=new ProgressDialog(this);
        dialog.setMessage("Uploading Image....");
        dialog.setCancelable(false);
        adapter = new GroupMessagesAdapter(this,messages);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);


        database.getReference().child("public")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messages.clear();
                        for(DataSnapshot snapshot1 : snapshot.getChildren()){
                            Message message = snapshot1.getValue(Message.class);
                            message.setMessageId(snapshot1.getKey());
                            messages.add(message);
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
        binding.sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageTxt = binding.messageBox.getText().toString();
                Date date = new Date();
                Message message = new Message(messageTxt, senderUid,date.getTime());
                binding.messageBox.setText("");


                database.getReference().child("public")
                        .push()
                        .setValue(message);
            }
        });
        binding.attachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent,25);
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==25){
            if(data!=null){
                if(data.getData()!=null){
                    Uri selectedImage = data.getData();
                    Calendar calendar = Calendar.getInstance();
                    StorageReference reference = storage.getReference().child("chats").child(calendar.getTimeInMillis()+"");
                    dialog.show();
                    reference.putFile(selectedImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            dialog.dismiss();
                            if(task.isSuccessful()){
                                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        String filePath = uri.toString();

                                        String messageTxt = binding.messageBox.getText().toString();
                                        Date date = new Date();
                                        Message message = new Message(messageTxt, senderUid,date.getTime());
                                        message.setMessage("photo");
                                        message.setImageUrl(filePath);
                                        binding.messageBox.setText("");

                                        database.getReference().child("public")
                                                .push()
                                                .setValue(message);

                                    }
                                });
                            }
                        }
                    });
                }
            }
        }
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}