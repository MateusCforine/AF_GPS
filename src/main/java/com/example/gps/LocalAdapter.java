package com.example.gps;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class LocalAdapter extends ArrayAdapter<LocalSalvo> {

    private Context context;
    private ArrayList<LocalSalvo> locais;

    public LocalAdapter(Context context, ArrayList<LocalSalvo> locais) {
        super(context, 0, locais);
        this.context = context;
        this.locais = locais;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
        }

        LocalSalvo local = locais.get(position);

        TextView txtTitulo = convertView.findViewById(android.R.id.text1);
        TextView txtSubtitulo = convertView.findViewById(android.R.id.text2);

        txtTitulo.setText(local.getNome());

        String texto = "Tipo: " + local.getTipo() +
                "\nCategoria: " + local.getCategoria() +
                "\nCoordenadas: " + local.getLatitude() + ", " + local.getLongitude() +
                "\nObservação: " + local.getObservacao();

        txtSubtitulo.setText(texto);

        return convertView;
    }
}