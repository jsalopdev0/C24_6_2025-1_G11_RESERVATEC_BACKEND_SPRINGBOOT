package com.reservatec.dto;

public class ReservasPorCarreraEspacioMesDTO {

    private String carrera;
    private String espacio;
    private int mes;        // 1 = Enero, 2 = Febrero, ...
    private long cantidad;

    public ReservasPorCarreraEspacioMesDTO(String carrera, String espacio, int mes, long cantidad) {
        this.carrera = carrera;
        this.espacio = espacio;
        this.mes = mes;
        this.cantidad = cantidad;
    }

    public String getCarrera() {
        return carrera;
    }

    public String getEspacio() {
        return espacio;
    }

    public int getMes() {
        return mes;
    }

    public long getCantidad() {
        return cantidad;
    }
}
