# Hotel Resort Reservation System - VIP Module

Author: Bryan Won Chu Ming

This NetBeans/Maven console project provides staff login, a system main menu,
the selected VIP room allocation module, and two management reports. It uses
the Entity-Control-Boundary (ECB) architectural pattern.

## Demonstration staff accounts

All demonstration accounts use the password `1234`.

| Username | Staff Name | Role | Assigned Module |
|---|---|---|---|
| `tang` | Tang Hong Yi | Front Desk Agent | Walk-In Booking |
| `bryan` | Bryan Won Chu Ming | VIP Manager | VIP Room Allocation |
| `bong` | Bong Xin Yee | Housekeeping Lead | Housekeeping |
| `carret` | Carret Chong Kar Loke | Service Lead | Front-Desk Service |

Login opens the main menu. Main menu option 2 opens the VIP module and option 5
opens the two reports. The other modules display an integration message until
their group members add their code. Logging out returns to the login screen.

## Priority rules

1. Diamond members have the highest priority.
2. Platinum members are next.
3. Elite members follow.
4. Guests in the same tier are ordered by registration time (FIFO).
5. A guest is allocated only to the requested room type.

The queue is a custom array-based binary max heap. The project does not use
`java.util.PriorityQueue`.

## ECB packages

- `entity`: staff, VIP guest, loyalty tier, room, and allocation record data.
- `control`: authentication, registration, heap allocation, room release, and
  report logic.
- `boundary`: all console interaction with the actor.
- `adt`: custom `PriorityQueueInterface` and `HeapPriorityQueue`.
- `utility`: static linear search, binary search, and merge sort algorithms.

Entities do not import boundary or control classes. The boundary imports only
control classes. The application main class is the composition root that wires
the boundary and controls together.

## Management reports

The system provides two structured console reports:

1. **VIP Priority Queue & Allocation Readiness Report**
   - Filters: exact guest ID, loyalty tier, preferred room type, and minimum
     waiting time.
   - Sorts: priority, waiting time, or guest ID.
   - Summary: tier breakdown, average wait, and guests immediately allocatable.

2. **VIP Room Allocation Performance Report**
   - Filters: exact guest ID, date range, loyalty tier, room type, and
     allocation status.
   - Sorts: allocation time, waiting time, or loyalty priority.
   - Summary: allocation totals, status/tier breakdown, average wait, and wait
     range.

Both reports combine binary/linear searching with merge sorting and display the
algorithms used in the report header.

## Run in NetBeans

1. Open this folder as a Maven project.
2. Confirm JDK 26 is selected.
3. Run `HotelResortReservationSystem.java`.

Sample rooms, waiting VIP guests, and completed allocation records are loaded at
startup so that the priority heap and reports can be demonstrated immediately.

## Verification

`src/test/java` contains a dependency-free verification runner covering heap
ordering, loyalty-tier/FIFO priority, automatic room allocation, and report
filtering. Run it with assertions enabled if desired:

```text
java -ea control.VipRoomAllocationSystemTest
```
