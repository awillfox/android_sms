# ENDPOINT.md

HTTP endpoints exposed by the SMS gateway. Default port `5000` (or `PORT` env var; otherwise probes upward from 5000 if 5000 is taken).

## Response envelope

All `/sms/*` responses use this JSON shape (defined in `pkg/response/http.go`):

```json
{
  "data": <any | null>,
  "error": <string | null>
}
```

`error` is the stringified Go error (or `null` on success). HTTP status is always `200 OK` even on validation/DB errors — clients must inspect the `error` field.

---

## `GET /ping`

Heartbeat from chi middleware.

**Response:** `200 OK`, body: `.` (plain text, not JSON envelope).

---

## `GET /sms/playground`

Banner / liveness for the SMS subsystem.

**Response:** `200 OK`

```json
{
  "data": "Welcome to my playground. Here, every tale is a sandbox of fate and fear, where your choices carve out the paths and possibilities.",
  "error": null
}
```

---

## `POST /sms/messages`

Persist a received SMS.

**Request body** (`application/json`) — `service.MessageParams`:

| Field      | Type       | Required | Notes                                                                 |
|------------|------------|----------|-----------------------------------------------------------------------|
| `from`     | string     | yes      | Sender phone number / identifier. Stored as `messages.sender`.        |
| `content`  | string     | yes      | Message body. Empty string rejected with `error: "content is empty"`. |
| `simSlot`  | integer    | yes      | 0-indexed slot from client. `-1` = unset. Stored as `slot+1`.         |
| `to`       | string     | no       | Accepted, **not persisted**.                                          |
| `tos`      | []string   | no       | Accepted, **not persisted**.                                          |
| `toName`   | string     | no       | Accepted, **not persisted**.                                          |
| `toNames`  | []string   | no       | Accepted, **not persisted**.                                          |
| `dir`      | string     | no       | Accepted, **not persisted**.                                          |
| `date`     | RFC3339    | no       | Accepted, **not persisted** (DB uses `CURRENT_TIMESTAMP`).            |

SIM slot mapping (after `+1` offset, see `internal/message/sim_slot.go`):

| Client `simSlot` | Stored `sim_slot` | Name    |
|------------------|-------------------|---------|
| `-1`             | `1`               | `SIM1`  |
| `0`              | `1`               | `SIM1`  |
| `1`              | `2`               | `SIM2`  |
| any other        | `n+1`             | `Unknown` if outside `0..2` |

> Note: client `-1` is coerced to `0` before the `+1`, so it lands on `SIM1`, not `Unset`. `Unset` (stored value `0`) is unreachable via this endpoint.

**Example request:**

```bash
curl -X POST http://localhost:5000/sms/messages \
  -H 'Content-Type: application/json' \
  -d '{
    "from": "+66123456789",
    "content": "hello world",
    "simSlot": 0
  }'
```

**Success response:** `200 OK`

```json
{
  "data": {
    "id": 42,
    "sender": "+66123456789",
    "content": "hello world",
    "sim_slot": 1,
    "sim_slot_name": "SIM1",
    "created_at": "2026-05-05T10:30:00Z"
  },
  "error": null
}
```

**Error responses:** `200 OK` with envelope error set.

- Empty content: `{"data": null, "error": "content is empty"}`
- Malformed JSON: `{"data": null, "error": "<json decode error>"}`
- DB failure: `{"data": null, "error": "<pgx error>"}`

---

## `GET /sms/messages`

List all stored messages. No pagination, no filtering.

**Request:** no parameters.

**Example:**

```bash
curl http://localhost:5000/sms/messages
```

**Success response:** `200 OK`

```json
{
  "data": [
    {
      "id": 42,
      "sender": "+66123456789",
      "content": "hello world",
      "sim_slot": 1,
      "sim_slot_name": "SIM1",
      "created_at": "2026-05-05T10:30:00Z"
    }
  ],
  "error": null
}
```

Empty result returns `"data": []` (sqlc `emit_empty_slices: true`).

**Error response:** `{"data": null, "error": "<pgx error>"}` on DB failure.

---

---

## `POST /notifications`

Persist an Android notification.

**Request body** (`application/json`) — `service.NotificationParams`:

| Field          | Type     | Required | Notes                                                    |
|----------------|----------|----------|----------------------------------------------------------|
| `package_name` | string   | yes      | App package, e.g. `com.whatsapp`. Empty rejected.        |
| `app_name`     | string   | no       | Display name.                                            |
| `title`        | string   | cond.    | At least one of `title` / `content` must be non-empty.   |
| `content`      | string   | cond.    | At least one of `title` / `content` must be non-empty.   |
| `posted_at`    | RFC3339  | no       | When the phone posted the notification. If zero/missing, server uses `time.Now()`. |

**Example:**

```bash
curl -X POST http://localhost:5000/notifications \
  -H 'Content-Type: application/json' \
  -d '{
    "package_name": "com.whatsapp",
    "app_name": "WhatsApp",
    "title": "Mom",
    "content": "dinner ready",
    "posted_at": "2026-05-05T10:30:00Z"
  }'
```

**Success response:** `200 OK`

```json
{
  "data": {
    "id": 1,
    "package_name": "com.whatsapp",
    "app_name": "WhatsApp",
    "title": "Mom",
    "content": "dinner ready",
    "posted_at": "2026-05-05T10:30:00Z",
    "created_at": "2026-05-05T10:30:01.123Z"
  },
  "error": null
}
```

**Error responses:** envelope error set.
- Empty package: `{"error": "package_name is empty"}`
- Both title and content empty: `{"error": "title and content are both empty"}`
- Malformed JSON / DB error: `{"error": "<error string>"}`

---

## `GET /notifications`

List all notifications, ordered `id desc`.

**Example:**

```bash
curl http://localhost:5000/notifications
```

**Response:** `200 OK`

```json
{
  "data": [ { "id": 2, "...": "..." }, { "id": 1, "...": "..." } ],
  "error": null
}
```

Empty result returns `"data": []`.

---

## `GET /notifications/{id}`

Get one notification by ID.

**Path params:** `id` (int64).

**Example:**

```bash
curl http://localhost:5000/notifications/42
```

**Success response:** same `Notification` shape as `POST /notifications` data.

**Error responses:**
- Non-numeric id: `{"error": "<strconv parse error>"}`
- Not found: `{"error": "no rows in result set"}` (pgx.ErrNoRows surfaces as the error string)

---

## `DELETE /notifications/{id}`

Delete one notification.

**Path params:** `id` (int64).

**Example:**

```bash
curl -X DELETE http://localhost:5000/notifications/42
```

**Success response:** `200 OK`

```json
{
  "data": { "deleted": 1, "id": 42 },
  "error": null
}
```

**Error responses:**
- Non-numeric id: `{"error": "<strconv parse error>"}`
- ID does not exist (0 rows affected): `{"error": "notification not found"}`

---

---

## `POST /phonebook`

Create a contact.

**Request body** — `service.ContactParams`:

| Field          | Type   | Required | Notes                                              |
|----------------|--------|----------|----------------------------------------------------|
| `phone_number` | string | yes      | Trimmed server-side. Empty rejected.               |
| `note`         | string | no       | Free text. Defaults to empty string.               |

**Example:**

```bash
curl -X POST http://localhost:5000/phonebook \
  -H 'Content-Type: application/json' \
  -d '{"phone_number": "+66123456789", "note": "Mom"}'
```

**Success response:** `200 OK`

```json
{
  "data": {
    "id": 1,
    "phone_number": "+66123456789",
    "note": "Mom",
    "created_at": "2026-05-05T10:30:00Z",
    "updated_at": "2026-05-05T10:30:00Z"
  },
  "error": null
}
```

---

## `GET /phonebook`

List all contacts, ordered `id desc`. Returns `[]` when empty.

---

## `GET /phonebook/{id}`

Get one contact. Errors with `"no rows in result set"` if absent.

---

## `PUT /phonebook/{id}`

Update phone number and/or note. Server bumps `updated_at = NOW()`.

**Body:** same shape as `POST /phonebook`. `phone_number` required.

```bash
curl -X PUT http://localhost:5000/phonebook/1 \
  -H 'Content-Type: application/json' \
  -d '{"phone_number": "+66123456789", "note": "Mom (work)"}'
```

Returns updated contact.

---

## `DELETE /phonebook/{id}`

Delete one contact.

```bash
curl -X DELETE http://localhost:5000/phonebook/1
```

Success: `{"data": {"deleted": 1, "id": 1}, "error": null}`. Missing id: `{"error": "contact not found"}`.

---

## UI pages

Static HTML pages embedded via `//go:embed` in `web/`. Tailwind via CDN. Pages poll their JSON endpoints every 5s; toggle off via the "Auto-refresh" checkbox.

| Path                  | Renders                                                    |
|-----------------------|------------------------------------------------------------|
| `GET /`               | 302 redirect to `/ui/sms`                                  |
| `GET /ui`             | 302 redirect to `/ui/sms`                                  |
| `GET /ui/sms`         | Table of SMS, polls `GET /sms/messages` every 5s           |
| `GET /ui/notifications` | Table of notifications + per-row Delete button (calls `DELETE /notifications/{id}`), polls `GET /notifications` every 5s |
| `GET /ui/phonebook`   | Add-contact form + table with inline Edit/Delete (calls `POST/PUT/DELETE /phonebook`), polls `GET /phonebook` every 5s |

Response: `200 OK`, `Content-Type: text/html; charset=utf-8`.

---

## Domain types

`Message` (returned in `data` for save/list):

```go
type Message struct {
    ID          int64     `json:"id"`
    Sender      string    `json:"sender"`
    Content     string    `json:"content"`
    SIMSlot     SIMSlot   `json:"sim_slot"`       // int: 0=Unset, 1=SIM1, 2=SIM2
    SIMSlotName string    `json:"sim_slot_name"`  // "Unset" | "SIM1" | "SIM2" | "Unknown"
    CreatedAt   time.Time `json:"created_at"`
}
```

`Notification`:

```go
type Notification struct {
    ID          int64     `json:"id"`
    PackageName string    `json:"package_name"`
    AppName     string    `json:"app_name"`
    Title       string    `json:"title"`
    Content     string    `json:"content"`
    PostedAt    time.Time `json:"posted_at"`
    CreatedAt   time.Time `json:"created_at"`
}
```

`Contact`:

```go
type Contact struct {
    ID          int64     `json:"id"`
    PhoneNumber string    `json:"phone_number"`
    Note        string    `json:"note"`
    CreatedAt   time.Time `json:"created_at"`
    UpdatedAt   time.Time `json:"updated_at"`
}
```
