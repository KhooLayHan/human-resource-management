# BHEL Human Resource Management System

**Module:** Distributed Computer Systems (CT024-3-3)

**Intake:** APU Level 3

A distributed Human Resource Management system built using **Java RMI**, **JavaFX**, and **MySQL**. 
This application allows HR staff to manage employees, recruitment, and training, while enabling employees 
to manage their profiles and leave applications.

---

## Prerequisites

Before running the application, ensure you have the following installed:

1. **Java Development Kit (JDK) 21** (Required for JavaFX and modern switch features).
2. **Apache Maven 3.8+** (https://maven.apache.org/install.html). 
3. **Docker Desktop** (Required for the database and integration tests).
4. **DBeaver** (Required to provide a visual overview of the database).

---

## Quick Setup Guide

### 1. Clone and Configure
Clone the repository and set up the local configuration file. 
**Note:** The default credentials in `config.properties.example`, `psw4j.properties.example`, and `.env.example` 
are pre-configured to work with the Docker setup below. You usually do not need to change them.


```bash
git clone https://github.com/KhooLayHan/human-resource-management.git
cd human-resource-management

# Create the configuration files from the template
cp .env.example .env 
cp src/main/resources/config.properties.example src/main/resources/config.properties
cp src/main/resources/psw4j.properties.example src/main/resources/psw4j.properties
```
### 2. Generate the keystore file for certification
```bash
cd src/main/resources
keytool -genkeypair -alias bhel_payroll -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore payroll_keystore.p12 -validity 3650 -storepass password123
cd ../../..
```
You can press Enter through the questions (First Name, Unit, etc.) or fill them in. Type 'yes' for the last confirmation question.

### 3. Start the Database (Docker)
We use Docker Compose to spin up a MySQL database instantly without manual installation.

```bash
docker compose up -d
```
- **Host:** `localhost`
- **Port:** `3306`
- **Database:** `hrm_db`
- **User:** `user` / **Password:** `password123`

### 4. Build the Project
Compile the code and run the tests. Note: The tests use **Testcontainers**, so Docker must be running.

```bash
mvn clean install
```

---

## Running the Application

This is a Client-Server application. You must start the Server first, then the Client.

**1. Start the Server:**
Open a terminal in the project root and run:

```bash
java -jar target/hrm-server.jar
```
Wait until you see the message: `Server is running and waiting for client connections...`*

**2. Start the Client:**
To ensure all JavaFX UI components load correctly, run the client using the Maven plugin.

Open a new terminal in the project root and run:

```bash
mvn clean javafx:run 
```

---

## Login Credentials

The server automatically starts in a **development** environment. Thus, it seeds the database with test data.

| Role | Username | Password   |
| :--- | :--- |:-----------|
| **HR Staff (Admin)** | `hr_admin` | `admin123` |
| **Employee** | `employee` | `user123`  |

---

## Technology Stack

- **Architecture:** Java RMI (Remote Method Invocation)
- **UI Framework:** JavaFX 21 (FXML-based)
- **Database:** MySQL 9.4.0 and up
- **Build Tool:** Apache Maven
- **Testing:** JUnit 5, AssertJ, Testcontainers
- **Security:** Password4j (Argon2id hashing), TLS-ready socket structure
- **Reporting:** OpenPDF

## Project Structure

- `src/main/java/org/bhel/hrm/client`: JavaFX UI controllers and views.
- `src/main/java/org/bhel/hrm/server`: RMI Server implementation, DAOs, and Business Logic.
- `src/main/java/org/bhel/hrm/common`: Shared DTOs, Exceptions, and the RMI Interface.
- `src/main/resources`: FXML views, CSS styles, and configuration files.

---

## Testing

To run the unit and integration tests and spin up a temporary database via **Testcontainers**:

```bash
mvn test
```

## Troubleshooting

**"JavaFX runtime components are missing"**

- If running from an IDE (IntelliJ/Eclipse), ensure you have configured the VM options to include the JavaFX module path, or simply use `mvn javafx:run` or the shaded JARs as described above.

**Database Connection Refused**

- Ensure Docker is running and the container is up (`docker ps`). If the port 3306 is occupied by a local MySQL installation, you may need to stop your local service or change the port in `docker-compose.yml` and `config.properties`.

---

### **Appendix: Connecting with DBeaver**

To visually inspect the database tables and verify data:

1.  **Install DBeaver:** Download and install DBeaver Community Edition.

2.  **Create New Connection:**
    - Click the plug icon (New Connection). 
    - Select **MySQL**.
 
3. **Connection Settings:**
   - **Server Host:** `localhost`
   - **Port:** `3306`
   - **Database:** `hrm_db`
   - **Username:** `user`
   - **Password:** `password123`

4. **Crucial Setting (Fixing "Public Key" Error):**
   - Click on the **"Driver properties"** tab (next to "Main"). 
   - Find the property **`allowPublicKeyRetrieval`**. 
   - Change the value to **`true`**.
   
5. **Test & Finish:** Click "Test Connection", then "Finish".

You should now see the `users`, `employees`, `leave_applications`, etc., tables and the seeded data.