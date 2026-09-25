# 开发日志

## 项目：springcloud-eshop（SpringCloud Alibaba + RAG + LLM 电商导购平台）

---

## 2026-07-22 — AI 导购流式对话修复 & 技术栈缺口

### 一、当前问题：AI 导购对话一直转圈圈

**现象：** 前端 AI 导购发送消息后，一直显示 loading/转圈，无任何响应返回。

**排查过程：**

1. 直接访问 AI 服务健康检测 → ✅ 正常 (`GET /api/ai/health`)
2. 非流式对话 (`POST /api/ai/chat`) → 400 Bad Request（经过网关时）
3. 流式对话 (`POST /api/ai/chat/stream`) → 400 Bad Request（经过网关时）
4. 直接访问 AI 服务端口 8085（跳过网关）→ ✅ 正常返回 SSE
5. 登录/注册 POST → ✅ 正常（确认网关 POST 路由没问题）

**根因：**

`AiGuideController.chatStream()` 方法使用了：

```java
@PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
```

- `produces` 约束要求请求必须带 `Accept: text/event-stream` 头
- 前端 `fetch()` 没有设置 `Accept` 头
- Spring Cloud Gateway（WebFlux 架构）转发时 content negotiation 处理与 Servlet 容器不一致，导致 400
- 直接访问 AI 服务时配合 `-H "Accept: text/event-stream"` 可以正常工作

**修复：**

```java
// 修改前
@PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
// 修改后
@PostMapping("/chat/stream")
```

去掉 `produces` 约束即可。`SseEmitter` 本身会自动设置 `Content-Type: text/event-stream` 响应头，不影响 SSE 功能。

**修复文件：**
- `eshop-ai-guide-service/src/main/java/com/eshop/ai/controller/AiGuideController.java` 第50行

**验证结果：✅ 后端修复成功，前端修复也确认**

**后端修复（`AiGuideController.java` 第50行）：**
- 去掉 `produces = MediaType.TEXT_EVENT_STREAM_VALUE` → SSE 流不再要求客户端传 `Accept: text/event-stream` 头

**前端修复（`frontend/src/api/ai.js` 第40-43行）：**
- SSE 解析时 Spring 的 `SseEmitter` 输出格式为 `data:{"type":"text"}`（冒号后**无空格**）
- 原前端代码 `line.startsWith('data: ')` 要求有空格，导致所有 SSE 事件被忽略 → 前端永远收不到 `done` 事件 → 无限转圈
- 修复为 `line.startsWith('data:')` + `line.slice(5).trimStart()`，兼容有无空格两种格式

**测试验证：**
- ✅ 直接访问 AI 服务 (localhost:8085) → SSE 流正常
- ✅ 通过网关路由 (localhost:8086) → SSE 流正常  
- ✅ 带 JWT Token 认证（Gateway 过滤后转发）→ SSE 流正常
- ✅ 通过 Vite 代理 (localhost:3000) → SSE 流正常
- ✅ 前端 SSE 数据行格式确认：`data:{"type":"text"}`（无空格）
- ✅ API Key 有效（`curl https://api.deepseek.com/v1/models` 返回 200）

**注意：** 调试过程中遇到的 400 错误实际是 Git Bash 发送中文到 `curl` 时的编码问题（Windows GBK → UTF-8），非代码缺陷。前端 `fetch()` 默认 UTF-8 编码无此问题。

---

### 二、技术栈审计结果

项目标称技术栈：**SpringCloud Alibaba + 自研 RAG + 大模型接入**

| 技术组件 | 状态 | 说明 |
|---------|------|------|
| Nacos 服务发现 | ✅ 已完成 | 6个微服务均已注册 |
| Gateway 路由 | ✅ 已完成 | 5条路由 + JWT 全局过滤器 |
| OpenFeign | ✅ 已完成 | Cart→Product, AI→Product |
| 负载均衡 | ✅ 已完成 | 默认 Round-Robin |
| DeepSeek 大模型 | ✅ 已完成 | 流式(SSE) + 非流式双模式 |
| **自研 RAG** | **❌ 缺失** | 无向量数据库、无 Embedding、无知识库问答 |
| **Sentinel 熔断降级** | **❌ 缺失** | 无流控/降级/熔断配置 |
| **Nacos 配置中心** | **❌ 缺失** | 全部配置在 yml 文件中硬编码 |
| **密码加密** | **❌ 缺失** | 明文存储，需要 bcrypt |
| **MyBatis-Plus 分页插件** | **❌ 缺失** | 运行时可能报错 |

---

### 三、RAG 实现方案

#### 为什么需要 RAG？

目前 AI 导购虽然接了 DeepSeek 大模型，但存在以下问题：

1. **知识截止日期限制** — 大模型只知训练数据中的知识（如 DeepSeek 知识截止 2025-05）
2. **无法获取商品实时信息** — 虽然现有实现通过 Feign 查询「猜你喜欢」和「随机推荐」，但不够智能
3. **无法回答商品详情问题** — 用户问「XX 手机有什么颜色」「哪个商品性价比高」时，大模型不知道具体商品数据
4. **无法回答平台相关问题** — 如「退货政策是什么」「什么时候发货」

RAG 可以让大模型「看到」平台的实时数据。

#### 技术选型

| 组件 | 方案 | 理由 |
|------|------|------|
| 向量数据库 | **Elasticsearch**（推荐）或 pgvector | ES 适合全文搜索+向量搜索混合，本机已有环境则更好 |
| Embedding 模型 | **BCE Embedding**（BAAI）或通义千问 Embedding | 国产、中文效果好、免费 |
| 知识来源 | 商品库 + 静态文档（政策/FAQ） | 两部分：结构化商品数据 + 非结构化文档 |
| RAG 框架 | **LangChain4j**（Java 生态）或自定义 | LangChain4j 有 Spring Boot 集成 |

#### 推荐实现路径（分阶段）

**Phase 1：商品语义搜索（MVP）**

1. 创建 `eshop-rag-service` 新模块（或合并在 AI 导购服务中）
2. 引入 Embedding 模型（通过 HTTP API 调用）
3. 商品数据同步：定时任务将商品信息向量化存入向量库
4. AI 导购对话时：先做向量检索找到相关商品 → 拼入 Prompt → 调用 DeepSeek

**Phase 2：文档知识库**

1. 建立平台 FAQ / 退货政策 / 物流说明等静态知识文档
2. 文档切片 + 向量化存储
3. 对话中根据意图判断是「商品查询」还是「知识问答」

**Phase 3：对话记忆增强**

1. 历史对话向量化，实现长期记忆
2. 多轮对话中自动检索相关历史上下文

#### 关键数据流

```
用户消息
    ↓
[意图分类] ←→ 商品查询 → 向量检索商品 → 商品信息拼入 Prompt
    ↓                    ↓
  闲聊/其他      DeepSeek + 上下文 → SSE 流式回复
    ↓
  无 RAG 增强       ↓
             同时 Feign 拉取推荐商品
                 → 拼接在回复末尾
```

---

### 四、其他待修复问题

1. **密码明文存储** — `UserService.java:43`，`user.setPassword(password)` 无加密
2. **MyBatis-Plus 分页插件** — OrderService 使用 `new Page<>()` 依赖分页插件，需在配置中注入 `PaginationInnerInterceptor`
3. **Sentinel 熔断降级** — 需要在 Gateway 和 AI 服务中集成 Sentinel，防止服务雪崩
4. **Nacos 配置中心** — 需要将 `application.yml` 中的配置迁移到 Nacos Config

---

### 五、架构图（当前现状）

```
前端 (Vue3 + Vite)
    ↓ /api/*
Gateway (Spring Cloud Gateway, :8086)
    ↓ JWT 过滤
    ├── User Service (:8081) — 用户注册登录
    ├── Product Service (:8082) — 商品 CRUD
    ├── Cart Service (:8083) — 购物车
    ├── Order Service (:8084) — 订单
    └── AI Guide Service (:8085) — 对话 + DeepSeek 调用
```

**问题：**
- Sentinel 未接入，Gateway 无降级保护
- AI 服务如崩溃 → 前端无反馈（超时转圈）
- 缺少配置中心，每次改配置需重启服务

---

### 六、AI 导购定位升级：从商品搜索到平台全能入口

讨论日期：2026-07-22

#### 当前局限

当前 AI 导购只做了 DeepSeek 的对话接入，存在两个关键问题：

1. **AI 回复的商品不是商城真实商品** — DeepSeek 根据训练数据生成回复，提到的商品（如"华为P60"）不一定在数据库里有
2. **只覆盖商品推荐，不覆盖平台功能** — 用户问"怎么退货""我的快递到哪了""有没有优惠券"时，AI 不知道怎么回答，用户也不知道这些功能入口在哪

**用户场景驱动：**
- 老爷爷想用语音购物，打字困难
- 用户遇到售后问题，不知道在哪找售后窗口
- 功能多了以后，菜单层级越来越深，用户找不到

#### 核心定位变化

```
之前：AI 导购 = 智能商品搜索框 / 对话机器人
之后：AI 导购 = 平台全能入口（商品搜索 + 功能导航 + 语音交互）
```

AI 要成为用户和复杂功能之间的**翻译层**：用户不需要知道功能入口在哪，直接说需求，AI 理解并给出带可点击操作的回复。

#### 三条实现路径

##### 路径 A：前端硬编码路由映射（最低成本）

**做法：**
- 后端维护一份「平台功能注册表」，注入到 DeepSeek 系统提示词中
- AI 回复时按约定格式标注可执行操作（如 `[[action:售后|/order/after-sale]]`）
- 前端解析这种标记渲染为可点击按钮/链接

```
用户："我要退货"
  ↓ DeepSeek 回复含标记
"好的，您的订单 #12345 已签收3天，在7天退换期内。
可以 [[action:申请退货|/order/123/return]] 或 [[action:联系客服|/service]]"
  ↓ 前端渲染
"好的，您的订单 #12345 已签收3天，在7天退换期内。
可以 [📄 申请退货] 或 [📞 联系客服]"
```

**优点：** 今天就能做，不需要新服务，不增加架构复杂度
**缺点：** 功能注册表需手动维护；新增功能要更新 Prompt

##### 路径 B：功能意图中间层（推荐中期）

**做法：**
- 在 AI 服务中定义功能注册表（结构化 JSON）：

```json
{
  "售后申请":     { "route": "/order/after-sale", "keywords": ["退货","换货","退款","坏了","修"] },
  "物流查询":     { "route": "/order/logistics",  "keywords": ["快递","到哪了","物流","发货","配送"] },
  "修改地址":     { "route": "/order/address",    "keywords": ["改地址","换地址","重发","换个地方"] },
  "优惠券":       { "route": "/coupon",           "keywords": ["优惠券","折扣码","优惠","满减"] },
  "个人中心":     { "route": "/user/profile",     "keywords": ["我的","个人信息","改密码","头像"] },
  "购物车":       { "route": "/cart",             "keywords": ["购物车","加购","买东西"] }
}
```

- 用户提问 → 先做意图匹配（LLM 分类或关键词匹配）→ 确定命中的功能 → 把功能信息拼入 Prompt
- DeepSeek 回复时结构化返回 `{text, actions: [{label, route}]}`
- 前端收到后同时渲染文本和操作卡片

**优点：** 功能可动态注册，扩展性好；意图匹配准确度高
**缺点：** 需要写匹配逻辑；比路径 A 多一点开发量

##### 路径 C：RAG + 完整语义理解（最终形态）

**做法：**
- 把每个功能写成一段场景化的说明文档（如"退货政策文档"）
- 文档向量化存入向量数据库
- 用户提问 → 语义检索最相关的 1-3 个功能 + 商品 → LLM 综合生成

```
用户："我买的手机有个零件坏了，能换吗？"
                              ↓ 语义检索
商品检索 → "华为P60"（在库）
功能检索 → "7天无理由退换货政策" "售后申请流程"
                              ↓ LLM 综合
回复："您购买的华为P60在7天无理由退换范围内，
      需要帮您打开售后申请页面吗？"
      [📄 申请售后] [📞 联系客服]
```

**优点：** 功能文档可随时更新，语义理解最灵活；与 RAG 架构天然统一
**缺点：** 需要向量数据库 + Embedding 模型；开发周期最长

#### 语音输入评估

难度：**低**（纯前端改动，0 后端改动）

使用浏览器原生 Web Speech API：

```javascript
const recognition = new (window.SpeechRecognition || window.webkitSpeechRecognition)()
recognition.lang = 'zh-CN'
recognition.onresult = (e) => input.value = e.results[0][0].transcript
```

- Chrome / Edge 全支持，不需要第三方服务
- 在聊天输入框加一个 🎤 按钮即可
- 语音识别结果填入输入框，用户确认后发送

#### 当前最严重的问题（先于功能升级解决）

**DeepSeek 推荐的商品不是商城实际商品。** 需要先做「商品数据注入 Prompt」：

```
用户提问 "推荐手机"
         ↓
搜索 product-service 获取实际商品列表
         ↓
把商品列表拼入 DeepSeek 的 message context
         ↓
DeepSeek 基于真实商品数据生成回复
```

这是 Phase 0，改 1 个文件就能做。

#### 建议分阶段计划

| 阶段 | 内容 | 工作量 | 前置条件 |
|------|------|--------|---------|
| ~~Phase 0~~ | ~~商品数据注入 Prompt~~ | ~~改 2 个后端文件~~ | ~~已完成~~ |
| **Phase 1** | 功能注册表 + 前端 action 卡片渲染 | 后端 1 个新类 + 前端改 1 个组件 | 无 |
| **Phase 1.5** | 🎤 语音输入按钮 | 纯前端，加 1 个按钮 | 无 |
| **Phase 2** | RAG 商品语义搜索 | 新建 eshop-rag-service | 需要向量数据库 |
| **Phase 3** | RAG + 功能文档语义检索 | 融合 Phase 1 + Phase 2 | 需要 Phase 2 完成 |

---

## 2026-07-22 — RAG Phase 0 实现：商品数据注入 DeepSeek Prompt

### 改动内容

**问题：** DeepSeek 推荐的商品是训练数据中的商品（如「华为P60」「iPhone 15」），不是商城数据库里的真实商品。用户搜「手机」会得到不存在于数据库中的商品信息。

**方案：RAG（检索增强生成）Phase 0 — 查询时检索商品并注入 Prompt**

```
用户提问 "推荐手机"
         ↓
搜索 product-service 获取真实商品列表（关键词匹配）
         ↓
格式化为可读文本（名称、价格、描述、品牌、销量、类目）
         ↓
注入到 DeepSeek 系统提示词的「当前商城可售商品」章节
         ↓
DeepSeek 严格基于真实商品数据生成回复
```

### 修改文件

**1. `eshop-ai-guide-service/.../service/DeepSeekService.java`**

- `buildRequestBody(messages, stream)` → 增加 `productContext` 参数
- `buildSystemPrompt()` → 增加 `productContext` 参数
  - 有商品数据：注入格式化商品列表，限令 AI 只推荐列表中商品
  - 无商品数据：提示 AI 告知用户「暂无此商品」
- 新增 `callStreaming(messages, productContext, ...)` 重载方法
- 新增 `callNonStreaming(messages, productContext)` 重载方法
- 旧方法保持签名不变，委托给新方法（向后兼容）

**2. `eshop-ai-guide-service/.../service/AiGuideService.java`**

- `chat()`（非流式）：先搜索商品再调用 DeepSeek，而不是之后
- `chatStream()`（流式 SSE）：在异步调用 DeepSeek **之前**搜索商品，注入上下文
- 新增 `formatProductContext()` 方法：将商品列表格式化为 LLM 可读文本
- `extractKeywords()` 改进：增加动作词停用表（推荐、找、搜索、想要等），去除常见动作动词保留核心商品关键词
- `searchProducts()` 改进：支持多关键词兜底策略（完整关键词无结果时，逐个尝试分词后的子关键词）

### 关键技术决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 检索时机 | 调用 DeepSeek **之前** | 让 AI 在生成时就知道真实商品，而非事后补推荐 |
| 商品数量 | 检索 10 个，注入全部 | 平衡上下文长度和召回覆盖率 |
| 注入位置 | System Prompt，非 Messages | 保证优先级最高，AI 每次回复都看到 |
| 降级策略 | 无商品时提示 AI 坦诚告知 | 避免 AI 编造不存在的商品 |
| 关键词提取 | 简单去停用词 | 当前够用，后续可替换为 NLP 语义检索 |

### 数据流（更新后）

```
用户消息
    ↓
[保存到 DB]
    ↓
[RAG 检索] → Product Feign → product-service 关键词搜索
    ↓
[格式化为上下文文本] ← 最多 10 个商品
    ↓
[构建请求体] → System Prompt + 商品上下文 + 历史消息
    ↓
[调用 DeepSeek API] → 流式 SSE 回复
    ↓
[保存回复到 DB] + [发送 done 事件含推荐商品]
```

### 验证结果

```
用户提问 "推荐手机"
         ↓
提取关键词: "推荐手机" → 去掉动作词"推荐" → "手机"
         ↓
Product Service 搜索: LIKE '%手机%' → 命中 "5G智能手机"
         ↓
注入 Prompt:
  1. 【5G智能手机】 — ¥3999.0
     旗舰处理器，120Hz高刷屏
     品牌：小米
     销量：已售 15678 件
     类目：智能手机
         ↓
DeepSeek 回复（基于真实商品）：
"1️⃣ 5G智能手机 — ¥3999.0
   ✅ 推荐理由：搭载旗舰级处理器，性能强劲...配备120Hz高刷屏...
   目前已售出 15678件，口碑非常好！"
         ↓
Done 事件返回推荐商品卡片：{name: "5G智能手机", price: 3999.0, brand: "小米"}
```

**结论：** ✅ RAG Phase 0 验证通过，AI 导购现在基于真实商品数据回复

### 后续计划

- **Phase 0.5（短期优化）**：用 Embedding 语义搜索替代关键词匹配，提高检索准确率
- **Phase 1（功能注册表）**：让 AI 也能导航到订单、售后等功能页面
- **Phase 2（完整 RAG）**：向量数据库 + 语义检索（需要单独搭建）

---

## 2026-07-22 — 功能完善：密码加密 / 分页插件 / 后台增强 / AI 导购页面 / Sentinel 集成

### 改动清单

#### 1. 密码加密（eshop-user-service）

**问题：** 密码明文存储（`UserService.java:43`），`user.setPassword(password)` 无加密。

**修复：**
- `pom.xml` 添加 `spring-security-crypto` 依赖
- `UserService.java` 引入 `BCryptPasswordEncoder`
- `register()`：`passwordEncoder.encode(password)` 加密入库
- `login()`：`passwordEncoder.matches(password, user.getPassword())` 比对密文
- `updatePassword()`：同上，新旧密码均加密处理

#### 2. MyBatis-Plus 分页插件

**问题：** 使用 `new Page<>()` 但无分页插件，`page.getTotal()` 始终为 0。

**修复：**
- `eshop-product-service` 新建 `config/MyBatisPlusConfig.java`
- `eshop-order-service` 新建 `config/MyBatisPlusConfig.java`
- 注入 `PaginationInnerInterceptor(DbType.MYSQL)`

#### 3. 后台管理增强（frontend）

**AdminProducts.vue：**
- `changeStatus()` 原来只做前端切换，改为调 `PUT /api/product/{id}` 后端 API 持久化
- 补充 `useToastStore` 导入

**AdminOrders.vue：**
- `deliver()` 原来用 `fetch()` 硬编码 URL 和 Token，改为统一 `request.js` 拦截器

**AdminDashboard.vue：**
- 待处理订单数从静态 `-` 改为实时拉取 `GET /api/order/admin/orders?status=1`
- 使用 `Promise.allSettled` 并行加载

#### 4. AI 导购前端页面（frontend）

**问题：** 后端 AI 导购 API 已完善（SSE 流式对话），但前端只有浮窗组件，缺少独立全屏页面。

**新增 `Chat.vue`：**
- 全屏聊天页面，路由 `/chat`
- 左侧对话历史侧栏（列出历史对话，支持加载和删除）
- 欢迎语 + 快捷提问按钮
- SSE 流式消息渲染 + Markdown 加粗解析
- 推荐商品卡片展示（可点击跳转商品详情）
- 停止生成按钮
- 新建对话 / 清空对话

**其他改动：**
- `router/index.js` 注册 `/chat` 路由
- `NavBar.vue` 导航栏增加「AI 导购」入口

#### 5. Sentinel 熔断降级集成

**改动范围（6 个微服务全部接入）：**

| 服务 | 依赖 | Feign Sentinel |
|------|------|----------------|
| Gateway | `sentinel` + `sentinel-gateway` | — |
| AI Guide | `sentinel` | ✅ 开启 |
| Cart | `sentinel` | ✅ 开启 |
| Order | `sentinel` | ✅ 开启 |
| Product | `sentinel` | — |
| User | `sentinel` | — |

**配置：**
- 父 POM `dependencyManagement` 统一管理 Sentinel 版本
- Gateway：`scg.fallback` 配置 503 降级响应
- AI / Cart / Order：`spring.cloud.openfeign.sentinel.enabled: true` 开启 Feign 熔断
- 各服务配置 Sentinel dashboard 地址 `localhost:8718`（可选）

### 修复文件清单

| 模块 | 文件 | 改动类型 |
|------|------|----------|
| 父工程 | `pom.xml` | 新增 Sentinel 版本管理 |
| user-service | `pom.xml` | 新增 `spring-security-crypto` |
| user-service | `UserService.java` | 密码加密改造 |
| product-service | `pom.xml` | 新增 Sentinel |
| product-service | `config/MyBatisPlusConfig.java` | **新建** — 分页插件 |
| product-service | `application.yml` | 新增 Sentinel 配置 |
| order-service | `pom.xml` | 新增 Sentinel |
| order-service | `config/MyBatisPlusConfig.java` | **新建** — 分页插件 |
| order-service | `application.yml` | 新增 Sentinel + Feign Sentinel |
| cart-service | `pom.xml` | 新增 Sentinel |
| cart-service | `application.yml` | 新增 Sentinel + Feign Sentinel |
| ai-guide-service | `pom.xml` | 新增 Sentinel |
| ai-guide-service | `application.yml` | 新增 Sentinel + Feign Sentinel |
| gateway | `pom.xml` | 新增 Sentinel + sentinel-gateway |
| gateway | `application.yml` | 新增 Sentinel SCG 降级 |
| frontend | `views/Chat.vue` | **新建** — AI 导购全屏页面 |
| frontend | `router/index.js` | 新增 `/chat` 路由 |
| frontend | `components/NavBar.vue` | 新增「AI 导购」导航 |
| frontend | `views/admin/AdminProducts.vue` | 上下架调后端 API |
| frontend | `views/admin/AdminOrders.vue` | 改用 request.js |
| frontend | `views/admin/AdminDashboard.vue` | 实时拉取订单数 |

### 未完成的（需外部基础设施）

- **Nacos 配置中心** — 需 Nacos Server 运行（当前 Docker 未启动），改配置到 Config 需重启服务
- **RAG 向量数据库** — Phase 1~3，需要 ES / 向量数据库，当前关键词搜索+子串兜底已够用
