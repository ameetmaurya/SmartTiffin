# 🌿 NutriDeliver — India's First Smart Hostel Food Delivery Ecosystem

> ⚡ **No platform like this exists yet.**
> NutriDeliver is the first end-to-end intelligent food delivery system
> built exclusively for hostels and campuses — combining real-time kitchen
> management, AI-powered demand forecasting, live delivery tracking, and
> charity meal donation in one unified platform.

---

## 🚀 Why NutriDeliver is Different

| Feature | NutriDeliver | Swiggy/Zomato | Mess Systems |
|--------|-------------|---------------|--------------|
| Built for hostels | ✅ | ❌ | ⚠️ Partial |
| AI meal forecasting | ✅ | ❌ | ❌ |
| Live kitchen prep tracking | ✅ | ❌ | ❌ |
| OTP-based delivery verification | ✅ | ❌ | ❌ |
| Charity meal donation system | ✅ | ❌ | ❌ |
| Slot-based cook shift booking | ✅ | ❌ | ❌ |
| Delivery partner wallet system | ✅ | ✅ | ❌ |
| Hub check-in via OTP | ✅ | ❌ | ❌ |

---

## 🌟 What Makes This a First-of-Its-Kind Platform

### 🤖 AI Forecast Panel
> Predicts meal demand and calculates exact raw materials & vegetables
> needed — reducing food waste and over-preparation.

### 🍱 Charity Meal Donation
> Unsold meals are automatically flagged as **DONATED** and redirected
> to charity — a feature no food delivery platform offers today.

### 🔐 OTP-Based Delivery Verification
> Every delivery is confirmed with a student-generated OTP —
> eliminating fake delivery markings completely.

### 🗺️ Real-Time Map Tracking
> Powered by **Leaflet.js + Geolocation** — delivery partners see
> live bike position and hostel destination on an interactive map.

### 👨‍🍳 Kitchen Display System
> Cooks get a live prep list that only unlocks after hub check-in —
> ensuring accountability and hygiene compliance.

---

## 👥 User Roles

| Role | Dashboard | Key Capability |
|------|-----------|----------------|
| 🎓 Student | `/student/dashboard` | Order meals, track delivery via OTP |
| 👨‍🍳 Kitchen Cook | `/cook/dashboard` | Manage prep list, book shifts |
| 🚴 Delivery Partner | `/delivery/dashboard` | Accept orders, verify OTP, track on map |
| 🛠️ Admin | `/admin/dashboard` | Menu, dispatch, AI forecast, inventory |

---

## 🍳 Kitchen Display System (Cook Flow)

- Cook logs in → views **Dashboard Overview**
- Books **Kitchen Slot** (Breakfast / Lunch / Dinner)
- Views and manages booked shifts
- **Live Prep List locked** until hub check-in via OTP
- Proceeds to prep screen only after verified check-in
- Can update profile and view platform policies

---

## 🚴 Delivery Partner Flow

**Dashboard:** `/delivery/dashboard`

**Navbar:** Wallet Balance (₹) | Offline / Logout toggle

### Off-Duty Mode
- Book delivery slots (Breakfast / Lunch / Dinner)
- Hub Check-In via **4-digit Entry OTP**

### On-Duty Mode
- **Live map** powered by Leaflet.js + browser Geolocation
- Bike icon (you) + Hostel destination rendered on map
- **Current Delivery:** Iterate active jobs → ask student OTP
  → `POST /delivery/verify-otp`
- **Ready for Pickup:** Iterate available orders
  → `POST /delivery/accept/{orderId}`

---

## 🛠️ Admin Panel

**Dashboard:** `/admin/dashboard`

### 📊 System Overview
- Total Registered Students
- Active Kitchen Cooks
- Active Delivery Partners

### 🍽️ Menu Configuration
- Add/Edit meals via dynamic form
- Separate **Pure Veg** and **Non-Veg** menu tables
- JS auto-populates form fields on edit

### 🚚 Dispatch Operations
- **Staff Gate Pass** — list cooks/drivers, check-in status, entry OTP
- **AI Forecast Panel** — predicted orders + raw materials needed

### 📦 Live Dispatch Queue
```
PENDING (Cooking...)
   ↓
PACKED → Handover Form POST → Database: Update Status
   ↓
OUT_FOR_DELIVERY (With Driver)
   ↓
DELIVERED ✅      or      DONATED 🤝 (Charity Meal)
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3.2, Java 17 |
| Frontend | Thymeleaf, HTML/CSS/JS |
| Database | MySQL (Railway) |
| Security | Spring Security 6 |
| Maps | Leaflet.js + Geolocation API |
| Deployment | Render (Docker) |

---

## ⚙️ Environment Variables

| Key | Description |
|-----|-------------|
| `DATABASE_URL` | JDBC MySQL connection string |
| `DATABASE_USERNAME` | DB username |
| `DATABASE_PASSWORD` | DB password |
| `PORT` | Server port (default 8080) |

---

## 🚀 Deployment

- Hosted on **Render** (Dockerized Spring Boot)
- Database on **Railway MySQL**
- Environment variables managed securely via Render dashboard

---

> 💡 *Built to solve a real problem faced by millions of hostel students
> across India — where no structured, tech-driven food delivery solution exists.*
