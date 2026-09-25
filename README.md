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

### [Interpretability-AQA-main](./Interpretability-AQA-main/)

基于 **可解释性动作质量评估（AQA）** 的视频理解项目。

在开源工作 [UIL-AQA](https://link.springer.com/article/10.1007/s11263-025-02638-6)（IJCV 2025）/ Interpretable-AQA（BMVC 2024, Oral）基础上，针对**训练不可复现**和 **Fis-V 数据集结果偏低**两个问题做了修复与调优：

- **修复跨 run 非确定性**：`worker_init_fn` 原本取父进程 numpy 运行时状态作为种子，Windows `spawn` 多进程下导致同配置两次训练 SRCC 相差达 0.27（0.679 vs 0.412）。改为模块级可 pickle 的 `_SeedWorker`，直接以 `cfg.seed` 为基础种子，修复后两次运行结果完全一致。
- **Fis-V 结果偏低**：定位到 epoch 数被硬编码截断、`dino_loss` 噪声在测试期仍生效、分数归一化因子 `/43` 写死三个原因，修复后 SRCC 从 0.6695 向论文基线 ~0.78 逼近。

- 技术栈：PyTorch 2.0.1 + CUDA 11.8，TQN（Temporal Query Network）+ Evaluator 双分支
- 数据集：LOGO、Fis-V、Rhythmic Gymnastics（均使用预提取特征训练）

详细的问题排查与技术决策记录见项目内的 `problem/`、`模型的日志和文件/FIX_LOG.md`。

> ⚠️ 模型权重（`ckpts/*.pt`，单个 600MB+）**未包含在本仓库中**，超出 GitHub 单文件 100MB 限制。

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
