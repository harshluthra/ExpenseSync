# 💸 Expense Splitter Application

A backend service built using **Spring Boot** that helps a group of users manage shared expenses and view simplified debts among them.

---

## 🚀 How to Run the Application

### 🧱 Prerequisites

- Java 17
- Maven

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
  - Request Body:
    ```json
    {
      "name": "xyz",
      "email": "xyz@example.com"
    }
    ```
  - Example:
    ```bash
    curl --location --request POST 'http://localhost:8080/users' \
    --header 'Content-Type: application/json' \
    --data-raw '{
      "name": "xyz",
      "email": "xyz@example.com"
    }'
    ```
  - Response:
    ```json
    {
        "uuid": "46a383e9-6fbd-4c75-ae4a-ffe289e1f0f4",
        "name": "xyz", 
        "email": "xyz@example.com" 
    }
    ```

- `GET /users`

  - Fetches all users, or a specific user if `email` query param is provided
  - Example:
    ```bash
    curl --location --request GET 'http://localhost:8080/users?email=xyz@example.com'
    ```

---

### 💰 ExpenseController

Allows users to record shared expenses.

#### Endpoints:

- `POST /expenses`

  - Adds a new expense where the amount is split among participants
    - Supports different split types: `EQUAL`, `EXACT`, `PERCENT`
      - For now, only `EQUAL` and `EXACT` is functional;  `PERCENT` is a placeholder for future logic
        - Request Body for `EQUAL`:
          ```json
          {
            "description": "Dinner",
            "amount": 1500,
            "paidByEmail": "krish@example.com",
            "splitType": "EQUAL",
            "participants": [
              { "email": "krish@example.com" },
              { "email": "janhvi@example.com" },
              { "email": "harsh@example.com" }
            ]
          }
          ```

        - Request Body for `EXACT`:
            ```json
            {
                "description": "Dinner",
                "amount": 900,
                "paidByEmail": "janhvi@example.com",
                "splitType": "EXACT",
                "participants": [
                    { "email": "krish@example.com", "share": 300 },
                    { "email": "janhvi@example.com", "share": 300 },
                    { "email": "harsh@example.com", "share": 300 }
                ]
            }
          ```
      - Returns: the expense details along with amount owed and amount to receive by each user
       ```json
       {
        "id": 4,
        "description": "Dinner",
        "amount": 900,
        "paidBy": {
          "email": "janhvi@example.com",
          "name": "Janhvi"
        },
        "participants": [
          {
              "name": "Krish",
              "email": "krish@example.com",
              "amountOwed": 300,
              "amountToReceive": 0
          },
          {
              "name": "Harsh",
              "email": "harsh@example.com",
              "amountOwed": 300,
              "amountToReceive": 0
          },
          {
              "name": "Janhvi",
              "email": "janhvi@example.com",
              "amountOwed": 0,
              "amountToReceive": 600
          }
        ],
        "createdAt": "2025-04-02T14:53:30.640135"
      }
      ```
- `GET /expenses?email=xyz@example.com&showParticipants=true`

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

- Each expense must include the payer in the list of participants.
- The simplified API uses a greedy algorithm to minimize total number of transactions.

---
## 🧠 Algorithms Used

### Raw Balance Calculation

- Iterates over all expenses for a user.
- For each expense:
  - If the user is the payer, others owe them equally.
  - If the user is a participant but not the payer, they owe their share.
- Aggregates net amounts between users (payer vs participant).
- While raw balance doesn’t aim to minimize transactions, we apply small optimizations like:
  - Batching repeated counterparties *(same person appearing multiple times).*
  - Skipping offsetting transactions *(e.g., A owes B ₹100 and B owes A ₹100 → net 0).*

### Simplified Balance Calculation

- Calculates net balance for each user (amount paid - amount owed).
- Categorizes users as:
  - Creditors: Net positive balance.
  - Debtors: Net negative balance.
- Uses a **greedy matching algorithm**:
  - Matches largest debtor with largest creditor.
  - Transfers the smallest possible amount to reduce outstanding balances.
  - Repeats until all balances are settled.
- Result: Minimum number of transactions needed for full group settlement.

