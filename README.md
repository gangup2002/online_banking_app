# Bank Transfer System — COSC 32133 / BECS 32263 Assignment 01

Spring Boot 4, Java 21, Maven, Thymeleaf, Spring Data JPA, MySQL and Lombok.

## Features

- Dashboard with customer balances, total balance and SUCCESS / FAILED counts.
- Transfers protected by `@Transactional(rollbackFor = Exception.class)`.
- Failed transfer audit records saved separately with `Propagation.REQUIRES_NEW`.
- JPQL, native SQL, aggregate and `@Modifying` queries in the repositories.
- Customer registration and case-insensitive name search.
- Account transaction history and total successful amount sent.
- Large successful transfer search by minimum amount.
- Deposit and withdrawal operations, each transactional and recorded in `bank_transactions`.
- Task 12 transfer fee: 1% is debited from the sender and credited to bank account `999999`.
- Simulated failure checkbox to demonstrate rollback.

## Run locally

1. Start MySQL in XAMPP (normally port 3306).
2. Confirm `src/main/resources/application.properties` has the correct MySQL username and password.
3. Use Java 21 and Maven, then run from this folder:

   ```bash
   mvn clean spring-boot:run
   ```

4. Open `http://localhost:8080`.

The database `bank_db` is created automatically. On a completely empty database, the data loader creates Kamal (LKR 50,000), Nimali (LKR 25,000), Saman (LKR 10,000), and the bank fee account `999999` (LKR 0). It does not overwrite existing customer balances.

## Important assignment note

Task 9's original expected Kamal balance after a LKR 5,000 transfer is LKR 45,000, before Task 12 adds the fee. In this completed Task 12 version, the 1% fee is also debited, so the sender is debited LKR 5,050 and the fee account receives LKR 50. This is why the Task 12 version's balances differ from the earlier Task 9 example.

For Task 10, temporarily change the transfer annotation to `@Transactional(propagation = Propagation.SUPPORTS)` only for the experiment, then restore `@Transactional(rollbackFor = Exception.class)` immediately afterward. Do not leave the unsafe annotation in the submitted final version.

## Git

The `target/` build output and `secrets.properties` are excluded by `.gitignore`. Commit source files, templates, CSS and project configuration to the feature branch.
# online_banking_app
