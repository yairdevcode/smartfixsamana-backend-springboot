package com.smartfixsamana.repositories;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.smartfixsamana.models.entities.RepairPart;

public interface IRepairPartRepository extends CrudRepository<RepairPart, Long> {

    List<RepairPart> findByRepairId(Long repairId);



}
