Feature: User Registration
  As an unregistered user
  I want to register on the website with my email, password and username
  So that I can be a registered user and be able to login and make purchases

  Background:
    Given the user database is empty

  Scenario: Successful user registration
    When I submit a registration with username "johndoe", email "john@example.com", and password "Secure1@pass"
    Then the registration should succeed with status 200
    And the response should contain a JWT token
    And the response should contain user with username "johndoe"
    And the response should not expose the password

  Scenario: First registered user receives admin role
    When I submit a registration with username "firstadmin", email "admin@example.com", and password "Admin1@secure"
    Then the registration should succeed with status 200
    And the response user should have role "ROLE_ADMIN"

  Scenario: Second registered user receives customer role
    Given a user is already registered with username "firstadmin", email "admin@example.com", and password "Admin1@secure"
    When I submit a registration with username "customer1", email "customer1@example.com", and password "Secure1@pass"
    Then the registration should succeed with status 200
    And the response user should have role "ROLE_CUSTOMER"

  Scenario: Registration with duplicate username
    Given a user is already registered with username "johndoe", email "existing@example.com", and password "Secure1@pass"
    When I submit a registration with username "johndoe", email "new@example.com", and password "Secure1@pass"
    Then the registration should fail with status 400
    And the response should contain error "Username already exists"

  Scenario: Registration with duplicate email
    Given a user is already registered with username "firstuser", email "shared@example.com", and password "Secure1@pass"
    When I submit a registration with username "seconduser", email "shared@example.com", and password "Secure1@pass"
    Then the registration should fail with status 400
    And the response should contain error "Email already exists"

  Scenario: Registration with weak password - no uppercase
    When I submit a registration with username "johndoe", email "john@example.com", and password "nouppercase1@"
    Then the registration should fail with status 400
    And the response should contain a password validation error

  Scenario: Registration with weak password - no digit
    When I submit a registration with username "johndoe", email "john@example.com", and password "NoDigit@pass"
    Then the registration should fail with status 400
    And the response should contain a password validation error

  Scenario: Registration with weak password - too short
    When I submit a registration with username "johndoe", email "john@example.com", and password "Ab1@"
    Then the registration should fail with status 400
    And the response should contain a password validation error

  Scenario: Registration with blank username
    When I submit a registration with username "", email "john@example.com", and password "Secure1@pass"
    Then the registration should fail with status 400

  Scenario: Registration with invalid email format
    When I submit a registration with username "johndoe", email "not-an-email", and password "Secure1@pass"
    Then the registration should fail with status 400
