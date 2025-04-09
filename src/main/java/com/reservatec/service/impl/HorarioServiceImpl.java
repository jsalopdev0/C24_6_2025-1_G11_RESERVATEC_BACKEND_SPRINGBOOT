package com.reservatec.service.impl;
import com.reservatec.entity.Horario;
import com.reservatec.repository.HorarioRepository;
import com.reservatec.service.HorarioService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class HorarioServiceImpl implements HorarioService {

    private final HorarioRepository horarioRepository;

    public HorarioServiceImpl(HorarioRepository horarioRepository) {
        this.horarioRepository = horarioRepository;
    }

    @Override
    public List<Horario> listarTodos() {
        return horarioRepository.findAll();
    }

    @Override
    public Horario guardar(Horario horario) {
        return horarioRepository.save(horario);
    }

    @Override
    public void eliminar(Long id) {
        horarioRepository.deleteById(id);
    }

    @Override
    public Horario buscarPorId(Long id) {
        return horarioRepository.findById(id).orElse(null);
    }
}