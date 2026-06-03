package com.example.gps;

import android.os.AsyncTask;
import android.location.Location;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class LocalApi {

    public interface LocalApiCallback {
        void onSucesso(ArrayList<LocalSalvo> locais);
        void onErro(String erro);
    }

    public static void buscarLocaisProximos(double latitude, double longitude, String categoria, LocalApiCallback callback) {
        new BuscarLocaisTask(latitude, longitude, categoria, callback).execute();
    }

    private static class BuscarLocaisTask extends AsyncTask<Void, Void, ArrayList<LocalSalvo>> {

        private double latitude;
        private double longitude;
        private String categoria;
        private LocalApiCallback callback;
        private String erro = "";

        public BuscarLocaisTask(double latitude, double longitude, String categoria, LocalApiCallback callback) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.categoria = categoria;
            this.callback = callback;
        }

        @Override
        protected ArrayList<LocalSalvo> doInBackground(Void... voids) {
            ArrayList<LocalSalvo> lista = new ArrayList<>();

            try {
                String tipoOsm = converterCategoriaParaOsm(categoria);

                String query =
                        "[out:json];" +
                                "(" +
                                "node[\"" + tipoOsm.split("=")[0] + "\"=\"" + tipoOsm.split("=")[1] + "\"](around:2000," + latitude + "," + longitude + ");" +
                                "way[\"" + tipoOsm.split("=")[0] + "\"=\"" + tipoOsm.split("=")[1] + "\"](around:2000," + latitude + "," + longitude + ");" +
                                "relation[\"" + tipoOsm.split("=")[0] + "\"=\"" + tipoOsm.split("=")[1] + "\"](around:2000," + latitude + "," + longitude + ");" +
                                ");" +
                                "out center;";

                URL url = new URL("https://overpass-api.de/api/interpreter");

                HttpURLConnection conexao = (HttpURLConnection) url.openConnection();
                conexao.setRequestMethod("POST");
                conexao.setDoOutput(true);
                conexao.setConnectTimeout(15000);
                conexao.setReadTimeout(15000);

                OutputStream os = conexao.getOutputStream();
                os.write(("data=" + query).getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conexao.getInputStream()));

                StringBuilder resposta = new StringBuilder();
                String linha;

                while ((linha = reader.readLine()) != null) {
                    resposta.append(linha);
                }

                reader.close();

                JSONObject jsonObject = new JSONObject(resposta.toString());
                JSONArray elementos = jsonObject.getJSONArray("elements");

                for (int i = 0; i < elementos.length(); i++) {
                    JSONObject item = elementos.getJSONObject(i);

                    double latLocal;
                    double lonLocal;

                    if (item.has("lat") && item.has("lon")) {
                        latLocal = item.getDouble("lat");
                        lonLocal = item.getDouble("lon");
                    } else if (item.has("center")) {
                        JSONObject center = item.getJSONObject("center");
                        latLocal = center.getDouble("lat");
                        lonLocal = center.getDouble("lon");
                    } else {
                        continue;
                    }

                    String nome = "Local sem nome";

                    if (item.has("tags")) {
                        JSONObject tags = item.getJSONObject("tags");

                        if (tags.has("name")) {
                            nome = tags.getString("name");
                        }
                    }

                    float[] resultadoDistancia = new float[1];
                    Location.distanceBetween(latitude, longitude, latLocal, lonLocal, resultadoDistancia);

                    double distanciaMetros = resultadoDistancia[0];
                    String tipo = categoria + " - " + String.format("%.0f", distanciaMetros) + " metros";

                    LocalSalvo local = new LocalSalvo(
                            "",
                            nome,
                            tipo,
                            latLocal,
                            lonLocal,
                            "",
                            ""
                    );

                    lista.add(local);
                }

            } catch (Exception e) {
                erro = e.getMessage();
            }

            return lista;
        }

        @Override
        protected void onPostExecute(ArrayList<LocalSalvo> locais) {
            if (erro == null || erro.isEmpty()) {
                callback.onSucesso(locais);
            } else {
                callback.onErro(erro);
            }
        }

        private String converterCategoriaParaOsm(String categoria) {
            switch (categoria) {
                case "Farmácia":
                    return "amenity=pharmacy";

                case "Hospital":
                    return "amenity=hospital";

                case "Escola":
                    return "amenity=school";

                case "Restaurante":
                    return "amenity=restaurant";

                case "Praça":
                    return "leisure=park";

                case "Mercado":
                    return "shop=supermarket";

                default:
                    return "amenity=restaurant";
            }
        }
    }
}