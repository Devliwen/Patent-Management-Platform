# 专利管理平台 API 文档

本文档描述了专利管理平台的后端接口，主要分为“专利查询（公共数据库）”和“个人专利管理”两部分。

## 1. 专利查询接口 (Patent Query)
**Base URL**: `/api/patents`

该模块用于查询平台公共数据库中的专利数据（包括 Wind, Solar, Biomass, Hydrogen, Lilon 五大类）。

### 1.1 查询专利列表
**GET** `/api/patents`

**参数**:
| 参数名 | 类型 | 必填 | 描述 |
|---|---|---|---|
| `category` | string | 是 | 专利类别，可选值: `wind`, `solar`, `biomass`, `hydrogen`, `lilon` |
| `query` | string | 否 | 搜索关键字（标题、摘要等） |
| `page` | int | 否 | 页码，默认 0 |
| `size` | int | 否 | 每页数量，默认 10 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "publicNum": "CN123456",
        "title": "一种风力发电机",
        ...
      }
    ],
    "totalPages": 5,
    "totalElements": 50
  }
}
```

### 1.2 获取专利详情
**GET** `/api/patents/{category}/{publicNum}`

**参数**:
| 参数名 | 类型 | 必填 | 描述 |
|---|---|---|---|
| `category` | string | 是 | 专利类别 |
| `publicNum` | string | 是 | 专利公开号 |

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "publicNum": "CN123456",
    "title": "一种风力发电机",
    "abstractText": "...",
    ...
  }
}
```

---

## 2. 个人专利管理接口 (User Personal Patent Management)
**Base URL**: `/api/user-patents`

该模块用于用户管理自己上传的专利数据。需要登录（携带 Token）。

### 2.1 上传个人专利
**POST** `/api/user-patents`

**Body (JSON)**:
```json
{
  "category": "solar",          // 必填，类别
  "title": "高效太阳能板",        // 必填，标题
  "publicNum": "CN987654",      // 可选，公开号
  "abstractText": "摘要内容...",  // 可选
  "ipc": "H01L",                // 可选，IPC分类
  "cpc": "Y02E",                // 可选，CPC分类
  "applicant": "张三",           // 可选，申请人
  "inventor": "李四",            // 可选，发明人
  "visibility": "PUBLIC"        // 可选，可见性 (PUBLIC/PRIVATE)
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "title": "高效太阳能板",
    "ownerUserId": 1001,
    "createdAt": "2023-10-01T12:00:00"
  }
}
```

### 2.2 查看专利列表
**GET** `/api/user-patents`

**参数**:
| 参数名 | 类型 | 必填 | 描述 |
|---|---|---|---|
| `owner` | string | 否 | 传 `me` 查看“我的专利”；不传查看所有公开的个人专利 |
| `category` | string | 否 | 按类别筛选 |
| `query` | string | 否 | 搜索关键字 |
| `visibility`| string | 否 | 筛选可见性 (如 `PUBLIC`, `PRIVATE`) |

**场景说明**:
1. **查看我上传的专利**: `GET /api/user-patents?owner=me`
2. **查看所有公开的用户专利**: `GET /api/user-patents`

### 2.3 查看专利详情
**GET** `/api/user-patents/{id}`

**参数**:
| 参数名 | 类型 | 必填 | 描述 |
|---|---|---|---|
| `id` | long | 是 | 专利记录 ID |

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "title": "高效太阳能板",
    ...
  }
}
```

### 2.4 修改个人专利
**PUT** `/api/user-patents/{id}`

**Body (JSON)**:
(同上传接口，仅包含需要修改的字段)

### 2.5 删除个人专利
**DELETE** `/api/user-patents/{id}`

**描述**: 仅能删除自己上传的专利。

**响应**:
```json
{
  "code": 200,
  "message": "success"
}
```
