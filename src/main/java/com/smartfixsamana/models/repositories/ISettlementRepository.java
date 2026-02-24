package com.smartfixsamana.models.repositories;

import com.smartfixsamana.models.entities.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ISettlementRepository extends JpaRepository<Settlement, Long> {
}
