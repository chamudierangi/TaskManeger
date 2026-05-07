package com.example.taskmaneger;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private TextView welcomeText;
    private RecyclerView tasksRecyclerView;
    private FloatingActionButton fabAddTask;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        welcomeText = findViewById(R.id.welcomeText);
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView);
        fabAddTask = findViewById(R.id.fabAddTask);

        String userName = getIntent().getStringExtra("USER_NAME");
        if (userName != null) {
            welcomeText.setText("Hello, " + userName + "!");
        } else {
            welcomeText.setText("Welcome to Task Manager!");
        }

        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList, new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onUpdate(Task task) {
                showUpdateTaskDialog(task);
            }

            @Override
            public void onDelete(Task task) {
                showDeleteTaskDialog(task);
            }
        });
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tasksRecyclerView.setAdapter(taskAdapter);

        // Explicitly get the reference
        // IMPORTANT: Replace this with your actual Firebase Database URL from the Firebase Console
        // It looks like: https://your-project-id-default-rtdb.firebaseio.com/
        String databaseUrl = "https://taskmaneger-fa8cc-default-rtdb.firebaseio.com/";
        
        try {
            databaseReference = FirebaseDatabase.getInstance(databaseUrl).getReference("Tasks");
        } catch (Exception e) {
            // Fallback to default if URL is wrong, but this usually fails if google-services.json is incomplete
            databaseReference = FirebaseDatabase.getInstance().getReference("Tasks");
        }
        
        databaseReference.keepSynced(true);

        fabAddTask.setOnClickListener(v -> showAddTaskDialog());

        loadTasks();
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        EditText editTaskTitle = dialogView.findViewById(R.id.editTaskTitle);
        EditText editTaskDate = dialogView.findViewById(R.id.editTaskDate);

        editTaskDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(HomeActivity.this,
                    (view, year1, month1, dayOfMonth) -> {
                        String selectedDate = year1 + "-" + String.format("%02d", (month1 + 1)) + "-" + String.format("%02d", dayOfMonth);
                        editTaskDate.setText(selectedDate);
                    }, year, month, day);
            datePickerDialog.show();
        });

        builder.setPositiveButton("Add", (dialog, which) -> {
            String title = editTaskTitle.getText().toString().trim();
            String date = editTaskDate.getText().toString().trim();

            if (!title.isEmpty() && !date.isEmpty()) {
                String id = databaseReference.push().getKey();
                Task task = new Task(id, title, date);
                if (id != null) {
                    databaseReference.child(id).setValue(task);
                    Toast.makeText(HomeActivity.this, "Task Added", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(HomeActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void showUpdateTaskDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        TextView dialogTitle = new TextView(this);
        dialogTitle.setText("Update Task");
        dialogTitle.setPadding(20, 20, 20, 20);
        dialogTitle.setTextSize(20);
        builder.setCustomTitle(dialogTitle);

        EditText editTaskTitle = dialogView.findViewById(R.id.editTaskTitle);
        EditText editTaskDate = dialogView.findViewById(R.id.editTaskDate);

        editTaskTitle.setText(task.getTitle());
        editTaskDate.setText(task.getDate());

        editTaskDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            // Try to parse existing date
            String[] parts = task.getDate().split("-");
            int year = parts.length == 3 ? Integer.parseInt(parts[0]) : calendar.get(Calendar.YEAR);
            int month = parts.length == 3 ? Integer.parseInt(parts[1]) - 1 : calendar.get(Calendar.MONTH);
            int day = parts.length == 3 ? Integer.parseInt(parts[2]) : calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(HomeActivity.this,
                    (view, year1, month1, dayOfMonth) -> {
                        String selectedDate = year1 + "-" + String.format("%02d", (month1 + 1)) + "-" + String.format("%02d", dayOfMonth);
                        editTaskDate.setText(selectedDate);
                    }, year, month, day);
            datePickerDialog.show();
        });

        builder.setPositiveButton("Update", (dialog, which) -> {
            String title = editTaskTitle.getText().toString().trim();
            String date = editTaskDate.getText().toString().trim();

            if (!title.isEmpty() && !date.isEmpty()) {
                task.setTitle(title);
                task.setDate(date);
                databaseReference.child(task.getId()).setValue(task);
                Toast.makeText(HomeActivity.this, "Task Updated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(HomeActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void showDeleteTaskDialog(Task task) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    databaseReference.child(task.getId()).removeValue();
                    Toast.makeText(HomeActivity.this, "Task Deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadTasks() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                taskList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Task task = dataSnapshot.getValue(Task.class);
                    if (task != null) {
                        taskList.add(task);
                    }
                }
                // Sort by date
                Collections.sort(taskList, (t1, t2) -> t1.getDate().compareTo(t2.getDate()));
                taskAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomeActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
