# GitHub Secrets 配置指南

为了使用 GitHub Actions 自动发布到 Maven Central，需要配置以下 Secrets。

## 配置步骤

1. 打开你的 GitHub 仓库
2. 进入 **Settings** > **Secrets and variables** > **Actions**
3. 点击 **New repository secret**
4. 添加以下 Secrets

---

## 必需的 Secrets

### 1. OSSRH_USERNAME

**说明**: Sonatype OSSRH 用户名

**获取方式**:
1. 访问 https://central.sonatype.com/
2. 注册账号
3. 使用注册的用户名

**示例值**: `your-username`

---

### 2. OSSRH_PASSWORD

**说明**: Sonatype OSSRH 密码

**获取方式**:
- 使用你在 Sonatype 注册时设置的密码
- 或者生成 User Token（推荐）

**示例值**: `your-password` 或 `your-token`

---

### 3. SIGNING_KEY_ID

**说明**: GPG 密钥 ID（最后 8 位）

**获取方式**:

```bash
# 列出所有密钥
gpg --list-keys

# 输出示例:
# pub   rsa3072 2024-01-01 [SC]
#       1234567890ABCDEF1234567890ABCDEF12345678
# uid           [ultimate] Your Name <your@email.com>

# 取最后 8 位: 12345678
```

**示例值**: `12345678`

---

### 4. SIGNING_PASSWORD

**说明**: GPG 密钥密码

**获取方式**:
- 使用你在生成 GPG 密钥时设置的密码

**示例值**: `your-gpg-password`

---

### 5. GPG_SECRET_KEY

**说明**: GPG 私钥（Base64 编码）

**获取方式**:

**Windows (PowerShell):**
```powershell
# 导出密钥
gpg --export-secret-keys -o secring.gpg

# Base64 编码
[Convert]::ToBase64String([IO.File]::ReadAllBytes("secring.gpg")) | Out-File -Encoding ASCII gpg-base64.txt

# 复制 gpg-base64.txt 的内容
```

**Linux/Mac:**
```bash
# 导出密钥
gpg --export-secret-keys -o secring.gpg

# Base64 编码
base64 secring.gpg > gpg-base64.txt

# 复制 gpg-base64.txt 的内容
cat gpg-base64.txt
```

**示例值**: 一长串 Base64 编码的字符串

---

## 验证配置

配置完成后，可以通过以下方式验证：

### 1. 手动触发工作流

1. 进入 **Actions** 标签页
2. 选择 **Publish to Maven Central** 工作流
3. 点击 **Run workflow**
4. 输入版本号（如 `1.0.6`）
5. 点击 **Run workflow**

### 2. 创建 Release 触发

1. 进入 **Releases** 标签页
2. 点击 **Draft a new release**
3. 创建新的 Tag（如 `v1.0.6`）
4. 填写 Release 信息
5. 点击 **Publish release**
6. 工作流会自动触发

---

## 安全建议

### 1. 使用 User Token 代替密码

在 Sonatype Central Portal 中生成 User Token：

1. 登录 https://central.sonatype.com/
2. 进入 Account Settings
3. 生成 User Token
4. 使用 Token 作为 `OSSRH_PASSWORD`

### 2. 定期轮换密钥

- 每 6-12 个月更新 GPG 密钥
- 更新 Sonatype 密码或 Token
- 更新 GitHub Secrets

### 3. 限制访问权限

- 只给必要的人员访问 Secrets
- 使用 Environment Secrets 进行更细粒度的控制
- 定期审查 Actions 日志

### 4. 备份密钥

- 备份 GPG 密钥到安全位置
- 记录密钥 ID 和密码
- 保存 Sonatype 凭证

---

## 故障排查

### 问题 1: GPG 签名失败

**错误信息**: `gpg: signing failed: No secret key`

**解决方案**:
1. 检查 `SIGNING_KEY_ID` 是否正确（最后 8 位）
2. 检查 `GPG_SECRET_KEY` 是否正确编码
3. 检查 `SIGNING_PASSWORD` 是否正确

### 问题 2: 认证失败

**错误信息**: `401 Unauthorized`

**解决方案**:
1. 检查 `OSSRH_USERNAME` 和 `OSSRH_PASSWORD` 是否正确
2. 确认 Sonatype 账号已激活
3. 尝试使用 User Token 代替密码

### 问题 3: 命名空间未验证

**错误信息**: `Namespace not verified`

**解决方案**:
1. 登录 https://central.sonatype.com/
2. 完成命名空间验证流程
3. 对于 `io.github.yourusername`，需要验证 GitHub 账号

### 问题 4: Base64 解码失败

**错误信息**: `base64: invalid input`

**解决方案**:
1. 确保 Base64 字符串没有换行符
2. 重新生成 Base64 编码
3. 使用 `base64 -w 0` (Linux) 或 `-b 0` (Mac) 避免换行

---

## 测试配置

创建一个测试工作流来验证 Secrets 配置：

```yaml
name: Test Secrets

on:
  workflow_dispatch:

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - name: Test OSSRH credentials
        run: |
          if [ -z "${{ secrets.OSSRH_USERNAME }}" ]; then
            echo "❌ OSSRH_USERNAME not set"
          else
            echo "✅ OSSRH_USERNAME is set"
          fi
          
          if [ -z "${{ secrets.OSSRH_PASSWORD }}" ]; then
            echo "❌ OSSRH_PASSWORD not set"
          else
            echo "✅ OSSRH_PASSWORD is set"
          fi
      
      - name: Test GPG configuration
        run: |
          if [ -z "${{ secrets.SIGNING_KEY_ID }}" ]; then
            echo "❌ SIGNING_KEY_ID not set"
          else
            echo "✅ SIGNING_KEY_ID is set"
          fi
          
          if [ -z "${{ secrets.SIGNING_PASSWORD }}" ]; then
            echo "❌ SIGNING_PASSWORD not set"
          else
            echo "✅ SIGNING_PASSWORD is set"
          fi
          
          if [ -z "${{ secrets.GPG_SECRET_KEY }}" ]; then
            echo "❌ GPG_SECRET_KEY not set"
          else
            echo "✅ GPG_SECRET_KEY is set"
          fi
      
      - name: Test GPG key decoding
        run: |
          echo "${{ secrets.GPG_SECRET_KEY }}" | base64 --decode > test.gpg
          if [ -f test.gpg ]; then
            echo "✅ GPG key decoded successfully"
            ls -lh test.gpg
          else
            echo "❌ GPG key decoding failed"
          fi
```

将此内容保存为 `.github/workflows/test-secrets.yml`，然后手动运行来验证配置。

---

## 参考资料

- [GitHub Encrypted Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Maven Central Publishing Guide](https://central.sonatype.org/publish/publish-guide/)
- [GPG Signing Guide](https://central.sonatype.org/publish/requirements/gpg/)
