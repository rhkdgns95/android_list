package com.example.listactivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.listactivity.model.ChatModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private Button button;
    private EditText editText;
    private RecyclerView recyclerView;

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button) findViewById(R.id.mainactivity_button);
        final EditText editText = (EditText) findViewById(R.id.mainactivity_edittext);
        recyclerView = (RecyclerView) findViewById(R.id.mainactivity_recyclerview);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatModel.Comment comment = new ChatModel.Comment();
                comment.message = editText.getText().toString();
                comment.timestamp = ServerValue.TIMESTAMP;

                button.setEnabled(false); // Firebase의 DB에 저장이 되기전까지 대기.

                /**
                 *  (1) Firebase 실시간 데이터베이스 저장.
                 *
                 *   Firebase의 Database에
                 *   실시간으로 /chatrooms/comments에
                 *   내용, 시간이 포함됨
                 *   데이터 값이 저장된다.
                 */
                FirebaseDatabase.getInstance().getReference().child("chatrooms").child("comments").push().setValue(comment).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        editText.setText("");
                        button.setEnabled(true);
                    }
                });
            }
        });
        subscribeMessage();
    }
    void subscribeMessage () {
        FirebaseDatabase.getInstance().getReference().child("chatrooms").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot item : dataSnapshot.getChildren()) {
                    recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                    recyclerView.setAdapter(new RecyclerViewAdapter());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    /**
     *   RecyclerViewAdapter
     *
     *   RecyclerView를
     *   ViewHolder에 담아서
     *   관리되도록 한다.
     */
    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        List<ChatModel.Comment> comments;

        public RecyclerViewAdapter() {
            comments = new ArrayList<>();
            getMessageList();
        }
        /**
         *  getMessageList()
         *
         *  서버에 /chatrooms/comments의 내용을 가져온다.
         */
        void getMessageList() {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("chatrooms").child("comments");
            ValueEventListener valueEventListener = databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    comments.clear(); // clear하지 않으면, 중복된 내용들이 쌓이게됨.
                    int i = 0;
                    for(DataSnapshot item : dataSnapshot.getChildren()) {
                        String key = item.getKey();
                        ChatModel.Comment comment = item.getValue(ChatModel.Comment.class);
                        comments.add(comment);
                    }

                    if(comments.size() == 0) {
                        // 저장된 내용이 없는경우.
                        return;
                    } else {
                        notifyDataSetChanged();// 새로운 데이터를 갱신.
                        recyclerView.scrollToPosition(comments.size() - 1); // 채팅방 스크롤 하단으로 갱신하기.
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        /**
         *  getMessage(position): 선택한 메시지 읽기.
         *  @param position: DB에 속한 값.
         */
        void getMessage(final int position) {
            Toast.makeText(MainActivity.this, (position + 1) + "번째 텍스트: ", Toast.LENGTH_SHORT).show();
        }
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
            String message = comments.get(position).message;
            long unixTime = (long) comments.get(position).timestamp;
            Date date = new Date(unixTime);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            String time = simpleDateFormat.format(date);

            MessageViewHolder messageViewHolder = ((MessageViewHolder) holder);

            messageViewHolder.textView_message.setText(message);
            messageViewHolder.textView_timestamp.setText(time);
            messageViewHolder.linearLayout_main.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    getMessage(position);
                    return true;
                }
            });

            // 추가하도록한다.
            // messageViewHolder.textView_message.setBackground(R.drawable.message_bubble_right);
            messageViewHolder.textView_message.setTextSize(25);

            messageViewHolder.linearLayout_main.setGravity(Gravity.RIGHT);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        private class MessageViewHolder extends RecyclerView.ViewHolder {
            public LinearLayout linearLayout_main;
            public TextView textView_message;
            public TextView textView_timestamp;

            public MessageViewHolder(View view) {
                super(view);
                textView_message = (TextView) view.findViewById(R.id.messageitem_textview_message);
                textView_timestamp = (TextView) view.findViewById(R.id.messageItem_textview_timestamp);
                linearLayout_main = (LinearLayout) view.findViewById(R.id.messageitem_linearlayout);
            }
        }
    }
}