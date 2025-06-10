package com.syos.adapter.out.DatabaseSpecificAdaptors.repository;

import com.syos.domain.model.Employee;
import com.syos.domain.exception.DatabaseOperationException;
import java.util.Optional;

public interface EmployeeRepository {
    Optional<Employee> findByUsername(String username) throws DatabaseOperationException;
}