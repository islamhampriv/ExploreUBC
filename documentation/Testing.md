# Testing

## 1. Change History

| **Change Date**   | **Modified Sections** | **Rationale** |
| ----------------- | --------------------- | ------------- |
| _Nothing to show_ |

---

## 2. Back-end Test Specification: APIs

### 2.1. Locations of Back-end Tests and Instructions to Run Them

#### 2.1.1. Tests

| **Interface** | **Describe Group Location, No Mocks** | **Describe Group Location, With Mocks** | **Mocked Components** |
| --- | --- | --- | --- |
| **GET /api/v1/users/:userId** | [`backend/tests/no-mock/users.test.ts`](../backend/tests/no-mock/users.test.ts) | [`backend/tests/mock/users.test.ts`](../backend/tests/mock/users.test.ts) | Get one user's data from the database |
| **GET /api/v1/users/:userId/preferences** | [`backend/tests/no-mock/users.test.ts`](../backend/tests/no-mock/users.test.ts) | [`backend/tests/mock/users.test.ts`](../backend/tests/mock/users.test.ts) | Get one user's preference data from the database |
| **PATCH /api/v1/users/:userId/preferences** | [`backend/tests/no-mock/users.test.ts`](../backend/tests/no-mock/users.test.ts) | [`backend/tests/mock/users.test.ts`](../backend/tests/mock/users.test.ts) | Update a user's preference data in the database |
| **POST /api/v1/users/:userId/itineraries** | [`backend/tests/no-mock/itineraries.test.ts`](../backend/tests/no-mock/itineraries.test.ts) | [`backend/tests/mock/itineraries.test.ts`](../backend/tests/mock/itineraries.test.ts) | Create an itinerary for a user in the database |
| **GET /api/v1/users/:userId/itineraries** | [`backend/tests/no-mock/itineraries.test.ts`](../backend/tests/no-mock/itineraries.test.ts) | [`backend/tests/mock/itineraries.test.ts`](../backend/tests/mock/itineraries.test.ts) | Get all itineraries for a user from the database. The itinerary-list endpoint accepts the optional Boolean query parameters `saved` and `inRecommendation`. |
| **GET /api/v1/users/:userId/itineraries/:itineraryId/details** | [`backend/tests/no-mock/itineraries.test.ts`](../backend/tests/no-mock/itineraries.test.ts) | [`backend/tests/mock/itineraries.test.ts`](../backend/tests/mock/itineraries.test.ts) | Get an itinerary's details from the database |
| **GET /api/v1/users/:userId/itineraries/:itineraryId** | [`backend/tests/no-mock/itineraries.test.ts`](../backend/tests/no-mock/itineraries.test.ts) | [`backend/tests/mock/itineraries.test.ts`](../backend/tests/mock/itineraries.test.ts) | Get one complete itinerary from the database |
| **PATCH /api/v1/users/:userId/itineraries/:itineraryId** | [`backend/tests/no-mock/itineraries.test.ts`](../backend/tests/no-mock/itineraries.test.ts) | [`backend/tests/mock/itineraries.test.ts`](../backend/tests/mock/itineraries.test.ts) | Update an itinerary's details in the database |
| **DELETE /api/v1/users/:userId/itineraries/:itineraryId** | [`backend/tests/no-mock/itineraries.test.ts`](../backend/tests/no-mock/itineraries.test.ts) | [`backend/tests/mock/itineraries.test.ts`](../backend/tests/mock/itineraries.test.ts) | Delete an itinerary from the database |
| **POST /api/v1/users/:userId/itineraries/:itineraryId/activities** | [`backend/tests/no-mock/itineraries.test.ts`](../backend/tests/no-mock/itineraries.test.ts) | [`backend/tests/mock/itineraries.test.ts`](../backend/tests/mock/itineraries.test.ts) | Add an activity to an itinerary in the database |
| **PATCH /api/v1/users/:userId/itineraries/:itineraryId/activities/:activityId** | [`backend/tests/no-mock/itineraries.test.ts`](../backend/tests/no-mock/itineraries.test.ts) | [`backend/tests/mock/itineraries.test.ts`](../backend/tests/mock/itineraries.test.ts) | Update an itinerary activity in the database |
| **DELETE /api/v1/users/:userId/itineraries/:itineraryId/activities/:activityId** | [`backend/tests/no-mock/itineraries.test.ts`](../backend/tests/no-mock/itineraries.test.ts) | [`backend/tests/mock/itineraries.test.ts`](../backend/tests/mock/itineraries.test.ts) | Remove an activity from an itinerary in the database |
| **POST /api/v1/users/:userId/itineraries/:itineraryId/transportation-segments** | [`backend/tests/no-mock/itineraries.test.ts`](../backend/tests/no-mock/itineraries.test.ts) | [`backend/tests/mock/itineraries.test.ts`](../backend/tests/mock/itineraries.test.ts) | Add a transportation segment to an itinerary in the database |
| **PATCH /api/v1/users/:userId/itineraries/:itineraryId/transportation-segments/:segmentId** | [`backend/tests/no-mock/itineraries.test.ts`](../backend/tests/no-mock/itineraries.test.ts) | [`backend/tests/mock/itineraries.test.ts`](../backend/tests/mock/itineraries.test.ts) | Update a transportation segment in the database |
| **DELETE /api/v1/users/:userId/itineraries/:itineraryId/transportation-segments/:segmentId** | [`backend/tests/no-mock/itineraries.test.ts`](../backend/tests/no-mock/itineraries.test.ts) | [`backend/tests/mock/itineraries.test.ts`](../backend/tests/mock/itineraries.test.ts) | Remove a transportation segment from an itinerary in the database |

#### 2.1.2. Commit Hash Where Tests Run

`[Insert Commit SHA here]`

#### 2.1.3. Explanation on How to Run the Tests

1. **Clone the Repository**:

   - Open your terminal and run:
     ```
     git clone https://github.com/example/your-project.git
     ```

2. **...**

### 2.2. GitHub Actions Configuration Location

`~/.github/workflows/backend-tests.yml`

### 2.3. Jest Coverage Report Screenshots With Mocks

_(Placeholder for Jest coverage screenshot with mocks enabled)_

### 2.4. Jest Coverage Report Screenshots Without Mocks

_(Placeholder for Jest coverage screenshot without mocks)_

---

## 3. Tests of Non-Functional Requirements

### 3.1. Test Locations in Git

| **Non-Functional Requirement**  | **Location in Git**                              |
| ------------------------------- | ------------------------------------------------ |
| **Performance (Response Time)** | [`backend/tests/nfr/response_time.test.js`](#) |
| **Chat Data Security**          | [`backend/tests/nfr/chat_security.test.js`](#) |

### 3.2. Test Verification and Logs

- **Performance (Response Time)**

  - **Verification:** This test suite simulates multiple concurrent API calls using Jest along with a load-testing utility to mimic real-world user behavior. The focus is on key endpoints such as user login and study group search to ensure that each call completes within the target response time of 2 seconds under normal load. The test logs capture metrics such as average response time, maximum response time, and error rates. These logs are then analyzed to identify any performance bottlenecks, ensuring the system can handle expected traffic without degradation in user experience.
  - **Log Output**
    ```
    [Placeholder for response time test logs]
    ```

- **Chat Data Security**
  - **Verification:** ...
  - **Log Output**
    ```
    [Placeholder for chat security test logs]
    ```

---

## 4. Front-end Test Specification

### 4.1. Location in Git of Front-end Test Suite:

`frontend/src/androidTest/java/com/studygroupfinder/`

### 4.2. Tests

- **Use Case: Update Preferences**

  - **Expected Behaviors:**
    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | 1. The user opens the user profile tab. | Open User Profile screen |
    | 2. The app shows the logged in userName, the user's start location input field, transportation, budget, interest dropdowns, and a `Saved Prefrences` button. | Check that the user name is present.<br>Check the start location input is present and enabled.<br>Check the three preferences dropdowns are present and enabled.<br>Check the save button is present and enabled. |
    | 3. The user inputs a new start location. | Input "12345 abc Avenue, Vancouver BC" in the text field. |
    | 4. The user selects another transportation preference | Click the transportation dropdown <br> Check the list option 'walking', 'cycling', 'transit','driving','other' appears. <br>Select "transit" and check if the field value is 'transit'    |
    | 5. The user selects another budget preference | Click the budget dropdown <br> Check the list option 'economical', 'balanced', 'premium' appears. <br>Select "balanced" and check if the field value is 'balanced'    |
    | 6. The user selects another interest preference | Click the interest dropdown <br> Check the list option 'food', 'art', 'outdoors', 'markets', 'live_music', 'gaming' appears. <br>Select "outdoors" and check if the field value is 'outdoor'    |
    | 7. The user saves the user profile change | Click the `Save preferences` button <br>Check the confirmation message "Preference saved` appears   |
    <!-- | 3a. The user inputs an ill-formatted string. | Input "_^_^^OQ#$" in the text field. |
    | 3a1. The app displays an error message prompting the user for the expected format. | Check that a dialog is opened with the text: "Please use only alphanumeric characters ". |
    | 4. The user presses the "Add" button. | Click the button labelled "add ". |
    | 5. The screen refreshes and the new item is at the bottom of the todo list. | Check that a text box with the text "buy milk" is present on screen.<br>Input "buy chocolate" in the text field.<br>Click the button labelled "add".<br>Check that two text boxes are present on the screen with "buy milk" on top and "buy chocolate" at the bottom. |
    | 5a. The list exceeds the maximum todo-list size. | Repeat steps 3 to 5 ten times.<br>Check that a dialog is opened with the text: "You have too many items, try completing one first". | -->

  - **Test Logs:**
    ```
    [Placeholder for Espresso test execution logs]
    ```

- **Use Case: ...**

  - **Expected Behaviors:**

    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | ...                | ...                 |

  - **Test Logs:**
    ```
    [Placeholder for Espresso test execution logs]
    ```

- **...**
