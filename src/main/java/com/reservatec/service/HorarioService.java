package com.reservatec.service;
import com.reservatec.entity.Horario;
import java.util.List;

public interface HorarioService {
    List<Horario> listarTodos();
    Horario guardar(Horario horario);
    void eliminar(Long id);
    Horario buscarPorId(Long id);
}
