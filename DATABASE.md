# 🗄 Database Design (Firestore)

## 📁 Collections

### users

* id
* username
* email
* avatar
* bio
* followers_count
* following_count
* is_premium

### posts

* id
* user_id
* content
* image_url
* created_at
* like_count
* comment_count

### comments

* id
* post_id
* user_id
* content
* created_at

### likes

* id
* user_id
* post_id

### follows

* id
* follower_id
* following_id

### messages

* id
* sender_id
* receiver_id
* content
* timestamp

### saved_posts

* id
* user_id
* post_id

### payments

* id
* user_id
* amount
* status
* created_at

### notifications

* id
* user_id
* content
* is_read
