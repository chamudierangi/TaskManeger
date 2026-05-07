package com.example.taskmaneger;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private OnTaskActionListener listener;

    public interface OnTaskActionListener {
        void onUpdate(Task task);
        void onDelete(Task task);
    }

    public TaskAdapter(List<Task> taskList, OnTaskActionListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.titleTextView.setText(task.getTitle());
        holder.dateTextView.setText(task.getDate());

        // Apply strike-through if completed
        updateStrikeThrough(holder.titleTextView, task.isCompleted());
        
        // Remove listener before setting state to avoid loops
        holder.statusCheckBox.setOnCheckedChangeListener(null);
        holder.statusCheckBox.setChecked(task.isCompleted());

        holder.statusCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
            updateStrikeThrough(holder.titleTextView, isChecked);
            if (task.getId() != null) {
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tasks").child(task.getId());
                ref.child("completed").setValue(isChecked);
            }
        });

        holder.editTask.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUpdate(task);
            }
        });

        holder.deleteTask.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(task);
            }
        });
    }

    private void updateStrikeThrough(TextView textView, boolean isCompleted) {
        if (isCompleted) {
            textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            textView.setPaintFlags(textView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView dateTextView;
        CheckBox statusCheckBox;
        ImageView editTask;
        ImageView deleteTask;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.taskTitle);
            dateTextView = itemView.findViewById(R.id.taskDate);
            statusCheckBox = itemView.findViewById(R.id.taskStatus);
            editTask = itemView.findViewById(R.id.editTask);
            deleteTask = itemView.findViewById(R.id.deleteTask);
        }
    }
}
