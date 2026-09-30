# Requirements and Design

## 1. Change History

| **Change Date**   | **Modified Sections** | **Rationale** |
| ----------------- | --------------------- | ------------- |
| _Nothing to show_ |

---

## 2. Project Description

[WRITE_PROJECT_DESCRIPTION_HERE]

---

## 3. Requirements Specification

### **3.1. List of Features**


### **3.2. Use Case Diagram**


### **3.3. Actors Description**
1. **[WRITE_NAME_HERE]**: ...
2. **[WRITE_NAME_HERE]**: ...

### **3.4. Use Case Description**
#### Use Cases for Feature 1: Itinerary Recommendation

1. **Generate Itineraries**: The user enters a starting location, available time, transportation mode, and optional interests or constraints. The system combines these inputs with the user's profile, generates candidate activities, verifies their locations, calculates travel and transportation costs, and returns four ranked itineraries. Generating a new set replaces the user's previous recommendation set only after generation succeeds.
2. **Review Recommendations**: The user reviews the latest generated itineraries and compares their activities, schedules, transportation segments, estimated costs, and ranking explanations. The latest recommendation set remains available after the user closes and reopens the application.
3. **Save a Recommendation**: The user selects a generated recommendation to save. The system copies it into the user's saved itineraries, after which the itinerary can be edited through Itinerary Management.

#### Use Cases for Feature 2: Itinerary Management

1. **Create an Itinerary**: The user creates a blank saved itinerary and provides its basic details.
2. **Modify Itinerary Details**: The user changes an itinerary's name or description.
3. **Add Activities or Transportation Segments**: The user adds an activity or transportation segment to a saved itinerary.
4. **Remove Activities or Transportation Segments**: The user removes an activity or transportation segment from a saved itinerary.
5. **Reorder Activities or Transportation Segments**: The user changes the order of activities and their associated transportation segments.
6. **Edit Activities or Transportation Segments**: The user changes details such as an activity's name, description, location, or time, or a transportation segment's mode and timing. The system recalculates affected travel information and itinerary totals when necessary.
7. **Delete an Itinerary**: The user permanently removes a saved itinerary.
8. **View an Itinerary on the Map**: The user views the itinerary's activity locations and transportation routes on a map.

#### Use Cases for Feature 3: User Profile

1. **Update Transportation Preferences**: The user selects preferred transportation modes for future itinerary and travel-cost recommendations.
2. **Update Budget Preferences**: The user sets or changes the default budget used for future recommendations.
3. **Update Interest Preferences**: The user selects interests, such as food, art, or outdoor activities, that the system uses to personalize future itinerary and live-event recommendations.

#### Use Cases for Feature 4: Authentication

1. **Sign In with Google**: The user authenticates with Google, and the backend validates the Google identity before granting access to the associated ExploreUBC account.
2. **Sign Up with Google**: A first-time user authenticates with Google, after which the system creates an ExploreUBC account linked to the validated Google identity.
3. **Sign Out**: The user ends the current ExploreUBC session and returns to the authentication screen.

### **3.5. Formal Use Case Specifications (5 Most Major Use Cases)**
<a name="uc1"></a>

#### Use Case 1: Generate Itineraries (Feature 1)

**Description**: The user can generate four ranked itinerary recommendations based on trip constraints and profile preferences.

**Primary actor(s)**: User
    
**Main success scenario**:
1. The user opens the itinerary-generation screen.
2. The user enters a starting location, available time, budget, transportation mode, and any request-specific interests or constraints.
3. The user selects **Generate Itineraries**.
4. The system loads the user's saved profile preferences and validates the trip request.
5. The system generates candidate activities and resolves them into verified places.
6. The system obtains travel times and distances, estimates transportation costs, and removes candidates that violate the user's constraints.
7. The system ranks the feasible itineraries and saves the four highest-ranked candidates as the user's latest recommendation set.
8. The system displays the four recommendations with their activities, transportation segments, estimated costs, and ranking explanations.

**Failure scenario(s)**:
- 4a. One or more required trip inputs are missing or invalid.
    - 4a1. The system identifies the invalid fields.
    - 4a2. The user corrects the inputs and submits the request again.
- 5a. An external generation or place-resolution service is unavailable or returns invalid data.
    - 5a1. The system does not replace the user's previous recommendation set.
    - 5a2. The system informs the user that recommendations could not be generated and allows another attempt.
- 7a. Fewer than four feasible itineraries satisfy the user's constraints.
    - 7a1. The system displays the feasible itineraries that were found.
    - 7a2. The system suggests relaxing the time, transportation, or interest constraints.

<a name="uc2"></a>

#### Use Case 2: [ ] (Feature 2)

#### Use Case 3: Update Profile Preferences (Feature 3)

**Description**: The user can update the transportation, budget, or interest preferences used to personalize future recommendations.

**Primary actor(s)**: User

**Main success scenario**:
1. The user opens the profile screen.
2. The system displays the user's current preferences.
3. The user changes one or more transportation, budget, or interest preferences.
4. The user saves the changes.
5. The system validates and stores the updated preferences.
6. The system confirms the update and uses the new preferences for future itinerary and live-event recommendations.

**Failure scenario(s)**:
- 3a. The user enters an invalid budget value.
    - 3a1. The system identifies the invalid value.
    - 3a2. The user enters a valid value before saving.
- 5a. The profile update cannot be stored because of a server or network error.
    - 5a1. The system keeps the previously saved preferences unchanged.
    - 5a2. The system informs the user that the update failed and allows another attempt.

<a name="uc3"></a>

#### Use Case 4: Sign In with Google (Feature 4)

**Description**: The user can sign in with Google to access an existing ExploreUBC account.

**Primary actor(s)**: User

**Supporting actor(s)**: Google Authentication

**Main success scenario**:
1. The user selects **Sign In with Google**.
2. The application opens the Google authentication flow.
3. The user selects a Google account and grants the requested authentication permissions.
4. Google returns proof of the authenticated identity to the application.
5. The application sends the Google identity token to the ExploreUBC backend.
6. The backend validates the token and finds the associated ExploreUBC account.
7. The backend establishes an ExploreUBC session.
8. The application opens the authenticated home screen.

**Failure scenario(s)**:
- 3a. The user cancels Google authentication.
    - 3a1. The application returns to the authentication screen without creating a session.
- 4a. Google authentication fails.
    - 4a1. The application displays an authentication error and allows the user to try again.
- 6a. The backend rejects an invalid or expired Google identity token.
    - 6a1. The application does not create a session.
    - 6a2. The application asks the user to restart the Google sign-in flow.

### **3.6. Screen Mock-ups**


### **3.7. Non-Functional Requirements**
<a name="nfr1"></a>

1. **[WRITE_NAME_HERE]**
    - **Description**: ...
    - **Justification**: ...
2. ...

---

## 4. Designs Specification
### **4.1. Main Components**
1. **[WRITE_NAME_HERE]**
    - **Purpose**: ...
    - **Interfaces**: 
        1. ...
            - **Purpose**: ...
        2. ...
2. ...


### **4.2. Databases**
1. **[WRITE_NAME_HERE]**
    - **Purpose**: ...
2. ...


### **4.3. External Modules**
1. **[WRITE_NAME_HERE]** 
- **Purpose**: ...
2. ...


### **4.4. Frameworks and Libraries**
1. **[WRITE_NAME_HERE]**
    - **Purpose**: ...
    - **Reason**: ...
2. ...


### **4.5. Dependencies Diagram**


### **4.6. Use Case Sequence Diagram (5 Most Major Use Cases)**
1. [**[WRITE_NAME_HERE]**](#uc1)\
[SEQUENCE_DIAGRAM_HERE]
2. ...


### **4.7. Design of Non-Functional Requirements**
1. [**[WRITE_NAME_HERE]**](#nfr1)
    - **Implementation**: ...
2. ...
