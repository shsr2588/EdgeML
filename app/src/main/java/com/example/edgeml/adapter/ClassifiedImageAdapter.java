package com.example.edgeml.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edgeml.R;
import com.example.edgeml.data.ClassifiedImage;

import java.util.List;

public class ClassifiedImageAdapter extends RecyclerView.Adapter<ClassifiedImageAdapter.ViewHolder> {

    private List<ClassifiedImage> classifiedImageList;

    public ClassifiedImageAdapter(List<ClassifiedImage> classifiedImageList) {
        this.classifiedImageList = classifiedImageList;
    }

    @NonNull
    @Override
    public ClassifiedImageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.classified_image_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassifiedImageAdapter.ViewHolder holder, int position) {
        ClassifiedImage classifiedImage = classifiedImageList.get(position);
        holder.imageView.setImageBitmap(classifiedImage.getImage());
        holder.labelView.setText(classifiedImage.getLabel());
        holder.timeView.setText("Inference Time: " + classifiedImage.getInferenceTime() + " ms");
    }

    @Override
    public int getItemCount() {
        return classifiedImageList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView labelView, timeView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewItem);
            labelView = itemView.findViewById(R.id.labelViewItem);
            timeView = itemView.findViewById(R.id.timeViewItem);
        }
    }
}