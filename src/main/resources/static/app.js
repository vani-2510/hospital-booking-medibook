const state = {
  token: localStorage.getItem("medibook-token"),
  user: null,
  authMode: "login",
  loginMethod: null,
  page: "",
  selectedDoctor: null,
  doctorFilter: ""
};

const authView = document.querySelector("#auth-view");
const appView = document.querySelector("#app-view");
const content = document.querySelector("#app-content");
const toast = document.querySelector("#toast");

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, character => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  })[character]);
}

function formatInr(amount) {
  return new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(Number(amount) || 0);
}

function showToast(message, isError = false) {
  toast.textContent = message;
  toast.classList.toggle("error", isError);
  toast.classList.add("show");
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => toast.classList.remove("show"), 3400);
}

async function api(path, options = {}) {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  const response = await fetch(path, { ...options, headers });
  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (response.status === 401 && path !== "/api/auth/login") logout(false);
  if (!response.ok) throw new Error(data?.error || `Request failed (${response.status})`);
  return data;
}

function setAuthMode(mode) {
  state.authMode = mode;
  const registering = mode === "register";
  const backButton = document.querySelector("#auth-back-button");
  backButton.classList.toggle("hidden", !registering && state.loginMethod === null);
  backButton.querySelector("span").textContent = registering ? "Back to sign in" : "Back to login options";
  document.querySelector("#email-login-option").classList.toggle("hidden", registering || state.loginMethod !== null);
  document.querySelector("#auth-form").classList.toggle("hidden", !registering && state.loginMethod === null);
  document.querySelector("#auth-title").textContent = registering ? "Create your account" : "Sign in to MediBook";
  document.querySelector("#auth-eyebrow").textContent = registering ? "YOUR CARE, ORGANIZED" : "WELCOME BACK";
  document.querySelector("#auth-subtitle").textContent = registering
    ? "Create an account to find a doctor and book a visit."
    : "Manage your care and upcoming visits.";
  document.querySelector("#auth-submit").textContent = registering ? "Create account" : "Sign in";
  document.querySelector("#auth-switch-copy").textContent = registering ? "Already have an account?" : "New to MediBook?";
  document.querySelector("#auth-switch").textContent = registering ? "Sign in" : "Create an account";
  document.querySelector("#name-field").classList.toggle("hidden", !registering);
  document.querySelector("#account-type-field").classList.toggle("hidden", !registering);
  document.querySelector("#phone-field").classList.toggle("hidden", !registering);
  document.querySelector("#confirm-password-field").classList.toggle("hidden", !registering);
  document.querySelector('[name="name"]').required = registering;
  document.querySelector('[name="phone"]').required = registering;
  const confirmPassword = document.querySelector('[name="confirmPassword"]');
  confirmPassword.required = registering;
  for (const name of ["password", "confirmPassword"]) {
    const input = document.querySelector(`[name="${name}"]`);
    const toggle = document.querySelector(`[data-toggle-password="${name}"]`);
    input.type = "password";
    toggle.setAttribute("aria-pressed", "false");
    toggle.querySelector(".icon-show-password").classList.remove("hidden");
    toggle.querySelector(".icon-hide-password").classList.add("hidden");
  }
  const email = document.querySelector('[name="email"]');
  const password = document.querySelector('[name="password"]');
  const loginPhone = document.querySelector('[name="loginPhone"]');
  email.value = "";
  password.value = "";
  loginPhone.value = "";
  document.querySelector("#email-login-field").classList.toggle("hidden", !registering && state.loginMethod === "phone");
  document.querySelector("#phone-login-field").classList.toggle("hidden", registering || state.loginMethod !== "phone");
  email.required = registering || state.loginMethod === "email";
  loginPhone.required = !registering && state.loginMethod === "phone";
  if (registering) {
    email.autocomplete = "email";
    password.autocomplete = "new-password";
  } else {
    email.autocomplete = "off";
    password.autocomplete = "off";
  }
  const department = document.querySelector('#doctor-registration-fields [name="department"]');
  department.innerHTML = '<option value="">Choose a department</option>' + departmentOptions("");
  syncDoctorRegistrationFields();
}

function syncDoctorRegistrationFields() {
  const isDoctor = state.authMode === "register"
    && document.querySelector('[name="accountType"]').value === "DOCTOR";
  const fields = document.querySelector("#doctor-registration-fields");
  fields.classList.toggle("hidden", !isDoctor);
  for (const name of ["department", "specialization"]) {
    fields.querySelector(`[name="${name}"]`).required = isDoctor;
  }
  const password = document.querySelector('#auth-form [name="password"]');
  password.minLength = isDoctor ? 12 : 8;
  password.placeholder = isDoctor ? "At least 12 characters" : "At least 8 characters";
  document.querySelector('[name="confirmPassword"]').minLength = password.minLength;
}

function rolePages() {
  if (state.user?.role === "ADMIN") return [
    ["admin-home", "Overview"], ["admin-patient-dashboard", "Patient dashboard"],
    ["admin-doctor-dashboard", "Doctor dashboard"], ["admin-doctors", "Doctors"],
    ["admin-doctor-requests", "Doctor requests"], ["admin-appointments", "Appointments"],
    ["admin-patients", "Patients"]
  ];
  if (state.user?.role === "DOCTOR") return [
    ["doctor-home", "Overview"], ["doctor-patients", "Patients"], ["doctor-schedule", "Schedule"]
  ];
  if (state.user?.role === "PATIENT") return [
    ["patient-home", "Overview"], ["patient-doctors", "Find a doctor"], ["patient-appointments", "My visits"]
  ];
  return [];
}

function enterApp(user) {
  state.user = user;
  const pages = rolePages();
  if (!pages.length) {
    logout(false);
    showToast("This account does not have access to a dashboard.", true);
    return;
  }
  closeProfile();
  authView.classList.add("hidden");
  appView.classList.remove("hidden");
  document.querySelector("#user-name").textContent = user.name;
  if (!pages.some(([page]) => page === state.page)) state.page = pages[0][0];
  document.querySelector("#main-nav").innerHTML = pages.map(([page, title]) =>
    `<button class="nav-link ${state.page === page ? "active" : ""}" data-page="${page}" type="button">${title}</button>`
  ).join("");
  renderPage();
}

function logout(notify = true) {
  closeProfile();
  localStorage.removeItem("medibook-token");
  state.token = null;
  state.user = null;
  state.page = "";
  appView.classList.add("hidden");
  authView.classList.remove("hidden");
  state.loginMethod = null;
  setAuthMode("login");
  if (notify) showToast("You are signed out.");
}

function closeProfile() {
  const button = document.querySelector("#profile-button");
  const panel = document.querySelector("#profile-panel");
  if (panel.classList.contains("hidden")) return;
  panel.classList.add("hidden");
  document.body.classList.remove("profile-open");
  button.setAttribute("aria-expanded", "false");
  button.focus();
}

function profileDetails(profile) {
  const createdAt = profile.createdAt ? new Date(profile.createdAt).toLocaleDateString() : "Not available";
  const details = [
    ["Full name", profile.name],
    ["Email address", profile.email],
    ["Phone number", profile.phone || "Not provided"],
    ["Account type", profile.role],
    ["Member since", createdAt],
    ["Profile ID", profile.id]
  ];
  return `<dl>${details.map(([label, value]) => `<div class="profile-detail"><dt>${label}</dt><dd>${escapeHtml(value)}</dd></div>`).join("")}</dl>`;
}

function pageHeading(kicker, title, description = "") {
  return `<div class="page-heading"><div><p class="eyebrow">${escapeHtml(kicker)}</p><h1>${escapeHtml(title)}</h1>${description ? `<p>${escapeHtml(description)}</p>` : ""}</div></div>`;
}

function emptyState(title, detail) {
  return `<div class="empty-state"><strong>${escapeHtml(title)}</strong>${escapeHtml(detail)}</div>`;
}

function statusTag(status) {
  return `<span class="status status-${escapeHtml(status)}">${escapeHtml(status)}</span>`;
}

async function renderPage() {
  const pages = rolePages();
  if (!pages.length) {
    logout(false);
    return;
  }
  if (!pages.some(([page]) => page === state.page)) state.page = pages[0][0];
  document.querySelectorAll(".nav-link").forEach(link => link.classList.toggle("active", link.dataset.page === state.page));
  content.innerHTML = '<div class="loading">Loading your workspace...</div>';
  try {
    if (state.page === "patient-home") return await renderPatientHome();
    if (state.page === "patient-doctors") return await renderPatientDoctors();
    if (state.page === "patient-appointments") return await renderPatientAppointments();
    if (state.page === "doctor-home") return await renderDoctorHome();
    if (state.page === "doctor-patients") return await renderDoctorPatients();
    if (state.page === "doctor-schedule") return await renderDoctorSchedule();
    if (state.page === "admin-home") return await renderAdminHome();
    if (state.page === "admin-patient-dashboard") return await renderAdminPatientDashboard();
    if (state.page === "admin-doctor-dashboard") return await renderAdminDoctorDashboard();
    if (state.page === "admin-doctors") return await renderAdminDoctors();
    if (state.page === "admin-doctor-requests") return await renderAdminDoctorRequests();
    if (state.page === "admin-appointments") return await renderAdminAppointments();
    if (state.page === "admin-patients") return await renderAdminPatients();
  } catch (error) {
    content.innerHTML = `${pageHeading("WORKSPACE", "We couldn't load this view")}<div class="panel">${emptyState("Please try again", error.message)}</div>`;
  }
}

async function renderPatientHome() {
  const [appointments, doctors] = await Promise.all([api("/api/appointments/mine"), api("/api/doctors")]);
  const upcoming = appointments.filter(item => item.status !== "CANCELLED" && item.status !== "COMPLETED")
    .sort((a, b) => `${a.date} ${a.timeSlot}`.localeCompare(`${b.date} ${b.timeSlot}`)).slice(0, 3);
  content.innerHTML = `${pageHeading("PATIENT OVERVIEW", `Good to see you, ${state.user.name.split(" ")[0]}`, "Keep your appointments and care team close at hand.")}
    <section class="metric-grid">
      <article class="metric"><div class="metric-label">Upcoming visits</div><div class="metric-value">${upcoming.length}</div><div class="metric-accent">On your calendar</div></article>
      <article class="metric"><div class="metric-label">Available doctors</div><div class="metric-value">${doctors.length}</div><div class="metric-accent">Ready to see patients</div></article>
      <article class="metric"><div class="metric-label">Past visits</div><div class="metric-value">${appointments.filter(item => item.status === "COMPLETED").length}</div><div class="metric-accent">Your visit history</div></article>
      <article class="metric"><div class="metric-label">Care status</div><div class="metric-value" style="font-size:20px">Up to date</div><div class="metric-accent">Your profile is active</div></article>
    </section>
    <div class="section-title"><div><h2>Next on your calendar</h2><p>Your upcoming appointments</p></div><button class="button button-primary" data-page="patient-doctors" type="button">Find a doctor</button></div>
    <div class="panel">${upcoming.length ? appointmentTable(upcoming, true) : emptyState("No upcoming visits", "Browse doctors to find a time that works for you.")}</div>`;
}

async function renderPatientDoctors() {
  const query = state.doctorFilter ? `?department=${encodeURIComponent(state.doctorFilter)}` : "";
  const doctors = await api(`/api/doctors${query}`);
  content.innerHTML = `${pageHeading("CARE TEAM", "Find your doctor", "Browse available specialists and book a visit.")}
    <div class="filter-row"><select id="department-filter" aria-label="Filter by department"><option value="">All departments</option>${departmentOptions(state.doctorFilter)}</select><span class="muted">${doctors.length} available</span></div>
    <div class="doctor-grid">${doctors.length ? doctors.map(doctorCard).join("") : emptyState("No doctors listed yet", "Your care team will appear here when an administrator adds doctors.")}</div>
    ${state.selectedDoctor ? bookingPanel(state.selectedDoctor) : ""}`;
  document.querySelector("#department-filter").value = state.doctorFilter;
}

function departmentOptions(selected) {
  return ["Cardiology", "Dermatology", "Orthopedics", "Neurology", "Pediatrics", "Gynecology", "Ophthalmology", "ENT", "Psychiatry", "General Medicine"]
    .map(name => `<option ${selected === name ? "selected" : ""} value="${name}">${name}</option>`).join("");
}

function doctorCard(doctor) {
  return `<article class="panel doctor-card"><div class="doctor-top"><div class="avatar">${escapeHtml(doctor.name.split(" ").map(part => part[0]).slice(0, 2).join(""))}</div><div><h3 class="doctor-name">${escapeHtml(doctor.name)}</h3><div class="doctor-specialty">${escapeHtml(doctor.specialization)}</div></div></div>
    <div class="doctor-meta"><span>${escapeHtml(doctor.department)}</span><span>${doctor.experienceYears} yrs experience</span></div>
    <p class="doctor-bio">${escapeHtml(doctor.bio || doctor.qualifications || "Experienced specialist accepting appointments.")}</p>
    <div class="doctor-meta"><span>${escapeHtml(doctor.availableDays.join(", "))}</span><strong>${formatInr(doctor.consultationFee)}</strong></div>
    <button class="button button-primary" data-action="select-doctor" data-id="${doctor.id}" type="button">Choose a time</button></article>`;
}

function bookingPanel(doctor) {
  const minDate = new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10);
  return `<section id="booking-panel" class="panel booking-form" style="margin-top:22px"><div><p class="eyebrow">BOOK A VISIT</p><h2>Appointment with ${escapeHtml(doctor.name)}</h2><p class="muted">${escapeHtml(doctor.department)} · ${formatInr(doctor.consultationFee)} consultation</p></div>
    <form id="booking-form" class="form-stack"><input type="hidden" name="doctorId" value="${doctor.id}"><div class="form-row"><label class="field">Date<input name="date" type="date" min="${minDate}" required></label><label class="field">Available time<select name="timeSlot" required><option value="">Choose a time</option>${doctor.availableSlots.map(slot => `<option value="${escapeHtml(slot)}">${escapeHtml(slot)}</option>`).join("")}</select></label></div>
    <label class="field">What brings you in?<textarea name="symptoms" maxlength="2000" required placeholder="Share a short note to help your doctor prepare"></textarea></label><div id="triage-result" class="triage-box"></div><div class="form-actions"><button class="button button-primary" type="submit">Confirm appointment</button><button class="button button-quiet" data-action="triage" type="button">Ask AI for guidance</button><button class="button button-quiet" data-action="clear-doctor" type="button">Close</button></div></form></section>`;
}

async function renderPatientAppointments() {
  const appointments = await api("/api/appointments/mine");
  content.innerHTML = `${pageHeading("YOUR VISITS", "Appointments", "Review and manage your booked visits.")}
    <div class="panel">${appointments.length ? appointmentTable(appointments, true) : emptyState("No appointments yet", "When you book a visit, it will appear here.")}</div>`;
}

function appointmentTable(items, patientActions = false, includePatient = false) {
  return `<div class="table-wrap"><table><thead><tr><th>Visit</th>${includePatient ? "<th>Patient</th>" : ""}<th>Doctor</th><th>Department</th><th>Reason</th><th>Status</th>${patientActions ? "<th></th>" : ""}</tr></thead><tbody>${items.map(item => `<tr>
    <td>${escapeHtml(item.date)}<br><span class="muted">${escapeHtml(item.timeSlot)}</span></td>${includePatient ? `<td>${escapeHtml(item.patientName)}<br><span class="muted">${escapeHtml(item.patientEmail)}</span></td>` : ""}<td>${escapeHtml(item.doctorName)}</td><td>${escapeHtml(item.department)}</td>
    <td>${escapeHtml(item.symptoms)}</td><td>${statusTag(item.status)}</td>${patientActions ? `<td>${item.status === "PENDING" || item.status === "CONFIRMED" ? `<button class="button button-danger button-small" data-action="cancel-appointment" data-id="${item.id}" type="button">Cancel</button>` : ""}</td>` : ""}</tr>`).join("")}</tbody></table></div>`;
}

async function renderDoctorHome() {
  const appointments = await api("/api/appointments/doctor");
  const activeVisits = appointments.filter(item => item.status === "PENDING" || item.status === "CONFIRMED");
  const pending = appointments.filter(item => item.status === "PENDING");
  const completed = appointments.filter(item => item.status === "COMPLETED");
  const patientCount = new Set(appointments.map(item => item.patientId)).size;
  const upcoming = activeVisits.slice().sort((a, b) => `${a.date} ${a.timeSlot}`.localeCompare(`${b.date} ${b.timeSlot}`)).slice(0, 5);
  content.innerHTML = `${pageHeading("DOCTOR WORKSPACE", `Welcome, ${state.user.name.split(" ")[0]}`, "Your appointments and patients in one place.")}
    <section class="metric-grid"><article class="metric"><div class="metric-label">Active appointments</div><div class="metric-value">${activeVisits.length}</div><div class="metric-accent">Pending or confirmed</div></article><article class="metric"><div class="metric-label">Awaiting action</div><div class="metric-value">${pending.length}</div><div class="metric-accent">Patient requests</div></article><article class="metric"><div class="metric-label">Completed visits</div><div class="metric-value">${completed.length}</div><div class="metric-accent">Your visit history</div></article><article class="metric"><div class="metric-label">Total patients</div><div class="metric-value">${patientCount}</div><div class="metric-accent">All assigned bookings</div></article></section>
    <div class="section-title"><div><h2>Upcoming patient visits</h2><p>Your next bookings</p></div><button class="button button-primary" data-page="doctor-patients" type="button">View patients</button></div>
    <div class="panel">${upcoming.length ? doctorAppointmentTable(upcoming, false) : emptyState("No patient bookings yet", "New appointments for you will appear here.")}</div>`;
}

async function renderDoctorPatients() {
  const appointments = await api("/api/appointments/doctor");
  content.innerHTML = `${pageHeading("DOCTOR WORKSPACE", "Patients", "Patients who have booked appointments with you.")}
    <div class="panel">${appointments.length ? doctorAppointmentTable(appointments, true) : emptyState("No patients yet", "Patients who book an appointment with you will appear here.")}</div>`;
}

function doctorAppointmentTable(appointments, includeActions) {
  return `<div class="table-wrap"><table><thead><tr><th>Visit</th><th>Patient</th><th>Contact</th><th>Reason</th><th>Status</th>${includeActions ? "<th>Update</th>" : ""}</tr></thead><tbody>${appointments.map(item => `<tr>
    <td>${escapeHtml(item.date)}<br><span class="muted">${escapeHtml(item.timeSlot)}</span></td><td>${escapeHtml(item.patientName)}</td><td>${escapeHtml(item.patientEmail)}<br><span class="muted">${escapeHtml(item.patientPhone || "No phone")}</span></td><td>${escapeHtml(item.symptoms)}</td><td>${statusTag(item.status)}</td>
    ${includeActions ? `<td><div class="table-actions">${item.status === "PENDING" ? `<button class="button button-primary button-small" data-action="set-status" data-status="CONFIRMED" data-id="${item.id}" type="button">Confirm</button>` : ""}${item.status === "CONFIRMED" ? `<button class="button button-quiet button-small" data-action="set-status" data-status="COMPLETED" data-id="${item.id}" type="button">Complete</button>` : ""}${item.status !== "PENDING" && item.status !== "CONFIRMED" ? "—" : ""}</div></td>` : ""}</tr>`).join("")}</tbody></table></div>`;
}

async function renderDoctorSchedule() {
  const doctor = await api("/api/doctors/me");
  const days = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];
  const selectedDays = new Set(doctor.availableDays);
  content.innerHTML = `${pageHeading("DOCTOR WORKSPACE", "Your schedule", "Set the days and appointment times patients can book.")}
    <form id="schedule-form" class="panel booking-form"><h2>${escapeHtml(doctor.name)}</h2><label class="field">Available days<div class="filter-row">${days.map(day => `<label><input type="checkbox" name="availableDays" value="${day}" ${selectedDays.has(day) ? "checked" : ""}> ${day.slice(0, 3)}</label>`).join("")}</div></label>
    <label class="field">Available time slots<input name="availableSlots" value="${escapeHtml(doctor.availableSlots.join(", "))}" placeholder="09:00, 09:30, 10:00" required><span class="muted">Enter 24-hour times separated by commas.</span></label>
    <button class="button button-primary" type="submit">Save schedule</button></form>`;
}

async function renderAdminHome() {
  const stats = await api("/api/admin/stats");
  const visits = await api("/api/admin/appointments");
  content.innerHTML = `${pageHeading("ADMINISTRATION", "Hospital overview", "Manage doctors, patients, and appointment activity.")}
    <section class="metric-grid"><article class="metric"><div class="metric-label">Registered users</div><div class="metric-value">${stats.users}</div><div class="metric-accent">All account roles</div></article><article class="metric"><div class="metric-label">Doctors</div><div class="metric-value">${stats.doctors}</div><div class="metric-accent">Care team profiles</div></article><article class="metric"><div class="metric-label">Appointments</div><div class="metric-value">${stats.appointments}</div><div class="metric-accent">All-time bookings</div></article><article class="metric"><div class="metric-label">Pending</div><div class="metric-value">${stats.pendingAppointments}</div><div class="metric-accent">Awaiting confirmation</div></article></section>
    <div class="section-title"><div><h2>Recent appointments</h2><p>Latest booking activity</p></div><button class="button button-quiet" data-page="admin-appointments" type="button">View all</button></div><div class="panel">${visits.length ? appointmentTable(visits.slice(-8).reverse()) : emptyState("No appointments yet", "Bookings will show here.")}</div>`;
}

async function renderAdminPatientDashboard() {
  const [patients, appointments, doctors] = await Promise.all([
    api("/api/admin/patients"), api("/api/admin/appointments"), api("/api/doctors")
  ]);
  const activeVisits = appointments.filter(item => item.status === "PENDING" || item.status === "CONFIRMED");
  const completedVisits = appointments.filter(item => item.status === "COMPLETED");
  const upcoming = activeVisits.slice().sort((a, b) => `${a.date} ${a.timeSlot}`.localeCompare(`${b.date} ${b.timeSlot}`));
  content.innerHTML = `${pageHeading("PATIENT DASHBOARD · ADMIN VIEW", "Patient care overview", "Review patient activity and the doctors available for booking.") }
    <section class="metric-grid">
      <article class="metric"><div class="metric-label">Patient accounts</div><div class="metric-value">${patients.length}</div><div class="metric-accent">Registered patients</div></article>
      <article class="metric"><div class="metric-label">Available doctors</div><div class="metric-value">${doctors.length}</div><div class="metric-accent">Ready to see patients</div></article>
      <article class="metric"><div class="metric-label">Active visits</div><div class="metric-value">${activeVisits.length}</div><div class="metric-accent">Pending or confirmed</div></article>
      <article class="metric"><div class="metric-label">Completed visits</div><div class="metric-value">${completedVisits.length}</div><div class="metric-accent">Patient visit history</div></article>
    </section>
    <div class="section-title"><div><h2>Active patient appointments</h2><p>Pending and confirmed bookings across patients</p></div><button class="button button-quiet" data-page="admin-patients" type="button">View patients</button></div>
    <div class="panel">${upcoming.length ? appointmentTable(upcoming, false, true) : emptyState("No active visits", "Patient bookings will appear here.")}</div>`;
}

async function renderAdminDoctorDashboard() {
  const [doctors, appointments] = await Promise.all([
    api("/api/admin/doctors"), api("/api/admin/appointments")
  ]);
  const activeVisits = appointments.filter(item => item.status === "PENDING" || item.status === "CONFIRMED");
  const pendingVisits = appointments.filter(item => item.status === "PENDING");
  const completedVisits = appointments.filter(item => item.status === "COMPLETED");
  const doctorAppointments = appointments.slice().sort((a, b) => `${a.date} ${a.timeSlot}`.localeCompare(`${b.date} ${b.timeSlot}`));
  content.innerHTML = `${pageHeading("DOCTOR DASHBOARD · ADMIN VIEW", "Doctor workspace overview", "Review provider profiles and appointments across the care team.")}
    <section class="metric-grid">
      <article class="metric"><div class="metric-label">Doctor profiles</div><div class="metric-value">${doctors.length}</div><div class="metric-accent">Approved providers</div></article>
      <article class="metric"><div class="metric-label">Active visits</div><div class="metric-value">${activeVisits.length}</div><div class="metric-accent">Pending or confirmed</div></article>
      <article class="metric"><div class="metric-label">Awaiting action</div><div class="metric-value">${pendingVisits.length}</div><div class="metric-accent">Patient requests</div></article>
      <article class="metric"><div class="metric-label">Completed visits</div><div class="metric-value">${completedVisits.length}</div><div class="metric-accent">Doctor visit history</div></article>
    </section>
    <div class="section-title"><div><h2>Doctor appointments</h2><p>Bookings across all doctors</p></div><button class="button button-quiet" data-page="admin-doctors" type="button">Manage doctors</button></div>
    <div class="panel">${doctorAppointments.length ? adminDoctorAppointmentTable(doctorAppointments) : emptyState("No appointments yet", "Bookings will appear here.")}</div>`;
}

function adminDoctorAppointmentTable(appointments) {
  return `<div class="table-wrap"><table><thead><tr><th>Visit</th><th>Doctor</th><th>Patient</th><th>Reason</th><th>Status</th></tr></thead><tbody>${appointments.map(item => `<tr>
    <td>${escapeHtml(item.date)}<br><span class="muted">${escapeHtml(item.timeSlot)}</span></td><td>${escapeHtml(item.doctorName)}<br><span class="muted">${escapeHtml(item.department)}</span></td><td>${escapeHtml(item.patientName)}<br><span class="muted">${escapeHtml(item.patientEmail)}</span></td><td>${escapeHtml(item.symptoms)}</td><td>${statusTag(item.status)}</td>
  </tr>`).join("")}</tbody></table></div>`;
}

async function renderAdminDoctors() {
  const doctors = await api("/api/admin/doctors");
  content.innerHTML = `${pageHeading("ADMINISTRATION", "Doctor directory", "Add specialists and manage the public directory.")}
    <div class="split-layout"><form id="doctor-form" class="panel booking-form"><h2>Add a doctor</h2><div class="form-row"><label class="field">Full name<input name="name" required maxlength="120"></label><label class="field">Email<input name="email" type="email" required maxlength="190"></label></div>
    <div class="form-row"><label class="field">Initial password<input name="password" type="password" minlength="12" required maxlength="72"></label><label class="field">Phone<input name="phone" maxlength="30"></label></div>
    <div class="form-row"><label class="field">Department<select name="department" required>${departmentOptions("")}</select></label><label class="field">Specialization<input name="specialization" required maxlength="120"></label></div>
    <div class="form-row"><label class="field">Qualifications<input name="qualifications" maxlength="200"></label><label class="field">Years of experience<input name="experienceYears" type="number" min="0" value="0" required></label></div>
    <div class="form-row"><label class="field">Consultation fee (₹)<input name="consultationFee" type="number" min="0" step="0.01" value="0" required></label><label class="field">Available days<input name="availableDays" value="Monday,Tuesday,Wednesday,Thursday,Friday" required></label></div>
    <label class="field">Available times<input name="availableSlots" value="09:00,09:30,10:00,10:30,11:00,11:30,13:00,13:30,14:00,14:30,15:00" required></label><label class="field">Short biography<textarea name="bio" maxlength="1000"></textarea></label><button class="button button-primary" type="submit">Add doctor</button></form>
    <div class="panel"><div class="panel-pad"><h2 class="panel-heading">Listed doctors</h2></div>${doctors.length ? `<div class="table-wrap"><table><thead><tr><th>Name</th><th>Department</th><th>Fee (₹)</th><th>Status</th><th></th></tr></thead><tbody>${doctors.map(doctor => `<tr><td>${escapeHtml(doctor.name)}<br><span class="muted">${escapeHtml(doctor.email)}</span></td><td>${escapeHtml(doctor.department)}</td><td>${formatInr(doctor.consultationFee)}</td><td>${doctor.active ? "Active" : "Inactive"}</td><td><button class="button button-quiet button-small" data-action="toggle-doctor" data-active="${!doctor.active}" data-id="${doctor.id}" type="button">${doctor.active ? "Deactivate" : "Activate"}</button></td></tr>`).join("")}</tbody></table></div>` : emptyState("No doctors yet", "Add a doctor using the form.")}</div></div>`;
}

async function renderAdminDoctorRequests() {
  const requests = await api("/api/admin/doctor-requests");
  content.innerHTML = `${pageHeading("ADMINISTRATION", "Doctor requests", "Review doctor accounts before they can sign in or appear in the directory.")}
    <div class="doctor-request-list">${requests.length ? requests.map(doctor => `<article class="panel doctor-request-card">
      <div class="doctor-request-heading"><div><p class="eyebrow">${escapeHtml(doctor.department)}</p><h2>${escapeHtml(doctor.name)}</h2><p class="muted">${escapeHtml(doctor.email)} · ${escapeHtml(doctor.phone || "No phone")}</p></div><span class="status status-PENDING">Pending approval</span></div>
      <div class="doctor-request-details"><div><span>Specialization</span><strong>${escapeHtml(doctor.specialization)}</strong></div><div><span>Qualifications</span><strong>${escapeHtml(doctor.qualifications || "Not provided")}</strong></div><div><span>Experience</span><strong>${doctor.experienceYears} years</strong></div><div><span>Consultation fee</span><strong>${formatInr(doctor.consultationFee)}</strong></div><div><span>Available days</span><strong>${escapeHtml(doctor.availableDays.join(", "))}</strong></div><div><span>Available times</span><strong>${escapeHtml(doctor.availableSlots.join(", "))}</strong></div></div>
      ${doctor.bio ? `<p class="doctor-request-bio">${escapeHtml(doctor.bio)}</p>` : ""}
      <div class="form-actions"><button class="button button-primary" data-action="approve-doctor-request" data-id="${doctor.id}" type="button">Approve doctor</button><button class="button button-danger" data-action="reject-doctor-request" data-id="${doctor.id}" type="button">Reject request</button></div>
    </article>`).join("") : emptyState("No pending requests", "New doctor signups will appear here for review.")}</div>`;
}

async function renderAdminAppointments() {
  const visits = await api("/api/admin/appointments");
  content.innerHTML = `${pageHeading("ADMINISTRATION", "All appointments", "Review appointment history across the hospital.")}
    <div class="panel">${visits.length ? `<div class="table-wrap"><table><thead><tr><th>Visit</th><th>Patient</th><th>Doctor</th><th>Department</th><th>Reason</th><th>Status</th><th>Manage</th></tr></thead><tbody>${visits.slice().reverse().map(item => `<tr>
    <td>${escapeHtml(item.date)}<br><span class="muted">${escapeHtml(item.timeSlot)}</span></td><td>${escapeHtml(item.patientName)}<br><span class="muted">${escapeHtml(item.patientEmail)} · ${escapeHtml(item.patientPhone || "No phone")}</span></td><td>${escapeHtml(item.doctorName)}</td><td>${escapeHtml(item.department)}</td><td>${escapeHtml(item.symptoms)}</td><td>${statusTag(item.status)}</td>
    <td><div class="table-actions">${item.status === "PENDING" ? `<button class="button button-primary button-small" data-action="set-status" data-status="CONFIRMED" data-id="${item.id}" type="button">Approve</button>` : ""}${item.status === "CONFIRMED" ? `<button class="button button-primary button-small" data-action="set-status" data-status="COMPLETED" data-id="${item.id}" type="button">Complete</button>` : ""}${item.status === "PENDING" || item.status === "CONFIRMED" ? `<button class="button button-danger button-small" data-action="set-status" data-status="CANCELLED" data-id="${item.id}" type="button">Cancel</button>` : "—"}</div></td></tr>`).join("")}</tbody></table></div>` : emptyState("No appointments yet", "Patient bookings will appear here.")}</div>`;
}

async function renderAdminPatients() {
  const patients = await api("/api/admin/patients");
  content.innerHTML = `${pageHeading("ADMINISTRATION", "Patients", "Patient accounts and contact details.")}
    <div class="panel">${patients.length ? `<div class="table-wrap"><table><thead><tr><th>Name</th><th>Email</th><th>Phone</th><th>Member since</th></tr></thead><tbody>${patients.map(patient => `<tr><td>${escapeHtml(patient.name)}</td><td>${escapeHtml(patient.email)}</td><td>${escapeHtml(patient.phone || "—")}</td><td>${escapeHtml(new Date(patient.createdAt).toLocaleDateString())}</td></tr>`).join("")}</tbody></table></div>` : emptyState("No patients yet", "Registered patient accounts will appear here.")}</div>`;
}

async function handleAuthSubmit(form) {
  const values = Object.fromEntries(new FormData(form).entries());
  const registering = state.authMode === "register";
  if (registering && values.password !== values.confirmPassword) {
    showToast("Passwords do not match.", true);
    return;
  }
  let path = state.loginMethod === "phone" ? "/api/auth/login/phone" : "/api/auth/login";
  let body = state.loginMethod === "phone"
    ? { phone: values.loginPhone, password: values.password }
    : { email: values.email, password: values.password };
  if (registering && values.accountType === "DOCTOR") {
    path = "/api/auth/register-doctor-request";
    body = {
      name: values.name, email: values.email, phone: values.phone, password: values.password,
      department: values.department, specialization: values.specialization,
      qualifications: values.qualifications, experienceYears: Number(values.experienceYears),
      consultationFee: Number(values.consultationFee),
      availableDays: [...form.querySelectorAll('[name="availableDays"]:checked')].map(input => input.value),
      availableSlots: [...form.querySelectorAll('[name="availableSlots"]:checked')].map(input => input.value),
      bio: values.bio
    };
    if (!body.availableDays.length || !body.availableSlots.length) {
      showToast("Select at least one available day and one time slot.", true);
      return;
    }
  } else if (registering) {
    path = "/api/auth/register";
    body = { name: values.name, email: values.email, phone: values.phone, password: values.password };
  }
  const result = await api(path, {
    method: "POST", body: JSON.stringify(body)
  });
  if (registering) {
    const doctorRequest = values.accountType === "DOCTOR";
    if (doctorRequest) {
      state.token = result.token;
      localStorage.setItem("medibook-token", result.token);
      form.reset();
      setAuthMode("login");
      state.page = "doctor-home";
      enterApp(result);
      showToast("Doctor dashboard opened. An admin must activate your profile before patients can book you.");
      return;
    }
    state.token = result.token;
    localStorage.setItem("medibook-token", result.token);
    form.reset();
    state.page = "patient-home";
    enterApp(result);
    showToast("Welcome to your patient dashboard.");
    return;
  }
  state.token = result.token;
  localStorage.setItem("medibook-token", result.token);
  enterApp(result);
  showToast(`Welcome back, ${result.name}.`);
}

async function handleBookingSubmit(form) {
  const body = Object.fromEntries(new FormData(form).entries());
  body.doctorId = Number(body.doctorId);
  await api("/api/appointments", { method: "POST", body: JSON.stringify(body) });
  state.selectedDoctor = null;
  state.page = "patient-appointments";
  enterApp(state.user);
  showToast("Your appointment request has been sent.");
}

async function handleDoctorSubmit(form) {
  const data = new FormData(form);
  const body = Object.fromEntries(data.entries());
  body.experienceYears = Number(body.experienceYears);
  body.consultationFee = Number(body.consultationFee);
  body.availableDays = body.availableDays.split(",").map(value => value.trim()).filter(Boolean);
  body.availableSlots = body.availableSlots.split(",").map(value => value.trim()).filter(Boolean);
  await api("/api/admin/doctors", { method: "POST", body: JSON.stringify(body) });
  await renderAdminDoctors();
  showToast("Doctor added to the directory.");
}

async function handleScheduleSubmit(form) {
  const availableDays = [...form.querySelectorAll('[name="availableDays"]:checked')].map(input => input.value);
  const availableSlots = form.elements.availableSlots.value.split(",").map(value => value.trim()).filter(Boolean);
  await api("/api/doctors/me/schedule", {
    method: "PUT", body: JSON.stringify({ availableDays, availableSlots })
  });
  showToast("Your schedule has been saved.");
}

function openLoginForm(method) {
  state.loginMethod = method;
  setAuthMode("login");
  document.querySelector(method === "phone" ? '[name="loginPhone"]' : '[name="email"]').focus();
}
document.querySelector("#auth-back-button").addEventListener("click", () => {
  state.loginMethod = null;
  setAuthMode("login");
  document.querySelector("#phone-login-button").focus();
});
document.querySelector("#phone-login-button").addEventListener("click", () => openLoginForm("phone"));
document.querySelector("#email-login-button").addEventListener("click", () => openLoginForm("email"));
document.querySelector("#auth-switch").addEventListener("click", () => {
  state.loginMethod = null;
  setAuthMode(state.authMode === "login" ? "register" : "login");
});
document.querySelector('[name="accountType"]').addEventListener("change", syncDoctorRegistrationFields);
document.querySelectorAll("[data-toggle-password]").forEach(button => {
  button.addEventListener("click", () => {
    const input = document.querySelector(`[name="${button.dataset.togglePassword}"]`);
    const visible = input.type === "password";
    input.type = visible ? "text" : "password";
    button.setAttribute("aria-pressed", String(visible));
    button.setAttribute("aria-label", `${visible ? "Hide" : "Show"} ${button.dataset.togglePassword === "password" ? "password" : "confirm password"}`);
    button.title = button.getAttribute("aria-label");
    button.querySelector(".icon-show-password").classList.toggle("hidden", visible);
    button.querySelector(".icon-hide-password").classList.toggle("hidden", !visible);
  });
});
document.querySelector("#profile-button").addEventListener("click", async () => {
  const button = document.querySelector("#profile-button");
  const panel = document.querySelector("#profile-panel");
  const details = document.querySelector("#profile-details");
  const opening = panel.classList.contains("hidden");
  panel.classList.toggle("hidden", !opening);
  document.body.classList.toggle("profile-open", opening);
  button.setAttribute("aria-expanded", String(opening));
  if (!opening) return;
  document.querySelector("#profile-close").focus();
  details.innerHTML = '<p class="profile-loading">Loading profile...</p>';
  try {
    details.innerHTML = profileDetails(await api("/api/auth/me"));
  } catch (error) {
    details.innerHTML = `<p class="profile-loading">${escapeHtml(error.message)}</p>`;
  }
});
document.querySelector("#profile-close").addEventListener("click", closeProfile);
document.addEventListener("keydown", event => {
  if (event.key === "Escape") closeProfile();
});
document.querySelector("#logout").addEventListener("click", () => logout());
document.addEventListener("click", async event => {
  const pageButton = event.target.closest("[data-page]");
  if (pageButton) {
    if (!rolePages().some(([page]) => page === pageButton.dataset.page)) return;
    state.page = pageButton.dataset.page;
    state.selectedDoctor = null;
    enterApp(state.user);
    return;
  }
  const actionButton = event.target.closest("[data-action]");
  if (!actionButton) return;
  try {
    const id = Number(actionButton.dataset.id);
    if (actionButton.dataset.action === "select-doctor") {
      const doctors = await api("/api/doctors");
      state.selectedDoctor = doctors.find(doctor => doctor.id === id) || null;
      await renderPatientDoctors();
      document.querySelector("#booking-panel")?.scrollIntoView({ behavior: "smooth", block: "start" });
    }
    if (actionButton.dataset.action === "clear-doctor") {
      state.selectedDoctor = null;
      await renderPatientDoctors();
    }
    if (actionButton.dataset.action === "cancel-appointment") {
      await api(`/api/appointments/${id}/cancel`, { method: "PATCH" });
      await renderPage();
      showToast("Appointment cancelled.");
    }
    if (actionButton.dataset.action === "set-status") {
      await api(`/api/appointments/${id}/status`, {
        method: "PATCH", body: JSON.stringify({ status: actionButton.dataset.status })
      });
      await renderPage();
      showToast("Appointment status updated.");
    }
    if (actionButton.dataset.action === "toggle-doctor") {
      await api(`/api/doctors/${id}/active`, {
        method: "PATCH", body: JSON.stringify({ active: actionButton.dataset.active === "true" })
      });
      await renderAdminDoctors();
      showToast("Doctor directory updated.");
    }
    if (actionButton.dataset.action === "approve-doctor-request") {
      await api(`/api/admin/doctor-requests/${id}/approve`, { method: "PATCH" });
      await renderAdminDoctorRequests();
      showToast("Doctor account approved and activated.");
    }
    if (actionButton.dataset.action === "reject-doctor-request") {
      await api(`/api/admin/doctor-requests/${id}`, { method: "DELETE" });
      await renderAdminDoctorRequests();
      showToast("Doctor request rejected.");
    }
    if (actionButton.dataset.action === "triage") {
      const symptoms = document.querySelector('#booking-form [name="symptoms"]').value.trim();
      if (!symptoms) return showToast("Add a short description of your symptoms first.", true);
      actionButton.disabled = true;
      actionButton.textContent = "Getting guidance...";
      try {
        const result = await api("/api/triage", { method: "POST", body: JSON.stringify({ symptoms }) });
        const resultBox = document.querySelector("#triage-result");
        resultBox.textContent = `${result.result}\n\nAI guidance is informational and is not a diagnosis.`;
        resultBox.classList.add("visible");
      } catch (error) {
        const message = String(error.message || "").toLowerCase();
        const sessionExpired = message.includes("401");
        const resultBox = document.querySelector("#triage-result");
        resultBox.classList.add("visible");
        resultBox.textContent = sessionExpired
          ? "Your session has expired. Sign in again to continue."
          : message.includes("not configured") || message.includes("credentials") || message.includes("service unavailable")
          ? "AI triage is not configured right now, but you can still book the appointment without it."
          : error.message;
        showToast(sessionExpired
          ? "Your session expired. Please sign in again."
          : "AI triage is unavailable right now, but booking is still available.", false);
      } finally {
        actionButton.disabled = false;
        actionButton.textContent = "Ask AI for guidance";
      }
    }
  } catch (error) {
    showToast(error.message, true);
  }
});

document.addEventListener("change", event => {
  if (event.target.matches('[name="availableDays"], [name="availableSlots"]')) {
    const groupName = event.target.name;
    const selectedCount = document.querySelectorAll(`#doctor-registration-fields [name="${groupName}"]:checked`).length;
    document.querySelector(`[data-selection-summary="${groupName}"]`).textContent = `${selectedCount} selected`;
  }
  if (event.target.id === "department-filter") {
    state.doctorFilter = event.target.value;
    renderPatientDoctors().catch(error => showToast(error.message, true));
  }
});

document.addEventListener("submit", async event => {
  event.preventDefault();
  try {
    const form = event.target;
    if (form.id === "auth-form") await handleAuthSubmit(form);
    if (form.id === "booking-form") await handleBookingSubmit(form);
    if (form.id === "doctor-form") await handleDoctorSubmit(form);
    if (form.id === "schedule-form") await handleScheduleSubmit(form);
  } catch (error) {
    showToast(error.message, true);
  }
});

async function start() {
  setAuthMode("login");
  if (!state.token) return;
  try {
    const user = await api("/api/auth/me");
    enterApp(user);
  } catch {
    logout(false);
  }
}

start();