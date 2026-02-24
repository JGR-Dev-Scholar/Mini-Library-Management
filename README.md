# 📚 Mini Library Management GUI

A Java Swing desktop application for managing a small book library, built with a MySQL database backend. Developed as a group exercise project.

---

## 📋 Features

- 🔐 **Login System** – credentials are stored and verified against a MySQL database
- 📗 **Regular Books** – add, edit, and delete regular book records
- 📘 **Textbooks** – add, edit, and delete textbook records with grade level tracking
- 🗄️ **Persistent Storage** – all records are saved to MySQL and reload automatically on launch
- ✅ **Input Validation** – required fields, numeric checks, and grade range enforcement
- 🖥️ **Clean Split UI** – form panel on the left, separate tables for each book type on the right

---

## 🖼️ App Preview

| Screen | Description |
|--------|-------------|
| **Login** | Username and password verified against the `users` table |
| **Main App** | Left panel for data entry, right panel shows Regular Books and Textbooks in separate tables |
| **CRUD** | Save, Edit, Delete, and Cancel buttons centralized on the form panel |

---

## 🛠️ Tech Stack

| Technology | Purpose |
|------------|---------|
| Java (JDK 17+) | Core application language |
| Java Swing | Desktop GUI framework |
| MySQL 9.6 | Database for persistent storage |
| JDBC (MySQL Connector/J 9.6) | Java-to-MySQL connection |
| VSCode | Development environment |

---

## ⚙️ Project Structure

```
mini-library/
├── .vscode/
│   └── settings.json        ← References the JDBC JAR in lib/
├── lib/
│   └── mysql-connector-j-9.6.0.jar   ← Download separately (see setup)
├── src/
│   └── pckSamples/
│       └── Exer6_MiniLibrary.java     ← Main source file
├── .gitignore
└── README.md
```

---

## 🗄️ Database Setup

**1. Open MySQL Workbench and run the following SQL:**

```sql
CREATE DATABASE IF NOT EXISTS mini_library;
USE mini_library;

-- Stores login credentials
CREATE TABLE IF NOT EXISTS users (
    id       INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

-- Stores both Regular Books and Textbooks
-- grade_level is NULL for Regular Books
CREATE TABLE IF NOT EXISTS books (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    type        VARCHAR(20)  NOT NULL,
    title       VARCHAR(255) NOT NULL,
    pages       INT          NOT NULL,
    grade_level INT          DEFAULT NULL
);

-- Default login accounts
INSERT INTO users (username, password) VALUES
    ('admin', 'password'),
    ('user',  '1234');
```

**2. To change or add login credentials anytime:**
```sql
USE mini_library;

-- Change a password
UPDATE users SET password = 'newpassword' WHERE username = 'admin';

-- Add a new user
INSERT INTO users (username, password) VALUES ('newuser', 'theirpassword');

-- View all users
SELECT * FROM users;
```

---

## 🚀 Getting Started

### Prerequisites
- [Java JDK 17+](https://www.oracle.com/java/technologies/downloads/)
- [MySQL Server 9.6](https://dev.mysql.com/downloads/mysql/)
- [MySQL Connector/J JAR](https://dev.mysql.com/downloads/connector/j/) – Platform Independent
- [VSCode](https://code.visualstudio.com/) with the Java Extension Pack

### Installation Steps

**1. Clone the repository**
```bash
git clone https://github.com/yourusername/mini-library.git
```
Or via GitHub Desktop: **File** → **Clone Repository** → paste the URL.

**2. Add the JDBC driver**
- Download `mysql-connector-j-9.6.0.jar` from the link above
- Place it inside the `lib/` folder of the project

**3. Configure your database password**

Open `src/pckSamples/Exer6_MiniLibrary.java` and find the `DBConnection` class:
```java
private static final String URL      = "jdbc:mysql://localhost:3306/mini_library";
private static final String USER     = "root";
private static final String PASSWORD = "";   // ← put your MySQL root password here
```

**4. Set up the database**
- Open MySQL Workbench
- Run the SQL script from the [Database Setup](#️-database-setup) section above

**5. Configure VSCode to recognize the JAR**

Make sure `.vscode/settings.json` contains:
```json
{
    "java.project.referencedLibraries": [
        "lib/**/*.jar"
    ]
}
```
Then press `Ctrl + Shift + P` → `Java: Clean Java Language Server Workspace` → Restart.

**6. Run the app**
- Open `Exer6_MiniLibrary.java` in VSCode
- Click the **Run** button or press `F5`

---

## 🔑 Default Login Credentials

| Username | Password |
|----------|----------|
| `admin`  | `unlock` |
| `user`   | `1234` |

> Credentials can be updated anytime directly in MySQL Workbench.

---

## 👥 Authors

> The Developers who worked on this project.

- **Joseph Gabriel A. Roces** - Quality Engineer / Documentation
- **Sean Patrick Brix V. Salamera** – Data Engineer
- **Marvin Karl R. Sangco** – SWE BackEnd
- **Leonard Vincent L. Camat** - SWE FrontEnd

---

## 📝 Notes

- Passwords are stored as **plain text** in the database — this is intentional for a school exercise. In a real-world application, passwords should be hashed using BCrypt or a similar algorithm.
- The `lib/` folder is excluded from the repository via `.gitignore`. Each developer must download and add the JDBC JAR manually.
- The database password in `DBConnection.java` is **not committed** to the repository. Each developer sets their own locally.

