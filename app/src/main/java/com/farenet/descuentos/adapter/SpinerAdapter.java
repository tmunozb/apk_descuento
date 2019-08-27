package com.farenet.descuentos.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.farenet.descuentos.R;

import java.util.List;

public class SpinerAdapter<T> extends ArrayAdapter<T> {

    private Context context;
    private List<T> values;

    public SpinerAdapter(Context context, List<T> values) {
        super(context, 0, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return initView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return initView(position, convertView, parent);
    }

    private View initView(int position, View convertView, ViewGroup paren){
        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.spin_custom, paren ,false);
        }
        TextView textView = convertView.findViewById(R.id.tv_sp_planta);

        T t = getItem(position);

        textView.setText(t.toString());

        return convertView;
    }
}
