# Mountcrest Hospital Management System 🏥

A comprehensive desktop GUI application designed to manage hospital operations, built with **Java Swing** and integrated with a **MySQL** database.

## 🚀 Features
* **Multi-Role Authentication:** Secure login portals for Patients, Doctors, and Administrators.
* **Smart Appointment Booking:** Patients can book appointments based on issue type, which dynamically routes them to the correct specialist (e.g., "Heart" -> Cardiologist).
* **Automated Billing:** Calculates final consultation bills based on patient types (Military, Insurance, Gratis, Regular).
* **Doctor Dashboard:** Doctors can view their daily schedule pulled in real-time from the database.
* **Administrative Control:** * View analytics and high-earning doctors.
  * Execute database-level stored procedures to hire or fire faculty.
  * Manage active appointments.

## 🛠️ Tech Stack
* **Frontend:** Java (Swing, AWT)
* **Backend Database:** MySQL
* **Database Integration:** JDBC (Java Database Connectivity) with Prepared Statements to prevent SQL injection.

## ⚙️ Database Setup Instructions
To run this application locally, you must recreate the SQL database.

1. Install MySQL and ensure the server is running on `localhost:3306`.
2. Create a new database named `mountcrest_db`.
3. You will need to create the following core tables:
   * `doctors`
   * `specializations`
   * `patients`
   * `patient_types`
   * `appointments`
   * `admin`
4. The application relies on the following Stored Procedures. Ensure these are compiled in your database:
   * `fire_doctor(admin_id, doc_id)`
   * `rehire_doctor(admin_id, doc_id)`
   * `add_doctor(username, password, spec_id, salary)`
   * `doctor_schedule(doc_id)`
5. Update line 34 in `Main.java` with your local MySQL password:
   `DriverManager.getConnection("jdbc:mysql://localhost:3306/mountcrest_db", "root", "YOUR_LOCAL_PASSWORD");`

## 🏃‍♂️ How to Run
Ensure you have the JDK installed. Compile and run the application from your terminal:
```bash
javac Main.java
java Main
