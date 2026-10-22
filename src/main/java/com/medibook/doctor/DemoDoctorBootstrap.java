package com.medibook.doctor;

import com.medibook.user.User;
import com.medibook.user.UserRepository;
import com.medibook.user.UserRole;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDoctorBootstrap implements CommandLineRunner {
    private static final List<String> AVAILABLE_DAYS =
            List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");
    private static final List<String> AVAILABLE_SLOTS =
            List.of("09:00", "10:00", "11:00", "13:00", "14:00", "15:00");
    private static final List<DemoDoctor> DEMO_DOCTORS = List.of(
            new DemoDoctor("Dr. Aarav Sharma", "demo.cardiology@medibook.test", "Cardiology",
                    "Cardiologist", "MBBS, MD Cardiology", 8, 800),
            new DemoDoctor("Dr. Ananya Rao", "demo.dermatology@medibook.test", "Dermatology",
                    "Dermatologist", "MBBS, MD Dermatology", 6, 700),
            new DemoDoctor("Dr. Kabir Mehta", "demo.orthopedics@medibook.test", "Orthopedics",
                    "Orthopedic surgeon", "MBBS, MS Orthopedics", 9, 900),
            new DemoDoctor("Dr. Diya Iyer", "demo.neurology@medibook.test", "Neurology",
                    "Neurologist", "MBBS, DM Neurology", 11, 1000),
            new DemoDoctor("Dr. Rohan Gupta", "demo.pediatrics@medibook.test", "Pediatrics",
                    "Pediatrician", "MBBS, MD Pediatrics", 7, 600),
            new DemoDoctor("Dr. Meera Nair", "demo.gynecology@medibook.test", "Gynecology",
                    "Gynecologist", "MBBS, MS Obstetrics and Gynecology", 8, 700),
            new DemoDoctor("Dr. Ishaan Patel", "demo.ophthalmology@medibook.test", "Ophthalmology",
                    "Ophthalmologist", "MBBS, MS Ophthalmology", 10, 600),
            new DemoDoctor("Dr. Sana Khan", "demo.ent@medibook.test", "ENT",
                    "ENT specialist", "MBBS, MS ENT", 7, 550),
            new DemoDoctor("Dr. Arjun Reddy", "demo.psychiatry@medibook.test", "Psychiatry",
                    "Psychiatrist", "MBBS, MD Psychiatry", 12, 800),
            new DemoDoctor("Dr. Kavya Menon", "demo.general@medibook.test", "General Medicine",
                    "General physician", "MBBS, MD General Medicine", 10, 500),
            new DemoDoctor("Dr. Vivaan Joshi", "doctor11@medibook.test", "Cardiology",
                    "Interventional cardiologist", "MBBS, DM Cardiology", 5, 950),
            new DemoDoctor("Dr. Tara Kapoor", "doctor12@medibook.test", "Dermatology",
                    "Clinical dermatologist", "MBBS, DDVL", 4, 650),
            new DemoDoctor("Dr. Neel Desai", "doctor13@medibook.test", "Orthopedics",
                    "Sports orthopedist", "MBBS, DNB Orthopedics", 6, 850),
            new DemoDoctor("Dr. Aditi Kulkarni", "doctor14@medibook.test", "Neurology",
                    "Clinical neurologist", "MBBS, MD, DM Neurology", 9, 1100),
            new DemoDoctor("Dr. Reyansh Das", "doctor15@medibook.test", "Pediatrics",
                    "Child health specialist", "MBBS, DCH", 5, 550),
            new DemoDoctor("Dr. Nisha Pillai", "doctor16@medibook.test", "Gynecology",
                    "Women's health specialist", "MBBS, DGO", 7, 750),
            new DemoDoctor("Dr. Omar Siddiqui", "doctor17@medibook.test", "Ophthalmology",
                    "Eye specialist", "MBBS, DOMS", 6, 600),
            new DemoDoctor("Dr. Pooja Sethi", "doctor18@medibook.test", "ENT",
                    "Otolaryngologist", "MBBS, DLO", 5, 500),
            new DemoDoctor("Dr. Dev Malhotra", "doctor19@medibook.test", "Psychiatry",
                    "Consultant psychiatrist", "MBBS, DPM", 8, 850),
            new DemoDoctor("Dr. Mira Bhat", "doctor20@medibook.test", "General Medicine",
                    "Internal medicine physician", "MBBS, DNB Medicine", 6, 550),
            new DemoDoctor("Dr. Arnav Chawla", "doctor21@medibook.test", "Cardiology",
                    "Cardiac physician", "MBBS, MD Medicine, DM Cardiology", 7, 900),
            new DemoDoctor("Dr. Riya Sen", "doctor22@medibook.test", "Dermatology",
                    "Dermatology consultant", "MBBS, MD Skin and VD", 8, 750),
            new DemoDoctor("Dr. Yash Verma", "doctor23@medibook.test", "Orthopedics",
                    "Joint care specialist", "MBBS, MS Orthopedics", 10, 950),
            new DemoDoctor("Dr. Leela Thomas", "doctor24@medibook.test", "Pediatrics",
                    "Pediatric consultant", "MBBS, MD Child Health", 9, 650),
            new DemoDoctor("Dr. Zoya Ali", "doctor25@medibook.test", "General Medicine",
                    "Primary care physician", "MBBS, DNB Family Medicine", 4, 450));

    private final UserRepository users;
    private final DoctorRepository doctors;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String password;

    public DemoDoctorBootstrap(UserRepository users, DoctorRepository doctors,
                               PasswordEncoder passwordEncoder,
                               @Value("${app.demo-doctors.enabled:false}") boolean enabled,
                               @Value("${app.demo-doctors.password:}") String password) {
        this.users = users;
        this.doctors = doctors;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) return;
        if (password.length() < 12 || password.length() > 72) {
            throw new IllegalStateException("When demo doctors are enabled, DEMO_DOCTOR_PASSWORD must be 12-72 characters.");
        }

        for (DemoDoctor demo : DEMO_DOCTORS) {
            User account = users.findByEmail(demo.email()).orElseGet(() -> users.save(new User(
                    demo.name(), demo.email(), passwordEncoder.encode(password), null, UserRole.DOCTOR)));
            if (account.getRole() != UserRole.DOCTOR) continue;
            account.updateName(demo.name());
            users.save(account);

            Doctor doctor = doctors.findByUserId(account.getId()).orElseGet(() -> new Doctor(account,
                    demo.department(), demo.specialization(), demo.qualifications(), demo.experienceYears(),
                    String.join(",", AVAILABLE_DAYS), String.join(",", AVAILABLE_SLOTS),
                    demo.consultationFee(), "Fictional sample profile for local booking and UI testing."));
            doctor.updateProfile(demo.department(), demo.specialization(), demo.qualifications(),
                    demo.experienceYears(), String.join(",", AVAILABLE_DAYS),
                    String.join(",", AVAILABLE_SLOTS), demo.consultationFee(),
                    "Fictional sample profile for local booking and UI testing.");
            doctors.save(doctor);
        }
    }

    private record DemoDoctor(String name, String email, String department, String specialization,
                              String qualifications, int experienceYears, double consultationFee) {}
}