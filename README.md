# Billing Management System

A web-based billing and inventory management system built as part of a university software engineering project. The goal of this project was to take an existing console-based billing system and redesign it into a complete web application using Java Servlets, JSP, Apache Tomcat and MySQL while following Clean Architecture principles.

The system allows employees to manage inventory, process sales, generate reports and manage customers through a browser-based interface. It also includes a customer-facing online shop where users can browse products and place orders.

---

## What the system can do

### Authentication
- Secure login and registration
- Role-based access for employees and customers
- Password hashing before storing credentials

### Inventory Management
- Add, update and remove products
- Manage shelf stock
- Manage website stock
- Track stock batches
- Remove expired inventory

### Billing
- Process counter sales
- Generate bills
- View billing summaries
- Support different payment methods

### Online Shop
- Customer registration and login
- Browse available products
- Purchase products online
- View available website stock

### Reporting
- Inventory reports
- Sales reports
- Batch stock reports

### Testing
- Unit tests for business logic
- Servlet tests
- Concurrency tests for selected web components

---

## Technologies Used

### Backend
- Java
- Jakarta Servlets
- JSP
- JDBC
- Maven
- Apache Tomcat

### Database
- MySQL

### Testing
- JUnit
- Mockito

### Development Tools
- IntelliJ IDEA
- Git
- GitHub

---

## Project Structure

```
src
├── main
│   ├── java
│   │   ├── adapter
│   │   ├── application
│   │   ├── domain
│   │   ├── service
│   │   └── web
│   ├── resources
│   └── webapp
└── test
```

The project follows a layered architecture to keep the business logic independent from the user interface and database implementation.

---

## Running the Project

### Requirements

- Java 20
- Apache Tomcat
- Maven
- MySQL

### Setup

1. Clone the repository.

```bash
git clone https://github.com/SooryaDS/SYOS-CCCP.git
```

2. Create the required MySQL database.

3. Update the database connection details in:

```
src/main/java/com/syos/adapter/out/util/DatabaseConnection.java
```

4. Build the project.

```bash
mvn clean install
```

5. Deploy the generated WAR file (or run directly through IntelliJ) using Apache Tomcat.

---


## What I Learned

This project gave me hands-on experience with building a complete Java web application from scratch. During development I learned how to:

- Design applications using Clean Architecture
- Build dynamic web applications with Servlets and JSP
- Connect Java applications to MySQL using JDBC
- Implement role-based authentication
- Write unit and servlet tests
- Manage projects using Maven and Git
- Deploy applications with Apache Tomcat

---

## Future Improvements

- REST API support
- Spring Boot migration
- JWT authentication
- Docker deployment
- Better dashboard analytics
- Responsive mobile-friendly interface

---

## Author

**Soorya De Silva**

Feel free to explore the project or reach out if you have any feedback.
