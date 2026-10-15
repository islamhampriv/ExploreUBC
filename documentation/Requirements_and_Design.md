# Requirements and Design

## 1. Change History

| **Change Date**   | **Modified Sections** | **Rationale** |
| ----------------- | --------------------- | ------------- |
| October 1, 2026   | 3.3, 3.4, 3.5         | Added actors, use case descriptions, and formal use case specifications for Feature Sets 2 and 3. |

---

## 2. Project Description

[WRITE_PROJECT_DESCRIPTION_HERE]

---

## 3. Requirements Specification

### **3.1. List of Features**

### **3.2. Use Case Diagram**

![My diagram](images/Usecase_diagram.png)

### **3.3. Actors Description**

1. **Exchange Student**: The primary user of ExploreUBC. An incoming or current UBC exchange student who plans trips around campus and Vancouver, maintains a travel profile, creates itineraries, views maps, estimates transportation costs, and receives live event recommendations.
2. **Google Maps Service**: An external mapping service that supplies map tiles, geocoded activity coordinates, transportation routes between itinerary stops, and popular nearby locations.
3. **Event Information Service**: An external or backend-fed source of upcoming local events (for example concerts, shows, and campus events) used to generate in-app live recommendations.


### **3.4. Use Case Description**
#### Use Cases for Feature 1: Itinerary Recommendation

1. **Generate Itineraries**: The user can generate four ranked itinerary recommendations based on trip constraints and profile preferences.
2. **Review Recommendations**: The user reviews the latest generated itineraries and compares their activities, schedules, transportation segments, and rankings.
3. **Save a Recommendation**: The user selects a generated recommendation to save.

#### Use Cases for Feature 2: Itinerary Management

1. **Create an Itinerary**: The user creates a blank saved itinerary and provides its basic details.
2. **Modify Itinerary Details**: The user changes an itinerary's name or description.
3. **Add Activities or Transportation Segments**: The user adds an activity or transportation segment to a saved itinerary.
4. **Remove Activities or Transportation Segments**: The user removes an activity or transportation segment from a saved itinerary.
5. **Reorder Activities or Transportation Segments**: The user changes the order of activities and their associated transportation segments.
6. **Edit Activities or Transportation Segments**: The user changes details such as an activity's name, description, location, or time, or a transportation segment's mode and timing.
7. **Delete an Itinerary**: The user permanently removes a saved itinerary.
8. **View an Itinerary on the Map**: The user views the itinerary's activity locations and transportation routes on a map.

#### Use Cases for Feature 3: User Profile

1. **Update Transportation Preferences**: The user selects preferred transportation modes for future itinerary recommendations.
2. **Update Budget Preferences**: The user sets or changes the default budget used for future recommendations.
3. **Update Interest Preferences**: The user selects interests, such as food, art, or outdoor activities, that the system uses to personalize future itinerary and live-event recommendations.

#### Use Cases for Feature 4: Authentication

1. **Sign In with Google**: The user authenticates with Google, and the backend validates the Google identity before granting access to the associated ExploreUBC account.
2. **Sign Up with Google**: A first-time user authenticates with Google, after which the system creates an ExploreUBC account linked to the validated Google identity.
3. **Sign Out**: The user ends the current ExploreUBC session and returns to the authentication screen.
- Use cases for feature 1: [WRITE_FEATURE_1_NAME_HERE]

1. **[WRITE_NAME_HERE]**: ...
2. **[WRITE_NAME_HERE]**: ...

- Use cases for feature set 2: Google Maps Integration and Transportation Budget Calculator
3. **View Itinerary on Map**: The exchange student selects a saved itinerary and the system plots its activity locations and connecting transportation routes on an interactive Google Map.
4. **Browse Popular Locations**: The exchange student opens the map and the system displays pins for popular locations in the current map region so the student can inspect them and consider adding them to a trip.


### **3.5. Formal Use Case Specifications (5 Most Major Use Cases)**
The five specifications below are the major use cases for Feature Sets 2 and 3.

<a name="uc1"></a>

#### Use Case 1: Generate Itineraries (Feature 1)

**Description**: The user can generate four ranked itinerary recommendations based on trip constraints and profile preferences.

**Primary actor(s)**: User
    
**Main success scenario**:
1. The user opens the itinerary-generation screen.
2. The user enters a starting location, available time, budget, transportation mode, and any request-specific interests or constraints.
3. The user selects **Generate Itineraries**.
4. The system generates and ranks four feasible candidate itineraries.
5. The system displays the four recommendations with their activities, transportation segments and rankings.

**Failure scenario(s)**:
- 3a. One or more required trip inputs are missing or invalid.
    - 3a1. The system identifies the invalid fields.
    - 3a2. The user corrects the inputs and submits the request again.
- 4a. An external generation or place-resolution service is unavailable or returns invalid data.
    - 4a1. The system does not replace the user's previous recommendation set.
    - 4b2. The system informs the user that recommendations could not be generated and allows another attempt.
- 4b. Fewer than four feasible itineraries satisfy the user's constraints.
    - 4b1. The system displays the feasible itineraries that were found.
    - 4b2. The system suggests relaxing the time, transportation, or interest constraints.

<a name="uc2"></a>

#### Use Case 2: Edit an Activity (Feature 2)

**Description**: The user changes an activity's name, description, location, or duration.

**Primary actor(s)**: User

**Main success scenario**:
1. The user opens a saved itinerary.
2. The user selects an activity.
3. The system displays the activity's details.
4. The user selects `Edit`.
5. The user edits the activity's name, duration, location, or description.
6. The user selects `Save`.
7. The system saves and displays the updated activity.

**Failure scenario(s)**:
- 5a. The user enters an invalid start or end time.
    - 5a1. The system disables **Save**.
    - 5a2. The system displays an error message.
- 6a. The activity update cannot be stored because of a server or network error.
    - 6a1. The system leaves the previously saved activity unchanged.
    - 6a2. The system informs the user that the update failed and allows another attempt.

#### Use Case 3: Update Profile Preferences (Feature 3)

**Description**: The user can update the transportation, budget, or interest preferences used to personalize future recommendations.

**Primary actor(s)**: User

**Main success scenario**:
1. The user opens the profile screen.
2. The system displays the user's current preferences.
3. The user changes one or more transportation, budget, or interest preferences.
4. The user saves the changes.
5. The system stores and confirms the update.

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
3. The user selects a Google account.
4. The system validates the account and establishes the user session.
5. The application opens the authenticated home screen.

**Failure scenario(s)**:
- 3a. The user cancels Google authentication.
    - 3a1. The application returns to the authentication screen without creating a session.
- 4a. Google authentication fails.
    - 4a1. The application displays an authentication error and allows the user to try again.
- 4b. The backend rejects an invalid or expired Google identity token.
    - 4b1. The application does not create a session.
    - 4b2. The application asks the user to restart the Google sign-in flow.

<a name="uc5"></a>

#### Use Case 5: View Itinerary on Map

**Description**: The exchange student visualizes a selected trip itinerary on Google Maps, including activity pins and transportation routes between consecutive activities.

**Primary actor(s)**: Exchange Student

**Main success scenario**:
1. The exchange student opens ExploreUBC and navigates to their saved itineraries.
2. The exchange student selects the itinerary they want to visualize.
3. The exchange student opens the map view for that itinerary.
4. The system loads the itinerary's scheduled activities (type, name, start/end time, location) and transportation segments (type, start/end time, pick-up/drop-off location, distance).
5. The system requests geocoded coordinates, map tiles, and route polylines from the Google Maps Service.
6. The Google Maps Service returns valid coordinates, map data, and routes for the itinerary.
7. The system displays the map with activity pins in chronological order and drawn routes between consecutive activities.
8. The exchange student pans, zooms, and taps pins to inspect activity and route details.

**Failure scenario(s)**:
- 1a. The exchange student is not authenticated.
    - 1a1. The system prompts the student to sign in.
    - 1a2. After successful authentication, the use case resumes at step 1. Otherwise, the use case ends.

- 2a. The student has no saved itineraries.
    - 2a1. The system shows an empty-state message explaining that an itinerary must be created first.
    - 2a2. The use case ends.

- 2b. The selected itinerary contains no activities.
    - 2b1. The system informs the student that the itinerary has nothing to plot.
    - 2b2. The use case ends.

- 4a. The system cannot load itinerary data because of a network or database error.
    - 4a1. The system displays an error and offers a retry action.
    - 4a2. If retry succeeds, the use case resumes at step 4. Otherwise, the use case ends.

- 5a. One or more activities have a missing or invalid location.
    - 5a1. The system plots the activities that can be geocoded and marks the invalid stops as unmapped.
    - 5a2. The system warns the student that some activities could not be shown on the map.
    - 5a3. The use case continues with the remaining mapped activities.

- 6a. The Google Maps Service is unavailable or returns an error.
    - 6a1. The system displays a map-unavailable message and offers the existing list view of the itinerary.
    - 6a2. The use case ends.

- 6b. A route cannot be computed between two consecutive activities.
    - 6b1. The system still shows both activity pins and omits only the failed route segment.
    - 6b2. The system notifies the student that this transportation leg could not be drawn.
    - 6b3. The use case continues with the remaining routes.

<a name="uc6"></a>


#### Use Case 6: Browse Popular Locations

**Description**: The exchange student views popular locations as pins on the map for the current region (UBC campus, the itinerary area, or the student's surroundings) and inspects a pin for details.

**Primary actor(s)**: Exchange Student

**Main success scenario**:
1. The exchange student opens the map or explore view in ExploreUBC.
2. The system determines the map region to display, using the selected itinerary area, the student's current location, or a default UBC campus region.
3. The system requests popular locations in that region from the Google Maps Service.
4. The Google Maps Service returns a set of popular places with names, categories, and coordinates.
5. The system displays those places as pins on the map.
6. The exchange student taps a pin.
7. The system shows the location's name, category, and address, and offers an option to add it to an itinerary.

**Failure scenario(s)**:
- 1a. The exchange student is not authenticated.
    - 1a1. The system prompts the student to sign in.
    - 1a2. After successful authentication, the use case resumes at step 1. Otherwise, the use case ends.

- 2a. The student has denied location permission and no itinerary region is available.
    - 2a1. The system defaults the map region to the UBC campus area.
    - 2a2. The use case continues at step 3.

- 3a. The Google Maps Service request fails or times out.
    - 3a1. The system attempts to show any previously cached popular locations for the region.
    - 3a2. If cached data exists, the use case continues at step 5 with a stale-data notice. Otherwise, the system reports that popular locations could not be loaded and the use case ends.

- 4a. No popular locations are returned for the region.
    - 4a1. The system shows the map without popular-location pins and informs the student that none were found nearby.
    - 4a2. The use case ends.

- 6a. The selected pin's details cannot be loaded.
    - 6a1. The system displays an error that the location details are unavailable.
    - 6a2. The pin remains on the map and the use case ends for that selection.


<a name="uc7"></a>

#### Use Case 7: View Event Recommendations

**Description**: The user views event recommendations personalised to their interests and location. The system periodically retrieves event listings from an external event service to keep the recommendations current. The user can view an event’s details and add it to an itinerary.

**Primary actor(s)**: User

**Secondary actor(s)**: External Event Service

**Preconditions:**

- The user has saved interests and a location in their profile.
- The system has retrieved and stored event listings during a periodic check.

**Main success scenario**:

1. The user opens the live-events feature.
2. The system displays events matching the user’s interests and location.
3. The user selects an event.
4. The system displays the event’s details.
5. The user chooses to add the event to an itinerary.
6. The system displays the user's itineraries.
7. The user selects an itinerary.
8. The system verifies that the event is still available and stores it in the selected itinerary.

**Failure scenario(s)**:

- 2a. No events match the user’s interests and location.
  - 2a1. The system shows that no events are available at the moment.
- 6a. The user does not have a saved itinerary.
  - 6a1. The system informs the user that no itinerary is available and offers the option to create one.
  - 6a2. The user chooses to create an itinerary and enters its required details.
  - 6a3. The system creates the itinerary.
  - 6a4. The flow continues at step 8 using the newly created itinerary.
- 8a. The selected event is no longer available.
  - 8a1. The system tells the user the event can no longer be added.
  - 8a2. The system leaves the itinerary unchanged and lets the user choose another event.

<a name="uc8"></a>

#### Use Case 8: View Transportation Cost Estimate

**Description**: When an itinerary is created or modified, the system automatically calculates and stores a transportation cost estimate based on its transportation segments. The system uses the transportation mode and route information stored with each segment. For a car segment, it uses the vehicle type specified by the itinerary or, if none is specified, the vehicle type saved in the user's profile. The itinerary card displays the total estimated transportation cost, and the user can select it to open a detailed cost breakdown without first opening the itinerary.

**Primary actor**: User

**Supporting actor**: External fuel price data source

**Preconditions**:

- An itinerary containing transportation segments with route information has been created or modified.
- The system has attempted to calculate and store its total transportation cost and per-segment breakdown.

**Main success scenario**:

1. The user views a screen containing an itinerary card.
2. The system retrieves the itinerary's stored total transportation cost estimate.
3. The system displays the total estimated transportation cost on the itinerary card.
4. The user selects the transportation cost on the card.
5. The system retrieves the stored per-segment cost breakdown.
6. The system displays a pop-up containing the estimated cost for each transportation segment, including the mode and the pricing information used.

**Failure scenario**:

- 2a. The system could not calculate a total because route or pricing information was unavailable for one or more transportation segments.
  - 2a1. The itinerary card identifies the transportation cost as unavailable.
  - 2a2. The system identifies the affected segments when breakdown information is available.
- 2b. The system could not calculate a car segment because neither the itinerary nor the user's profile specified a vehicle type.
  - 2b1. The itinerary card identifies the transportation cost as unavailable.
  - 2b2. The system informs the user that a vehicle type must be added to the itinerary or their profile before the total can be estimated.
- 5a. The cost breakdown cannot be loaded because of a server or network error.
  - 5a1. The system keeps the itinerary card visible.
  - 5a2. The system informs the user that the breakdown is temporarily unavailable and allows another attempt.

**Postcondition:** The itinerary card displays its total estimated transportation cost and makes a per-segment breakdown available, or clearly indicates that the estimate is unavailable.

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
1. **Itineraries**
    - **Purpose**: Manages saved itineraries and their activities.
    - **Interfaces**:
        1. `Itinerary createItinerary(String userId, ItineraryDetails details)`
            - **Purpose [3.4.2.1]**: Creates a saved itinerary for a user.
        2. `Itinerary insertItinerary(String userId, ItineraryDetails details)`
            - **Purpose [3.4.2.1]**: Stores a new itinerary in MongoDB.
        3. `Itinerary getItinerary(String userId, String itineraryId)`
            - **Purpose [3.4.2.2, 3.4.2.8]**: Retrieves a user's saved itinerary.
        4. `Itinerary findItinerary(String userId, String itineraryId)`
            - **Purpose [3.4.2.2, 3.4.2.8]**: Retrieves an itinerary document from MongoDB.
        5. `Itinerary updateItineraryDetails(String userId, String itineraryId, ItineraryDetails details)`
            - **Purpose [3.4.2.2]**: Updates the details of a saved itinerary.
        6. `bool updateItineraryDetails(String userId, String itineraryId, ItineraryDetails details)`
            - **Purpose [3.4.2.2]**: Stores updated itinerary details in MongoDB.
        7. `Activity addActivity(String userId, String itineraryId, ActivityInput activity)`
            - **Purpose [3.4.2.3]**: Adds an activity to a saved itinerary.
        8. `bool insertEmbeddedActivity(String userId, String itineraryId, Activity activity)`
            - **Purpose [3.4.2.3]**: Stores an activity in an itinerary document in MongoDB.
        9. `TransportationSegment addTransportationSegment(String userId, String itineraryId, TransportationSegmentInput segment)`
            - **Purpose [3.4.2.3]**: Adds a transportation segment to a saved itinerary.
        10. `bool insertEmbeddedTransportationSegment(String userId, String itineraryId, TransportationSegment segment)`
            - **Purpose [3.4.2.3]**: Stores a transportation segment in an itinerary document in MongoDB.
        11. `bool removeActivity(String userId, String itineraryId, String activityId)`
            - **Purpose [3.4.2.4]**: Removes an activity from a saved itinerary.
        12. `bool removeEmbeddedActivity(String userId, String itineraryId, String activityId)`
            - **Purpose [3.4.2.4]**: Removes an activity from an itinerary document in MongoDB.
        13. `bool removeTransportationSegment(String userId, String itineraryId, String segmentId)`
            - **Purpose [3.4.2.4]**: Removes a transportation segment from a saved itinerary.
        14. `bool removeEmbeddedTransportationSegment(String userId, String itineraryId, String segmentId)`
            - **Purpose [3.4.2.4]**: Removes a transportation segment from an itinerary document in MongoDB.
        15. `Itinerary reorderActivities(String userId, String itineraryId, String[] orderedActivityIds)`
            - **Purpose [3.4.2.5]**: Reorders activities and their associated transportation segments.
        16. `bool updateItineraryOrder(String userId, String itineraryId, String[] orderedActivityIds, TransportationSegment[] segments)`
            - **Purpose [3.4.2.5]**: Stores an updated itinerary order in MongoDB.
        17. `Activity updateActivity(String userId, String itineraryId, String activityId, ActivityUpdate update)`
            - **Purpose [3.4.2.6]**: Updates an activity in a saved itinerary.
        18. `bool updateEmbeddedActivity(String userId, String itineraryId, String activityId, ActivityUpdate update)`
            - **Purpose [3.4.2.6]**: Stores an updated activity in MongoDB.
        19. `TransportationSegment updateTransportationSegment(String userId, String itineraryId, String segmentId, TransportationSegmentUpdate update)`
            - **Purpose [3.4.2.6]**: Updates a transportation segment in a saved itinerary.
        20. `bool updateEmbeddedTransportationSegment(String userId, String itineraryId, String segmentId, TransportationSegmentUpdate update)`
            - **Purpose [3.4.2.6]**: Stores an updated transportation segment in MongoDB.
        21. `bool deleteItinerary(String userId, String itineraryId)`
            - **Purpose [3.4.2.7]**: Deletes a user's saved itinerary.
        22. `bool removeItinerary(String userId, String itineraryId)`
            - **Purpose [3.4.2.7]**: Removes an itinerary document from MongoDB.
2. **Users**
     - **Purpose**: It represents ExploreUBC accounts and persistent traveler preferences. It supports CRUD operations on the account. It provides profile data to the `Recommendation` and `LiveEvents` components. It exchanges a verified external identity for an ExploreUBC session.
    - **Interfaces**:
        1. `UserPreferences getPreferences(String userId)`
            - **Purpose**: Retrieves a user's saved preferences.
        2. `UserPreferences findPreferences(String userId)`
            - **Purpose**: Retrieves a user's preferences from MongoDB.
        3. `UserPreferences updatePreferences(String userId, PreferencesUpdate update)`
            - **Purpose**: Updates a user's profile preferences.
        4. `bool updatePreferences(String userId, PreferencesUpdate update)`
            - **Purpose**: Stores updated preferences in MongoDB.
2. **LiveEvents**
     - **Purpose**: ...
    - **Interfaces**: 
        1. ...
            - **Purpose**: ...
        2. ...
2. **Recommendations**
     - **Purpose**: It owns itinerary generation, rank calculation, and the create and read of latest recommendation set. It's responsible for requesting the LLM for itineraries, persisting the latest recommendation set.
    - **Interfaces**:
        1. `RecommendationSet findLatestRecommendations(String userId)`
            - **Purpose [3.4.1.2]**: Retrieves a user's latest recommendation set from MongoDB.
        2. `RecommendationSet generateRecommendations(String userId, TripRequest tripRequest)`
            - **Purpose [3.4.1.1]**: Generates itinerary recommendations for a user.
        3. `String generateItineraries(TripRequest tripRequest, UserPreferences preferences)`
            - **Purpose [3.4.1.1]**: Requests generated itinerary JSON from the LLM Service.
        4. `Itinerary[] parseItineraries(String itineraryJson)`
            - **Purpose [3.4.1.1]**: Converts generated itinerary JSON into itinerary objects.
        5. `bool replaceLatestRecommendations(String userId, Itinerary[] rankedItineraries)`
            - **Purpose [3.4.1.1]**: Stores a user's latest recommendation set in MongoDB.
### **4.1.1. Additional Components To Be Defined**

1. **[WRITE_NAME_HERE]**
   - **Purpose**: ...
   - **Interfaces**:
     1. ...
        - **Purpose**: ...
     2. ...
2. ...

### **4.2. Databases**
1. **Users**
    - **Purpose**: Represents an application user in ExploreUBC
    ```javascript
        "googleSubject",
        "email",
        "displayName",
        "start_location",
        "preferences":{
            "transportation":{
                "mode": "'walking' | 'cycling' | 'transit' | 'driving' | 'other'"
            }
        "budget": "'economical' | 'balanced' | 'premium'",
        "interest": "'food' | 'art' | 'outdoors' | 'markets' | 'live_music' | 'gaming'",
        "create_at",
        "updated_at"
      },
    ```
2. **Itineraries**
    - **Purpose**: Represents a saved itinerary
    ```javascript
        type Itinerary = {
            id: string;
            ownerId: string;
            name: string;
            description?: string;
            startingLocation?: string;
            items: ItineraryItem[]:{
                type ActivityItem = {
                    id: string;
                    name: string;
                    image: string;
                    description?: string;
                    category?: string;
                    location: string;
                    startTime: string;
                    endTime: string;
                    };

                    type TransportationItem = {
                    id: string;
                    mode: 'walking' | 'cycling' | 'transit' | 'driving' | 'other';
                    startLocation: string;
                    endLocation: string;
                    distance_m: number;
                    startTime: string;
                    endTime: string;
                    };
            };
            createdAt: string;
            updatedAt: string;
            saved: boolean;
            inRecommendation: boolean;
            score: {
                // fields that contribute to the score calculation
            }
        };
    ```
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
#### 4.6.1. [**Generate Itineraries**](#uc1)

```mermaid
sequenceDiagram
    actor User
    participant Frontend
    participant Recommendations
    participant Users
    participant Itineraries
    participant LLM as LLM Service
    participant MongoDB

    User->>Frontend: void openGenerationScreen()
    Frontend-->>User: TripRequestForm getTripRequestForm()

    User->>Frontend: void submitTripRequest(TripRequest tripRequest)
    Frontend->>Frontend: ValidationResult validateTripRequest(TripRequest tripRequest)

    alt ValidationResult.isValid() == false
        Frontend-->>User: void displayValidationErrors(FieldError[] errors)

    else ValidationResult.isValid() == true
        Frontend->>Recommendations: RecommendationSet generateRecommendations(String userId, TripRequest tripRequest)

        Recommendations->>Users: UserPreferences getPreferences(String userId)
        Users-->>Recommendations: UserPreferences

        Recommendations->>LLM: String generateItineraries(TripRequest tripRequest, UserPreferences preferences)
        LLM-->>Recommendations: String itineraryJson

        alt LLM response and itinerary JSON are valid
            Recommendations->>Itineraries: Itinerary[] parseItineraries(String itineraryJson)
            Itineraries-->>Recommendations: Itinerary[] itineraries

            Recommendations->>Itineraries: Itinerary[] calculateCosts(Itinerary[] itineraries)
            Itineraries-->>Recommendations: Itinerary[] costedItineraries

            Recommendations->>Recommendations: Itinerary[] rankItineraries(Itinerary[] itineraries, UserPreferences preferences)

            Recommendations->>MongoDB: bool replaceLatestRecommendations(String userId, Itinerary[] rankedItineraries)
            MongoDB-->>Recommendations: bool stored

            Recommendations-->>Frontend: RecommendationSet
            Frontend-->>User: void displayRecommendations(RecommendationSet recommendations)

        else LLM response or itinerary JSON is invalid
            Note over Recommendations,MongoDB: Previous recommendations remain unchanged
            Recommendations-->>Frontend: GenerationError
            Frontend-->>User: void displayGenerationError(GenerationError error)
        end
    end
```

#### 4.6.2. [**Edit Activities**](#uc2)

```mermaid
sequenceDiagram
    actor User
    participant Frontend
    participant Itineraries
    participant MongoDB

    User->>Frontend: void openItinerary(String itineraryId)
    Frontend->>Itineraries: Itinerary getItinerary(String userId, String itineraryId)
    Itineraries->>MongoDB: Itinerary findItinerary(String userId, String itineraryId)
    MongoDB-->>Itineraries: Itinerary
    Itineraries-->>Frontend: Itinerary
    Frontend-->>User: void displayItinerary(Itinerary itinerary)

    User->>Frontend: void selectActivity(String activityId)
    Frontend->>Frontend: Activity findActivity(Itinerary itinerary, String activityId)
    Frontend-->>User: void displayActivityDetails(Activity activity)

    User->>Frontend: void openActivityEditor(String activityId)
    Frontend-->>User: ActivityForm getActivityForm(Activity activity)

    User->>Frontend: void updateActivityForm(ActivityUpdate update)
    Frontend->>Frontend: ValidationResult validateActivityUpdate(ActivityUpdate update)

    alt ValidationResult.isValid() == false
        Frontend-->>User: void disableSave()
        Frontend-->>User: void displayValidationErrors(FieldError[] errors)

    else ValidationResult.isValid() == true
        Frontend-->>User: void enableSave()
        User->>Frontend: void saveActivity()

        Frontend->>Itineraries: Activity updateActivity(String userId, String itineraryId, String activityId, ActivityUpdate update)
        Itineraries->>Itineraries: ValidationResult validateActivityUpdate(ActivityUpdate update)

        alt ValidationResult.isValid() == false
            Itineraries-->>Frontend: ValidationError
            Frontend-->>User: void displayValidationErrors(FieldError[] errors)

        else ValidationResult.isValid() == true
            Itineraries->>MongoDB: bool updateEmbeddedActivity(String userId, String itineraryId, String activityId, ActivityUpdate update)

            alt stored == true
                MongoDB-->>Itineraries: bool stored
                Itineraries-->>Frontend: Activity updatedActivity
                Frontend->>Frontend: void replaceActivity(Itinerary itinerary, Activity updatedActivity)
                Frontend-->>User: void displayActivityDetails(Activity updatedActivity)

            else stored == false
                MongoDB-->>Itineraries: StorageError
                Note over Itineraries,MongoDB: Previously stored itinerary remains unchanged
                Itineraries-->>Frontend: StorageError
                Frontend-->>User: void displaySaveError(StorageError error)
            end
        end
    end
```
1. [**[Use Case 6: Browse Popular Location]**](#uc6)
   ![Browse Popular Location sequence diagram](images/browse_popular_locations.png)
2. [**View Event Recommendations**](#uc7)\
   ![View Event Recommendations sequence diagram](images/view_events_seq_diagram.png)
3. [**Calculate Transportation Cost**](#uc8)\
   ![Calculate Transportation Cost sequence diagram](images/transportation_cost_seq_diagram.png)

### **4.7. Design of Non-Functional Requirements**

1. [**[WRITE_NAME_HERE]**](#nfr1)
   - **Implementation**: ...
2. ...
