package com.reservatec.service.impl;
import com.reservatec.entity.Espacio;
import com.reservatec.repository.EspacioRepository;
import com.reservatec.service.EspacioService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EspacioServiceImpl implements EspacioService {

    private final EspacioRepository espacioRepository;

    public EspacioServiceImpl(EspacioRepository espacioRepository) {
        this.espacioRepository = espacioRepository;
    }

    @Override
    public List<Espacio> listarTodos() {
        return espacioRepository.findAll();
    }

    @Override
    public Espacio guardar(Espacio espacio) {
        return espacioRepository.save(espacio);
    }

    @Override
    public void eliminar(Long id) {
        espacioRepository.deleteById(id);
    }

    @Override
    public Espacio buscarPorId(Long id) {
        return espacioRepository.findById(id).orElse(null);
    }
}