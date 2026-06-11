package com.example.gps;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView txtLocalizacao;
    private Spinner spCategoriaBusca;
    private Spinner spCategoriaFinalidade;
    private EditText edtObservacao;
    private Button btnBuscar;
    private Button btnSalvar;
    private Button btnAtualizar;
    private Button btnLimpar;
    private ListView listLocaisApi;
    private ListView listLocaisSalvos;

    private ArrayList<LocalSalvo> listaLocaisApi = new ArrayList<>();
    private ArrayList<LocalSalvo> listaLocaisSalvos = new ArrayList<>();

    private LocalAdapter adapterApi;
    private LocalAdapter adapterSalvos;

    private FirebaseFirestore db;

    private double latitudeAtual = 0;
    private double longitudeAtual = 0;

    private LocalSalvo localSelecionadoApi = null;
    private LocalSalvo localSelecionadoSalvo = null;

    private static final int CODIGO_PERMISSAO_LOCALIZACAO = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iniciarComponentes();
        configurarSpinners();
        configurarAdapters();

        db = FirebaseFirestore.getInstance();

        solicitarPermissaoLocalizacao();

        btnBuscar.setOnClickListener(v -> buscarLocaisNaApi());

        btnSalvar.setOnClickListener(v -> salvarLocal());

        btnAtualizar.setOnClickListener(v -> atualizarLocalSalvo());

        btnLimpar.setOnClickListener(v -> limparCampos());

        listLocaisApi.setOnItemClickListener((parent, view, position, id) -> {
            localSelecionadoApi = listaLocaisApi.get(position);
            localSelecionadoSalvo = null;

            edtObservacao.setText("");

            Toast.makeText(
                    this,
                    "Local selecionado para salvar: " + localSelecionadoApi.getNome(),
                    Toast.LENGTH_LONG
            ).show();
        });

        listLocaisSalvos.setOnItemClickListener((parent, view, position, id) -> {
            localSelecionadoSalvo = listaLocaisSalvos.get(position);
            localSelecionadoApi = null;

            edtObservacao.setText(localSelecionadoSalvo.getObservacao());

            selecionarItemSpinner(spCategoriaFinalidade, localSelecionadoSalvo.getCategoria());

            Toast.makeText(
                    this,
                    "Local carregado para edição",
                    Toast.LENGTH_SHORT
            ).show();
        });

        listLocaisSalvos.setOnItemLongClickListener((parent, view, position, id) -> {
            LocalSalvo local = listaLocaisSalvos.get(position);
            confirmarExclusao(local);
            return true;
        });

        carregarLocaisSalvos();
    }

    private void iniciarComponentes() {
        txtLocalizacao = findViewById(R.id.txtLocalizacao);
        spCategoriaBusca = findViewById(R.id.spCategoriaBusca);
        spCategoriaFinalidade = findViewById(R.id.spCategoriaFinalidade);
        edtObservacao = findViewById(R.id.edtObservacao);
        btnBuscar = findViewById(R.id.btnBuscar);
        btnSalvar = findViewById(R.id.btnSalvar);
        btnAtualizar = findViewById(R.id.btnAtualizar);
        btnLimpar = findViewById(R.id.btnLimpar);
        listLocaisApi = findViewById(R.id.listLocaisApi);
        listLocaisSalvos = findViewById(R.id.listLocaisSalvos);
    }

    private void configurarSpinners() {
        String[] categoriasBusca = {
                "Farmácia",
                "Hospital",
                "Escola",
                "Restaurante",
                "Praça",
                "Mercado"
        };

        ArrayAdapter<String> adapterBusca = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categoriasBusca
        );

        adapterBusca.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategoriaBusca.setAdapter(adapterBusca);

        String[] categoriasFinalidade = {
                "Estudo",
                "Saúde",
                "Lazer",
                "Alimentação",
                "Compras",
                "Outros"
        };

        ArrayAdapter<String> adapterFinalidade = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categoriasFinalidade
        );

        adapterFinalidade.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategoriaFinalidade.setAdapter(adapterFinalidade);
    }

    private void configurarAdapters() {
        adapterApi = new LocalAdapter(this, listaLocaisApi);
        listLocaisApi.setAdapter(adapterApi);

        adapterSalvos = new LocalAdapter(this, listaLocaisSalvos);
        listLocaisSalvos.setAdapter(adapterSalvos);
    }

    private void solicitarPermissaoLocalizacao() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    CODIGO_PERMISSAO_LOCALIZACAO
            );

        } else {
            capturarLocalizacaoAtual();
        }
    }

    private void capturarLocalizacaoAtual() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        CODIGO_PERMISSAO_LOCALIZACAO
                );

                return;
            }

            LocationManager locationManager =
                    (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            if (locationManager == null) {
                Toast.makeText(
                        this,
                        "Serviço de localização indisponível",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (location != null) {
                latitudeAtual = location.getLatitude();
                longitudeAtual = location.getLongitude();

                mostrarEnderecoOuCoordenadas();

            } else {
                txtLocalizacao.setText(
                        "Não foi possível obter a localização. Ative o GPS e tente novamente."
                );

                Toast.makeText(
                        this,
                        "Localização não encontrada. No emulador, configure uma localização manual.",
                        Toast.LENGTH_LONG
                ).show();
            }

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Erro ao capturar localização: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void mostrarEnderecoOuCoordenadas() {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());

            List<Address> enderecos =
                    geocoder.getFromLocation(latitudeAtual, longitudeAtual, 1);

            if (enderecos != null && !enderecos.isEmpty()) {
                Address endereco = enderecos.get(0);
                String textoEndereco = endereco.getAddressLine(0);

                txtLocalizacao.setText(
                        "Localização atual:\n" +
                                textoEndereco + "\n\n" +
                                "Latitude: " + latitudeAtual + "\n" +
                                "Longitude: " + longitudeAtual
                );

            } else {
                txtLocalizacao.setText(
                        "Localização atual:\n" +
                                "Latitude: " + latitudeAtual + "\n" +
                                "Longitude: " + longitudeAtual
                );
            }

        } catch (Exception e) {
            txtLocalizacao.setText(
                    "Localização atual:\n" +
                            "Latitude: " + latitudeAtual + "\n" +
                            "Longitude: " + longitudeAtual
            );
        }
    }

    private void buscarLocaisNaApi() {
        if (latitudeAtual == 0 && longitudeAtual == 0) {
            Toast.makeText(
                    this,
                    "Localização ainda não capturada. Tentando capturar novamente...",
                    Toast.LENGTH_LONG
            ).show();

            capturarLocalizacaoAtual();
            return;
        }

        String categoria = spCategoriaBusca.getSelectedItem().toString();

        Toast.makeText(
                this,
                "Buscando locais próximos...",
                Toast.LENGTH_SHORT
        ).show();

        LocalApi.buscarLocaisProximos(
                latitudeAtual,
                longitudeAtual,
                categoria,
                new LocalApi.LocalApiCallback() {
                    @Override
                    public void onSucesso(ArrayList<LocalSalvo> locais) {
                        listaLocaisApi.clear();
                        listaLocaisApi.addAll(locais);
                        adapterApi.notifyDataSetChanged();

                        localSelecionadoApi = null;

                        if (locais.isEmpty()) {
                            Toast.makeText(
                                    MainActivity.this,
                                    "Nenhum local encontrado para essa categoria",
                                    Toast.LENGTH_LONG
                            ).show();
                        } else {
                            Toast.makeText(
                                    MainActivity.this,
                                    "Locais encontrados: " + locais.size() + ". Clique em um local para selecionar.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }

                    @Override
                    public void onErro(String erro) {
                        Toast.makeText(
                                MainActivity.this,
                                "Erro na API: " + erro,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void salvarLocal() {
        if (localSelecionadoApi == null) {
            Toast.makeText(
                    this,
                    "Primeiro clique em um local encontrado pela API antes de salvar",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String observacao = edtObservacao.getText().toString().trim();
        String categoria = spCategoriaFinalidade.getSelectedItem().toString();

        if (observacao.isEmpty()) {
            edtObservacao.setError("Digite uma observação");
            Toast.makeText(
                    this,
                    "Digite uma observação antes de salvar",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String id = db.collection("locais_salvos").document().getId();

        LocalSalvo local = new LocalSalvo(
                id,
                localSelecionadoApi.getNome(),
                localSelecionadoApi.getTipo(),
                localSelecionadoApi.getLatitude(),
                localSelecionadoApi.getLongitude(),
                observacao,
                categoria
        );

        db.collection("locais_salvos")
                .document(id)
                .set(local)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            this,
                            "Local salvo com sucesso!",
                            Toast.LENGTH_LONG
                    ).show();

                    limparCampos();
                    carregarLocaisSalvos();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            this,
                            "Erro ao salvar no Firebase: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void carregarLocaisSalvos() {
        db.collection("locais_salvos")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaLocaisSalvos.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        LocalSalvo local = document.toObject(LocalSalvo.class);

                        if (local.getId() == null || local.getId().isEmpty()) {
                            local.setId(document.getId());
                        }

                        listaLocaisSalvos.add(local);
                    }

                    adapterSalvos.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            this,
                            "Erro ao carregar locais salvos: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void atualizarLocalSalvo() {
        if (localSelecionadoSalvo == null) {
            Toast.makeText(
                    this,
                    "Selecione um local salvo para editar",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String observacao = edtObservacao.getText().toString().trim();
        String categoria = spCategoriaFinalidade.getSelectedItem().toString();

        if (observacao.isEmpty()) {
            edtObservacao.setError("Digite uma observação");
            Toast.makeText(
                    this,
                    "Digite uma observação antes de atualizar",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        localSelecionadoSalvo.setObservacao(observacao);
        localSelecionadoSalvo.setCategoria(categoria);

        db.collection("locais_salvos")
                .document(localSelecionadoSalvo.getId())
                .set(localSelecionadoSalvo)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            this,
                            "Local atualizado com sucesso!",
                            Toast.LENGTH_LONG
                    ).show();

                    limparCampos();
                    carregarLocaisSalvos();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            this,
                            "Erro ao atualizar no Firebase: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void confirmarExclusao(LocalSalvo local) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir local")
                .setMessage("Deseja excluir o local: " + local.getNome() + "?")
                .setPositiveButton("Sim", (dialog, which) -> excluirLocal(local))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void excluirLocal(LocalSalvo local) {
        db.collection("locais_salvos")
                .document(local.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            this,
                            "Local excluído com sucesso!",
                            Toast.LENGTH_LONG
                    ).show();

                    carregarLocaisSalvos();
                    limparCampos();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            this,
                            "Erro ao excluir no Firebase: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void limparCampos() {
        edtObservacao.setText("");
        localSelecionadoApi = null;
        localSelecionadoSalvo = null;
        spCategoriaFinalidade.setSelection(0);
    }

    private void selecionarItemSpinner(Spinner spinner, String valor) {
        if (valor == null) {
            return;
        }

        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();

        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equalsIgnoreCase(valor)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CODIGO_PERMISSAO_LOCALIZACAO) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                capturarLocalizacaoAtual();

            } else {
                txtLocalizacao.setText("Permissão de localização negada.");

                Toast.makeText(
                        this,
                        "Sem permissão não é possível usar o GPS",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }
}