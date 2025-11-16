Awesome — this is a well-defined mechanical rework, and I can help you turn this into a **precise technical implementation plan** for the codebase. Below is a structured breakdown you can drop into a design doc (`LOUNGE_REWORK.md`) and use as guidance for the eventual engineering tasks.

I’ve preserved your gameplay intentions ✅ but translated them into:

✔ data model changes
✔ simulation logic changes
✔ UI changes
✔ migration/update plan
✔ phased rollout

---

# ✅ Lounge System Rework — Technical Specification

## 🧱 1️⃣ New Lounge Progression Model

Level 1:
    Capacity = Base Capacity
    Feature Slots: (Normal=1; Capacity=0; Unique=0)

Level 2:
    Capacity = Base_Capacity + Capacity_Increment
    Feature Slots: (Normal=2; Capacity=1; Unique=0)

Level 3:
    Capacity = Base_Capacity + 2 x Capacity_Increment
    Feature Slots: (Normal=3; Capacity=1; Unique=0)

Level 4:
    Capacity = Base_Capacity + 3 x Capacity_Increment
    Feature Slots: (Normal=3; Capacity=2; Unique=1)

Level 5:
    Capacity = Base_Capacity + 4 x Capacity_Increment
    Feature Slots: (Normal=5; Capacity=2; Unique=1)

Level 6:
    Capacity = Base_Capacity + 6 x Capacity_Increment
    Feature Slots: (Normal=6; Capacity=3; Unique=2)

Base Capacity = 5000 pax
Capacity_Increment = 7500 pax
Capacity_Slot_Bonus = 100000

---

## 🧮 2️⃣ Lounge Capacity Rules

* Capacity is a **hard cap** per simulation cycle (week)
* Lounge usage stats already exist in the DB → reuse them
* If more pax want to use than capacity → they are **blocked** (no satisfaction gain)

### UI Exposure (minimal)

✔ Show capacity
✔ Show current usage
✔ Show available slots and types

---

## 🎯 3️⃣ Lounge Features System

Features = installed upgrades using slots

Properties per Feature:

```scala
case class LoungeFeature(
   id: Int,
   name: String,
   cost: Int,
   category: FeatureCategory,
   capacityBoost: Int = 0,
   drawback: Option[Drawback] = None
)
```

Feature Categories:

```
- Usual
- CapacityUpgrade
- Unique
```

💸 Only gameplay effect initially: **cost sink**
✔ No pax satisfaction effects **yet**
✔ Just displayed in UI

This means Twirl modifications are limited to:

* Show available features
* Show installed features
* Show buy button

---

## 🏚️ 4️⃣ Lounge Condition & Renovation

Decay rule:

```
100% → 0% over (20 * 52) weeks  ~20 years
Linear decrement: 0.09615% per week
```

Effects:

* When < 100% → reduces lounge effectiveness (TBD)
* When 0% → no benefits

Auto-renovation setting:

* Player can choose a threshold (off by default)
* Renovation = restore to 100%

Cost:

```
renovationCost =
   (baseConstructionCost(level) +
    sum(featureCost)) * wearPercentage
```

Example:
If lounge is worth $10M and at 60%:
→ 40% worn → renovation is 4M

---

## 🚧 5️⃣ Construction Time Mechanics

### Build/upgrade delays

* Level 1 construction: 52–104 weeks configurable
* Level upgrades: shorter, e.g.

```
Level2: 26 weeks
Level3: 26
Level4: 39
Level5: 39
Level6: 52
```

* Feature installation: 2–8 weeks depending on category

While under construction:

* Lounges exist but **reduced loyalty gain** (later optional)
* Cannot be used? (config toggle)

Represent with new state:

```scala
enum LoungeStatus { Operational, UnderConstruction, ClosedForRenovation }
```

---

## 🧩 6️⃣ Required Codebase Changes

| Area                    | Change                                                                           |
| ----------------------- | -------------------------------------------------------------------------------- |
| Model                   | Add: `capacity`, `condition`, `features`, `status`, maybe `constructionProgress` |
| Persistence             | Update AirlineSource + related DAO                                               |
| Simulation              | Update rider usage → cap enforcement                                             |
| Simulation Tick         | Condition decay + auto-renovation checks                                         |
| UI Templates            | Display new stats / purchase actions                                             |
| Controllers             | Add endpoints to buy features / renovate / upgrade                               |
| AirportSimulation.scala | Remove reliance on ridership usage to *qualify* lounges                          |

---

## 🔌 7️⃣ Database Migration Plan

When deploying:

1. Add new DB columns:

```
capacity
condition (default 100)
features (json)
status (operational)
construction_progress
```

2. Set existing lounges:

* Level = map old → new (1→1, 2→3, 3→5 recommended)
* Capacity = retroactive based on level

3. Feature slots all empty at start

---

## 🧱 8️⃣ Phasing / Iterative Rollout

To avoid breakage:

| Phase     | Deliverable                                      |
| --------- | ------------------------------------------------ |
| ✅ Phase A | Extend Level structure + capacity + display only |
| Phase B   | Condition & Renovation mechanics                 |
| Phase C   | Features system (purchase + UI)                  |
| Phase D   | Construction delays                              |
| Phase E   | Rebalance Loyalty & Pax Satisfaction effects     |

We do not need to break everything at once.

---

## ✅ Next Actions (what I need from you)

To start coding Phase A, I will need:

✔ Confirm the exact **capacity baseline & increment values**
*(e.g. Base = 500 pax/week, Increment X = +300/level?)*

✔ Confirm **existing Level mapping**
Recommend:

```
Old L1 → New L1  
Old L2 → New L3  
Old L3 → New L5  
```

Or alternative mapping?

✔ Approve DB + Model field names

---
