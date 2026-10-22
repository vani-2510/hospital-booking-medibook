package com.medibook.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientIdOrderByAppointmentDateDescTimeSlotDesc(Long patientId);
    List<Appointment> findByDoctorIdOrderByAppointmentDateDescTimeSlotDesc(Long doctorId);
    boolean existsByDoctorIdAndAppointmentDateAndTimeSlotAndStatusNot(
            Long doctorId, LocalDate date, LocalTime time, AppointmentStatus status);
    Optional<Appointment> findByIdAndPatientId(Long id, Long patientId);
    Optional<Appointment> findByIdAndDoctorId(Long id, Long doctorId);
}