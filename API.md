# 🔌 API Integration

## 🤖 OpenAI API

* Auto generate tags
* Suggest code improvements

### Example:

POST /openai
{
"prompt": "Fix this code..."
}

---

## 💳 VNPAY Sandbox

### Payment Flow:

1. User chọn Premium
2. Gửi request tới VNPAY
3. Redirect thanh toán
4. Nhận callback

---

## 🖼 ImgBB API (Upload ảnh)

POST https://api.imgbb.com/1/upload

Params:

* key: API_KEY
* image: base64

Response:

* image_url
