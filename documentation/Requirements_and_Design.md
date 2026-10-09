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

- Use cases for feature 1: [WRITE_FEATURE_1_NAME_HERE]

1. **[WRITE_NAME_HERE]**: ...
2. **[WRITE_NAME_HERE]**: ...

- Use cases for feature set 2: Google Maps Integration and Transportation Budget Calculator
3. **View Itinerary on Map**: The exchange student selects a saved itinerary and the system plots its activity locations and connecting transportation routes on an interactive Google Map.
4. **Browse Popular Locations**: The exchange student opens the map and the system displays pins for popular locations in the current map region so the student can inspect them and consider adding them to a trip.


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

#### Use Case 3: View Event Recommendations

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

<a name="uc4"></a>

#### Use Case 4: View Transportation Cost Estimate

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

1. [**[Use Case 1: Browse Popular Location]**](#uc1)
   ![Browse Popular Location sequence diagram](images/browse_popular_locations.png)
2. [**View Event Recommendations**](#uc3)\
   ![View Event Recommendations sequence diagram](images/view_events_seq_diagram.png)
3. [**Calculate Transportation Cost**](#uc4)\
   ![Calculate Transportation Cost sequence diagram](images/transportation_cost_seq_diagram.png)

### **4.7. Design of Non-Functional Requirements**

1. [**[WRITE_NAME_HERE]**](#nfr1)
   - **Implementation**: ...
2. ...
