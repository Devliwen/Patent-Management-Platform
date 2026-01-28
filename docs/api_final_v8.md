# 高校知识产权运营服务平台接口文档-V1.0（最终合并版）

## 0. 说明

### 0.1 响应体统一格式
响应数据类型：application/json

| 名称 | 类型 | 是否必须 | 默认值 | 备注 |
| ---- | ---- | -------- | ------ | ---- |
| code | number | 必须 |  | 响应码：0-成功，其他-失败 |
| message | string | 非必须 |  | 提示信息 |
| data | object/array/string/null | 非必须 |  | 返回数据 |

### 0.2 认证与授权
用户登录成功后，下发 JWT 令牌；后续请求在 Header 携带 `Authorization: Bearer <token>`。

---

## 1. 认证与用户基础接口

### 1.1 注册
**POST** `/api/auth/register`

**Body**:
```json
{
  "username": "zhangsan",
  "password": "123456",
  "phone": "13800000000",
  "nickname": "张三"
}
```

### 1.2 登录
**POST** `/api/auth/login`

**Body**:
```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

### 1.3 获取当前用户信息
**GET** `/api/users/me`

### 1.4 更新用户资料
**PUT** `/api/users/me/profile`

**Body**:
```json
{
  "nickname": "新昵称",
  "avatarUrl": "http://...",
  "realName": "真实姓名",
  "idNumber": "身份证号"
}
```

---

## 2. 专利相关接口

### 2.1 查询公共数据库专利（按类别）
**GET** `/api/patents`

**参数**:
- `category` (必填): wind/solar/biomass/hydrogen/lilon
- `query` (可选): 搜索关键字

### 2.2 创建/更新公共数据库专利
**POST** `/api/patents`

**Body**:
```json
{
  "publicNum": "CN123456789A",
  "title": "一种用于专利匹配的方法",
  "abstractText": "...",
  "applicant": "..."
}
```

### 2.3 获取单条公共专利
**GET** `/api/patents/{category}/{publicNum}`

### 2.4 个人专利管理 (User Personal Patent)

#### 2.4.1 上传个人专利
**POST** `/api/user-patents`

**Body**:
```json
{
  "category": "solar",
  "title": "高效太阳能板",
  "publicNum": "CN987654",
  "visibility": "PUBLIC"
}
```

#### 2.4.2 查看个人专利列表
**GET** `/api/user-patents`

**参数**:
- `owner`: 传 `me` 查看“我的专利”
- `category`: 筛选类别
- `query`: 搜索关键字
- `visibility`: 筛选可见性 (PUBLIC/PRIVATE)

#### 2.4.3 查看个人专利详情
**GET** `/api/user-patents/{id}`

#### 2.4.4 修改个人专利
**PUT** `/api/user-patents/{id}`

#### 2.4.5 删除个人专利
**DELETE** `/api/user-patents/{id}`

---

## 3. 专家相关接口

### 3.1 查询专家
**GET** `/api/experts`

**参数**:
- `query`: 姓名或领域关键字

### 3.2 申请/更新专家认证
**PUT** `/api/experts/me`

**Body**:
```json
{
  "field": "人工智能",
  "expertise": "知识图谱",
  "achievements": "...",
  "contactInfo": "..."
}
```

### 3.3 审核专家 (管理员)
**POST** `/api/admin/experts/{userId}/audit`

**Body**:
```json
{
  "certStatus": "APPROVED" 
}
```

---

## 4. 需求与匹配相关接口

### 4.1 创建需求
**POST** `/api/requirements`

**Body**:
```json
{
  "title": "设备预测性维护",
  "keywords": "预测性维护,异常检测",
  "techDirection": "时序异常检测",
  "cooperationMode": "技术转让"
}
```

### 4.2 需求匹配专利
**GET** `/api/requirements/{id}/match-patents`

### 4.3 需求匹配专家
**GET** `/api/requirements/{id}/match-experts`

### 4.4 保存专利匹配结果
**POST** `/api/requirements/{id}/match-patents/persist`

**Body**:
```json
{
  "items": [
    {
      "patentCategory": "wind",
      "patentPublicNum": "CN123456789A",
      "matchScore": 0.912,
      "matchReason": "关键词命中"
    }
  ]
}
```

### 4.5 保存专家匹配结果
**POST** `/api/requirements/{id}/match-experts/persist`

**Body**:
```json
{
  "items": [
    {
      "expertId": 1,
      "matchScore": 0.855,
      "matchReason": "expertise 匹配"
    }
  ]
}
```

---

## 5. 转化成果相关接口

### 5.1 查询全部成果
**GET** `/api/transformations`

### 5.2 创建成果记录
**POST** `/api/transformations`

**Body**:
```json
{
  "patentCategory": "wind",
  "patentPublicNum": "CN123456789A",
  "expertId": 1,
  "status": "Success",
  "benefitAmount": 1000000
}
```

---

## 6. 专利价值评估

### 6.1 生成评估报告
**POST** `/api/valuations`

**Body**:
```json
{
  "patentSource": "EXTERNAL", 
  "patentCategory": "wind",
  "patentPublicNum": "CN123456789A",
  "modelVersion": "v1"
}
```
或
```json
{
  "patentSource": "USER",
  "userPatentId": 123,
  "modelVersion": "v1"
}
```

### 6.2 查询评估报告列表
**GET** `/api/valuations`

**参数**:
- `patentSource`: EXTERNAL / USER
- `patentCategory` & `patentPublicNum` (当 source=EXTERNAL)
- `userPatentId` (当 source=USER)

### 6.3 维护评估模型参数
**PUT** `/api/valuation-params/{key}`

**Body**:
```json
{
  "paramValue": "0.5",
  "description": "权重参数"
}
```
