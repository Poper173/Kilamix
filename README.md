Consuming this API endpoints continue naking an app 



# iTechTube API Documentation
php artisan serve --host=0.0.0.0 --port=8000

## Base URL
```
http://127.0.0.1:8000/api
```

## Authentication
- All protected routes require Bearer token in Authorization header
- Token format: `Authorization: Bearer <token>`
- Get token via `/login` endpoint

---

## Table of Contents
1. [Authentication](#authentication)
2. [User](#user)
3. [Categories](#categories)
4. [Videos](#videos)
5. [Playlists](#playlists)
6. [Watch History](#watch-history)
7. [Subscriptions](#subscriptions)
8. [Comments](#comments)
9. [Channels](#channels)
10. [Live Streaming](#live-streaming)
11. [Admin](#admin)

---

## Authentication

### Register User
**POST** `/register`

Request body:
```json
{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "password123",
    "password_confirmation": "password123",
    "role": "user"
}
```

Response (201):
```json
{
    "success": true,
    "message": "User registered successfully",
    "data": {
        "user": {
            "id": 1,
            "name": "John Doe",
            "email": "john@example.com",
            "role": "user"
        }
    }
}
```

### Login
**POST** `/login`

Request body:
```json
{
    "email": "john@example.com",
    "password": "password123"
}
```

Response (200):
```json
{
    "success": true,
    "data": {
        "token": "1|abc123...",
        "user": {
            "id": 1,
            "name": "John Doe",
            "email": "john@example.com",
            "role": "creator"
        }
    }
}
```

### Logout
**POST** `/logout`
- **Auth Required**: Yes

Response (200):
```json
{
    "success": true,
    "message": "Logged out successfully"
}
```

### Get Current User
**GET** `/me`
- **Auth Required**: Yes

Response (200):
```json
{
    "success": true,
    "data": {
        "id": 1,
        "name": "John Doe",
        "email": "john@example.com",
        "role": "creator"
    }
}
```

### Get Liked Videos
**GET** `/user/liked-videos`
- **Auth Required**: Yes

Query params:
- `page` (optional): Page number

Response (200):
```json
{
    "success": true,
    "data": [...],
    "meta": {
        "current_page": 1,
        "last_page": 3,
        "total": 25
    }
}
```

---

## User

### Get User Profile
**GET** `/user/profile`
- **Auth Required**: Yes

Response (200):
```json
{
    "success": true,
    "data": {
        "id": 1,
        "name": "John Doe",
        "email": "john@example.com",
        "avatar": "http://127.0.0.1:8000/storage/avatars/avatar.jpg",
        "created_at": "2026-01-01T10:00:00Z"
    }
}
```

### Update Profile
**PUT** `/user/profile`
- **Auth Required**: Yes

Request body (multipart/form-data):
```json
{
    "name": "John Doe",
    "avatar": [file]
}
```

### Change Password
**PUT** `/user/password`
- **Auth Required**: Yes

Request body:
```json
{
    "current_password": "oldpassword123",
    "password": "newpassword123",
    "password_confirmation": "newpassword123"
}
```

---

## Categories

### List Categories
**GET** `/categories`

Response (200):
```json
{
    "success": true,
    "data": [
        {
            "id": 1,
            "name": "Technology",
            "description": "Tech videos",
            "slug": "technology",
            "video_count": 15
        }
    ]
}
```

### Get Category
**GET** `/categories/{id}`

---

## Videos

### List Videos (Home Page)
**GET** `/videos`

Query params:
- `page` (optional): Page number
- `category_id` (optional): Filter by category
- `sort` (optional): Sort by (newest, popular, most_liked)

Response (200):
```json
{
    "success": true,
    "data": [
        {
            "id": 1,
            "title": "My Video",
            "description": "Video description",
            "thumbnail_url": "http://127.0.0.1:8000/storage/thumbnails/thumb.jpg",
            "video_url": "http://127.0.0.1:8000/api/videos/1/stream",
            "views_count": 100,
            "likes_count": 10,
            "duration": 3600,
            "user": {
                "id": 1,
                "name": "Creator Name",
                "avatar": "http://127.0.0.1:8000/storage/avatars/avatar.jpg"
            },
            "category": {
                "id": 1,
                "name": "Technology"
            },
            "created_at": "2026-01-01T10:00:00Z"
        }
    ],
    "links": {...},
    "meta": {...}
}
```

### Get Single Video
**GET** `/videos/{id}`

Response (200):
```json
{
    "success": true,
    "data": {
        "id": 1,
        "title": "My Video",
        "description": "Video description",
        "video_url": "http://127.0.0.1:8000/api/videos/1/stream",
        "thumbnail_url": "http://127.0.0.1:8000/storage/thumbnails/thumb.jpg",
        "views_count": 100,
        "likes_count": 10,
        "is_liked": false,
        "is_authenticated": true,
        "user": {...},
        "category": {...},
        "created_at": "2026-01-01T10:00:00Z"
    }
}
```

### Stream Video
**GET** `/videos/{id}/stream`
- Returns video file stream

### Record Watch Progress
**POST** `/videos/{id}/watch`
- **Auth Required**: Yes

Request body:
```json
{
    "progress": 120,
    "completed": false
}
```

### Search Videos
**GET** `/videos/search?q=search_term&page=1`

### Upload Video
**POST** `/videos`
- **Auth Required**: Yes
- Content-Type: multipart/form-data

Request body:
```json
{
    "title": "My Video",
    "description": "Video description",
    "category_id": 1,
    "video": [file],
    "thumbnail": [file] (optional)
}
```

### Get My Videos
**GET** `/my-videos`
- **Auth Required**: Yes

### Update Video
**PUT** `/videos/{id}`
- **Auth Required**: Yes (Owner)

Request body:
```json
{
    "title": "Updated Title",
    "description": "Updated description",
    "category_id": 1,
    "visibility": "public"
}
```

### Delete Video
**DELETE** `/videos/{id}`
- **Auth Required**: Yes (Owner)

---

## Video Likes

### Toggle Like
**POST** `/videos/{id}/like`
- **Auth Required**: Yes

Response:
```json
{
    "success": true,
    "data": {
        "liked": true,
        "likes_count": 11
    }
}
```

### Get Like Status
**GET** `/videos/{id}/like/status`

Response:
```json
{
    "success": true,
    "data": {
        "is_liked": true,
        "likes_count": 10,
        "is_authenticated": true
    }
}
```

---

## Playlists

### List My Playlists
**GET** `/playlists`
- **Auth Required**: Yes

### Create Playlist
**POST** `/playlists`
- **Auth Required**: Yes

Request body:
```json
{
    "name": "My Playlist",
    "description": "Playlist description",
    "is_public": true
}
```

### Get Playlist
**GET** `/playlists/{id}`

### Add Video to Playlist
**POST** `/playlists/{id}/videos`
- **Auth Required**: Yes (Owner)

Request body:
```json
{
    "video_id": 1
}
```

### Delete Playlist
**DELETE** `/playlists/{id}`
- **Auth Required**: Yes (Owner)

---

## Watch History

### Get Watch History
**GET** `/history`
- **Auth Required**: Yes

Response:
```json
{
    "success": true,
    "data": [
        {
            "id": 1,
            "video": {...},
            "progress": 120,
            "completed": false,
            "watched_at": "2026-01-01T10:00:00Z"
        }
    ]
}
```

### Record Watch History
**POST** `/history`
- **Auth Required**: Yes

Request body:
```json
{
    "video_id": 1,
    "progress": 120,
    "completed": false
}
```

### Get Continue Watching
**GET** `/history/continue-watching`
- **Auth Required**: Yes

---

## Subscriptions

### Toggle Subscription
**POST** `/channels/{channelId}/subscribe`
- **Auth Required**: Yes

Response:
```json
{
    "success": true,
    "data": {
        "subscribed": true,
        "subscribers_count": 100
    }
}
```

### Get Subscription Status
**GET** `/channels/{channelId}/subscription`
- **Auth Required**: Yes

### Get My Subscriptions
**GET** `/subscriptions`
- **Auth Required**: Yes

### Get Channel Subscribers
**GET** `/channels/{channelId}/subscribers`
- **Auth Required**: Yes

---

## Channels

### Get Public Channel Profile
**GET** `/channels/{channelId}`

Response:
```json
{
    "success": true,
    "data": {
        "id": 1,
        "name": "Channel Name",
        "channel_description": "Channel description",
        "avatar": "http://127.0.0.1:8000/storage/avatars/avatar.jpg",
        "channel_banner": "http://127.0.0.1:8000/storage/banners/banner.jpg",
        "is_live": false,
        "current_stream_title": null,
        "current_viewers": 0,
        "total_views": 10000,
        "total_subscribers": 500,
        "videos_count": 25,
        "videos": [...],
        "created_at": "2026-01-01T10:00:00Z"
    }
}
```

---

## Live Streaming

### Get All Live Streams
**GET** `/live`

Response:
```json
{
    "success": true,
    "data": [
        {
            "id": 1,
            "channel_name": "Creator Channel",
            "stream_title": "Live Stream Title",
            "stream_viewers": 50,
            "stream_started_at": "2026-01-01T12:00:00Z",
            "avatar": "http://127.0.0.1:8000/storage/avatars/avatar.jpg",
            "total_subscribers": 500
        }
    ],
    "count": 3
}
```

### Get Live Stream
**GET** `/live/{channelId}`

### Get Stream Chat Messages
**GET** `/live/{channelId}/chat`

Query params:
- `limit` (optional): Max 100, default 50
- `since_id` (optional): Get messages after this ID

Response:
```json
{
    "success": true,
    "data": [
        {
            "id": 1,
            "channel_id": 1,
            "user_id": 2,
            "user_name": "Viewer Name",
            "user_avatar": "http://127.0.0.1:8000/storage/avatars/viewer.jpg",
            "message": "Hello everyone!",
            "created_at": "2026-01-01T12:05:00Z"
        }
    ],
    "count": 1
}
```

### Send Chat Message
**POST** `/live/{channelId}/chat`
- **Auth Required**: Yes

Request body:
```json
{
    "message": "Hello everyone!"
}
```

### Join Stream
**POST** `/live/{channelId}/join`

Response:
```json
{
    "success": true,
    "data": {
        "stream_viewers": 51,
        "message": "Joined stream successfully"
    }
}
```

### Leave Stream
**POST** `/live/{channelId}/leave`

### Get Viewer Count
**GET** `/live/{channelId}/viewers`

---

## Creator/Channel Management

### Get Creator Channel Profile
**GET** `/creator/channel`
- **Auth Required**: Yes (Creator)

### Update Channel Profile
**PUT** `/creator/channel`
- **Auth Required**: Yes (Creator)

### Get Stream Key
**GET** `/creator/stream/key`
- **Auth Required**: Yes (Creator)

### Regenerate Stream Key
**POST** `/creator/stream/regenerate-key`
- **Auth Required**: Yes (Creator)

### Start Stream
**POST** `/creator/stream/start`
- **Auth Required**: Yes (Creator)

Request body:
```json
{
    "title": "My Live Stream"
}
```

Response:
```json
{
    "success": true,
    "message": "Stream started successfully",
    "data": {
        "stream_status": "live",
        "stream_title": "My Live Stream",
        "stream_url": "rtmp://localhost/live",
        "stream_key": "sk_1_abc123..."
    }
}
```

### Stop Stream
**POST** `/creator/stream/stop`
- **Auth Required**: Yes (Creator)

### Get Stream Status
**GET** `/creator/stream/status`
- **Auth Required**: Yes (Creator)

### Creator Stream Monitor
**GET** `/creator/stream/monitor`
- **Auth Required**: Yes (Creator)

Response:
```json
{
    "success": true,
    "data": {
        "stream_status": "live",
        "stream_title": "My Live Stream",
        "stream_viewers": 50,
        "stream_started_at": "2026-01-01T12:00:00Z",
        "stream_duration": 3600,
        "stream_duration_formatted": "01:00:00",
        "recent_chat": [...]
    }
}
```

### Get Channel Stats
**GET** `/creator/stats`
- **Auth Required**: Yes (Creator)

### Get Channel Analytics
**GET** `/creator/analytics`
- **Auth Required**: Yes (Creator)

---

## Admin

### Get Dashboard Stats
**GET** `/admin/stats`
- **Auth Required**: Yes (Admin)

### Get Users
**GET** `/admin/users`
- **Auth Required**: Yes (Admin)

### Toggle User Status
**POST** `/admin/users/{id}/toggle-status`
- **Auth Required**: Yes (Admin)

### Update User Role
**POST** `/admin/users/{id}/role`
- **Auth Required**: Yes (Admin)

### Get Admin Videos
**GET** `/admin/videos`
- **Auth Required**: Yes (Admin)

### Approve/Reject Video
**POST** `/admin/videos/{id}/approve`
**POST** `/admin/videos/{id}/reject`
- **Auth Required**: Yes (Admin)

---

## Error Responses

```json
{
    "success": false,
    "message": "Error description",
    "errors": {
        "field": ["Validation error"]
    }
}
```

### HTTP Status Codes
- 200 - Success
- 201 - Created
- 400 - Bad Request
- 401 - Unauthorized
- 403 - Forbidden
- 404 - Not Found
- 422 - Validation Error
- 429 - Too Many Requests
- 500 - Server Error

---


*Last Updated: 2026-02-10*
*Backend: Laravel 11 with Sanctum Authentication*
