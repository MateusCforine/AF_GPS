package com.example.gps;

public class LocalSalvo {

    private String id;
    private String nome;
    private String tipo;
    private double latitude;
    private double longitude;
    private String observacao;
    private String categoria;

    public LocalSalvo() {
        // Construtor vazio obrigatório para o Firebase
    }

    public LocalSalvo(String id, String nome, String tipo, double latitude, double longitude,
                      String observacao, String categoria) {
        this.id = id;
        this.nome = nome;
        this.tipo = tipo;
        this.latitude = latitude;
        this.longitude = longitude;
        this.observacao = observacao;
        this.categoria = categoria;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    @Override
    public String toString() {
        return nome + "\n" +
                "Tipo: " + tipo + "\n" +
                "Categoria: " + categoria + "\n" +
                "Coordenadas: " + latitude + ", " + longitude + "\n" +
                "Observação: " + observacao;
    }
}