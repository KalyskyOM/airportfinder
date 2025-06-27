package com.example.airportfinder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for displaying NOTAM data in a RecyclerView
 */
public class NotamAdapter extends RecyclerView.Adapter<NotamAdapter.NotamViewHolder> {
    
    private List<NotamData> notams;
    
    public NotamAdapter(List<NotamData> notams) {
        this.notams = notams;
    }
    
    @NonNull
    @Override
    public NotamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.notam_item, parent, false);
        return new NotamViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull NotamViewHolder holder, int position) {
        NotamData notam = notams.get(position);
        holder.bind(notam);
    }
    
    @Override
    public int getItemCount() {
        return notams != null ? notams.size() : 0;
    }
    
    public void updateNotams(List<NotamData> newNotams) {
        this.notams = newNotams;
        notifyDataSetChanged();
    }
    
    static class NotamViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNotamId;
        private final TextView tvNotamType;
        private final TextView tvNotamText;
        private final TextView tvNotamValidity;
        
        public NotamViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNotamId = itemView.findViewById(R.id.tvNotamId);
            tvNotamType = itemView.findViewById(R.id.tvNotamType);
            tvNotamText = itemView.findViewById(R.id.tvNotamText);
            tvNotamValidity = itemView.findViewById(R.id.tvNotamValidity);
        }
        
        public void bind(NotamData notam) {
            if (notam != null) {
                tvNotamId.setText(notam.getId() != null ? notam.getId() : itemView.getContext().getString(R.string.notam_not_available));
                tvNotamType.setText(notam.getType() != null ? notam.getType() : itemView.getContext().getString(R.string.notam_type_unknown));
                tvNotamText.setText(notam.getText() != null ? notam.getText() : itemView.getContext().getString(R.string.notam_no_details));
                tvNotamValidity.setText(notam.getFormattedValidity() != null ? notam.getFormattedValidity() : itemView.getContext().getString(R.string.notam_validity_unknown));
            } else {
                tvNotamId.setText(itemView.getContext().getString(R.string.notam_not_available));
                tvNotamType.setText(itemView.getContext().getString(R.string.notam_type_unknown));
                tvNotamText.setText(itemView.getContext().getString(R.string.notam_no_details));
                tvNotamValidity.setText(itemView.getContext().getString(R.string.notam_validity_unknown));
            }
        }
    }
}
