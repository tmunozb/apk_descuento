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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adapter genérico para Spinners. Muestra toString() por defecto,
 * o una etiqueta custom con Labeler<T>.
 */
public class SpinerAdapter<T> extends ArrayAdapter<T> {

    /** Permite definir cómo mostrar cada item sin depender de toString(). */
    public interface Labeler<T> {
        String getLabel(@Nullable T item);
    }

    private final LayoutInflater inflater;
    private final int layoutResId;
    private final int textViewId;
    private final Labeler<T> labeler;

    /** Constructor usual: usa tu layout R.layout.spin_custom y R.id.tv_sp_text */
    public SpinerAdapter(@NonNull Context context, @Nullable List<T> items) {
        this(context, items, R.layout.spin_custom, R.id.tv_sp_text, item -> String.valueOf(item));
    }

    /** Constructor avanzado: puedes pasar layout/textView y un labeler. */
    public SpinerAdapter(@NonNull Context context,
                         @Nullable List<T> items,
                         int layoutResId,
                         int textViewId,
                         @NonNull Labeler<T> labeler) {
        super(context, 0, items != null ? new ArrayList<>(items) : new ArrayList<T>());
        this.inflater = LayoutInflater.from(context);
        this.layoutResId = layoutResId;
        this.textViewId = textViewId;
        this.labeler = labeler != null ? labeler : (it -> String.valueOf(it));
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // no afecta porque inflamos manual
    }

    /** Reemplaza la data del adapter (lo importante para refrescar tras la sync). */
    public void setItems(@Nullable List<T> items) {
        clear();
        if (items != null && !items.isEmpty()) {
            addAll(items);
        }
        notifyDataSetChanged();
    }

    /** Devuelve la lista actual (copia inmutable). */
    public List<T> getItems() {
        List<T> copy = new ArrayList<>();
        for (int i = 0; i < getCount(); i++) copy.add(getItem(i));
        return Collections.unmodifiableList(copy);
    }

    @Override
    public @NonNull View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return initView(position, convertView, parent);
    }

    @Override
    public @NonNull View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return initView(position, convertView, parent);
    }

    private @NonNull View initView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = inflater.inflate(layoutResId, parent, false);
        }
        TextView tv = v.findViewById(textViewId);
        T item = getItem(position);
        tv.setText(item != null ? labeler.getLabel(item) : "");
        return v;
    }
}
