# 高校知识产权运营服务平台接口文档-V1.0（项目书全量规划版）

## 0. 说明

> ## 1. 认证与用户基础接口（规划）
>
> ### 1.1 注册
>
> #### 1.1.1 基本信息
>
> > 请求路径：/api/auth/register
> >
> > 请求方式：POST
> >
> > 接口描述：注册新用户（写入 user_account），可同时写入 user_profile。
>
> #### 1.1.2 请求参数
>
> 请求参数格式：application/json
>
> 请求参数说明：
>
> | 参数名称 | 说明   | 类型   | 是否必须 | 备注                     |
> | -------- | ------ | ------ | -------- | ------------------------ |
> | username | 用户名 | string | 是       | 唯一                     |
> | password | 密码   | string | 是       | 服务端保存 password_hash |
> | phone    | 手机   | string | 否       |                          |
> | email    | 邮箱   | string | 否       |                          |
> | nickname | 昵称   | string | 否       | user_profile             |
>
> 请求数据样例：
>
> ```json
> {
>   "username": "zhangsan",
>   "password": "123456",
>   "phone": "13800000000",
>   "nickname": "张三"
> }
> ```
>
> #### 1.1.3 响应数据
>
> 响应数据样例：
>
> ```json
> {
>   "code": 0,
>   "message": "操作成功",
>   "data": {
>     "userId": 1
>   }
> }
> ```
>
> ### 1.2 登录
>
> #### 1.2.1 基本信息
>
> > 请求路径：/api/auth/login
> >
> > 请求方式：POST
> >
> > 接口描述：登录成功后返回 JWT token。
>
> #### 1.2.2 请求参数
>
> 请求参数格式：application/json
>
> 请求参数说明：
>
> | 参数名称 | 说明   | 类型   | 是否必须 | 备注 |
> | -------- | ------ | ------ | -------- | ---- |
> | username | 用户名 | string | 是       |      |
> | password | 密码   | string | 是       |      |
>
> 请求数据样例：
>
> ```json
> {
>   "username": "zhangsan",
>   "password": "123456"
> }
> ```
>
> #### 1.2.3 响应数据
>
> 响应数据样例：
>
> ```json
> {
>   "code": 0,
>   "message": "操作成功",
>   "data": "jwt-token-string"
> }
> ```
>
> ### 1.3 获取当前用户信息
>
> > 请求路径：/api/users/me
> >
> > 请求方式：GET
> >
> > 接口描述：返回 user_account + user_profile + expert_profile(如有) + 主机构(如有)。
>
> ## 2. 用户资料接口（规划）
>
> ### 2.1 更新用户资料
>
> #### 2.1.1 基本信息
>
> > 请求路径：/api/users/me/profile
> >
> > 请求方式：PUT
> >
> > 接口描述：更新 user_profile（昵称、头像、实名信息等）。
>
> #### 2.1.2 请求参数
>
> 请求参数格式：application/json
>
> | 参数名称  | 说明    | 类型   | 是否必须 | 备注 |
> | --------- | ------- | ------ | -------- | ---- |
> | nickname  | 昵称    | string | 否       |      |
> | avatarUrl | 头像URL | string | 否       |      |
> | realName  | 实名    | string | 否       |      |
> | idNumber  | 证件号  | string | 否       |      |

### 0.2 响应体统一格式（推荐）

响应数据类型：application/json

响应参数说明：

| 名称 | 类型 | 是否必须 | 默认值 | 备注 |
| ---- | ---- | -------- | ------ | ---- |
| code | number | 必须 |  | 响应码：0-成功，1-失败 |
| message | string | 非必须 |  | 提示信息 |
| data | object/array/string/null | 非必须 |  | 返回数据 |

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": null
}
```

> 备注：当前后端已有部分接口返回的是实体/数组本身（未包裹 code/message/data）。本文档以“产品化接口规范”优先，便于前后端联调与后续扩展。

### 0.3 认证与授权（JWT，规划）

> 说明：用户登录成功后，下发 JWT 令牌；后续请求在 Header 携带 `Authorization: Bearer <token>`。
>
> 未登录访问需认证接口时，HTTP 状态码为 401。
>
> 权限控制依据 RBAC（role/permission/user_role/role_permission）。

### 0.4 运行配置（Profiles）

> 说明：项目使用 Spring Profiles 管理数据库配置，默认 profile 为 `local`（H2 内存库），MySQL 连接使用 `mysql` profile。

local 启动示例：

```bash
./gradlew bootRun
```

mysql 启动示例：

```bash
./gradlew bootRun --args="--spring.profiles.active=mysql"
```

## 2. 专利相关接口（多表）

### 2.1 查询专利（按类别）

#### 2.1.1 基本信息

> 请求路径：/api/patents
>
> 请求方式：GET
>
> 接口描述：按 category 指定分表查询专利；query 为空返回该分表全部专利，query 不为空按标题/摘要/申请人/发明人模糊查询。

#### 2.1.2 请求参数

请求参数格式：queryString

请求参数说明：

| 参数名称 | 说明 | 类型 | 是否必须 | 备注 |
| -------- | ---- | ---- | -------- | ---- |
| category | 专利类别 | string | 是 | wind/solar/biomass/hydrogen/lilon |
| query | 查询关键字 | string | 否 | 为空返回全表 |

请求数据样例：

```shell
GET /api/patents?category=wind&query=人工智能
GET /api/patents?category=wind
```

#### 2.1.3 响应数据

响应参数说明：`data` 为 PatentBase 数组（字段与专利分表一致）

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": [
    {
      "publicNum": "CN123456789A",
      "title": "一种用于……的方法",
      "abstractText": "本发明公开了……",
      "applicant": "某某科技有限公司",
      "inventor": "张三",
      "ipc": "G06F 16/00"
    }
  ]
}
```

### 2.2 创建/更新专利（按类别）

#### 2.2.1 基本信息

> 请求路径：/api/patents
>
> 请求方式：POST
>
> 接口描述：向 category 对应分表写入专利数据；publicNum 为主键，重复会覆盖更新。

#### 2.2.2 请求参数

请求参数格式：queryString + application/json

请求参数说明（QueryString）：

| 参数名称 | 说明 | 类型 | 是否必须 | 备注 |
| -------- | ---- | ---- | -------- | ---- |
| category | 专利类别 | string | 是 | wind/solar/biomass/hydrogen/lilon |

请求参数说明（Body）：

| 参数名称 | 说明 | 类型 | 是否必须 | 备注 |
| -------- | ---- | ---- | -------- | ---- |
| publicNum | 公开号 | string | 是 | 主键 public_num |
| title | 标题 | string | 否 |  |
| abstractText | 摘要 | string | 否 | 映射到表字段 abstract |
| applicant | 申请人 | string | 否 |  |
| inventor | 发明人 | string | 否 |  |
| appliNum | 申请号 | string | 否 |  |
| appliDate | 申请日 | string | 否 |  |
| publicDate | 公布日 | string | 否 |  |
| ipc | IPC | string | 否 | 映射到表字段 IPC |
| cpc | CPC | string | 否 | 映射到表字段 CPC |
| nec | NEC | string | 否 | 映射到表字段 NEC |
| legalStatus | 法律状态 | string | 否 |  |
| latestLegalStatus | 最新法律状态 | string | 否 |  |
| status | 状态 | string | 否 |  |
| type | 类型 | string | 否 |  |
| applicantAddress | 申请人地址 | string | 否 |  |
| patentee | 专利权人 | string | 否 |  |
| patenteeAddress | 专利权人地址 | string | 否 |  |
| agent | 代理机构/代理人 | string | 否 |  |
| patentDetails | 详情/全文 | string | 否 |  |

请求数据样例：

```shell
POST /api/patents?category=wind
```

```json
{
  "publicNum": "CN123456789A",
  "title": "一种用于专利匹配的方法",
  "abstractText": "本发明公开了……",
  "ipc": "G06F 16/00",
  "inventor": "张三",
  "applicant": "某某科技有限公司"
}
```

#### 2.2.3 响应数据

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "publicNum": "CN123456789A",
    "title": "一种用于专利匹配的方法"
  }
}
```

### 2.3 获取单条专利（按类别 + 公开号）

#### 2.3.1 基本信息

> 请求路径：/api/patents/{category}/{publicNum}
>
> 请求方式：GET
>
> 接口描述：根据 category + publicNum 获取单条专利记录。

#### 2.3.2 请求参数

请求参数格式：path

请求参数说明：

| 参数名称 | 说明 | 类型 | 是否必须 | 备注 |
| -------- | ---- | ---- | -------- | ---- |
| category | 专利类别 | string | 是 | wind/solar/biomass/hydrogen/lilon |
| publicNum | 公开号 | string | 是 |  |

请求数据样例：

```shell
GET /api/patents/wind/CN123456789A
```

#### 2.3.3 响应数据

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "publicNum": "CN123456789A",
    "title": "一种用于……的方法"
  }
}
```

## 3. 专家相关接口

### 3.1 查询专家

#### 3.1.1 基本信息

> 请求路径：/api/experts
>
> 请求方式：GET
>
> 接口描述：query 为空返回全部专家；query 不为空按 name/field 模糊查询。

#### 3.1.2 请求参数

请求参数格式：queryString

请求参数说明：

| 参数名称 | 说明 | 类型 | 是否必须 | 备注 |
| -------- | ---- | ---- | -------- | ---- |
| query | 查询关键字 | string | 否 | 为空返回全表 |

请求数据样例：

```shell
GET /api/experts?query=人工智能
GET /api/experts
```

#### 3.1.3 响应数据

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "李四",
      "field": "人工智能",
      "expertise": "知识图谱"
    }
  ]
}
```

### 3.2 创建专家

#### 3.2.1 基本信息

> 请求路径：/api/experts
>
> 请求方式：POST
>
> 接口描述：新增一条专家记录。

#### 3.2.2 请求参数

请求参数格式：application/json

请求参数说明：

| 参数名称 | 说明 | 类型 | 是否必须 | 备注 |
| -------- | ---- | ---- | -------- | ---- |
| name | 姓名 | string | 是 |  |
| field | 领域 | string | 否 |  |
| expertise | 专长 | string | 否 |  |
| achievements | 成果 | string | 否 |  |
| contactInfo | 联系方式 | string | 否 |  |

请求数据样例：

```json
{
  "name": "李四",
  "field": "人工智能",
  "expertise": "知识图谱"
}
```

#### 3.2.3 响应数据

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "id": 1,
    "name": "李四",
    "field": "人工智能"
  }
}
```

## 4. 需求与匹配相关接口

### 4.1 创建需求

#### 4.1.1 基本信息

> 请求路径：/api/requirements
>
> 请求方式：POST
>
> 接口描述：发布一条需求记录；createdDate 自动写入，status 默认 PENDING。

#### 4.1.2 请求参数

请求参数格式：application/json

请求参数说明：

| 参数名称 | 说明 | 类型 | 是否必须 | 备注 |
| -------- | ---- | ---- | -------- | ---- |
| title | 标题 | string | 是 |  |
| description | 详情 | string | 否 |  |
| keywords | 关键词 | string | 否 | 用于匹配 |
| techDirection | 技术方向 | string | 否 | 对齐 requirement.tech_direction |
| cooperationMode | 合作模式 | string | 否 | 对齐 requirement.cooperation_mode |

请求数据样例：

```json
{
  "title": "设备预测性维护",
  "keywords": "预测性维护,异常检测",
  "techDirection": "时序异常检测",
  "cooperationMode": "技术转让"
}
```

#### 4.1.3 响应数据

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "id": 1,
    "title": "设备预测性维护",
    "status": "PENDING"
  }
}
```

### 4.2 需求匹配专利（跨 5 表）

#### 4.2.1 基本信息

> 请求路径：/api/requirements/{id}/match-patents
>
> 请求方式：GET
>
> 接口描述：基于需求的 keywords 或 title，在 5 张专利分表中进行检索匹配并返回摘要列表。

#### 4.2.2 请求参数

请求参数格式：path

请求参数说明：

| 参数名称 | 说明 | 类型 | 是否必须 | 备注 |
| -------- | ---- | ---- | -------- | ---- |
| id | 需求ID | number | 是 |  |

请求数据样例：

```shell
GET /api/requirements/1/match-patents
```

#### 4.2.3 响应数据

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": [
    {
      "category": "wind",
      "publicNum": "CN123456789A",
      "title": "一种用于……的方法",
      "applicant": "某某科技有限公司",
      "inventor": "张三"
    }
  ]
}
```

### 4.3 需求匹配专家

#### 4.3.1 基本信息

> 请求路径：/api/requirements/{id}/match-experts
>
> 请求方式：GET
>
> 接口描述：基于需求的 keywords 或 title，在专家库的 expertise 字段中进行模糊匹配。

#### 4.3.2 请求参数

请求参数格式：path

请求参数说明：

| 参数名称 | 说明 | 类型 | 是否必须 | 备注 |
| -------- | ---- | ---- | -------- | ---- |
| id | 需求ID | number | 是 |  |

请求数据样例：

```shell
GET /api/requirements/1/match-experts
```

#### 4.3.3 响应数据

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "李四",
      "field": "人工智能"
    }
  ]
}
```

### 4.4 匹配结果落库（规划，对齐 requirement_patent_match / requirement_expert_match）

> 接口描述：用于“匹配推送/历史追踪/统计分析”，将匹配结果写入数据库。

#### 4.4.1 保存专利匹配结果

> 请求路径：/api/requirements/{id}/match-patents/persist
>
> 请求方式：POST
>
> 接口描述：将匹配到的专利结果写入 requirement_patent_match（可包含 matchScore、matchReason）。

请求参数格式：path + application/json

请求参数说明（Path）：

| 参数名称 | 说明 | 类型 | 是否必须 | 备注 |
| -------- | ---- | ---- | -------- | ---- |
| id | 需求ID | number | 是 |  |

请求参数说明（Body）：

| 参数名称 | 说明 | 类型 | 是否必须 | 备注 |
| -------- | ---- | ---- | -------- | ---- |
| items | 匹配项列表 | array | 是 | requirement_patent_match[] |
| \|-patentCategory | 专利类别 | string | 是 | wind/solar/biomass/hydrogen/lilon |
| \|-patentPublicNum | 公开号 | string | 是 | |
| \|-matchScore | 分数 | number | 否 | |
| \|-matchReason | 原因 | string | 否 | |

请求数据样例：

```json
{
  "items": [
    {
      "patentCategory": "wind",
      "patentPublicNum": "CN123456789A",
      "matchScore": 0.912,
      "matchReason": "关键词命中：预测性维护、异常检测"
    }
  ]
}
```

#### 4.4.2 保存专家匹配结果

> 请求路径：/api/requirements/{id}/match-experts/persist
>
> 请求方式：POST
>
> 接口描述：将匹配到的专家结果写入 requirement_expert_match。

请求参数格式：path + application/json

请求数据样例：

```json
{
  "items": [
    {
      "expertId": 1,
      "matchScore": 0.855,
      "matchReason": "expertise 匹配：异常检测"
    }
  ]
}
```

## 5. 转化成果相关接口

### 5.1 查询全部成果

#### 5.1.1 基本信息

> 请求路径：/api/transformations
>
> 请求方式：GET
>
> 接口描述：查询全部转化成果记录（transformation_result）。

#### 5.1.2 请求参数

无

#### 5.1.3 响应数据

响应数据样例：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "patentCategory": "wind",
      "patentPublicNum": "CN123456789A",
      "status": "Success"
    }
  ]
}
```

### 5.2 创建成果记录

#### 5.2.1 基本信息

> 请求路径：/api/transformations
>
> 请求方式：POST
>
> 接口描述：新增一条转化成果记录。

#### 5.2.2 请求参数

请求参数格式：application/json

请求参数说明（与 transformation_result 对齐）：

| 参数名称 | 说明 | 类型 | 是否必须 | 备注 |
| -------- | ---- | ---- | -------- | ---- |
| patentCategory | 专利类别 | string | 是 | wind/solar/biomass/hydrogen/lilon |
| patentPublicNum | 公开号 | string | 是 | 对应分表 public_num |
| expertId | 专家ID | number | 否 | 对应 expert.id |
| requirementId | 需求ID | number | 否 | 对应 requirement.id |
| partnerOrgId | 合作方机构ID | number | 否 | 对应 organization.id |
| description | 描述 | string | 否 | |
| transformationDate | 转化日期 | string | 否 | yyyy-MM-dd |
| status | 状态 | string | 否 | Success / In Progress |
| benefitAmount | 转化效益金额 | number | 否 | |

请求数据样例：

```json
{
  "patentCategory": "wind",
  "patentPublicNum": "CN123456789A",
  "expertId": 1,
  "requirementId": 1,
  "partnerOrgId": 10,
  "status": "Success",
  "benefitAmount": 1000000
}
```

## 6. 专利价值评估与预测（规划，对齐 patent_valuation_report / patent_valuation_model_param）

### 6.1 生成评估报告

#### 6.1.1 基本信息

> 请求路径：/api/valuations
>
> 请求方式：POST
>
> 接口描述：对指定专利生成价值评估报告并保存（写入 patent_valuation_report）。

#### 6.1.2 请求参数

请求参数格式：application/json

请求参数说明：

| 参数名称 | 说明 | 类型 | 是否必须 | 备注 |
| -------- | ---- | ---- | -------- | ---- |
| patentCategory | 专利类别 | string | 是 | wind/solar/biomass/hydrogen/lilon |
| patentPublicNum | 公开号 | string | 是 | |
| modelVersion | 模型版本 | string | 否 | |

请求数据样例：

```json
{
  "patentCategory": "wind",
  "patentPublicNum": "CN123456789A",
  "modelVersion": "v1"
}
```

### 6.2 查询评估报告列表

> 请求路径：/api/valuations
>
> 请求方式：GET
>
> 接口描述：按专利定位信息查询报告列表。

请求参数格式：queryString

| 参数名称 | 说明 | 类型 | 是否必须 | 备注 |
| -------- | ---- | ---- | -------- | ---- |
| patentCategory | 专利类别 | string | 是 | |
| patentPublicNum | 公开号 | string | 是 | |

### 6.3 维护评估模型参数

> 请求路径：/api/valuation-params/{key}
>
> 请求方式：PUT
>
> 接口描述：更新评估模型参数（写入 patent_valuation_model_param），用于管理员维护评估准确性。

 
