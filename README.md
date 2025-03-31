# 💸 Expense Splitter Application

A backend service built using **Spring Boot** that helps a group of users manage shared expenses and view simplified debts among them.

---

## 🚀 How to Run the Application

### 🧱 Prerequisites

- Java 17
- Maven
- Hibernate

### ▶️ Steps

1. **Clone the repository**

```bash
git clone {github_link}
```

2. **Build the project**

```bash
mvn clean install
```

3. **Run the application**

```bash
mvn spring-boot:run
```

Application will start on: `http://localhost:8080`

---

## 📦 Controller Overview

### 👤 UserController

Handles creation and listing of users.

#### Endpoints:

- `POST /users`

  - Creates a new user
  - Request Params: `name`, `email`
  - Example:
    ```bash
    curl --location --request POST 'http://localhost:8080/users?name=xyz&email=xyz@example.com' \
    --header 'Content-Type: application/json'
    ```
  - Response:
    ```json
    {
      "id": 1,
      "uuid": "46a383e9-6fbd-4c75-ae4a-ffe289e1f0f4",
      "name": "xyz",
      "email": "xyz@example.com",
      "createdAt": "2025-04-01T03:26:41.096567",
      "updatedAt": "2025-04-01T03:26:41.096652"
    }
    ```

- `GET /users`

  - Returns list of all users

- `GET /users/email?email=xyz@example.com`

  - Fetches a user by email
  - Example:
    ```bash
    curl --location --request GET 'http://localhost:8080/users/email?email=xyz@example.com'
    ```

---

### 💰 ExpenseController

Allows users to record shared expenses.

#### Endpoints:

- `POST /expenses`

  - Adds a new expense where the amount is split equally among all participants
  - Request Body:
    ```json
    {
      "description": "Dinner",
      "amount": 1500,
      "paidByEmail": "krish@example.com",
      "participantEmails": ["krish@example.com", "janhvi@example.com", "harsh@example.com"]
    }
    ```
  - Returns: the expense details along with amount owed and amount to receive by each user

- `GET /expenses/by-user?email=xyz@example.com&showParticipants=true`

  - Fetches expenses that involve the given user
  - Optional flag `showParticipants` to include participant-level breakdown

---

### 🧾 BalanceController

Calculates what users owe to each other.

#### Endpoints:

- `GET /balances/raw?email=xyz@example.com`

  - Returns non-simplified balances (who owes what to whom)
  - Response includes net balance and individual transactions

- `GET /balances/simplified?email=xyz@example.com`

  - Returns simplified debts with minimum transactions needed
  - Useful for final settlement of dues

---

## 📥 Postman Collection

If you'd like to try out all the APIs quickly, a Postman collection is available in the root directory of the project (alongside the README and pom.xml). You can import it into your Postman workspace to easily test all available endpoints.

---

## ✅ Example Use Case

1. Create users A, B, and C.
2. A pays ₹900 for a meal with all three participating.
3. Each owes ₹300. The app tracks who paid and who owes whom.
4. View `GET /balances/raw` and `GET /balances/simplified` for A to see detailed and optimized summaries.

---

## 📌 Notes

- All amounts are split equally among participants.
- Each expense must include the payer in the list of participants.
- The simplified API uses a greedy algorithm to minimize total number of transactions.

---

