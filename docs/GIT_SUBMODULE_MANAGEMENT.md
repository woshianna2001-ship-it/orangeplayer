# Git 子模块管理指南

## 📋 当前状态

**GSYVideoPlayer-source** 在 Git 中被标记为子模块（gitlink），但没有独立的 .git 目录，导致：
- ❌ 修改不会被 Git 跟踪
- ❌ 无法提交子模块内的更改
- ❌ 无法拉取上游更新

## 🎯 推荐方案：修复子模块 + 本地分支管理

### 步骤 1：重新初始化子模块

```bash
# 1. 创建 .gitmodules 文件
cat > .gitmodules << 'EOF'
[submodule "GSYVideoPlayer-source"]
    path = GSYVideoPlayer-source
    url = https://github.com/CarGuo/GSYVideoPlayer.git
    branch = master
EOF

# 2. 初始化子模块
git submodule init

# 3. 更新子模块（拉取上游代码）
git submodule update --remote
```

### 步骤 2：在子模块中创建本地分支

```bash
# 1. 进入子模块目录
cd GSYVideoPlayer-source

# 2. 创建并切换到自定义分支
git checkout -b orangeplayer-custom

# 3. 添加你的修改
git add module-lite-more-fixed.sh
git add 16kpatch/
git add doc/IJK_BUILD_WITH_16K_PATCH.md

# 4. 提交修改
git commit -m "添加 OrangePlayer 自定义配置

- 添加 module-lite-more-fixed.sh（修复版 FFmpeg 配置）
- 添加 16K Page Size 补丁
- 添加编译文档
"

# 5. 回到主项目
cd ..
```

### 步骤 3：在主项目中提交子模块引用

```bash
# 1. 添加 .gitmodules
git add .gitmodules

# 2. 更新子模块引用
git add GSYVideoPlayer-source

# 3. 提交
git commit -m "修复 GSYVideoPlayer-source 子模块配置"
```

---

## 🔄 日常使用：如何拉取上游更新

### 方法 1：手动合并（推荐）

```bash
# 1. 进入子模块
cd GSYVideoPlayer-source

# 2. 添加上游远程仓库（只需执行一次）
git remote add upstream https://github.com/CarGuo/GSYVideoPlayer.git

# 3. 拉取上游更新
git fetch upstream

# 4. 查看上游更新内容
git log HEAD..upstream/master --oneline

# 5. 合并上游更新到你的自定义分支
git merge upstream/master

# 6. 解决冲突（如果有）
# 编辑冲突文件，然后：
git add <冲突文件>
git commit

# 7. 回到主项目，更新子模块引用
cd ..
git add GSYVideoPlayer-source
git commit -m "更新 GSYVideoPlayer-source 到最新版本"
```

### 方法 2：Rebase（保持提交历史整洁）

```bash
# 1. 进入子模块
cd GSYVideoPlayer-source

# 2. 拉取上游更新
git fetch upstream

# 3. Rebase 你的修改到上游最新版本
git rebase upstream/master

# 4. 解决冲突（如果有）
# 编辑冲突文件，然后：
git add <冲突文件>
git rebase --continue

# 5. 回到主项目
cd ..
git add GSYVideoPlayer-source
git commit -m "更新 GSYVideoPlayer-source 到最新版本"
```

---

## 📊 对比不同方案

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| **子模块 + 本地分支** | ✅ 可跟踪上游更新<br>✅ 可管理自定义修改<br>✅ 清晰的版本控制 | ⚠️ 需要手动合并更新<br>⚠️ 稍微复杂 | ✅ 推荐（你的情况） |
| **转为普通目录** | ✅ 简单直接<br>✅ 易于修改 | ❌ 无法跟踪上游更新<br>❌ 难以对比差异 | ⚠️ 不推荐 |
| **Fork 仓库** | ✅ 可跟踪上游更新<br>✅ 可提交到远程 | ⚠️ 需要维护 fork<br>⚠️ 需要 GitHub 账号 | ⚠️ 如果要贡献代码 |

---

## 🛠️ 实用命令

### 查看子模块状态

```bash
# 查看所有子模块状态
git submodule status

# 查看子模块的远程仓库
cd GSYVideoPlayer-source
git remote -v

# 查看当前分支
git branch

# 查看与上游的差异
git diff upstream/master
```

### 查看你的自定义修改

```bash
cd GSYVideoPlayer-source

# 查看与上游的差异
git diff upstream/master

# 查看你的提交历史
git log upstream/master..HEAD --oneline

# 查看修改的文件列表
git diff --name-only upstream/master
```

### 撤销子模块修改

```bash
# 撤销子模块内的所有修改
cd GSYVideoPlayer-source
git reset --hard HEAD

# 回到主项目指定的 commit
cd ..
git submodule update --force
```

---

## 🎯 你的具体操作步骤

### 第一次设置（只需执行一次）

```bash
# 1. 创建 .gitmodules
cat > .gitmodules << 'EOF'
[submodule "GSYVideoPlayer-source"]
    path = GSYVideoPlayer-source
    url = https://github.com/CarGuo/GSYVideoPlayer.git
    branch = master
EOF

# 2. 进入子模块，创建自定义分支
cd GSYVideoPlayer-source
git init
git remote add origin https://github.com/CarGuo/GSYVideoPlayer.git
git fetch origin
git checkout -b orangeplayer-custom origin/master

# 3. 添加上游远程仓库
git remote add upstream https://github.com/CarGuo/GSYVideoPlayer.git

# 4. 提交你的自定义修改
git add module-lite-more-fixed.sh
git add 16kpatch/
git commit -m "添加 OrangePlayer 自定义配置"

# 5. 回到主项目
cd ..
git add .gitmodules
git add GSYVideoPlayer-source
git commit -m "修复 GSYVideoPlayer-source 子模块配置"
```

### 以后拉取上游更新

```bash
# 1. 进入子模块
cd GSYVideoPlayer-source

# 2. 拉取上游更新
git fetch upstream

# 3. 合并到你的分支
git merge upstream/master

# 4. 回到主项目
cd ..
git add GSYVideoPlayer-source
git commit -m "更新 GSYVideoPlayer-source"
```

---

## 💡 最佳实践

1. **定期拉取上游更新**：每月检查一次上游是否有重要更新
2. **保持自定义修改最小化**：只修改必要的文件，减少合并冲突
3. **记录修改原因**：在 commit message 中说明为什么要修改
4. **测试后再合并**：拉取上游更新后，先测试再提交到主项目

---

## 🔗 相关资源

- [Git 子模块官方文档](https://git-scm.com/book/zh/v2/Git-%E5%B7%A5%E5%85%B7-%E5%AD%90%E6%A8%A1%E5%9D%97)
- [GSYVideoPlayer 官方仓库](https://github.com/CarGuo/GSYVideoPlayer)
- [Git Submodule 最佳实践](https://git-scm.com/book/en/v2/Git-Tools-Submodules)
