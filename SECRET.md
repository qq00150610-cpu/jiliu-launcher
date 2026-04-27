# 极流桌面 敏感配置信息

## 阿里云短信配置
```yaml
aliyun_sms:
  access_key_id: "YOUR_ACCESS_KEY_ID"
  access_key_secret: "YOUR_ACCESS_KEY_SECRET"
  sign_name: "极流桌面"
  template_code: "SMS_XXXXXXXXX"
```

## 邮箱SMTP配置
```yaml
smtp:
  host: "smtp.gmail.com"
  port: 587
  username: "your_email@gmail.com"
  password: "your_app_password"
  from_name: "极流桌面"
```

## 使用说明
1. 请将上述占位符替换为实际的配置信息
2. 阿里云短信：请前往阿里云控制台申请短信服务
3. 邮箱：请使用支持SMTP的邮箱，建议使用企业邮箱
4. 请勿将实际配置提交到Git
