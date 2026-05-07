package com.smartfixsamana.models.services;

import java.util.List;
import java.util.Optional;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import com.smartfixsamana.models.entities.Customer;
import com.smartfixsamana.models.repositories.ICustomerRepository;

@Service
public class CustomerService {

    private final ICustomerRepository iCustomerRepository;

    public CustomerService(ICustomerRepository iCustomerRepository) {
        this.iCustomerRepository = iCustomerRepository;
    }

    public Long countAll() {
        return iCustomerRepository.count();
    }

    public List<Customer> findAll() {
        return (List<Customer>) iCustomerRepository.findAll();
    }
    

    public Optional<Customer> findById(@PathVariable Long id) {

        return iCustomerRepository.findById(id);

    }

    public Customer create(Customer customer) {

        return iCustomerRepository.save(customer);

    }

    public Customer update(Long customerId, Customer customerDetails) {

        Customer customer = iCustomerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cliente no encontrado con el ID: " + customerId));
        customer.setName(customerDetails.getName());
        customer.setLastname(customerDetails.getLastname());
        customer.setPhone(customerDetails.getPhone());
        customer.setEmail(customerDetails.getEmail());

        return iCustomerRepository.save(customer);
    }

    public void delete(Long customerId) {

        Customer customer = iCustomerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cliente no encontrado con el ID: " + customerId));
        iCustomerRepository.delete(customer);
    }

    public Page<Customer> findByKeyword(String keyword, Pageable pageable) {
        return iCustomerRepository.findCustomersByKeyword(keyword, pageable);
    }

}
