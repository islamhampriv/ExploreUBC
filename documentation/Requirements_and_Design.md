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


### **3.3. Actors Description**
1. **Exchange Student**: The primary user of ExploreUBC. An incoming or current UBC exchange student who plans trips around campus and Vancouver, maintains a travel profile, creates itineraries, views maps, estimates transportation costs, and receives live event recommendations.
2. **Google Maps Service**: An external mapping service that supplies map tiles, geocoded activity coordinates, transportation routes between itinerary stops, and popular nearby locations.
3. **Event Information Service**: An external or backend-fed source of upcoming local events (for example concerts, shows, and campus events) used to generate in-app live recommendations.

### **3.4. Use Case Description**
- Use cases for feature 1: [WRITE_FEATURE_1_NAME_HERE]
1. **[WRITE_NAME_HERE]**: ...
2. **[WRITE_NAME_HERE]**: ...

- Use cases for feature set 2: Google Maps Integration and Transportation Budget Calculator
3. **View Itinerary on Map**: The exchange student selects a saved itinerary and the system plots its activity locations and connecting transportation routes on an interactive Google Map.
4. **Browse Popular Locations**: The exchange student opens the map and the system displays pins for popular locations in the current map region so the student can inspect them and consider adding them to a trip.
5. **Calculate Transportation Budget**: The exchange student requests a transportation cost estimate for an itinerary. The system computes costs from vehicle type, fuel efficiency, fuel price, and segment distances, then presents an itemized and total budget.

- Use cases for feature set 3: Live Event Recommendations
6. **Receive Live Event Recommendations**: While the exchange student is using the app, the system matches upcoming local events to the student's profile and surfaces relevant in-app notifications in real time.
7. **Review a Live Event Recommendation**: The exchange student opens an in-app live event recommendation, reviews its details, and either adds the event to an itinerary or dismisses it.

### **3.5. Formal Use Case Specifications (5 Most Major Use Cases)**
The five specifications below are the major use cases for Feature Sets 2 and 3.

<a name="uc1"></a>

#### Use Case 1: View Itinerary on Map

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

<a name="uc2"></a>

#### Use Case 2: Browse Popular Locations

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

<a name="uc3"></a>

#### Use Case 3: Calculate Transportation Budget

**Description**: The exchange student estimates the transportation cost of a selected itinerary. The system calculates per-segment and total cost using vehicle type, fuel efficiency, fuel price, and distance.

**Primary actor(s)**: Exchange Student

**Main success scenario**:
1. The exchange student opens a saved itinerary.
2. The exchange student chooses to calculate the transportation budget.
3. The system loads the itinerary's transportation segments, including vehicle/mode type and distance.
4. The system pre-fills calculation inputs from the student's profile or last-used values (vehicle type, fuel efficiency, and fuel price) and presents them for confirmation.
5. The exchange student reviews or edits the inputs and confirms the calculation.
6. The system computes an estimated cost for each driving segment and a total transportation budget.
7. The system displays an itemized breakdown (segment, distance, mode, estimated cost) and the total estimated cost.

**Failure scenario(s)**:
- 1a. The selected itinerary cannot be loaded.
    - 1a1. The system displays an error and offers a retry action.
    - 1a2. If retry succeeds, the use case resumes at step 1. Otherwise, the use case ends.

- 2a. The itinerary has no transportation segments.
    - 2a1. The system informs the student that there is no transportation to cost.
    - 2a2. The use case ends.

- 3a. One or more segments are missing distance.
    - 3a1. The system attempts to obtain missing distances from the Google Maps Service using pick-up and drop-off locations.
    - 3a2. If distance can be obtained, the use case continues. If not, the system excludes that segment, warns the student, and continues with the remaining segments. If no segment has a usable distance, the use case ends.

- 4a. The student's profile has no transportation preferences, so inputs cannot be pre-filled.
    - 4a1. The system presents empty input fields and asks the student to enter vehicle type, fuel efficiency, and fuel price.
    - 4a2. The use case continues at step 5.

- 5a. The student submits invalid inputs (for example a missing field, non-numeric value, or non-positive fuel efficiency or fuel price).
    - 5a1. The system highlights the invalid fields and explains the expected format and range.
    - 5a2. The student corrects the inputs, and the use case resumes at step 5.

- 5b. One or more segments use a non-driving mode (walking, cycling, or public transit) that cannot use the fuel-based formula.
    - 5b1. The system assigns a cost of $0 to walking and cycling segments.
    - 5b2. For public transit, the system either uses a student-entered fare if provided or excludes that segment from the fuel calculation and labels it as fare-not-estimated.
    - 5b3. The use case continues for the remaining driving segments.

- 6a. The calculation cannot be completed because required data is still missing after validation.
    - 6a1. The system reports that the transportation budget could not be calculated and lists the missing data.
    - 6a2. The use case ends.

<a name="uc4"></a>

#### Use Case 4: Receive Live Event Recommendations

**Description**: While the exchange student is using the app, the system fetches upcoming local events, ranks them against the student's profile (interests, budget preference, and transportation preferences), and shows in-app notifications for relevant events such as concerts or shows.

**Primary actor(s)**: Exchange Student

**Main success scenario**:
1. The exchange student is signed in and using ExploreUBC with an existing travel profile.
2. The system fetches currently upcoming local events from the Event Information Service.
3. The system filters out events that are in the past, already on the student's itinerary, or previously dismissed.
4. The system ranks the remaining events using the student's profile as context (interests, budget preference, and transportation preferences).
5. The system selects one or more highly ranked upcoming events.
6. The system displays an in-app notification with the event name, time, and location.
7. The exchange student sees the in-app recommendation.

**Failure scenario(s)**:
- 1a. The exchange student is not signed in.
    - 1a1. The system does not generate live event recommendations.
    - 1a2. The use case ends.

- 1b. The student has no profile, or the profile is missing interests and preferences.
    - 1b1. The system skips personalized ranking and does not send profile-based live recommendations.
    - 1b2. The system may prompt the student to complete their profile.
    - 1b3. The use case ends.

- 1c. The student has disabled in-app live event updates.
    - 1c1. The system does not display live event notifications.
    - 1c2. The use case ends.

- 2a. The Event Information Service is unavailable or returns an error.
    - 2a1. The system retries the fetch according to its live-update policy.
    - 2a2. If the retry fails, the system does not show a recommendation and remains silent (no stale or empty notification spam).
    - 2a3. The use case ends.

- 3a. After filtering, no upcoming events remain.
    - 3a1. The system does not display a notification.
    - 3a2. The use case ends.

- 4a. No remaining event is a strong enough match to the student's profile.
    - 4a1. The system does not display a low-relevance notification.
    - 4a2. The use case ends.

- 6a. The in-app notification cannot be presented because the student has navigated away from the app.
    - 6a1. The system queues the recommendation for the next time the student opens the app, subject to the event still being upcoming and relevant.
    - 6a2. The use case ends for the current session.

<a name="uc5"></a>

#### Use Case 5: Review a Live Event Recommendation

**Description**: The exchange student opens an in-app live event recommendation, reviews why it was suggested, and either adds it to an itinerary or dismisses it.

**Primary actor(s)**: Exchange Student

**Main success scenario**:
1. The exchange student taps an in-app live event recommendation.
2. The system loads the event's details (name, type, start/end time, location, and the profile reasons it was recommended).
3. The system displays the event details and actions to add the event to an itinerary or dismiss it.
4. The exchange student chooses to add the event to a selected itinerary.
5. The system checks that the event does not conflict with existing scheduled activities.
6. The system adds the event as an itinerary activity and confirms the update.
7. The system dismisses the notification so the same event is not recommended again.

**Failure scenario(s)**:
- 1a. The recommendation is no longer valid (the event was cancelled, sold out, or has already started).
    - 1a1. The system informs the student that the event is no longer available.
    - 1a2. The system dismisses the recommendation.
    - 1a3. The use case ends.

- 2a. Event details cannot be loaded because of a network or service error.
    - 2a1. The system displays an error and offers a retry action.
    - 2a2. If retry succeeds, the use case resumes at step 2. Otherwise, the use case ends.

- 4a. The student dismisses the recommendation instead of adding it.
    - 4a1. The system removes the in-app notification.
    - 4a2. The system records the dismissal so the same event is not immediately recommended again.
    - 4a3. The use case ends.

- 4b. The student has no itinerary to add the event to.
    - 4b1. The system prompts the student to create or select an itinerary.
    - 4b2. If the student creates or selects one, the use case resumes at step 5. Otherwise, the use case ends.

- 5a. The event overlaps an existing activity on the selected itinerary.
    - 5a1. The system warns the student about the scheduling conflict and shows the overlapping activity.
    - 5a2. The student either cancels the add or confirms adding the event anyway.
    - 5a3. If the student cancels, the event is not added and the use case ends. If the student confirms, the use case continues at step 6.

- 6a. The system fails to save the event to the itinerary.
    - 6a1. The system displays an error and offers a retry action.
    - 6a2. If retry succeeds, the use case continues at step 7. Otherwise, the itinerary is left unchanged and the use case ends.

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
