# GPG 密钥设置指南

Maven Central 要求所有发布的包必须使用 GPG 签名。本指南将帮助你生成和配置 GPG 密钥。

---

## Windows 系统

### 1. 安装 GPG

下载并安装 **Gpg4win**:
- 官网: https://www.gpg4win.org/
- 下载完整版（包含 Kleopatra GUI 工具）

### 2. 生成密钥

**方式一: 使用命令行**

```powershell
# 打开 PowerShell 或 CMD
gpg --gen-key
```

按提示输入：
- 真实姓名: `Your Name`
- 电子邮件: `your@email.com`
- 密码: 设置一个强密码（记住它！）

**方式二: 使用 Kleopatra GUI**

1. 打开 Kleopatra
2. 点击 **New Key Pair**
3. 选择 **Create a personal OpenPGP key pair**
4. 填写姓名和邮箱
5. 点击 **Create**

### 3. 查看密钥

```powershell
gpg --list-keys
```

输出示例：
```
pub   rsa3072 2024-01-01 [SC]
      1234567890ABCDEF1234567890ABCDEF12345678
uid           [ultimate] Your Name <your@email.com>
sub   rsa3072 2024-01-01 [E]
```

**密钥 ID**: 取最后 8 位 → `12345678`

### 4. 导出密钥

```powershell
# 导出私钥
gpg --export-secret-keys -o secring.gpg

# Base64 编码（用于 GitHub Secrets）
[Convert]::ToBase64String([IO.File]::ReadAllBytes("secring.gpg")) | Out-File -Encoding ASCII gpg-base64.txt
```

### 5. 上传公钥到服务器

```powershell
gpg --keyserver keyserver.ubuntu.com --send-keys 12345678
```

或使用其他服务器：
```powershell
gpg --keyserver keys.openpgp.org --send-keys 12345678
```

---

## Linux 系统

### 1. 安装 GPG

大多数 Linux 发行版已预装 GPG。如果没有：

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install gnupg
```

**CentOS/RHEL:**
```bash
sudo yum install gnupg
```

**Arch Linux:**
```bash
sudo pacman -S gnupg
```

### 2. 生成密钥

```bash
gpg --gen-key
```

按提示输入信息。

### 3. 查看密钥

```bash
gpg --list-keys
```

### 4. 导出密钥

```bash
# 导出私钥
gpg --export-secret-keys -o ~/.gnupg/secring.gpg

# Base64 编码
base64 ~/.gnupg/secring.gpg > gpg-base64.txt
```

### 5. 上传公钥

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys 12345678
```

---

## macOS 系统

### 1. 安装 GPG

使用 Homebrew:
```bash
brew install gnupg
```

### 2. 生成密钥

```bash
gpg --gen-key
```

### 3. 查看密钥

```bash
gpg --list-keys
```

### 4. 导出密钥

```bash
# 导出私钥
gpg --export-secret-keys -o ~/.gnupg/secring.gpg

# Base64 编码（不换行）
base64 -b 0 ~/.gnupg/secring.gpg > gpg-base64.txt
```

### 5. 上传公钥

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys 12345678
```

---

## 配置 Gradle

### 方式一: 使用 gradle.properties（本地开发）

创建或编辑 `gradle.properties`:

```properties
signing.keyId=12345678
signing.password=your-gpg-password
signing.secretKeyRingFile=C:/Users/YourName/.gnupg/secring.gpg  # Windows
# signing.secretKeyRingFile=/home/yourname/.gnupg/secring.gpg  # Linux
# signing.secretKeyRingFile=/Users/yourname/.gnupg/secring.gpg  # macOS
```

### 方式二: 使用环境变量（CI/CD）

**Windows:**
```powershell
$env:SIGNING_KEY_ID="12345678"
$env:SIGNING_PASSWORD="your-gpg-password"
$env:SIGNING_SECRET_KEY_RING_FILE="C:/Users/YourName/.gnupg/secring.gpg"
```

**Linux/macOS:**
```bash
export SIGNING_KEY_ID=12345678
export SIGNING_PASSWORD=your-gpg-password
export SIGNING_SECRET_KEY_RING_FILE=$HOME/.gnupg/secring.gpg
```

---

## 验证签名

### 测试签名功能

创建测试文件并签名：

```bash
# 创建测试文件
echo "test" > test.txt

# 签名
gpg --sign test.txt

# 验证签名
gpg --verify test.txt.gpg
```

### 测试 Gradle 签名

```bash
# 构建并签名
gradlew clean build sign

# 检查签名文件
# 应该生成 .asc 文件
```

---

## 常见问题

### Q1: gpg: signing failed: Inappropriate ioctl for device

**解决方案 (Linux/macOS):**
```bash
export GPG_TTY=$(tty)
```

或添加到 `~/.bashrc` 或 `~/.zshrc`:
```bash
echo 'export GPG_TTY=$(tty)' >> ~/.bashrc
source ~/.bashrc
```

### Q2: gpg: signing failed: No secret key

**解决方案:**
1. 检查密钥 ID 是否正确
2. 确认密钥已生成：`gpg --list-secret-keys`
3. 检查密钥文件路径是否正确

### Q3: gpg: can't connect to the agent

**解决方案 (Windows):**
1. 重启 GPG Agent:
   ```powershell
   gpgconf --kill gpg-agent
   gpgconf --launch gpg-agent
   ```

**解决方案 (Linux/macOS):**
```bash
gpgconf --kill gpg-agent
gpg-agent --daemon
```

### Q4: 密钥过期

**解决方案:**
```bash
# 编辑密钥
gpg --edit-key 12345678

# 在 GPG 提示符下
gpg> expire
# 选择新的过期时间
gpg> save
```

### Q5: 忘记密码

**解决方案:**
- GPG 密码无法恢复
- 需要生成新的密钥对
- 撤销旧密钥（如果已上传）

---

## 密钥管理最佳实践

### 1. 备份密钥

```bash
# 备份私钥
gpg --export-secret-keys -o backup-secret.gpg

# 备份公钥
gpg --export -o backup-public.gpg

# 备份信任数据库
gpg --export-ownertrust > backup-trust.txt
```

将备份文件保存到安全位置（加密的 U 盘、密码管理器等）。

### 2. 设置过期时间

生成密钥时设置合理的过期时间（如 2 年），到期前可以延长。

### 3. 使用强密码

- 至少 12 个字符
- 包含大小写字母、数字、特殊字符
- 不要使用常见密码
- 使用密码管理器保存

### 4. 定期轮换

- 每 1-2 年生成新密钥
- 撤销旧密钥
- 更新所有使用旧密钥的地方

### 5. 撤销证书

生成撤销证书以备不时之需：

```bash
gpg --gen-revoke 12345678 > revoke.asc
```

保存到安全位置，如果密钥泄露可以使用它撤销密钥。

---

## 上传公钥到多个服务器

为了提高可用性，建议上传到多个密钥服务器：

```bash
# Ubuntu 密钥服务器
gpg --keyserver keyserver.ubuntu.com --send-keys 12345678

# OpenPGP 密钥服务器
gpg --keyserver keys.openpgp.org --send-keys 12345678

# MIT 密钥服务器
gpg --keyserver pgp.mit.edu --send-keys 12345678
```

---

## 验证公钥已上传

```bash
# 搜索你的密钥
gpg --keyserver keyserver.ubuntu.com --search-keys your@email.com
```

或访问网页：
- https://keyserver.ubuntu.com/
- https://keys.openpgp.org/

---

## 参考资料

- [GPG 官方文档](https://gnupg.org/documentation/)
- [Maven Central GPG 要求](https://central.sonatype.org/publish/requirements/gpg/)
- [GitHub GPG 签名指南](https://docs.github.com/en/authentication/managing-commit-signature-verification)
