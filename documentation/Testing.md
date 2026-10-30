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
| **GET /api/v1/users/:userId** | [`backend/tests/no-mock/users/users.test.ts`](../backend/tests/no-mock/users/users.test.ts) | [`backend/tests/mock/users/users.test.ts`](../backend/tests/mock/users/users.test.ts) | Get one user's data from the database |
| **PATCH /api/v1/users/:userId** | [`backend/tests/no-mock/users/users.test.ts`](../backend/tests/no-mock/users/users.test.ts) | [`backend/tests/mock/users/users.test.ts`](../backend/tests/mock/users/users.test.ts) | Update a user's display name, starting location, or preferences in the database |
| **POST /api/v1/users/:userId/itineraries** | [`backend/tests/no-mock/itineraries/itineraries.test.ts`](../backend/tests/no-mock/itineraries/itineraries.test.ts) | [`backend/tests/mock/itineraries/itineraries.test.ts`](../backend/tests/mock/itineraries/itineraries.test.ts) | Create an itinerary for a user in the database |
| **GET /api/v1/users/:userId/itineraries** | [`backend/tests/no-mock/itineraries/itineraries.test.ts`](../backend/tests/no-mock/itineraries/itineraries.test.ts) | [`backend/tests/mock/itineraries/itineraries.test.ts`](../backend/tests/mock/itineraries/itineraries.test.ts) | Get all itineraries for a user from the database. The itinerary-list endpoint accepts the optional Boolean query parameters `saved` and `inRecommendation`. |
| **GET /api/v1/users/:userId/itineraries/:itineraryId/details** | [`backend/tests/no-mock/itineraries/itineraries.test.ts`](../backend/tests/no-mock/itineraries/itineraries.test.ts) | [`backend/tests/mock/itineraries/itineraries.test.ts`](../backend/tests/mock/itineraries/itineraries.test.ts) | Get an itinerary's details from the database |
| **GET /api/v1/users/:userId/itineraries/:itineraryId** | [`backend/tests/no-mock/itineraries/itineraries.test.ts`](../backend/tests/no-mock/itineraries/itineraries.test.ts) | [`backend/tests/mock/itineraries/itineraries.test.ts`](../backend/tests/mock/itineraries/itineraries.test.ts) | Get one complete itinerary from the database |
| **PATCH /api/v1/users/:userId/itineraries/:itineraryId** | [`backend/tests/no-mock/itineraries/itineraries.test.ts`](../backend/tests/no-mock/itineraries/itineraries.test.ts) | [`backend/tests/mock/itineraries/itineraries.test.ts`](../backend/tests/mock/itineraries/itineraries.test.ts) | Update an itinerary's details in the database |
| **DELETE /api/v1/users/:userId/itineraries/:itineraryId** | [`backend/tests/no-mock/itineraries/itineraries.test.ts`](../backend/tests/no-mock/itineraries/itineraries.test.ts) | [`backend/tests/mock/itineraries/itineraries.test.ts`](../backend/tests/mock/itineraries/itineraries.test.ts) | Delete an itinerary from the database |
| **POST /api/v1/users/:userId/itineraries/:itineraryId/activities** | [`backend/tests/no-mock/itineraries/itineraries.test.ts`](../backend/tests/no-mock/itineraries/itineraries.test.ts) | [`backend/tests/mock/itineraries/itineraries.test.ts`](../backend/tests/mock/itineraries/itineraries.test.ts) | Add an activity to an itinerary in the database |
| **PATCH /api/v1/users/:userId/itineraries/:itineraryId/activities/:activityId** | [`backend/tests/no-mock/itineraries/itineraries.test.ts`](../backend/tests/no-mock/itineraries/itineraries.test.ts) | [`backend/tests/mock/itineraries/itineraries.test.ts`](../backend/tests/mock/itineraries/itineraries.test.ts) | Update an itinerary activity in the database |
| **DELETE /api/v1/users/:userId/itineraries/:itineraryId/activities/:activityId** | [`backend/tests/no-mock/itineraries/itineraries.test.ts`](../backend/tests/no-mock/itineraries/itineraries.test.ts) | [`backend/tests/mock/itineraries/itineraries.test.ts`](../backend/tests/mock/itineraries/itineraries.test.ts) | Remove an activity from an itinerary in the database |
| **POST /api/v1/users/:userId/itineraries/:itineraryId/transportation-segments** | [`backend/tests/no-mock/itineraries/itineraries.test.ts`](../backend/tests/no-mock/itineraries/itineraries.test.ts) | [`backend/tests/mock/itineraries/itineraries.test.ts`](../backend/tests/mock/itineraries/itineraries.test.ts) | Add a transportation segment to an itinerary in the database |
| **PATCH /api/v1/users/:userId/itineraries/:itineraryId/transportation-segments/:segmentId** | [`backend/tests/no-mock/itineraries/itineraries.test.ts`](../backend/tests/no-mock/itineraries/itineraries.test.ts) | [`backend/tests/mock/itineraries/itineraries.test.ts`](../backend/tests/mock/itineraries/itineraries.test.ts) | Update a transportation segment in the database |
| **DELETE /api/v1/users/:userId/itineraries/:itineraryId/transportation-segments/:segmentId** | [`backend/tests/no-mock/itineraries/itineraries.test.ts`](../backend/tests/no-mock/itineraries/itineraries.test.ts) | [`backend/tests/mock/itineraries/itineraries.test.ts`](../backend/tests/mock/itineraries/itineraries.test.ts) | Remove a transportation segment from an itinerary in the database |

#### 2.1.2. Commit Hash Where Tests Run

`[Insert Commit SHA here]`

#### 2.1.3. Explanation on How to Run the Tests

1. Add a reachable `MONGODB_URI` to `backend/.env`. The no-mock suites create uniquely named temporary databases and drop them after the run.
2. From `backend`, install dependencies with `npm install`.
3. Run the user and itinerary API suites with coverage:
   ```sh
   npm run test:no-mock
   ```
4. Run the mocked database-failure and race-condition suites with coverage:
   ```sh
   npm run test:mock
   ```

### 2.2. GitHub Actions Configuration Location

`~/.github/workflows/backend-tests.yml`

### 2.3. Jest Coverage Report Screenshots With Mocks

Run `npm run test:mock` to generate the per-file HTML report under `backend/coverage/mock`. 
![Users Coverage](https://raw.githubusercontent.com/islamhampriv/ExploreUBC/main/documentation/images/mock-users.png)
![Itineraries Coverage](https://raw.githubusercontent.com/islamhampriv/ExploreUBC/main/documentation/images/mock-itineraries.png)
The current mocked user and itinerary suites cover 55.82% of statements, 40.85% of branches, 64.70% of functions, and 55.93% of lines. Lower standalone mock coverage is expected because these tests focus on database exceptions and concurrency-related error handling rather than normal API behavior.

### 2.4. Jest Coverage Report Screenshots Without Mocks


Run `npm run test:no-mock` to generate the per-file HTML report under `backend/coverage/no-mock`.
![Users Coverage](https://raw.githubusercontent.com/islamhampriv/ExploreUBC/main/documentation/images/no-mock-users.png)
![Itineraries Coverage](https://raw.githubusercontent.com/islamhampriv/ExploreUBC/main/documentation/images/no-mock-itineraries.png)
The current user and itinerary suites cover 96.11% of statements, 93.11% of branches, 99.01% of functions, and 96.09% of lines.

The no-mock Jest configuration enforces minimum global thresholds of 95% statements, 90% branches, 95% functions, and 95% lines.

### 2.5. Jest Coverage Report Screenshots (Mock + No-Mocks)


Run `npm run test:combined` to generate the per-file HTML report under `backend/coverage/combined`.
![Users Coverage](https://raw.githubusercontent.com/islamhampriv/ExploreUBC/main/documentation/images/combined-users.png)
![Itineraries Coverage](https://raw.githubusercontent.com/islamhampriv/ExploreUBC/main/documentation/images/combined-itineraries.png)
Statements: 98.05%, Branches: 96.91%, Functions: 99.01%, Lines: 98.04%

### 2.6. Coverage Justifications

The no-mock run is intentionally below 100% because database/network failure branches require controlled mocks, empty Express path parameters cannot be produced through normal HTTP routing, and race conditions in which a document is removed between a service read and write cannot be triggered deterministically without mocking.
The main remaining branches are unreachable empty Express route parameters and `findPreferences`, which belongs to the currently excluded recommendation flow. `*Types.ts` and `*Repository.ts` files contain only TypeScript types and interfaces that are erased at runtime, so both focused coverage configurations exclude them. The mock suites cover the controllable database-failure and concurrency-related branches.

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

- Itinerary scenarios: [`frontend/app/src/androidTest/java/com/exploreUBC/cpen321application/e2e/itineraries/ItineraryE2ETest.kt`](../frontend/app/src/androidTest/java/com/exploreUBC/cpen321application/e2e/itineraries/ItineraryE2ETest.kt)
- User scenarios: [`frontend/app/src/androidTest/java/com/exploreUBC/cpen321application/e2e/users/UserProfileE2ETest.kt`](../frontend/app/src/androidTest/java/com/exploreUBC/cpen321application/e2e/users/UserProfileE2ETest.kt)

### 4.2. Tests

- **Use Case: Update Preferences** (Users)

  - **Expected Behaviors:**
    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | 1. The user opens the user profile tab. | Open User Profile screen |
    | 2. The app shows the logged in userName, the user's start location input field, transportation, budget, interest dropdowns, and a `Saved Prefrences` button. | Check that the user name is present.<br>Check the start location input is present and enabled.<br>Check the three preferences dropdowns are present and enabled.<br>Check the save button is present and enabled. |
    | 2a. The user enters profile details that exceed the supported length. | Click the `Edit` button.<br>Input a 201-character value in the `Display name` field and a valid value in the `Starting location` field. |
    | 2a1. The profile length error is displayed. | Check that the `Save profile` button is disabled.<br>Check that the red error message `Display name must be at most 200 characters.` is present. |
    | 3. The user inputs a new start location. | Input "12345 abc Avenue, Vancouver BC" in the text field. |
    | 4. The user selects another transportation preference | Click the transportation dropdown <br> Check the list option 'walking', 'cycling', 'transit','driving','other' appears. <br>Select "transit" and check if the field value is 'transit'    |
    | 5. The user selects another budget preference | Click the budget dropdown <br> Check the list option 'economical', 'balanced', 'premium' appears. <br>Select "balanced" and check if the field value is 'balanced'    |
    | 6. The user selects another interest preference | Click the interest dropdown <br> Check the list option 'food', 'art', 'outdoors', 'markets', 'live_music', 'gaming' appears. <br>Select "outdoors" and check if the field value is 'outdoor'    |
    | 7. The user saves the user profile change | Click the `Save preferences` button <br>Check the confirmation message "Preference saved` appears   |

- **Use Case: Create an itinerary** (Itineraries)

  - **Expected Behaviors:**
    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | 1. The user opens the Trips page | Open the Trips page and <br> Check that the "Create" button is present |
    | 2. The user clicks the 'Create' button  | Check that a blank itinerary details form is present  |
    | 3. The user clicks 'back to itineraries' button  | Check that a new itinerary is present under "Saved Itineraries" |

- **Use Case: Modify an itinerary** (Itineraries)

  - **Expected Behaviors:**
    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | 1. The user opens an itinerary under the "Saved Itineraries" list| Open the Itinerary details <br> Check that the Itinerary details form is present and the `edit` button is present |
    | 2. The user clicks the 'edit' button  | Check that input forms "Name", "Description", and "Starting location" are present and enabled. <br> Check that the "Save details" button is present  |
    | 3. The user types "new name" under "Name" field, "new description" under "Description" field, and "UBC nest" under "Starting Location" field, | Input "new name" in the "Name" text field. Input "new description" in the "Description" text field. Input "ubc nest" in the "Starting Location" text field.    |
    | 3a. The user enters itinerary details that exceed the supported length. | Input a 101-character value in the `Name` field. |
    | 3a1. The itinerary length error is displayed. | Check that the `Save details` button is disabled.<br>Check that the red error message `Name must be at most 100 characters.` is present. |
    | 4. The user clicks the  "Save details" button  | Check that the message "Itinerary details saved" is present <br> Check that the fields are updated with the input from step 3 and disabled.  |

- **Use Case: Add an activity** (Itineraries)

  - **Expected Behaviors:**
    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | 1. The user opens an itinerary under the "Saved Itineraries" list| Open the Itinerary details <br> Check that the "Itinerary details form is present and the `+ Activity` button is present" |
    | 2. The user clicks the `+ Activity` button  | Check that an empty new activty form is present <br> Check that under "Category" dropdown, the list options "food", "art", "outdoors", "markets", "live_music", "gaming", "museum", "other" are present. <br> Check that the "Add" button is disabled. |
    | 3. The user types "new activity" under "Name", selects "outdoors" under the required "Category" dropdown, inputs "UBC nest" under "Location", and sets start time to "2027-11-30 14:00:00" and end time to "2027-11-30 15:00:00". The optional "Image URL" is left blank. | Input "new activity" in the "Name" field.<br>Select "outdoors" in the "Category" dropdown.<br>Input "UBC nest" in the "Location" field.<br>Leave "Image URL (optional)" blank.<br>Check that the start and end times are displayed. |
     | 3a. The user enters an invalid time range.   | Set start time to "2027-11-30 14:00:00", Set end time to "2027-11-30 13:00:00"|
     | 3a1. The error message is displayed | Check the "Add" button is disabled <br> Check the error message "End time must be after start time." is present |
    | 3b. The user enters an activity name that exceeds the supported length. | Input a 201-character value in the `Name` field.<br>Keep all other required fields valid. |
    | 3b1. The activity length error is displayed. | Check that the `Add` button is disabled.<br>Check that the red error message `Name must be at most 200 characters.` is present. |
    | 4. The user clicks the  "Add" button  | Check that the "Itinerary details" page is present <br> Check that the new activity is present (check the activity name, start time and end time, and the "Activity" tag is present). |

- **Use Case: Add a transportation** (Itineraries)

  - **Expected Behaviors:**
    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | 1. The user opens an itinerary under the "Saved Itineraries" list| Open the Itinerary details <br> Check that the "Itinerary details form is present and the `+ Transportation` button is present" |
    | 2. The user clicks the `+ Transportation` button  | Check that an empty new transportation form is present <br> Check that the under "Mode" dropdown, the list options "walking", "cycling", "transit", "driving", "other" are present. <br> Check that the "Add" button is disabled. |
    | 3. The user selects "walking" under the "Mode" dropdown, inputs "UBC Nest" under "Start location", "UBC MCLD" under "End location", and `400` under "Distance in meters", then sets start time to "2027-11-30 14:00:00" and end time to "2027-11-30 15:00:00". | Check that the "walking" option is selected for the "Mode" dropdown.<br>Input "UBC Nest" in the "Start location" field.<br>Input "UBC MCLD" in the "End location" field.<br>Input `400` in the "Distance in meters" field.<br>Check that the start and end times are displayed. |
     | 3a. The user enters an invalid distance.   | Input "abc" in distance field|
     | 3a1. The error message is displayed | Check the "Add" button is disabled <br> Check the error message "Distance must be a number." is present |
    | 3b. The user enters an invalid time range. | Set start time to `2027-11-30 14:00:00` and end time to `2027-11-30 13:00:00`. |
    | 3b1. The time-range error is displayed. | Check that the `Add` button is disabled.<br>Check that the error message `End time must be after start time.` is present. |
    | 3c. The user enters a transportation location that exceeds the supported length. | Input a 501-character value in the `Start location` field.<br>Keep all other required fields valid. |
    | 3c1. The transportation length error is displayed. | Check that the `Add` button is disabled.<br>Check that the red error message `Start location must be at most 500 characters.` is present. |
    | 4. The user clicks the  "Add" button  | Check that the "Itinerary details" page is present <br> Check that the new transportation is present (check the transportation mode, start time and end time, and the "Transportation" tag is present). |

- **Use Case: Modify an activity** (Itineraries)

  - **Expected Behaviors:**
    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | 1. The user opens an activity from the Itinerary Details page | Check that "Edit" button is present <br> Check that the Activity form is present. |
    | 2. The user clicks the `Edit` button  | Check that the activity fields are enabled.<br>Check that "Name", "Category", "Location", "Start time", and "End time" have required-field asterisks.<br>Check that "Image URL" is marked optional. |
    | 3. The user types "edited activity" under "Name", selects "art" under "Category", inputs "UBC Nest" under "Location", and sets start time to "2027-11-30 14:00:00" and end time to "2027-11-30 15:00:00". | Input "edited activity" in the "Name" field.<br>Select "art" in the "Category" dropdown.<br>Input "UBC Nest" in the "Location" field.<br>Leave "Image URL (optional)" blank.<br>Check that the start and end times are displayed. |
    | 4. The user clicks the  "Save" button  | Check that the message "Activity saved." is present. |

- **Use Case: Modify a transportation** (Itineraries)

  - **Expected Behaviors:**
    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | 1. The user opens a transportation segment from the Itinerary Details page | Check that the "Edit" button is present.<br>Check that the transportation details are present. |
    | 2. The user clicks the `Edit` button  | Check that the fields from Transportation form are enabled.  |
    | 3. The user selects "transit" under the "Mode" dropdown, inputs "UBC Nest" under "Start location", "UBC Exchange" under "End location", and `400` under "Distance in meters", then sets start time to "2027-11-30 14:00:00" and end time to "2027-11-30 15:00:00". | Check that the "transit" option is selected.<br>Input the start location, end location, and distance values.<br>Check that the start and end times are displayed. |
    | 3a. The user enters an invalid distance. | Replace the value in the `Distance in meters` field with `abc`. |
    | 3a1. The distance error is displayed. | Check that the `Save` button is disabled.<br>Check that the error message `Distance must be a number.` is present. |
    | 3b. The user enters an invalid time range. | Set start time to `2027-11-30 14:00:00` and end time to `2027-11-30 13:00:00`. |
    | 3b1. The time-range error is displayed. | Check that the `Save` button is disabled.<br>Check that the error message `End time must be after start time.` is present. |
    | 3c. The user enters a transportation location that exceeds the supported length. | Input a 501-character value in the `Start location` field.<br>Keep all other required fields valid. |
    | 3c1. The transportation length error is displayed. | Check that the `Save` button is disabled.<br>Check that the red error message `Start location must be at most 500 characters.` is present. |
    | 4. The user clicks the  "Save" button  | Check that the message "Transportation saved." is present. |

- **Use Case: Remove an Activities** (Itineraries)

  - **Expected Behaviors:**
    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | 1. The user opens an activity from the Itinerary Details page | Check that "Delete" button is present. |
    | 2. The user clicks the 'delete' button  | Check that a dialog with a message "Delete Item?" is present.  |
    | 3. The user clicks the 'yes' button from the dialog | Check that Itineray Details page <br> Check that the message "Item deleted." is present <br> Check that the deleted activity is not present".|

- **Use Case: Remove a Transportation** (Itineraries)

  - **Expected Behaviors:**
    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | 1. The user opens an Transportation from the Itinerary Details page | Check that "Delete" button is present. |
    | 2. The user clicks the 'delete' button  | Check that a dialog with a message "Delete Item?" is present.  |
    | 3. The user clicks the 'yes' button from the dialog | Check that Itineray Details page <br> Check that the message "Item deleted." is present <br> Check that the deleted transportation is not present".|

- **Use Case: Delete an itinerary** (Itineraries)

  - **Expected Behaviors:**
    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | 1. The user opens an itinerary under the "Saved Itineraries" list| Open the Itinerary details <br> Check that the Itinerary details form is present and the `delete` button is present. |
    | 2. The user clicks the 'delete' button  | Check that a dialog with a message "Delete Itinerary?" is present.  |
    | 3. The user clicks the 'yes' button from the dialog | Check that Trip Screen is present <br> Check that the message "Itinerary deleted." is present <br> Check that the deleted itinerary is not present under "Saved itineraries".|


    <!-- | 3a. The user inputs an ill-formatted string. | Input "_^_^^OQ#$" in the text field. |
    | 3a1. The app displays an error message prompting the user for the expected format. | Check that a dialog is opened with the text: "Please use only alphanumeric characters ". |
    | 4. The user presses the "Add" button. | Click the button labelled "add ". |
    | 5. The screen refreshes and the new item is at the bottom of the todo list. | Check that a text box with the text "buy milk" is present on screen.<br>Input "buy chocolate" in the text field.<br>Click the button labelled "add".<br>Check that two text boxes are present on the screen with "buy milk" on top and "buy chocolate" at the bottom. |
    | 5a. The list exceeds the maximum todo-list size. | Repeat steps 3 to 5 ten times.<br>Check that a dialog is opened with the text: "You have too many items, try completing one first". | -->

  - **Test Logs:**
    ```
    Starting 9 tests on Medium_Phone_API_36.0(AVD) - 16
    Finished 9 tests on Medium_Phone_API_36.0(AVD) - 16
    BUILD SUCCESSFUL
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
