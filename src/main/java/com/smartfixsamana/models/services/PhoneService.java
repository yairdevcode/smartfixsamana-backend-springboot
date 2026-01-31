package com.smartfixsamana.models.services;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import com.smartfixsamana.models.entities.Phone;
import com.smartfixsamana.models.repositories.IPhoneRepository;

@Service
public class PhoneService {

    private final IPhoneRepository iPhoneRepository;

    public PhoneService(IPhoneRepository iPhoneRepository) {
        this.iPhoneRepository = iPhoneRepository;
    }
    public Long countAll() {
        return iPhoneRepository.count();
    }

    public List<Phone> findAll() {
        return (List<Phone>) iPhoneRepository.findAll();
    }

    public Optional<Phone> findById(@PathVariable Long id) {

        return iPhoneRepository.findById(id);

    }

    public Phone create(Phone phone) {

        return iPhoneRepository.save(phone);

    }

    public Phone update(Long phoneId, Phone phoneDetails) {

        Phone phone = iPhoneRepository.findById(phoneId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Celular no encontrado con el ID: " + phoneId));
        phone.setBrand(phoneDetails.getBrand());
        phone.setModel(phoneDetails.getModel());

        return iPhoneRepository.save(phone);
    }

    public void delete(Long phoneId) {

        Phone phone = iPhoneRepository.findById(phoneId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Celular no encontrado con el ID: " + phoneId));
        iPhoneRepository.delete(phone);
    }

    public Page<Phone> findByKeyword(String keyword, Pageable pageable) {
        return iPhoneRepository.findPhonesByKeyword(keyword, pageable);
    }

}
