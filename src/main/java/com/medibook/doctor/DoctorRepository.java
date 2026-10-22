package com.medibook.doctor;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findByActiveTrueOrderByUserNameAsc();
    List<Doctor> findByActiveTrueAndDepartmentIgnoreCaseOrderByUserNameAsc(String department);
    Optional<Doctor> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select doctor from Doctor doctor where doctor.id = :id")
    Optional<Doctor> findByIdForBooking(@Param("id") Long id);
}