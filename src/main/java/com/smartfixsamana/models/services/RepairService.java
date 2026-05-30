package com.smartfixsamana.models.services;

import java.util.List;
import java.util.Optional;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.smartfixsamana.models.dto.RepairDTO;
import com.smartfixsamana.models.entities.Phone;
import com.smartfixsamana.models.entities.Customer;
import com.smartfixsamana.models.entities.Repair;
import com.smartfixsamana.models.repositories.IRepairRepository;

@Service
public class RepairService {

    private final IRepairRepository iRepairRepository;

    private final CustomerService customerService;

    private final PhoneService phoneService;

    public RepairService(IRepairRepository iRepairRepository, CustomerService customerService, PhoneService phoneService) {
        this.iRepairRepository = iRepairRepository;
        this.customerService = customerService;
        this.phoneService = phoneService;
    }
    public Long countAll() {
        return iRepairRepository.count();
    }

    public List<Repair> getAll() {
        return (List<Repair>) iRepairRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public Optional<Repair> getById(Long id) {
        return iRepairRepository.findById(id);
    }

    public Repair save(RepairDTO repairDTO) {
        Customer customer = customerService.findById(repairDTO.customerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        Phone phone = phoneService.findById(repairDTO.phoneId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Celular no encontrado"));

        Repair repair = new Repair();
        repair.setCustomer(customer);
        repair.setPhone(phone);
        repair.setFault(repairDTO.fault());
        repair.setState(repairDTO.state());
        repair.setDate(repairDTO.date());
        repair.setLaborCost(repairDTO.laborCost() != null ? repairDTO.laborCost() : 0.0);
        repair.recalculateTotalCost();

        return iRepairRepository.save(repair);
    }

    public Repair update(Long id, RepairDTO repairDTO) {
        Repair repair = iRepairRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reparación no encontrada"));

        Customer customer = customerService.findById(repairDTO.customerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        Phone phone = phoneService.findById(repairDTO.phoneId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Celular no encontrado"));

        repair.setCustomer(customer);
        repair.setPhone(phone);
        repair.setFault(repairDTO.fault());
        repair.setState(repairDTO.state());
        repair.setDate(repairDTO.date());
        repair.setLaborCost(repairDTO.laborCost() != null ? repairDTO.laborCost() : repair.getLaborCost());
        repair.recalculateTotalCost();

        return iRepairRepository.save(repair);
    }

    /**
     * Updates only the labor cost for a repair.
     */
    public Repair updateLaborCost(Long id, Double laborCost) {
        Repair repair = iRepairRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reparación no encontrada"));
        repair.setLaborCost(laborCost != null ? laborCost : 0.0);
        repair.recalculateTotalCost();
        return iRepairRepository.save(repair);
    }

    /**
     * Saves a repair entity directly.
     */
    public Repair save(Repair repair) {
        return iRepairRepository.save(repair);
    }

    public void delete(Long id) {
        Repair repair = iRepairRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Reparación no encontrada con el ID: " + id));
        iRepairRepository.delete(repair);
    }

    // Búsqueda paginada por palabra clave
    public Page<Repair> findByKeyword(String keyword, Pageable pageable) {
        return iRepairRepository.findRepairsByKeyword(keyword, pageable);
    }
}
