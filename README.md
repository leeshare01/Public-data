# Public-data

存放多个项目的仓库，每个子文件夹是一个独立项目。

---

## 项目列表

### [微服务电商智能导购平台](./微服务电商智能导购平台/)

基于 **Spring Cloud Alibaba + RAG + 大模型** 的电商平台。

在常规微服务电商功能（商品、购物车、订单、用户）之上，增加了一个 AI 导购服务：用户用自然语言描述需求，服务通过 **RAG（检索增强生成）** 从商品库中检索候选商品，再交给大模型生成推荐回答。

- 后端：Spring Boot 3.2.5 + Spring Cloud Alibaba 2023.0.1.0 + MyBatis-Plus + MySQL + Redis，Maven 多模块
- 前端：Vue 3 + Vite
- 启动：进入项目目录后执行 `start.bat`（Windows）或 `bash start.sh`（Linux / macOS）

详细说明见 [项目 README](./微服务电商智能导购平台/README.md)。

---

## 添加新项目

在根目录建一个子文件夹，把项目放进去即可：

```bash
mkdir 新项目名
# 把新项目文件拷进去
git add 新项目名
git commit -m "新增项目：新项目名"
git push
```

> ⚠️ 新项目文件夹里**不要**执行 `git init`。否则 Git 会把它当子模块引用处理，`git push` 看似成功，但 GitHub 上那个文件夹是空的、点不进去。

每个子项目建议自带一份 `README.md`，并在上面的「项目列表」中补一条索引。
