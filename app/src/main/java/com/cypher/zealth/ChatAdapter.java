package com.cypher.zealth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 1;
    private static final int TYPE_BOT = 2;

    private final List<ChatMessage> items;

    public ChatAdapter(List<ChatMessage> items) {
        this.items = items;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isUser ? TYPE_USER : TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            View v = inflater.inflate(R.layout.item_chat_user, parent, false);
            return new UserVH(v);
        } else {
            View v = inflater.inflate(R.layout.item_chat_bot, parent, false);
            return new BotVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = items.get(position);
        if (holder instanceof UserVH) {
            ((UserVH) holder).txt.setText(msg.text);
        } else if (holder instanceof BotVH) {
            ((BotVH) holder).txt.setText(msg.text);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class UserVH extends RecyclerView.ViewHolder {
        TextView txt;
        UserVH(@NonNull View itemView) {
            super(itemView);
            txt = itemView.findViewById(R.id.txtMessage);
        }
    }

    static class BotVH extends RecyclerView.ViewHolder {
        TextView txt;
        BotVH(@NonNull View itemView) {
            super(itemView);
            txt = itemView.findViewById(R.id.txtMessage);
        }
    }
}
